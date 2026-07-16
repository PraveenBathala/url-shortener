package com.example.urlshortener.cache;

import java.util.Optional;

public interface ShortUrlCache {

    Optional<CachedShortUrl> get(String shortCode);

    void put(CachedShortUrl cachedShortUrl);

    void putNegative(String shortCode);

    void evict(String shortCode);
}
