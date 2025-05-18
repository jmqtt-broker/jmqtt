package org.jmqtt.starter.api.controller;

import lombok.RequiredArgsConstructor;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.store.rdb.daoobject.SessionDO;
import org.jmqtt.broker.store.rdb.daoobject.SubscriptionDO;
import org.jmqtt.starter.api.entity.MessageVo;
import org.jmqtt.starter.api.entity.PageVo;
import org.jmqtt.starter.api.entity.Result;
import org.jmqtt.starter.api.service.SessionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("jmqtt/session")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping("{clientId}")
    public Result<SessionDO> selectByClientId(@PathVariable String clientId) {
        return Result.ok(sessionService.selectByClientId(clientId));
    }

    @GetMapping
    public Result<PageVo<SessionDO>> page(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int pageSize,
                                          SessionDO sessionDO) {
        return Result.ok(sessionService.page(page, pageSize, sessionDO));
    }

    @DeleteMapping("kick/{clientId}")
    public Result<?> kickConnection(@PathVariable String clientId) {
        sessionService.kickConnection(clientId);
        return Result.ok();
    }

    @GetMapping("subscriptions/{clientId}")
    public Result<List<SubscriptionDO>> getSubscriptions(@PathVariable String clientId) {
        return Result.ok(sessionService.getSubscriptions(clientId));
    }

    @GetMapping("will/{clientId}")
    public Result<MessageVo> getWillMessage(@PathVariable String clientId) {
        Message willMessage = sessionService.getWillMessage(clientId);
        return Result.ok(willMessage != null ? new MessageVo(willMessage) : null);
    }

}
