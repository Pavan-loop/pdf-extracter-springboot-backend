package com.madara.security.security.oauth2;

import com.madara.security.model.AuthProvider;
import com.madara.security.model.Role;
import com.madara.security.model.User;
import com.madara.security.repository.UserRepository;
import com.madara.security.security.config.CustomUserDetails;
import com.madara.security.security.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${application.oauth2.redirect-uri}")
    private String redirectUri;

    /**
     * Called after Google successfully authenticates the user.
     * 1. Extract user info from Google's OAuth2 response
     * 2. Create user in DB if first time login
     * 3. Generate our own JWT
     * 4. Redirect to Next.js with JWT as query param
     *    Next.js picks it up and stores it in localStorage/cookie
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Google provides these attributes
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getAttribute("sub"); // Google's unique user ID

        log.info("OAuth2 login success for email={}", email);

        // Find or create user in our DB
        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            // If they previously registered locally, link their Google account
            if (user.getAuthProvider() == AuthProvider.LOCAL) {
                user.setAuthProvider(AuthProvider.GOOGLE);
                user.setProviderId(providerId);
                userRepository.save(user);
                log.info("Linked Google account to existing LOCAL user: {}", email);
            }
        } else {
            // First time Google login — create account automatically
            user = User.builder()
                    .name(name)
                    .username(name)
                    .email(email)
                    .password(null) // No password for OAuth users
                    .authProvider(AuthProvider.GOOGLE)
                    .providerId(providerId)
                    .roles(Collections.singleton(Role.USER))
                    .isAccountEnabled(true)
                    .isAccountNonLocked(true)
                    .build();
            userRepository.save(user);
            log.info("New user created via Google OAuth: {}", email);
        }

        // Build CustomUserDetails so JwtService can generate the token
        CustomUserDetails userDetails = new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getAuthorities()
        );

        // Generate our standard JWT — from here everything works exactly like local login
        String jwt = jwtService.generateToken(userDetails);

        // Redirect to Next.js with token in query param
        // Next.js reads ?token=... and stores it
        String targetUrl = redirectUri + "?token=" + jwt;
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
