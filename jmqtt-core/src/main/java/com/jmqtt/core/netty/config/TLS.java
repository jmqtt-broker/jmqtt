package com.jmqtt.core.netty.config;

import com.jmqtt.core.exception.ConfigException;
import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class TLS {

    private Boolean useClientCa;
    private String sslKeyStoreType;
    private String sslKeyFilePath;
    private String sslManagerPwd;
    private String sslStorePwd;

    public static TLS defaultConfig() {
        return builder()
                .useClientCa(false)
                .sslKeyStoreType("PKCS12")
                .sslKeyFilePath("classpath:ssl/server.pfx")
                .sslManagerPwd("jmqtt1234")
                .sslStorePwd("jmqtt1234")
                .build();
    }

    public void checkTlsConfig() {
        if (useClientCa == null) {
            throw ConfigException.of("ssl config property useClientCa is null");
        }
        if (sslKeyStoreType == null) {
            throw ConfigException.of("ssl config property sslKeyStoreType is null");
        }
        if (sslKeyFilePath == null) {
            throw ConfigException.of("ssl config property sslKeyFilePath is null");
        }
        if (sslManagerPwd == null) {
            throw ConfigException.of("ssl config property sslManagerPwd is null");
        }
        if (sslStorePwd == null) {
            throw ConfigException.of("ssl config property sslStorePwd is null");
        }
    }

    public ChannelHandler getSslHandler(SocketChannel channel) {
        SslContext sslContext = createSSLContext();
        SSLEngine sslEngine = sslContext.newEngine(
                channel.alloc(),
                channel.remoteAddress().getHostString(),
                channel.remoteAddress().getPort());
        if (useClientCa) {
            sslEngine.setNeedClientAuth(true);
        }
        return new SslHandler(sslEngine);
    }

    private SslContext createSSLContext() {
        try {
            InputStream ksInputStream;
            if (sslKeyFilePath.startsWith("classpath:")) {
                ksInputStream = TLS.class.getClassLoader().getResourceAsStream(sslKeyFilePath.replace("classpath:", "").trim());
            } else {
                ksInputStream = new FileInputStream(sslKeyFilePath);
            }
            KeyStore ks = KeyStore.getInstance(sslKeyStoreType);
            ks.load(ksInputStream, sslStorePwd.toCharArray());
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, sslManagerPwd.toCharArray());
            SslContextBuilder contextBuilder = SslContextBuilder.forServer(kmf);
            if (useClientCa) {
                contextBuilder.clientAuth(ClientAuth.REQUIRE);
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ks);
                contextBuilder.trustManager(tmf);
            }
            return contextBuilder.sslProvider(SslProvider.valueOf("JDK")).build();
        } catch (Exception ex) {
            log.error("Create ssl context failure.", ex);
        }
        return null;
    }

}
