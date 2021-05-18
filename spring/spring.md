# Spring重点

## Spring IOC

```bash
# IOC(控制反转)
	# 取代传统通过new关键字创建对象的方式
	# 对象通过spring容器来创建和管理
# DI(依赖注入)
	# 容器将管理的Spring Bean对象依赖的其他对象进行注入
```

## 配置

### XML配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
		xmlns:context="http://www.springframework.org/schema/context"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="
		http://www.springframework.org/schema/beans
		http://www.springframework.org/schema/beans/spring-beans.xsd
		http://www.springframework.org/schema/context
		http://www.springframework.org/schema/context/spring-context.xsd">

	<!--可以引用其他资源，统一入口-->
	<import resource="aa.xml"/>

	<!--开启注解扫描-->
	<context:component-scan base-package=""/>

	<!--
		bean标签：声明一个bean对象，由spring容器加载
		id：bean唯一标志
		class：全限定类名
		lazy-init：是否懒加载；true：懒加载
		scope：生命范围
			singleton：单例bean，对应一个spring容器只有一个，容器管理bean的整个生命周期
			prototype：多例bean，每次获取都会创建一个，容器只负责创建bean，不负责管理bean的销毁
		init-method：bean初始化处理方法
		destroy-method：bean销毁方法
	-->

	<bean id="person"
		  class="model.Person"
		  lazy-init="true"
		  scope="singleton"
		  init-method="xmlInitMethod"
		  destroy-method="xmlDestroyMethod"
	>
		<!--仅仅配置class，此时是通过构造器进行对象实例化-->
		<!--如果不配置构造参数，则是使用无参构造实例化。也可配置构造参数，使用有参构造进行实例化-->
		<!--构造参数实例化，实际也是通过构造函数实现依赖注入-->
		<!--
		 	name：构造参数名称(推荐)
		 	index：构造参数序号(不推荐)
		 	value：对于基础数据类型可以直接赋值
		 	ref：对于对象类型可以引用bean对象
		 -->
		<constructor-arg name="" index="" value="" ref="" />

		<!--配置property，表示使用set方法进行依赖注入-->
		<!--
			name：属性名称
			value：对于基础数据类型可以直接赋值
		 	ref：对于对象类型可以引用bean对象
		-->
		<property name="" ref="" value=""/>

		<property name="">
			<!--注入Array数组、Set类型、List类型，三者标签可串用-->
			<!--value：基本数据类型；ref：引用对象-->
			<array>
				<value></value>
				<ref bean="profession"></ref>
			</array>
			<set>
				<value></value>
			</set>
			<list>
				<value></value>
			</list>

			<!--注入Map、Properties类型-->
			<!---->
			<map>
				<entry key="" value="" key-ref="" value-ref=""/>
			</map>
			<props>
				<prop key="">value</prop>
			</props>
		</property>
	</bean>

	<!-- 配置class + factory-method(静态方法)，表示使用静态方法进行bean实例化，方法返回值作为bean -->
	<bean id="profession" class="model.Profession" factory-method="aspectOf"/>

	<!-- 配置 factory-bean(引用对象) + factory-method(实例方法)，表示使用引用对象的实例方法返回值作为bean -->
	<bean id="profession1" factory-bean="profession" factory-method="getAge"/>

</beans>
```

### 注解配置

```bash
# @Configuration
	# 该注解标识在类上，表示当前类是一个配置类
# @Bean	
	# 该注解标识在方法上，表示当前方法返回值由spring管理
# @Component
	# 该注解标识在类上，通知spring管理这个类
	# 变形
		# @Controller
		# @Service
		# @Repository
# @ComponentScan
	# 开启包扫描
# @Autowired
	# 依赖注入
		# 默认按照类型匹配注入
	# 通过该注解实现构造方法注入和set方法注入，可以标识在有参构造方法上、set()方法上、属性上。
# @Qualifier
	# 配合@Autowired使用，解决类型冲突
# @Scope(value="")
	# bean作用域
# @Lazy	
	# 开启懒加载
# @ImportResource	
	# 引入xml配置内容
```

## Spring 容器

### BeanFactory

```bash
# 容器顶级接口
	# 定义一些spring容器的功能规范
```

#### ApplicationContext

```bash
# 实现了BeanFactory的容器高级接口
	# 常用子类
		# 解析xml
			# ClassPathXmlApplicationContext
				# 使用classpath类路径加载xml
			# FileSystemXmlApplicationContext
				# 使用绝对路径加载xml(不推荐)
		# 基于注解配置
			# AnnotationConfigApplicationContext
				# 加载Configuration配置类
