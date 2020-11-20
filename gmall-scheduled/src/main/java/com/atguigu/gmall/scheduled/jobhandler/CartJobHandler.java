package com.atguigu.gmall.scheduled.jobhandler;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.scheduled.mapper.CartMapper;
import com.atguigu.gmall.scheduled.pojo.Cart;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.apache.commons.lang3.StringUtils;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
public class CartJobHandler {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private CartMapper cartMapper;
    private static final String EXCEPTION_KEY="cart:exception";
    private static final String KEY_PREFIX="cart:info:";
    @XxlJob("cartJobHandler")
    public ReturnT<String> handler (String param){
        BoundSetOperations<String, String> setOps = this.redisTemplate.boundSetOps(EXCEPTION_KEY);
        String userId = setOps.pop();
        //取空为止
        while (StringUtils.isNotBlank(userId)){
            //先清空该用户数据所有的记录
            cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id",userId));
            //再读取出redis中该用户的购物车记录
            BoundHashOperations<String, Object, Object> hashOperations = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
            List<Object> cartJsons = hashOperations.values();
            if(!CollectionUtils.isEmpty(cartJsons)){
               //添加到mysql
                cartJsons.forEach(cartJson->{
                    Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                    this.cartMapper.insert(cart);
                });
            }
            //取下个
            userId=setOps.pop();
        }
        return ReturnT.SUCCESS;
    }
}
