package com.shopsphere.product;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;
import java.time.Duration;
@Configuration
public class CacheConfig {
  @Bean RedisCacheManager cacheManager(RedisConnectionFactory cf) {
    RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new JdkSerializationRedisSerializer(getClass().getClassLoader())));
    return RedisCacheManager.builder(cf).cacheDefaults(base.entryTtl(Duration.ofMinutes(10)))
        .withCacheConfiguration("product", base.entryTtl(Duration.ofMinutes(30)))
        .withCacheConfiguration("productList", base.entryTtl(Duration.ofMinutes(2))) // short TTL: lists go stale fastest
        .build();
  }
}
