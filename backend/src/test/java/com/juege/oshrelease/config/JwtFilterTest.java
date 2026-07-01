package com.juege.oshrelease.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.juege.oshrelease.repo.AppUserRepository;
import javax.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtFilterTest {

    @Test
    void invalidTokenShouldReturn401Json() throws Exception {
        JwtService jwtService = Mockito.mock(JwtService.class);
        AppUserRepository appUserRepository = Mockito.mock(AppUserRepository.class);
        JwtFilter filter = new JwtFilter(jwtService, appUserRepository, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dashboard/summary");
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        Mockito.when(jwtService.parseToken("bad-token")).thenThrow(new IllegalArgumentException("bad token"));

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        assertTrue(response.getContentAsString().contains("登录已失效，请重新登录"));
        Mockito.verify(chain, Mockito.never()).doFilter(Mockito.any(), Mockito.any());
    }
}
