package com.mall.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllowedHttpMethodFilterTest {

    private final AllowedHttpMethodFilter filter = new AllowedHttpMethodFilter(new ObjectMapper());

    @Test
    void allowsGetAndPost() throws Exception {
        for (String method : Arrays.asList("GET", "POST")) {
            MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/test");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertFalse(response.isCommitted());
            assertTrue(chain.getRequest() != null);
        }
    }

    @Test
    void rejectsEveryOtherMethod() throws Exception {
        for (String method : Arrays.asList("PUT", "DELETE", "PATCH", "HEAD", "OPTIONS", "TRACE")) {
            MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/test");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertEquals(405, response.getStatus(), method);
            assertEquals("GET, POST", response.getHeader(HttpHeaders.ALLOW), method);
            assertTrue(response.getContentAsString().contains("METHOD_NOT_ALLOWED"), method);
            assertTrue(chain.getRequest() == null, method);
        }
    }
}
