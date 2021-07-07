package sessionfactory;

import sqlsession.SqlSession;

/**
 * SqlSessionFactory接口：用于创建SqlSession
 */
public interface SqlSessionFactory {
    /**
     * 获取SqlSession
     * @return
     */
    SqlSession openSession();
}
