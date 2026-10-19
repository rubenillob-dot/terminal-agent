package com.agent;

import com.agent.core.AgentContext;
import com.agent.core.AutonomousAgent;
import com.agent.model.Task;
import com.agent.model.TaskResult;
import com.agent.planner.DefaultTaskPlanner;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Main application launcher for the Autonomous Software Engineering Agent.
 *
 * <p>Demonstrates the autonomous software development lifecycle by receiving a technical
 * problem specification, generating thread-safe enterprise code, compiling natively via javac,
 * testing execution, and verifying software design compliance.</p>
 */
public final class Main {

    private Main() {
        // Prevent instantiation
    }

    /**
     * Application entry point.
     *
     * @param args command-line arguments (optional workspace root path)
     */
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║   Autonomous Agent for Java Software Architecture & Systems   ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");

        Path workspaceRoot = args.length > 0 ? Path.of(args[0]) : Path.of(".").toAbsolutePath().normalize();
        System.out.println("[Init] Active Workspace Root: " + workspaceRoot);

        AgentContext context = AgentContext.createDefault(workspaceRoot);
        AutonomousAgent agent = new AutonomousAgent(context);

        // Technical Challenge: High-concurrency TTL LRU Cache with Eviction Callbacks
        String cacheSource = """
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
                """;

        String testSource = """
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
                """;

        Map<String, Object> contextData = new HashMap<>();
        contextData.put(DefaultTaskPlanner.CONTEXT_FILES_KEY, Map.of(
                "src/com/demo/cache/ConcurrentLruCache.java", cacheSource,
                "src/com/demo/cache/CacheValidationTest.java", testSource
        ));
        contextData.put(DefaultTaskPlanner.CONTEXT_COMPILE_CMD_KEY, "javac -d out src/com/demo/cache/*.java");
        contextData.put(DefaultTaskPlanner.CONTEXT_TEST_CMD_KEY, "java -cp out com.demo.cache.CacheValidationTest");

        Task task = new Task(
                "TASK-PROD-001",
                "Implement, compile, test and verify high-performance thread-safe ConcurrentLruCache with TTL",
                contextData
        );

        TaskResult result = agent.executeTask(task);

        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("AGENT TASK SUMMARY: " + result.getTask().getId());
        System.out.println("Status: " + result.getStatus());
        System.out.println("Success: " + result.isSuccess());
        System.out.println("Duration: " + result.getDuration().toMillis() + " ms");
        System.out.println("Steps Executed: " + result.getStepsExecuted());
        System.out.println("Summary: " + result.getSummary());
        System.out.println("═══════════════════════════════════════════════════════════════");
    }
}
