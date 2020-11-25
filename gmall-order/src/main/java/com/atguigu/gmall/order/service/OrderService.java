package com.atguigu.gmall.order.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.vo.OrderConfirmVo;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.atguigu.sms.vo.ItemSaleVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderService {
    @Autowired
    private GmallCartClient cartClient;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallUmsClient umsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    @Autowired
    private GmallOmsClient omsClient;
    private static final String KEY_PREFIX="order:token";
    public OrderConfirmVo confirm() {
    OrderConfirmVo confirmVo = new OrderConfirmVo();
            //先获取登录信息
            UserInfo userInfo = LoginInterceptor.getUserInfo();
          Long userId = userInfo.getUserId();
        // 获取用户选中的购物车信息
        CompletableFuture<Void> cartCompletableFuture =CompletableFuture.runAsync(() -> {
            ResponseVo<List<Cart>> cartsResponseVo = this.cartClient.queryCheckedCartsByUserId(userId);
            List<Cart> carts = cartsResponseVo.getData();
            //判断是否为空
            if (CollectionUtils.isEmpty(carts)) {
                throw new OrderException("你没有选中的购物车记录");
            }
            //把购物车记录转化成订单详情
            List<OrderItemVo> itemVos = carts.stream().map(cart -> {
                OrderItemVo itemVo = new OrderItemVo();
                itemVo.setSkuId(cart.getSkuId());
                itemVo.setCount(cart.getCount());
                ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
                SkuEntity skuEntity = skuEntityResponseVo.getData();
                if (skuEntity != null) {
                    itemVo.setTitle(skuEntity.getTitle());
                    itemVo.setDefaultImage(skuEntity.getDefaultImage());
                    itemVo.setWeight(skuEntity.getWeight());
                    itemVo.setPrice(skuEntity.getPrice());
                }
                ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryStockBySkuId(cart.getSkuId());
                List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
                if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                    itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                }
                ResponseVo<List<SkuAttrValueEntity>> saleAttrResponseVo = this.pmsClient.querySaleAttrValueBySkuId(cart.getSkuId());
                List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrResponseVo.getData();
                itemVo.setSaleAttrs(skuAttrValueEntities);
                ResponseVo<List<ItemSaleVo>> saleResponseVo = this.smsClient.querySaleBySkuId(cart.getSkuId());
                List<ItemSaleVo> itemSaleVos = saleResponseVo.getData();
                itemVo.setSales(itemSaleVos);
                return itemVo;
            }).collect(Collectors.toList());
            confirmVo.setOrderItems(itemVos);
        }, threadPoolExecutor);

        //获取用户的收货地址
        CompletableFuture<Void> addressCompletableFuture = CompletableFuture.runAsync(()-> {
            ResponseVo<List<UserAddressEntity>> addressesResponseVo = this.umsClient.queryAddressesByUserId(userId);
            List<UserAddressEntity> addressEntities = addressesResponseVo.getData();
            confirmVo.setAddresses(addressEntities);
        },threadPoolExecutor);

        //根据用户id查询用户信息（购买信息）
        CompletableFuture<Void> umsCompletableFuture = CompletableFuture.runAsync(()-> {
            ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUserById(userId);
            UserEntity userEntity = userEntityResponseVo.getData();
            if (userEntity != null) {
                confirmVo.setBounds(userEntity.getIntegration());
            }
        }, threadPoolExecutor);
        //生成OrderToken
        CompletableFuture<Void> tokenCompletableFuture = CompletableFuture.runAsync(() -> {
            String orderToken = IdWorker.getTimeId();
            confirmVo.setOrderToken(orderToken);
            this.redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, "11",24, TimeUnit.HOURS);
        }, threadPoolExecutor);
        CompletableFuture.allOf(cartCompletableFuture,addressCompletableFuture,umsCompletableFuture,tokenCompletableFuture).join();
        return confirmVo;
    }

    public String submit(OrderSubmitVo orderSubmitVo) {
        //1、防重
        String orderToken = orderSubmitVo.getOrderToken();
        if(StringUtils.isBlank(orderToken)){
            throw new RuntimeException("请求不合法！");
        }
        String script = "if (redis.call('exists', KEYS[1])==1)  then return redis.call('del', KEYS[1]) else return 0 end";
        Boolean flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(KEY_PREFIX+orderToken), "");
        if(!flag){
            throw new RuntimeException("页面已过期或者您也提交");
        }
        //2、验总价
        List<OrderItemVo> items = orderSubmitVo.getItems();
        if(CollectionUtils.isEmpty(items)){
          throw new OrderException("您没有选中的购物车记录！");
        }
        BigDecimal currentBigDecimal = items.stream().map(item -> {
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(item.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null) {
                return skuEntity.getPrice().multiply(item.getCount());
            }
            return new BigDecimal(0);
        }).reduce((a, b) -> a.add(b)).get();
        BigDecimal totalPrice = orderSubmitVo.getTotalPrice();//页面价格
        if(totalPrice.compareTo(currentBigDecimal)!=0){
            throw new OrderException("页面也过期，请刷新后重试");
        }
        //3、验库存并锁库存
        List<SkuLockVo> lockVos = items.stream().map(item -> {
            SkuLockVo lockVo = new SkuLockVo();
            lockVo.setSkuId(item.getSkuId());
            lockVo.setCount(item.getCount().intValue());
            return lockVo;
        }).collect(Collectors.toList());
        ResponseVo<List<SkuLockVo>> lockResponseVo = this.wmsClient.checkAndLock(lockVos, orderToken);
        List<SkuLockVo> skuLockVos = lockResponseVo.getData();
        if(!CollectionUtils.isEmpty(skuLockVos)){
            throw new OrderException(JSON.toJSONString(skuLockVos));
        }
        //4、创建订单
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        try {
            this.omsClient.saveOrder(orderSubmitVo, userId);
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: 异步标记为无效订单
        }
        //5、异步删除购物车

        return null;
    }
}
