package com.jmqtt.core.mqtt.helper;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <a href="https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901031">Reason Code</a>
 */
@Getter
@AllArgsConstructor
public enum Reason {

    SUCCESS((byte) 0x00, "Success / Normal disconnection / Granted QoS 0"),
    GRANTED_QOS_1((byte) 0x01, "Granted QoS 1"),
    GRANTED_QOS_2((byte) 0x02, "Granted QoS 2"),
    DISCONNECT_WITH_WILL_MESSAGE((byte) 0x04, "Disconnect with Will Message"),
    NO_MATCHING_SUBSCRIBERS((byte) 0x10, "No matching subscribers"),
    NO_SUBSCRIPTION_EXISTED((byte) 0x11, "No subscription existed"),
    SERVER_MOVED((byte) 0x17, "Server moved"),
    NO_SHARED_SUBSCRIPTION_EXISTS((byte) 0x18, "No shared subscription exists"),
    UNKNOWN_TOPIC_FILTER((byte) 0x19, "Unknown topic filter"),

    UNSPECIFIED_ERROR((byte) 0x80, "Unspecified error"),
    MALFORMED_PACKET((byte) 0x81, "Malformed Packet"),
    PROTOCOL_ERROR((byte) 0x82, "Protocol Error"),
    IMPLEMENTATION_SPECIFIC_ERROR((byte) 0x83, "Implementation specific error"),
    UNSUPPORTED_PROTOCOL_VERSION((byte) 0x84, "Unsupported Protocol Version"),
    CLIENT_IDENTIFIER_NOT_VALID((byte) 0x85, "Client Identifier not valid"),
    BAD_USER_NAME_OR_PASSWORD((byte) 0x86, "Bad User Name or Password"),
    NOT_AUTHORIZED((byte) 0x87, "Not Authorized"),
    SERVER_UNAVAILABLE((byte) 0x88, "Server Unavailable"),
    SERVER_BUSY((byte) 0x89, "Server Busy"),
    BANNED((byte) 0x8A, "Banned"),
    SERVER_SHUTTING_DOWN((byte) 0x8B, "Server Shutting Down"),
    BAD_AUTHENTICATION_METHOD((byte) 0x8C, "Bad Authentication Method"),
    KEEP_ALIVE_TIMEOUT((byte) 0x8D, "Keep Alive timeout"),
    SESSION_TAKEN_OVER((byte) 0x8E, "Session taken over"),
    TOPIC_FILTER_INVALID((byte) 0x8F, "Topic Filter invalid"),
    TOPIC_NAME_INVALID((byte) 0x90, "Topic Name invalid"),
    PACKET_IDENTIFIER_IN_USE((byte) 0x91, "Packet Identifier in use"),
    PACKET_IDENTIFIER_NOT_FOUND((byte) 0x92, "Packet Identifier not found"),
    RECEIVE_MAXIMUM_EXCEEDED((byte) 0x93, "Receive Maximum exceeded"),
    TOPIC_ALIAS_INVALID((byte) 0x94, "Topic Alias invalid"),
    PACKET_TOO_LARGE((byte) 0x95, "Packet too large"),
    MESSAGE_RATE_TOO_HIGH((byte) 0x96, "Message Rate too high"),
    QUOTA_EXCEEDED((byte) 0x97, "Quota exceeded"),
    ADMINISTRATIVE_ACTION((byte) 0x98, "Administrative action"),
    PAYLOAD_FORMAT_INVALID((byte) 0x99, "Payload Format invalid"),
    RETAIN_NOT_SUPPORTED((byte) 0x9A, "Retain not supported"),
    QOS_NOT_SUPPORTED((byte) 0x9B, "QoS not supported"),
    USE_ANOTHER_SERVER((byte) 0x9C, "Use another server"),
    SERVER_MOVED_ANOTHER_LOCATION((byte) 0x9D, "Server moved to another location"),
    SHARED_SUBSCRIPTIONS_NOT_SUPPORTED((byte) 0x9E, "Shared Subscriptions not supported"),
    CONNECTION_RATE_EXCEEDED((byte) 0x9F, "Connection Rate exceeded"),
    MAXIMUM_CONNECT_TIME((byte) 0xA0, "Maximum connect time"),
    SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED((byte) 0xA1, "Subscription Identifiers not supported"),
    WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED((byte) 0xA2, "Wildcard Subscriptions not supported"),
    ;

    private final byte reasonCode;

    private final String reasonString;

}
