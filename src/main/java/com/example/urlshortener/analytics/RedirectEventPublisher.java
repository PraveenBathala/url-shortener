package com.example.urlshortener.analytics;

public interface RedirectEventPublisher {

    void publish(RedirectEvent event);
}
