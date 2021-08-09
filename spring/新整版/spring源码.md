# Spring IOC

## 主要概念

```bash
# IOC(控制反转)
	# 取代传统通过new关键字创建对象的方式
	# 对象通过spring容器来创建和管理
# DI(依赖注入)
	# 容器将管理的Spring Bean对象依赖的其他对象进行注入
	# 无需主动进行对象依赖的控制(依赖设置)
```



## Spring IOC 容器

`Spring IOC`主要的作用是在项目启动时，将程序所需对象进行预先创建，而无需在程序运行时通过 `new` 关键字进行对象创建



### BeanFactory

因为所需对象都在启动时预先创建，此时创建的对象应该有一个存储容器，它就是`Spring IOC` 容器：`BeanFactory`

`BeanFactory` 作为容器的顶级接口，定义了容器对于`Spring Bean`的基本操作功能

具体功能请查看接口：`org.springframework.beans.factory.BeanFactory`



### ApplicationContext

`BeanFactory`的实现接口，基于`BeanFactory`提供了更加丰富的功能

接口：`org.springframework.context.ApplicationContext`



## Spring IOC容器工作流程

对于`Spring IOC` 容器来说，有以下告知：

- 容器需要加载哪些预设`Bean`对象，供后续程序运行使用
- `IOC`容器创建`Bean`对象，不仅仅是通过`new`关键字进行对象创建，还包括一系列生命周期管理

对于容器如何加载预设`Bean`对象，通常我们使用以下两种配置方式：

- `XML`文档配置
- `Annotation`注解配置



## XML配置工作流程简述

`XML`配置方式的工作流程是比较清晰的，所以我们先分析此流程

工作流程主要分为四个部分：

- 准备部分
  - 创建`IOC`容器
  - 加载`XML`配置文件
  - 解析`XML`配置文件内容，生成对应`BeanDefinition`
  - `BeanFactoryPostProcessor`对象实例化
  - `BeanFactoryPostProcessor`对象对`IOC`容器进行后置处理
  - `BeanPostProcessor`对象实例化

- 创建实例
  - `Bean`对象实例化
  - `Bean`对象属性设置
- 实例后处理
  - `BeanPostProcessor`初始化前置处理
  - `Bean`对象初始化
  - `BeanPostProcessor`初始化后置处理
- 结束
  - `Bean`对象存储到`IOC`容器



### 使用方式

```java
public static void main(String[] args) {
    //容器创建：加载xml配置文件
    ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("my-spring-beans.xml");
    //从创建后的容器中获取bean实例
    Person person = (Person) applicationContext.getBean("person");
    person.doSomeThing();
    //容器关闭，销毁单例bean
    applicationContext.close();
}
```



### 1. IOC容器创建

实际我们在创建`IOC`容器时，会创建两个对象

- `ApplicationContext`
  - 上下文对象，主要进行容器初始化刷新动作
- `DefaultListableBeanFactory`
  - 真正IOC容器，管理`Bean`对象



#### ApplicationContext对象创建

`ApplicationContext`对象主要是进行`IOC`容器对象的刷新工作

对应于不同配置方式，解析方式不同，使用的子类也不同

常用子类：

- `XML`配置
  - `ClassPathXmlApplicationContext`
    - `org.springframework.context.support.ClassPathXmlApplicationContext`
    - 使用classpath类路径加载xml
  - `FileSystemXmlApplicationContext`
    - `org.springframework.context.support.FileSystemXmlApplicationContext`
    - 使用绝对路径加载xml(不推荐)
- `Annotation`注解配置
  - `AnnotationConfigApplicationContext`
    - `org.springframework.context.annotation.AnnotationConfigApplicationContext`
    - 加载`Configuration`配置类

这些类都是`AbstractApplicationContext`抽象类的实现子类



`ApplicationContext`对应实现结构类图

![image-20210803201518626](.\图片\applicationContext结构类图)



我们解析`XML`配置的常用子类就是`ClassPathXmlApplicationContext`，我们现在分析一下它的构造方法

```java
/**
 * configLocations：xml配置文件路径
 * refresh：是否自动刷新上下文，通常为true
 * parent：对应父ApplicationContext容器
 */
public ClassPathXmlApplicationContext(String[] configLocations, boolean refresh, @Nullable ApplicationContext parent){
    //设置父ApplicationContext
    super(parent);
    //设置配置文件路径
    setConfigLocations(configLocations);
    //刷新上下文
    if (refresh) {
        refresh();
    }
}
```

由此可见，`refresh`方法就是IOC容器工作的主要入口

`refresh`方法是继承自抽象父类`AbstractApplicationContext`，下面我们了解一下这个类



#### AbstractApplicationContext

`AbstractApplicationContext`作为`ApplicationContext`的抽象实现类，主要作用是定义`ApplicationContext`子类的刷新上下文的功能方法`refresh`，同时管理`BeanFactoryPostProcessor`对象

```java
// org.springframework.context.support.AbstractApplicationContext
public abstract class AbstractApplicationContext {
    //BeanFactory后置处理器集合
    private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();
    //持有DefaultListableBeanFactory容器对象
    private DefaultListableBeanFactory beanFactory;
    //xml资源路径，继承自org.springframework.context.support.AbstractRefreshableConfigApplicationContext
    private String[] configLocations;
    
    /* beanFactoryPostProcessor注册 */
    public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
		this.beanFactoryPostProcessors.add(postProcessor);
	}
    
    /* 容器具体工作入口：刷新上下文，下面整理了主要工作内容 */
    public void refresh() throws BeansException, IllegalStateException {
        // 创建beanFactory工厂，加载beanDefinitions.
        ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

        // 加载BeanFactoryPostProcessor，并实例化，后通过postProcessBeanFactory方法对BeanFactory进行增强处理
        invokeBeanFactoryPostProcessors(beanFactory);

        // 加载注册BeanPostProcessor
        registerBeanPostProcessors(beanFactory);

        // 实例化所有剩余的（非延迟初始化）单例。
        finishBeanFactoryInitialization(beanFactory);
    }
}
```

由上述`refresh`方法简介可知，`ClassPathXmlApplicationContext`通过`refresh`方法进行上下文刷新动作

同时我们发现`ApplicationContext`持有一个`DefaultListableBeanFactory`对象，那这个对象的作用是什么？



#### DefaultListableBeanFactory

真正的`IOC`容器，主要进行`BeanDefinition、Bean`的创建、管理动作，还提供了`BeanPostProcessor`的存储容器，是一个成熟的`BeanFactory`对象

```java
//org.springframework.beans.factory.support.DefaultListableBeanFactory
public class DefaultListableBeanFactory implements ConfigurableListableBeanFactory, BeanDefinitionRegistry {
    //BeanDefinition注册容器
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);
    //BeanPostProcessor实例对象集合
    private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();
    
    
    /* 注册BeanDefinition，继承自 BeanDefinitionRegistry 接口 */
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition){
        this.beanDefinitionMap.put(beanName, beanDefinition);
    }
    /* 添加BeanPostProcessor实例对象，继承自 org.springframework.beans.factory.support.AbstractBeanFactory */
    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
        this.beanPostProcessors.add(beanPostProcessor);
    }
    /* 从Spring容器中获取实例对象的入口 */
    public <T> T getBean(Class<T> requiredType) throws BeansException {}
}
```



我们发现`DefaultListableBeanFactory`并没有`SpringBean`的管理容器，这时我们查看`DefaultListableBeanFactory`的类图

![image-20210730202847229](.\图片\DefaultListableBeanFactory类图)

实际`SpringBean`的管理、注册是通过父类`DefaultSingletonBeanRegistry`实现，这个我们后续分析



`DefaultListableBeanFactory`创建的入口是`obtainFreshBeanFactory`方法

```java
//org.springframework.context.support.AbstractApplicationContext#obtainFreshBeanFactory
protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
    refreshBeanFactory();
    return getBeanFactory();
}

//org.springframework.context.support.AbstractRefreshableApplicationContext#refreshBeanFactory
protected final void refreshBeanFactory() throws BeansException {
    //初始化beanFactory，默认实现：DefaultListableBeanFactory
    DefaultListableBeanFactory beanFactory = createBeanFactory();
    //加载BeanDefinitions
    loadBeanDefinitions(beanFactory);
}

//org.springframework.context.support.AbstractRefreshableApplicationContext#createBeanFactory
protected DefaultListableBeanFactory createBeanFactory() {
    return new DefaultListableBeanFactory(getInternalParentBeanFactory());
}

默认创建DefaultListableBeanFactory
```



#### 作个小结

- `ApplicationContext`称为`IOC`容器上下文，主要负责创建`DefaultListableBeanFactory`，并通过`refresh`实现上下文刷新
  - 上下文刷新：就是进行容器初始化`Bean`的动作
- `DefaultListableBeanFactory`是真正进行`SpringBean`对象创建、管理的对象，它被`ApplicationContext`持有，为其提供`Bean`对象的操控能力



### 2. XML配置加载解析

此处添加一下`XML`文档配置示例：

#### XML配置

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



XML文件加载解析入口，就是上述`loadBeanDefinitions`方法

```java
跟踪调用链 
//1. org.springframework.context.support.AbstractXmlApplicationContext#loadBeanDefinitions

//2. org.springframework.context.support.AbstractXmlApplicationContext#loadBeanDefinitions
protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
    Resource[] configResources = getConfigResources();
    if (configResources != null) {
        reader.loadBeanDefinitions(configResources);
    }
    String[] configLocations = getConfigLocations();
    if (configLocations != null) {
        reader.loadBeanDefinitions(configLocations);
    }
}

我们发现后续工作委托给了XmlBeanDefinitionReader，如果是configLocations配置，则需要通过路径配置，加载文件为Resource
//org.springframework.beans.factory.support.AbstractBeanDefinitionReader#loadBeanDefinitions
public int loadBeanDefinitions(String location, @Nullable Set<Resource> actualResources){
	//加载location对应文件资源
	Resource[] resources = ((ResourcePatternResolver) resourceLoader).getResources(location);
	//继续解析
	int count = loadBeanDefinitions(resources);
}

Resource资源将会加载为InputStream文件流，再解析为xml对应Element节点，一一解析处理
//org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#parseBeanDefinitions
protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
    for (int i = 0; i < nl.getLength(); i++) {
        Node node = nl.item(i);
        if (node instanceof Element) {
            Element ele = (Element) node;
            //通过isDefaultNamespace方法判断，当前元素是否为http://www.springframework.org/schema/beans命名空间内容
            if (delegate.isDefaultNamespace(ele)) {
                parseDefaultElement(ele, delegate);
            }else {
                //其他命名空间，则需要特殊解析对象进行处理
                delegate.parseCustomElement(ele);
            }
        }
    }
}   

parseDefaultElement将根据不同元素，进行不同的解析
//org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#parseDefaultElement
private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
    if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) { //import
        importBeanDefinitionResource(ele);
    }
    else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) { //alias
        processAliasRegistration(ele);
    }
    else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) { //bean
        processBeanDefinition(ele, delegate);
    }
    else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) { //beans
        // recurse
        doRegisterBeanDefinitions(ele);
    }
}
```



### 3. BeanDefinition注册

在分析BeanDefinition注册前，我们先了解一下什么是BeanDefinition

由于`IOC`容器不同于普通使用`new`关键字创建，还包括一系列生命周期管理

而且对于预设`Bean`对象，不应是加载解析一个则创建一个，而是统一创建

所以，我们需要一个对象来缓存对于配置文件的解析结果，供后续对象创建使用，它就是`BeanDefinition`



#### BeanDefinition

`BeanDefinition`是一个`Bean`对象描述的顶级接口，它定义了许多`Bean`的信息操作功能

接口：`org.springframework.beans.factory.config.BeanDefinition`



#### AbstractBeanDefinition

`BeanDefinition`的抽象实现类，为子类配置了`BeanDefinition`的通用属性和行为

```java
//org.springframework.beans.factory.support.AbstractBeanDefinition
public abstract class AbstractBeanDefinition extends BeanMetadataAttributeAccessor implements BeanDefinition {
    //bean生命周期
    private String scope = SCOPE_DEFAULT;
    //bean对应类型
    private volatile Object beanClass;
    //工厂方法名称
    private String factoryMethodName;
    //工厂bean名称
    private String factoryBeanName;
}
```

常用子类：

- RootBeanDefinition
  - `org.springframework.beans.factory.support.RootBeanDefinition`
- GenericBeanDefinition
  - `org.springframework.beans.factory.support.GenericBeanDefinition`



#### BeanDefinition的具体解析

由上可知，`BeanDefinition`的解析入口为`processBeanDefinition`

```java
//org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#processBeanDefinition
protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
	//解析bean元素为BeanDefinition
	BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
	//将解析后的BeanDefinition注册到BeanFactory中，即DefaultListableBeanFactory中
	BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
}

具体解析在org.springframework.beans.factory.xml.BeanDefinitionParserDelegate#parseBeanDefinitionElement方法中
```



##### BeanDefinition注册

上述可知，`BeanDefinition`注册到`BeanFactory`中，是通过`BeanDefinitionReaderUtils#registerBeanDefinition`工具类方法实现

而实际注册就是`DefaultListableBeanFactory`处理的

```java
//org.springframework.beans.factory.support.DefaultListableBeanFactory#registerBeanDefinition
public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition){
    this.beanDefinitionMap.put(beanName, beanDefinition);
}

跟踪此方法进行调试可获取以下信息：
    1. 有哪些BeanDefinition注册到BeanFactory中
    2. 断点对应BeanDefinition的加载过程
```



#### BeanDefinitionRegistry

由上可知，我们通过`DefaultListableBeanFactory`可以注册管理`BeanDefinition`，具体通过`registerBeanDefinition`方法，而其实现自`BeanDefinitionRegistry`接口，`BeanDefinitionRegistry`接口主要定义了对于`BeanDefinition`的管理方法

```java
//org.springframework.beans.factory.support.BeanDefinitionRegistry
public interface BeanDefinitionRegistry extends AliasRegistry {
    /* 注册BeanDefinition */
    void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeanDefinitionStoreException;
    /* 获取BeanDefinition */
    BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;
    /* 获取所有注册的BeanDefinition名称 */
    String[] getBeanDefinitionNames();
}
```



此时，我们已经创建了`ApplicationContext`上下文、`DefaultListableBeanFactory`IOC容器，并且解析好`XML`配置、注册了`BeanDefinition`

正常来说，准备工作已经完成，我们后续着手进行`Bean`实例化即可。

不不不，格局小了！`Spring`不仅仅只是解析`XML`配置中的`Bean`，还提供了更加强大的功能，那就是通过`BeanFactoryPostProcessor`实现`BeanFactory`后处理



### 4. BeanFactoryPostProcessor实例化及后置处理

#### BeanFactoryPostProcessor

`BeanFactoryPostProcessor`就是一个允许在`BeanFactory`进行`BeanDefinition`注册后，还可以对已注册的`BeanDefinition`进行修改的接口

通常用于同一调整`BeanDefinition`中属性值

```java
//org.springframework.beans.factory.config.BeanFactoryPostProcessor
@FunctionalInterface
public interface BeanFactoryPostProcessor {
	/* 对BeanFactory进行后处理 */
	void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;
}
```



`BeanFactoryPostProcessor`将在前置与普通`SpringBean`进行实例化，并进行`postProcessBeanFactory`方法调用，处理入口：

