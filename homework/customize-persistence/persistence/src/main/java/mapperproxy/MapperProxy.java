package mapperproxy;

import lombok.AllArgsConstructor;
import model.Configuration;
import model.MapperStatement;
import sqlsession.SqlSession;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

@AllArgsConstructor
public class MapperProxy implements InvocationHandler {

  SqlSession sqlSession;

  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // 获取方法名称
    String methodName = method.getName();
    // 获取方法所在类名称
    String className = method.getDeclaringClass().getName();
    // 获取statementId
    String statementId = className + "." + methodName;

    Configuration configuration = sqlSession.getConfiguration();
    Map<String, MapperStatement> mappedStatementMap = configuration.getMappedStatementMap();
    MapperStatement mapperStatement = mappedStatementMap.get(statementId);

    String sqlType = mapperStatement.getSqlType();
    Object reuslt;
    switch (sqlType) {
      case "select":
        // 获取返回值类型
        Type genericReturnType = method.getGenericReturnType();
        // 根据类型是否是泛型判断返回结果是否list
        boolean isList = genericReturnType instanceof ParameterizedType;
        reuslt = sqlSession.select(statementId, isList, args);
        break;
      case "update":
        reuslt = sqlSession.update(statementId,args);
        break;
      case "delete":
        reuslt = sqlSession.delete(statementId,args);
        break;
      case "insert":
        reuslt = sqlSession.insert(statementId,args);
        break;
      default:
        throw new RuntimeException("未知sqlType：" + sqlType);
    }
    return reuslt;
  }
}