```

#### AbstractApplicationContext

```bash
# ApplicationContext抽象子类
# 内部定义refresh方法具体实现
	# 为spring容器具体功能实现入口
```

```java
public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {
    //BeanFactory后置处理器集合
    private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();
    //同步监视器的“刷新”和“销毁”
	private final Object startupShutdownMonitor = new Object();
    
    @Override
	public void refresh() throws BeansException, IllegalStateException {
		//具体功能实现入口
	}
}
```

#### DefaultListableBeanFactory

```bash
# 成熟的BeanFactory
	# 由我们使用的spring容器对象持有
		# AbstractRefreshableApplicationContext
			# private DefaultListableBeanFactory beanFactory;
	# 真正进行加载Bean元数据进行对象创建及管理的对象
```

```java
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
		implements ConfigurableListableBeanFactory, BeanDefinitionRegistry, Serializable {
    //beanDefinition容器
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);
    //单例池，用于保存实例化后的单例bean
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
    //bean后处理器容器
    private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();
}
```

### BeanDefinition

```bash
# bean定义的描述对象
	# 具体实现
		# org.springframework.beans.factory.support.RootBeanDefinition
```

```java
public class RootBeanDefinition extends AbstractBeanDefinition {
    //bean的Class类
    private volatile Object beanClass;
    //bean的作用域
    private String scope = SCOPE_DEFAULT;
    //是否延迟加载
    private boolean lazyInit = false;
    //初始化方法名称
    private String initMethodName;
    //销毁方法名称
    private String destroyMethodName;
}
```

### BeanFactory创建

实现类：均为org.springframework.beans.factory.support.DefaultListableBeanFactory

#### xml解析方式

```bash
# org.springframework.context.support.AbstractApplicationContext#refresh
    # org.springframework.context.support.AbstractApplicationContext#obtainFreshBeanFactory
        # org.springframework.context.support.AbstractRefreshableApplicationContext#refreshBeanFactory
     	   # org.springframework.context.support.AbstractRefreshableApplicationContext#createBeanFactory
```

#### 注解解析方式

```bash
# AnnotationConfigApplicationContext的父类GenericApplicationContext的构造器中
```

### BeanDefinitions加载

#### xml解析方式

```bash
# org.springframework.context.support.AbstractApplicationContext#refresh
	# org.springframework.context.support.AbstractApplicationContext#obtainFreshBeanFactory
		# org.springframework.context.support.AbstractRefreshableApplicationContext#refreshBeanFactory
			# org.springframework.context.support.AbstractRefreshableApplicationContext#loadBeanDefinitions
```

#### 注解解析方式

```bash
# 加载@Configuration注解注释的配置类
	# AnnotationConfigApplicationContext初始化参数
	# 此时仅仅加载具体配置类的BeanDefinition

# org.springframework.context.annotation.AnnotationConfigApplicationContext#AnnotationConfigApplicationContext
	# org.springframework.context.annotation.AnnotationConfigApplicationContext#register
		# org.springframework.context.annotation.AnnotatedBeanDefinitionReader#doRegisterBean
			# 实例化BeanDefinition
				# new AnnotatedGenericBeanDefinition(beanClass)
			# 注册BeanDefinition到BeanFactory中
				# org.springframework.beans.factory.support.BeanDefinitionReaderUtils#registerBeanDefinition
					# org.springframework.beans.factory.support.DefaultListableBeanFactory#registerBeanDefinition
```









#### 加载BeanDefinitions

```
- 解析配置加载为BeanDefinition
- 注册BeanDefinition到BeanFactory
	- 添加到beanDefinitionMap
- 代码跟踪
    - org.springframework.context.support.AbstractRefreshableApplicationContext#loadBeanDefinitions
    - org.springframework.beans.factory.xml.XmlBeanDefinitionReader#doLoadBeanDefinitions
    - org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#doRegisterBeanDefinitions
    - org.springframework.beans.factory.support.DefaultListableBeanFactory#registerBeanDefinition
    - org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#doRegisterBeanDefinitions
    - org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#parseBeanDefinitions
    - org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#parseDefaultElement
    - org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#processBeanDefinition
    - org.springframework.beans.factory.xml.BeanDefinitionParserDelegate#parseBeanDefinitionElement
    - org.springframework.beans.factory.xml.BeanDefinitionParserDelegate#createBeanDefinition
