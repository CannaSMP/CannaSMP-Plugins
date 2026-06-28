package net.cannasmp.statsapi.authentication;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import net.cannasmp.statsapi.config.ApiConfig;

import java.nio.charset.StandardCharsets;

public final class ApiKeyAuthenticator {
    private final ApiConfig config;

    public ApiKeyAuthenticator(ApiConfig config) {
        this.config = config;
    }

    public boolean isAuthenticated(HttpExchange exchange) {
        if (!config.apiKeyConfigured()) {
            return false;
        }
        Headers headers = exchange.getRequestHeaders();
        String authorization = headers.getFirst("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return constantEquals(config.apiKey(), authorization.substring("Bearer ".length()).trim());
        }
        String apiKey = headers.getFirst("X-API-Key");
        return apiKey != null && constantEquals(config.apiKey(), apiKey.trim());
    }

    public boolean requiresAuthentication(String path) {
        return config.requireAuthByDefault() && !config.isPublicEndpoint(path);
    }

    private boolean constantEquals(String expected, String actual) {
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = actual.getBytes(StandardCharsets.UTF_8);
        int diff = a.length ^ b.length;
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            diff |= (i < a.length ? a[i] : 0) ^ (i < b.length ? b[i] : 0);
        }
        return diff == 0;
    }
}
