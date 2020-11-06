package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

@Data
public class SearchParamVo {
    //搜索关键字
    private String keyword;
    //品牌过滤
    private List<Long> brandId;
    //分类的过滤条件
    private List<Long> cid;
    //规格参数的过滤
    private List<String> props;
    //排序字段
    private Integer sort;
    //价格区间过滤
    private Double priceTo;
    private Double priceFrom;
    //库存
    private Boolean store;
    //页码
    private Integer pageNum=1;//默认第一页
    //每页大小
    private final Integer pageSize=10;
}
