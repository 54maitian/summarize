# Spring重点

# Spring IOC

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

- Spring容器顶级接口
- 定义一些spring容器的功能规范
- org.springframework.beans.factory.BeanFactory

### ApplicationContext

- 实现了BeanFactory的容器高级接口

- 实现了一些结构，为Spring容器提供了额外功能

- org.springframework.context.ApplicationContext

### AbstractApplicationContext

- ApplicationContext抽象子类

- 内部定义refresh方法具体实现
  - spring容器具体功能实现入口

```java
//org.springframework.context.support.AbstractApplicationContext
public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {
    //BeanFactory后置处理器集合
    private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();
    //同步监视器的“刷新”和“销毁”
	private final Object startupShutdownMonitor = new Object();
    //持有真正进行Bean数据加载的容器对象
    private DefaultListableBeanFactory beanFactory;
    
    @Override
	public void refresh() throws BeansException, IllegalStateException {
		//具体功能实现入口
	}
    
    /* 注册BeanFactoryPostProcessor */
    public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
		this.beanFactoryPostProcessors.add(postProcessor);
	}
}
```

### 常用实现类

```bash
# 常用子类
	# 解析xml
        # ClassPathXmlApplicationContext
            # org.springframework.context.support.ClassPathXmlApplicationContext
            # 使用classpath类路径加载xml
        # FileSystemXmlApplicationContext
            # org.springframework.context.support.FileSystemXmlApplicationContext
            # 使用绝对路径加载xml(不推荐)
	# 基于注解配置
        # AnnotationConfigApplicationContext
            # org.springframework.context.annotation.AnnotationConfigApplicationContext
            # 加载Configuration配置类
```

### DefaultListableBeanFactory

- 成熟的BeanFactory

- 真正进行加载Bean元数据进行对象创建及管理的对象
  - 由我们使用的spring容器对象AbstractRefreshableApplicationContext持有

```java
//org.springframework.beans.factory.support.DefaultListableBeanFactory
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
		implements ConfigurableListableBeanFactory, BeanDefinitionRegistry, Serializable {
    //beanDefinition容器
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);
    //单例池，用于保存实例化后的单例bean
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
    
    
    
    /* 注册beanDefinition */
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeanDefinitionStoreException {
        this.beanDefinitionMap.put(beanName, beanDefinition);
    }
}
```

### AbstractBeanFactory

- BeanFactory实现类

- DefaultSingletonBeanRegistry实现类
  - 提供单例bean注册功能
  - org.springframework.beans.factory.support.DefaultSingletonBeanRegistry

```java
//org.springframework.beans.factory.support.AbstractBeanFactory
public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport implements ConfigurableBeanFactory {
    //bean后处理器容器
    private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();
    
    /* 注册BeanPostProcessor实例 */
    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
        this.beanPostProcessors.add(beanPostProcessor);
    }
}
```

### BeanDefinition

- bean定义的描述对象的顶级接口
- 定义了描述对象的具体行为

- org.springframework.beans.factory.config.BeanDefinition

### AbstractBeanDefinition

- BeanDefinition抽象实现类
- 提供大量BeanDefinition的属性和行为方法

```java
//org.springframework.beans.factory.support.AbstractBeanDefinition
public abstract class AbstractBeanDefinition extends BeanMetadataAttributeAccessor implements BeanDefinition, Cloneable {
    //bean对应类型
    private volatile Object beanClass;
    //bean生命周期
    private String scope = SCOPE_DEFAULT;
    //工厂方法名称
    private String factoryMethodName;
    //工厂bean名称
    private String factoryBeanName;
}
```

### GenericBeanDefinition

- 常用的AbstractBeanDefinition实现类
- org.springframework.beans.factory.support.GenericBeanDefinition

## 流程解析

Spring加载单例过程主要为以下几个部分：

- Spring容器创建
- BeanDefinitions加载
- BeanFactoryPostProcessor
  - 获取BeanDefinition
  - 实例化
  - 后置处理
    - 通过postProcessBeanFactory方法对BeanFactory进行增强处理
    - 主要是操作BeanDefinitions
- BeanPostProcessor加载
  - 获取获取BeanDefinition
  - 实例化
- Bean实例化
- BeanPostProcessor前置处理

### BeanFactory创建