```java
// 1. org.springframework.context.support.AbstractApplicationContext#invokeBeanFactoryPostProcessors
protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
    PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());
}

具体调用入口：
// 2. org.springframework.context.support.PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors
public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {
    
    //从BeanFactory容器中获取类型为BeanDefinitionRegistryPostProcessor的beanNames
    String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
    for (String ppName : postProcessorNames) {
        //遍历获取的beanNames，从BeanFactory中获取对应BeanDefinitionRegistryPostProcessor对象
        currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
    }
    //执行BeanDefinitionRegistryPostProcessor后置处理器
    invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
    
    
    //从BeanFactory容器中获取类型为BeanFactoryPostProcessor的beanNames
    String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);
    for (String postProcessorName : nonOrderedPostProcessorNames) {
        //遍历获取的beanNames，从BeanFactory中获取对应BeanFactoryPostProcessor对象
        nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
    }
    //执行BeanFactoryPostProcessor后置处理器
    invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);
}    

//org.springframework.context.support.PostProcessorRegistrationDelegate#invokeBeanDefinitionRegistryPostProcessors
private static void invokeBeanDefinitionRegistryPostProcessors(
    Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {
    for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
        //遍历执行postProcessBeanDefinitionRegistry方法
        postProcessor.postProcessBeanDefinitionRegistry(registry);
    }
}

//org.springframework.context.support.PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors
private static void invokeBeanFactoryPostProcessors(
    Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {
    for (BeanFactoryPostProcessor postProcessor : postProcessors) {
        //遍历执行postProcessBeanFactory方法
        postProcessor.postProcessBeanFactory(beanFactory);
    }
}
```



诶，明明是在讲述`BeanFactoryPostProcessor`，怎么乱入一个`BeanDefinitionRegistryPostProcessor`，这就要和你说道说道

#### BeanDefinitionRegistryPostProcessor

`BeanDefinitionRegistryPostProcessor`是`BeanFactoryPostProcessor`的实现接口，主要用于后置处理向`BeanFactory`中注册额外的`BeanDefinition`

此时大家会想，反正我`BeanFactoryPostProcessor`都已经是后置处理了，不如我直接进行`BeanDefinition`的注册

实际来说，我们通过接口定义规范，此时应保持接口功能单一，所以就有了`BeanDefinitionRegistryPostProcessor`

```java
//org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {
	/* 后置向BeanFactory中注册BeanDefinition */
	void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException;
}
```



#### 作个小结

- `BeanFactoryPostProcessor`
  - 用于修改已注册的`BeanDefinition`
- `BeanDefinitionRegistryPostProcessor`
  - 用于注册额外的`BeanDefinition`

所以`BeanDefinitionRegistryPostProcessor`先于`BeanFactoryPostProcessor`处理



### 5. BeanPostProcessor对象实例化

`BeanFactoryPostProcessor`已经实例化，且进行了后置处理，准备阶段还没结束吗？

这里就需要了解一下`BeanPostProcessor`接口，`BeanPostProcessor`接口定义了`Bean`实例对象在进行初始化操作前后进行特殊处理的功能

```java
//org.springframework.beans.factory.config.BeanPostProcessor
public interface BeanPostProcessor {
	/* 初始化前处理 */
	default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/*初始化后处理*/
	default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}
}
```



`BeanPostProcessor`针对于所有实例化`Bean`对象进行处理，所以对于`BeanPostProcessor`，我们不应该在后续需要使用时进行实例化，而是应统一实例化并进行缓存，供后续使用，入口

```java
// 1. org.springframework.context.support.AbstractApplicationContext#registerBeanPostProcessors
protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
    PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
}

// 2. org.springframework.context.support.PostProcessorRegistrationDelegate#registerBeanPostProcessors
public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {
    //从BeanFactory容器中获取类型为BeanPostProcessor的beanNames
    String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);
    for (String ppName : postProcessorNames) {
        //从BeanFactory容器中获取BeanPostProcessor对象
        BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
        priorityOrderedPostProcessors.add(pp);
    }
    //注册BeanPostProcessor
    registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);
}

// 3. 注册BeanPostProcessor到BeanFactory容器中
private static void registerBeanPostProcessors(
    ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {
    for (BeanPostProcessor postProcessor : postProcessors) {
        beanFactory.addBeanPostProcessor(postProcessor);
    }
}
```



实际注册类为抽象父类`AbstractBeanFactory`

```java
//org.springframework.beans.factory.support.AbstractBeanFactory
public abstract class AbstractBeanFactory  extends FactoryBeanRegistrySupport implements ConfigurableBeanFactory {
    //BeanPostProcessor存储容器
    private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();
    
    /* 注册BeanPostProcessor */
    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
		this.beanPostProcessors.add(beanPostProcessor);
	}
}    
```

至此，前期准备工作已完成，后续则进行实例化及其后续操作



### 6. Bean对象实例化

`Bean`对象实例化，就是创建需要的`Bean`对象，但是需注意以下两点：

- IOC容器初始化阶段只会实例化 单例、非延时加载的 `Bean`
- 此时`Bean`对象实例不是成熟的`SpringBean`

此时存在疑问，什么是单例？什么是延时加载？



什么叫单例？这就要描述一下`SpringBean`对象的作用域(scope)，作用域表示`SpringBean`的存在范围

#### 作用域

IOC容器最初提供了`单例(singleton)`和 `多例(prototype)`，在2.0之后又引入了三种在`Web`应用中使用的作用域，对应`WebApplicationContext`容器



##### 单例(singleton)

`SpringBean`默认的作用域，其具有以下特性：

- **对象实例数量**：singleton类型的bean定义，在一个容器中只存在一个共享实例，所有对该类型bean的依赖都引用这一单一实例
- **对象存活时间**：singleton类型bean定义，从容器启动，到它第一次被请求而实例化开始，只要容器不销毁或退出，该类型的单一实例就会一直存活



##### 多例(prototype)

多例对象是不适用于全局共享的，而是在使用时需要创建新实例，每次从容器中都能获取新的实例对象，其具有以下特性：

- **对象实例数量**：prototype类型的bean定义，不受IOC容器管理，每次使用时，将由容器创建新的实例对象
- **对象存活时间**：prototype类型bean定义，在程序运行使用时，由IOC容器创建，之后交由程序控制，不再受IOC容器管理



##### 请求域(request)

`web`应用的IOC容器，为每个HTTP请求，创建一个全新的`Request-Processor`对象供当前请求使用，请求结束后，对象销毁

##### 会话域(session)

Spring容器会为每个独立的`session`创建属于它们自己全新的`UserPreferences`对象实例

##### global session

只有应用在基于portlet的Web应用程序中才有意义，不深入了解



#### 延时加载

延时加载，表示不在容器初始化时进行对象实例化，而是在程序运行使用时，再进行实例创建

xml配置

```xml
<bean id="a" class="com.dabing.model.A" lazy-init="true"/>
```

对应`AbstractBeanDefinition#lazyInit`属性，默认值为`false`



#### Bean实例化

##### 实例化方式

此时我们将进行`Bean`的实例化动作，我们此时通过分析`XML`文件得到的`BeanDefinition`来进行实力化，那么我们能从`BeanDefinition`中得到哪些信息呢？

根据XML配置，我们通常的配置信息有

| bean标签        | BeanDefinition属性        | 含义                   |
| --------------- | ------------------------- | ---------------------- |
| class           | beanClass                 | Bean对应的Class        |
| constructor-arg | constructorArgumentValues | 构造参数信息           |
| factory-bean    | factoryBeanName           | Bean对应的实例工厂对象 |
| factory-method  | factoryMethodName         | Bean对应的工厂方法     |

由此`Bean`对象存在以下几种实例化方式

```bash
# Bean实例化的方法
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBeanInstance
	
# 创建方式
# 1. 无参构造方式
	# bean标签
		# class
	# BeanDefinition
		# beanClass
	# 入口
		# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#instantiateBean
	# 1.1 常规是通过Class对象获取Constructor对象
		# java.lang.Class#getDeclaredConstructor
	# 1.2 通过Constructor对象实例化
		# java.lang.reflect.Constructor#newInstance
		
# 2. 有参构造方式
	# bean标签 
		# class
		# constructor-arg
	# BeanDefinition
		# beanClass
		# constructorArgumentValues
	# 入口
		# org.springframework.beans.factory.support.ConstructorResolver#autowireConstructor
	# 过程
        # 2.1 获取当前Class对象的所有构造器
            # java.lang.Class#getConstructors
        # 2.2 构造参数解析、获取目标构造器后调用构造
            # org.springframework.beans.factory.support.ConstructorResolver#instantiate
            # java.lang.reflect.Constructor#newInstance
		
# 3. 实例方法
	# bean标签
		# factory-bean
		# factory-method
	# BeanDefinition
		# factoryBeanName
		# factoryMethodName
	# 入口
		# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#instantiateUsingFactoryMethod
		# org.springframework.beans.factory.support.ConstructorResolver#instantiateUsingFactoryMethod
	# 过程
        # 3.1 从BeanFactory中获取factoryBean
            # org.springframework.beans.factory.support.AbstractBeanFactory#getBean
        # 3.2 获取factoryBean的方法列表
            # java.lang.Class#getMethods
        # 3.3 获取目标方法，进行方法调用
        	# org.springframework.beans.factory.support.ConstructorResolver#instantiate
        	# java.lang.reflect.Method#invoke
        	
# 4. 静态方法
	# bean标签
		# beanClass
		# factory-method
	# BeanDefinition
		# beanClass
		# factoryMethodName
	# 静态方法基本同实例方法，只不过其无需获取对应方法实例对象
		# java.lang.reflect.Method#invoke调用时，传入null对象即可

# 5. 带参数实例/静态方法
	# bean标签
		# 多出constructor-arg配置
	# BeanDefinition
		# 多出constructorArgumentValues
	# 入口一致
	# 过程
		# 多出根据参数获取方法入参，之后依旧是获取目标方法调用
			# org.springframework.beans.factory.support.ConstructorResolver#instantiate
```



##### 实例化入口

```java
//org.springframework.context.support.AbstractApplicationContext#finishBeanFactoryInitialization
protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
    // 交由DefaultListableBeanFactory，实例化所有剩余的（非延迟初始化）单例.
    beanFactory.preInstantiateSingletons();
}
```



所以实际的实例化是交由 `DefaultListableBeanFactory`进行处理，下面分析`preInstantiateSingletons`方法

##### preInstantiateSingletons

```java
//org.springframework.beans.factory.support.DefaultListableBeanFactory#preInstantiateSingletons
public void preInstantiateSingletons() throws BeansException {
    //获取IOC容器中注册的BeanDefinitionNames
    List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);

    // 遍历创建
    for (String beanName : beanNames) {
        RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
        //判断是否抽象、单例、延时加载
        if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
            // 创建对象
            getBean(beanName);
        }
    }
}
```



由上可知，实际进行`Bean`实例化的入口是`AbstractBeanFactory#getBean`，此方法有实现自`BeanFactory`多个重载方法

```java
/* 根据beanName获取单个实例 */
public Object getBean(String name) throws BeansException {
    return doGetBean(name, null, null, false);
}

/* 根据beanName、requiredType获取单个实例 */
public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
    return doGetBean(name, requiredType, null, false);
}

/* 根据beanName获取单个实例， args用于实例创建，而非实例检索使用*/
public Object getBean(String name, Object... args) throws BeansException {
    return doGetBean(name, null, args, false);
}

/* 根据beanName、requiredType获取单个实例， args用于实例创建，而非实例检索使用*/
public <T> T getBean(String name, @Nullable Class<T> requiredType, @Nullable Object... args) throws BeansException {
    return doGetBean(name, requiredType, args, false);
}
```

我们可以发现，其都调用到`doGetBean`，所以`doGetBean`为`BeanFactory`为容器执行创建实例的入口



##### doGetBean

```java
//org.springframework.beans.factory.support.AbstractBeanFactory#doGetBean
protected <T> T doGetBean(final String name, @Nullable final Class<T> requiredType,
                          @Nullable final Object[] args, boolean typeCheckOnly) throws BeansException {

    //如果name以 & 为前缀，则获取去除 & 前缀的 beanName
    final String beanName = transformedBeanName(name);
    Object bean;

    // 查看三级缓存中是否存在
    Object sharedInstance = getSingleton(beanName);
    if (sharedInstance != null && args == null) {
        bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
    } else {
        //获取对应BeanDefinition
        final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
        // 实例化单例对象
        if (mbd.isSingleton()) {
            //通过getSingleton获取单例对象，传入对应ObjectFactory的lambda表达式
            sharedInstance = getSingleton(beanName, () -> {
                return createBean(beanName, mbd, args);
            });
            bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
        }
        return (T) bean;
    }
}

由上可知，单例对象获取的入口为getSingleton
    
//org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#getSingleton
public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
    //尝试从单例池中获取实例
    Object singletonObject = this.singletonObjects.get(beanName);
    //通过lambda表达式对应ObjectFactory#getObject，获取Bean实例
    singletonObject = singletonFactory.getObject();
    //保存到一级缓存
    addSingleton(beanName, singletonObject);
    //返回
    return singletonObject;
}

ObjectFactory是一个对象工厂接口
    
//org.springframework.beans.factory.ObjectFactory    
@FunctionalInterface
public interface ObjectFactory<T> {
	/* 通过getObject返回一个泛型实例 */
	T getObject() throws BeansException;
}    

所以实例创建的入口就是createBean方法
    
//org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBean   
protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args){
    //创建Bean
    Object beanInstance = doCreateBean(beanName, mbdToUse, args);
}    

//org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args){
    //1. 创建Bean实例对象
    instanceWrapper = createBeanInstance(beanName, mbd, args);
}
```



至此，确认了实际进行实例化的方法就是`createBeanInstance`

##### createBeanInstance方法分析

```java
//org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBeanInstance
protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
    // 获取BeanDefinition对应beanClass
    Class<?> beanClass = resolveBeanClass(mbd, beanName);

	//静态方法、实例方法创建
    if (mbd.getFactoryMethodName() != null) {
        return instantiateUsingFactoryMethod(beanName, mbd, args);
    }

    // 自动装配的候选构造函数
    Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
    if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
        mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
        return autowireConstructor(beanName, mbd, ctors, args);
    }

    // 默认构造的首选构造函数？
    ctors = mbd.getPreferredConstructors();
    if (ctors != null) {
        return autowireConstructor(beanName, mbd, ctors, null);
    }

    // 没有特殊处理：只需使用无参数构造函数。
    return instantiateBean(beanName, mbd);
}
```



#### FactoryBean

`FactoryBean`是一个工厂对象接口

其特点在于

- IOC容器中，以配置的 `beanName`  注册 `FactoryBean` 在单例池中
- 通过 `beanName` 从IOC容器中获取，返回不是`FactoryBean`本身，而是通过 `FactoryBean#getObject`获取的实例
- 通过 `& + beanName`，则可以从IOC容器中获取到`FactoryBean`本身

```java
//org.springframework.beans.factory.FactoryBean
public interface FactoryBean<T> {
	/* 管理对象获取方法 */
	T getObject() throws Exception;

	/* 返回管理对象对应Class */
	Class<?> getObjectType();

	/* 管理对象是否单例 */
	default boolean isSingleton() {
		return true;
	}
}
```



##### 实例化FactoryBean对象

创建所有单例对象，一般`FactoryBean`都是单例对象，所以将在IOC容器刷新时创建 
相对于普通`Bean`实例化，在`preInstantiateSingletons`中，存在对于`FactoryBean`的判断

```java
public void preInstantiateSingletons() throws BeansException {
    //获取所有
    List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);
    for (String beanName : beanNames) {
        RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
        if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
            if (isFactoryBean(beanName)) {
                //如果是FactoryBean，我们先创建FactoryBean对象，方式就是通过添加 & 前缀，表示获取FactoryBean对象本身
                Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);
                if (bean instanceof FactoryBean) {
                    final FactoryBean<?> factory = (FactoryBean<?>) bean;
                    //判断SmartFactoryBean#isEagerInit方法，判断是否直接获取FactoryBean生产对象
                    boolean isEagerInit;
                    if (System.getSecurityManager() != null && factory instanceof SmartFactoryBean) {
                        isEagerInit = AccessController.doPrivileged((PrivilegedAction<Boolean>)
                                                                    ((SmartFactoryBean<?>) factory)::isEagerInit,
                                                                    getAccessControlContext());
                    }
                    else {
                        isEagerInit = (factory instanceof SmartFactoryBean &&
                                       ((SmartFactoryBean<?>) factory).isEagerInit());
                    }
                    if (isEagerInit) {
                        //如果是急切需要创建，则通过不添加 & 前缀的beanName获取FactoryBean工厂创建对象
                        getBean(beanName);
                    }
                }
            }
            else {
                // 非FactoryBean，则直接创建
                getBean(beanName);
            }
        }
    }
}

//判断当前name对应对象是否FactoryBean对象
public boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException {
    //如果name以 & 为前缀，则获得去除 & 前缀的 beanName
    String beanName = transformedBeanName(name);
    //尝试从三级缓存中获取
    Object beanInstance = getSingleton(beanName, false);
    if (beanInstance != null) {
        //判断是否FactoryBean类型实例
        return (beanInstance instanceof FactoryBean);
    }
    // 获取不到对象实例，则通过BeanDefinition判断
    return isFactoryBean(beanName, getMergedLocalBeanDefinition(beanName));
}
```



