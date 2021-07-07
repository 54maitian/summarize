package sessionfactory;

import configbuilder.XMLConfigBuilder;
import model.Configuration;
import resources.Resources;

import java.io.InputStream;

/** 用与创建SqlSessionFactory，工厂模式 */
public class SqlSessionFactoryBuilder {

  public SqlSessionFactory build(InputStream inputStream) throws Exception {
    // 通过配置文件解析对象，获取configuration
    XMLConfigBuilder xmlConfigBuilder = new XMLConfigBuilder();
    Configuration configuration = xmlConfigBuilder.parseConfig(inputStream);

    // 返回默认实现
    return new DefaultSqlSessionFactory(configuration);
  }
}
