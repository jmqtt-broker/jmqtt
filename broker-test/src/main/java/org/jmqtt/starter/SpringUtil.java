package org.jmqtt.starter;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Spring工具类
 *
 * @author xiongxiaoyang
 * @version 1.0
 * @since 2020/5/23
 */
@Component
public class SpringUtil implements ApplicationContextAware {


    private static ApplicationContext applicationContext;

    private static Environment environment;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        if (SpringUtil.applicationContext == null) {
            SpringUtil.applicationContext = applicationContext;
            SpringUtil.environment = applicationContext.getEnvironment();
        }
    }

    /**
     * 获取applicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 通过name获取 Bean.
     */
    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }

    /**
     * 通过class获取Bean
     */
    public static <T> T getBean(Class<T> clazz) {
        return getApplicationContext().getBean(clazz);
    }

    /**
     * 通过name,以及Clazz返回指定的Bean
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return getApplicationContext().getBean(name, clazz);
    }

    public static <T> Map<String, T> getBeansOfType(Class<T> clazz) {
        return getApplicationContext().getBeansOfType(clazz);
    }

    /**
     * 获取 environment
     *
     * @return
     */
    public static Environment getEnv() {
        return SpringUtil.environment;
    }

    /**
     * 含有 property
     *
     * @param key
     * @return
     */
    public static boolean containsProperty(String key) {
        return environment.containsProperty(key);
    }

    /**
     * 获取 property
     *
     * @param key
     * @return
     */
    public static String getProperty(String key) {
        return environment.getProperty(key);
    }
}
