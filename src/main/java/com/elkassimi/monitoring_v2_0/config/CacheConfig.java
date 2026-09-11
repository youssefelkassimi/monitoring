package com.elkassimi.monitoring_v2_0.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;

/**
 * Redis-backed cache manager for @Cacheable/@CacheEvict (see AgentService).
 *
 * GenericJackson2JsonRedisSerializer (Jackson 2-based) is deprecated since
 * Spring Data Redis 4.0, for removal, in favor of GenericJacksonJsonRedisSerializer
 * (Jackson 3-based, package tools.jackson.databind.*). This uses that one.
 *
 * Two things that trip people up moving to this API:
 *  - It's built via GenericJacksonJsonRedisSerializer.builder(), not `new`.
 *  - Default typing (the "@class" hint that lets a cached List<Agent> come
 *    back as the right type) now needs an explicit PolymorphicTypeValidator
 *    scoped to your own base package - enableUnsafeDefaultTyping() exists
 *    but is explicitly flagged by Spring as an RCE risk on untrusted input,
 *    so this uses the scoped/validated variant instead.
 *
 * No JavaTimeModule registration needed for the Instant fields on Agent -
 * java.time support has been built into jackson-databind directly since
 * Jackson 3.0.0-rc3, nothing to add.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.elkassimi.monitoring_v2_0")
                .allowIfSubType("java.util")
                .build();

        GenericJacksonJsonRedisSerializer jsonSerializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .build();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(60))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                // per-cache override example - shorter TTL for agent lookups
                // as a staleness backstop alongside the explicit @CacheEvicts.
                .withCacheConfiguration("agents", defaultConfig.entryTtl(Duration.ofSeconds(30)))
                .withCacheConfiguration("users", defaultConfig.entryTtl(Duration.ofSeconds(30)))
                .build();
    }
}
