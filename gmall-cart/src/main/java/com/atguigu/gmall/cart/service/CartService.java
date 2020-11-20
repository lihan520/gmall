package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.CartException;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.sms.vo.ItemSaleVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private CartAsyncService asyncService;
    private static final String KEY_PREFIX="cart:info:";
    private static final String PRICE_PREFIX="cart:price:";
    public void addCart(Cart cart) {
        String userId = this.getUserId();
        //通过外层的key获取内层的map
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        String skuId = cart.getSkuId().toString();
        BigDecimal count = cart.getCount();
        //判断该用户的购物车是否包含当前这条商品
        if(hashOps.hasKey(skuId)){
            //包含，则更新数量
            String cartJson = hashOps.get(skuId).toString();
            cart=JSON.parseObject(cartJson,Cart.class);
            cart.setCount(cart.getCount().add(count));
            //写redis，写mysql
            this.asyncService.updateCart(userId,cart);
        }else {
            //不包含，则新增一条记录
            cart.setUserId(userId);
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if(skuEntity==null){
                return;
            }
            cart.setTitle(skuEntity.getTitle());
            cart.setPrice(skuEntity.getPrice());
            cart.setDefaultImage(skuEntity.getDefaultImage());
            //查询库存信息
            ResponseVo<List<WareSkuEntity>> listResponseVo = this.wmsClient.queryStockBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = listResponseVo.getData();
            if(!CollectionUtils.isEmpty(wareSkuEntities)){
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity ->
                        wareSkuEntity.getStock()-wareSkuEntity.getStockLocked()>0
                ));
            }
            //销售属性
            ResponseVo<List<SkuAttrValueEntity>> listResponseVo1 = this.pmsClient.querySaleAttrValueBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = listResponseVo1.getData();
            cart.setSaleAttrs(JSON.toJSONString(skuAttrValueEntities));
            //营销信息
            ResponseVo<List<ItemSaleVo>> listResponseVo2 = this.smsClient.querySaleBySkuId(cart.getSkuId());
            List<ItemSaleVo> saleVos = listResponseVo2.getData();
            cart.setSales(JSON.toJSONString(saleVos));
            cart.setCheck(true);
            this.asyncService.insertCart(userId,cart);
            //添加价格缓存
            redisTemplate.opsForValue().set(PRICE_PREFIX+skuId,skuEntity.getPrice().toString());
        }
        hashOps.put(skuId,JSON.toJSONString(cart));

    }

    private String getUserId() {
        //获取登录信息，如果userId不为空，就以userId作为key，如果userId为空，就以userKey作为key
        UserInfo userInfo = LoginInterceptor.getUserInfo();

        if(userInfo.getUserId()==null){
          return userInfo.getUserKey();
        }
        return userInfo.getUserId().toString();
    }

    public Cart queryCart(Long skuId) {
        String userId=getUserId();
        //获取内层的map
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        if(hashOps.hasKey(skuId.toString())){
            String cartJson = hashOps.get(skuId.toString()).toString();
            return JSON.parseObject(cartJson,Cart.class);
        }
        throw new CartException("此用户不存在这条购物记录");
    }
    @Async
    public String executor1(){
        try {
            System.out.println("executor1方法开始执行。。。。");
            TimeUnit.SECONDS.sleep(4);
            int i=1/0;
            System.out.println("executor1方法结束执行");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
    @Async
    public ListenableFuture<String> executor2(){
        try {
            System.out.println("executor2方法开始执行。。。。");
            TimeUnit.SECONDS.sleep(5);
            System.out.println("executor2方法结束执行");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return  AsyncResult.forValue("hello executor2");
    }

    public List<Cart> queryCarts() {
        //1、获取userKey
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();
        //2、查询未登录状态的购物车
        BoundHashOperations<String, Object, Object> unLoginHashOps = redisTemplate.boundHashOps(KEY_PREFIX + userKey);
        List<Object> unLoginCartJsons = unLoginHashOps.values();
        List<Cart> unLoginCarts=null;
        if(!CollectionUtils.isEmpty(unLoginCartJsons)){
             unLoginCarts = unLoginCartJsons.stream().map(cartJson ->{
                 Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                 cart.setCurrentPrice(new BigDecimal(redisTemplate.opsForValue().get(PRICE_PREFIX+cart.getSkuId())));
                 return cart;
             }).collect(Collectors.toList());

        }
        //3、获取userId，并判断userId是否为空，为空则直接返回未登录的购物车
        Long userId = userInfo.getUserId();
        if(userId==null){
            return unLoginCarts;
        }
        //4、获取登录状态的购物车内存map
        BoundHashOperations<String, Object, Object> LoginHashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        //5、把未登录的购物车合并到登录状态的购物车内层map中
        if(!CollectionUtils.isEmpty(unLoginCarts)){
            unLoginCarts.forEach(cart->{
                String skuId=cart.getSkuId().toString();
                BigDecimal count = cart.getCount();
                if(LoginHashOps.hasKey(skuId)){
                    //登录状态的购物车中包含该记录，更新数量
                    String cartJson = LoginHashOps.get(skuId).toString();
                    cart= JSON.parseObject(cartJson, Cart.class);
                    cart.setCount(cart.getCount().add(count));
                   //更新到数据库
                    this.asyncService.updateCart(userId.toString(),cart);
                }else {
                    //登录状态的购物车不包含该条记录，新增一条记录
                    cart.setUserId(userId.toString());
                    this.asyncService.insertCart(userId.toString(),cart);
                }
                //更新到redis
                LoginHashOps.put(skuId,JSON.toJSONString(cart));

            });
            //6、删除未登录的购物车
            redisTemplate.delete(KEY_PREFIX+userKey);
            asyncService.deleteCart(userKey);
        }
        //7、查询登录状态的购物车
        List<Object> loginCartJsons = LoginHashOps.values();
        if(!CollectionUtils.isEmpty(loginCartJsons)){
            return loginCartJsons.stream().map(cartJson->{
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                cart.setCurrentPrice(new BigDecimal(redisTemplate.opsForValue().get(PRICE_PREFIX+cart.getSkuId())));
                return cart;
            }).collect(Collectors.toList());
        }
        return null;
    }

    public void updateNum(Cart cart) {
        String userId = getUserId();
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        if(hashOps.hasKey(cart.getSkuId().toString())){
            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            BigDecimal count = cart.getCount();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(count);
            hashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
            this.asyncService.updateCart(userId,cart);
            return;
        }
        throw new CartException("该用户的购物车不包含该条记录");
    }

    public void updateStatus(Cart cart) {
        String userId = getUserId();
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        if(hashOps.hasKey(cart.getSkuId().toString())){
            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            Boolean check = cart.getCheck();
            cart=JSON.parseObject(cartJson,Cart.class);
            cart.setCheck(check);
            hashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
            this.asyncService.updateCart(userId,cart);
            return;
        }
        throw new CartException("该用户的购物车不包含该条记录");
    }

    public void deleteCart(Long skuId) {
        String userId = getUserId();
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        if(hashOps.hasKey(skuId.toString())){
            hashOps.delete(skuId.toString());
            this.asyncService.deleteCartByUserIdAndSkuId(userId,skuId);
            return;
        }
        throw new CartException("该用户的购物车不包含该条记录");
    }
}
