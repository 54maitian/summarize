package executor;

import cache.Cache;
import model.BoundSql;
import model.Configuration;
import model.MapperStatement;
import utils.GenericTokenParser;
import utils.ParameterMapping;
import utils.ParameterMappingTokenHandler;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class SimpleExecutor implements Executor {
  private Configuration configuration;

  private Cache localCache;

  public SimpleExecutor(Configuration configuration) {
    this.configuration = configuration;
    this.localCache  = new Cache();
  }

  /** 真正的jdbc操作 */
  public <T> List<T> select(String statementId, Object... params)
      throws Exception {

    //一级缓存中获取
    Object o = localCache.getObject(statementId);
    if (o != null) {
      return (List<T>) o;
    }

    // 获取sql对应mapperStatement对象
    MapperStatement mapperStatement = configuration.getMappedStatementMap().get(statementId);

    PreparedStatement statement = this.getStatement(statementId, params);

    // 进行结果获取
    ResultSet resultSet = statement.executeQuery();
    // 获取结果集类型
    String resultType = mapperStatement.getResultType();
    Class<?> resultClass = getClassType(resultType);

    // 进行结果集封装
    List resultList = new ArrayList();
    while (resultSet.next()) {
      // 获取元数据
      ResultSetMetaData metaData = resultSet.getMetaData();
      // 创建响应结果对象
      Object result = resultClass.newInstance();
      for (int i = 1; i < metaData.getColumnCount(); i++) {
        // 获取字段名称
        String columnName = metaData.getColumnName(i);
        // 获取字段值
        Object value = resultSet.getObject(columnName);
        // 通过内省方式设置值
        PropertyDescriptor propertyDescriptor = new PropertyDescriptor(columnName, resultClass);
        Method writeMethod = propertyDescriptor.getWriteMethod();
        writeMethod.invoke(result, value);
      }
      // 将结果入列表
      resultList.add(result);
    }

    //一级缓存存储
    localCache.putObject(statementId, resultList);

    return resultList;
  }

  @Override
  public int update(String statementId, Object... params) throws Exception {
    PreparedStatement statement = this.getStatement(statementId, params);
    statement.execute();
    return statement.getUpdateCount();
  }

  @Override
  public int insert(String statementId, Object... params) throws Exception {
    return this.update(statementId, params);
  }

  @Override
  public int delete(String statementId, Object... params) throws Exception {
    return this.update(statementId, params);
  }

  @Override
  public void commit() {
    localCache.clear();
  }

  private PreparedStatement getStatement(String statementId, Object... params)  throws Exception{
    // 获取数据库连接
    Connection connection = configuration.getDataSource().getConnection();
    // 获取sql对应mapperStatement对象
    MapperStatement mapperStatement = configuration.getMappedStatementMap().get(statementId);

    // 获取原始sql：select t.* from person t where t.id = #{id} and t.name = #{name}
    String sql = mapperStatement.getSql();
    // 进行sql解析
    // 将#{content} 替换为 占位符 ？
    // 将#{content} 中参数名称获取
    BoundSql boundSql = getBoundSql(sql);

    // sql预编译
    PreparedStatement preparedStatement = connection.prepareStatement(boundSql.getSql());

    // 进行参数设置
    List<ParameterMapping> mappingList = boundSql.getMappingList();
    // 获取入参类型
    String parameterType = mapperStatement.getParameterType();
    Class<?> parameterClass = getClassType(parameterType);

    // 此处使用fori，需要获取下标使用
    for (int i = 0; i < mappingList.size(); i++) {
      ParameterMapping parameterMapping = mappingList.get(i);
      // 获取目标属性id
      String content = parameterMapping.getContent();
      // 通过反射，获取参数中对应id的值
      Field declaredField = parameterClass.getDeclaredField(content);
      declaredField.setAccessible(true);
      Object o = declaredField.get(params[0]);
      // 设置到参数中
      // jdbc中，占位符从1开始
      preparedStatement.setObject(i + 1, o);
    }
    return preparedStatement;
  }

  /**
   * 获取BoundSql对象
   *
   * @param sql
   * @return
   */
  private BoundSql getBoundSql(String sql) {
    // 标记处理类
    ParameterMappingTokenHandler tokenHandler = new ParameterMappingTokenHandler();
    GenericTokenParser genericTokenParser = new GenericTokenParser("#{", "}", tokenHandler);
    // 解析之后的sql
    String parse = genericTokenParser.parse(sql);
    // 解析出的参数名称
    List<ParameterMapping> parameterMappings = tokenHandler.getParameterMappings();
    return new BoundSql(parse, parameterMappings);
  }

  /**
   * 获取clasName对应class对象
   *
   * @param clasName
   * @return
   */
  private Class<?> getClassType(String clasName) throws Exception {
    if (clasName != null) {
      return Class.forName(clasName);
    }
    return null;
  }
}