实现类：均为org.springframework.beans.factory.support.DefaultListableBeanFactory

#### xml解析方式

```bash
# org.springframework.context.support.AbstractApplicationContext#refresh
    # org.springframework.context.support.AbstractApplicationContext#obtainFreshBeanFactory
        # org.springframework.context.support.AbstractRefreshableApplicationContext#refreshBeanFactory
     	   # org.springframework.context.support.AbstractRefreshableApplicationContext#createBeanFactory
     	   # new DefaultListableBeanFactory()
```

#### 注解解析方式

```bash
# AnnotationConfigApplicationContext的父类GenericApplicationContext的构造器中
```

### BeanDefinitions加载

#### xml解析方式

```bash
# 加载xml配置中对应所有bean配置
# 注意关键点方法即可

# 入口
	# org.springframework.context.support.AbstractApplicationContext#refresh
		# org.springframework.context.support.AbstractApplicationContext#obtainFreshBeanFactory
			# org.springframework.context.support.AbstractRefreshableApplicationContext#refreshBeanFactory
				# org.springframework.context.support.AbstractRefreshableApplicationContext#loadBeanDefinitions
				
# 资源加载为Document对象
	# org.springframework.beans.factory.xml.XmlBeanDefinitionReader#doLoadBeanDefinitions
	
# 执行BeanDefinitions注册
	# org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#doRegisterBeanDefinitions
	# org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#parseDefaultElement
		# 区别不同标签执行不同操作
		
# 针对bean标签进行处理
	# org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#processBeanDefinition
		# 将标签节点解析为BeanDefinition对象
			# org.springframework.beans.factory.xml.BeanDefinitionParserDelegate#parseBeanDefinitionElement
		# 将解析后的BeanDefinition注册
			# org.springframework.beans.factory.support.BeanDefinitionReaderUtils#registerBeanDefinition
			# org.springframework.beans.factory.support.DefaultListableBeanFactory#registerBeanDefinition
			# 注册到beanDefinitionMap中
				# this.beanDefinitionMap.put(beanName, beanDefinition);
```

#### 注解解析方式

```bash
# 加载@Configuration注解注释的配置类
	# AnnotationConfigApplicationContext初始化参数
	# 此时仅仅加载具体配置类的BeanDefinition

# org.springframework.context.annotation.AnnotationConfigApplicationContext#AnnotationConfigApplicationContext
	# org.springframework.context.annotation.AnnotationConfigApplicationContext#register
		# AnnotatedBeanDefinitionReader：接手具体工作
		# org.springframework.context.annotation.AnnotatedBeanDefinitionReader#doRegisterBean
			# 实例化BeanDefinition
				# new AnnotatedGenericBeanDefinition(beanClass)
			# 注册BeanDefinition到BeanFactory中
				# org.springframework.beans.factory.support.BeanDefinitionReaderUtils#registerBeanDefinition
					# org.springframework.beans.factory.support.DefaultListableBeanFactory#registerBeanDefinition
						# this.beanDefinitionMap.put(beanName, beanDefinition)
```

#### 解析结果

```bash
# 解析后的BeanDefinition统一保存在DefaultListableBeanFactory的beanDefinitionMap
	# 可以跟踪注册方法，获取我们想要的信息
	# org.springframework.beans.factory.support.DefaultListableBeanFactory#registerBeanDefinition
# 针对于不同的bean配置方法，解析后BeanDefinition内容不同
    # 构造创建
        # beanClass
            # 全限定类型，即可获取对应构造器进行对象创建
    # 静态方法创建
        # beanClass
        # factoryMethodName
            # 根据全限定类名和对应静态方法名称，进行对象创建
    # 实例方法创建
        # factoryBeanName
        # factoryMethodName
            # 获取对应的实例工厂对象，调用对应实例方法进行对象创建
```

### BeanFactoryPostProcessor后置处理

#### BeanFactoryPostProcessor

- BeanFactory的后置处理器顶级接口
- 通过BeanFactoryPostProcessor在实例化BeanFactory后对其进行增强操作
- 函数式接口

```java
//org.springframework.beans.factory.config.BeanFactoryPostProcessor
@FunctionalInterface
public interface BeanFactoryPostProcessor {
    /* 接收beanFactory */
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;
}
```

#### 实例化及调用

