package com.hometusk.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtCookieAuthFilter extends OncePerRequestFilter {

    private static final String COOKIE_NAME = "hometusk_token";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/api/v1/auth/login".equals(path)
                || "/api/v1/auth/register".equals(path)
                || "/api/v1/auth/refresh".equals(path)
                || "/api/v1/auth/logout".equals(path)
                || path.startsWith("/api/v1/auth/mobile/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getHeader(HttpHeaders.AUTHORIZATION) != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractTokenFromCookie(request);
        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String bearerToken = "Bearer " + token;
        HttpServletRequest wrapped = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                    return bearerToken;
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                    return Collections.enumeration(List.of(bearerToken));
                }
                return super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = Collections.list(super.getHeaderNames());
                boolean hasAuth = names.stream().anyMatch(header -> header.equalsIgnoreCase(HttpHeaders.AUTHORIZATION));
                if (!hasAuth) {
                    names.add(HttpHeaders.AUTHORIZATION);
                }
                return Collections.enumeration(names);
            }
        };

        filterChain.doFilter(wrapped, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
