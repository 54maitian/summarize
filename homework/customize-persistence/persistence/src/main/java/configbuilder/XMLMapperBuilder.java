package configbuilder;

import cache.Cache;
import lombok.AllArgsConstructor;
import model.Configuration;
import model.MapperStatement;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.util.List;

/** 解析mapper配置文件 */
@AllArgsConstructor
public class XMLMapperBuilder {

  private Configuration configuration;

  /**
   * 解析mapper文件流数据
   *
   * @param inputStream
   * @throws Exception
   */
  public void parseMapper(InputStream inputStream) throws Exception {
    // 解析流文件，获取document对象
    Document document = new SAXReader().read(inputStream);

    // 获取根节点，对应<mapper>
    Element rootElement = document.getRootElement();

    // 获取nameSpace
    String nameSpace = rootElement.attributeValue("nameSpace");
    configuration.addMapper(Class.forName(nameSpace));

    // 二级缓存
    Cache cache = null;
    List<Element> cacheList = rootElement.selectNodes("//cache");
    if (cacheList != null && cacheList.size() > 0) {
      cache = new Cache();
      configuration.addCache(cache);
    }

    // 获取<select>配置，此处仅实现select查询功能
    List<Element> list = rootElement.selectNodes("//select | //update | //delete | //insert");

    // 解析select标签
    for (Element element : list) {
      // 获取sql类型
      String sqlType = element.getName();
      // 获取sqlId
      String id = element.attributeValue("id");
      String parameterType = element.attributeValue("parameterType");
      String resultType = element.attributeValue("resultType");
      String sql = element.getTextTrim();

      // 获取sql全局唯一id
      String statementId = nameSpace + "." + id;

      // 创建MapperStatement对象
      MapperStatement mapperStatement =
          MapperStatement.builder()
              .id(id)
              .parameterType(parameterType)
              .resultType(resultType)
              .sql(sql)
              .sqlType(sqlType)
              .cache(cache)
              .build();

      // 将解析结果保存
      configuration.getMappedStatementMap().put(statementId, mapperStatement);
    }
  }
}