```bash
# 入口
	# org.springframework.context.support.AbstractApplicationContext#refresh
	# org.springframework.context.support.AbstractApplicationContext#invokeBeanFactoryPostProcessors
	# org.springframework.context.support.PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors

# 1.定义加载
	# 从BeanFactory中获取BeanFactoryPostProcessor类型的BeanDefinition
	# org.springframework.beans.factory.ListableBeanFactory#getBeanNamesForType
	
# 2.实例创建
	# 创建BeanFactoryPostProcessor实例
	# org.springframework.beans.factory.BeanFactory#getBean
	
# 3.后置处理
	# 后置处理方法postProcessBeanFactory调用
	# org.springframework.context.support.PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors
	
# 注：此流程为自定义BeanFactoryPostProcessor的标准流程
	# 存在特殊BeanFactoryPostProcessor不一定经过此流程进行处理
	# 可以跟踪其postProcessBeanFactory方法
```

#### 自定义BeanFactoryPostProcessor

```bash
# 实现自定义BeanFactoryPostProcessor并且让其工作需要两步
    # 实现BeanFactoryPostProcessor接口
    # 配置将其交由spring管理
```

#### 



### BeanDefinitionRegistryPostProcessor

```java
//通过前置处理的方式，向BeanFactory容器中注册BeanDefinition
public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {
	void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException;
}
```

#### 触发时机

```bash
# 执行时机优先于BeanFactoryPostProcessor
# org.springframework.context.support.AbstractApplicationContext#refresh
	# org.springframework.context.support.AbstractApplicationContext#invokeBeanFactoryPostProcessors
		# org.springframework.context.support.PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors
			# 获取BeanDefinitionRegistryPostProcessor类型BeanDefinition
				# org.springframework.beans.factory.ListableBeanFactory#getBeanNamesForType
			# 实例化BeanDefinitionRegistryPostProcessor
            	# org.springframework.beans.factory.BeanFactory#getBean
			# org.springframework.context.support.PostProcessorRegistrationDelegate#invokeBeanDefinitionRegistryPostProcessors
				# 遍历执行BeanDefinitionRegistryPostProcessor的postProcessBeanDefinitionRegistry方法
```

### ConfigurationClassPostProcessor

```bash
# 实现了BeanDefinitionRegistryPostProcessor接口
	# 实现注解模式下的包扫描

# AnnotationConfigApplicationContext构造时加载其BeanDefinition
	# org.springframework.context.annotation.AnnotationConfigApplicationContext#AnnotationConfigApplicationContext()
        # org.springframework.context.annotation.AnnotatedBeanDefinitionReader#AnnotatedBeanDefinitionReader
            # org.springframework.context.annotation.AnnotationConfigUtils#registerAnnotationConfigProcessors
                # 负责创建一系列bean，如果有自定义的则跳过
                # org.springframework.context.annotation.AnnotationConfigUtils#registerPostProcessor
                    # org.springframework.beans.factory.support.DefaultListableBeanFactory#registerBeanDefinition
                                        
# 执行BeanDefinitionRegistryPostProcessor的postProcessBeanDefinitionRegistry(包扫描处理)
	# 加载配置类BeanDefinition，获取包扫描注解ComponentScan/ComponentScans，进行包扫描(加载BeanDefinition)
	# org.springframework.context.annotation.ConfigurationClassPostProcessor#postProcessBeanDefinitionRegistry
		# org.springframework.context.annotation.ConfigurationClassPostProcessor#processConfigBeanDefinitions
            # ConfigurationClassParser：接手
                # org.springframework.context.annotation.ConfigurationClassParser#parse
                    # org.springframework.context.annotation.ConfigurationClassParser#processConfigurationClass
                        # org.springframework.context.annotation.ConfigurationClassParser#doProcessConfigurationClass
                            # 获取包扫描注解
            # ComponentScanAnnotationParser：接手，进行包扫描处理
                # org.springframework.context.annotation.ComponentScanAnnotationParser#parse
                    # org.springframework.context.annotation.ClassPathBeanDefinitionScanner#doScan
                        # 扫描包下对象并解析为BeanDefinition
                            # findCandidateComponents()
                        # 将解析的BeanDefinition进行注册
                            # org.springframework.context.annotation.ClassPathBeanDefinitionScanner#registerBeanDefinition
			# 将现有的BeanDefinition进行判断，如果其方法有@Bean注释的，则创建BeanDefinition
				# factoryBeanName：对应BeanDefinition的id名称
					# 静态方法：不设置factoryBeanName，设置BeanClassName
				# factoryMethodName：对应方法名称，作为此BeanDefinition的id
				# org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader#loadBeanDefinitions
					# ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForConfigurationClass
						# ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForBeanMethod
							# org.springframework.beans.factory.support.BeanDefinitionRegistry#registerBeanDefinition
    
# 执行BeanFactoryPostProcessor的postProcessBeanFactory	
	# 将配置类BeanDefinition进行CGLIB的增强包装
	# org.springframework.context.annotation.ConfigurationClassPostProcessor#postProcessBeanFactory
		# org.springframework.context.annotation.ConfigurationClassPostProcessor#enhanceConfigurationClasses
			# org.springframework.context.annotation.ConfigurationClassEnhancer#enhance
				# 返回一个增强后的Class：enhancedClass替换原BeanDefinition的Class
				# 实现了将配置类实例进行包装
```



