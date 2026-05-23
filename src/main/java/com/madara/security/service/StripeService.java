package com.madara.security.service;

import com.madara.security.model.Plan;
import com.madara.security.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@ConditionalOnProperty(name = "application.stripe.secret-key")
@RequiredArgsConstructor
@Slf4j
public class StripeService {

    private final UserRepository userRepository;

    @Value("${application.stripe.secret-key}")
    private String secretKey;

    @Value("${application.stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${application.stripe.price-id}")
    private String priceId;

    @Value("${application.stripe.success-url:http://localhost:3000/subscription/success}")
    private String successUrl;

    @Value("${application.stripe.cancel-url:http://localhost:3000/subscription/cancel}")
    private String cancelUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    public String createCheckoutSession(String userEmail) throws Exception {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomerEmail(userEmail)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build())
                .putMetadata("userEmail", userEmail)
                .build();

        Session session = Session.create(params);
        log.info("Stripe checkout session created for user={}", userEmail);
        return session.getUrl();
    }

    @Transactional
    public void handleWebhook(String payload, String sigHeader) throws Exception {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Stripe webhook signature verification failed");
            throw new IllegalArgumentException("Invalid Stripe webhook signature");
        }

        log.info("Stripe webhook received: type={}", event.getType());

        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            case "customer.subscription.deleted" -> handleSubscriptionCancelled(event);
            default -> log.debug("Unhandled Stripe event: {}", event.getType());
        }
    }

    private void handleCheckoutCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("Could not deserialize Stripe session"));

        String userEmail = session.getMetadata().get("userEmail");
        if (userEmail == null) {
            log.error("No userEmail in Stripe session metadata");
            return;
        }

        userRepository.findByEmail(userEmail).ifPresent(user -> {
            user.setPlan(Plan.SUBSCRIBER);
            user.setSubscriptionExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
            user.setPagesUploadedThisMonth(0);
            user.setQuotaResetAt(null);
            userRepository.save(user);
            log.info("User upgraded to SUBSCRIBER via Stripe: {}", userEmail);
        });
    }

    private void handleSubscriptionCancelled(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("Could not deserialize Stripe session"));

        String userEmail = session.getCustomerEmail();
        if (userEmail == null) return;

        userRepository.findByEmail(userEmail).ifPresent(user -> {
            user.setPlan(Plan.FREE);
            user.setSubscriptionExpiresAt(null);
            user.setPagesUploadedThisMonth(0);
            userRepository.save(user);
            log.info("User downgraded to FREE after Stripe cancellation: {}", userEmail);
        });
    }
}
