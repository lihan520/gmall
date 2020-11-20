package com.atguigu.gmall.pms.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class SkuAttrValueMapperTest {
@Autowired
private SkuAttrValueMapper skuAttrValueMapper;
    @Test
    void querySkusJsonBySpuId() {
        this.skuAttrValueMapper.querySkusJsonBySpuId(2l);
    }
}