### BeanPostProcessor加载

#### BeanPostProcessor

- 在bean实例化之后，进行后处理的顶级接口

```java
//org.springframework.beans.factory.config.BeanPostProcessor
public interface BeanPostProcessor {
    //bean初始化前处理
	default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}
    //bean初始化后处理
	default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}
}
```

#### 加载注册

```bash
# 入口
	# org.springframework.context.support.AbstractApplicationContext#refresh
	# org.springframework.context.support.AbstractApplicationContext#registerBeanPostProcessors
	# org.springframework.context.support.PostProcessorRegistrationDelegate#registerBeanPostProcessors
	
# 1.加载定义	
	# 从BeanFactory中获取BeanPostProcessor类型定义
    # org.springframework.beans.factory.ListableBeanFactory#getBeanNamesForType
    
# 2.实例化
	# org.springframework.beans.factory.BeanFactory#getBean
	
# 3.注册
	# 将创建的BeanPostProcessor实例保存到AbstractBeanFactory的beanPostProcessors中
	# org.springframework.context.support.PostProcessorRegistrationDelegate#registerBeanPostProcessors
	# org.springframework.beans.factory.config.ConfigurableBeanFactory#addBeanPostProcessor
	
# 注：可以从两个点跟踪BeanPostProcessor加载过程
	# BeanPostProcessor实例化构造调用
	# BeanPostProcessor注册
		# beanPostProcessors.add(beanPostProcessor)
```

### Bean实例化过程

- 解析加载的BeanDefinition，创建对应实例对象
- 实例化后的Bean还不是成熟的SpringBean
- 此处是针对于单例bean
  - 单例bean由Spring容器管理，在容器创建过程中实例化

```bash
# 入口
	# org.springframework.context.support.AbstractApplicationContext#refresh
	# org.springframework.context.support.AbstractApplicationContext#finishBeanFactoryInitialization
	# org.springframework.beans.factory.config.ConfigurableListableBeanFactory#preInstantiateSingletons
	# org.springframework.beans.factory.support.AbstractBeanFactory#getBean
	
# Spring容器创建对象的入口
	# org.springframework.beans.factory.support.AbstractBeanFactory#doGetBean
	# 根据传入的beanName获取BeanDefinition
		# org.springframework.beans.factory.support.AbstractBeanFactory#getMergedLocalBeanDefinition
    	
# 实例创建入口
	# org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#getSingleton
	# 1.判断单例池是否已存在此对象
		# singletonObjects.get(beanName)
	# 2.调用传入getSingleton的一个ObjectFactory的lambda对象的getObject方法获取实例对象
		# org.springframework.beans.factory.ObjectFactory#getObject
	# 3. 实例注册到单例池
		# org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#addSingleton

# 上述 2 中ObjectFactory工作内容
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBean
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBeanInstance

# 实例创建:createBeanInstance
	# 根据配置条件使用不同方法进行实例创建
	# 1.构造创建
		# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#instantiateBean
		# 获取到对应构造器对象
			# org.springframework.beans.factory.support.SimpleInstantiationStrategy#instantiate
			# 由class类型获取构造器
				# java.lang.Class#getDeclaredConstructor
		# 调用newInstance()
			# org.springframework.beans.BeanUtils#instantiateClass
			# java.lang.reflect.Constructor#newInstance
	# 2.
	
# 注
	# spring框架中真正操作的方法往往是 doXxx() 的形式
	# 理解单例池看下面的循环依赖
```

