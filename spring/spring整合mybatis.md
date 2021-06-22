# Spring整合Mybatis

```bash
# Spring整合Mybatis主要通过两个类
	# SqlSessionFactoryBean
		# org.mybatis.spring.SqlSessionFactoryBean
		# 完成SqlSessionFactory的创建
```

## Spring主配置

```xml
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="
                           http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans-3.1.xsd
                           http://www.springframework.org/schema/context
                           http://www.springframework.org/schema/context/spring-context-3.1.xsd">

    <!--加载jdbc属性配置-->
    <context:property-placeholder location="classpath*:/jdbc.properties"/>

    <bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource">
        <property name="driverClassName" value="${database.driver}"/>
        <property name="url" value="${database.url}"/>
        <property name="username" value="${database.username}"/>
        <property name="password" value="${database.password}"/>
    </bean>

    <bean id="sessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
        <property name="mapperLocations" value="classpath:com.learn/mapper/*Mapper.xml"/>
        <property name="dataSource" ref="dataSource"/>
    </bean>

    <bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
        <property name="basePackage" value="com.learn.mapper"/>
        <property name="sqlSessionFactoryBeanName" value="sessionFactory"/>
    </bean>
</beans>
```

## 过程分析

```bash
# 由上述配置可知我们向Spring容器中注册了两个Bean，而两者的配置有对应
	# SqlSessionFactoryBean
	# MapperScannerConfigurer
	
# MapperScannerConfigurer
	# 实现BeanDefinitionRegistryPostProcessor
	# 扫描mapper接口包下mapper接口，创建其对应BeanDefinition
		# org.mybatis.spring.mapper.MapperScannerConfigurer#postProcessBeanDefinitionRegistry
		# org.springframework.context.annotation.ClassPathBeanDefinitionScanner#doScan
		
	# 1. 获取BeanDefinition
    	# org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider#findCandidateComponents
    # 2. 将获取到的BeanDefinition注册到BeanFactory中
    	# org.springframework.context.annotation.ClassPathBeanDefinitionScanner#registerBeanDefinition
    # 3. 注册完毕，将获取的BeanDefinition进行处理，设置为MapperFactoryBeans
    	# org.mybatis.spring.mapper.ClassPathMapperScanner#processBeanDefinitions
    	
```





