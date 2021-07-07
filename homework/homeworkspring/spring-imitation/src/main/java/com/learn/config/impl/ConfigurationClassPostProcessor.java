package com.learn.config.impl;

import com.learn.annotation.Component;
import com.learn.annotation.ComponentScan;
import com.learn.annotation.Configuration;
import com.learn.config.BeanDefinition;
import com.learn.config.BeanDefinitionRegistryPostProcessor;
import com.learn.config.support.BeanDefinitionRegistry;
import com.learn.factory.BeanFactory;
import com.learn.utils.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author T00032266
 * @DateTime 2021/6/29
 */
public class ConfigurationClassPostProcessor implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws Exception {
        String[] beanDefinitionNames = registry.getBeanDefinitionNames();

        List<BeanDefinition> beanDefinitionList = new ArrayList<>();
        //筛选@Configuration注释的BeanDefinition
        for (String beanDefinitionName : beanDefinitionNames) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanDefinitionName);
            Class beanClass = beanDefinition.getBeanClass();
            List<String> annotations = ClassUtils.getAnnotations(beanClass);
            if (annotations.contains(Configuration.class.getName())) {
                beanDefinitionList.add(beanDefinition);
            }
        }

        if (beanDefinitionList.isEmpty()) {
            return;
        }

        for (BeanDefinition beanDefinition : beanDefinitionList) {
            processConfigurationClass(beanDefinition, registry);
        }


    }

    /**
     * 处理Configuration配置类
     */
    private void processConfigurationClass(BeanDefinition beanDefinition, BeanDefinitionRegistry registry) throws Exception {
        Class beanClass = beanDefinition.getBeanClass();

        //处理包扫描
        Annotation componentScanAnnotation = beanClass.getAnnotation(ComponentScan.class);
        if (componentScanAnnotation != null) {
            Method method = componentScanAnnotation.annotationType().getDeclaredMethod("basePackages");
            String[] basePackages = (String[])method.invoke(componentScanAnnotation);

            if (basePackages == null || basePackages.length == 0) {
                basePackages = new String[]{beanClass.getPackage().getName()};
            }
            doScan(basePackages, registry);
        }

    }

    /**
     * 处理包扫描
     */
    private void doScan(String[] basePackages, BeanDefinitionRegistry registry) {
        for (String basePackage : basePackages) {
            Set<Class<?>> classes = ClassUtils.getClasses(basePackage);
            for (Class<?> aClass : classes) {
                List<String> annotations = ClassUtils.getAnnotations(aClass);
                if (annotations.contains(Component.class.getName()) && ClassUtils.isClass(aClass)) {
                    BeanDefinition beanDefinition = new BeanDefinition(aClass);
                    String beanName = ClassUtils.getBeanName(beanDefinition.getBeanClass().getName());
                    registry.registerBeanDefinition(beanName, beanDefinition);
                }
            }
        }
    }

    @Override
    public void postProcessBeanFactory(BeanFactory beanFactory) {

    }
}