#### 涉及类库

##### ObjectFactory

- 函数式接口
- 一个工厂接口

```java
//org.springframework.beans.factory.ObjectFactory
@FunctionalInterface
public interface ObjectFactory<T> {
	/* 调用方法获取一个对象 */
	T getObject() throws BeansException;
}
```











### BeanPostProcessor前置处理

#### BeanPostProcessor

- 在bean实例化之后，进行后处理的顶级接口

```java

```







## BeanPostProcessor

```java
//在bean实例化之后，进行后处理的顶级接口
public interface BeanPostProcessor {
    //bean初始化前处理
	default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}
    //bean初始化后处理
	default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}
}
```

### 自定义BeanPostProcessor

```bash
# 实现BeanPostProcessor接口
# 交由spring管理
```

### 实例化/注册

```bash
# org.springframework.context.support.AbstractApplicationContext#refresh
	# org.springframework.context.support.AbstractApplicationContext#registerBeanPostProcessors
	# PostProcessorRegistrationDelegate：接手
		# org.springframework.context.support.PostProcessorRegistrationDelegate#registerBeanPostProcessors
			# 获取BeanPostProcessor类型BeanDefinition
				# beanFactory.getBeanNamesForType
			# 实例化Bean
				# beanFactory.getBean
			# 注册到BeanFactory
				# org.springframework.context.support.PostProcessorRegistrationDelegate#registerBeanPostProcessors
					# org.springframework.beans.factory.config.ConfigurableBeanFactory#addBeanPostProcessor
                    	# this.beanPostProcessors.add(beanPostProcessor)
```

### 触发时机

#### postProcessBeforeInitialization

```bash
# bean实例化具体调用位置
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
	# bean进行初始化
		# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#initializeBean
        # 初始化前处理
        	# AbstractAutowireCapableBeanFactory#applyBeanPostProcessorsBeforeInitialization
        		# org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization
```

#### postProcessAfterInitialization

```bash
# bean实例化具体调用位置
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
	# bean进行初始化
		# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#initializeBean
		# 具体初始化操作
			# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#invokeInitMethods
				# org.springframework.beans.factory.InitializingBean#afterPropertiesSet
        # 初始化后处理
        	# AbstractAutowireCapableBeanFactory#applyBeanPostProcessorsAfterInitialization
        		# org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization
```

### ApplicationContextAwareProcessor

```bash
# 实现了BeanPostProcessor接口
	# 可以对实现了部分Aware接口的bean对象，调用其对应的set方法
	# org.springframework.context.support.ApplicationContextAwareProcessor#postProcessBeforeInitialization
		# org.springframework.context.support.ApplicationContextAwareProcessor#invokeAwareInterfaces

# 有些Aware接口方法调用不是通过其BeanPostProcessor实现类进行
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
		# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#initializeBean
			# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#invokeAwareMethods
```

### CommonAnnotationBeanPostProcessor

```bash
# 对应@PostConstruct、@PreDestroy注解使用
	# 注解配置模式会自动配置此BeanPostProcessor
		# org.springframework.context.annotation.AnnotationConfigUtils#registerAnnotationConfigProcessors
	# xml配置模式需要手动配置bean定义

# 实现了类InitDestroyAnnotationBeanPostProcessor
	# postProcessBeforeInitialization
		# 执行@PostConstruct注释的初始化方法
# 实现了DestructionAwareBeanPostProcessor
	# postProcessBeforeDestruction
		# 执行@PreDestroy注释的预销毁方法
```

### AutowiredAnnotationBeanPostProcessor

