package model;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

/** @author T00032266 @DateTime 2021/5/17 */
//@Component
public class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

  public MyBeanFactoryPostProcessor() {
    System.out.println("BeanFactoryPostProcessor 初始化");
  }

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
      throws BeansException {
    System.out.println("BeanFactoryPostProcessor 处理");
  }
}
