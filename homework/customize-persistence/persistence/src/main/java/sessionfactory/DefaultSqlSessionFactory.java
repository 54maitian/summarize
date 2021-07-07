package sessionfactory;

import lombok.AllArgsConstructor;
import model.Configuration;
import sqlsession.DefaultSqlSession;
import sqlsession.SqlSession;

/**
 * SqlSessionFactory默认实现
 */
@AllArgsConstructor
public class DefaultSqlSessionFactory implements SqlSessionFactory {

    private Configuration configuration;

    /**
     * 获取sqlSession
     * @return
     */
    public SqlSession openSession() {
        //返回sqlSession默认实现
        return new DefaultSqlSession(configuration);
    }
}
