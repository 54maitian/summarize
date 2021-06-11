package model;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * @author T00032266
 * @DateTime 2021/5/17
 */
//@Component
public class MyBeanPostProcessor implements BeanPostProcessor {


	public MyBeanPostProcessor() {
    System.out.println("BeanPostProcessor 初始化");
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    	System.out.println("BeanPostProcessor 前置处理" + beanName);
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		System.out.println("BeanPostProcessor 后置处理" + beanName);
		return bean;
	}
}
