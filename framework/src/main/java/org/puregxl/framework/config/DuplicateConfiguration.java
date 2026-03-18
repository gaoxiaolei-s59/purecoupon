package org.puregxl.framework.config;


import org.puregxl.framework.indepence.DuplicateSubmissionAspect;
import org.puregxl.framework.indepence.NoMQDuplicateConsumeAspect;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;


public class DuplicateConfiguration {

    @Bean
    public DuplicateSubmissionAspect duplicateSubmissionAspect(RedissonClient redissonClient) {
        return new DuplicateSubmissionAspect(redissonClient);
    }

    @Bean
    public NoMQDuplicateConsumeAspect noMQDuplicateConsumeAspect(StringRedisTemplate stringRedisTemplate) {
        return new NoMQDuplicateConsumeAspect(stringRedisTemplate);
    }
}
