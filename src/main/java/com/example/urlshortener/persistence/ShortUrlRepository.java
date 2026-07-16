package com.example.urlshortener.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.urlshortener.domain.ShortUrl;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, String> {
}
