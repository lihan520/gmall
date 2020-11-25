package com.atguigu.gmall.oms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.feign.GmallPmsClient;
import com.atguigu.gmall.oms.feign.GmallUmsClient;
import com.atguigu.gmall.oms.mapper.OrderItemMapper;
import com.atguigu.gmall.oms.service.OrderItemService;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.pms.entity.SpuDescEntity;
import com.atguigu.gmall.pms.entity.SpuEntity;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.service.OrderService;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements OrderService {
@Autowired
private GmallUmsClient umsClient;
@Autowired
private OrderItemService orderItemService;
@Autowired
private GmallPmsClient pmsClient;
    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<OrderEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<OrderEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public OrderEntity saveOrder(OrderSubmitVo orderSubmitVo, Long userId) {
        //保存订单表
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setUserId(userId);
        orderEntity.setOrderSn(orderSubmitVo.getOrderToken());
        orderEntity.setCreateTime(new Date());
        ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUserById(userId);
        UserEntity userEntity = userEntityResponseVo.getData();
        if(userEntity!=null){
            orderEntity.setUsername(userEntity.getUsername());
        }
        orderEntity.setTotalAmount(orderSubmitVo.getTotalPrice());
        orderEntity.setPayAmount(orderSubmitVo.getTotalPrice());
        orderEntity.setPayType(orderSubmitVo.getPayType());
        orderEntity.setSourceType(0);
        orderEntity.setStatus(0);
        orderEntity.setDeliveryCompany(orderSubmitVo.getDeliveryCompany());
        UserAddressEntity address = orderSubmitVo.getAddress();
        if(address!=null){
            orderEntity.setReceiverAddress(address.getAddress());
            orderEntity.setReceiverRegion(address.getRegion());
            orderEntity.setReceiverProvince(address.getProvince());
            orderEntity.setReceiverPostCode(address.getPostCode());
            orderEntity.setReceiverCity(address.getCity());
            orderEntity.setReceiverPhone(address.getPhone());
            orderEntity.setReceiverName(address.getName());
        }
        orderEntity.setDeleteStatus(0);
        orderEntity.setUseIntegration(orderSubmitVo.getBounds());
        this.save(orderEntity);
        Long orderId = orderEntity.getId();
        //保存订单详情表
        List<OrderItemVo> items = orderSubmitVo.getItems();
        List<OrderItemEntity> itemEntities=items.stream().map(item->{
            OrderItemEntity orderItemEntity = new OrderItemEntity();
            orderItemEntity.setOrderId(orderId);
            orderEntity.setOrderSn(orderSubmitVo.getOrderToken());
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(item.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if(skuEntity!=null){
            orderItemEntity.setSkuId(skuEntity.getId());
            orderItemEntity.setSkuPic(skuEntity.getDefaultImage());
            orderItemEntity.setSkuPrice(skuEntity.getPrice());
            orderItemEntity.setSkuQuantity(item.getCount().intValue());
            orderItemEntity.setSkuName(skuEntity.getName());
            }
            ResponseVo<List<SkuAttrValueEntity>> saleAttrResponseVo = this.pmsClient.querySaleAttrValueBySkuId(item.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrResponseVo.getData();
            orderItemEntity.setSkuAttrsVals(JSON.toJSONString(skuAttrValueEntities));
            ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuEntityResponseVo.getData();
            if(spuEntity!=null){
                orderItemEntity.setSpuId(spuEntity.getId());
                orderItemEntity.setSpuName(spuEntity.getName());
                orderItemEntity.setCategoryId(spuEntity.getCategoryId());
            }
            ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
            if(spuDescEntity!=null){
                orderItemEntity.setSpuPic(spuDescEntity.getDecript());
            }
            //TODO: 积分 品牌

            return orderItemEntity;
        }).collect(Collectors.toList());
        this.orderItemService.saveBatch(itemEntities);
        return orderEntity;
    }

}