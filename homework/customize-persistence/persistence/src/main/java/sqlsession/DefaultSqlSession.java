package sqlsession;

import executor.CacheExecutor;
import executor.Executor;
import executor.SimpleExecutor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import model.Configuration;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class DefaultSqlSession implements SqlSession {
  private Configuration configuration;
  private Executor executor;

  public DefaultSqlSession(Configuration configuration) {
    this.configuration = configuration;
    this.executor = new SimpleExecutor(configuration);
    this.executor = new CacheExecutor(this.executor, configuration);
  }


  public <T> T select(String statementId, boolean isList, Object... params) throws Exception {

    List<Object> resultList = executor.select(statementId, params);
    if (!isList) {
      if (resultList.size() > 1) {
        throw new RuntimeException("查询结果过多");
      } else if (resultList.size() < 1) {
        // 无查询结果，返回null
        return null;
      } else {
        return (T) resultList.get(0);
      }
    }
    return (T) resultList;
  }

  @Override
  public int update(String statementId, Object... params) throws Exception {
    return executor.update(statementId, params);
  }

  @Override
  public int insert(String statementId, Object... params) throws Exception {
    return executor.insert(statementId, params);
  }

  @Override
  public int delete(String statementId, Object... params) throws Exception {
    return executor.delete(statementId, params);
  }


  /** 通过jdk动态代理生成mapper的代理对象，则无需自定义mapper实现类 */
  public <T> T getMapper(Class<T> mapperClass) {
    return configuration.getMapper(mapperClass, this);
  }

  public void commit(){
    executor.commit();
  }
}
