package com.example.urlshortener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.example.urlshortener.domain.ShortUrl;
import com.example.urlshortener.domain.ShortUrlStatus;
import com.example.urlshortener.persistence.ShortUrlRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Import(UrlShortenerFlowIT.FixedClockConfig.class)
class UrlShortenerFlowIT {

    private static final Instant NOW = Instant.parse("2026-07-16T20:00:00Z");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("urlshortener")
            .withUsername("urlshortener")
            .withPassword("urlshortener");

    @Container
    @ServiceConnection
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Test
    void createPersistRedirectAndDisableFlow() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "destinationUrl": "https://example.com/products/123",
                                  "expiresAt": "2027-01-01T00:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").isNotEmpty())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        JsonNode body = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String shortCode = body.get("shortCode").asText();

        assertThat(shortUrlRepository.findById(shortCode)).isPresent();

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/products/123"));

        // cache hit path
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/products/123"));

        mockMvc.perform(delete("/api/v1/urls/" + shortCode))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.errorCode").value("SHORT_CODE_DISABLED"));

        mockMvc.perform(delete("/api/v1/urls/missing1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownExpiredAndDisabledCodes() throws Exception {
        mockMvc.perform(get("/unknown1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SHORT_CODE_NOT_FOUND"));

        shortUrlRepository.saveAndFlush(new ShortUrl(
                "expired1",
                "https://example.com/expired",
                ShortUrlStatus.ACTIVE,
                NOW,
                NOW,
                Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(get("/expired1"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.errorCode").value("SHORT_CODE_EXPIRED"));

        ShortUrl disabled = new ShortUrl(
                "disable1",
                "https://example.com/disabled",
                ShortUrlStatus.ACTIVE,
                NOW,
                NOW,
                null);
        disabled.disable(NOW);
        shortUrlRepository.saveAndFlush(disabled);

        mockMvc.perform(get("/disable1"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.errorCode").value("SHORT_CODE_DISABLED"));
    }

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
