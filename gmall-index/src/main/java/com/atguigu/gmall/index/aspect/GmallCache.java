package com.atguigu.gmall.index.aspect;


import org.springframework.stereotype.Indexed;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {
    /*
    * 缓存key的前缀
    * 结构：模块名+':'+实例名+":"
    * ；例如:首页工程三级分类缓存
    * index:cates:
    * */
    String prefix() default "gmall:cache:";
     /*
     * 缓存的过期时间：单位为分钟
     * */
    long timeout() default 5l;
    /*
    * 防止缓存雪崩，给缓存时间添加随机值
    * 这里可以指定随机范围
    * */
    long random() default 5l;
    /*
    * 为了防止缓存击穿，给缓存区添加分布式锁
    * 这是指定分布式锁的前缀
    * 最终分布式锁名称的组成lock+方法参数
    * */
    String lock() default "lock";
}
