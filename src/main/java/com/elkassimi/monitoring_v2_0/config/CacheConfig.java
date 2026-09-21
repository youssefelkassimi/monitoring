package com.elkassimi.monitoring_v2_0.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

@Configuration
public class CacheConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Use GenericJacksonJsonRedisSerializer for values (Jackson 3)
        GenericJacksonJsonRedisSerializer serializer =
                GenericJacksonJsonRedisSerializer.create(builder -> builder
                        .enableDefaultTyping(createPolymorphicTypeValidator())
                        .typePropertyName("@class"));

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    private PolymorphicTypeValidator createPolymorphicTypeValidator() {
        return BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
    }
}