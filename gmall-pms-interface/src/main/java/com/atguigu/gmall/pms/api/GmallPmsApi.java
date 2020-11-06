package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GmallPmsApi {
    @PostMapping("pms/spu/json")
     ResponseVo<List<SpuEntity>> querySpuByPageJson(@RequestBody PageParamVo pageParamVo);
    @GetMapping("pms/sku/spu/{spuId}")
    ResponseVo<List<SkuEntity>> querySkuEntitiesBySpuId(@PathVariable("spuId") Long spuId);
    @GetMapping("pms/brand/{id}")
    ResponseVo<BrandEntity> queryBrandById(@PathVariable("id") Long id);
    @GetMapping("pms/category/{id}")
     ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id);
    @GetMapping("pms/skuattrvalue/search/attr/{skuId}")
     ResponseVo<List<SkuAttrValueEntity>> querySearchSkuAttrValueBySkuIdAndCid(
            @PathVariable("skuId") Long skuId,
            @RequestParam("cid") Long cid
    );
    @GetMapping("pms/spuattrvalue/search/attr/{spuId}")
    public ResponseVo<List<SpuAttrValueEntity>> querySpuAttrValueBySpuIdAndCid(
            @PathVariable("spuId") Long spuId,
            @RequestParam("cid") Long cid);
}
