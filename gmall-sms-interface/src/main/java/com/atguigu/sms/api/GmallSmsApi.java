package com.atguigu.sms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.sms.vo.ItemSaleVo;
import com.atguigu.sms.vo.SkuSaleVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface GmallSmsApi {
    @PostMapping("sms/skubounds/skusale/save")
    ResponseVo<Object> saveSkuSale(@RequestBody SkuSaleVo skuSaleVo);
    @GetMapping("sms/skubounds/sku/{skuId}")
     ResponseVo<List<ItemSaleVo>>querySaleBySkuId(@PathVariable("skuId")Long skuId);
}
