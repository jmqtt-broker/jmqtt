package org.jmqtt.starter.api.controller;

import lombok.RequiredArgsConstructor;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.starter.api.entity.Result;
import org.jmqtt.starter.api.entity.UserInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("jmqtt/user")
@RequiredArgsConstructor
public class UserController {

    private final BrokerConfig brokerConfig;

    @GetMapping("getInfo")
    public Result<?> userInfo() {
        return Result.ok(new UserInfo(brokerConfig.getUser(), brokerConfig.getPwd()));
    }

}