```bash
# 实现@Autowired注入的后置处理器
	# 实现了MergedBeanDefinitionPostProcessor接口
# 创建时机
	# org.springframework.context.annotation.AnnotatedBeanDefinitionReader#AnnotatedBeanDefinitionReader
		# org.springframework.context.annotation.AnnotationConfigUtils#registerAnnotationConfigProcessors
			# 此处会注册一系列BeanPostProcessor的BeanDefinition
	# org.springframework.context.support.AbstractApplicationContext#refresh
		# org.springframework.context.support.AbstractApplicationContext#registerBeanPostProcessors
			# 此处加载注册BeanPostProcessor
				# org.springframework.beans.factory.support.AbstractBeanFactory#beanPostProcessors
# 调用时机
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
		# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#applyMergedBeanDefinitionPostProcessors
			# 调用MergedBeanDefinitionPostProcessor类型的BeanPostProcessor的postProcessMergedBeanDefinition
# 具体逻辑
	# org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor#postProcessMergedBeanDefinition
		# org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor#buildAutowiringMetadata
```

## InitializingBean

```java
//bean初始化顶级接口
public interface InitializingBean {
    //在bean实例化并设置属性后调用
	void afterPropertiesSet() throws Exception;
}
```

### 触发时机

```bash
# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#initializeBean
		# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#invokeInitMethods
			# 如果bean是InitializingBean类型子类，则调用afterPropertiesSet
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

### 实例化时机

#### singleton

```bash
# 容器创建时，实例化所有非懒加载单例bean
	# org.springframework.context.support.AbstractApplicationContext#refresh
		# org.springframework.context.support.AbstractApplicationContext#finishBeanFactoryInitialization
	# ConfigurableListableBeanFactory接手
		# org.springframework.beans.factory.config.ConfigurableListableBeanFactory#preInstantiateSingletons
			# 此处判断为单例模式才会执行后续的bean实例化动作
	# AbstractBeanFactory接手
		# org.springframework.beans.factory.support.AbstractBeanFactory#doGetBean
			# org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#getSingleton
				# 此处会先从单例池中获取，如果有则直接返回，所以单例bean对应容器只会创建一次
	# AbstractAutowireCapableBeanFactory接手：最终处理
		# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
```

#### prototype

```bash
# 在进行使用时，才会进行创建，每次获取都会创建一个新的
	# org.springframework.beans.factory.support.AbstractBeanFactory#getBean
		# org.springframework.beans.factory.support.AbstractBeanFactory#doGetBean
			# 执行后续doCreateBean流程
```

### doCreateBean

```bash
# createBeanInstance：创建Bean实例
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBeanInstance
		# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#instantiateUsingFactoryMethod
			# org.springframework.beans.factory.support.InstantiationStrategy#instantiate
			# SimpleInstantiationStrategy：为具体代码调用者，通过重载的instantiate方法进行调用
				# org.springframework.beans.factory.support.SimpleInstantiationStrategy#instantiate
					# 静态方法/实例方法
						# java.lang.reflect.Method#invoke
					# 构造器模式
						# 获取构造器
							# java.lang.Class#getDeclaredConstructor
						# 创建实例
							# java.lang.reflect.Constructor#newInstance
# populateBean：进行属性设置
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#populateBean

# initializeBean：进行属性初始化
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#initializeBean
```

## 延迟加载

```bash
# 如果单例Bean定义设置了延迟加载，则将在第一次获取使用时才会实例化
	# 实际获取单例bean，先会到单例池singletonObjects中获取，如果获取不到就会进行创建
	# 判断逻辑
		# org.springframework.context.support.AbstractApplicationContext#refresh
            # org.springframework.beans.factory.config.ConfigurableListableBeanFactory#preInstantiateSingletons
                # if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) 
```

## 循环依赖问题

```bash
# Bean相互持有依赖
# 解决
	# 单例
		# 构造器参数循环依赖
			# 报错BeanCurrentlyInCreationException
		# setter方式循环依赖
			# 三级缓存实现
	# 多例
		# 由于多例bean不由spring容器管理，无法进行循环依赖解决
```

### 三级缓存

#### 主要数据存储

```java
//org.springframework.beans.factory.support.DefaultSingletonBeanRegistry

/** Cache of singleton objects: bean name --> bean instance（缓存单例实例化对象的Map集合） */
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<String, Object>(64);

/** Cache of singleton factories: bean name --> ObjectFactory（单例的工厂Bean缓存集合） */
private final Map<String, ObjectFactory> singletonFactories = new HashMap<String, ObjectFactory>(16);

/** Cache of early singleton objects: bean name --> bean instance（早期的单身对象缓存集合） */
private final Map<String, Object> earlySingletonObjects = new HashMap<String, Object>(16);

