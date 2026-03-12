package org.puregxl.framework.config;


import org.puregxl.framework.indepence.DuplicateSubmissionAspect;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;


public class DuplicateConfiguration {

    @Bean
    public DuplicateSubmissionAspect duplicateSubmissionAspect(RedissonClient redissonClient) {
        return new DuplicateSubmissionAspect(redissonClient);
    }
}
