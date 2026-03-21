package com.example.bankingpaymentservice.actuator;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Endpoint(id = "cacheevict")
public class CacheEvictEndpoint {

    private final CacheManager cacheManager;

    public CacheEvictEndpoint(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @DeleteOperation
    public Map<String, Object> evict(String cacheName, String key) {
        String resolvedCacheName = StringUtils.hasText(cacheName) ? cacheName : "accounts";
        Cache cache = cacheManager.getCache(resolvedCacheName);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("cacheName", resolvedCacheName);

        if (cache == null) {
            response.put("status", "cache-not-found");
            return response;
        }

        if (StringUtils.hasText(key)) {
            cache.evict(key);
            response.put("status", "entry-evicted");
            response.put("key", key);
        } else {
            cache.clear();
            response.put("status", "cache-cleared");
        }

        return response;
    }
}
