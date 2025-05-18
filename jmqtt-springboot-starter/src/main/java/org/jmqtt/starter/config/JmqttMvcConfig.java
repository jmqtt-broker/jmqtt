package org.jmqtt.starter.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class JmqttMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String staticPath = ResourceUtils.CLASSPATH_URL_PREFIX + "/static/";
        registry.addResourceHandler("/jmqtt/**")
                .addResourceLocations(staticPath);
    }
}
