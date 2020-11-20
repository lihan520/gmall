package com.atguigu.gmall.index.aspect;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class GmallCacheAspect {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Around("@annotation(GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint)throws Throwable{
        //获取切点方法的签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //获取方法对象
        Method method = signature.getMethod();
        //获取方法上指定注解的对象
        GmallCache annotation = method.getAnnotation(GmallCache.class);
        //获取注解中的前缀
        String prefix = annotation.prefix();
        //获取方法的参数
        Object[] args = joinPoint.getArgs();
        String param= Arrays.asList(args).toString();
        //获取方法的返回值类型
        Class<?> returnType = method.getReturnType();
        //拦截代码块：判断缓存中有没有
        String json = (String) this.redisTemplate.opsForValue().get(prefix + param);
        //判断缓存中的数据是否为空
        if(StringUtils.isNotBlank(json)){
            return JSON.parseObject(json,returnType);
        }
        //没有，加分布式锁
        String lock=annotation.lock();
        RLock rLock = this.redissonClient.getLock(lock + param);
        rLock.lock();
        Object result;
        try {
            //判断缓存中有没有，有直接返回(加锁的过程中，别的请求可能已经把数据放入了缓存)
            String json2 = (String) this.redisTemplate.opsForValue().get(prefix + param);
            //判断缓存的数据是否为空
            if(StringUtils.isNotBlank(json2)){
                return JSON.parseObject(json2,returnType);
            }
            //执行目标方法
           result = joinPoint.proceed(joinPoint.getArgs());
            //拦截代码块：放入缓存 释放分布锁
            long timeout = annotation.timeout();
            long random = annotation.random();
            this.redisTemplate.opsForValue().set(prefix+param,JSON.toJSONString(result),timeout + new Random().nextInt((int) random), TimeUnit.MINUTES);
        } finally {
            rLock.unlock();
        }
        return result;
    }
}