`FactoryBean`也是一个单例对象，所以也将从IOC容器中获取对应实例

```java
//获取beanName对应实例
sharedInstance = getSingleton(beanName, () -> {
    return createBean(beanName, mbd, args);
});
//进行FactoryBean的处理
bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
```



在IOC容器实例化对象后，将通过`getObjectForBeanInstance`处理实例化对象

##### getObjectForBeanInstance

```java
// org.springframework.beans.factory.support.AbstractBeanFactory#getObjectForBeanInstance
// name为 transformedBeanName 方法处理前，beanName为 transformedBeanName 方法处理后
// 如果非FactoryBean，则name与beanName相同；如果为FactoryBean，name 为 & + beanName
protected Object getObjectForBeanInstance(Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {
    // isFactoryDereference表示name以 & 为前缀
    if (BeanFactoryUtils.isFactoryDereference(name)) {
        //对象为空对象，直接返回
        if (beanInstance instanceof NullBean) {
            return beanInstance;
        }
        //对象不是FactoryBean，报错，只有FactoryBean对象，其对应name才能以 & 为前缀
        if (!(beanInstance instanceof FactoryBean)) {
            throw new BeanIsNotAFactoryException(beanName, beanInstance.getClass());
        }
    }
    
    // 非FactoryBean对象，或者是获取 FactoryBean对象本身，则直接返回
    if (!(beanInstance instanceof FactoryBean) || BeanFactoryUtils.isFactoryDereference(name)) {
        return beanInstance;
    }

    Object object = null;
    if (mbd == null) {
        //尝试从FactoryBean对象缓存中获取
        object = getCachedObjectForFactoryBean(beanName);
    }
    //缓存中获取不到
    if (object == null) {
        // 类型强转
        FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
        if (mbd == null && containsBeanDefinition(beanName)) {
            // 获取beanName对应，即注册在IOC容器的BeanDefinition
            mbd = getMergedLocalBeanDefinition(beanName);
        }
        boolean synthetic = (mbd != null && mbd.isSynthetic());
        //获取FactoryBean生产的实例
        object = getObjectFromFactoryBean(factory, beanName, !synthetic);
    }
    return object;
}


//org.springframework.beans.factory.BeanFactoryUtils#isFactoryDereference
//通过判断 & 前缀，判断是否 FactoryBean 引用
public static boolean isFactoryDereference(@Nullable String name) {
    return (name != null && name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX));
}


//org.springframework.beans.factory.support.FactoryBeanRegistrySupport#getObjectFromFactoryBean
// 获取 FactoryBean 生产的实例，包括factoryBeanObjectCache缓存处理
// FactoryBeanRegistrySupport#factoryBeanObjectCache: Map<String, Object> factoryBeanObjectCache = new ConcurrentHashMap<>(16);
protected Object getObjectFromFactoryBean(FactoryBean<?> factory, String beanName, boolean shouldPostProcess) {
    //判断为单例对象，且beanName存在于IOC容器中
    if (factory.isSingleton() && containsSingleton(beanName)) {
        //尝试从缓存中获取对应实例
        Object object = this.factoryBeanObjectCache.get(beanName);
        if (object == null) {
            //从FactoryBean中获取获取实例
        	object = doGetObjectFromFactoryBean(factory, beanName);
            //将获取的实例存入缓存
            this.factoryBeanObjectCache.put(beanName, object);
        }
    } else {
        //由此可知，如果beanName没有受IOC容器管理，则不进行factoryBeanObjectCache缓存管理
    	Object object = doGetObjectFromFactoryBean(factory, beanName);
    }
    return object;
}


//具体执行对象生产
//org.springframework.beans.factory.support.FactoryBeanRegistrySupport#doGetObjectFromFactoryBean
private Object doGetObjectFromFactoryBean(final FactoryBean<?> factory, final String beanName){
    object = factory.getObject();
}
```



##### 作个小结

- IOC容器刷新时，将创建`FactoryBean`实例，对应`beanName`存储在单例池中
- 通过`name`从容器中获取，将`name`通过`transformedBeanName`方法获取`beanName`
  - 如果有 & 前缀，则去除
- 通过`getSingleton`方法，以`beanName`，从IOC容器中获取`FactoryBean`对象
- 再通过`getObjectForBeanInstance`方法处理
  - `name`存在 & 前缀
    - 返回`FactoryBean`对象
  - `name`不存在 & 前缀
    - 则通过`FactoryBean#getObject`获取对象



### 7. 属性设置

对象实例化后，对于XML文件中配置的构造属性`constructor-arg`，在实例创建时已经进行设置；而对于配置的普通属性`property`，则需要进行单独设置

属性配置加载为`MutablePropertyValues  AbstractBeanDefinition#propertyValues`

```java
属性配置加载由XML文件中解析为AbstractBeanDefinition#propertyValues
    MutablePropertyValues propertyValues;

实际属性信息存储
    List<PropertyValue> propertyValueList

//org.springframework.beans.PropertyValue
public class PropertyValue extends BeanMetadataAttributeAccessor implements Serializable {
    //属性名
    private final String name;
    //属性值
	private final Object value;
}    
    
//org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args){
    //创建Bean实例对象
    instanceWrapper = createBeanInstance(beanName, mbd, args);
    //属性设置
    populateBean(beanName, mbd, instanceWrapper);
}

//org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#populateBean
protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {
    //获取属性配置
    PropertyValues pvs = (mbd.hasPropertyValues() ? mbd.getPropertyValues() : null);
    if (pvs != null) {
        //进行属性设置
        applyPropertyValues(beanName, mbd, bw, pvs);
    }
}

//org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#applyPropertyValues
protected void applyPropertyValues(String beanName, BeanDefinition mbd, BeanWrapper bw, PropertyValues pvs) {
    //获取属性列表
    List<PropertyValue> original = Arrays.asList(pvs.getPropertyValues());
    //创建解析结果对象
    List<PropertyValue> deepCopy = new ArrayList<>(original.size());
    for (PropertyValue pv : original) {
        String propertyName = pv.getName();
        Object originalValue = pv.getValue();
        //解析属性配置对应值，如果是引用对象，则通过getBean获取对象
        Object resolvedValue = valueResolver.resolveValueIfNecessary(pv, originalValue);
        //保存解析后的属性值
        pv.setConvertedValue(convertedValue);
        //添加到deepCopy中
        deepCopy.add(pv);
    }
    //进行属性设置
    bw.setPropertyValues(new MutablePropertyValues(deepCopy));
}

属性设置的实质*
//org.springframework.beans.BeanWrapperImpl.BeanPropertyHandler#setValue
public void setValue(final @Nullable Object value) throws Exception {
    //通过属性描述器PropertyDescriptor，获取对应的writeMethod，即setter方法
    final Method writeMethod = (this.pd instanceof GenericTypeAwarePropertyDescriptor ?
					((GenericTypeAwarePropertyDescriptor) this.pd).getWriteMethodForActualAccess() :
					this.pd.getWriteMethod());
    //取消安全检查
    ReflectionUtils.makeAccessible(writeMethod);
    //进行方法调用
    writeMethod.invoke(getWrappedInstance(), value);
}
```

注意：由上述代码分析可知，对于`XML文件配置`的属性，**必须要有其对应`setter`方法**，否则将报错



#### 循环依赖问题

在代码开发时，经常存在一种情况：IOC容器管理的两个实例相互依赖，即互为属性

那么IOC容器在创建对应实例时，由于我们通过`getBean`进行对象获取时，获取对象应为成熟`SpringBean`，则表示其已经经过了属性设置阶段

此时，创建对象A，需要对象B作为属性；而创建对象B，又需要对象A作为属性，此时则陷入了循环依赖中

对此spring框架也是提供了解决方案，但是仍旧有其限制：

- 单例
  - 构造器参数循环依赖
    - 影响了对象实例化，所以无法解决
    - 将报错`BeanCurrentlyInCreationException`
  - setter方式循环依赖
    - 通过三级缓存解决
- 多例
  - 由于多例bean不由spring容器管理，无法进行循环依赖解决



#### 三级缓存

下面我们就来分析一下三级缓存，三级缓存实质就是指三个对象的缓存容器

##### DefaultSingletonBeanRegistry

`DefaultSingletonBeanRegistry`是`DefaultListableBeanFactory`的父类，主要用于注册IOC容器管理的`单例bean`

```java
//org.springframework.beans.factory.support.DefaultSingletonBeanRegistry
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {
    /** 一级缓存：单例池：bean 名称到 bean 实例. */
	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
    /** 二级缓存：早期单例对象的缓存：bean 名称到 bean 实例. */
	private final Map<String, Object> earlySingletonObjects = new HashMap<>(16);
    /** 三级缓存：单例工厂缓存：bean 名称到 ObjectFactory. */
	private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);
    /**一组已注册的单例，包含按注册顺序排列的 bean 名称。 */
	private final Set<String> registeredSingletons = new LinkedHashSet<>(256);
    
    
    /* 从BeanRegistry中中获取单例对象*/
    protected Object getSingleton(String beanName, boolean allowEarlyReference) {
		// 1. 尝试从单例池中获取单例对象
		Object singletonObject = this.singletonObjects.get(beanName);
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
			synchronized (this.singletonObjects) {
				// 2. 单例池中不存在，则尝试从二级缓存中获取
				singletonObject = this.earlySingletonObjects.get(beanName);
				if (singletonObject == null && allowEarlyReference) {
					// 3. 二级缓存中也不存在，则尝试从三级缓存中获取
					ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
					if (singletonFactory != null) {
						//三级缓存中存在，则获取三级缓存中对应ObjectFactory，调用getObject进行对象获取
						singletonObject = singletonFactory.getObject();
						//将三级缓存中对象存储到二级缓存中
						this.earlySingletonObjects.put(beanName, singletonObject);
						this.singletonFactories.remove(beanName);
					}
				}
			}
		}
		//返回响应
		return singletonObject;
	}
    
    /* 注册对象对应ObjectFactory到三级缓存 */
    protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(singletonFactory, "Singleton factory must not be null");
		synchronized (this.singletonObjects) {
			//如果单例池中不存在此对象
			if (!this.singletonObjects.containsKey(beanName)) {
				//将注册实例对应ObjectFactory到三级缓存中
				this.singletonFactories.put(beanName, singletonFactory);
				//移除二级缓存中对象
				this.earlySingletonObjects.remove(beanName);
				//标志此对象已注册
				this.registeredSingletons.add(beanName);
			}
		}
	}
    
    /* 注册对象到单例池 */
    protected void addSingleton(String beanName, Object singletonObject) {
		synchronized (this.singletonObjects) {
			//注册到单例池
			this.singletonObjects.put(beanName, singletonObject);
			//清除二、三级缓存存储
			this.singletonFactories.remove(beanName);
			this.earlySingletonObjects.remove(beanName);
			//标志此对象已注册
			this.registeredSingletons.add(beanName);
		}
	}
    
}
```



假设存在对象A、B相互依赖，对应XML配置

```xml
<bean id="a" class="com.dabing.model.A">
    <property name="b" ref="b"/>
</bean>

<bean id="b" class="com.dabing.model.B">
    <property name="a" ref="a"/>
</bean>
```



##### 步骤分析

```java
IOC容器先实例化对象A，则进入方法doGetBean

1. 尝试从缓存中获取对象A
//org.springframework.beans.factory.support.AbstractBeanFactory#doGetBean    
protected <T> T doGetBean(final String name, @Nullable final Class<T> requiredType,
			@Nullable final Object[] args, boolean typeCheckOnly) throws BeansException {
    // #1. 急切地检查单例缓存是否有手动注册的单例。
    Object sharedInstance = getSingleton(beanName);
    
    if (mbd.isSingleton()) {
        //#2. 通过getSingleton获取实例
        sharedInstance = getSingleton(beanName, () -> {
            return createBean(beanName, mbd, args);
        });
        bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
    }
}

2. 此时对象A还未创建，则将通过getSingleton创建对象
//org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#getSingleton(java.lang.String, org.springframework.beans.factory.ObjectFactory<?>)    
public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
    //尝试从单例池中获取
    Object singletonObject = this.singletonObjects.get(beanName);
    if (singletonObject == null) {
        //单例池中获取不到，则通过ObjectFactory#getObject获取对象
        singletonObject = singletonFactory.getObject();
        //将创建的对象保存到一级缓存中
        addSingleton(beanName, singletonObject);
    }
}    
    
3. 将通过lambda表达式对应ObjectFactory#getObject调用createBean创建对象A，实际调用到doCreateBean方法
//org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args){
    // 创建Bean实例对象
    instanceWrapper = createBeanInstance(beanName, mbd, args);
    //对象保存到三级缓存
    addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
    //属性设置
    populateBean(beanName, mbd, instanceWrapper);
}    

4. 此时将通过createBeanInstance创建实例A，并通过addSingletonFactory将对象A保存到三级缓存中
singletonFactories：对象A对应ObjectFactory
对象A对应属性B为空    
    
5. 对象A，创建后，则将通过populateBean设置对象A对应属性B；此时将通过递归调用getBean创建对象B  
    
6. 对象B与对象A一致通过步骤1/2/3/4，此时
singletonFactories：对象A、B对应ObjectFactory
对象A对应属性B为空，对象B对应属性A为空    
    
7. 此时通过populateBean进行对象B属性设置，此时将递归通过getBean从IOC容器中获取对象A作为属性设置
  	这时将调用getSingleton从IOC容器中获取对象A，发现对象A存在于三级缓存singletonFactories中，所以：
    1. 从三级缓存中获取对象A对应ObjectFactory
    2. 通过ObjectFactory#getObject获取对象A的代理对象
    3. 将对象A存储到二级缓存earlySingletonObjects中
    4. 返回对象A，设置到属性B
    
8. 当前现况
singletonFactories：对象B对应ObjectFactory
earlySingletonObjects：对象A
对象A对应属性B为空，对象B对应属性A已设置
    
9. 由于对象B已经属性设置成功，则属性B为成熟SpringBean，即通过  addSingleton 方法将对象B保存到单例池
singletonObjects：对象B
earlySingletonObjects：对象A
对象A对应属性B为空，对象B对应属性A已设置
    
10. 此时对象A获取到对象B，则进行属性设置，之后将通过  addSingleton 方法对象A保存到单例池    
singletonObjects：对象A、B    
对象A对应属性B已设置，对象B对应属性A已设置    
```



至此循环依赖处理结束，那么为何需要三级缓存的设计，下面来分析

单例池`singletonObjects`：用于存储成熟`SpringBean`，后续程序运行时可以从中获取所需对象



三级缓存`singletonFactories`

我们发现三级缓存比较特殊，它不是存储对象实例，而是存储对应ObjectFactory，我们分析一下

```java
实际通过addSingletonFactory添加的ObjectFactory对象为lambda表达式
addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));

下面分析一下getEarlyBeanReference方法
//org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#getEarlyBeanReference
protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
    Object exposedObject = bean;
    if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
                SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
                exposedObject = ibp.getEarlyBeanReference(exposedObject, beanName);
            }
        }
    }
    return exposedObject;
} 

发现实际是通过SmartInstantiationAwareBeanPostProcessor#getEarlyBeanReference对实例进行了代理，实质是进行了AOP代理工作
如果Bean需要进行AOP代理，则需要通过ObjectFactory来创建代理对象返回    
```



