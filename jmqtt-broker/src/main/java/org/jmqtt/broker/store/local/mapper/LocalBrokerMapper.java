
package org.jmqtt.broker.store.local.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.jmqtt.broker.store.rdb.daoobject.BrokerDO;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface LocalBrokerMapper extends Mapper<BrokerDO> {

    @Select("select id,broker_id,ip,tcp_port,tcp_port_ssl, ws_port, ws_port_ssl, status, online_at, offline_at from jmqtt_broker where broker_id = #{brokerId}")
    BrokerDO getBroker(String brokerId);

    @Select("select id,broker_id,ip,tcp_port,tcp_port_ssl, ws_port, ws_port_ssl, status, online_at, offline_at from jmqtt_broker")
    List<BrokerDO> getAll();

    @Insert("<script>" +
            "insert into jmqtt_broker(id,broker_id,ip,tcp_port,tcp_port_ssl, ws_port, ws_port_ssl, status, online_at, offline_at) values " +
            "(#{id},#{brokerId},#{ip},#{tcpPort},#{tcpPortSsl}, #{wsPort}, #{wsPortSsl}, #{status}, #{onlineAt}, #{offlineAt}) " +
            "<trim prefix=\"on DUPLICATE key update\" suffixOverrides=\",\">" +
            "<if test=\"tcpPort != null\">tcp_port=#{tcpPort},</if>" +
            "<if test=\"tcpPortSsl != null\">tcp_port_ssl=#{tcpPortSsl},</if>" +
            "<if test=\"wsPort != null\">ws_port=#{wsPort},</if>" +
            "<if test=\"wsPortSsl != null\">ws_port_ssl=#{wsPortSsl},</if>" +
            "<if test=\"status != null\">status=#{status},</if>" +
            "<if test=\"onlineAt != null\">online_at=#{onlineAt},</if>" +
            "<if test=\"offlineAt != null\">offline_at=#{offlineAt}</if>" +
            "</trim>" +
            "</script>"
    )
    Long storeBroker(BrokerDO brokerDO);

    @Delete("delete from jmqtt_broker where broker_id = #{brokerId} ")
    Long del(String brokerId);

}
