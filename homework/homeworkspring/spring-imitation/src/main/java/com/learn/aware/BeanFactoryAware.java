package com.learn.aware;

import com.learn.factory.impl.DefaultListableBeanFactory;

/**
 * @author T00032266
 * @DateTime 2021/7/7
 */
public interface BeanFactoryAware {

    void setBeanFactory(DefaultListableBeanFactory beanFactory);
}
