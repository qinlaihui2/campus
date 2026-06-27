package com.campus.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfig {

    /** 各缓存区域的默认 TTL */
    private static final Map<String, Duration> TTL_CONFIG = new HashMap<>();

    static {
        TTL_CONFIG.put("course:list", Duration.ofMinutes(5));       // 课程列表 5 分钟
        TTL_CONFIG.put("course:detail", Duration.ofMinutes(10));    // 课程详情 10 分钟
        TTL_CONFIG.put("square:hot", Duration.ofMinutes(10));       // 热门帖子 10 分钟
        TTL_CONFIG.put("square:list", Duration.ofMinutes(3));       // 广场列表 3 分钟
        TTL_CONFIG.put("announcement:list", Duration.ofMinutes(5)); // 公告列表 5 分钟
        TTL_CONFIG.put("announcement:carousel", Duration.ofMinutes(10)); // 轮播 10 分钟
        TTL_CONFIG.put("market:list", Duration.ofMinutes(3));       // 二手列表 3 分钟
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 默认配置：1 小时 TTL
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper())))
                .disableCachingNullValues();

        // 为每个 cache name 设置不同 TTL
        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        TTL_CONFIG.forEach((name, ttl) ->
                configs.put(name, defaultConfig.entryTtl(ttl)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(configs)
                .transactionAware()
                .build();
    }

    /** 配置 ObjectMapper 以支持多态类型和 Java 8 时间 */
    private ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }
}
