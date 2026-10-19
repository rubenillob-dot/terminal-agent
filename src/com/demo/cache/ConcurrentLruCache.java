package com.demo.cache;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

/**
 * High-performance, thread-safe LRU Cache with time-to-live (TTL) expiration.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class ConcurrentLruCache<K, V> {

    private record CacheEntry<V>(V value, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final int capacity;
    private final long ttlMillis;
    private final LinkedHashMap<K, CacheEntry<V>> map;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final BiConsumer<K, V> evictionListener;

    public ConcurrentLruCache(int capacity, long ttlMillis, BiConsumer<K, V> evictionListener) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be positive.");
        this.capacity = capacity;
        this.ttlMillis = ttlMillis;
        this.evictionListener = evictionListener != null ? evictionListener : (k, v) -> {};
        this.map = new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, CacheEntry<V>> eldest) {
                boolean shouldEvict = size() > ConcurrentLruCache.this.capacity;
                if (shouldEvict && ConcurrentLruCache.this.evictionListener != null) {
                    ConcurrentLruCache.this.evictionListener.accept(eldest.getKey(), eldest.getValue().value());
                }
                return shouldEvict;
            }
        };
    }

    public void put(K key, V value) {
        lock.writeLock().lock();
        try {
            map.put(key, new CacheEntry<>(value, Instant.now().plusMillis(ttlMillis)));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<V> get(K key) {
        lock.writeLock().lock();
        try {
            CacheEntry<V> entry = map.get(key);
            if (entry == null) return Optional.empty();
            if (entry.isExpired()) {
                map.remove(key);
                evictionListener.accept(key, entry.value());
                return Optional.empty();
            }
            return Optional.of(entry.value());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try {
            return map.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}
