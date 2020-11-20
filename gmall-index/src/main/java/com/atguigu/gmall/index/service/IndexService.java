package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.aspect.GmallCache;
import com.atguigu.gmall.index.fegin.GmallPmsClient;
import com.atguigu.gmall.index.lock.DistributedLock;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;


import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {
    @Autowired
    private GmallPmsClient gmallPmsClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private DistributedLock distributedLock;
    @Autowired
    private RedissonClient redissonClient;
    public static final String KEY_PREFIX="index:category:";
    public List<CategoryEntity> queryLvl1Categories() {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.gmallPmsClient.queryCategory(0l);
        return listResponseVo.getData();
    }
    @GmallCache(prefix =KEY_PREFIX,timeout = 1296001,random = 144001,lock = "lock:cates:")
    public List<CategoryEntity> queryLvl2CategoriesWithSub(Long pid) {

            ResponseVo<List<CategoryEntity>> listResponseVo = this.gmallPmsClient.queryCategoriesWithSub(pid);

            return  listResponseVo.getData();
        }


    public void testLock() {
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.distributedLock.tryLock("lock", uuid, 30);
        if(lock){
            //获取锁成功
            String numString = this.redisTemplate.opsForValue().get("num");
            if(StringUtils.isBlank(numString)){
                return;
            }
            int num = Integer.parseInt(numString);
            this.redisTemplate.opsForValue().set("num",String.valueOf(++num));
//            try {
//                TimeUnit.SECONDS.sleep(180);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
            this.testSub("lock",uuid);
            this.distributedLock.unLock("lock",uuid);
    }
    public void testLock1() {
        //尝试获取锁
        String uuid = UUID.randomUUID().toString();
        Boolean flag = this.redisTemplate.opsForValue().setIfAbsent("lock", "uuid",3,TimeUnit.SECONDS);
        //如果获取锁失败，重试
        if (!flag) {
            try {
                Thread.sleep(50);
                testLock1();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else {
            //无法保证获取和过期时间之间的原子性
            //this.redisTemplate.expire("lock",3,TimeUnit.SECONDS);
            //获取锁成功，执行业务完释放锁
            String numString = this.redisTemplate.opsForValue().get("num");
            if(StringUtils.isBlank(numString)){
                return;
            }
            int num = Integer.parseInt(numString);
            this.redisTemplate.opsForValue().set("num",String.valueOf(++num));
            //释放锁,为了防止误删，删除之前需要判断是不是自己的锁
            //判断自己的锁和删除锁之间也要具备原子性
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            this.redisTemplate.execute(new DefaultRedisScript<>(script,Boolean.class), Arrays.asList("lock"),uuid);
//            if(StringUtils.equals(uuid,this.redisTemplate.opsForValue().get("lock"))){
//                this.redisTemplate.delete("lock");
            }

        }
    public void testSub(String lockName,String uuid){
            this.distributedLock.tryLock(lockName,uuid,30);
            System.out.println("验证可重入的分布式锁");
            this.distributedLock.unLock(lockName,uuid);
        }
    }


