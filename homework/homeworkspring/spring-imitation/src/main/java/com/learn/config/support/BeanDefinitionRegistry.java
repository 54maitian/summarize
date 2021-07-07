package com.learn.config.support;

import com.learn.config.BeanDefinition;

public interface BeanDefinitionRegistry {
    String[] getBeanDefinitionNames();

    BeanDefinition getBeanDefinition(String beanName);

    void registerBeanDefinition(String beanName, BeanDefinition beanDefinition);
}
