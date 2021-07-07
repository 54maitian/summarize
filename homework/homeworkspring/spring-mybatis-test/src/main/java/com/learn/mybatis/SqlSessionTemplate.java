package com.learn.mybatis;

import com.learn.annotation.Component;
import resources.Resources;
import sessionfactory.SqlSessionFactory;
import sessionfactory.SqlSessionFactoryBuilder;
import sqlsession.SqlSession;

import java.io.InputStream;

/**
 * @author T00032266
 * @DateTime 2021/7/6
 */
@Component
public class SqlSessionTemplate {

    private SqlSessionFactory sqlSessionFactory = null;

    SqlSession openSqlSession() throws Exception {
        if (sqlSessionFactory == null) {
            //解析配置文件流
            InputStream resourceAsStream = Resources.getResourceAsStream("sqlMapConfig.xml");
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(resourceAsStream);
        }
        return sqlSessionFactory.openSession();
    }
}
