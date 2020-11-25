package com.atguigu.gmall.wms.mapper;

import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 * 
 * @author lihan
 * @email lihan@qq.com
 * @date 2020-10-29 11:20:22
 */
@Mapper
public interface WareSkuMapper extends BaseMapper<WareSkuEntity> {
	 List<WareSkuEntity> checkLock(@Param("skuId") Long skuId, @Param("count") Integer count);
	 int lock(@Param("id") Long id,@Param("count") Integer count);
	 int unlock(@Param("id") Long id,@Param("count") Integer count);


}
