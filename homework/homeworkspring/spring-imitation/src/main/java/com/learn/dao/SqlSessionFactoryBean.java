package com.learn.dao;

import com.learn.factory.FactoryBean;
import com.learn.init.InitializingBean;
import lombok.Getter;
import resources.Resources;
import sessionfactory.SqlSessionFactory;
import sessionfactory.SqlSessionFactoryBuilder;

import java.io.InputStream;

/**
 * @author T00032266
 * @DateTime 2021/6/30
 */
@Getter
public class SqlSessionFactoryBean implements InitializingBean, FactoryBean {

    private SqlSessionFactory sqlSessionFactory;

    @Override
    public void afterPropertiesSet() throws Exception {
        //解析配置文件流
        InputStream resourceAsStream = Resources.getResourceAsStream("sqlMapConfig.xml");
        //获取sqlSession
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(resourceAsStream);

        this.sqlSessionFactory = sqlSessionFactory;
    }

    @Override
    public Object getObject() throws Exception {
        return sqlSessionFactory;
    }

    @Override
    public Class<?> getObjectType() {
        return SqlSessionFactory.class;
    }
}
