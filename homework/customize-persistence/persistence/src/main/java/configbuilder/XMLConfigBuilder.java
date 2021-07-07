package configbuilder;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.AllArgsConstructor;
import model.Configuration;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import resources.Resources;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/** 解析sqlMapConfig.xml配置文件 */
public class XMLConfigBuilder {
  private Configuration configuration;

  /** 构造函数默认创建configuration */
  public XMLConfigBuilder() {
    this.configuration = new Configuration();
  }

  /**
   * 解析config配置文件流数据
   *
   * @param inputStream
   * @return
   * @throws Exception
   */
  public Configuration parseConfig(InputStream inputStream) throws Exception {
    // 解析流文件，获取document对象
    Document document = new SAXReader().read(inputStream);

    // 获取根节点，对应<configuration>
    Element rootElement = document.getRootElement();

    // 获取对应属性<property>: database配置
    // 添加//表示向内部查找
    List<Element> list = rootElement.selectNodes("//property");

    // 创建properties封装解析结果
    Properties properties = new Properties();

    // 遍历解析,获取属性配置
    for (Element element : list) {
      String name = element.attributeValue("name");
      String value = element.attributeValue("value");
      properties.setProperty(name, value);
    }

    // 创建数据源，此处使用c3p0连接池，并设置对应连接属性
    ComboPooledDataSource dataSource = new ComboPooledDataSource();
    dataSource.setDriverClass(properties.get("dirverClass").toString());
    dataSource.setJdbcUrl(properties.get("url").toString());
    dataSource.setUser(properties.get("username").toString());
    dataSource.setPassword(properties.get("password").toString());

    // 将数据源设置到configuration
    configuration.setDataSource(dataSource);

    // 解析对应mapper配置
    List<Element> mapperList = rootElement.selectNodes("//mapper");

    for (Element element : mapperList) {
      // 获取mapper配置对应路径
      String mapperConfigPath = element.attributeValue("resource");
      // 解析配置文件为stream流
      InputStream mapperStream = Resources.getResourceAsStream(mapperConfigPath);
      // 创建XMLMapperBuilder用于解析mapper配置文件流
      XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(configuration);
      // 解析文件流
      xmlMapperBuilder.parseMapper(mapperStream);
    }

    return configuration;
  }
}
