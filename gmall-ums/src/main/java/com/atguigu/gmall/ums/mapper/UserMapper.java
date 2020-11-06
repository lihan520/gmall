package com.atguigu.gmall.ums.mapper;

import com.atguigu.gmall.ums.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表
 * 
 * @author lihan
 * @email lihan@qq.com
 * @date 2020-10-29 11:14:59
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
	
}
