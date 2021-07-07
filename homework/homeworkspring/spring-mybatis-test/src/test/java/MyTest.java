import com.learn.MyConfiguration;
import com.learn.annotation.Configuration;
import com.learn.dao.PersonDao;
import com.learn.factory.impl.AnnotationConfigApplicationContext;

/**
 * @author T00032266
 * @DateTime 2021/7/6
 */
public class MyTest {

    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(MyConfiguration.class);
        PersonDao personDao = (PersonDao)applicationContext.getBean("personDao");
        personDao.tranfer();
    }
}