二级缓存`earlySingletonObjects`，有人可能说，感觉处理循环依赖，只需要一、三级缓存即可，由三级缓存工厂创建对象直接保存到单例池中不就可以了？

但实际来说，由三级缓存工厂对象创建的`bean`，并不是一个成熟的`SpringBean`对象，而是一个早期暴露的`SpringBean`(没有走完整个生命周期)

但是其有具体作用：

- 早期暴露的目的：如果一个Bean被多个Bean依赖，则无需再次由三级缓存创建代理对象并保存到二级缓存中，而是直接可以从二级缓存中获取
- 不取消二级缓存的目的：由于一级缓存都是保存完整的SpringBean，如果取消二级缓存，将提前暴露的不完整的SpringBean也保存在此，则导致一级缓存属性不唯一，违背单一原则



后面分析一下对象实例化后处理，其入口为`initializeBean`

```java
//org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args){
    //实例化
	instanceWrapper = createBeanInstance(beanName, mbd, args);
    //属性设置
    populateBean(beanName, mbd, instanceWrapper);
    //初始化
    exposedObject = initializeBean(beanName, exposedObject, mbd);
}

//org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#initializeBean
protected Object initializeBean(final String beanName, final Object bean, @Nullable RootBeanDefinition mbd) {
    Object wrappedBean = bean;
    // BeanPostProcessor初始化前置处理
    wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
    // 初始化操作
    invokeInitMethods(beanName, wrappedBean, mbd);
    // BeanPostProcessor初始化后置处理
    wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    return wrappedBean;
}
```



### 8. BeanPostProcessor初始化前置处理

```java
//org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#applyBeanPostProcessorsBeforeInitialization
public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName){
    Object result = existingBean;
    //遍历注册的BeanPostProcessor，调用postProcessBeforeInitialization方法进行初始化前处理
    for (BeanPostProcessor processor : getBeanPostProcessors()) {
        Object current = processor.postProcessBeforeInitialization(result, beanName);
        if (current == null) {
            return result;
        }
        result = current;
    }
    return result;
}
```

### 9. 初始化

初始化操作实际对应XML配置的有两种

- 对象实现`InitializingBean`接口
- XML中`bean`标签配置了`init-method`



#### InitializingBean

`InitializingBean`是一个用做初始化的接口

```java
//org.springframework.beans.factory.InitializingBean
public interface InitializingBean {
	/* 通过afterPropertiesSet进行初始操作 */
	void afterPropertiesSet() throws Exception;
}
```



#### init-method

`init-method`对应方法应是一个无参函数，对应XML配置

```xml
<bean id="personWithInit" class="com.dabing.model.Person" init-method="initMethod"/>
```

对应`BeanDefinition`属性

```java
//org.springframework.beans.factory.support.AbstractBeanDefinition#initMethodName
private String initMethodName;
```



#### 初始化调用

```java
//org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#invokeInitMethods
protected void invokeInitMethods(String beanName, final Object bean, @Nullable RootBeanDefinition mbd){
    //判读当前bean是否InitializingBean类型
    boolean isInitializingBean = (bean instanceof InitializingBean);
    if (isInitializingBean && (mbd == null || !mbd.isExternallyManagedInitMethod("afterPropertiesSet"))) {
        //调用afterPropertiesSet方法进行初始处理
        ((InitializingBean) bean).afterPropertiesSet();
    }

    if (mbd != null && bean.getClass() != NullBean.class) {
        //获取init-method配置对应initMethodName
        String initMethodName = mbd.getInitMethodName();
        if (StringUtils.hasLength(initMethodName) &&
            !(isInitializingBean && "afterPropertiesSet".equals(initMethodName)) &&
            !mbd.isExternallyManagedInitMethod(initMethodName)) {
            //进行init-method方法调用
            invokeCustomInitMethod(beanName, bean, mbd);
        }
    }
}
```



### 10. BeanPostProcessor初始化后置处理

```java
//org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#applyBeanPostProcessorsAfterInitialization
public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName){
    Object result = existingBean;
     //遍历注册的BeanPostProcessor，调用postProcessAfterInitialization方法进行初始后前处理
    for (BeanPostProcessor processor : getBeanPostProcessors()) {
        Object current = processor.postProcessAfterInitialization(result, beanName);
        if (current == null) {
            return result;
        }
        result = current;
    }
    return result;
}
```



至此，对于解析XML配置的`ClassPathXmlApplicationContext`容器的创建、刷新过程就总结完毕

****



## 注解配置工作流程简述

对于开发来说，XML配置是繁琐、重量级的，所以Spring推出了注解配置的方式来替代传统XML配置，方便开发，下面我们将分析注解配置是如何实现IOC容器的创建、刷新动作

对于注解配置方式来说，我们使用的`ApplicationContext`为`AnnotationConfigApplicationContext`



### 使用方式

```java
public static void main(String[] args) {
    //容器创建：加载主配置类
    AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(MyConfiguration.class);
    //从创建后的容器中获取bean实例
    Person person = (Person) applicationContext.getBean("person");
    person.doSomeThing();
    //容器关闭，销毁单例bean
    applicationContext.close();
}
```



对于XML配置方式而言，我们会将所需对象都统一配置在XML配置文件中，所以我们解析配置文件则可以获取所需`beanDefinition`

而对于注解配置而言，我们的解析入口只有一个配置类数组`Class<?>...`，此时我们通过传入`Class<?>...`仅能获取少量的`BeanDefinition`，那么此时该如何让容器获取到我们所需对象的配置？

实际上`AnnotationConfigApplicationContext`是通过初始化一系列后置处理器来实现，下面我们来分析一下



### 1.IOC容器创建

上述代码可知，注解配置方式通过`AnnotationConfigApplicationContext`来实现功能，那么我们先分析一下它的构造方法

但是在此之前，我们先分析一下其父类`GenericApplicationContext`



#### GenericApplicationContext

在前面所述`ApplicationContext`的类图来说，`AnnotationConfigApplicationContext`单独实现了`GenericApplicationContext`，下面分析一下原因

我们先看一下`GenericApplicationContext`类图

![image-20210803202547381](.\图片\GenericApplicationContext类图)



发现其实现了`BeanDefinitionRegistry`，这个正常来说是`DefaultListableBeanFactory`实现的接口，用于`BeanDefinition`的管理

下面分析`GenericApplicationContext`

```java
//org.springframework.context.support.GenericApplicationContext
public class GenericApplicationContext extends AbstractApplicationContext implements BeanDefinitionRegistry {
    //持有了DefaultListableBeanFactory引用
	private final DefaultListableBeanFactory beanFactory;
    
    public GenericApplicationContext() {
        //构造时，实例化DefaultListableBeanFactory
		this.beanFactory = new DefaultListableBeanFactory();
	}
    
    /* 实现自BeanDefinitionRegistry，实际通过DefaultListableBeanFactory进行BeanDefinition的管理 */
	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {
		this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
	}
	public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		return this.beanFactory.getBeanDefinition(beanName);
	}
}
```



而由于`GenericApplicationContext`为`AnnotationConfigApplicationContext`父类，所以在构造`AnnotationConfigApplicationContext`前，将会执行`GenericApplicationContext`的构造方法，实例化`DefaultListableBeanFactory`

那么对比于XML配置方式在`obtainFreshBeanFactory`方法实例化`DefaultListableBeanFactory`，此处提前实例化的用意是什么？我们后续分析



此时查看`AnnotationConfigApplicationContext`的构造方法

```java
//org.springframework.context.annotation.AnnotationConfigApplicationContext#AnnotationConfigApplicationContext
public AnnotationConfigApplicationContext(Class<?>... componentClasses) {
    //调用重载构造
    this();
    //注册register
    register(componentClasses);
    //刷新IOC容器
    refresh();
}

////org.springframework.context.annotation.AnnotationConfigApplicationContext#AnnotationConfigApplicationContext
public AnnotationConfigApplicationContext() {
    this.reader = new AnnotatedBeanDefinitionReader(this);
    this.scanner = new ClassPathBeanDefinitionScanner(this);
}
```



我们发现构造时创建了两个对象：

- `AnnotatedBeanDefinitionReader`
- `ClassPathBeanDefinitionScanner`

我们来分析仪器它们的作用



### 2.  AnnotatedBeanDefinitionReader

`AnnotatedBeanDefinitionReader`主要有两个作用：

```java
//org.springframework.context.annotation.AnnotatedBeanDefinitionReader
public class AnnotatedBeanDefinitionReader {
    //持有BeanDefinitionRegistry，实际是AnnotationConfigApplicationContext对象
	private final BeanDefinitionRegistry registry;
    
    /* 构造 */
    public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment) {
		//保存registry
		this.registry = registry;
		//通过工具类AnnotationConfigUtils注册注解处理器
		AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
	}
    
    /* 注册componentClasses */
    public void register(Class<?>... componentClasses) {
		for (Class<?> componentClass : componentClasses) {
			registerBean(componentClass);
		}
	}
}
```



`AnnotationConfigUtils#registerAnnotationConfigProcessors`中，实现了`AnnotatedBeanDefinitionReader`的主要功能：注册注解处理器

这也是`AnnotationConfigApplicationContext`主要功能实现的起点

```java
//org.springframework.context.annotation.AnnotationConfigUtils
public abstract class AnnotationConfigUtils {
    //判断参数
    private static final boolean jsr250Present;

	static {
		ClassLoader classLoader = AnnotationConfigUtils.class.getClassLoader();
        //通过能否加装目标类，确认判断依据
		jsr250Present = ClassUtils.isPresent("javax.annotation.Resource", classLoader);
	}

    //org.springframework.context.annotation.AnnotationConfigUtils#registerAnnotationConfigProcessors
    public static void registerAnnotationConfigProcessors(BeanDefinitionRegistry registry) {
        registerAnnotationConfigProcessors(registry, null);
    }

    // 具体处理，注册注解后置处理器
    public static Set<BeanDefinitionHolder> registerAnnotationConfigProcessors(
        BeanDefinitionRegistry registry, @Nullable Object source) {
        //从BeanDefinitionRegistry中获取对应DefaultListableBeanFactory
        DefaultListableBeanFactory beanFactory = unwrapDefaultListableBeanFactory(registry);

        Set<BeanDefinitionHolder> beanDefs = new LinkedHashSet<>(8);

        //尝试注册默认ConfigurationClassPostProcessor
        if (!registry.containsBeanDefinition(CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME)) {
            RootBeanDefinition def = new RootBeanDefinition(ConfigurationClassPostProcessor.class);
            def.setSource(source);
            beanDefs.add(registerPostProcessor(registry, def, CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME));
        }

        //尝试注册AutowiredAnnotationBeanPostProcessor
        if (!registry.containsBeanDefinition(AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)) {
            RootBeanDefinition def = new RootBeanDefinition(AutowiredAnnotationBeanPostProcessor.class);
            def.setSource(source);
            beanDefs.add(registerPostProcessor(registry, def, AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME));
        }
		
        // 尝试注册CommonAnnotationBeanPostProcessor
        // 支持JSR-250的一些注解：@Resource、@PostConstruct、@PreDestroy等
        if (jsr250Present && !registry.containsBeanDefinition(COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)) {
            RootBeanDefinition def = new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class);
            def.setSource(source);
            beanDefs.add(registerPostProcessor(registry, def, COMMON_ANNOTATION_PROCESSOR_BEAN_NAME));
        }
        return beanDefs;
    }
}
```



#### 作个小结

由上述代码可知两点：

- 为何需要在`GenericApplicationContext`构造时提起创建`DefaultListableBeanFactory`
  - 主要是用于在初始化时，能向`DefaultListableBeanFactory`中注册所需`BeanPostProcessor`
- 通过`AnnotationConfigUtils#registerAnnotationConfigProcessors`，默认向`BeanFactory`中注册了以下对象
  - `ConfigurationClassPostProcessor`
  - `AutowiredAnnotationBeanPostProcessor`
  - `CommonAnnotationBeanPostProcessor`



### 3. ClassPathBeanDefinitionScanner

实际上`AnnotationConfigApplicationContext`还有一个重载的构造，通过传入指定包，进行IOC容器初始化

```java
public AnnotationConfigApplicationContext(String... basePackages) {
    this();
    scan(basePackages);
    refresh();
}

在其scan方法中，使用了ClassPathBeanDefinitionScanner进行基础包扫描
    
public void scan(String... basePackages) {
    this.scanner.scan(basePackages);
}    
```



`ClassPathBeanDefinitionScanner`是一个`Bean`扫描器，用于扫描`classPath`下的Bean，并将其注册到IOC容器中，默认通过`@Component`进行`Bean`过滤

但是其已经被`AnnotatedBeanDefinitionReader`替代，具体来说，是被`ConfigurationClassPostProcessor`所取代，所以此处我们不具体分析



### 4. register方法

`register`方法在重载构造后调用，主要用于将传入的`componentClasses`进行注册，实际通过`AnnotatedBeanDefinitionReader`进行工作

```java
//org.springframework.context.annotation.AnnotationConfigApplicationContext#register
public void register(Class<?>... componentClasses) {
    this.reader.register(componentClasses);
}

//org.springframework.context.annotation.AnnotatedBeanDefinitionReader#register
public void register(Class<?>... componentClasses) {
    for (Class<?> componentClass : componentClasses) {
        registerBean(componentClass);
    }
}

//org.springframework.context.annotation.AnnotatedBeanDefinitionReader#registerBean(java.lang.Class<?>)
public void registerBean(Class<?> beanClass) {
    doRegisterBean(beanClass, null, null, null);
}

//org.springframework.context.annotation.AnnotatedBeanDefinitionReader#doRegisterBean
<T> void doRegisterBean(Class<T> beanClass, @Nullable Supplier<T> instanceSupplier, @Nullable String name,
			@Nullable Class<? extends Annotation>[] qualifiers, BeanDefinitionCustomizer... definitionCustomizers) {
    //创建beanClass对应BeanDefinition
    AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(beanClass);
    //获取对应beanName
    String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, this.registry));
    //处理通过注解包括@Lazy、@Primary、@DependsOn、@Role、@Description
    AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);
	//包装为BeanDefinitionHolder
    BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);
    //注册BeanDefinition
    BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);
}
```



所以，`register`方法会将构造传入的`componentClasses`注册到`BeanFactory`中



### 5. refresh

1. `AnnotationConfigApplicationContext`也继承了`AbstractApplicationContext`，所以将调用`AbstractApplicationContext#refresh`进行处理

通过上述分析XML配置解析时，调用`refresh`，将先调用`obtainFreshBeanFactory`创建`BeanFactory`并进行`BeanDefinition`的加载、注册

2. 而在`AnnotationConfigApplicationContext#refreshBeanFactory(继承自GenericApplicationContext)`中并没有同`ClassPathXMLApplicationContext#refreshBeanFactory(继承自AbstractRefreshableApplicationContext)`一样进行`BeanFactory`创建和`BeanDefinition`的加载工作

3. 因为前面已经通过父类`GenericApplicationContext`的构造创建了`DefaultListableBeanFactory`，并通过`register`方法注册了`componentClasses`对应的`BeanDefinition`

4. 那么此时又存在问题，我们通过`register`仅仅注册了`componentClasses`对应的`BeanDefinition`，而我们不可能将所有需要使用的`Bean`都传入，那么IOC容器将如何加载我们所需`BeanDefinition`？



我们经过前面的处理，在`DefaultListableBeanFactory`中实际注册了哪些`BeanDefinition`？

- `componentClasses`
- `ConfigurationClassPostProcessor`
- `AutowiredAnnotationBeanPostProcessor`
- `CommonAnnotationBeanPostProcessor`

而对于其他`BeanDefinition`的加载注册，实际是通过`ConfigurationClassPostProcessor`结合注解来实现，下面我们分析注解



### Spring用于注册BeanDefinition的注解

