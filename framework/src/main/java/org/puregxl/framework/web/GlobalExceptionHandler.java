package org.puregxl.framework.web;

import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.puregxl.framework.exception.AbstractException;
import org.puregxl.framework.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = AbstractException.class)
    public Result abstractException (HttpServletRequest request, AbstractException ex) {
        log.error("[{}] {} [ex] {}", request.getMethod(), request.getRequestURL().toString(), ex, ex.getCause());
        return Results.failure(ex);
    }

    /**
     * 拦截未捕获异常
     */
    @ExceptionHandler(value = Throwable.class)
    public Result defaultErrorHandler(HttpServletRequest request, Throwable throwable) {
        log.error("[{}] {} ", request.getMethod(), getUrl(request), throwable);
        return Results.failure();
    }

    /**
     * 获取请求完整url
     * @param request
     * @return
     */
    private String getUrl(HttpServletRequest request) {
        if (StringUtils.isEmpty(request.getRequestURL().toString())) {
            return request.getRequestURI();
        }
        return request.getRequestURL().toString() + "?" + request.getQueryString();
    }

}
