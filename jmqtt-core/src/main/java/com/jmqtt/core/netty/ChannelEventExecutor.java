package com.jmqtt.core.netty;

import com.jmqtt.core.common.Control;
import com.jmqtt.core.netty.event.ChannelEvent;
import com.jmqtt.core.netty.event.ChannelEventListener;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ChannelEventExecutor implements Runnable, Control {

    private final LinkedBlockingQueue<ChannelEvent> eventQueue;
    private final ChannelEventListener listener;
    private volatile boolean stopped = false;

    public ChannelEventExecutor(ChannelEventListener channelEventListener) {
        this.eventQueue = new LinkedBlockingQueue<>();
        this.listener = channelEventListener;
    }

    public void putEvent(final ChannelEvent channelEvent) {
        if (this.eventQueue.size() <= 100000) {
            this.eventQueue.add(channelEvent);
        } else {
            log.warn("event queue size[{}] enough, so drop this event {}", this.eventQueue.size(), channelEvent.toString());
        }
    }

    @Override
    public void run() {
        while (!this.stopped) {
            try {
                ChannelEvent event = this.eventQueue.poll(3000, TimeUnit.MILLISECONDS);
                if (event != null && listener != null) {
                    switch (event.getEventType()) {
                        case CONNECT:
                            listener.onChannelConnect(event.getRemoteAddr(), event.getChannel());
                            break;
                        case CLOSE:
                            listener.onChannelClose(event.getRemoteAddr(), event.getChannel());
                            break;
                        case EXCEPTION:
                            listener.onChannelException(event.getRemoteAddr(), event.getChannel());
                            break;
                        case IDLE:
                            listener.onChannelIdle(event.getRemoteAddr(), event.getChannel());
                            break;
                        default:
                            break;
                    }
                }
            } catch (Throwable t) {
                log.warn("ChannelEvent exception.", t);
            }
        }
        log.info("ChannelEventExecutor stoped.");
    }

    @Override
    public void start() {
        Thread.startVirtualThread(this);
    }

    @Override
    public void stop() {
        this.stopped = true;
    }
}
