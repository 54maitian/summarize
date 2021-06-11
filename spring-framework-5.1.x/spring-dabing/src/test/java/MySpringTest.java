import config.MyConfiguration;
import model.Person;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author T00032266 @DateTime 2021/5/17
 */
public class MySpringTest {
	public static void main(String[] args) {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("my-spring-beans.xml");
		//AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(MyConfiguration.class);
		Person person = (Person) applicationContext.getBean("person");
		person.doSomeThing();
		applicationContext.close();
	}
}
