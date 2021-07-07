package com.learn.config.support.impl;

import com.learn.config.BeanDefinition;
import com.learn.config.support.BeanDefinitionRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author T00032266
 * @DateTime 2021/6/29
 */
public class DefaultSingletonBeanRegistry implements BeanDefinitionRegistry {
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap(256);


    protected void addSingleton(String beanName, Object singletonObject) {
        synchronized (this.singletonObjects){
            this.singletonObjects.put(beanName, singletonObject);
        }
    }

    protected Object getSingleton(String beanName) {
        return singletonObjects.get(beanName);
    }


    @Override
    public String[] getBeanDefinitionNames() {
        return new String[0];
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        return null;
    }

    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {

    }
}
