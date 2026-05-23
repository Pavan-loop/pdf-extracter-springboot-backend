package com.madara.security.websocket;

import com.madara.security.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:3000", "http://localhost:3001")
                .withSockJS()
                .setHeartbeatTime(25000);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null) return message;

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        log.warn("WebSocket CONNECT rejected: missing or invalid Authorization header");
                        throw new IllegalArgumentException("Missing Authorization header");
                    }

                    String jwt = authHeader.substring(7);
                    try {
                        String email = jwtService.extractEmail(jwt);
                        if (email == null) throw new IllegalArgumentException("Email not found in token");

                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                        if (!jwtService.isTokenValid(jwt, userDetails)) {
                            throw new IllegalArgumentException("Token is expired or invalid");
                        }

                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(
                                        email,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        accessor.setUser(auth);
                        log.info("WebSocket CONNECT authenticated: {}", email);

                    } catch (Exception e) {
                        log.error("WebSocket CONNECT rejected: {}", e.getMessage());
                        throw new IllegalArgumentException("Invalid token: " + e.getMessage());
                    }
                }

                if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                    String user = accessor.getUser() != null ? accessor.getUser().getName() : "unknown";
                    log.info("WebSocket DISCONNECT: {}", user);
                }

                return message;
            }
        });
    }
}
