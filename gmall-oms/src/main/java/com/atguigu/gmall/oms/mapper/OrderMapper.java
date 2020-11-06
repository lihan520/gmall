package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author lihan
 * @email lihan@qq.com
 * @date 2020-10-28 23:09:46
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
	
}