/** Set of registered singletons, containing the bean names in registration order（单例的实例化对象名称集合） */
private final Set<String> registeredSingletons = new LinkedHashSet<String>(64);
/**
 * 添加单例实例到三级缓存
 * 解决循环引用的问题
 */
protected void addSingletonFactory(String beanName, ObjectFactory singletonFactory) {
	Assert.notNull(singletonFactory, "Singleton factory must not be null");
	synchronized (this.singletonObjects) {
		if (!this.singletonObjects.containsKey(beanName)) {
			this.singletonFactories.put(beanName, singletonFactory);
			this.earlySingletonObjects.remove(beanName);
			this.registeredSingletons.add(beanName);
		}
	}
}

/**
 * 获取单例实例
 */
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    //先从单例池获取
    Object singletonObject = this.singletonObjects.get(beanName);
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        synchronized (this.singletonObjects) {
            //从二级缓存获取
            singletonObject = this.earlySingletonObjects.get(beanName);
            if (singletonObject == null && allowEarlyReference) {
                ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                if (singletonFactory != null) {
                    //由三级缓存中获取，如有则从三级缓存中移动到二级缓存
                    singletonObject = singletonFactory.getObject();
                    this.earlySingletonObjects.put(beanName, singletonObject);
                    this.singletonFactories.remove(beanName);
                }
            }
        }
    }
    return singletonObject;
}

/**
 * 将单例bean存储到单例池
 */
protected void addSingleton(String beanName, Object singletonObject) {
    synchronized (this.singletonObjects) {
        this.singletonObjects.put(beanName, singletonObject);
        this.singletonFactories.remove(beanName);
        this.earlySingletonObjects.remove(beanName);
        this.registeredSingletons.add(beanName);
    }
}
```

### 具体思路

```bash
# 假设 A B 互相依赖
	# 实例化所有单例bean
		# org.springframework.context.support.AbstractApplicationContext#refresh
			# org.springframework.context.support.AbstractApplicationContext#finishBeanFactoryInitialization
				# org.springframework.beans.factory.support.DefaultListableBeanFactory#preInstantiateSingletons
	# 1. 创建A实例
		# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
		# 2. A调用构造进行实例化
			# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBeanInstance
			# 此时A对象已经创建，只是对应B对象依赖没有设置
		# 3. 调用addSingletonFactory将A放入三级缓存中
			# org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#addSingletonFactory
		# 4. A实例化后进行属性设置
			# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#populateBean
			# 属性设置时，因为引用对象B没有进行创建，则需要创建依赖对象
	# 5. 创建B实例
		# 6. B调用构造进行实例化
		# 7. 调用addSingletonFactory将B放入三级缓存中
		# 8. B实例化后进行属性设置
			# 9. 获取B对应A属性依赖
			# 10. 调用getSingleton获取A实例
				# 此时A提前暴露在三级缓存中，则可以从三级缓存中获取A实例，并转移至二级缓存中
			# 11. 将A通过set方法设置到B中
				# 此时B对象中存在A对象依赖，B对象完成依赖注入已经是一个成熟实例了
				# A对象在二级缓存中保存
				# B对象在三级缓存中保存
			# 12. B创建完成，调用addSingleton保存到单例池中
				# org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#addSingleton
	# 13. 此时B创建完成，将返回的B对象设置到A对象中，A对象也成为成熟实例
	# 14. A创建完成，调用addSingleton保存到单例池中
