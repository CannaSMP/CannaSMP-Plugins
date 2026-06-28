package net.cannasmp.statsapi.authentication;

import com.sun.net.httpserver.HttpExchange;
import net.cannasmp.statsapi.config.ApiConfig;

public final class IpWhitelist {
    private final ApiConfig config;

    public IpWhitelist(ApiConfig config) {
        this.config = config;
    }

    public boolean isAllowed(HttpExchange exchange) {
        if (!config.ipWhitelistEnabled()) {
            return true;
        }
        String address = exchange.getRemoteAddress().getAddress().getHostAddress();
        return config.ipWhitelist().contains(address);
    }
}
