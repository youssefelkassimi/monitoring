package com.elkassimi.monitoring_v2_0.config;

import com.elkassimi.monitoring_v2_0.model.Agent;
import com.elkassimi.monitoring_v2_0.service.JwtService;
import com.elkassimi.monitoring_v2_0.service.TokenHash;
import com.elkassimi.monitoring_v2_0.repository.AgentRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final AgentRepository agentRepository;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/actuator/health", "/error").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/agent-register", "/api/heartbeat").hasRole("AGENT")
                        .requestMatchers("/api/metrics", "/api/inventory", "/api/discovery", "/api/logs",
                                "/api/alerts", "/api/commands/**", "/api/agent-config/**").hasRole("AGENT")
                        .requestMatchers("/api").hasAnyRole("ADMIN", "VIEWER")
                        .anyRequest().hasAnyRole("ADMIN", "VIEWER"))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, agentRepository),
                        UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, ex) ->
                                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Authentication required"))
                        .accessDeniedHandler((request, response, ex) ->
                                response.sendError(HttpStatus.FORBIDDEN.value(), "Forbidden")));
        return http.build();
    }

    @RequiredArgsConstructor
    static class JwtAuthenticationFilter extends OncePerRequestFilter {
        private final JwtService jwtService;
        private final AgentRepository agentRepository;

        @Override
        protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response,
                                        @NonNull FilterChain filterChain) throws ServletException, IOException {
            String header = request.getHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            try {
                String token = header.substring(7);
                Claims claims = jwtService.parse(token);
                String kind = claims.get("kind", String.class);
                if ("agent".equals(kind)) {
                    authenticateAgent(request, token, claims);
                } else if ("user".equals(kind)) {
                    String role = claims.get("role", String.class);
                    if (role == null) {
                        throw new JwtException("Missing user role");
                    }
                    authenticate(claims.getSubject(), "ROLE_" + role);
                } else {
                    throw new JwtException("Unknown token kind");
                }
            } catch (AgentOfflineException ex) {
                response.sendError(HttpStatus.FORBIDDEN.value(), "Agent is offline");
                return;
            } catch (JwtException | IllegalArgumentException ex) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid or expired token");
                return;
            }
            filterChain.doFilter(request, response);
        }

        private void authenticateAgent(HttpServletRequest request, String token, Claims claims) {
            Optional<Agent> candidate = agentRepository.findByAgentId(claims.getSubject());
            Agent agent = candidate.orElseThrow(() -> new JwtException("Unknown agent"));
            if (!TokenHash.sha256(token).equals(agent.getTokenHash())
                    || agent.getProvisioningStatus() == Agent.ProvisioningStatus.REVOKED
                    || agent.getProvisioningStatus() == Agent.ProvisioningStatus.EXPIRED) {
                throw new JwtException("Agent token is not active");
            }
            boolean connectionEndpoint = request.getRequestURI().equals("/api/agent-register")
                    || request.getRequestURI().equals("/api/heartbeat");
            if (!connectionEndpoint && agent.getStatus() != Agent.AgentStatus.ONLINE) {
                throw new AgentOfflineException();
            }
            authenticate(agent.getAgentId(), "ROLE_AGENT");
        }

        private void authenticate(String subject, String authority) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    subject, null, List.of(new SimpleGrantedAuthority(authority)));
            org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .setAuthentication(authentication);
        }
    }

    static class AgentOfflineException extends RuntimeException {
    }
}
