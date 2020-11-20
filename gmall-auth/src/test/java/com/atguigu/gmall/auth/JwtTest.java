package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
    public class JwtTest {

        // 别忘了创建D:\\project\rsa目录
        private static final String pubKeyPath = "D:\\project\\rsa\\rsa.pub";
        private static final String priKeyPath = "D:\\project\\rsa\\rsa.pri";

        private PublicKey publicKey;

        private PrivateKey privateKey;

        @Test
        public void testRsa() throws Exception {
            RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
        }

            @BeforeEach
        public void testGetRsa() throws Exception {
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
            this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
        }

        @Test
        public void testGenerateToken() throws Exception {
            Map<String, Object> map = new HashMap<>();
            map.put("id", "11");
            map.put("username", "liuyan");
            // 生成token
            String token = JwtUtils.generateToken(map, privateKey, 5);
            System.out.println("token = " + token);
        }

        @Test
        public void testParseToken() throws Exception {
            String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE2MDU1MjI0MTN9.MwuqxzAzl_l9w_a6NO87PFiT_GXdc8TdCf-FRLo29Vou4DT1svRGo2PdAqwGaWn5jElABl91IpXtOmq5IdjZv6A4eyiLjHELH04cwjr38Ci9xGQmKEHpHfjIMl4sH2xZiaG_afItJ7w-Nyuafzup2Dqi-SofmP3fSK17bFE2iAMqKzr-rxkaX4Io8RSMr0HpwHgB0yT7NifwQ1WjRQZKSHKMmWgY1MfDdM3C-YpF3FnE7WuDyuqpgnK7NHGmUnk_3p4rzxDlhKihvgH4C_EvZ1rON8jzwhgJ8EYkBakRBX85MSpIwd_lLAaY2XN-AsbboHAHqvYLzNls5JIuydenww";

            // 解析token
            Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
            System.out.println("id: " + map.get("id"));
            System.out.println("userName: " + map.get("username"));
        }
    }

