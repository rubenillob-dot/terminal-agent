package com.demo.cache;

import java.util.concurrent.atomic.AtomicInteger;

public class CacheValidationTest {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("--> Testing ConcurrentLruCache...");

        AtomicInteger evictions = new AtomicInteger(0);
        ConcurrentLruCache<String, String> cache = new ConcurrentLruCache<>(2, 200, (k, v) -> evictions.incrementAndGet());

        cache.put("k1", "v1");
        cache.put("k2", "v2");
        cache.put("k3", "v3"); // triggers LRU eviction of k1

        if (cache.get("k1").isPresent()) throw new AssertionError("k1 should have been evicted by capacity.");
        if (cache.get("k2").isEmpty()) throw new AssertionError("k2 should be present.");
        if (cache.get("k3").isEmpty()) throw new AssertionError("k3 should be present.");

        // Test TTL expiration
        Thread.sleep(250);
        if (cache.get("k2").isPresent()) throw new AssertionError("k2 should have expired due to TTL.");

        System.out.println("--> [OK] CacheValidationTest passed with all assertions verified.");
    }
}
