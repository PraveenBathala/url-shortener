package com.example.urlshortener.api;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.urlshortener.service.RedirectService;

@RestController
public class RedirectController {

    private final RedirectService redirectService;

    public RedirectController(RedirectService redirectService) {
        this.redirectService = redirectService;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String destination = redirectService.resolveDestination(shortCode);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(destination));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
