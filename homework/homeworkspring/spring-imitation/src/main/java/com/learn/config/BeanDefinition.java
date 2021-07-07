package com.learn.config;

import com.learn.model.Attr;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author T00032266
 * @DateTime 2021/6/29
 */
@Getter
@Setter
public class BeanDefinition {
    private Class beanClass;
    private String factoryMethodName;
    private String factoryBeanName;
    private List<Attr> attrList;

    public BeanDefinition(Class beanClass) {
        this.beanClass = beanClass;
    }
}
