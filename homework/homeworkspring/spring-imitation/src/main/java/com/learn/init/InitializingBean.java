package com.learn.init;

public interface InitializingBean {
    void afterPropertiesSet() throws Exception;
}
