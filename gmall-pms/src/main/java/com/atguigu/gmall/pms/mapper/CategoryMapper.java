package com.atguigu.gmall.pms.mapper;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author lihan
 * @email lihan@qq.com
 * @date 2020-10-28 10:17:26
 */
@Mapper
public interface CategoryMapper extends BaseMapper<CategoryEntity> {
	
}
