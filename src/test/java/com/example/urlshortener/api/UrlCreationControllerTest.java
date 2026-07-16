package com.example.urlshortener.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.urlshortener.api.dto.ShortUrlResponse;
import com.example.urlshortener.api.error.GlobalExceptionHandler;
import com.example.urlshortener.api.error.InvalidDestinationUrlException;
import com.example.urlshortener.domain.ShortUrlStatus;
import com.example.urlshortener.service.UrlCreationService;

@WebMvcTest(controllers = UrlCreationController.class)
@Import({GlobalExceptionHandler.class, RequestIdFilter.class})
class UrlCreationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UrlCreationService urlCreationService;

    @Test
    void returnsCreatedWithBody() throws Exception {
        when(urlCreationService.create(any())).thenReturn(new ShortUrlResponse(
                "aB7xK9P",
                "http://localhost:8080/aB7xK9P",
                "https://example.com/products/123",
                Instant.parse("2026-07-16T20:00:00Z"),
                Instant.parse("2027-01-01T00:00:00Z"),
                ShortUrlStatus.ACTIVE));

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "destinationUrl": "https://example.com/products/123",
                                  "expiresAt": "2027-01-01T00:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("aB7xK9P"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/aB7xK9P"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void returnsStableErrorSchemaForInvalidDestination() throws Exception {
        when(urlCreationService.create(any()))
                .thenThrow(new InvalidDestinationUrlException("Destination URL scheme must be http or https"));

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinationUrl":"javascript:alert(1)"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("INVALID_DESTINATION_URL"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/urls"))
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