#### @Component

```java
//org.springframework.stereotype.Component
@Target(ElementType.TYPE)
public @interface Component {
	/* BeanDefinition对应beanName */
	String value() default "";
}
```

`@Component`主要用于标志当前类是一个需要注册到IOC容器的对象，IOC容器将会加载其class对应`BeanDefinition`并注册到IOC容器中

其存在几个变种的注解：

- `@Service`
- `@Controller`
- `@Repository`

这几个注解上都标注了`@Component`注解，并通过`@AliasFor`进行别名处理

它们存在的目的主要用于程序开发的三层规范使用



#### @Configuration

```java
//org.springframework.context.annotation.Configuration
@Target(ElementType.TYPE)
@Component
public @interface Configuration {
	/* 对应于@Component的别名，就是BeanDefinition对应beanName */
	@AliasFor(annotation = Component.class)
	String value() default "";
}
```

`@Configuration`注解主要表明当前类是一个配置类，IOC容器处理时，将会将其当做一个配置类进行解析



#### @ComponentScan

```java
@Target(ElementType.TYPE)
@Repeatable(ComponentScans.class)
public @interface ComponentScan {
    
    /* 组件扫描的基础包路径配置 */
	@AliasFor("basePackages")
	String[] value() default {};
    @AliasFor("value")
	String[] basePackages() default {};
}
```

`@ComponentScan`注解主要配合`@Component`注解使用，主要用于扫描给定包下所有`@Component`注释的类，将其加载到IOC容器中



#### @ComponentScans

```java
//org.springframework.context.annotation.ComponentScans
@Target(ElementType.TYPE)
public @interface ComponentScans {
   ComponentScan[] value();
}
```

`@ComponentScans`注解就是多个`@Component`注解的组合体



#### @Import

```java
//org.springframework.context.annotation.Import
@Target(ElementType.TYPE)
public @interface Import {
	/* 要导入的类集合*/
	Class<?>[] value();
}
```

`@Import`注解主要用于向IOC容器中导入额外的类，主要包括：

- `@Configuration`注释的配置类
- `ImportSelector`接口实现类
- `ImportBeanDefinitionRegistrar`接口实现类
- 普通的组件类



#### @ImportResource

```java
//org.springframework.context.annotation.ImportResource
@Target(ElementType.TYPE)
public @interface ImportResource {
   @AliasFor("locations")
   String[] value() default {};

   /* 要导入文件的路径，支持classpath: file: 前缀 */
   @AliasFor("value")
   String[] locations() default {};

   /* 解析配置文件的具体BeanDefinitionReader解析器，默认为BeanDefinitionReader */
   Class<? extends BeanDefinitionReader> reader() default BeanDefinitionReader.class;
}
```

注解配置方式是用于取代传统XML配置方式，但是可能我们在项目使用时，仍旧存在一些XML配置文件，那么此时我们还需加载这个配置文件并解析其中注册的`bean`对象，就需要使用`@ImportResource`注解引入对应`application.xml`配置文件



#### @Bean

```java
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface Bean {
    
    /* 对应BeanDefinition的beanName */
    @AliasFor("name")
	String[] value() default {};
    @AliasFor("value")
	String[] name() default {};
}
```

`@Bean`不同于上述其他注解，它们都是注释在类上的，而`@Bean`是注释在方法、注解上的

`@Bean`注解主要用于将目标方法的返回值注册到IOC容器中，类似于XML配置方式的实例方法、静态方法的形式

`@Bean`常用于第三方类库的引入，因为我们没法在第三方类库上添加上`@Component`注解



### ConfigurationClassPostProcessor

我们先查看一下`ConfigurationClassPostProcessor`类图

![image-20210804161554497](.\图片\ConfiguratioinClassPostProcessor类图)

可以看出`ConfigurationClassPostProcessor`实现了`BeanDefinitionRegistryPostProcessor、BeanFactoryPostProcessor`接口，所以实现了接口方法：

- `BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry`
- `BeanFactoryPostProcessor#postProcessBeanFactory`

由前面分析可知，在`BeanFactoryPostProcessor`后置处理时，将先执行`postProcessBeanDefinitionRegistry`方法，后执行`postProcessBeanFactory`方法

所以我们先分析`postProcessBeanDefinitionRegistry`方法



#### postProcessBeanDefinitionRegistry

正常来说`BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry`一般用于向IOC容器中注册额外的`BeanDefinition`，此处也是

```java
//org.springframework.context.annotation.ConfigurationClassPostProcessor#postProcessBeanDefinitionRegistry
public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
    processConfigBeanDefinitions(registry);
}  

//org.springframework.context.annotation.ConfigurationClassPostProcessor#processConfigBeanDefinitions
public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
    // 目标配置类BeanDefinition集合容器
    List<BeanDefinitionHolder> configCandidates = new ArrayList<>();
    // 从IOC容器中获取当前注册的所有BeanDefinitionNames
    String[] candidateNames = registry.getBeanDefinitionNames();
    
    for (String beanName : candidateNames) {
        // 从IOC容器中获取对应BeanDefinition
        BeanDefinition beanDef = registry.getBeanDefinition(beanName);
        // 如果是已经解析过的配置类，则不再判断
        if (ConfigurationClassUtils.isFullConfigurationClass(beanDef) ||
            ConfigurationClassUtils.isLiteConfigurationClass(beanDef)) {
        }
        // 调用checkConfigurationClassCandidate方法判断当前beanDef是否为配置类
        else if (ConfigurationClassUtils.checkConfigurationClassCandidate(beanDef, this.metadataReaderFactory)) {
            // 将符合条件的配置类保存
            configCandidates.add(new BeanDefinitionHolder(beanDef, beanName));
        }
    }
    
    // 创建配置类解析器
    ConfigurationClassParser parser = new ConfigurationClassParser(
        this.metadataReaderFactory, this.problemReporter, this.environment,
        this.resourceLoader, this.componentScanBeanNameGenerator, registry);
    // 配置类去重
    Set<BeanDefinitionHolder> candidates = new LinkedHashSet<>(configCandidates);
    // 解析配置类
    parser.parse(candidates);
    
    // 获取ConfigurationClassParser解析结果的ConfigurationClass
    Set<ConfigurationClass> configClasses = new LinkedHashSet<>(parser.getConfigurationClasses());
    // 获取ConfigurationClassBeanDefinitionReader
    if (this.reader == null) {
        this.reader = new ConfigurationClassBeanDefinitionReader(
            registry, this.sourceExtractor, this.resourceLoader, this.environment,
            this.importBeanNameGenerator, parser.getImportRegistry());
    }
    // 通过ConfigurationClassBeanDefinitionReader#loadBeanDefinitions，将解析后的配置类注册
    this.reader.loadBeanDefinitions(configClasses);
}
```

由上述代码分析可知，`postProcessBeanDefinitionRegistry`方法大致实现功能分为四步：

1.  获取IOC容器中注册的`BeanDefinition`进行分析
2.  判断`BeanDefinition`是否为配置类，并将配置类筛选出来
3.  创建`ConfigurationClassParser`解析所有配置类对应`BeanDefinition`
4.  将解析得到的`BeanDefinition`注册到IOC容器中

那么我们逐步分析



##### 1. 判断是否配置类

我们先要分析一下如何判断一个`BeanDefinition`对应是一个配置类

此时我们借用了一个工具类`ConfigurationClassUtils`，它主要用于识别配置类



###### ConfigurationClassUtils

```java
//org.springframework.context.annotation.ConfigurationClassUtils
abstract class ConfigurationClassUtils {
    //配置类在BeanDefinition中的属性标识
    private static final String CONFIGURATION_CLASS_ATTRIBUTE =
			Conventions.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "configurationClass");
    
    //标志为完全配置类
    private static final String CONFIGURATION_CLASS_FULL = "full";
	//标志为精简配置类
	private static final String CONFIGURATION_CLASS_LITE = "lite";
    
    //额外的候选标志注解集合
    private static final Set<String> candidateIndicators = new HashSet<>(8);
	static {
		candidateIndicators.add(Component.class.getName());
		candidateIndicators.add(ComponentScan.class.getName());
		candidateIndicators.add(Import.class.getName());
		candidateIndicators.add(ImportResource.class.getName());
	}
}
```

`ConfigurationClassUtils`通过向`BeanDefinition`的属性中添加一个`CONFIGURATION_CLASS_ATTRIBUTE`的属性，标志其为一个配置类



我们分析`postProcessBeanDefinitionRegistry`方法，发现其通过`ConfigurationClassUtils`的`isFullConfigurationClass、isLiteConfigurationClass`方法来判断是否已经解析过得配置类，判断依据就是`CONFIGURATION_CLASS_ATTRIBUTE`属性

```java
//org.springframework.context.annotation.ConfigurationClassUtils#isFullConfigurationClass
public static boolean isFullConfigurationClass(BeanDefinition beanDef) {
    return CONFIGURATION_CLASS_FULL.equals(beanDef.getAttribute(CONFIGURATION_CLASS_ATTRIBUTE));
}

//org.springframework.context.annotation.ConfigurationClassUtils#isLiteConfigurationClass
public static boolean isLiteConfigurationClass(BeanDefinition beanDef) {
    return CONFIGURATION_CLASS_LITE.equals(beanDef.getAttribute(CONFIGURATION_CLASS_ATTRIBUTE));
}
```



如果不是已解析的配置类，将通过`checkConfigurationClassCandidate`方法判断是否配置类，如果是，将为其设置`CONFIGURATION_CLASS_ATTRIBUTE`属性

**checkConfigurationClassCandidate**

```java
//org.springframework.context.annotation.ConfigurationClassUtils#checkConfigurationClassCandidate
//判断是否配置类    
public static boolean checkConfigurationClassCandidate(BeanDefinition beanDef, MetadataReaderFactory metadataReaderFactory) {
    //获取对应注解元数据
    AnnotationMetadata metadata;
    //具体不分析注解元数据的获取...
	//判断是否完整配置类
    if (isFullConfigurationCandidate(metadata)) {
        beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_FULL);
    }
    //判断是否精简版配置类
    else if (isLiteConfigurationCandidate(metadata)) {
        beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_LITE);
    }
    else {
        return false;
    }   
    return true;
}  
```

我们发现`checkConfigurationClassCandidate`方法中主要通过`isFullConfigurationCandidate、isLiteConfigurationCandidate`来解析对应元数据判断当前`BeanDefinition`是否配置类，下面我们具体分析两方法



**isFullConfigurationCandidate**

```java
//org.springframework.context.annotation.ConfigurationClassUtils#isFullConfigurationCandidate
public static boolean isFullConfigurationCandidate(AnnotationMetadata metadata) {
    return metadata.isAnnotated(Configuration.class.getName());
}
```

`isFullConfigurationCandidate`实际是通过分析 类、及其超类 上是否存在`@Configuration`注解为判断依据，所以存在`@Configuration`就是一个完整配置类



**isLiteConfigurationClass**

```java
public static boolean isLiteConfigurationCandidate(AnnotationMetadata metadata) {
    // 1. 不要考虑接口或注释...
    if (metadata.isInterface()) {
        return false;
    }
	
    // 2. 判断是否存在@Component、@ComponentScan、@Import、@ImportResource中其一注解
    for (String indicator : candidateIndicators) {
        if (metadata.isAnnotated(indicator)) {
            return true;
        }
    }
    
	// 3. 判断是否存在以@Bean注释的方法
    return metadata.hasAnnotatedMethods(Bean.class.getName());
}

//org.springframework.core.type.StandardAnnotationMetadata#hasAnnotatedMethods
//判断当前类是否有方法注释有目标注解
public boolean hasAnnotatedMethods(String annotationName) {
    try {
        //获取当前类所有方法
        Method[] methods = getIntrospectedClass().getDeclaredMethods();
        for (Method method : methods) {
            //判断是否有目标注解
            if (!method.isBridge() && method.getAnnotations().length > 0 &&
                AnnotatedElementUtils.isAnnotated(method, annotationName)) {
                return true;
            }
        }
        return false;
    }
}
```



###### 作个小结

通过`ConfigurationClassUtils`判断`BeanDefinition`是否配置类，判断依据及其区分：

- 完整配置类
  - 类及其超类上有`@Configuration`注解
- 精简配置类
  - 类及其超类上有`@Component、@ComponentScan、@Import、@ImportResource`其一注解
  - 类中存在以`@Bean`标注的方法

而且对于配置类，我们通过向对应`BeanDefinition`设置`CONFIGURATION_CLASS_ATTRIBUTE`属性来标志为配置类，且区分完整、精简配置类



##### 2. 解析配置类BeanDefinition

我们通过创建一个`ConfigurationClassParser`来解析配置类对应`BeanDefinition`，我们分析一下



###### ConfigurationClassParser

```java
//org.springframework.context.annotation.ConfigurationClassParser
class ConfigurationClassParser {
    //解析得到的所有ConfigurationClass
    private final Map<ConfigurationClass, ConfigurationClass> configurationClasses = new LinkedHashMap<>();
}
```



我们发现通过`ConfigurationClassParser`解析后，得到的是`ConfigurationClass`对象，我们先分析一下`ConfigurationClass`类

###### ConfigurationClass

```java
final class ConfigurationClass {
    
    //对应BeanDefinition的beanName
	private String beanName;
    
    //注解元数据
    private final AnnotationMetadata metadata;
    
    //类Class对应资源对象
    private final Resource resource;
}
```



我们分析一下`ConfigurationClassParser`解析`BeanDefinition`的入口，即其`parse`方法

```java
//org.springframework.context.annotation.ConfigurationClassParser
class ConfigurationClassParser {
    //被处理类的缓存容器
    private final Map<ConfigurationClass, ConfigurationClass> configurationClasses = new LinkedHashMap<>();
    
    public void parse(Set<BeanDefinitionHolder> configCandidates) {
        //遍历配置类
        for (BeanDefinitionHolder holder : configCandidates) {
            //多个重载方法解析配置类
            if (bd instanceof AnnotatedBeanDefinition) {
                parse(((AnnotatedBeanDefinition) bd).getMetadata(), holder.getBeanName());
            }
            else if (bd instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) bd).hasBeanClass()) {
                parse(((AbstractBeanDefinition) bd).getBeanClass(), holder.getBeanName());
            }
            else {
                parse(bd.getBeanClassName(), holder.getBeanName());
            }
        }
    }
}
```

实际多个重载方法都调用到了`processConfigurationClass`方法，主要是将根据不同信息，封装对应`ConfigurationClass`对象作为入参进行统一处理



**processConfigurationClass**

```java
//org.springframework.context.annotation.ConfigurationClassParser#processConfigurationClass    
protected void processConfigurationClass(ConfigurationClass configClass) throws IOException {
    //根据@Condition注解，判断当前配置类是否处理
    if (this.conditionEvaluator.shouldSkip(configClass.getMetadata(), ConfigurationPhase.PARSE_CONFIGURATION)) {
        return;
    }
    //递归处理配置类及其超类层次结构。
    SourceClass sourceClass = asSourceClass(configClass);
    do {
        sourceClass = doProcessConfigurationClass(configClass, sourceClass);
    }
    while (sourceClass != null);
    //将处理的类缓存到configurationClasses中
    this.configurationClasses.put(configClass, configClass);
} 
```

可以看出，`processConfigurationClass`的作用包括

- `@Condition`条件过滤
- 通过循环处理的配置类及其超类
- 将解析后的配置类缓存



##### doProcessConfigurationClass

按Spring中方法命名习惯，`doProcessConfigurationClass`才是真正进行配置类处理的方法，由于`doProcessConfigurationClass`方法中内容比较多，我们一一单独分析

```
org.springframework.context.annotation.ConfigurationClassParser#doProcessConfigurationClass
```





###### 第一部分：嵌套内部类处理

