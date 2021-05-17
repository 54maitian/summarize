# spring重点

## IOC

```
- 控制反转
	- 对象的创建不需要自动使用new关键字创建，而是通过配置，spring加载配置文件，创建BeanFactory容器，通过解析配置，将需要的对象加载并初始化保存到容器中，在使用时从容器中获取
```

## DI

```
- 依赖注入
	- BeanFactory容器管理的对象，对其依赖对象，spring自动进行依赖注入(即进行属性设置)
	- 容器全权负责组件的装配，它会把符合依赖关系的对象通过属性（JavaBean中的setter）或者是构造子传递给需要的对象
```

## 容器初始化

### BeanFactory

```
- 容器顶级接口
	- 定义一些spring容器的功能规范
```

```
- 容器初始化
	- org.springframework.context.support.AbstractApplicationContext#refresh
    - org.springframework.context.support.AbstractApplicationContext#obtainFreshBeanFactory
    - org.springframework.context.support.AbstractRefreshableApplicationContext#refreshBeanFactory
    - org.springframework.context.support.AbstractRefreshableApplicationContext#createBeanFactory
- 具体工作实现类
	- DefaultListableBeanFactory
- 常用实现类
	- 加载xml配置文件
        - ClassPathXmlApplicationContext
            - 使用classpath类路径
		- FileSystemXmlApplicationContext
			- 使用绝对路径(不推荐)
	- 解析注释
    	- AnnotationConfigApplicationContext
    		- 配置Configuration配置类
```

#### DefaultListableBeanFactory

```java
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
		implements ConfigurableListableBeanFactory, BeanDefinitionRegistry, Serializable {
    //beanDefinition容器
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);
    //bean后处理器容器
    private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();
}
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

