package com.jmqtt.core.mqtt.helper;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Reason {


    SUCCESS((byte) 0x00,"success"),
    SESSION_TAKEN_OVER((byte) 0x8E, "Session taken over."),
    SERVER_TERMINATED_CONNECTION((byte) 0x8B, "Server terminated connection."),
    SERVER_SHUTTING_DOWN_CONNECTION((byte) 0x8B, "Server shutting down."),
    KEEP_ALIVE_TIMEOUT((byte) 0x8D, "Keep Alive timeout."),
    RETAIN_NOT_SUPPORTED((byte) 0x9A, "Retain not supported."),
    QOS_NOT_SUPPORTED((byte) 0x9B, "QoS not supported."),
    PACKET_TOO_LARGE((byte) 0x95, "Packet too large."),
    PERMISSION_NOT_ALLOWED((byte) 0x87, "permission is not allowed"),
    ALISA_ERROR((byte) 0x82, "Alisa Error."),
    NO_MATCHING_SUBSCRIBERS((byte) 0x10, "No matching subscribers."),
    SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED((byte) 0xA1, "Subscription Identifiers not supported."),
    SHARED_SUBSCRIPTIONS_NOT_SUPPORTED((byte) 0x9E, "Shared Subscriptions not supported."),
    WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED((byte) 0xA2, "Wildcard Subscriptions not supported."),
    ;

    private final byte reasonCode;

    private final String reasonString;

}
