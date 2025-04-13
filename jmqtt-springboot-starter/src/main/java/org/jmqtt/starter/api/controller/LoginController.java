package org.jmqtt.starter.api.controller;

import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.common.helper.CaffeineUtil;
import org.jmqtt.starter.api.entity.LoginParam;
import org.jmqtt.starter.api.entity.Result;
import org.jmqtt.starter.api.entity.UserInfo;
import org.jmqtt.starter.utils.CookieUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@RestController
@RequestMapping("jmqtt")
@RequiredArgsConstructor
public class LoginController {

    private final BrokerConfig brokerConfig;

    @PostMapping("login")
    public Result<JSONObject> login(@RequestBody LoginParam login, HttpServletResponse response) {
        if (brokerConfig.getUser().equals(login.getUsername()) &&
                brokerConfig.getPwd().equals(login.getPassword())) {
            String token = UUID.randomUUID().toString();
            CookieUtil.setCookie(response, "token", token);
            JSONObject res = new JSONObject();
            res.put("token", token);
            CaffeineUtil.put(token, new UserInfo(login.getUsername(), login.getUsername()), 30 * 60);
            return Result.ok(res);
        }
        return Result.fail("登录失败");
    }

    @PostMapping("logout")
    public Result<?> logout() {
        return Result.ok();
    }

}
