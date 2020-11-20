package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;


import java.util.List;

@Controller
public class IndexController {
    @Autowired
    private IndexService indexService;
    @GetMapping("/xxx")
    @ResponseBody
    public String test(@RequestHeader("userId")String userId){
        return "获取到的用户登录信息："+userId;
    }
    @GetMapping
    public String toIndex(Model model){
        List<CategoryEntity> categoryEntities= this.indexService.queryLvl1Categories();
        model.addAttribute("categories",categoryEntities);
        return "index";
    }
    @GetMapping("index/cates/{pid}")
    @ResponseBody
    public ResponseVo<List<CategoryEntity>>queryLvl2CategoriesWithSub(@PathVariable("pid")Long pid){
        List<CategoryEntity> categoryEntities=this.indexService.queryLvl2CategoriesWithSub(pid);
        return ResponseVo.ok(categoryEntities);
    }
    @GetMapping("index/test/lock")
    @ResponseBody
    public ResponseVo testLock(){
        this.indexService.testLock();
        return ResponseVo.ok();
    }
}
