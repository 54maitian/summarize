package model;

import cache.Cache;
import cache.TransactionCacheManager;
import lombok.Getter;
import lombok.Setter;
import mapperproxy.MapperProxyFactory;
import sqlsession.SqlSession;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class Configuration {
  // 数据源对象
  private DataSource dataSource;

  // key：statementId；value：sql配置解析结果对象
  private Map<String, MapperStatement> mappedStatementMap = new HashMap();

  public Map<Class, MapperProxyFactory> knownMappers = new HashMap<Class, MapperProxyFactory>();

  public Map<String, Cache> caches = new HashMap<>();

  public  <T> void addMapper(Class<T> type) {
    knownMappers.put(type, new MapperProxyFactory<T>(type));
  };

  public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
    MapperProxyFactory<T> mapperProxyFactory = knownMappers.get(type);
    return mapperProxyFactory.newInstance(sqlSession);
  }

  public void addCache(Cache cache) {
    caches.put(cache.getId(), cache);
  }

  public Cache getCache(String id) {
    return caches.get(id);
  }
}
