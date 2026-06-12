package com.hometusk.auth.filter;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("JwtCookieAuthFilter")
class JwtCookieAuthFilterTest {

    private final JwtCookieAuthFilter filter = new JwtCookieAuthFilter();

    @Test
    @DisplayName("adds bearer authorization header from access cookie")
    void addsAuthorizationHeaderFromCookie() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.setCookies(new Cookie("hometusk_token", "access-token"));
        CapturingFilterChain chain = new CapturingFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.request.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer access-token");
    }

    @Test
    @DisplayName("keeps explicit authorization header unchanged")
    void keepsExplicitAuthorizationHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer explicit-token");
        request.setCookies(new Cookie("hometusk_token", "cookie-token"));
        CapturingFilterChain chain = new CapturingFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.request.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer explicit-token");
    }

    private static final class CapturingFilterChain extends MockFilterChain {
        private HttpServletRequest request;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response)
                throws IOException, ServletException {
            this.request = (HttpServletRequest) request;
            super.doFilter(request, response);
        }
    }
}
