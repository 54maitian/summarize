package model;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/** @author T00032266 @DateTime 2021/5/17 */
@Component
public class Person implements InitializingBean {

  public Person() {
    System.out.println("person 实例化");
  }


  	public void doSomeThing() {
	  System.out.println("doSomeThing");
  }

	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("实例化处理");
	}
}
