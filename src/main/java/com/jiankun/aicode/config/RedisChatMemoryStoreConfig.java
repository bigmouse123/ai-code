package com.jiankun.aicode.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.community.store.memory.chat.redis.StoreType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * redis对话记忆存储配置类
 *
 * @author lijiankun
 * @since 2026/3/10
 */
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedisChatMemoryStoreConfig {

    private String host;

    private int port;

    private String password;

    private long ttl;
    
    /**
     * Redis 存储类型：STRING / JSON
     * 普通 Redis 未安装 RedisJSON 模块时请使用 STRING
     */
    private StoreType storeType = StoreType.STRING;

    @Bean
    public RedisChatMemoryStore redisChatMemoryStore() {
        return RedisChatMemoryStore.builder()
                .host(host)
                .port(port)
                .password(password)
                .ttl(ttl)
                .storeType(storeType)
                .build();
    }
}
