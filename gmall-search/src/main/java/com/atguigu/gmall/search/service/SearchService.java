package com.atguigu.gmall.search.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchService {
    @Autowired
    private RestHighLevelClient restHighLevelClient;
    private static final ObjectMapper MAPPER=new ObjectMapper();
    public SearchResponseVo search(SearchParamVo paramVo) {
        try {
            SearchRequest searchRequest = new SearchRequest(new String[]{"goods"}, buildDsl(paramVo));
            SearchResponse response = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            System.err.println(response);
            SearchResponseVo responseVo = this.parseSearchResult(response);
            //分页参数在请求的参数中
            responseVo.setPageNum(paramVo.getPageNum());
            responseVo.setPageSize(paramVo.getPageSize());
            return  responseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private SearchResponseVo parseSearchResult(SearchResponse response){
        SearchResponseVo responseVo = new SearchResponseVo();
        //解析hits命中对象
        SearchHits hits = response.getHits();
        // 总命中的记录数
        responseVo.setTotal(hits.getTotalHits());
        SearchHit[] hitsHits = hits.getHits();
        if(hitsHits==null&&hitsHits.length==0){
            // TODO: 打广告
            return null;
        }
        //需要把SearchHit[]数组转化成goodsList集合
        List<Goods> goodsList = Arrays.stream(hitsHits).map(hitsHit -> {
            try {
                String json = hitsHit.getSourceAsString();
                //Goods goods = JSON.parseObject(json, Goods.class);
                Goods goods = MAPPER.readValue(json, Goods.class);
                //获取高亮结果集 设置给goods对象
                Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
                HighlightField highlightField = highlightFields.get("title");
                Text[] fragments = highlightField.getFragments();
                goods.setTitle(fragments[0].string());
                return goods;
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
        responseVo.setGoodsList(goodsList );

        //解析聚合结果集
        Map<String, Aggregation> aggregationMap = response.getAggregations().asMap();
        //解析品牌的聚合结果集
        ParsedLongTerms brandIdAgg = (ParsedLongTerms) aggregationMap.get("brandIdAgg");
        List<? extends Terms.Bucket> brandBuckets = brandIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(brandBuckets)){
            responseVo.setBrands( brandBuckets.stream().map(bucket->{
                BrandEntity brandEntity = new BrandEntity();
                //获取attrIdAgg的key，这个key就是品牌的id
                brandEntity.setId(bucket.getKeyAsNumber().longValue());
                //获取了品牌子聚合的map
                Map<String, Aggregation> subAggregationMap = bucket.getAggregations().asMap();
                //获取了品牌名称子聚合
                ParsedStringTerms brandNameAgg = (ParsedStringTerms)subAggregationMap.get("brandNameAgg");
                List<? extends Terms.Bucket> nameAggBuckets = brandNameAgg.getBuckets();
                //获取桶中的第一个元素
                if(!CollectionUtils.isEmpty(nameAggBuckets)){
                    brandEntity.setName(nameAggBuckets.get(0).getKeyAsString());
                }
                //获取logo的子聚合
                ParsedStringTerms logoAgg = (ParsedStringTerms) subAggregationMap.get("logoAgg");
                List<? extends Terms.Bucket> logoAggBuckets = logoAgg.getBuckets();
                if(!CollectionUtils.isEmpty(logoAggBuckets)){
                    brandEntity.setLogo(logoAggBuckets.get(0).getKeyAsString());
                }

                return brandEntity;
            }).collect(Collectors.toList()));
        }

        //解析分类的聚合结果集
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms) aggregationMap.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryBuckets = categoryIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(categoryBuckets)){
            responseVo.setCategories(categoryBuckets.stream().map(bucket->{
                CategoryEntity categoryEntity = new CategoryEntity();
                //获取分类的id
                categoryEntity.setId(((Terms.Bucket)bucket).getKeyAsNumber().longValue());
                //获取分类名称
                ParsedStringTerms categoryNameAgg = (ParsedStringTerms) ((Terms.Bucket) bucket).getAggregations().get("categoryNameAgg");
                List<? extends Terms.Bucket> nameAggBuckets = categoryNameAgg.getBuckets();
                if(!CollectionUtils.isEmpty(nameAggBuckets)){
                    categoryEntity.setName(nameAggBuckets.get(0).getKeyAsString());
                }
                return categoryEntity;
            }).collect(Collectors.toList()));
        }

        //解析规格参数嵌套的结果聚合级
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        //获取嵌套聚合中的子聚合，attrIIdAgg
        ParsedLongTerms attrIdAgg = (ParsedLongTerms) attrAgg.getAggregations().get("attrIdAgg");
        //获取id子聚合中的桶集合
        List<? extends Terms.Bucket> attrIdAggBuckets = attrIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(attrIdAggBuckets)){
            //把桶集合转化成SearchResponseAttrVo集合
            List<SearchResponseAttrVo> filters = attrIdAggBuckets.stream().map(bucket -> {
                SearchResponseAttrVo responseAttrVo = new SearchResponseAttrVo();
                //获取规格参数的id
                responseAttrVo.setAttrId(bucket.getKeyAsNumber().longValue());
                //获取了id聚合中所有的子聚合
                Map<String, Aggregation> subAggregationMap = bucket.getAggregations().asMap();
                //获取了attrNameAgg子集合
                ParsedStringTerms attrNameAgg = (ParsedStringTerms) subAggregationMap.get("attrNameAgg");
                List<? extends Terms.Bucket> nameAggBuckets = attrNameAgg.getBuckets();
                //name子聚合应该有且仅有一个桶元素
                if(!CollectionUtils.isEmpty(nameAggBuckets)){
                 //获取第一个桶，并获取其key
                    responseAttrVo.setAttrName(nameAggBuckets.get(0).getKeyAsString());
                }
                //获取value子聚合了
                ParsedStringTerms attrValueAgg = (ParsedStringTerms) subAggregationMap.get("attrValueAgg");
                List<? extends Terms.Bucket> valueAggBuckets = attrValueAgg.getBuckets();
                if(!CollectionUtils.isEmpty(valueAggBuckets)){

                    responseAttrVo.setAttrValues(valueAggBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList()));

                }

                return responseAttrVo;
            }).collect(Collectors.toList());
            responseVo.setFilters(filters);
        }

        return responseVo;
    }
    /**
     * 构建查询DSL语句
     * @return
     */
    private SearchSourceBuilder buildDsl (SearchParamVo paramVo){
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        String keyword = paramVo.getKeyword();
        if(StringUtils.isBlank(keyword)){
            //打广告，TODO
            return null;
        }
        //1.构建查询条件(bool查询)
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //1.1 匹配查询
        boolQueryBuilder.must(QueryBuilders.matchQuery("title",keyword).operator(Operator.AND));
        //1.2 过滤
        // 1.2.1 品牌过滤
        List<Long> brandId = paramVo.getBrandId();
        if(!CollectionUtils.isEmpty(brandId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId",brandId));
        }
        // 1.2.2. 分类过滤
        List<Long> cid = paramVo.getCid();
        if(!CollectionUtils.isEmpty(cid)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId",cid));
        }
        //1.2.3. 价格区间过滤
        Double priceFrom = paramVo.getPriceFrom();
        Double priceTo = paramVo.getPriceTo();
        if(priceFrom!=null||priceTo!=null){
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            if(priceFrom!=null){
                rangeQuery.gte(priceFrom);
            }
            if(priceTo!=null){
                rangeQuery.lte(priceTo);
            }
            boolQueryBuilder.filter(rangeQuery);
        }
        //1.2.4. 是否有货
        Boolean store = paramVo.getStore();
        if(store!=null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("store",store));
        }
        //1.2.5. 规格参数的过滤  规格参数的过滤 props=5:高通-麒麟&props=6:骁龙865-硅谷1000
       List<String> props=paramVo.getProps();
        if(!CollectionUtils.isEmpty(props)){
            props.forEach(prop->{
               String[] attrs = StringUtils.split(prop, ":");
               if(attrs!=null&&attrs.length==2){
                  String attrId=attrs[0];
                  String attrValueString=attrs[1];
                  String[] attrValues = StringUtils.split(attrValueString, "-");
                  BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                  boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId",attrId));
                  boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue",attrValues));
                  boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs",boolQuery, ScoreMode.None));
                }
            });
        }
         sourceBuilder.query(boolQueryBuilder);
        // 2. 构建排序 0-默认，得分降序；1-按价格升序；2-按价格降序；3-按创建时间降序；4-按销量降序
        Integer sort = paramVo.getSort();
        String field="";
        SortOrder order=null;
        switch (sort){
            case 1: field="price"; order=SortOrder.ASC; break;
            case 2: field="price"; order=SortOrder.DESC; break;
            case 3: field="sales"; order=SortOrder.ASC; break;
            case 4: field="sales"; order=SortOrder.DESC; break;
            default: field="_score"; order=SortOrder.DESC;break;
        }
        sourceBuilder.sort(field,order);
        //3. 构建分页
        Integer pageNum = paramVo.getPageNum();
        Integer pageSize = paramVo.getPageSize();
        sourceBuilder.from((pageNum-1)*pageSize);
        sourceBuilder.size(pageSize);
        //4.构建高亮
        sourceBuilder.highlighter(new HighlightBuilder().field("title").preTags("<font style='color:red'>").postTags("</font>"));
        //5. 构建聚合
         //5.1 构建品牌聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId").subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName")).
                subAggregation(AggregationBuilders.terms("logoAgg").field("logo")));
        // 5.2 构建分类聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId").subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));
        //5.3 构建规格参数的嵌套集聚合
        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg","searchAttrs").subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId").subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName")).subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue"))));
        //6. 结果集过滤
        sourceBuilder.fetchSource(new String[]{"skuId", "title", "price", "defaultImage","subTitle"},null);
        System.out.println(sourceBuilder.toString());
        return sourceBuilder;
}
   }

