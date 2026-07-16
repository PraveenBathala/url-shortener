package com.example.urlshortener;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.example.urlshortener.config.AppProperties;
import com.redis.testcontainers.RedisContainer;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class UrlShortenerApplicationTests {

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
    private Environment environment;

    @Autowired
    private AppProperties appProperties;

    @Test
    void contextLoadsWithEnvironmentBackedConfiguration() {
        assertThat(environment.getProperty("spring.application.name")).isEqualTo("url-shortener");
        assertThat(appProperties.getBaseUrl()).isNotBlank();
        assertThat(appProperties.getShortCode().getLength()).isEqualTo(7);
        assertThat(appProperties.getShortCode().getMaxGenerationAttempts()).isPositive();
        assertThat(appProperties.getDestinationUrl().getMaxLength()).isEqualTo(2048);
        assertThat(postgres.isRunning()).isTrue();
        assertThat(redis.isRunning()).isTrue();
    }
}
