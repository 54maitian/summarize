package com.learn.factory.impl;


import com.learn.config.BeanDefinition;
import com.learn.config.BeanDefinitionRegistryPostProcessor;
import com.learn.config.BeanFactoryPostProcessor;
import com.learn.config.BeanPostProcessor;
import com.learn.config.impl.AutowiredAnnotationBeanPostProcessor;
import com.learn.config.impl.ConfigurationClassPostProcessor;
import com.learn.utils.ClassUtils;

import java.util.ArrayList;

import java.util.List;
import java.util.Set;

/**
 * @author T00032266
 * @DateTime 2021/6/29
 */
public class AnnotationConfigApplicationContext {
    public static final String CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME =
            "org.springframework.context.annotation.internalConfigurationAnnotationProcessor";

    public static final String AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME =
            "org.springframework.context.annotation.internalAutowiredAnnotationProcessor";

    private final DefaultListableBeanFactory beanFactory;


    public AnnotationConfigApplicationContext(Class<?>... componentClasses) throws Exception {
        beanFactory = new DefaultListableBeanFactory();

        //注册ConfigurationClassPostProcessor
        BeanDefinition beanDefinition = new BeanDefinition(ConfigurationClassPostProcessor.class);
        registerBeanDefinition(CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME, beanDefinition);

        //注册AutowiredAnnotationBeanPostProcessor
        beanDefinition = new BeanDefinition(AutowiredAnnotationBeanPostProcessor.class);
        registerBeanDefinition(AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME, beanDefinition);

        for (Class<?> componentClass : componentClasses) {
            register(componentClass);
        }

        refresh();
    }



    public void refresh() throws Exception {
        DefaultListableBeanFactory beanFactory = obtainFreshBeanFactory();

        //BeanFactoryPostProcessor前置处理
        invokeBeanFactoryPostProcessors(beanFactory);

        // 加载注册BeanPostProcessor
        registerBeanPostProcessors(beanFactory);

        // 实例化所有剩余的（非延迟初始化）单例。
        finishBeanFactoryInitialization(beanFactory);
    }

    private void finishBeanFactoryInitialization(DefaultListableBeanFactory beanFactory) throws Exception {
        Set<String> registeredSingletons = beanFactory.getRegisteredSingletons();
        for (String beanDefinitionName : beanFactory.getBeanDefinitionNames()) {
            if (!registeredSingletons.contains(beanDefinitionName)) {
                beanFactory.getBean(beanDefinitionName);
            }
        }
    }


    /**
     * 加载注册BeanPostProcessor
     */
    private void registerBeanPostProcessors(DefaultListableBeanFactory beanFactory) throws Exception {
        String[] postProcessors = beanFactory.getBeanNamesForType(BeanPostProcessor.class);
        for (String postProcessorName : postProcessors) {
            beanFactory.addBeanPostProcessor((BeanPostProcessor)beanFactory.getBean(postProcessorName));
        }
    }


    /**
     * BeanFactoryPostProcessor前置处理
     */
    private void invokeBeanFactoryPostProcessors(DefaultListableBeanFactory beanFactory) throws Exception {
        //BeanDefinitionRegistryPostProcessor处理
        List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();
        String[] registryPostProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class);
        for (String postProcessorName : registryPostProcessorNames) {
            currentRegistryProcessors.add((BeanDefinitionRegistryPostProcessor)beanFactory.getBean(postProcessorName));
        }
        for (BeanDefinitionRegistryPostProcessor registryPostProcessor : currentRegistryProcessors) {
            registryPostProcessor.postProcessBeanDefinitionRegistry(beanFactory);
        }

        //BeanFactoryPostProcessor处理
        List<BeanFactoryPostProcessor> currentPostProcessors = new ArrayList<>();
        String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class);
        for (String postProcessorName : postProcessorNames) {
            currentPostProcessors.add((BeanFactoryPostProcessor)beanFactory.getBean(postProcessorName));
        }
        for (BeanFactoryPostProcessor postProcessor : currentPostProcessors) {
            postProcessor.postProcessBeanFactory(beanFactory);
        }
    }


    /**
     *  获取beanFactory
     */
    private DefaultListableBeanFactory obtainFreshBeanFactory() {
        return this.beanFactory;
    }


    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition){
        this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
    }


    public void register(Class<?>... componentClasses) throws Exception {
        for (Class<?> componentClass : componentClasses) {
            registerBean(componentClass);
        }
    }

    public void registerBean(Class<?> beanClass) throws Exception {
        doRegisterBean(beanClass);
    }

    public <T> void doRegisterBean(Class<T> beanClass) throws Exception {
        BeanDefinition beanDefinition = new BeanDefinition(beanClass);
        String beanName = ClassUtils.generateBeanName(beanDefinition);
        registerBeanDefinition(beanName, beanDefinition);
    }

    /**
     * 名称获取对象
     */
    public Object getBean(String name) throws Exception {
        return this.beanFactory.getBean(name);
    }

}
