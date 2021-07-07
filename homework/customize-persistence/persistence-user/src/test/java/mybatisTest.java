import mapper.PersonMapper;
import model.Person;
import org.junit.Test;
import resources.Resources;
import sessionfactory.SqlSessionFactory;
import sessionfactory.SqlSessionFactoryBuilder;
import sqlsession.SqlSession;

import java.io.InputStream;

public class mybatisTest {

    @Test
    public void test() throws Exception{
        //解析配置文件流
        InputStream resourceAsStream = Resources.getResourceAsStream("sqlMapConfig.xml");
        //获取sqlSession
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(resourceAsStream);
        SqlSession sqlSession = sqlSessionFactory.openSession();
        SqlSession sqlSession2 = sqlSessionFactory.openSession();
        Person person = Person.builder().id(1).name("张三").age(18).sex(1).build();
        Object result1 = sqlSession.select("mapper.PersonMapper.selectOne", false, person);
        sqlSession.commit();
        Object result2 = sqlSession2.select("mapper.PersonMapper.selectOne", false, person);

        PersonMapper mapper = sqlSession.getMapper(PersonMapper.class);
        mapper.insertPerson(person);
    }
}
