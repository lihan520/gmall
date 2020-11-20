package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import com.atguigu.gmall.pms.service.SkuImagesService;
import com.atguigu.gmall.pms.service.SpuAttrValueService;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.sms.vo.SkuSaleVo;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import com.atguigu.gmall.pms.service.SpuService;
import org.springframework.util.CollectionUtils;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySpu(PageParamVo pageParamVo, Long categoryId) {
        //封装查询条件
        QueryWrapper<SpuEntity> queryWrapper = new QueryWrapper<>();
        //如果分类id不为0，要根据分类id查，否则查全部
        if (categoryId!=0){
            queryWrapper.eq("category_id",categoryId);
        }
        //如果用户输入了检索条件，根据检索条件查询
        String key = pageParamVo.getKey();
        if(StringUtils.isNotBlank(key)){
            queryWrapper.and(t->t.like("name",key).or().like("id",key));
        }
        return new PageResultVo(this.page(pageParamVo.getPage(),queryWrapper));
    }
    @Autowired
    private SpuDescMapper spuDescMapper;
    @Autowired
    private SpuAttrValueService spuAttrValueService;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private SkuImagesService skuImagesService;
    @Autowired
    private SkuAttrValueService skuAttrValueService;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @GlobalTransactional
    @Override
    public void bigSave(SpuVo spu) {

        //1保存spu相关
        // 1.1 保存spu基本信息

        Long spuId =  saveSpu(spu);//获取新增后的spuId;

        //1.2. 保存spu的描述信息 spu_desc
        saveSpuDesc(spu, spuId);

        //1.3 保存spu的规格参数信息
        saveBaseAttr(spu, spuId);
        //2. 保存sku相关信息
        saveSku(spu, spuId);
        //发送消息
        this.rabbitTemplate.convertAndSend("PMS_SPU_EXCHANGE","item.insert",spuId);
        }


        //最后制造异常
//        int i=1/0;
        private void saveSku(SpuVo spu,Long spuId){
            List<SkuVo> skuVos = spu.getSkus();
            if(CollectionUtils.isEmpty(skuVos)){
                return;
            }
            skuVos.forEach(skuVo -> {
                // 2.1 保存sku基本信息
                SkuEntity skuEntity = new SkuEntity();
                BeanUtils.copyProperties(skuVo,skuEntity);
                // 品牌和分类的id需要从spu中获取
                skuEntity.setBrandId(spu.getBrandId());
                skuEntity.setCatagoryId(spu.getCategoryId());

                //获取图片列表
                List<String> images = skuVo.getImages();
                // 如果图片列表不为null，则设置默认图片
                if(!CollectionUtils.isEmpty(images)){
                    // 设置第一张图片作为默认图片
                    skuEntity.setDefaultImage(skuEntity.getDefaultImage()==null ? images.get(0) : skuEntity.getDefaultImage());
                }
                skuEntity.setSpuId(spuId);
                this.skuMapper.insert(skuEntity);
                //获取skuId
                Long skuId = skuEntity.getId();

                //2.2 保存图片信息
                if(!CollectionUtils.isEmpty(images)){
                    String defaultImage=images.get(0);
                    List<SkuImagesEntity> skuImageses = images.stream().map(image -> {
                        SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                        skuImagesEntity.setDefaultStatus(StringUtils.equals(defaultImage, image) ? 1 : 0);
                        skuImagesEntity.setSkuId(skuId);
                        skuImagesEntity.setSort(0);
                        skuImagesEntity.setUrl(image);
                        return skuImagesEntity;
                    }).collect(Collectors.toList());
                    this.skuImagesService.saveBatch(skuImageses);
                }

                //2.3 保存sku的规格参数(销售属性)
                List<SkuAttrValueEntity> saleAttrs = skuVo.getSaleAttrs();
                saleAttrs.forEach(saleAttr->{
                    // 设置属性名，需要根据id查询AttrEntity
                    saleAttr.setSort(0);
                    saleAttr.setSkuId(skuId);
                });
                this.skuAttrValueService.saveBatch(saleAttrs);

                //3.保存营销相关信息，需要远程调用gmall-sms
                SkuSaleVo skuSaleVo = new SkuSaleVo();
                BeanUtils.copyProperties(skuVo,skuSaleVo);
                skuSaleVo.setSkuId(skuId);
                this.smsClient.saveSkuSale(skuSaleVo);
            });
        }
        private void saveBaseAttr(SpuVo spu,Long spuId){
            List<SpuAttrValueVo> baseAttrs = spu.getBaseAttrs();
            if(!CollectionUtils.isEmpty(baseAttrs)){
                List<SpuAttrValueEntity> spuAttrValueEntities=baseAttrs.stream().map(spuAttrValueVo -> {
                    SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                    BeanUtils.copyProperties(spuAttrValueVo,spuAttrValueEntity);
                    spuAttrValueEntity.setSort(0);
                    spuAttrValueEntity.setSpuId(spuId);
                    return spuAttrValueEntity;
                }).collect(Collectors.toList());
                this.spuAttrValueService.saveBatch(spuAttrValueEntities);
            }
        }
        private void saveSpuDesc(SpuVo spu,Long spuId){
            SpuDescEntity spuDescEntity = new SpuDescEntity();
            // 注意：spu_desc表的主键是spu_id,需要在实体类中配置该主键不是自增主键
            spuDescEntity.setSpuId(spuId);
            // // 把商品的图片描述，保存到spu详情中，图片地址以逗号进行分割
            spuDescEntity.setDecript(StringUtils.join(spu.getSpuImages(),","));
            this.spuDescMapper.insert(spuDescEntity);
        }
        private Long saveSpu(SpuVo spu){
            spu.setPublishStatus(1); // 默认是已上架
            spu.setCreateTime(new Date());
            spu.setUpdateTime(spu.getCreateTime()); // 新增时，更新时间和创建时间一致
            this.save(spu);
            return spu.getId();
        }
    }

