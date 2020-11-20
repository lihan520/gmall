package com.atguigu.gmall.pms.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.CategoryMapper;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, CategoryEntity> implements CategoryService {
    @Autowired
    private CategoryMapper categoryMapper;
    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<CategoryEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageResultVo(page);
    }


    @Override
    public List<CategoryEntity> queryCategory(Long parentId) {
        // 构造查询条件
        QueryWrapper<CategoryEntity> wrapper = new QueryWrapper<>();
        // 如果parentId为-1，说明用户没有传该字段，查询所有
        if (parentId != -1){
            wrapper.eq("parent_id", parentId);
        }

        return this.categoryMapper.selectList(wrapper);
    }

    @Override
    public List<CategoryEntity> queryCategoriesWithSub(Long pid) {
        return this.categoryMapper.queryCategoriesByPid(pid);
    }

    @Override
    public List<CategoryEntity> queryAllCategoriesByCid3(Long cid) {
        //查询三级分类
        CategoryEntity cid3 = this.categoryMapper.selectOne(new QueryWrapper<CategoryEntity>().eq("id", cid));
        //查询二级分类
        CategoryEntity cid2 = this.categoryMapper.selectOne(new QueryWrapper<CategoryEntity>().eq("id", cid3.getParentId()));
        //查询一级分类
        CategoryEntity cid1 = this.categoryMapper.selectOne(new QueryWrapper<CategoryEntity>().eq("id", cid2.getParentId()));
        return Arrays.asList(cid1,cid2,cid3);
    }
}