package org.jmqtt.starter.api.controller;

import lombok.RequiredArgsConstructor;
import org.jmqtt.broker.store.rdb.daoobject.BrokerDO;
import org.jmqtt.starter.api.entity.PageVo;
import org.jmqtt.starter.api.entity.Result;
import org.jmqtt.starter.api.service.BrokerService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("jmqtt/broker")
@RequiredArgsConstructor
public class BrokerInfoController {

    private final BrokerService brokerService;

    @GetMapping("selectByBrokerId/{brokerId}")
    public Result<BrokerDO> selectByBrokerId(@PathVariable String brokerId) {
        return Result.ok(brokerService.selectByBrokerId(brokerId));
    }

    @GetMapping
    public Result<PageVo<BrokerDO>> page(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         BrokerDO brokerDO) {
        return Result.ok(brokerService.page(page, pageSize, brokerDO));
    }

}