```java
protected final SourceClass doProcessConfigurationClass(ConfigurationClass configClass, SourceClass sourceClass){
    //省略...
	
    if (configClass.getMetadata().isAnnotated(Component.class.getName())) {
        // 处理配置类对应 嵌套内部类
        processMemberClasses(configClass, sourceClass);
    }
    
    //省略...
}

//org.springframework.context.annotation.ConfigurationClassParser#processMemberClasses
private void processMemberClasses(ConfigurationClass configClass, SourceClass sourceClass) throws IOException {
    //获取配置类的内部类
    Collection<SourceClass> memberClasses = sourceClass.getMemberClasses();
    if (!memberClasses.isEmpty()) {
        List<SourceClass> candidates = new ArrayList<>(memberClasses.size());
        for (SourceClass memberClass : memberClasses) {
            //判断内部类是否配置类
            if (ConfigurationClassUtils.isConfigurationCandidate(memberClass.getMetadata()) &&
                !memberClass.getMetadata().getClassName().equals(configClass.getMetadata().getClassName())) {
                candidates.add(memberClass);
            }
        }
        OrderComparator.sort(candidates);
        for (SourceClass candidate : candidates) {
            //递归调用processConfigurationClass进行嵌套内部类处理
            processConfigurationClass(candidate.asConfigClass(configClass));
        }
    }
}
```



**小结**：第一部分主要用于解析配置类的嵌套内部类是否为一个配置类，如果是，则解析它



###### 第二部分：@PropertySources处理

```java
protected final SourceClass doProcessConfigurationClass(ConfigurationClass configClass, SourceClass sourceClass){
    //省略...
    for (AnnotationAttributes propertySource : AnnotationConfigUtils.attributesForRepeatable(
        sourceClass.getMetadata(), PropertySources.class,
        org.springframework.context.annotation.PropertySource.class)) {
        if (this.environment instanceof ConfigurableEnvironment) {
            processPropertySource(propertySource);
        }
        else {
            logger.info("Ignoring @PropertySource annotation on [" + sourceClass.getMetadata().getClassName() +
                        "]. Reason: Environment must implement ConfigurableEnvironment");
        }
    }
    //省略...
}
```



**小结**：第二部分主要用于解析`@PropertySources、@PropertySource`注解，加载其配置`properties`配置文件，解析对应属性添加到环境变量中，后续可以通过`@Value`注解进行引入注入，具体处理不去分析了



###### 第三部分：@ComponentScan处理

```java
protected final SourceClass doProcessConfigurationClass(ConfigurationClass configClass, SourceClass sourceClass){
    //省略...
    
    //获取对应@ComponentScan、@ComponentScans的注解属性信息
    Set<AnnotationAttributes> componentScans = AnnotationConfigUtils.attributesForRepeatable(
        sourceClass.getMetadata(), ComponentScans.class, ComponentScan.class);
    if (!componentScans.isEmpty() &&
        !this.conditionEvaluator.shouldSkip(sourceClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN)) {
        for (AnnotationAttributes componentScan : componentScans) {
            // 通过ComponentScanAnnotationParser处理需要扫描的包，获取对应BeanDefinitions
            Set<BeanDefinitionHolder> scannedBeanDefinitions =
                this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());
            // 遍历包扫描结果
            for (BeanDefinitionHolder holder : scannedBeanDefinitions) {
                BeanDefinition bdCand = holder.getBeanDefinition().getOriginatingBeanDefinition();
                if (bdCand == null) {
                    bdCand = holder.getBeanDefinition();
                }
                if (ConfigurationClassUtils.checkConfigurationClassCandidate(bdCand, this.metadataReaderFactory)) {
                    //如果被扫描BeanDefinition为配置类，则递归调用parse方法进行配置类解析
                    parse(bdCand.getBeanClassName(), holder.getBeanName());
                }
            }
        }
    }
    
    //省略...
}
```



实际处理委托给`ComponentScanAnnotationParser#parse`进行处理

```java
//org.springframework.context.annotation.ComponentScanAnnotationParser#parse
public Set<BeanDefinitionHolder> parse(AnnotationAttributes componentScan, final String declaringClass) {
    //创建ClassPathBeanDefinitionScanner实例
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(this.registry,
              componentScan.getBoolean("useDefaultFilters"), this.environment, this.resourceLoader);

    //属性解析，省略...

    //获取目标扫描包
    Set<String> basePackages = new LinkedHashSet<>();
    //获取basePackages属性对应包数组
    String[] basePackagesArray = componentScan.getStringArray("basePackages");
    for (String pkg : basePackagesArray) {
        //包通过 ，； \t\n 进行分割
        String[] tokenized = StringUtils.tokenizeToStringArray(this.environment.resolvePlaceholders(pkg),
                                                               ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
        //添加解析后的包
        Collections.addAll(basePackages, tokenized);
    }
    //获取basePackageClasses属性对应类
    for (Class<?> clazz : componentScan.getClassArray("basePackageClasses")) {
        //添加clazz所属的包
        basePackages.add(ClassUtils.getPackageName(clazz));
    }
    //如果上述配置都没有，则默认获取当前配置类所属包
    if (basePackages.isEmpty()) {
        basePackages.add(ClassUtils.getPackageName(declaringClass));
    }
    //排除当前配置类
    scanner.addExcludeFilter(new AbstractTypeHierarchyTraversingFilter(false, false) {
        @Override
        protected boolean matchClassName(String className) {
            return declaringClass.equals(className);
        }
    });
    //执行包扫描
    return scanner.doScan(StringUtils.toStringArray(basePackages));
}
```



我们发现，`ComponentScanAnnotationParser#parse`主要进行`@ComponentScan`注解的属性解析，获取对应`basePackages`，之后交由`ClassPathBeanDefinitionScanner#doScan`进行包扫描工作

```java
//org.springframework.context.annotation.ClassPathBeanDefinitionScanner#doScan
protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
    Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<>();
    for (String basePackage : basePackages) {
        //扫描包下符合条件的组件，默认是添加了@Component注解的类
        Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
        for (BeanDefinition candidate : candidates) {
            //生成beanName
            String beanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);
            if (checkCandidate(beanName, candidate)) {
                BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
                definitionHolder =
                    AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
                //保存到响应结果中
                beanDefinitions.add(definitionHolder);
                //将扫描的结果组件，注册其BeanDefinition到BeanFactory中
                registerBeanDefinition(definitionHolder, this.registry);
            }
        }
    }
    return beanDefinitions;
}
```



**小结**：对于`@ComponentScan`的注解的处理，主要实现两个功能：

- 获取注解配置对应包下添加了`@Component`注解的组件，并注册其`BeanDefinition`到`BeanFactory`中
- 如果扫描组件中存在配置类，则递归调用`ConfigurationClassParser#parse`方法解析配置类



###### 第四部分：@Import处理

```java
protected final SourceClass doProcessConfigurationClass(ConfigurationClass configClass, SourceClass sourceClass){
    //省略...
    
    // 处理任何@Import 注释
		processImports(configClass, sourceClass, getImports(sourceClass), true);
    
    //省略...
}
```



通过`getImports`获取配置类中`@Import`注解引入的类集合

```java
//org.springframework.context.annotation.ConfigurationClassParser#getImports
private Set<SourceClass> getImports(SourceClass sourceClass) throws IOException {
    Set<SourceClass> imports = new LinkedHashSet<>();
    Set<SourceClass> visited = new LinkedHashSet<>();
    collectImports(sourceClass, imports, visited);
    return imports;
}

//org.springframework.context.annotation.ConfigurationClassParser#collectImports
private void collectImports(SourceClass sourceClass, Set<SourceClass> imports, Set<SourceClass> visited) throws IOException {
    if (visited.add(sourceClass)) {
        //遍历处理配置类所有注解
        for (SourceClass annotation : sourceClass.getAnnotations()) {
            String annName = annotation.getMetadata().getClassName();
            if (!annName.equals(Import.class.getName())) {
                //如果不是@Import注解，则递归处理当前注解，主要用于处理注解上的@Import注解
                collectImports(annotation, imports, visited);
            }
        }
        //获取@Import注解引入的类集合
        imports.addAll(sourceClass.getAnnotationAttributes(Import.class.getName(), "value"));
    }
}
```



通过`processImports`方法，处理所引入类

```java
private void processImports(ConfigurationClass configClass, SourceClass currentSourceClass,
			Collection<SourceClass> importCandidates, boolean checkForCircularImports) {

    // 如果@Import注解配置引入value为空，直接返回
    if (importCandidates.isEmpty()) {
        return;
    }

    // 循环处理被引入类
    for (SourceClass candidate : importCandidates) {
        if (candidate.isAssignable(ImportSelector.class)) {
            // 1. 被引入类为ImportSelector子类，进行处理
            Class<?> candidateClass = candidate.loadClass();
            // 获取引入类的ImportSelector实例
            ImportSelector selector = BeanUtils.instantiateClass(candidateClass, ImportSelector.class);
            ParserStrategyUtils.invokeAwareMethods(
                selector, this.environment, this.resourceLoader, this.registry);
            if (selector instanceof DeferredImportSelector) {
                this.deferredImportSelectorHandler.handle(configClass, (DeferredImportSelector) selector);
            }
            else {
                // 调用ImportSelector#selectImports方法，获取由ImportSelector引入的类
                String[] importClassNames = selector.selectImports(currentSourceClass.getMetadata());
                Collection<SourceClass> importSourceClasses = asSourceClasses(importClassNames);
                // 递归调用processImports进行引入处理
                processImports(configClass, currentSourceClass, importSourceClasses, false);
            }
        } else if (candidate.isAssignable(ImportBeanDefinitionRegistrar.class)) {
            // 2. 被引入类为ImportBeanDefinitionRegistrar子类，进行处理
            Class<?> candidateClass = candidate.loadClass();
            // 获取被引入类ImportBeanDefinitionRegistrar实例
            ImportBeanDefinitionRegistrar registrar =
                BeanUtils.instantiateClass(candidateClass, ImportBeanDefinitionRegistrar.class);
            // 通过addImportBeanDefinitionRegistrar方法，将获取的ImportBeanDefinitionRegistrar实例保存到当前配置类的ConfigurationClass中
            configClass.addImportBeanDefinitionRegistrar(registrar, currentSourceClass.getMetadata());
        } else {
            // 3. 候选类不是 ImportSelector 或 ImportBeanDefinitionRegistrar，将其视为一个配置类进行处理
            // 递归调用ConfigurationClassParser#processConfigurationClass方法处理非ImportSelector、ImportBeanDefinitionRegistrar的类
            // 将通过ConfigurationClass#importedBy记录由那个配置类导入，具体逻辑在asConfigClass方法中
            processConfigurationClass(candidate.asConfigClass(configClass));
        }
    }
}
```



`addImportBeanDefinitionRegistrar`缓存`ImportBeanDefinitionRegistrar`实例到当前配置类的`ConfigurationClass`中

```java
//org.springframework.context.annotation.ConfigurationClass
final class ConfigurationClass {

    // key：ImportBeanDefinitionRegistrar实例
    // value：当前配置类注解元数据
    private final Map<ImportBeanDefinitionRegistrar, AnnotationMetadata> importBeanDefinitionRegistrars = new LinkedHashMap<>();

    public void addImportBeanDefinitionRegistrar(
        ImportBeanDefinitionRegistrar registrar, AnnotationMetadata importingClassMetadata) {	
        this.importBeanDefinitionRegistrars.put(registrar, importingClassMetadata);
    }
}
```



**小结**：对于`@Import`引入的类，分为三种

- `ImportSelector`子类
  - 创建其`ImportSelector`实例
  - 通过调用`ImportSelector#selectImports`方法，获取由ImportSelector引入的类
  - 递归调用`processImports`方法处理其引入的类
- `ImportBeanDefinitionRegistrar`子类
  - 创建其`ImportBeanDefinitionRegistrar`实例
  - 调用`addImportBeanDefinitionRegistrar`方法，将获取的`ImportBeanDefinitionRegistrar`实例保存到当前配置类的`ConfigurationClass`中
- 其他类型的类
  - 将其视为一个配置类进行处理
  - 递归调用ConfigurationClassParser#processConfigurationClass方法处理



###### 第五部分：@ImportResource处理

```java
protected final SourceClass doProcessConfigurationClass(ConfigurationClass configClass, SourceClass sourceClass){
    //省略...

    // 处理任何@ImportResource 注释
    // 获取@ImportResource注解元数据
    AnnotationAttributes importResource =
        AnnotationConfigUtils.attributesFor(sourceClass.getMetadata(), ImportResource.class);
    if (importResource != null) {
        // 获取引入application.xml路径对应resources资源
        String[] resources = importResource.getStringArray("locations");
        // 获取配置的BeanDefinitionReader解析对象Class，默认为BeanDefinitionReader
        Class<? extends BeanDefinitionReader> readerClass = importResource.getClass("reader");
        for (String resource : resources) {
            // 用properties变量替换资源路径中${name}值，得到结果的资源路径
            String resolvedResource = this.environment.resolveRequiredPlaceholders(resource);
            // 调用ConfigurationClass#addImportedResource方法，保存对应资源到配置类ConfigurationClass中
            configClass.addImportedResource(resolvedResource, readerClass);
        }
    }

    //省略...
}
```



调用`ConfigurationClass#addImportedResource`方法，保存对应资源到配置类`ConfigurationClass`中

```java
// org.springframework.context.annotation.ConfigurationClass
final class ConfigurationClass {
    
    // key：解析后application.xml路径，用properties变量替换资源路径中${name}值，得到结果的资源路径
    // value：对应BeanDefinitionReader解析器的Class对象
    private final Map<String, Class<? extends BeanDefinitionReader>> importedResources = new LinkedHashMap<>();

    public void addImportedResource(String importedResource, Class<? extends BeanDefinitionReader> readerClass) {
        this.importedResources.put(importedResource, readerClass);
    }
}
```



###### 第六部分：@Bean处理

```java
protected final SourceClass doProcessConfigurationClass(ConfigurationClass configClass, SourceClass sourceClass){
    //省略...

    // 处理@Bean注释方法
    // 获取配置类中所有@Bean注释的方法元数据集合
    Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(sourceClass);
    for (MethodMetadata methodMetadata : beanMethods) {
        // 调用ConfigurationClass#addBeanMethod方法，将@Bean对应方法报错到配置类ConfigurationClass中
        configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
    }

    //省略...
}
```



调用`ConfigurationClass#addBeanMethod`方法，保存对应资源到配置类`ConfigurationClass`中

```java
//org.springframework.context.annotation.ConfigurationClass
final class ConfigurationClass {
    
    // @Bean注解方法集合
	private final Set<BeanMethod> beanMethods = new LinkedHashSet<>();
    
    public void addBeanMethod(BeanMethod method) {
		this.beanMethods.add(method);
	}
}
```



###### 第七部分：接口上@Bean处理

```java
protected final SourceClass doProcessConfigurationClass(ConfigurationClass configClass, SourceClass sourceClass){
    //省略...

    // 处理接口上的@Bean方法
    processInterfaces(configClass, sourceClass);

    //省略...
}
```



通过`processInterfaces`解析配置类接口的所有方法，查找`@Bean`注释的方法

```java
//org.springframework.context.annotation.ConfigurationClassParser#processInterfaces
private void processInterfaces(ConfigurationClass configClass, SourceClass sourceClass) throws IOException {
    for (SourceClass ifc : sourceClass.getInterfaces()) {
        // 遍历所有的接口，获取其中@Bean注释的方法
        Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(ifc);
        for (MethodMetadata methodMetadata : beanMethods) {
            if (!methodMetadata.isAbstract()) {
                // Java 8+ 接口上的默认方法或其他具体方法...
                // 调用ConfigurationClass#addBeanMethod方法，将@Bean对应方法报错到配置类ConfigurationClass中
                configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
            }
        }
        //递归处理接口的接口
        processInterfaces(configClass, ifc);
    }
}
```



###### 第八部分：处理父类

