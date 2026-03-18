package org.puregxl.framework.indepence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.puregxl.framework.exception.ServiceException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Aspect
@Slf4j
@RequiredArgsConstructor
public class NoMQDuplicateConsumeAspect {
    
    private final StringRedisTemplate stringRedisTemplate;
    
    private static final String LUA_SCRIPT = """
            local key = KEYS[1]
            local value = ARGV[1]
            local expire_time_ms = ARGV[2]
            return redis.call('SET', key, value, 'NX', 'GET', 'PX', expire_time_ms)
            """;

    /**
     * 增强方法标记 {@link NoMQDuplicateConsume} 注解逻辑`
     */
    @Around("@annotation(org.puregxl.framework.indepence.NoMQDuplicateConsume)")
    public Object noMQRepeatConsume(ProceedingJoinPoint joinPoint) throws Throwable {
        NoMQDuplicateConsume noMQDuplicateConsume = getAnnotation(joinPoint);
        //先获取唯一key
        String uniqueKey = noMQDuplicateConsume.keyPrefix() + SpELUtil.parseKey(noMQDuplicateConsume.key(), ((MethodSignature) joinPoint.getSignature()).getMethod(), joinPoint.getArgs());

        String executed = stringRedisTemplate.execute(
                RedisScript.of(LUA_SCRIPT, String.class),
                List.of(uniqueKey),
                IdempotentMQConsumeStatusEnum.CONSUMING.getCode(),
                String.valueOf(TimeUnit.SECONDS.toMillis(noMQDuplicateConsume.keyTimeout()))
        );

        if (Objects.nonNull(executed)) {
            boolean errorFlag = IdempotentMQConsumeStatusEnum.isError(executed);
            log.warn("[{}] MQ repeated consumption, {}.", uniqueKey, errorFlag ? "Wait for the client to delay consumption" : "Status is completed");
            if (errorFlag) {
                throw new ServiceException(String.format("消息消费者幂等异常，幂等标识：%s", uniqueKey));
            }
            return null;
        }

        Object result;
        try {
            result = joinPoint.proceed();
            stringRedisTemplate.opsForValue().set(uniqueKey, IdempotentMQConsumeStatusEnum.CONSUMED.getCode(), noMQDuplicateConsume.keyTimeout(), TimeUnit.SECONDS);
        } catch (Throwable e) {
            //删除唯一标识key
            stringRedisTemplate.delete(uniqueKey);
            throw e;
        }

        return result;
    }


    /**
     * 获取注解的方法
     */
    public NoMQDuplicateConsume getAnnotation(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = joinPoint.getTarget().getClass().getDeclaredMethod(methodSignature.getName(), methodSignature.getMethod().getParameterTypes());
        return targetMethod.getAnnotation(NoMQDuplicateConsume.class);
    }
}
