package org.jmqtt.broker.remoting.session;

import io.netty.channel.ChannelHandlerContext;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 本服务连接的设备会话信息
 */
@Getter
@Setter
@EqualsAndHashCode(of = {"clientId"})
@NoArgsConstructor
public class ClientSession {

    /**
     * clientId uniqu in a cluseter
     */
    private String clientId;
    private int version;
    private boolean cleanStart;
    private transient ChannelHandlerContext ctx;
    private String userName;

    private transient AtomicInteger messageIdCounter = new AtomicInteger(1);

    public ClientSession(String clientId, boolean cleanStart, int version, ChannelHandlerContext ctx) {
        this.clientId = clientId;
        this.cleanStart = cleanStart;
        this.version = version;
        this.ctx = ctx;
    }

    public int generateMessageId() {
        int messageId = messageIdCounter.getAndIncrement();
        messageId = Math.abs(messageId % 0xFFFF);
        if (messageId == 0) {
            return generateMessageId();
        }
        return messageId;
    }

    public boolean isMqtt5() {
        return this.version == 5;
    }
}