```

## BeanFactoryPostProcessor

```
- BeanFactory后处理器接口
	- postProcessBeanFactory
		- BeanFactory实例化后处理
- Bean实例化前，加载BeanFactoryPostProcessor，通过postProcessBeanFactory方法对BeanFactory进行增强处理
	- org.springframework.context.support.AbstractApplicationContext#refresh
	- org.springframework.context.support.AbstractApplicationContext#invokeBeanFactoryPostProcessors
	- org.springframework.context.support.PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors
        - 从BeanFactory中获取对应BeanFactoryPostProcessor类型的定义
            - org.springframework.beans.factory.ListableBeanFactory#getBeanNamesForType
        - 获取BeanFactoryPostProcessor实例
        	- 进行实例化并返回
        	- org.springframework.beans.factory.BeanFactory#getBean
        - 遍历BeanFactoryPostProcessor实例，调用postProcessBeanFactory方法
        	- org.springframework.context.support.PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors
```

## BeanPostProcessor

```
- bean后处理器接口
	- postProcessBeforeInitialization
		- bean初始化前处理
	- postProcessAfterInitialization
		- bean初始化后处理
- 实例化/注册
	- org.springframework.context.support.AbstractApplicationContext#refresh
	- org.springframework.context.support.AbstractApplicationContext#registerBeanPostProcessors
	- org.springframework.context.support.PostProcessorRegistrationDelegate#registerBeanPostProcessors
	- 从BeanFactory中获取对应BeanPostProcessor类型的定义
		- org.springframework.beans.factory.ListableBeanFactory#getBeanNamesForType
	- 获取BeanPostProcessor实例
		- org.springframework.beans.factory.BeanFactory#getBean
	- 注册BeanPostProcessor到BeanFactory
		- org.springframework.context.support.PostProcessorRegistrationDelegate#registerBeanPostProcessors
		- org.springframework.beans.factory.config.ConfigurableBeanFactory#addBeanPostProcessor
```

### postProcessBeforeInitialization

```
- 初始化前处理器
	- org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean	
		- bean实例化具体调用
	- org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#initializeBean
		- bean初始化
	- org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#applyBeanPostProcessorsBeforeInitialization
		- 初始化前处理
	- org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization
```

### postProcessAfterInitialization

```
- 初始化后处理器
	- org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean	
		- bean实例化具体调用
	- org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#initializeBean
		- bean初始化
	- org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#applyBeanPostProcessorsAfterInitialization
		- 初始化后处理
	- org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization
```

## InitializingBean

```
- Bean初始化接口
	- afterPropertiesSet
		- 在bean实例化并设置属性后的初始化处理
- 代码调用
	- org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean	
		- bean实例化具体调用
	- org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#initializeBean
		- bean初始化
	- org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#invokeInitMethods
    	- 如果bean是InitializingBean类型，则调用
    		- org.springframework.beans.factory.InitializingBean#afterPropertiesSet
```

## Bean实例化

```
- Bean分类
	- 类型
        - 普通bean
        - FactoryBean
	- scope
		- singleton
			- 单例bean，由spring容器管理整个生命周期，对应一个spring容器只有一个共享bean
		- prototype
        	- 多例bean，spring容器只创建，不管理生命周期
```

### singleton

```
- 容器创建时，单例bean实例化
	- org.springframework.context.support.AbstractApplicationContext#refresh
	- org.springframework.context.support.AbstractApplicationContext#finishBeanFactoryInitialization
	- org.springframework.beans.factory.support.AbstractBeanFactory#doGetBean
		- 单例bean会调用addSingleton将其添加到BeanFactory中
	- org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#getSingleton
	- org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
	- org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBeanInstance
	- org.springframework.beans.factory.support.SimpleInstantiationStrategy#instantiate
		- 通过反射获取构造器
	- java.lang.reflect.Constructor#newInstance
    	- 进行实例化
- 属性设置
	- org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
	- org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#populateBean
    - 执行set方法
```

### prototype

```
- 在使用时，通过容器进行创建
	- org.springframework.context.support.AbstractApplicationContext#getBean(java.lang.String)
	- org.springframework.beans.factory.BeanFactory#getBean
	- org.springframework.beans.factory.support.AbstractBeanFactory#doGetBean
		- 在此方法，不同scope，
```

