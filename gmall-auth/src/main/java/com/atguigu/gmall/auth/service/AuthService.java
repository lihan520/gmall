package com.atguigu.gmall.auth.service;

import com.atguigu.gmall.auth.cofig.JwtProperties;
import com.atguigu.gmall.auth.feign.GmallUmsClient;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.UserException;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.IpUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@EnableConfigurationProperties(JwtProperties.class)
@Service
public class AuthService {
    @Autowired
    private  JwtProperties jwtProperties;
    @Autowired
    private GmallUmsClient gmallUmsClient;
    public void login(String loginName, String password, HttpServletRequest request, HttpServletResponse response) {
       //调用ums的远程接口接口信息
        ResponseVo<UserEntity> userEntityResponseVo = this.gmallUmsClient.queryUser(loginName, password);
        UserEntity userEntity= userEntityResponseVo.getData();
        //判断用户信息是否为空
        if(userEntity==null){
            throw new UserException("用户名或者密码错误！！！");
        }
        try {
            //3、生成jwt
            Map<String,Object> map=new HashMap<>();
            map.put("userId",userEntity.getId());
            map.put("username",userEntity.getUsername());
            map.put("ip", IpUtils.getIpAddressAtService(request));
            String token = JwtUtils.generateToken(map, jwtProperties.getPrivateKey(), jwtProperties.getExpire());
            //4、把jwt放入cookie中
            CookieUtils.setCookie(request, response, this.jwtProperties.getCookieName(), token,this.jwtProperties.getExpire() * 60);
            //5、为了方便展示用户昵称，应该把昵称也放入cookie中
            CookieUtils.setCookie(request,response,jwtProperties.getNickName(),userEntity.getNickname(),this.jwtProperties.getExpire()*60);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
