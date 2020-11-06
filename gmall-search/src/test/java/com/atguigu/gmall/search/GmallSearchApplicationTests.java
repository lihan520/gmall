package com.atguigu.gmall.search;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.fegin.GmallPmsClient;
import com.atguigu.gmall.search.fegin.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
class GmallSearchApplicationTests {
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;
@Autowired
private GoodsRepository goodsRepository;
@Autowired
private GmallWmsClient wmsClient;
@Autowired
private GmallPmsClient pmsClient;
    @Test
    void contextLoads() {
        //初始化和映射
        this.elasticsearchRestTemplate.createIndex(Goods.class);
        this.elasticsearchRestTemplate.putMapping(Goods.class);
        System.err.println("ssdsdsdsd");
        Integer pageNum=1;
        Integer pageSize=100;
        do{
            PageParamVo pageParamVo = new PageParamVo();
            pageParamVo.setPageNum(pageNum);
            pageParamVo.setPageSize(pageSize);
            //分页查询
            ResponseVo<List<SpuEntity>> listResponseVo = this.pmsClient.querySpuByPageJson(pageParamVo);
            List<SpuEntity> spuEntities= listResponseVo.getData();
            if (CollectionUtils.isEmpty(spuEntities)){
                break;
            }
            //遍历spu，查询spu下所有的sku集合，转化成goods集合saveAll
            spuEntities.forEach(spuEntity -> {
                //查询spu下的sku
                ResponseVo<List<SkuEntity>> skuResponseVo = this.pmsClient.querySkuEntitiesBySpuId(spuEntity.getId());
                List<SkuEntity> skuEntities= skuResponseVo.getData();
                if(!CollectionUtils.isEmpty(skuEntities)){
                    List<Goods> goodsList = skuEntities.stream().map(skuEntity -> {
                        Goods goods = new Goods();
                        //shu相关信息设置完成
                        goods.setSkuId(skuEntity.getId());
                        goods.setTitle(skuEntity.getTitle());
                        goods.setSubTitle(skuEntity.getSubtitle());
                        goods.setPrice(skuEntity.getPrice().doubleValue());
                        goods.setDefaultImage(skuEntity.getDefaultImage());
                        //spu中的创建时间
                        goods.setCreateTime(spuEntity.getCreateTime());
                        //查询库存相关信息并set
                        ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryStockBySkuId(skuEntity.getId());
                        List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
                        if(!CollectionUtils.isEmpty(wareSkuEntities)){
                            goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity ->
                                    wareSkuEntity.getStock()-wareSkuEntity.getStockLocked()>0
                            ));
                            goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a, b)-> a+b).get());
                        }
                        // 查询品牌
                        ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
                        BrandEntity brandEntity = brandEntityResponseVo.getData();
                         if (brandEntity!=null){
                             goods.setBrandId(brandEntity.getId());
                             goods.setBrandName(brandEntity.getName());
                             goods.setLogo(brandEntity.getLogo());
                             System.err.println("23323232323");
                         }
                         //查询分类
                        ResponseVo<CategoryEntity> categoryEntityResponseVo = this.pmsClient.queryCategoryById(skuEntity.getCatagoryId());
                        CategoryEntity categoryEntity = categoryEntityResponseVo.getData();
                        if(categoryEntity !=null){
                           goods.setCategoryId(categoryEntity.getId());
                           goods.setCategoryName(categoryEntity.getName());
                        }
                        //查询规格参数
                        List<SearchAttrValue> searchAttrValues=new ArrayList<>();
                        System.err.println("走到这了");
                        //销售类型的检索参数
                        ResponseVo<List<SkuAttrValueEntity>> skuAttrValueResponseVo = this.pmsClient.querySearchSkuAttrValueBySkuIdAndCid(skuEntity.getId(), skuEntity.getCatagoryId());
                        List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueResponseVo.getData();
                        if(!CollectionUtils.isEmpty(skuAttrValueEntities)){
                            System.err.println(skuAttrValueEntities);
                            searchAttrValues.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                                SearchAttrValue searchAttrValue = new SearchAttrValue();
                                BeanUtils.copyProperties(skuAttrValueEntity,searchAttrValue);
                                return searchAttrValue;
                            }).collect(Collectors.toList()));
                        }
                        //基本类型的检索参数
                        ResponseVo<List<SpuAttrValueEntity>> spuAttrValueResponseVo = this.pmsClient.querySpuAttrValueBySpuIdAndCid(skuEntity.getSpuId(), skuEntity.getCatagoryId());
                        List<SpuAttrValueEntity> spuAttrValueEntities = spuAttrValueResponseVo.getData();
                        if(!CollectionUtils.isEmpty(spuAttrValueEntities)){
                            searchAttrValues.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                                SearchAttrValue searchAttrValue = new SearchAttrValue();
                                BeanUtils.copyProperties(spuAttrValueEntity,searchAttrValue);
                                return searchAttrValue;
                            }).collect(Collectors.toList()));
                        }
                        goods.setSearchAttrs(searchAttrValues);
                        return goods;
                    }).collect(Collectors.toList());
                    //导入索引库
                    this.goodsRepository.saveAll(goodsList);
                }
            });
            pageSize=spuEntities.size();
            pageNum++;
        }while (pageNum==100);
    }

}
