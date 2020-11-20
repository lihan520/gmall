package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Controller
public class CartController {
    @Autowired
    private CartService cartService;
    /*
    * 新增购物车，新增成功之后要重定向到新增成功页面
    *
    * */
    @GetMapping
    public String addCart(Cart cart){
        this.cartService.addCart(cart);
        return "redirect:http://cart.gmall.com/addCart.html?skuId="+cart.getSkuId();
    }
    /*
    * 新增购物车成功页面，本质就是根据用户登录信息和skuId查询
    * */
    @GetMapping("addCart.html")
    public String queryCart(@RequestParam("skuId")Long skuId, Model model){
        Cart cart=this.cartService.queryCart(skuId);
        model.addAttribute("cart",cart);
        return "addCart";
    }
    @GetMapping("cart.html")
    public String queryCarts(Model model){
        List<Cart> carts =this.cartService.queryCarts();
        model.addAttribute("carts",carts);
        return "cart";
    }
    @PostMapping("updateNum")
    @ResponseBody
    public ResponseVo updateNum(@RequestBody Cart cart){
        this.cartService.updateNum(cart);
        return ResponseVo.ok();
    }
    @PostMapping("updateStatus")
    @ResponseBody
    public ResponseVo updateStatus(@RequestBody Cart cart){
        this.cartService.updateStatus(cart);
        return ResponseVo.ok();
    }
    @PostMapping("deleteCart")
    @ResponseBody
    public ResponseVo deleteCart(@RequestParam("skuId")Long skuId){
        this.cartService.deleteCart(skuId);
        return ResponseVo.ok();
    }
    @GetMapping("test")
    @ResponseBody
    public String test(HttpServletRequest request) throws ExecutionException, InterruptedException {
        long now = System.currentTimeMillis();
        System.out.println("这是controller的test方法，调用了executor1和executor2方法");
        String executor1 = this.cartService.executor1();
        ListenableFuture<String> executor2 = this.cartService.executor2();
       // System.out.println("在controller中获取子任务的返回结果集：executor1= "+executor1.get()+", executor2= "+executor2.get());
        //executor1.addCallback(t-> System.out.println("成功的回调："+t),ex-> System.out.println("失败的回调: "+ex));
        executor2.addCallback(t-> System.out.println("成功的回调："+t),ex-> System.out.println("失败的回调: "+ex));
        System.out.println("controller的test方法执行完成。。。。"+(System.currentTimeMillis()-now));
        return "hello test";
    }
}
