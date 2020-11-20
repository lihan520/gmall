package com.atguigu.gmall.cart.config;

import com.atguigu.gmall.cart.exception.CartUncaughtExceptionHandler;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig implements AsyncConfigurer {
    @Autowired
    private CartUncaughtExceptionHandler exceptionHandler;
    @Override
    public Executor getAsyncExecutor() {
        return null;
    }
    /*
    * 配置异常处理器
    * */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return exceptionHandler;
    }
}
