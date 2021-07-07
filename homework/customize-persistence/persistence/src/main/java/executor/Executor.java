package executor;

import model.Configuration;

import java.util.List;

public interface Executor {
   /**
    * 查询执行方法
    */
   <T> List<T> select(String statementId, Object... params) throws Exception;

   int update(String statementId, Object... params) throws Exception;

   int insert(String statementId, Object... params) throws Exception;

   int delete(String statementId, Object... params) throws Exception;

   void commit();
}
