package com.dama.wanderwave.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Slf4j
@Configuration
@Profile("dev")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisConfig {

	@Value("${spring.data.redis.host}")
	private String host;

	@Value("${spring.data.redis.port}")
	private Integer port;

	@Bean
	@Primary
	public LettuceConnectionFactory redisUsersConnectionFactory() {
		log.info("Creating Redis connection factory for users with host: {}, port: {}, database: 0", host, port);
		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
		config.setHostName(host);
		config.setPort(port);
		config.setDatabase(0);
		return new LettuceConnectionFactory(config);
	}

	@Bean
	public LettuceConnectionFactory redisPostsConnectionFactory() {
		log.info("Creating Redis connection factory for posts with host: {}, port: {}, database: 1", host, port);
		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
		config.setHostName(host);
		config.setPort(port);
		config.setDatabase(1);
		return new LettuceConnectionFactory(config);
	}

	@Bean
	public LettuceConnectionFactory redisFlowConnectionFactory() {
		log.info("Creating Redis connection factory for flow with host: {}, port: {}, database: 2", host, port);
		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
		config.setHostName(host);
		config.setPort(port);
		config.setDatabase(2);
		return new LettuceConnectionFactory(config);
	}

	@Bean(name = "redisUsersTemplate")
	public RedisTemplate<String, PostCache> redisUsersTemplate() {
		log.info("Creating Redis template for users");
		RedisTemplate<String, PostCache> template = new RedisTemplate<>();
		template.setConnectionFactory(redisUsersConnectionFactory());
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new Jackson2JsonRedisSerializer<>(PostCache.class));
		return template;
	}

	@Bean(name = "redisPostsTemplate")
	public RedisTemplate<String, UserCache> redisPostsTemplate() {
		log.info("Creating Redis template for posts");
		RedisTemplate<String, UserCache> template = new RedisTemplate<>();
		template.setConnectionFactory(redisPostsConnectionFactory());
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new Jackson2JsonRedisSerializer<>(UserCache.class));
		return template;
	}

	@Bean(name = "redisFlowTemplate")
	public RedisTemplate<String, Long> redisFlowTemplate() {
		log.info("Creating Redis template for flow");
		RedisTemplate<String, Long> template = new RedisTemplate<>();
		template.setConnectionFactory(redisFlowConnectionFactory());
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new LongRedisSerializer());
		return template;
	}
}