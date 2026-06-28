package net.cannasmp.statsapi.authentication;

import com.sun.net.httpserver.HttpExchange;
import net.cannasmp.statsapi.config.ApiConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RateLimiter {
    private final ApiConfig config;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimiter(ApiConfig config) {
        this.config = config;
    }

    public boolean isLimited(HttpExchange exchange) {
        if (!config.rateLimitEnabled()) {
            return false;
        }
        String address = exchange.getRemoteAddress().getAddress().getHostAddress();
        return !buckets.computeIfAbsent(address, ignored -> new Bucket()).tryConsume(config.rateLimitRequestsPerMinute());
    }

    private static final class Bucket {
        private long windowStarted = System.currentTimeMillis();
        private int used;

        synchronized boolean tryConsume(int limit) {
            long now = System.currentTimeMillis();
            if (now - windowStarted >= 60_000L) {
                windowStarted = now;
                used = 0;
            }
            if (used >= limit) {
                return false;
            }
            used++;
            return true;
        }
    }
}