```

### 注意

```bash
# 循环依赖对象进行toString方法调用，会进入死循环
```

# Spring AOP

```bash
# 通过代理的方式，实现目标类方法调用的增强，从而达到将一些通用逻辑抽取出来，实现解耦
# 通用逻辑将会以切面切入的方式进行调用
```

## AOP术语

|      |      |
| ---- | ---- |
|      |      |
|      |      |
|      |      |
|      |      |
|      |      |
|      |      |
|      |      |

## 配置

### xml配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
	   xmlns:aop="http://www.springframework.org/schema/aop"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
    http://www.springframework.org/schema/beans/spring-beans.xsd
http://www.springframework.org/schema/aop
    http://www.springframework.org/schema/aop/spring-aop.xsd">

	<!--开启aop注解扫描-->
	<aop:aspectj-autoproxy/>

	<!--提供advice增强功能的类-->
	<bean id="advice" class="aop.MyAOPAdvice"/>

	<!--aop配置-->
	<aop:config>
		<!--配置切点：表示要进行增强的点
			id：切点id
			expression：接收一个execution表达式，定位具体切点方法
		-->
		<aop:pointcut id="pc" expression="execution(* *..*.*(..))"/>
		<!--切面配置
			id：切面id
			ref：切面对应advice增强类
		-->
		<aop:aspect id="dodo" ref="advice">
			<!-- 前置通知：方法调用前-->
			<aop:before method="before" pointcut-ref="pc"/>
			<!-- 后置通知：方法正常返回-->
			<aop:after-returning method="afterReturning" pointcut-ref="pc"/>
			<!-- 异常通知：方法报错后，类似于catch内 -->
			<aop:after-throwing method="afterThrowing" pointcut-ref="pc"/>
			<!--最终通知：方法最终执行，类似于finally内-->
			<aop:after method="after" pointcut-ref="pc"/>
			<!--环绕通知-->
			<aop:around method="around" pointcut-ref="pc"/>
		</aop:aspect>
	</aop:config>

</beans>
```

### 注解配置

```java
//配置类上开启aop注解驱动
@EnableAspectJAutoProxy
```

```java
package aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * @author T00032266
 * @DateTime 2021/5/20
 */
//声明一个advice增强类
@Aspect
@Component
public class MyAOPAdvice {

	//使用一个空方法声明切点
	@Pointcut("execution(* * *..*.*(..))")
	public void pointcut(){};

	//前置通知
	//jp 连接点的基本信息
	//result 获取连接点的返回对象
	@Before("pointcut()")
	public void before(JoinPoint jp) {
		System.out.println("前置通知");
	}

	//最终通知
	@After("pointcut()")
	public void after() {
		System.out.println("前置通知");
	}

	//后置通知
	@AfterReturning(value = "pointcut()",returning = "msg")
	public void afterReturning(JoinPoint jp,Object msg) {
		System.out.println("前置通知");
	}

	//异常通知
	@AfterThrowing(value = "pointcut()",throwing = "ex")
	public void afterThrowing(Throwable ex) {
		System.out.println("前置通知");
	}

	//环绕通知
	// pjp 对连接点的方法内容进行整体控制
	//@Around("pointcut()")
	public Object  around(ProceedingJoinPoint pjp) throws Throwable {
		System.out.println("环绕before");
		Object proceed = null;
		try {
			proceed = pjp.proceed();
			System.out.println("环绕afterReturning");
		} catch (Throwable throwable) {
			System.out.println("环绕afterThrowing");
			throw throwable;
		} finally {
			System.out.println("环绕after");
		}

		return proceed;
	}
}
```

## AOP实现原理

```bash
# AnnotationAwareAspectJAutoProxyCreator
	# 父抽象类AbstractAutoProxyCreator实现了BeanPostProcessor
	
# BeanDefinition加载时机
	# 解析配置时，如果是开启aop注解驱动
		# 调用org.springframework.beans.factory.xml.NamespaceHandlerSupport#registerBeanDefinitionParser
			# parsers.push('aspectj-autoproxy',AspectJAutoProxyBeanDefinitionParser);
		# 调用org.springframework.aop.config.AspectJAutoProxyBeanDefinitionParser#parse
			# org.springframework.aop.config.AopNamespaceUtils#registerAspectJAnnotationAutoProxyCreatorIfNecessary
				# 注册一个AnnotationAwareAspectJAutoProxyCreator到beanDefinitionMap
# 进行BeanPostProcessor的后置处理器方法，将目标对象进行代理		
	# org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#postProcessAfterInitialization
		# org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#wrapIfNecessary
			# 解析对应Advice增强类的方法，获取到对应的切面方法
				# org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#getAdvicesAndAdvisorsForBean
			# 创建目标对象(符合切点)的代理对象
				# 建立一个代理工厂对象
					# org.springframework.aop.framework.ProxyFactory
				# 创建代理对象
					# org.springframework.aop.framework.ProxyFactory#getProxy
						# org.springframework.aop.framework.DefaultAopProxyFactory#createAopProxy
							# 会根据条件返回
								# org.springframework.aop.framework.JdkDynamicAopProxy
								# org.springframework.aop.framework.ObjenesisCglibAopProxy
```







