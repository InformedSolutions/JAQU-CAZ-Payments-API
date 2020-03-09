package uk.gov.caz.psr.configuration;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
@Configuration
@EnableCaching
@ConditionalOnProperty(value = "redis.enabled", havingValue = "true",
    matchIfMissing = false)
public class RedisConfiguration {

  @Value("${redis.endpoint}")
  private String redisClusterEndpoint;

  @Value("${redis.port}")
  private Integer redisClusterPort;

  @Value("${redis.ttlInHours}")
  private Integer redisTtl;

  /**
   * Customised redis template bean constructor.
   *
   * @param lettuceConnectionFactory a lettuce connection factory instance.
   * @return A customised redis template.
   */
  @Bean(name = "redisTemplate")
  public RedisTemplate<String, Object> redisTemplate(
      LettuceConnectionFactory lettuceConnectionFactory) {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(lettuceConnectionFactory);
    return redisTemplate;
  }

  /**
   * Custom bean for applying a TTL to redis values.
   *
   * @param redisConnectionFactory a configured redis connection factory.
   * @return A customised cache manager instance with a ttl applied
   */
  @Primary
  @Bean
  public CacheManager cacheManager(
      RedisConnectionFactory redisConnectionFactory) {
    Duration expiration = Duration.ofHours(redisTtl);
    return RedisCacheManager.builder(redisConnectionFactory)
        .cacheDefaults(
            RedisCacheConfiguration.defaultCacheConfig().entryTtl(expiration))
        .build();
  }

  /**
   * Customised lettuce connection factory builder.
   *
   * @return A lettuce connection factory instance.
   */
  @Bean
  @Profile("!integration-tests")
  public LettuceConnectionFactory lettuceConnectionFactory() {
    log.info("Creating redis-connection for a cluster");
    RedisClusterConfiguration clusterConfiguration =
        new RedisClusterConfiguration();
    clusterConfiguration.clusterNode(redisClusterEndpoint, redisClusterPort);
    return new LettuceConnectionFactory(clusterConfiguration);
  }

  /**
   * Customised lettuce connection factory for a single node redis instance.
   *
   * @return A lettuce connection factory instance.
   */
  @Bean
  @Profile("integration-tests")
  public LettuceConnectionFactory lettuceConnectionFactoryForSingleInstance() {
    log.info("Creating redis-connection for a single node instance");
    return new LettuceConnectionFactory(redisClusterEndpoint, redisClusterPort);
  }
}
