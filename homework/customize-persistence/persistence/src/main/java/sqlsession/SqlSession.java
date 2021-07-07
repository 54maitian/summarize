package sqlsession;

import model.Configuration;

import java.util.List;

public interface SqlSession {

  /** 自定义的查询方法，有兴趣可以扩展crud */
  <T> T select(String statementId, boolean isList, Object... params) throws Exception;

  int update(String statementId, Object... params) throws Exception;

  int insert(String statementId, Object... params) throws Exception;

  int delete(String statementId, Object... params) throws Exception;

  Configuration getConfiguration();

  /** 通过jdk动态代理生成mapper的代理对象，则无需自定义mapper实现类 */
  <T> T getMapper(Class<T> mapperClass);

  void commit();
}
