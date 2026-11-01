package com.jmqtt.core.cluster.record;

public record ClusterSessionRecord(String clientId, Boolean cleanStart) {

    public static ClusterSessionRecord of(String clientId, Boolean cleanStart) {
        return new ClusterSessionRecord(clientId, cleanStart);
    }

}
