package com.atguigu.gmall.sms.service;

import com.atguigu.sms.vo.SkuSaleVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;

/**
 * 商品spu积分设置
 *
 * @author lihan
 * @email lihan@qq.com
 * @date 2020-10-28 22:51:43
 */
public interface SkuBoundsService extends IService<SkuBoundsEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    void saveSkuSale(SkuSaleVo skuSaleVo);
}