```java
protected final SourceClass doProcessConfigurationClass(ConfigurationClass configClass, SourceClass sourceClass){
    //省略...

    //进程超类，如果有的话
    if (sourceClass.getMetadata().hasSuperClass()) {
        //获取父类
        String superclass = sourceClass.getMetadata().getSuperClassName();
        if (superclass != null && !superclass.startsWith("java") && !this.knownSuperclasses.containsKey(superclass)) {
            this.knownSuperclasses.put(superclass, configClass);
            // 返回父类sourceClass，通过processConfigurationClass方法中的do-while循环，继续处理父类
            return sourceClass.getSuperClass();
        }
    }

    // 没有父类，则返回空，processConfigurationClass方法中的do-while循环结束
    return null;
    
    //省略...
}
```



###### 作个小结

经过`doProcessConfigurationClass`的处理，除了最初注册的配置类信息，主要实现了以下：

- 将通过`@ComponentScan`扫描到的`@Component`标注的类的`BeanDefinition`注册到了IOC容器
- `ConfigurationClass`中额外存储信息
  - `ConfigurationClass#importedResources`
    - 保存通过`@ImportResource`加载的配置文件信息集合
  - `ConfigurationClass#importBeanDefinitionRegistrars`
    - 解析`@Import`注解获得的`ImportBeanDefinitionRegistrar`实例集合
  - `ConfigurationClass#beanMethods`
    - 解析`@Bean`注解获得的`BeanMethod`集合
- 新增的`ConfigurationClass`
  - 通过`@Import`注解导入的非`ImportSelector、ImportBeanDefinitionRegistrar`的类
  - 其通过`ConfigurationClass#importedBy`保存了导入其的配置类





##### 3. 处理解析ConfigurationClass

由代码可知，这一动作实际是委托给了`ConfigurationClassBeanDefinitionReader#loadBeanDefinitions`进行处理，下面我们分析`loadBeanDefinitions`方法



###### loadBeanDefinitions

```java
//org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader#loadBeanDefinitions
public void loadBeanDefinitions(Set<ConfigurationClass> configurationModel) {
    TrackedConditionEvaluator trackedConditionEvaluator = new TrackedConditionEvaluator();
    for (ConfigurationClass configClass : configurationModel) {
        //遍历调用loadBeanDefinitionsForConfigurationClass处理单个ConfigurationClass
        loadBeanDefinitionsForConfigurationClass(configClass, trackedConditionEvaluator);
    }
}
```



我们发现其调用了`loadBeanDefinitionsForConfigurationClass`方法处理单个`ConfigurationClass`

##### loadBeanDefinitionsForConfigurationClass

```java
//org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForConfigurationClass
private void loadBeanDefinitionsForConfigurationClass(
			ConfigurationClass configClass, TrackedConditionEvaluator trackedConditionEvaluator) {

    // 通过isImported方法判断是否通过@Import导入的的ConfigurationClass
    if (configClass.isImported()) {
        //处理@Import导入的ConfigurationClass，注册其BeanDefinition到IOC容器
        registerBeanDefinitionForImportedConfigurationClass(configClass);
    }

    //处理@Bean注解资源
    for (BeanMethod beanMethod : configClass.getBeanMethods()) {
        loadBeanDefinitionsForBeanMethod(beanMethod);
    }

    //处理@ImportResource注解添加资源
    loadBeanDefinitionsFromImportedResources(configClass.getImportedResources());

    //处理@Import注解添加的ImportBeanDefinitionRegistrar接口
    loadBeanDefinitionsFromRegistrars(configClass.getImportBeanDefinitionRegistrars());
}
```



###### 第一部分：处理@Import导入的普通类

```java
//org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForConfigurationClass
private void loadBeanDefinitionsForConfigurationClass(
    ConfigurationClass configClass, TrackedConditionEvaluator trackedConditionEvaluator) {
	
    //省略...
    
    // 通过isImported方法判断是否通过@Import导入的的ConfigurationClass
    if (configClass.isImported()) {
        //处理@Import导入的ConfigurationClass，注册其BeanDefinition到IOC容器
        registerBeanDefinitionForImportedConfigurationClass(configClass);
    }
    
    //省略...
}
```



先通过`ConfigurationClass#isImported`方法判断是否为被导入类

```java
final class ConfigurationClass {
	// 保存当前配置类由那个配置类导入
    private final Set<ConfigurationClass> importedBy = new LinkedHashSet<>(1);

    public boolean isImported() {
        return !this.importedBy.isEmpty();
    }
}
```

在`@Import`处理时，对于非`ImportSelector、ImportBeanDefinitionRegistrar`的类，将记录将其导入的配置类到`importedBy`中，这些类将注册到IOC容器中



通过`registerBeanDefinitionForImportedConfigurationClass`注册被`@Import`导入的普通类的`BeanDefinition`到IOC容器中

```java
//org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader#registerBeanDefinitionForImportedConfigurationClass
private void registerBeanDefinitionForImportedConfigurationClass(ConfigurationClass configClass) {
    //包装BeanDefinition
    AnnotationMetadata metadata = configClass.getMetadata();
    AnnotatedGenericBeanDefinition configBeanDef = new AnnotatedGenericBeanDefinition(metadata);
    BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(configBeanDef, configBeanName);
    definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
    
    //注册BeanDefinition到DefaultListableBeanFactory中
    this.registry.registerBeanDefinition(definitionHolder.getBeanName(), definitionHolder.getBeanDefinition());
    configClass.setBeanName(configBeanName);
}
```



###### 第二部分：处理注释@Bean的方法

```java
//org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForConfigurationClass
private void loadBeanDefinitionsForConfigurationClass(
    ConfigurationClass configClass, TrackedConditionEvaluator trackedConditionEvaluator) {
	
    //省略...
    
    //处理添加了@Bean注解的方法
    for (BeanMethod beanMethod : configClass.getBeanMethods()) {
    loadBeanDefinitionsForBeanMethod(beanMethod);
    }
    
    //省略...
}
```



具体解析分析`loadBeanDefinitionsForBeanMethod`方法

```java
//org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForBeanMethod
private void loadBeanDefinitionsForBeanMethod(BeanMethod beanMethod) {
    //获取方法所属类
    ConfigurationClass configClass = beanMethod.getConfigurationClass();
    // 获取方法名称
    MethodMetadata metadata = beanMethod.getMetadata();
    String methodName = metadata.getMethodName();

    //获取方法上@Bean注解属性
    AnnotationAttributes bean = AnnotationConfigUtils.attributesFor(metadata, Bean.class);
    
    // 考虑名称和任何别名
    List<String> names = new ArrayList<>(Arrays.asList(bean.getStringArray("name")));
    // 默认使用methodName作为beanName
    String beanName = (!names.isEmpty() ? names.remove(0) : methodName);

    //构建BeanDefinition
    ConfigurationClassBeanDefinition beanDef = new ConfigurationClassBeanDefinition(configClass, metadata);
    beanDef.setResource(configClass.getResource());
    beanDef.setSource(this.sourceExtractor.extractSource(metadata, configClass.getResource()));

    if (metadata.isStatic()) {
        // 静态@Bean 方法，设置beanClass、factoryMethodName，同XML配置中静态方法配置
        beanDef.setBeanClassName(configClass.getMetadata().getClassName());
        beanDef.setFactoryMethodName(methodName);
    }
    else {
        // 实例@Bean方法，设置factoryMethodName，同XML配置中实例方法配置
        beanDef.setFactoryBeanName(configClass.getBeanName());
        beanDef.setUniqueFactoryMethodName(methodName);
    }

    //注册BeanDefinition到IOC容器
    this.registry.registerBeanDefinition(beanName, beanDefToRegister);
}
```



###### 第三部分：处理@ImportResource引入配置文件

```java
//org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForConfigurationClass
private void loadBeanDefinitionsForConfigurationClass(
    ConfigurationClass configClass, TrackedConditionEvaluator trackedConditionEvaluator) {
	
    //省略...
    
    //处理@ImportResource注解添加资源
    loadBeanDefinitionsFromImportedResources(configClass.getImportedResources());
    
    //省略...
}
```



具体通过`loadBeanDefinitionsFromImportedResources`f方法解析`ConfigurationClass#importedResources`

```java
//org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsFromImportedResources
private void loadBeanDefinitionsFromImportedResources(Map<String, Class<? extends BeanDefinitionReader>> importedResources) {
    // 缓存BeanDefinitionReader实例
    Map<Class<?>, BeanDefinitionReader> readerInstanceCache = new HashMap<>();

    // 遍历
    importedResources.forEach((resource, readerClass) -> {
        //需要选择默认阅读器吗？
        if (BeanDefinitionReader.class == readerClass) {
            if (StringUtils.endsWithIgnoreCase(resource, ".groovy")) {
                // When clearly asking for Groovy, that's what they'll get...
                readerClass = GroovyBeanDefinitionReader.class;
            }
            else {
                // 主要是“.xml”文件，但也适用于任何其他扩展名
                readerClass = XmlBeanDefinitionReader.class;
            }
        }

        BeanDefinitionReader reader = readerInstanceCache.get(readerClass);
        if (reader == null) {
            try {
                // 实例化指定的 BeanDefinitionReader
                reader = readerClass.getConstructor(BeanDefinitionRegistry.class).newInstance(this.registry);
                // 如果可能，将当前的 ResourceLoader 委托给它
                if (reader instanceof AbstractBeanDefinitionReader) {
                    AbstractBeanDefinitionReader abdr = ((AbstractBeanDefinitionReader) reader);
                    abdr.setResourceLoader(this.resourceLoader);
                    abdr.setEnvironment(this.environment);
                }
                readerInstanceCache.put(readerClass, reader);
            }
            catch (Throwable ex) {
                throw new IllegalStateException(
                    "Could not instantiate BeanDefinitionReader class [" + readerClass.getName() + "]");
            }
        }

        //TODO SPR-6310：限定相对路径位置，如 AbstractContextLoader.modifyLocations 中所做的那样
        reader.loadBeanDefinitions(resource);
    });
}
```



**小结**：通常来说，就是通过`XmlBeanDefinitionReader`解析`application.xml`配置文件，获取`BeanDefinition`注册到IOC容器中，与XML配置方式一致



###### 第四部分：处理@Import注解添加的ImportBeanDefinitionRegistrar

```
//org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForConfigurationClass
private void loadBeanDefinitionsForConfigurationClass(
    ConfigurationClass configClass, TrackedConditionEvaluator trackedConditionEvaluator) {
	
    //省略...
    
    //处理@ImportResource注解添加资源
    loadBeanDefinitionsFromImportedResources(configClass.getImportedResources());
    
    //省略...
}
```



具体通过调用`loadBeanDefinitionsFromImportedResources`处理

```java
//org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsFromRegistrars
private void loadBeanDefinitionsFromRegistrars(Map<ImportBeanDefinitionRegistrar, AnnotationMetadata> registrars) {
    //遍历所有registrars，调用其具体registerBeanDefinitions方法，进行BeanDefinition的注册
    registrars.forEach((registrar, metadata) ->  registrar.registerBeanDefinitions(metadata, this.registry));
}
```



#### postProcessBeanFactory

`postProcessBeanFactory`实现自`BeanFactoryPostProcessor`，按照XML配置解析流程分析，`BeanFactoryPostProcessor#postProcessBeanFactory`在

`BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry`后执行

前面我们分析`postProcessBeanDefinitionRegistry`方法解析、加载了`BeanDefinition`注册到IOC容器

现在我们来分析一下`postProcessBeanFactory`方法

```java
//org.springframework.context.annotation.ConfigurationClassPostProcessor#postProcessBeanFactory
public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    //对加了@Configuration注解的类进行CGLIB代理
    enhanceConfigurationClasses(beanFactory);
    //向Spring中添加一个后置处理器ImportAwareBeanPostProcessor
    beanFactory.addBeanPostProcessor(new ImportAwareBeanPostProcessor(beanFactory));
}
```



实际`ConfigurationClassPostProcessor#postProcessBeanFactory`主要实现了两个功能：

- 对加了`@Configuration`注解的类进行CGLIB代理
- 向Spring中添加一个后置处理器`ImportAwareBeanPostProcessor`

下面我们一一分析



##### 1. enhanceConfigurationClasses

```java
// org.springframework.context.annotation.ConfigurationClassPostProcessor#enhanceConfigurationClasses
public void enhanceConfigurationClasses(ConfigurableListableBeanFactory beanFactory) {
    Map<String, AbstractBeanDefinition> configBeanDefs = new LinkedHashMap<>();
    for (String beanName : beanFactory.getBeanDefinitionNames()) {
        BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
        // 获取添加@Configuration注解的BeanDefinition
        if (ConfigurationClassUtils.isFullConfigurationClass(beanDef)) {
            configBeanDefs.put(beanName, (AbstractBeanDefinition) beanDef);
        }
    }
    if (configBeanDefs.isEmpty()) {
        return;
    }

    ConfigurationClassEnhancer enhancer = new ConfigurationClassEnhancer();
    for (Map.Entry<String, AbstractBeanDefinition> entry : configBeanDefs.entrySet()) {
        AbstractBeanDefinition beanDef = entry.getValue();
        Class<?> configClass = beanDef.resolveBeanClass(this.beanClassLoader);
        if (configClass != null) {
            // 通过ConfigurationClassEnhancer#enhance，创建CGLIB动态代理类Class
            Class<?> enhancedClass = enhancer.enhance(configClass, this.beanClassLoader);
            // 将当前BeanDefinition的beanClass替换，实现CGLIB代理
            beanDef.setBeanClass(enhancedClass);
        }
    }
}
```



可以发现`enhanceConfigurationClasses`作用：

- 通过`ConfigurationClassUtils#isFullConfigurationClass`筛选添加了`@Configuration`注解的完整配置类的`BeanDefinition`，即`Full`模式的
- 通过`ConfigurationClassEnhancer#enhance`，创建CGLIB动态代理类Class，再替换`BeanDefinition`的`beanClass`，实现功能增强

具体增强需要分析`ConfigurationClassEnhancer#enhance`



###### ConfigurationClassEnhancer

```java
class ConfigurationClassEnhancer {
    // 增强功能的CallBack接口数组
    private static final Callback[] CALLBACKS = new Callback[] {
			new BeanMethodInterceptor(),
			new BeanFactoryAwareMethodInterceptor(),
			NoOp.INSTANCE
	};
    
    /* 创建CGLIB动态代理类 */
    public Class<?> enhance(Class<?> configClass, @Nullable ClassLoader classLoader) {
		// 创建CGLIB动态代理类
		Class<?> enhancedClass = createClass(newEnhancer(configClass, classLoader));
		return enhancedClass;
	}
    
    /* 创建一个CGLIB代理Enhancer实例 */
    private Enhancer newEnhancer(Class<?> configSuperClass, @Nullable ClassLoader classLoader) {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(configSuperClass);
		enhancer.setInterfaces(new Class<?>[] {EnhancedConfiguration.class});
		enhancer.setUseFactory(false);
		enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
		enhancer.setStrategy(new BeanFactoryAwareGeneratorStrategy(classLoader));
		enhancer.setCallbackFilter(CALLBACK_FILTER);
		enhancer.setCallbackTypes(CALLBACK_FILTER.getCallbackTypes());
		return enhancer;
	}
    
    /* 创建动态代理类 */
    private Class<?> createClass(Enhancer enhancer) {
		// 通过Enhancer创建动态代理类
		Class<?> subclass = enhancer.createClass();
		// 静态注册回调（而不是线程本地） // 对于 OSGi 环境中的使用至关重要 (SPR-5932)...
		// 注册CallBack回调接口
		Enhancer.registerStaticCallbacks(subclass, CALLBACKS);
		// 返回动态代理类
		return subclass;
	}
}
```



可知`ConfigurationClassEnhancer#enhance`创建了CGLIB动态代理类

- 都实现了`EnhancedConfiguration`接口
  - 它其实是一个实现了`BeanFactoryAware`接口的空接口
- 此动态代理类存在一个名为`$$beanFactory`的属性

并注册了两个`CallBack`回调接口实例

- `BeanMethodInterceptor`
- `BeanFactoryAwareMethodInterceptor`





