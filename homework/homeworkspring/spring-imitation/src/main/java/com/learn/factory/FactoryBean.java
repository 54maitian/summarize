package com.learn.factory;

public interface FactoryBean {
    Object getObject() throws Exception;

    Class<?> getObjectType();

}
