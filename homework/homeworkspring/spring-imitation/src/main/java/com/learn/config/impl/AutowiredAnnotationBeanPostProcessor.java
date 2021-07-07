package com.learn.config.impl;

import com.learn.annotation.Autowired;
import com.learn.aware.BeanFactoryAware;
import com.learn.config.BeanDefinition;
import com.learn.config.MergedBeanDefinitionPostProcessor;
import com.learn.factory.impl.DefaultListableBeanFactory;
import com.learn.model.InjectionMetadata;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class AutowiredAnnotationBeanPostProcessor implements MergedBeanDefinitionPostProcessor, BeanFactoryAware {

    private DefaultListableBeanFactory beanFactory;

    private Map<String, InjectionMetadata> injectionMetadataCache = new HashMap<>();

    public void setBeanFactory(DefaultListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }


    @Override
    public void postProcessMergedBeanDefinition(BeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        boolean flag =false;
        InjectionMetadata injectionMetadata = new InjectionMetadata();
        Method[] declaredMethods = beanType.getDeclaredMethods();
        for (Method method : declaredMethods) {
            Autowired annotation = method.getAnnotation(Autowired.class);
            if (annotation != null) {
                injectionMetadata.addMethod(method);
                flag = true;
            }
        }


        Field[] declaredFields = beanType.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            Autowired annotation = declaredField.getAnnotation(Autowired.class);
            if (annotation != null) {
                injectionMetadata.addField(declaredField);
                flag = true;
            }
        }
        if (flag) {
            injectionMetadataCache.put(beanName, injectionMetadata);
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws Exception {
        InjectionMetadata injectionMetadata = injectionMetadataCache.get(beanName);
        if (injectionMetadata != null) {
            //方法处理
            for (Method method : injectionMetadata.getMethodList()) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                Object[] params = new Object[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; i++) {
                    Class<?> parameterType = parameterTypes[i];
                    params[i] = beanFactory.getBean(parameterType);
                }
                method.invoke(bean, params);
            }
            //属性处理
            for (Field field : injectionMetadata.getFieldList()) {
                Class<?> type = field.getType();
                field.setAccessible(true);
                field.set(bean, beanFactory.getBean(type));
            }
        }
        return bean;
    }
}
