package model;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

/**
 * @author T00032266
 * @DateTime 2021/5/17
 */
//@Component
public class MyFactoryBean implements FactoryBean<Person> {
	@Override
	public Person getObject() throws Exception {
		return null;
	}

	@Override
	public Class<?> getObjectType() {
		return Person.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
