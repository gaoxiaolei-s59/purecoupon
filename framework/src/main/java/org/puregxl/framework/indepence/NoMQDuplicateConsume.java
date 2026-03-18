package org.puregxl.framework.indepence;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NoMQDuplicateConsume {

    /**
     * 设置防重令牌 Key 前缀
     */
    String keyPrefix() default "";

    /**
     * 通过 SpEL 表达式生成的唯一 Key
     */
    String key();

    /**
     * 设置防重令牌 Key 过期时间，单位秒，默认 1 小时
     */
    long keyTimeout() default 3600L;

}
