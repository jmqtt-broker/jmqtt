package org.jmqtt.broker.common.config;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jmqtt.common.akka.AkkaConst;
import org.jmqtt.common.serializer.kryo.KryoSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class AkkaConfig {

    private Boolean enable = false;
    private String systemName = AkkaConst.SYSTEM_NAME;
    private String host;
    private String port;
    private List<String> roles;
    private List<String> clusterNodes;

    public String configStr() {
        JSONObject wrap = new JSONObject();
        JSONObject akka = new JSONObject();
        wrap.put("akka", akka);
        JSONObject remote = new JSONObject();
        JSONObject artery = new JSONObject();
        JSONObject canonical = new JSONObject();
        remote.put("artery", artery);
        artery.put("canonical", canonical);
        canonical.put("hostname", this.host);
        canonical.put("port", this.port);
        JSONObject cluster = new JSONObject();
        if (this.clusterNodes != null) {
            cluster.put("seed-nodes", this.clusterNodes.stream()
                    .map(n -> "akka://" + this.systemName + "@" + n)
                    .collect(Collectors.toList()));
        }
        if (this.roles != null) {
            cluster.put("roles", this.roles);
        }
        cluster.put("downing-provider-class", "akka.cluster.sbr.SplitBrainResolverProvider");
        // cluster.put("configuration-compatibility-check", new JSONObject(){{put("enforce-on-join", "off");}});
        akka.put("remote", remote);
        akka.put("cluster", cluster);
        JSONObject actor = new JSONObject();
        actor.put("provider", "cluster");
        actor.put("allow-java-serialization", "off");
        actor.put("warn-about-java-serializer-usage", "off");
        akka.put("actor", actor);
        JSONObject serializationIdentifiers = new JSONObject();
        JSONObject serializers = new JSONObject();
        JSONObject serializationBindings = new JSONObject();
        serializationIdentifiers.put("customkryo", KryoSerializer.IDENTIFIER);
        // serializationIdentifiers.put("json", 456);
        serializers.put("customkryo", "org.jmqtt.common.serializer.kryo.KryoSerializer");
        // serializers.put("json", "akka.serialization.jackson.JacksonJsonSerializer");
        serializationBindings.put("org.jmqtt.common.event.Event", "customkryo");
        actor.put("serialization-identifiers", serializationIdentifiers);
        actor.put("serializers", serializers);
        actor.put("serialization-bindings", serializationBindings);
        return wrap.toJSONString();
    }

}
