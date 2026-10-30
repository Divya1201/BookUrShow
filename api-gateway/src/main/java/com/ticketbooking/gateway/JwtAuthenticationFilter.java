package com.ticketbooking.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Validates the JWT once at the edge. Downstream services trust the X-User-* headers,
 * so any client-supplied copies of those headers are stripped first (prevents spoofing).
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_EMAIL_HEADER = "X-User-Email";

    private static final List<String> PUBLIC_PATHS = List.of("/api/auth/**", "/actuator/**");
    private static final List<String> PUBLIC_GET_PATHS = List.of("/api/movies/**", "/api/shows/**");

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final SecretKey key;

    public JwtAuthenticationFilter(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Always remove identity headers coming from the outside world
        ServerHttpRequest cleaned = request.mutate()
                .headers(h -> {
                    h.remove(USER_ID_HEADER);
                    h.remove(USER_EMAIL_HEADER);
                })
                .build();

        if (isPublic(path, request.getMethod())) {
            return chain.filter(exchange.mutate().request(cleaned).build());
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return reject(exchange);
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(authHeader.substring(7))
                    .getPayload();

            ServerHttpRequest authenticated = cleaned.mutate()
                    .header(USER_ID_HEADER, claims.getSubject())
                    .header(USER_EMAIL_HEADER, claims.get("email", String.class))
                    .build();
            return chain.filter(exchange.mutate().request(authenticated).build());
        } catch (JwtException | IllegalArgumentException e) {
            return reject(exchange);
        }
    }

    private boolean isPublic(String path, HttpMethod method) {
        if (PUBLIC_PATHS.stream().anyMatch(p -> matcher.match(p, path))) {
            return true;
        }
        return HttpMethod.GET.equals(method)
                && PUBLIC_GET_PATHS.stream().anyMatch(p -> matcher.match(p, path));
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1; // run before routing filters
    }
}
