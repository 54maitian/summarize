package com.learn.config;

public interface MergedBeanDefinitionPostProcessor extends BeanPostProcessor {

    void postProcessMergedBeanDefinition(BeanDefinition beanDefinition, Class<?> beanType, String beanName);
}
