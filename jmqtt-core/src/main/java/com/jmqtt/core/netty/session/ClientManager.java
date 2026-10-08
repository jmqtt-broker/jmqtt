package com.jmqtt.core.netty.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 客户端连接管理器
 */
public class ClientManager {

    private final static Map<String, ClientSession> CLIENT_STORE = new ConcurrentHashMap<>();

    private ClientManager(){}

    public static ClientSession getClient(String clientId){
        return CLIENT_STORE.get(clientId);
    }

    public static ClientSession putClient(String clientId,ClientSession clientSession){
        return CLIENT_STORE.put(clientId,clientSession);
    }

    public static boolean containClient(String clientId){
        return CLIENT_STORE.containsKey(clientId);
    }

    public static ClientSession removeClient(String clientId){
        if (clientId != null) {
            return CLIENT_STORE.remove(clientId);
        }
        return null;
    }

    public static void forEach(Consumer<ClientSession> consumer) {
        CLIENT_STORE.values().forEach(consumer);
    }

}
