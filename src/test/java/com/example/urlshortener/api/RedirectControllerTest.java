package com.example.urlshortener.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.urlshortener.api.error.GlobalExceptionHandler;
import com.example.urlshortener.api.error.InvalidShortCodeException;
import com.example.urlshortener.api.error.ShortCodeDisabledException;
import com.example.urlshortener.api.error.ShortCodeExpiredException;
import com.example.urlshortener.api.error.ShortCodeNotFoundException;
import com.example.urlshortener.service.RedirectService;

@WebMvcTest(controllers = RedirectController.class)
@Import({GlobalExceptionHandler.class, RequestIdFilter.class})
class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RedirectService redirectService;

    @Test
    void returnsFoundWithLocation() throws Exception {
        when(redirectService.resolveDestination("aB7xK9P")).thenReturn("https://example.com/products/123");

        mockMvc.perform(get("/aB7xK9P"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/products/123"));
    }

    @Test
    void mapsDomainErrorsToHttpStatuses() throws Exception {
        when(redirectService.resolveDestination("missing1")).thenThrow(new ShortCodeNotFoundException("missing1"));
        mockMvc.perform(get("/missing1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SHORT_CODE_NOT_FOUND"));

        when(redirectService.resolveDestination("expired1")).thenThrow(new ShortCodeExpiredException("expired1"));
        mockMvc.perform(get("/expired1"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.errorCode").value("SHORT_CODE_EXPIRED"));

        when(redirectService.resolveDestination("disable1")).thenThrow(new ShortCodeDisabledException("disable1"));
        mockMvc.perform(get("/disable1"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.errorCode").value("SHORT_CODE_DISABLED"));

        when(redirectService.resolveDestination("bad!")).thenThrow(new InvalidShortCodeException("Short code is malformed"));
        mockMvc.perform(get("/bad!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_SHORT_CODE"));
    }
}
