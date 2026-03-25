package com.novel.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ConcurrentHashMap<String, CacheEntry> localCache = new ConcurrentHashMap<>();
    private boolean redisAvailable = true;
    
    // 缓存键前缀
    private static final String RECOMMENDATION_PREFIX = "recommendation:";
    private static final String POPULAR_PREFIX = "popular:";
    
    // 缓存过期时间（分钟）
    private static final long RECOMMENDATION_CACHE_TIME = 30;
    private static final long POPULAR_CACHE_TIME = 60;

    // 本地缓存过期时间（毫秒）
    private static final long LOCAL_CACHE_EXPIRY = 30 * 60 * 1000; // 30分钟

    @Autowired
    public RedisCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        // 测试 Redis 连接
        testRedisConnection();
    }
    
    /**
     * 测试 Redis 连接，只在启动时打印一次
     */
    private void testRedisConnection() {
        try {
            redisTemplate.opsForValue().get("test-connection");
            System.out.println("✅ Redis 连接成功");
        } catch (Exception e) {
            redisAvailable = false;
            System.out.println("⚠️ Redis 连接失败，已自动切换到本地内存缓存模式");
            System.out.println("💡 提示：本地缓存模式在开发环境下可以正常工作，生产环境建议安装 Redis");
        }
    }

    /**
     * 获取个性化推荐缓存
     */
    public Object getPersonalizedRecommendations(Long userId) {
        String key = RECOMMENDATION_PREFIX + "user:" + userId;
        
        if (redisAvailable) {
            try {
                return redisTemplate.opsForValue().get(key);
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        
        // 使用本地缓存
        return getFromLocalCache(key);
    }

    /**
     * 设置个性化推荐缓存
     */
    public void setPersonalizedRecommendations(Long userId, Object recommendations) {
        String key = RECOMMENDATION_PREFIX + "user:" + userId;
        
        if (redisAvailable) {
            try {
                redisTemplate.opsForValue().set(key, recommendations, RECOMMENDATION_CACHE_TIME, TimeUnit.MINUTES);
                return;
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        
        // 使用本地缓存
        putInLocalCache(key, recommendations);
    }

    /**
     * 获取热门推荐缓存
     */
    public Object getPopularRecommendations() {
        String key = POPULAR_PREFIX + "list";
        
        if (redisAvailable) {
            try {
                return redisTemplate.opsForValue().get(key);
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        
        // 使用本地缓存
        return getFromLocalCache(key);
    }

    /**
     * 设置热门推荐缓存
     */
    public void setPopularRecommendations(Object recommendations) {
        String key = POPULAR_PREFIX + "list";
        
        if (redisAvailable) {
            try {
                redisTemplate.opsForValue().set(key, recommendations, POPULAR_CACHE_TIME, TimeUnit.MINUTES);
                return;
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        
        // 使用本地缓存
        putInLocalCache(key, recommendations);
    }

    /**
     * 通用方法：获取缓存
     */
    public Object get(String key) {
        if (redisAvailable) {
            try {
                return redisTemplate.opsForValue().get(key);
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        return getFromLocalCache(key);
    }

    /**
     * 通用方法：设置缓存
     */
    public void set(String key, Object value) {
        if (redisAvailable) {
            try {
                redisTemplate.opsForValue().set(key, value, 30, TimeUnit.MINUTES);
                return;
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        putInLocalCache(key, value);
    }

    /**
     * 通用方法：删除缓存
     */
    public void delete(String key) {
        if (redisAvailable) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                redisAvailable = false;
                System.out.println("Redis 连接失败");
            }
        }
        localCache.remove(key);
    }

    /**
     * 清除用户的个性化推荐缓存
     */
    public void clearPersonalizedRecommendations(Long userId) {
        String key = RECOMMENDATION_PREFIX + "user:" + userId;
        
        if (redisAvailable) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        
        localCache.remove(key);
    }

    /**
     * 清除热门推荐缓存
     */
    public void clearPopularRecommendations() {
        String key = POPULAR_PREFIX + "list";
        
        if (redisAvailable) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        
        localCache.remove(key);
    }

    /**
     * 清除所有推荐缓存
     */
    public void clearAllRecommendations() {
        clearPopularRecommendations();
        localCache.clear();
    }

    // ==================== 本地缓存实现 ====================

    private Object getFromLocalCache(String key) {
        CacheEntry entry = localCache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        localCache.remove(key);
        return null;
    }

    private void putInLocalCache(String key, Object value) {
        localCache.put(key, new CacheEntry(value, System.currentTimeMillis() + LOCAL_CACHE_EXPIRY));
    }

    /**
     * 本地缓存条目
     */
    private static class CacheEntry {
        private final Object value;
        private final long expiryTime;

        public CacheEntry(Object value, long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }

        public Object getValue() {
            return value;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}
