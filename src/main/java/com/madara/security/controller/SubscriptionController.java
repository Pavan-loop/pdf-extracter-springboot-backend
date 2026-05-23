package com.madara.security.controller;

import com.madara.security.response.DTO.ApiResponse;
import com.madara.security.service.StripeService;
import com.madara.security.utility.SecurityUtils;
import com.madara.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("subscription")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(StripeService.class)
public class SubscriptionController {

    private final StripeService stripeService;
    private final UserRepository userRepository;

    /**
     * Create a Stripe Checkout session.
     * Frontend redirects to the returned URL.
     * GET /subscription/checkout
     */
    @GetMapping("/checkout")
    public ResponseEntity<ApiResponse<Map<String, String>>> createCheckout() {
        try {
            long userId = SecurityUtils.getCurrentUserId();
            String email = userRepository.findById(userId)
                    .map(u -> u.getEmail())
                    .orElseThrow();

            String checkoutUrl = stripeService.createCheckoutSession(email);
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("checkoutUrl", checkoutUrl),
                    "Checkout session created",
                    HttpStatus.OK
            ));
        } catch (Exception e) {
            log.error("Failed to create Stripe checkout: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error("Failed to create checkout session", HttpStatus.INTERNAL_SERVER_ERROR)
            );
        }
    }

    /**
     * Stripe webhook — called by Stripe, NOT by the frontend.
     * This endpoint must be PUBLIC (no JWT required).
     * POST /subscription/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        try {
            stripeService.handleWebhook(payload, sigHeader);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Webhook rejected: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Webhook processing error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
