package com.madara.security.security;

import com.madara.security.security.jwt.JwtFilter;
import com.madara.security.security.oauth2.OAuthSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationProvider authenticationProvider;
    private final JwtFilter jwtFilter;
    private final OAuthSuccessHandler oAuthSuccessHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(request -> request
                        // Auth endpoints — no token required
                        .requestMatchers("/auth/**").permitAll()
                        // OAuth2 redirect endpoints — no token required
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        // WebSocket handshake — no token required
                        .requestMatchers("/ws/**").permitAll()
                        // Role-based routes
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/user/**").hasRole("USER")
                        // Everything else needs a valid JWT
                        .anyRequest().authenticated()
                )
                // FIX: STATELESS — no server-side HTTP sessions at all.
                // Every request must carry its own JWT and authenticate fresh.
                // IF_REQUIRED was the root cause: Spring was reusing the SecurityContext
                // from a previous user's HTTP session, making User A's identity leak
                // into User B's requests.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                // Google OAuth2 login
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuthSuccessHandler)
                )
                .build();
    }
}