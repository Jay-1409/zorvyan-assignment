package com.zorvyn.finance.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.zorvyn.finance.cache.CacheNames;
import com.zorvyn.finance.dto.response.DashboardSummaryResponse;
import com.zorvyn.finance.dto.response.FinancialRecordResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@Profile("!test")
public class RedisCacheConfig implements CachingConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        RedisCacheConfig.class
    );

    @Bean
    public CacheManager cacheManager(
        RedisConnectionFactory redisConnectionFactory,
        ObjectMapper objectMapper,
        @Value("${app.cache.dashboard-ttl-minutes:5}") long dashboardTtlMinutes,
        @Value(
            "${app.cache.record-by-id-ttl-minutes:2}"
        ) long recordByIdTtlMinutes
    ) {
        ObjectMapper cacheObjectMapper = objectMapper.copy();
        cacheObjectMapper.findAndRegisterModules();
        cacheObjectMapper.disable(
            SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
        );

        Jackson2JsonRedisSerializer<
            DashboardSummaryResponse
        > dashboardSerializer = new Jackson2JsonRedisSerializer<>(
            cacheObjectMapper,
            DashboardSummaryResponse.class
        );

        Jackson2JsonRedisSerializer<
            FinancialRecordResponse
        > recordByIdSerializer = new Jackson2JsonRedisSerializer<>(
            cacheObjectMapper,
            FinancialRecordResponse.class
        );

        RedisCacheConfiguration dashboardCacheConfiguration =
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(dashboardTtlMinutes))
                .disableCachingNullValues()
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(
                        dashboardSerializer
                    )
                );

        RedisCacheConfiguration recordByIdCacheConfiguration =
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(recordByIdTtlMinutes))
                .disableCachingNullValues()
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(
                        recordByIdSerializer
                    )
                );

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put(
            CacheNames.DASHBOARD_SUMMARY,
            dashboardCacheConfiguration
        );
        cacheConfigs.put(CacheNames.RECORD_BY_ID, recordByIdCacheConfiguration);

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(
                RedisCacheConfiguration.defaultCacheConfig().disableCachingNullValues()
            )
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(
                RuntimeException exception,
                Cache cache,
                Object key
            ) {
                LOGGER.warn(
                    "Cache GET error on cache={} key={}. Falling back to DB.",
                    cache.getName(),
                    key,
                    exception
                );
            }

            @Override
            public void handleCachePutError(
                RuntimeException exception,
                Cache cache,
                Object key,
                Object value
            ) {
                LOGGER.warn(
                    "Cache PUT error on cache={} key={}. Continuing without cache.",
                    cache.getName(),
                    key,
                    exception
                );
            }

            @Override
            public void handleCacheEvictError(
                RuntimeException exception,
                Cache cache,
                Object key
            ) {
                LOGGER.warn(
                    "Cache EVICT error on cache={} key={}.",
                    cache.getName(),
                    key,
                    exception
                );
            }

            @Override
            public void handleCacheClearError(
                RuntimeException exception,
                Cache cache
            ) {
                LOGGER.warn(
                    "Cache CLEAR error on cache={}.",
                    cache.getName(),
                    exception
                );
            }
        };
    }
}
