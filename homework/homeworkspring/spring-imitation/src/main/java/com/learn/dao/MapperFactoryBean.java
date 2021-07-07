package com.learn.dao;

import com.learn.factory.FactoryBean;

/**
 * @author T00032266
 * @DateTime 2021/7/7
 */
public class MapperFactoryBean implements FactoryBean {

    private Class objectType;

    @Override
    public Object getObject() throws Exception {
        return null;
    }

    @Override
    public Class<?> getObjectType() {
        return objectType;
    }

    @Override
    public void setObjectType(Class tClass) {
        this.objectType = tClass;
    }
}
