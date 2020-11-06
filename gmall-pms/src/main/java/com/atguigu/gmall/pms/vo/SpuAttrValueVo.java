package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;


public class SpuAttrValueVo extends SpuAttrValueEntity {

        public void setValueSelected(List<Object> valueSelected){
            // 如果接受的集合为空，则不设置
            if (CollectionUtils.isEmpty(valueSelected)){
                return;
            }
            this.setAttrValue(StringUtils.join(valueSelected, "*"));
        }
    }

