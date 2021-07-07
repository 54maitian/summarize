package mapperproxy;

import lombok.AllArgsConstructor;
import sqlsession.SqlSession;

import java.lang.reflect.Proxy;

@AllArgsConstructor
public class MapperProxyFactory<T> {

  private Class<T> mapperInterface;

  public T newInstance(SqlSession sqlSession) {
    return (T)
        Proxy.newProxyInstance(
            this.getClass().getClassLoader(), new Class[] {mapperInterface}, new MapperProxy(sqlSession));
  }
}
