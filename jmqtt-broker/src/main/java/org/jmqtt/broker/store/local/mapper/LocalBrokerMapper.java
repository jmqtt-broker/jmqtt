
package org.jmqtt.broker.store.local.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.jmqtt.broker.store.rdb.daoobject.BrokerDO;

import java.util.List;

public interface LocalBrokerMapper {

    @Select("select id,broker_id,ip,tcp_port,tcp_port_ssl, ws_port, ws_port_ssl, status, online_at, offline_at from jmqtt_broker where broker_id = #{brokerId}")
    BrokerDO getBroker(String brokerId);

    @Select("select id,broker_id,ip,tcp_port,tcp_port_ssl, ws_port, ws_port_ssl, status, online_at, offline_at from jmqtt_broker")
    List<BrokerDO> getAll();

    @Insert(
            "insert into jmqtt_broker(id,broker_id,ip,tcp_port,tcp_port_ssl, ws_port, ws_port_ssl, status, online_at, offline_at) values " +
                    "(#{id},#{brokerId},#{ip},#{tcpPort},#{tcpPortSsl}, #{wsPort}, #{wsPortSsl}, #{status}, #{onlineAt}, #{offLineAt}) " +
                    "on DUPLICATE key update tcp_port = #{tcpPort},tcp_port_ssl = #{tcpPortSsl},ws_port=#{wsPort}," +
                    "ws_port_ssl=#{wsPortSsl},status=#{status},online_at=#{onlineAt},offline_at=#{offLineAt}"
    )
    Long storeBroker(BrokerDO brokerDO);

    @Delete("delete from jmqtt_broker where broker_id = #{brokerId} ")
    Long del(String brokerId);

}
