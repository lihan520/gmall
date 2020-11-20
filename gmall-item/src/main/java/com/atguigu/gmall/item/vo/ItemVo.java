package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ItemVo {
    //三级分类
    private List<CategoryEntity> categories;
    //品牌
    private Long brandId;
    private String brandName;
    //spu
    private Long spuId;
    private String spuName;
    //sku
    private Long skuId;
    private String title;
    private String subTitle;
    private BigDecimal price;
    private Integer weight;
    private String defaultImage;
    //sku图片
    private List<SkuImagesEntity> images;
    //营销信息
    private List<ItemSaleVo> sales;
    //是否有有货
    private Boolean store=false;
    //spu下的销售属性
    //[{attrId:4,attrName:颜色,attrValues:["暗夜黑，白天白"]}]
    private List<SaleAttrValueVo> saleAttrs;
    //当前sku的销售属性
    private Map<Long,String> saleAttr;
    //sku列表的映射关系
    private String skusJson;
    // spu的海报信息
    private List<String> spuImages;
    // 规格参数组及组下的规格参数(带值)
    private List<ItemGroupVo> groups;
}
