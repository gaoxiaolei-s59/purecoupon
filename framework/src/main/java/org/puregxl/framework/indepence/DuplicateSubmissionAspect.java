package org.puregxl.framework.indepence;


import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.puregxl.framework.exception.ClientException;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@RequiredArgsConstructor
public class DuplicateSubmissionAspect {

    private final RedissonClient redissonClient;

    /**
     * 定义切面类 防止重复提交
     *
     * @param joinPoint
     * @return
     */
    @Around("@annotation(org.puregxl.framework.indepence.DuplicateSubmission)")
    public Object DuplicateSubmission(ProceedingJoinPoint joinPoint) throws Throwable {
        DuplicateSubmission duplicateSubmission = getDuplicateSubmission(joinPoint);
        RLock lock = redissonClient.getLock(String.format("no-dumplicate-submit:path:%s:userid:%s:md5:%s", getServletPath(), getCurrentUserID(), calcArgsMD5(joinPoint)));
        if (!lock.tryLock()) {
            throw new ClientException(duplicateSubmission.message());
        }
        Object result;

        try {
            //执行原来的逻辑
            result = joinPoint.proceed();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return result;
    }


    /**
     * 返回自定义注解
     */
    private static DuplicateSubmission getDuplicateSubmission(ProceedingJoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();

        // 取方法上的注解
        DuplicateSubmission annotation = AnnotationUtils.findAnnotation(method, DuplicateSubmission.class);
        return annotation;
    }


    /**
     * 获取上下文路径
     *
     * @return
     */
    private String getServletPath() {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return sra.getRequest().getServletPath();
    }

    /**
     * 返回用户id
     * @return
     */
    private String getCurrentUserID() {
        return "1810518709471555585";
    }

    /**
     * 返回自定义签名
     */
    private String calcArgsMD5(ProceedingJoinPoint joinPoint) {
        return DigestUtil.md5Hex(JSON.toJSONBytes(joinPoint.getArgs()));
    }

}
