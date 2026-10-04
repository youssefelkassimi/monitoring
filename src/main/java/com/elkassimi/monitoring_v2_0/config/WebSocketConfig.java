package com.elkassimi.monitoring_v2_0.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.elkassimi.monitoring_v2_0.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final List<String> ADMIN_TOPICS = List.of(
            "/topic/admin",
            "/topic/users",
            "/topic/user_update",
            "/topic/command_result",
            "/topic/commands");

    private final JwtService jwtService;

    @Value("${app.websocket.topic:/topic}")
    private String topicPrefix;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setApplicationDestinationPrefixes("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new JwtStompChannelInterceptor(jwtService));
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws");
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Bean
    ObjectMapper mapper() {
        return new ObjectMapper();
    }

    private static final class JwtStompChannelInterceptor
            implements org.springframework.messaging.support.ChannelInterceptor {

        private final JwtService jwtService;

        private JwtStompChannelInterceptor(JwtService jwtService) {
            this.jwtService = jwtService;
        }

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                // 1. Authenticate and set user on CONNECT
                Authentication auth = authenticate(accessor.getFirstNativeHeader("Authorization"));
                accessor.setUser(auth);

                // 2. Save user to session attributes so it persists across SUBSCRIBE/SEND
                // frames
                if (accessor.getSessionAttributes() != null) {
                    accessor.getSessionAttributes().put("auth_user", auth);
                }
            } else {
                // 3. Restore user from session attributes for subsequent frames (SUBSCRIBE,
                // etc.)
                if (accessor.getUser() == null && accessor.getSessionAttributes() != null) {
                    Authentication auth = (Authentication) accessor.getSessionAttributes().get("auth_user");
                    if (auth != null) {
                        accessor.setUser(auth);
                    }
                }
            }

            // 4. Validate admin topics on SUBSCRIBE
            if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) && isAdminTopic(accessor.getDestination())
                    && !isAdmin(accessor.getUser())) {
                throw new MessageDeliveryException("Only administrators may subscribe to this topic");
            }

            return message;
        }

        private Authentication authenticate(String authorization) {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                throw new MessageDeliveryException("A bearer token is required for WebSocket connections");
            }
            try {
                Claims claims = jwtService.parse(authorization.substring(7));
                if (!"user".equals(claims.get("kind", String.class))) {
                    throw new JwtException("WebSocket requires a user token");
                }
                String role = claims.get("role", String.class);
                if (role == null) {
                    throw new JwtException("Missing user role");
                }
                return new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            } catch (JwtException | IllegalArgumentException ex) {
                throw new MessageDeliveryException(null, ex);
            }
        }

        private boolean isAdminTopic(String destination) {
            return destination != null && ADMIN_TOPICS.stream()
                    .anyMatch(topic -> destination.equals(topic) || destination.startsWith(topic + "/")
                            || destination.endsWith(topic));
        }

        private boolean isAdmin(Principal principal) {
            return principal instanceof Authentication authentication
                    && authentication.getAuthorities().stream()
                            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        }
    }
}