所以需要分析上述两个`MethodInterceptor`接口实例具体的增强逻辑，它们都是`ConfigurationClassEnhancer`的静态内部类

###### BeanFactoryAwareMethodInterceptor

```java
// org.springframework.context.annotation.ConfigurationClassEnhancer.BeanFactoryAwareMethodInterceptor
private static class BeanFactoryAwareMethodInterceptor implements MethodInterceptor, ConditionalCallback {

    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        // 从动态代理类中获取名为 $$beanFactory 的属性对应的Field实例
        Field field = ReflectionUtils.findField(obj.getClass(), BEAN_FACTORY_FIELD);
        Assert.state(field != null, "Unable to find generated BeanFactory field");
        // 设置当前setBeanFactory传入的BeanFactory到 $$beanFactory 属性上
        field.set(obj, args[0]);

        // 实际的（非 CGLIB）超类是否实现了 BeanFactoryAware？ // 如果是，调用它的 setBeanFactory() 方法。如果没有，就退出。
        if (BeanFactoryAware.class.isAssignableFrom(ClassUtils.getUserClass(obj.getClass().getSuperclass()))) {
            return proxy.invokeSuper(obj, args);
        }
        return null;
    }
	
    // 拦截setBeanFactory方法，此方法只有一个参数，且类型为BeanFactory
    public boolean isMatch(Method candidateMethod) {
        return isSetBeanFactory(candidateMethod);
    }

    public static boolean isSetBeanFactory(Method candidateMethod) {
        return (candidateMethod.getName().equals("setBeanFactory") &&
                candidateMethod.getParameterCount() == 1 &&
                BeanFactory.class == candidateMethod.getParameterTypes()[0] &&
                BeanFactoryAware.class.isAssignableFrom(candidateMethod.getDeclaringClass()));
    }
}
```



分析上述代码可知，`BeanFactoryAwareMethodInterceptor`主要功能

- 拦截实现自`BeanFactoryAware`接口的`setBeanFactory`方法的调用
  - 此方法在`SpringBean`初始化时调用
- 通过拦截`setBeanFactory`方法，获取到传入的`BeanFactory`实例，设置到动态代理类的 `$$beanFactory` 属性中



###### BeanMethodInterceptor

参照：https://www.cnblogs.com/yourbatman/p/13280344.html#lite%E6%A8%A1%E5%BC%8F%EF%BC%9A%E9%94%99%E8%AF%AF%E5%A7%BF%E5%8A%BF

在分析`BeanMethodInterceptor`处理前，我们先查看一个示例

```java
// 类创建
public class Son {}

@AllArgsConstructor
public class Parent {
	private Son son;
}

// Spring的配置类
// lite模式
public class MyConfiguration {
	@Bean
	public Son son(){
		Son son = new Son();
		System.out.println("son created..." + son.hashCode());
		return son;
	}

	@Bean
	public Parent parent() {
		// 调用this.son()方法获取son对象
		Son son = son();
		System.out.println("parent created...持有的Son是：" + son.hashCode());
		return new Parent(son);
	}
}
    
// full模式   
@Configuration 
public class MyConfiguration {
	@Bean
	public Son son(){
		Son son = new Son();
		System.out.println("son created..." + son.hashCode());
		return son;
	}

	@Bean
	public Parent parent() {
		// 调用this.son()方法获取son对象
		Son son = son();
		System.out.println("parent created...持有的Son是：" + son.hashCode());
		return new Parent(son);
	}
}    

//测试
AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(MyConfiguration.class);

// 测试输出
// lite模式
son created...1390913202	//IOC容器调用
son created...698741991		// parent方法调用this.son()
parent created...持有的Son是：698741991

// full模式    
son created...385739920		//IOC容器调用
parent created...持有的Son是：385739920
```



从上述例中可知

- Spring容器创建时，会调用配置类中`@Bean`注释的方法，获取实例
- `parent`方法中通过调用`this.son()`获取`son`实例，但是在不同配置类模式下，现象不同
  - lite模式
    - 调用`this.son()`，真正调用了`son`方法，创建了新的`son`实例
  - full模式
    - 没有在此调用`son`方法，而是从IOC容器中获取了`son`实例

那么为何会同样都是一样的调用代码，但是却有不同效果？



实际作用的就是`BeanMethodInterceptor`方法拦截器，下面我们具体分析

```java
// org.springframework.context.annotation.ConfigurationClassEnhancer.BeanMethodInterceptor
private static class BeanMethodInterceptor implements MethodInterceptor, ConditionalCallback {

    /*  匹配拦截，非setBeanFactory方法，有@Bean注释的方法 */
    public boolean isMatch(Method candidateMethod) {
        return (candidateMethod.getDeclaringClass() != Object.class &&
                !BeanFactoryAwareMethodInterceptor.isSetBeanFactory(candidateMethod) &&
                BeanAnnotationHelper.isBeanAnnotated(candidateMethod));
    }

    /* 具体拦截增强逻辑 */
    public Object intercept(Object enhancedConfigInstance, Method beanMethod, Object[] beanMethodArgs,
                            MethodProxy cglibMethodProxy) throws Throwable {
        // 获取BeanFactory
        ConfigurableBeanFactory beanFactory = getBeanFactory(enhancedConfigInstance);
        // 获取当前方法对应beanName，如果@Bean有配置，则取用，否则默认为方法名
        String beanName = BeanAnnotationHelper.determineBeanNameFor(beanMethod);

        // 如果是方法返回类型为FactoryBean，查看IOC容器中是否存在对应 beanName、$beanName 的实例
        if (factoryContainsBean(beanFactory, BeanFactory.FACTORY_BEAN_PREFIX + beanName) &&
            factoryContainsBean(beanFactory, beanName)) {
            // 从IOC容器中获取对应 $beanName 的实例
            Object factoryBean = beanFactory.getBean(BeanFactory.FACTORY_BEAN_PREFIX + beanName);
            // 代理增强这个factoryBean
            return enhanceFactoryBean(factoryBean, beanMethod.getReturnType(), beanFactory, beanName);

        }

        // 判断给定方法是否当前容器拦截调用的方法，主要针对内嵌调用，例 parent() 中调用 son()
        if (isCurrentlyInvokedFactoryMethod(beanMethod)) {
            // 直接调用被代理对象方法
            return cglibMethodProxy.invokeSuper(enhancedConfigInstance, beanMethodArgs);
        }

        // 处理内嵌调用情况
        return resolveBeanReference(beanMethod, beanMethodArgs, beanFactory, beanName);
    }
}
```



**逻辑**

- 获取前面通过`BeanFactoryAwareMethodInterceptor`设置的`BeanFactory`实例，实际就是从 `$$beanFactory` 属性的值
- 获取`beanName`
- `@Scope`注解处理，具体不分析
- 对于返回`FactoryBean`实例的处理
- `isCurrentlyInvokedFactoryMethod`方法判断是否内嵌调用
  - 如果不是，直接调用`invokeSuper`
  - 如果是，则调用`resolveBeanReference`方法进行处理



下面我们来具体分析

**isCurrentlyInvokedFactoryMethod**

由前面分析可知，对于`@Bean`注解注释的方法，我们将创建一个其对应的`BeanDefinition`注册到IOC容器中，而在由其`BeanDefinition`创建对应实例时，实际类似于实例/静态方法创建实例，所以将获取对应配置类对象，并调用其添加`@Bean`注解的方法获取实例

而由于通过`ConfigurationClassEnhancer`的处理，对应配置类实例是动态代理类实例，所以我们创建实例时，调用`@Bean`注解的方法将被`BeanMethodInterceptor`拦截，而在`@Bean`注释的方法中调用当前配置类的另一个被`@Bean`注释的方法时，由于另一个方法也被`@Bean`注解注释，它的调用也将被`BeanMethodInterceptor`拦截，但是两个调用是有区别的

- 对于直接的`@Bean`注释的方法调用，当前IOC容器创建的实例就是此方法的响应结果
- 而对于内嵌调用的`@Bean`注释的方法，它的响应结果并不是当前IOC容器创建的实例

而存储当前IOC容器创建的实例方法，是通过`SimpleInstantiationStrategy#currentlyInvokedFactoryMethod`对应的`ThreadLocal<Method>`进行存储

例如由上述例子中

- IOC容器创建`son`方法对应实例时，其在`ThredLocal`中存储的是`son`方法对应的`Method`
- IOC容器创建`parent`方法对应实例时，其在`ThredLocal`中存储的是`parent`方法对应的`Method`
- 而在`parent`方法中内嵌调用`son`方法获取`Son`实例时，此时并不是IOC容器创建`Son`实例，所以不会修改其`ThreadLocal`对应`Method`
  - 所以此时内嵌调用`son`方法时，其对应`ThreadLocal`的`Method`还是`parent`方法

```java
// org.springframework.context.annotation.ConfigurationClassEnhancer.BeanMethodInterceptor#isCurrentlyInvokedFactoryMethod
private boolean isCurrentlyInvokedFactoryMethod(Method method) {
    // 1. 获取当前SimpleInstantiationStrategy#currentlyInvokedFactoryMethod存储的Method，即创建当前SpringBean对应的Method
    Method currentlyInvoked = SimpleInstantiationStrategy.getCurrentlyInvokedFactoryMethod();
    // 2. 判断此时拦截的方法是否获取的Method
    return (currentlyInvoked != null && method.getName().equals(currentlyInvoked.getName()) &&
            Arrays.equals(method.getParameterTypes(), currentlyInvoked.getParameterTypes()));
}
```

所以`isCurrentlyInvokedFactoryMethod`可以判断当前被调用拦截`@Bean`注释的方法是否IOC容器发起的

- 如果是IOC容器发起，则直接调用`invokeSuper`
- 如果非IOC容器发起，而是`@Bean`注释的方法中主动调用`full`配置类中另一个被`@Bean`注解注释的方法，则调用`resolveBeanReference`进行处理



**resolveBeanReference**

```java
private Object resolveBeanReference(Method beanMethod, Object[] beanMethodArgs,
                                    ConfigurableBeanFactory beanFactory, String beanName) {
    // 主要逻辑，不是调用方法进行实例获取，而是从IOC容器中获取对应实例
    Object beanInstance = (useArgs ? beanFactory.getBean(beanName, beanMethodArgs) : beanFactory.getBean(beanName));
    return beanInstance;
}
```



**对于`FactoryBean`返回类型方法的处理**

执行此分支的前提是：IOC容器中已经存在对应`&beanName`和`beanName`两个Bean

那么意思是，当前对应`@Bean`注释的方法已经被IOC容器调用了，并且已经将生成的`SpringBean`保存到了IOC容器中；所以此处能进入分支，表示是内嵌调用了对应`@Bean`注释的方法，此时需要获取`FactoryBean`实例生产的对象

而由前面分析`FactoryBean`可知，其可以通过`isSingleton`方法控制从`FactoryBean`中获取的实例是否单例，实际控制是否单例实现：

- 如果是单例，则通过`beanName`从IOC容器中获取
- 如果是多例，则通过`&beanName`从IOC容器中获取对弈`FactoryBean`，再调用其`getObject`方法创建实例



对于`FactoryBean`返回类型方法的内嵌调用的处理有两步：

- 通过`&beanName`从IOC容器中获取对应`FactoryBean`
  - 不需要内嵌调用具体方法进行`FactoryBean`实例的获取，保证得到的`FactoryBean`是唯一的
- 通过`enhanceFactoryBean`代理包装获取的`FactoryBean`实例
  - 保证对于单例模式的`FactoryBean`，获取其管理的`Object`是唯一的
  - 实际就是通过`beanName`从IOC容器中获取实例



**enhanceFactoryBean**

```java
// org.springframework.context.annotation.ConfigurationClassEnhancer.BeanMethodInterceptor#enhanceFactoryBean
private Object enhanceFactoryBean(final Object factoryBean, Class<?> exposedType,
                                  final ConfigurableBeanFactory beanFactory, final String beanName) {

    // 获取FactoryBean的Class
    Class<?> clazz = factoryBean.getClass();
    // 判断Class是否被final修饰
    boolean finalClass = Modifier.isFinal(clazz.getModifiers());
    // 判断其getObject方法是否被final修饰
    boolean finalMethod = Modifier.isFinal(clazz.getMethod("getObject").getModifiers());
    // 如果是final修饰了类、getObject方法
    if (finalClass || finalMethod) {
        if (exposedType.isInterface()) {
            // 如果方法响应类型是一个接口，则是JDK动态代理
            return createInterfaceProxyForFactoryBean(factoryBean, exposedType, beanFactory, beanName);
        }
        else {
            // 如果是final修饰，又不是接口，则无法使用JDK、CGLIB动态代理，所以直接返回，不做代理处理
            return factoryBean;
        }
    }

    // 使用CGLIB动态代理
    return createCglibProxyForFactoryBean(factoryBean, beanFactory, beanName);
}


// JDK动态代理实现
private Object createInterfaceProxyForFactoryBean(final Object factoryBean, Class<?> interfaceType,
				final ConfigurableBeanFactory beanFactory, final String beanName) {
    return Proxy.newProxyInstance(
        factoryBean.getClass().getClassLoader(), new Class<?>[] {interfaceType},
        (proxy, method, args) -> {
            if (method.getName().equals("getObject") && args == null) {
                return beanFactory.getBean(beanName);
            }
            return ReflectionUtils.invokeMethod(method, factoryBean, args);
        });
}

// CGLIB动态代理实现
private Object createCglibProxyForFactoryBean(final Object factoryBean,
                                              final ConfigurableBeanFactory beanFactory, final String beanName) {

    Enhancer enhancer = new Enhancer();
    enhancer.setSuperclass(factoryBean.getClass());
    enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
    enhancer.setCallbackType(MethodInterceptor.class);

    // Ideally create enhanced FactoryBean proxy without constructor side effects,
    // analogous to AOP proxy creation in ObjenesisCglibAopProxy...
    Class<?> fbClass = enhancer.createClass();
    Object fbProxy = null;

    if (objenesis.isWorthTrying()) {
        try {
            fbProxy = objenesis.newInstance(fbClass, enhancer.getUseCache());
        }
        catch (ObjenesisException ex) {
            logger.debug("Unable to instantiate enhanced FactoryBean using Objenesis, " +
                         "falling back to regular construction", ex);
        }
    }

    if (fbProxy == null) {
        try {
            fbProxy = ReflectionUtils.accessibleConstructor(fbClass).newInstance();
        }
        catch (Throwable ex) {
            throw new IllegalStateException("Unable to instantiate enhanced FactoryBean using Objenesis, " +
                                            "and regular FactoryBean instantiation via default constructor fails as well", ex);
        }
    }

    ((Factory) fbProxy).setCallback(0, (MethodInterceptor) (obj, method, args, proxy) -> {
        if (method.getName().equals("getObject") && args.length == 0) {
            return beanFactory.getBean(beanName);
        }
        return proxy.invoke(factoryBean, args);
    });

    return fbProxy;
}
```



由上可知

- 除了对于`final`修饰了`类/getObject`的非接口类型返回值，都将对其`FactoryBean`实例进行动态代理
- 动态代理的主要实现就是对`getObject`进行拦截，不直接调用`getObject`，而是通过 `beanName` 从IOC容器中获取



###### 总结

`enhanceConfigurationClasses`方法主要是对于`full模式`(添加了 `@Configuration`注解)的配置类进行处理，通过 `ConfigurationClassEnhancer`进行CGLIB的动态代理，并注册两个 `CallBack` 接口实例进行拦截增强

- `BeanFactoryAwareMethodInterceptor`拦截 `setBeanFactory`方法
  - 将 `BeanFactory` 实例保存到动态代理类实例的 `$$beanFactory` 中
- `BeanMethodInterceptor` 拦截 `@Bean` 注释的方法
  - 主要目的是保证非IOC容器创建调用，在 `full模式` 配置类的 `@Bean`注释的方法中内嵌调用时，保证其内嵌调用 `@Bean`注释方法的到的响应结果是唯一的
  - 实质就是从IOC容器中获取，而不是调用具体方法创建

