package org.puregxl.framework.config;

import org.puregxl.framework.web.GlobalExceptionHandler;
import org.springframework.context.annotation.Bean;

public class WebAutoConfiguration {

    /**
     * 全局异常自动装配
     * @return
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler(){
        return new GlobalExceptionHandler();
    }
}
