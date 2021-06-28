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

## Spring容器

Spring容器，容器主要通过解析配置，进行对象实例的创建，管理实例对象的生命周期

Spring容器主要对应两个接口：

- BeanFactory
  - Spring容器顶级接口
  - org.springframework.beans.factory.BeanFactory
- ApplicationContext
  - Spring容器高级接口
  - BeanFactory接口的子接口
  - org.springframework.context.ApplicationContext

### 扩展类

#### AbstractApplicationContext

- ApplicationContext的抽象子类
- 包含Spring容器工作的主要代码

```java
// org.springframework.context.support.AbstractApplicationContext
public abstract class AbstractApplicationContext {
    //BeanFactory后置处理器集合
    private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();
    //持有DefaultListableBeanFactory容器对象
    private DefaultListableBeanFactory beanFactory;
    //xml资源路径，继承自org.springframework.context.support.AbstractRefreshableConfigApplicationContext
    private String[] configLocations;
    
    
    /* 容器具体工作入口 */
	public void refresh() {}
    /* beanFactoryPostProcessor注册 */
    public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
		this.beanFactoryPostProcessors.add(postProcessor);
	}
}
```

#### DefaultListableBeanFactory

- 成熟的beanFactory，Spring对象的管理容器对象

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

### 常用子类

```bash
# Spring容器主要作用是根据用户配置进行对象加载创建，而对应不同配置方式，解析的内容也不同，所以使用不同的子类

# 配置方式
	# 加载xml配置
        # ClassPathXmlApplicationContext
            # org.springframework.context.support.ClassPathXmlApplicationContext
            # 使用classpath类路径加载xml
        # FileSystemXmlApplicationContext
            # org.springframework.context.support.FileSystemXmlApplicationContext
            # 使用绝对路径加载xml(不推荐)
	# 加载注解配置主类
        # AnnotationConfigApplicationContext
            # org.springframework.context.annotation.AnnotationConfigApplicationContext
            # 加载Configuration配置类

# 共同点
	# 1. 都继承了AbstractApplicationContext抽象类
	# 2. 都持有DefaultListableBeanFactory容器对象进行Spring对象管理
	# 3. 主要工作入口
		# AbstractApplicationContext的refresh方法
```

## BeanDefinition定义

在Spring容器进行配置加载、对象创建时，不是解析配置后立即进行对象创建，而是将配置解析为对应BeanDefinition对象，BeanDefinition对象描述了Bean对象的具体信息(类型、实例化方式等)

### BeanDefinition

- Bean实例描述的顶级接口
- 定义了Bean描述的具体行为
- org.springframework.beans.factory.config.BeanDefinition

### 扩展类

#### AbstractBeanDefinition

- 成熟的BeanDefinition抽象基类
- 定义了BeanDefinition的大量属性和行为

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

### 常用子类

- org.springframework.beans.factory.support.RootBeanDefinition
- org.springframework.beans.factory.support.GenericBeanDefinition

## 容器工作流程

Spring容器对应于不同的配置方式，其使用的子类不同，其对应工作流程也有所不同。

单独使用为以下两种方式：

- XML配置
- 注解配置

此两种配置方式也可共用。

## XML配置解析

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

### 工作流程分析

基于上述配置，正常思路是：

1.  加载xml文件
2.  解析xml文件，获取BeanDefinition对象
3.  基于BeanDefinition对象，进行对象创建

而由于容器创建过程中还有一些特殊处理，所以整体流程大致如下：

1.  Application容器构造
2.  创建DefaultListableBeanFactory容器对象
3.  加载xml配置文件，获取BeanDefinition对象
4.  BeanFactoryPostProcessor对象实例化及后置处理
5.  BeanPostProcessor对象实例化
6.  Bean对象实例化
7.  Bean对象属性设置
8.  BeanPostProcessor初始化前置处理
9.  Bean对象初始化
10.  BeanPostProcessor初始化后置处理
11. Bean对象存储到Spring容器

下面就进行主体流程的分析

#### Application容器构造

- Spring容器的工作主要在Application容器构造时完成
- xml配置解析，常用ClassPathXmlApplicationContext子类，而ClassPathXmlApplicationContext子类工作主要在refresh方法中完成

```java
//org.springframework.context.support.AbstractApplicationContext#refresh
public void refresh() throws BeansException, IllegalStateException {
    // 准备此上下文以进行刷新.
    prepareRefresh();

    // 创建beanFactory工厂，加载beanDefinitions.
    ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

    // 加载BeanFactoryPostProcessor，并实例化，后通过postProcessBeanFactory方法对BeanFactory进行增强处理
    invokeBeanFactoryPostProcessors(beanFactory);

    // 加载注册BeanPostProcessor
    registerBeanPostProcessors(beanFactory);

    // 实例化所有剩余的（非延迟初始化）单例。
    finishBeanFactoryInitialization(beanFactory);
}
```

#### BeanFactory容器创建

```bash
# 入口
	# org.springframework.context.support.AbstractApplicationContext#obtainFreshBeanFactory
	
# 执行
	# org.springframework.context.support.AbstractRefreshableApplicationContext#createBeanFactory
	# new DefaultListableBeanFactory()
	# 默认实现：DefaultListableBeanFactory	
```

#### BeanDefinition加载

```bash
# 入口
	# org.springframework.context.support.AbstractApplicationContext#obtainFreshBeanFactory
	# org.springframework.context.support.AbstractRefreshableApplicationContext#loadBeanDefinitions
	
# 步骤
	# 1. 获取Application容器构造构造时保存的xml资源
		# org.springframework.context.support.AbstractRefreshableConfigApplicationContext#getConfigLocations
		
	# 2. XmlBeanDefinitionReader加载xml资源
		# org.springframework.beans.factory.support.AbstractBeanDefinitionReader#loadBeanDefinitions
		# 2.1 通过ResourceLoader加载xml资源
			# org.springframework.core.io.support.ResourcePatternResolver#getResources
			
	# 3. 解析xml资源，获取document
		# org.springframework.beans.factory.xml.XmlBeanDefinitionReader#doLoadBeanDefinitions
		
	# 4. 解析document
		# org.springframework.beans.factory.xml.XmlBeanDefinitionReader#registerBeanDefinitions
		# org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#doRegisterBeanDefinitions
		# 4.1 主要解析 "import", "alias", "bean" 标签
			# org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#parseBeanDefinitions
			# org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#parseDefaultElement
			
	# 5. bean标签解析
		# org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#processBeanDefinition
		# org.springframework.beans.factory.xml.BeanDefinitionParserDelegate#parseBeanDefinitionElement
		
	# 6.BeanDefinition注册到BeanFactory中
		# org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#processBeanDefinition
		# org.springframework.beans.factory.support.BeanDefinitionReaderUtils#registerBeanDefinition
		# org.springframework.beans.factory.support.DefaultListableBeanFactory#registerBeanDefinition
		
# 重点
	# DefaultListableBeanFactory#registerBeanDefinition是BeanDefinition的最终点，跟踪此方法进行调试可获取以下信息：
		# 1. 有哪些BeanDefinition注册到BeanFactory中
		# 2. 对应BeanDefinition的加载过程
```

#### BeanFactoryPostProcessor后置处理

##### BeanFactoryPostProcessor

- 一个后置处理器接口，允许在BeanDefinition加载后，进行额外操作：
  - 添加额外的BeanDefinition
  - 修改已加载BeanDefinition

```java
//org.springframework.beans.factory.config.BeanFactoryPostProcessor
@FunctionalInterface
public interface BeanFactoryPostProcessor {
	/* 后置处理方法 */
	void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;
}
```

##### 过程分析

```bash
# 入口
	# org.springframework.context.support.AbstractApplicationContext#invokeBeanFactoryPostProcessors
	# org.springframework.context.support.PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors
	
# 过程
# 1. 获取当前已注册beanFactoryPostProcessors
	# xml配置方式仅仅加载配置的beanFactoryPostProcessor，而注解配置方式可能会有前置的beanFactoryPostProcessor注册
	# org.springframework.context.support.AbstractApplicationContext#getBeanFactoryPostProcessors
	
# 2. 获取当前注册的BeanDefinition，其中为BeanFactoryPostProcessor子类的BeanDefinition
	# org.springframework.beans.factory.ListableBeanFactory#getBeanNamesForType
	
# 3. 根据当前BeanFactoryPostProcessor对应的BeanDefinition，获取其实例对象
	# org.springframework.beans.factory.BeanFactory#getBean
	
# 4. 遍历BeanFactoryPostProcessor实例，执行后置处理器
	# org.springframework.context.support.PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors
	# org.springframework.beans.factory.config.BeanFactoryPostProcessor#postProcessBeanFactory
	
# 注：
	# 此处进行了BeanFactoryPostProcessor的实例化及后置处理动作
```

#### BeanPostProcessor加载注册

##### BeanPostProcessor

- 一个允许修改实例化Bean的钩子接口

```java
//org.springframework.beans.factory.config.BeanPostProcessor
public interface BeanPostProcessor {
	/* Bean实例初始化前置处理 */
	Object postProcessBeforeInitialization(Object bean, String beanName);
	/* Bean实例初始化后置处理 */
	Object postProcessAfterInitialization(Object bean, String beanName);
}
```

##### 过程分析

```bash
# 入口
	# org.springframework.context.support.AbstractApplicationContext#registerBeanPostProcessors
	# org.springframework.context.support.PostProcessorRegistrationDelegate#registerBeanPostProcessors
	
# 过程
# 1. 获取当前注册的BeanDefinition，其中为BeanPostProcessor子类的BeanDefinition
	# org.springframework.beans.factory.ListableBeanFactory#getBeanNamesForType
	
# 2. 根据当前BeanPostProcessor对应的BeanDefinition，获取其实例对象
	# org.springframework.beans.factory.BeanFactory#getBean
	
# 3. 将获取的BeanPostProcessor实例注册到BeanFactory中
	# org.springframework.context.support.PostProcessorRegistrationDelegate#registerBeanPostProcessors
	# org.springframework.beans.factory.config.ConfigurableBeanFactory#addBeanPostProcessor
	# org.springframework.beans.factory.support.AbstractBeanFactory#addBeanPostProcessor
	# this.beanPostProcessors.add(beanPostProcessor);
	
# 重点
	# AbstractBeanFactory#addBeanPostProcessor是BeanPostProcessor注册的位置
	# org.springframework.beans.factory.support.AbstractBeanFactory#beanPostProcessors是BeanPostProcessor注册的容器
	# 跟踪这两点可以获取 BeanPostProcessor的注册和获取节点
```

#### Spring Bean创建

Bean根据生命周期(scope)区分主要为：

- singleton
  - 单例对象，在Spring容器创建时进行实例化
  - 容器中唯一，保存在单例池中
  - 由Spring容器负责整个生命周期(创建、销毁)
- prototype
  - 多例对象，程序从容器中获取时进行实例化
  - 存在多个，每次从容器中获取时，容器都会创建一个新的对象返回
  - Spring容器仅负责创建，而其他生命周期交由应用程序自行管理

所以在Spring容器的创建过程中，主要创建singleton单例Bean



Bean根据其特性分为以下两种：

- 普通Bean
- FactoryBean
  - 不是为了创建FactoryBean实例对象，而是通过getBean获取目标实例对象

##### 普通Bean创建入口

```bash
# Spring容器创建过程中进行单例创建的入口
	# org.springframework.context.support.AbstractApplicationContext#refresh
	# org.springframework.context.support.AbstractApplicationContext#finishBeanFactoryInitialization
	# org.springframework.beans.factory.config.ConfigurableListableBeanFactory#preInstantiateSingletons
	# org.springframework.beans.factory.support.AbstractBeanFactory#getBean
	# org.springframework.beans.factory.support.AbstractBeanFactory#doGetBean
	
# 单例对象和多例对象的主要区别在于是否由Spring容器管理，而Spring容器管理单例对象就是使用单例池：singletonObjects进行单例对象的存储
	# 所以单例对象的多了单例池单例池的控制，在于方法getSingleton
		# org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#getSingleton
	
# 而Bean实例创建的统一入口是
	# org.springframework.beans.factory.support.AbstractBeanFactory#createBean
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
```

##### Bean实例化

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

##### FactoryBean

- 单个对象工厂的接口

```java
//org.springframework.beans.factory.FactoryBean
public interface FactoryBean<T> {
	/* 返回工厂管理的实例 */
	T getObject() throws Exception;
	/* 返回对应实例类型 */
	Class<?> getObjectType();
	/* 是否单例 */
	default boolean isSingleton() {
		return true;
	}
}
```

###### 过程分析

```bash
# 对于FactoryBean的beanName，具有两种：
	# 配置的beanName
		# 表示要获取的是FactoryBean工厂管理的对象
	# &前缀 + 配置的beanName
		# 表示要获取的是FactoryBean工厂对象本身
	# 此处有一个判断方法
		# org.springframework.beans.factory.BeanFactoryUtils#isFactoryDereference

# BeanFactory创建过程中，FactoryBean实例化
	# 入口
		# org.springframework.beans.factory.support.DefaultListableBeanFactory#preInstantiateSingletons
	# 过程
		# 1. 判断当前要实例化的Bean是否是FactoryBean
			# org.springframework.beans.factory.support.AbstractBeanFactory#isFactoryBean(java.lang.String)
			# 获取当前beanName对应BeanDefinition
				# org.springframework.beans.factory.support.AbstractBeanFactory#getMergedLocalBeanDefinition
			# 判断当前BeanDefinition是否FactoryBean类型
				# org.springframework.beans.factory.support.AbstractBeanFactory#predictBeanType
                # 主要是使用Class类的isAssignableFrom方法
                	# java.lang.Class#isAssignableFrom
		# 2. 将name添加 &前缀 进行调用getBean方法进行实例化
			# org.springframework.beans.factory.support.AbstractBeanFactory#getBean(java.lang.String)
			# 2.1 获取消除 &前缀 的beanName
				# org.springframework.beans.factory.support.AbstractBeanFactory#transformedBeanName
			# 2.2 获取beanName对应BeanDefinition，进行对象实例化，过程同普通bean
				# org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#getSingleton
				# org.springframework.beans.factory.support.AbstractBeanFactory#createBean
			# 2.3 获取目标实例
				# org.springframework.beans.factory.support.AbstractBeanFactory#getObjectForBeanInstance

# 重点在于getObjectForBeanInstance方法中的判断
	# 1. 如果name中包含&前缀符号，则直接返回实例对象
    # 2. 如果name中不包含&前缀，则表示要获取FactoryBean工厂管理的对象
    	# org.springframework.beans.factory.support.FactoryBeanRegistrySupport#getObjectFromFactoryBean
    	# org.springframework.beans.factory.support.FactoryBeanRegistrySupport#doGetObjectFromFactoryBean
    	# org.springframework.beans.factory.FactoryBean#getObject

# 总结
	# 1. BeanFactory容器创建过程中，以添加&前缀的方法获取到FactoryBean对象存储到容器中
	# 2. 以beanName(bean标签配置的id)从容器中获取对象时，都是通过先从容器中获取FactoryBean对象，再进过getObjectForBeanInstance判断
		# 2.1 如果使用 & + beanName
			# 则返回FactoryBean对象本身
		# 2.2 如果使用 beanName
			# 则返回FactoryBean工厂管理的对象

# 额外知识点
# 1. 判断一个BeanDefinition是否为某类型/子类型
    # org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#predictBeanType
    # 主要判断依据为
    	# java.lang.Class#isAssignableFrom

# 2. 我们不仅可以通过beanName(即id)获取bean，还可以通过类型来获取bean对象，此时需要做：根据目标类型，获取对应beanName
	# 入口
		# org.springframework.context.support.AbstractApplicationContext#getBean(java.lang.Class<T>)
		# org.springframework.beans.factory.support.DefaultListableBeanFactory#resolveNamedBean
		# org.springframework.beans.factory.support.DefaultListableBeanFactory#getBeanNamesForType
		# org.springframework.beans.factory.support.DefaultListableBeanFactory#doGetBeanNamesForType
	# 过程
		# 1. 遍历所有的注册的beanDefinitionNames
		# 2. 判断当前beanName是否对应目标类型
			# org.springframework.beans.factory.support.AbstractBeanFactory#isTypeMatch
			# 先获取当前beanName对应实例
				# org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#getSingleton
			# 判断
                # 1. 如果beanName对应是FactoryBean，则判断对应管理对象是否目标类型
                    # org.springframework.beans.factory.support.FactoryBeanRegistrySupport#getTypeForFactoryBean
                    # org.springframework.beans.factory.FactoryBean#getObjectType
                # 2. 如果beanName对应不是是FactoryBean，则判断当前实例是否目标类型
                	# org.springframework.core.ResolvableType#isInstance
		# 3. 否则尝试匹配FactoryBean本身
		
# 3. FactoryBean的单例和多例管理
	# 实际是表示是否重复调用FactoryBean的getObject进行对象获取,如果getObject中返回的对象是唯一的，则获取的对象也是唯一
	# 入口
		# org.springframework.beans.factory.support.FactoryBeanRegistrySupport#getObjectFromFactoryBean
	# 判断
		# 多例
			# 则直接调用getObject从FactoryBean中获取对象
				# org.springframework.beans.factory.support.FactoryBeanRegistrySupport#doGetObjectFromFactoryBean
				# org.springframework.beans.factory.FactoryBean#getObject
		# 单例
			# 使用FactoryBeanRegistrySupport类的factoryBeanObjectCache存储FactoryBean管理的对象
				# org.springframework.beans.factory.support.FactoryBeanRegistrySupport#factoryBeanObjectCache
					# Map<String, Object> factoryBeanObjectCache = new ConcurrentHashMap<>(16);
			# 如果factoryBeanObjectCache缓存中存在，则直接返回，否则获取后存储
```

##### 延迟加载

```bash
# 延迟加载
	# 对于单例bean对象，不在Spring容器初始化时创建，而是在第一次获取使用时才会实例化
	# 1. 容器创建时，如果是延迟加载，则不进行对象创建
        # org.springframework.beans.factory.support.DefaultListableBeanFactory#preInstantiateSingletons
        # org.springframework.beans.factory.support.AbstractBeanDefinition#isLazyInit
	# 2. 第一次获取对象时
		# 由于容器创建时没有创建对应对象，此时单例池中没有对应实例，所以需要创建对象
		# org.springframework.beans.factory.support.AbstractBeanFactory#doGetBean
		# 尝试单例池中获取
			# org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#getSingleton
		# 获取失败后创建对象
			# org.springframework.beans.factory.support.AbstractBeanFactory#createBean
		# 之后将对象缓存到单例池
			# org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#addSingleton
	# 3. 第二次获取则直接从单例池中获取
```

#### Bean属性设置

bean配置过程中，对于构造参数constructor-arg，是对象实例化时进行设置，而对于普通属性property，则需要进行设置

```bash
# 入口
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#populateBean

# 过程
	# 1. 从BeanDefinition中获取当前对象的属性参数
		# org.springframework.beans.factory.support.AbstractBeanDefinition#getPropertyValues
	# 2. 实质就是遍历属性获取其对应的公开set方法，进行方法调用
		# java.lang.reflect.Method#invoke
```

##### 循环依赖问题

```bash
# Bean可以依赖另一个Bean对象作为属性，此时当前bean对象进行属性设置时，需要先获取当前属性对应的bean实例
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#populateBean
	# org.springframework.beans.factory.support.BeanDefinitionValueResolver#resolveReference
	# org.springframework.beans.factory.support.AbstractBeanFactory#getBean
	
# 而如果此时有两个Bean相互依赖时，由于getBean方法返回的是成熟的springBean对象(即已经经过属性设置阶段)，此时就会出现我们所说的循环依赖问题

# 对此spring框架也是提供了解决方案，但是仍旧有其限制
	# 单例
		# 构造器参数循环依赖
			# 影响了对象实例化，所以无法解决
			# 报错BeanCurrentlyInCreationException
		# setter方式循环依赖
			# 三级缓存实现
	# 多例
		# 由于多例bean不由spring容器管理，无法进行循环依赖解决
		
# spring三级缓存介绍
	# 三级缓存管理对象
		# DefaultListableBeanFactory
			# 继承了类org.springframework.beans.factory.support.DefaultSingletonBeanRegistry
			# 主要控制代码在DefaultSingletonBeanRegistry中
	# singletonObjects
		# 就是我们常说的单例池
	# earlySingletonObjects
		# 二级缓存
	# singletonFactories
		# 三级缓存
```

###### DefaultSingletonBeanRegistry

- 负责三级缓存管理
- DefaultSingletonBeanRegistry父类

```java
//org.springframework.beans.factory.support.DefaultSingletonBeanRegistry
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {
    // 单例池
	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
	// 单例工厂缓存：三级缓存
	private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);
	// 早期单例对象的缓存：二级缓存
	private final Map<String, Object> earlySingletonObjects = new HashMap<>(16);
    
    
    /** 从Registry中获取单例对象 */
    protected Object getSingleton(String beanName, boolean allowEarlyReference) {
        //优先从单例池中获取
		Object singletonObject = this.singletonObjects.get(beanName);
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
			synchronized (this.singletonObjects) {
                //单例池中没有，则从二级缓存中获取
				singletonObject = this.earlySingletonObjects.get(beanName);
				if (singletonObject == null && allowEarlyReference) {
                    //二级缓存中没有，则从三级缓存中获取
					ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
					if (singletonFactory != null) {
                        //从工厂对象中获取目标对象
						singletonObject = singletonFactory.getObject();
                        //从三级缓存中移入二级缓存
						this.earlySingletonObjects.put(beanName, singletonObject);
						this.singletonFactories.remove(beanName);
					}
				}
			}
		}
		return singletonObject;
	}
}
```

###### ObjectFactory

- 一个对象工厂
- 三级缓存的实体对象

```java
//org.springframework.beans.factory.ObjectFactory
@FunctionalInterface
public interface ObjectFactory<T> {
	T getObject() throws BeansException;
}
```

###### 过程分析

```bash
# 从spring容器BeanFactory中获取单例对象的入口是
	# org.springframework.beans.factory.support.AbstractBeanFactory#doGetBean 
	
#过程
	# 1. 尝试从DefaultSingletonBeanRegistry中获取单例对象 A
		# org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#getSingleton
		# 1.1 优先从单例池中获取
        # 1.2 单例池中没有，则尝试从二级缓存中获取
        # 1.3 二级缓存中没有，则尝试从三级缓存中获取
        	# 如果存在，则将对象从三级缓存移动到二级缓存
        	
	# 2. 单例池中无法获取则进行对象创建 对象A
        # org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBean
        # org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
        # org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBeanInstance
	# 3. 对象A 实例创建后，主动将其保存到三级缓存中
		# 1. 获取三级缓存对象
			# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#getEarlyBeanReference
		# 2. 保存到三级缓存中
			# org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#addSingletonFactory
	# 4. 对象A进行属性设置
		# 由于对象A 依赖了 对象B，此时需要从容器中获取对象B(进入处理支线：对象B的创建过程)
		# 1. 调用getSingleton获取对象B
			# 重复上述步骤1/2/3/4
		# 2. 此时对象B 引用了 对象A，进行对象B的属性设置，当前情况是：对象A、B都保存在三级缓存singletonFactories中
			# org.springframework.beans.factory.support.AbstractBeanFactory#getBean
			# org.springframework.beans.factory.support.AbstractBeanFactory#doGetBean
		# 3. 尝试从DefaultSingletonBeanRegistry中获取单例对象 A
			# org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#getSingleton
			# 此时对象A存在于三级缓存中
				# 则获取对象A ，并将对象A 由三级缓存移入二级缓存中
				# 此时对象A 不是成熟SpringBean，没有进行属性设置
		# 4. 设置对象B 的属性A 为获取到的 A实例对象，当前情况是：对象A存在于二级缓存，对象B存在于三级缓存
		# 5. 对象B进行后续流程
			# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#initializeBean
                # BeanPostProcessor初始化前置处理
                # 初始化
                # BeanPostProcessor初始化后置处理
		# 6. 此时对象B 创建为成熟SpringBean，此时将对象B 添加到单例池中
			# org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#addSingleton
		# 7. 将获取到的对象B 设置为 对象A的属性，退出处理支线。当前情况是：对象A存在于二级缓存，对象B存在于单例池
	# 5. 对象A进行后续流程
	# 6. 此时对象A 创建为成熟SpringBean，此时将对象A 添加到单例池中
		#  org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#addSingleton
	# 7. 到了此时，对象A、B都保存到了单例池中
	
# 总结
	# 1. 对象实例化
	# 2. 所有对象实例化后都将其保存到三级缓存中
	# 3. 属性设置
		# 尝试逐级缓存中获取对象。特：如果对象保存在三级缓存中被引用获取，则获取，并放入二级缓存中
		# 如果引用对象不存在与缓存中，则递归进行引用对象的创建
		# 获取到对象后进行属性设置
	# 4. 对象进行后置处理，并保存到单例池中，创建过程完成
```

#### BeanPostProcessor初始化前置处理

```bash
# 入口
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#initializeBean
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#applyBeanPostProcessorsBeforeInitialization
# 描述
	# 遍历所有当前注册的beanPostProcessors，顺序执行其postProcessBeforeInitialization，将处理后的对象返回
```

#### Bean对象初始化

```bash
# 入口
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#initializeBean
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#invokeInitMethods

# xml配置模式有两种初始化方式
	# 1. 实现InitializingBean接口
		# 优先执行
		# 判断如果当前对象为InitializingBean实例，则执行afterPropertiesSet
			# bean instanceof InitializingBean
			# ((InitializingBean) bean).afterPropertiesSet()
    # 2. bean标签配置初始化方法
        # init-method
        # BeanDefinition
        	# initMethodName
        # org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#invokeCustomInitMethod
        	# 获取当前类对应方法
        		# java.lang.Class#getMethods
        	# 获取与initMethodName同名的方法对象
        	# 执行方法
        		# java.lang.reflect.Method#invoke
        		
# 注
	# 初始化方法应是无参方法
```

##### InitializingBean

- 一个初始化接口，实现对象初始化操作

```java
//org.springframework.beans.factory.InitializingBean
public interface InitializingBean {
	void afterPropertiesSet() throws Exception;
}
```

#### BeanPostProcessor初始化后置处理

```bash
# 入口
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#initializeBean
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#applyBeanPostProcessorsAfterInitialization
# 描述
	# 遍历所有当前注册的beanPostProcessors，顺序执行其postProcessAfterInitialization，将处理后的对象返回
```

### 重点处理节点总结

```bash
# 主处理流程入口
	# org.springframework.context.support.AbstractApplicationContext#refresh
	
# BeanFactory工厂创建	
	# org.springframework.context.support.AbstractApplicationContext#obtainFreshBeanFactory
	# org.springframework.context.support.AbstractRefreshableApplicationContext#refreshBeanFactory
	# org.springframework.context.support.AbstractRefreshableApplicationContext#createBeanFactory
	
# BeanDefinition加载
	# org.springframework.context.support.AbstractRefreshableApplicationContext#loadBeanDefinitions
	# org.springframework.beans.factory.xml.XmlBeanDefinitionReader#doLoadBeanDefinitions
	# org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#doRegisterBeanDefinitions
	# org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#parseBeanDefinitions
	# org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#parseDefaultElement
	# org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#processBeanDefinition
	# 加载后的BeanDefinition注册到BeanFactory中
		# org.springframework.beans.factory.support.BeanDefinitionReaderUtils#registerBeanDefinition
		# org.springframework.beans.factory.support.DefaultListableBeanFactory#registerBeanDefinition
	
# BeanFactoryPostProcessor加载处理
	# org.springframework.context.support.AbstractApplicationContext#invokeBeanFactoryPostProcessors
	# org.springframework.context.support.PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors
		# 根据类型获取符合的beanName
			# org.springframework.beans.factory.ListableBeanFactory#getBeanNamesForType
		# 根据beanName获取对象
			# org.springframework.beans.factory.BeanFactory#getBean
		# 执行后置处理
			# org.springframework.context.support.PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors
		
# BeanPostProcessor加载注册
	# org.springframework.context.support.AbstractApplicationContext#registerBeanPostProcessors
	# org.springframework.context.support.PostProcessorRegistrationDelegate#registerBeanPostProcessors
		# 根据类型获取符合的beanName
			# org.springframework.beans.factory.ListableBeanFactory#getBeanNamesForType
		# 根据beanName获取对象
			# org.springframework.beans.factory.BeanFactory#getBean
		# 注册BeanPostProcessor
			# org.springframework.context.support.PostProcessorRegistrationDelegate#registerBeanPostProcessors
		
# Spring Bean创建
	# org.springframework.context.support.AbstractApplicationContext#finishBeanFactoryInitialization
	# org.springframework.beans.factory.support.DefaultListableBeanFactory#preInstantiateSingletons
    # 根据beanName获取对象
		# org.springframework.beans.factory.support.AbstractBeanFactory#getBean(java.lang.String)
	# 执行对象创建		
		# org.springframework.beans.factory.support.AbstractBeanFactory#doGetBean
	# 获取缓存单例对象/单例对象创建
		# org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#getSingleton
	# FactoryBean后处理
		# org.springframework.beans.factory.support.AbstractBeanFactory#getObjectForBeanInstance
	# 对象创建
		# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
		# 对象实例化
			# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBeanInstance
		# 保存到三级缓存
			# org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#addSingletonFactory
		# 属性设置
			# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#populateBean
		# 初始化过程
			# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#initializeBean
			# BeanPostProcessors前置处理
				# applyBeanPostProcessorsBeforeInitialization
			# 初始化操作
				# invokeInitMethods
			# BeanPostProcessors后置处理
				# applyBeanPostProcessorsAfterInitialization
	# 保存对象到单例池
    	# org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#addSingleton
```

## 注解配置解析

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

### 工作流程分析

注解方式不同于xml配置方式

- Application容器构造

- BeanFactory创建
  - 不在obtainFreshBeanFactory方法中，而是在AnnotationConfigApplicationContext父类构建时创建
  - 构建AnnotatedBeanDefinitionReader

- 主配置类加载
- BeanDefinition加载
  - 通过ConfigurationClassPostProcessor的后置处理方法实现postProcessBeanDefinitionRegistry
- 后续流程大致与xml配置方式相同

#### BeanFactory创建

```bash
# GenericApplicationContext为AnnotationConfigApplicationContext的父类
# 在AnnotationConfigApplicationContext构造时，会调用父类GenericApplicationContext的构造
# 在GenericApplicationContext构造中，会创建beanFactory，默认实现DefaultListableBeanFactory
	# org.springframework.context.support.GenericApplicationContext#GenericApplicationContext()
		# this.beanFactory = new DefaultListableBeanFactory();
```

#### 构建AnnotatedBeanDefinitionReader

```bash
# 在AnnotationConfigApplicationContext构造时，会创建AnnotatedBeanDefinitionReader
# 在AnnotatedBeanDefinitionReader构造方法中，调用了registerAnnotationConfigProcessors方法
	# org.springframework.context.annotation.AnnotationConfigUtils#registerAnnotationConfigProcessors
	# 注册了一下几个注解处理器的BeanDefinition
		# ConfigurationClassPostProcessor
		# AutowiredAnnotationBeanPostProcessor
		# CommonAnnotationBeanPostProcessor
```

#### 主配置类加载

```bash
# 主配置类是指AnnotationConfigApplicationContext构造时传入的参数，而不是表示@Configuration注释的类
# 入口
	# org.springframework.context.annotation.AnnotationConfigApplicationContext#AnnotationConfigApplicationContext
	# org.springframework.context.annotation.AnnotationConfigApplicationContext#register
	# org.springframework.context.annotation.AnnotatedBeanDefinitionReader#register
	# org.springframework.context.annotation.AnnotatedBeanDefinitionReader#doRegisterBean
	
# 步骤
	# 1. 创建配置类对应BeanDefinition(GenericBeanDefinition的子类)
		# org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition
	# 2. 解析主配置类上通用注解
		# org.springframework.context.annotation.AnnotationConfigUtils#processCommonDefinitionAnnotations
			# @Lazy
			# @Primary
			# @DependsOn
			# @Role
			# @Description
	# 3. 注册主配置类BeanDefinition到BeanFactory中
		# org.springframework.beans.factory.support.BeanDefinitionReaderUtils#registerBeanDefinition
		# org.springframework.beans.factory.support.DefaultListableBeanFactory#registerBeanDefinition
```

#### ConfigurationClassPostProcessor后置处理

##### BeanDefinitionRegistryPostProcessor

```java
//org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
//继承了BeanFactoryPostProcessor接口
public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {

	/** 允许在常规BeanFactoryPostProcessor处理前，注册Bean定义 */
	void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException;
}
```

ConfigurationClassPostProcessor实现了BeanDefinitionRegistryPostProcessor接口，所以重写了以下两个方法

- postProcessBeanDefinitionRegistry
- postProcessBeanFactory

##### postProcessBeanDefinitionRegistry处理

```bash
# 入口
	# org.springframework.context.support.PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors
	# 1. 先获取当前对应BeanDefinitionRegistryPostProcessor的BeanDefinition
		# org.springframework.beans.factory.ListableBeanFactory#getBeanNamesForType
	# 2. 获取实例
        # org.springframework.beans.factory.BeanFactory#getBean
    # 3. 执行BeanDefinitionRegistryPostProcessor的postProcessBeanDefinitionRegistry后置处理
        # org.springframework.context.support.PostProcessorRegistrationDelegate#invokeBeanDefinitionRegistryPostProcessors
        # org.springframework.context.annotation.ConfigurationClassPostProcessor#postProcessBeanDefinitionRegistry
        # org.springframework.context.annotation.ConfigurationClassPostProcessor#processConfigBeanDefinitions
        
# 具体处理
	# 入口
		# org.springframework.context.annotation.ConfigurationClassPostProcessor#processConfigBeanDefinitions
    # 1. 获取AnnotationConfigApplicationContext中主配置类，获取其中配置类
        # 1. 配置类判断
            # org.springframework.context.annotation.ConfigurationClassUtils#checkConfigurationClassCandidate
            # 1. 判断其是否存在@Configuration注解
                # org.springframework.context.annotation.ConfigurationClassUtils#isFullConfigurationCandidate
                # 主要判断依据
                    # 1. 获取类上注解
                        # java.lang.Class#getAnnotations
                    # 2. 获取注解类名称进行比较
                        # java.lang.Class#getName
                    # 3. 如果没有，则递归进行比较，知道全部递归完成
                # 如果存在，则设置配置属性为full
            # 2. 判断其是否存在@Bean，@Component，@ComponentScan，@Import，@ImportResource这些注解
                # org.springframework.context.annotation.ConfigurationClassUtils#isLiteConfigurationCandidate
    # 2. 解析获取的配置类
        # org.springframework.context.annotation.ConfigurationClassParser#parse
        # org.springframework.context.annotation.ConfigurationClassParser#processConfigurationClass
        # org.springframework.context.annotation.ConfigurationClassParser#doProcessConfigurationClass	
        
    # 3. 调用当前reader的loadBeanDefinitions方法
    	# org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader#loadBeanDefinitions
```

###### 配置类解析过程

```bash
# 入口
	# org.springframework.context.annotation.ConfigurationClassParser#processConfigurationClass
	# org.springframework.context.annotation.ConfigurationClassParser#doProcessConfigurationClass

# 1. 处理配置内部类
	# org.springframework.context.annotation.ConfigurationClassParser#processMemberClasses
	# 如果其内部类也是一个配置类，则将其也进行处理
		# 调用处理方法processConfigurationClass

# 2. 处理@PropertySources注解
	# 2.1 获取其对应PropertySource属性数组
		# org.springframework.context.annotation.AnnotationConfigUtils#attributesForRepeatable
	# 2.2 对应资源加载
		# org.springframework.context.annotation.ConfigurationClassParser#processPropertySource

# 3. 处理@ComponentScans、@ComponentScan注解
	# 3.1 获取其对应注解扫描包 componentScans
		# org.springframework.context.annotation.AnnotationConfigUtils#attributesForRepeatable
	# 3.2 解析ComponentScan注解
		# org.springframework.context.annotation.ComponentScanAnnotationParser#parse
		# 3.2.1 获取包，如果没有配置basePackages，则获取当前配置类的所在包
		# 3.2.2 解析包内资源
			# org.springframework.context.annotation.ClassPathBeanDefinitionScanner#doScan
			# 3.2.2.1 获取包下所有类资源对应BeanDefinition
				# org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider#findCandidateComponents
				# org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider#scanCandidateComponents
				# 类资源加载
					# org.springframework.core.io.support.ResourcePatternResolver#getResources
				# 资源读取
					# org.springframework.core.type.classreading.MetadataReaderFactory#getMetadataReader
				# 创建BeanDefinition
					# org.springframework.context.annotation.ScannedGenericBeanDefinition
			# 3.2.3.2 将解析后的BeanDefinition注册到BeanFactory
				# org.springframework.context.annotation.ClassPathBeanDefinitionScanner#registerBeanDefinition
				# org.springframework.beans.factory.support.BeanDefinitionReaderUtils#registerBeanDefinition
				# org.springframework.beans.factory.support.BeanDefinitionRegistry#registerBeanDefinition
		# 3.2.3 判断当前注册的类，如果此类也是配置类，则递归调用processConfigurationClass
			# org.springframework.context.annotation.ConfigurationClassUtils#checkConfigurationClassCandidate

# 4. 处理@Import注解
	# 1. 获取类上@Import注解的的引入类
		# org.springframework.context.annotation.ConfigurationClassParser#getImports
	# 2. 处理引入类
		# org.springframework.context.annotation.ConfigurationClassParser#processImports
		# 引入类分为三种
			# 1. 实现ImportSelector接口
				# 1. 获取对应选择的引入类
					# org.springframework.context.annotation.ImportSelector#selectImports
				# 2. 递归引入
					# org.springframework.context.annotation.ConfigurationClassParser#processImports
			# 2. 实现ImportBeanDefinitionRegistrar接口
				# 1. 获取对应ImportBeanDefinitionRegistrar实例
					# org.springframework.beans.BeanUtils#instantiateClass
				# 2. 注册到importBeanDefinitionRegistrars调用registerBeanDefinitions方法
					# Map<ImportBeanDefinitionRegistrar, AnnotationMetadata>
					# org.springframework.context.annotation.ConfigurationClass#addImportBeanDefinitionRegistrar
			# 3. 其他类型
				# 调用processConfigurationClass解析

# 5. 处理@ImportResource注解
	# 添加资源到importedResources(Map<String, Class<? extends BeanDefinitionReader>>)待后续解析
		# org.springframework.context.annotation.ConfigurationClass#addImportedResource
		
# 6. 处理@Bean注解
	# 1. 检索所有带有@Bean注解的方法
		# org.springframework.context.annotation.ConfigurationClassParser#retrieveBeanMethodMetadata
	# 2. 添加其到beanMethods(Set<BeanMethod>)后续处理
		# org.springframework.context.annotation.ConfigurationClass#addBeanMethod
		
# 7. 默认方法处理
	# 例
		# AppConfig类加了Configuration注解，是一个配置类，且实现了AppConfigInterface接口
		# 接口中有一个默认的实现方法(JDK8开始，接口中的方法可以有默认实现)，该方法上添加了@Bean注解
		# 此时会向Spring容器中添加一个InterfaceMethodBean类型的bean
		
# 8. 解析父类
	# 被解析的配置类继承了某个类，那么配置类的父类也会被进行解析doProcessConfigurationClass()(父类是JDK内置的类例外，即全类名以java开头的)
```

###### reader资源加载

```bash
# 入口
	# org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader#loadBeanDefinitions
	# org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForConfigurationClass
	
# 作用
	# 处理前面被解析的类
	
# 1. 判断此类是否被@Import引用
	# org.springframework.context.annotation.ConfigurationClass#isImported
	# 如果是，则将当前类注册
		# ConfigurationClassBeanDefinitionReader#registerBeanDefinitionForImportedConfigurationClass
		# org.springframework.beans.factory.support.BeanDefinitionRegistry#registerBeanDefinition
		# org.springframework.beans.factory.support.DefaultListableBeanFactory#registerBeanDefinition
		
# 2. 获取前面处理@Bean注解的	beanMethods进行处理
	# org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForBeanMethod
	
# 3. 获取前面处理@ImportResource添加的importedResources进行处理
	# org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsFromImportedResources
	
# 4. 获取前面处理@Import添加的importedResources进行处理importBeanDefinitionRegistrars进行处理
	# org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsFromRegistrars
```

##### postProcessBeanFactory处理

```bash
# ConfigurationClassPostProcessor的postProcessBeanFactory后置处理主要包括两个主要功能
	# 对加了@Configuration注解的类进行CGLIB代理
	# 向Spring中添加一个后置处理器ImportAwareBeanPostProcessor
```

###### CGLIB代理

```bash
# 入口
	# org.springframework.context.annotation.ConfigurationClassPostProcessor#postProcessBeanFactory
	# org.springframework.context.annotation.ConfigurationClassPostProcessor#enhanceConfigurationClasses
	
# 过程
	# 1. 获取当前对应@Configuration的BeanDefinition
		# 通过方法判断
			# org.springframework.context.annotation.ConfigurationClassUtils#isFullConfigurationClass
	# 2. 进行CGLIB代理
		# 实际就是将当前BeanDefinition的beanClass替换为代理的class
		# org.springframework.context.annotation.ConfigurationClassEnhancer#enhance
		# 1. 创建一个CGLIB代理实例
			# org.springframework.context.annotation.ConfigurationClassEnhancer#newEnhancer
		# 2. 创建代理类
			# org.springframework.context.annotation.ConfigurationClassEnhancer#createClass
			# org.springframework.cglib.proxy.Enhancer#createClass
			# 注册一个回调方法
				# org.springframework.cglib.proxy.Enhancer#registerStaticCallbacks
```

###### 添加后置处理器

```bash
# 入口
	# org.springframework.context.annotation.ConfigurationClassPostProcessor#postProcessBeanFactory
	
# 过程
	# 创建一个ImportAwareBeanPostProcessor
		# org.springframework.context.annotation.ConfigurationClassPostProcessor.ImportAwareBeanPostProcessor
	# 添加到Spring容器
		# org.springframework.beans.factory.config.ConfigurableBeanFactory#addBeanPostProcessor
		
# 注
	# ImportAwareBeanPostProcessor是ConfigurationClassPostProcessor内部类
	# ImportAwareBeanPostProcessor实现了BeanPostProcessor接口
```

#### AutowiredAnnotationBeanPostProcessor后置处理

借鉴链接：https://www.cnblogs.com/binarylei/p/12342100.html#4-postprocesspropertyvalues

```bash
# 主要处理@Autowired/@Value注解实现注入

# 构造器注入
	# determineCandidateConstructors
# 其他
	# postProcessMergedBeanDefinition
	# postProcessProperties
		# 属性注入
			# AutowiredFieldElement
		# AutowiredMethodElement
```

##### determineCandidateConstructors处理

```bash
# SmartInstantiationAwareBeanPostProcessor接口方法
	# org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor
	
# determineCandidateConstructors
	# 在spring容器创建Bean时，分析构造器上的@Autowired/@Value注解，确定候选构造函数
	# 入口
		# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
		# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBeanInstance
		# AbstractAutowireCapableBeanFactory#determineConstructorsFromBeanPostProcessors

# 过程
	# 获取SmartInstantiationAwareBeanPostProcessor类型的BeanPostProcessors
	# 执行determineCandidateConstructors方法

# determineCandidateConstructors
	# org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor#determineCandidateConstructors
	# 1. 获取当前类的构造方法
		# java.lang.Class#getDeclaredConstructors
	# 2. 获取其注释的@Autowired/@Value对应的属性
		# org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor#findAutowiredAnnotation
        # 如果获取到对应属性，则判断是否必须依赖
        	# org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor#determineRequiredStatus
        # 如果是必须依赖的，则使用此构造器
	# 3. 对象构建
		# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#autowireConstructor
```

##### postProcessMergedBeanDefinition处理

```bash
# 主要就是分析Bean实例对应@Autowired注解注册的字段和方法
# 入口
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#applyMergedBeanDefinitionPostProcessors
	
	
# 步骤
# 1. 查询Bean实例所有@Autowired注解注册的字段和方法
	# org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor#findAutowiringMetadata
	# org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor#buildAutowiringMetadata
	# 查看是否使用@Autowired注释的字段
		# org.springframework.util.ReflectionUtils#doWithLocalFields
		# org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor#findAutowiredAnnotation
	# 查看是否使用@Autowired注释的方法
		# org.springframework.util.ReflectionUtils#doWithLocalMethods
		# org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor#findAutowiredAnnotation
	# 都解析为对应InjectedElement保存起来
		# List<InjectionMetadata.InjectedElement> currElements
	# 通过获取父类的方式进行递归
		# java.lang.Class#getSuperclass
		# 结束条件
			# targetClass != null && targetClass != Object.class
	# 将最后结果封装为InjectionMetadata
		# new InjectionMetadata(clazz, elements);
	
# 2. 将获取的InjectionMetadata保存到injectionMetadataCache中
	# Map<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);
	# key为Bean实例的id
```

##### postProcessProperties处理

```bash
# 获取需要注入属性
# 入口
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#populateBean
		# 如果存在InstantiationAwareBeanPostProcessor类型的BeanPostProcessor
		# 调用postProcessProperties
		
# 过程
# 1. 从injectionMetadataCache中获取缓存的元数据InjectionMetadata
	# org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor#findAutowiringMetadata

# 2. 调用元数据inject方法
	# org.springframework.beans.factory.annotation.InjectionMetadata#inject
	# 1. 获取对应checkedElements
		# List<InjectionMetadata.InjectedElement>
	# 2.执行InjectedElement.inject方法
		# org.springframework.beans.factory.annotation.InjectionMetadata.InjectedElement#inject

# InjectedElement主要为两种，其inject方法处理也不同
	# AutowiredFieldElement
		# 1. 获取到当前AutowiredFieldElement对应Field
		# 2. 调用beanFactory的resolveDependency方法获取目标值，主要通过目标Field字段类型匹配，从beanFactory中获取对应实例
			# org.springframework.beans.factory.config.AutowireCapableBeanFactory#resolveDependenc
		# 3. 设置属性值
			# java.lang.reflect.Field#set
	# AutowiredMethodElement
		# 1. 获取到当前AutowiredFieldElement对应Field
		# 2. 调用beanFactory的resolveDependency方法获取目标值，主要通过目标方法参数类型匹配，从beanFactory中获取对应实例
			# 获取对应参数类型
				# java.lang.reflect.Method#getParameterTypes
			# 根据类型获取实例
				# org.springframework.beans.factory.config.AutowireCapableBeanFactory#resolveDependenc
		# 3. 调用方法设置属性值
			# java.lang.reflect.Method#invoke
```

#### CommonAnnotationBeanPostProcessor后置处理

```bash
# org.springframework.context.annotation.CommonAnnotationBeanPostProcessor
# 主要处理一下注解
# @Resource
	# @Resource的作用相当于@Autowired，
	# @Resource的作用相当于@Autowired，只不过@Autowired按byType自动注入，而@Resource默认按 byName自动注入
	# 相关处理
		# postProcessMergedBeanDefinition
		# postProcessPropertyValues
	
# @PostConstruct/@PreDestroy
	# 父类InitDestroyAnnotationBeanPostProcessor
		# postProcessMergedBeanDefinition
		# postProcessBeforeInitialization
	
```

##### postProcessMergedBeanDefinition处理

```bash
# 实现自接口MergedBeanDefinitionPostProcessor
	# org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor
	
# 触发入口
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#applyMergedBeanDefinitionPostProcessors
	# 获取MergedBeanDefinitionPostProcessor子类的BeanPostProcessors进行方法调用
		# org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor#postProcessMergedBeanDefinition
		
# 处理
# 1. 调用父类postProcessMergedBeanDefinition，处理@PostConstruct/@PreDestroy
	# org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor#postProcessMergedBeanDefinition
	# 处理
	# 1. 获取注释了@PostConstruct/@PreDestroy的方法对应元数据
		# org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor#findLifecycleMetadata
		# org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor#buildLifecycleMetadata
		# org.springframework.util.ReflectionUtils#doWithLocalMethods
		# java.lang.reflect.AccessibleObject#isAnnotationPresent
	# 2. 将对应的元数据放入对应缓存容器
		# Map<Class<?>, LifecycleMetadata> lifecycleMetadataCache = new ConcurrentHashMap<>(256);
		# LifecycleMetadata
			# initMethods
				# Collection<LifecycleElement>
			# destroyMethods
				# Collection<LifecycleElement>


# 2. 调用findResourceMetadata方法，处理@Resource注解
	# org.springframework.context.annotation.CommonAnnotationBeanPostProcessor#findResourceMetadata
	# 处理
    # 1. 获取注释了@Resource的方法和属性对应的InjectionMetadata元数据
        # org.springframework.context.annotation.CommonAnnotationBeanPostProcessor#findResourceMetadata
        # org.springframework.context.annotation.CommonAnnotationBeanPostProcessor#buildResourceMetadata
        # 方法
            # org.springframework.util.ReflectionUtils#doWithLocalFields
        # 属性
            # org.springframework.util.ReflectionUtils#doWithLocalMethods
    # 2. 将获取的InjectionMetadata保存到injectionMetadataCache中
        # Map<String, InjectionMetadata> injectionMetadataCache
```

##### postProcessBeforeInitialization处理

```bash
# 实现自BeanPostProcessor接口
# 触发入口
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#initializeBean
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#applyBeanPostProcessorsBeforeInitialization
	# org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization

# 处理
# 1. 获取前面处理放入lifecycleMetadataCache缓存的元数据
	# org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor#findLifecycleMetadata
# 2. 调用初始化方法，对应@PostConstruct
    # org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.LifecycleMetadata#invokeInitMethods
    # org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.LifecycleElement#invoke
    # java.lang.reflect.Method#invoke
# 由调用可知，@PostConstruct注释方法应为无参方法
```

##### postProcessPropertyValues处理

```bash
# 实现自InstantiationAwareBeanPostProcessor接口
	# org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor
# 触发入口
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#populateBean
	# 获取InstantiationAwareBeanPostProcessor子类的BeanPostProcessors
		# org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor#postProcessProperties

# 入口
	# org.springframework.context.annotation.CommonAnnotationBeanPostProcessor#postProcessPropertyValues
	# org.springframework.context.annotation.CommonAnnotationBeanPostProcessor#postProcessProperties
	
# 处理
# 1. 获取postProcessMergedBeanDefinition方法处理后放入injectionMetadataCache中的InjectionMetadata
	# org.springframework.context.annotation.CommonAnnotationBeanPostProcessor#findResourceMetadata
# 2. 调用inject方法进行资源注入
	# 不同@Autowired，使用name进行对象获取，而不是使用type
		# org.springframework.context.annotation.CommonAnnotationBeanPostProcessor.ResourceElement#getResourceToInject
		# org.springframework.context.annotation.CommonAnnotationBeanPostProcessor#autowireResource
		# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#resolveBeanByName
		# org.springframework.beans.factory.support.AbstractBeanFactory#getBean(java.lang.String, java.lang.Class<T>)
	# 属性设置
		# java.lang.reflect.Method#invoke
		# 
```

# Spring AOP

https://cloud.tencent.com/developer/article/1665081

https://blog.csdn.net/qq_38826019/article/details/117605566

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

## AOP实现过程

1. 解析配置，开启AOP功能，注册AnnotationAwareAspectJAutoProxyCreator类的BeanDefinition到BeanFactory中
2. AOP切面配置解析
   1. 解析配置，获取对应AbstractAspectJAdvice类型切面增强器
3. AnnotationAwareAspectJAutoProxyCreator处理

### 开启AOP功能

```bash
# 主要就是解析配置，注册AnnotationAwareAspectJAutoProxyCreator类的BeanDefinition到BeanFactory中
# 后续AnnotationAwareAspectJAutoProxyCreator通过实现BeanPostProcessor接口的方法，实现切面的代理对象创建
```

#### 注解配置

```bash
# 1. 配置类添加@EnableAspectJAutoProxy注解

# 2. @EnableAspectJAutoProxy注解使用@Import引入了AspectJAutoProxyRegistrar类

# 3. AspectJAutoProxyRegistrar类实现了ImportBeanDefinitionRegistrar接口
	# 调用其registerBeanDefinitions方法，注册AnnotationAwareAspectJAutoProxyCreator
	# 过程
		# 1. 调用registerBeanDefinitions方法
			# org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsFromRegistrars
			# org.springframework.context.annotation.ImportBeanDefinitionRegistrar#registerBeanDefinitions
		# 2. 调用工具类方法注册AnnotationAwareAspectJAutoProxyCreator
			# org.springframework.aop.config.AopConfigUtils#registerAspectJAnnotationAutoProxyCreatorIfNecessary
```

##### 涉及类库

###### EnableAspectJAutoProxy

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AspectJAutoProxyRegistrar.class)
public @interface EnableAspectJAutoProxy {
	boolean proxyTargetClass() default false;
	boolean exposeProxy() default false;
}
```

###### AspectJAutoProxyRegistrar

```java
//org.springframework.context.annotation.AspectJAutoProxyRegistrar
class AspectJAutoProxyRegistrar implements ImportBeanDefinitionRegistrar {
    /* 继承自ImportBeanDefinitionRegistrar接口 */
    public void registerBeanDefinitions(
			AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        //调用工具类方法注册AnnotationAwareAspectJAutoProxyCreator
        AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry);
    }
}
```

###### AopConfigUtils

```java
//org.springframework.aop.config.AopConfigUtils
public abstract class AopConfigUtils {
    
    /* 注册InfrastructureAdvisorAutoProxyCreator类的BeanDefinition*/
    public static BeanDefinition registerAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry, @Nullable Object source) {
        return registerOrEscalateApcAsRequired(InfrastructureAdvisorAutoProxyCreator.class, registry, source);
    }
}
```

#### XML配置

```bash
# 加载xml配置资源时，解析对应标签
# 入口
	# org.springframework.context.support.AbstractApplicationContext#obtainFreshBeanFactory
	# org.springframework.context.support.AbstractXmlApplicationContext#loadBeanDefinitions
	# org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#parseBeanDefinitions

# 解析对应aop配置标签
	# aop:aspectj-autoproxy
	# aop:config
	# advice增强功能的类
		# 普通bean标签
			
# aop标签解析入口
	# xml标签解析入口
		# org.springframework.context.support.AbstractApplicationContext#obtainFreshBeanFactory
        # org.springframework.context.support.AbstractXmlApplicationContext#loadBeanDefinitions
        # org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#parseBeanDefinitions 
	# 通过isDefaultNamespace方法判断，区分namespace：beans和其他标签处理
		# String BEANS_NAMESPACE_URI = "http://www.springframework.org/schema/beans";
		# org.springframework.beans.factory.xml.BeanDefinitionParserDelegate#isDefaultNamespace
	# namespace处理方法
		# beans
			# org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader#parseDefaultElement
		# 其他
			# org.springframework.beans.factory.xml.BeanDefinitionParserDelegate#parseCustomElement
			
# 其他namespace标签处理过程
# 1. 获取element对应namespaceURI
	# org.springframework.beans.factory.xml.BeanDefinitionParserDelegate#getNamespaceURI
	
# 2. 根据namespaceURI获取对应NamespaceHandler
	# org.springframework.beans.factory.xml.NamespaceHandlerResolver#resolve
	# 对应aop
		# org.springframework.aop.config.AopNamespaceHandler
		
# 3. AopNamespaceHandler进行处理
	# org.springframework.beans.factory.xml.NamespaceHandlerSupport#parse	
	
# 4. 根据要处理的element，获取其对应BeanDefinitionParser
	# org.springframework.beans.factory.xml.NamespaceHandlerSupport#findParserForElement
	# 不同标签对应parser
        # config
            # org.springframework.aop.config.ConfigBeanDefinitionParser
        # aspectj-autoproxy
            # org.springframework.aop.config.AspectJAutoProxyBeanDefinitionParser


# AspectJAutoProxyBeanDefinitionParser处理
	# 调用工具类方法注册AnnotationAwareAspectJAutoProxyCreator
		# org.springframework.aop.config.AopNamespaceUtils#registerAspectJAnnotationAutoProxyCreatorIfNecessary
		# org.springframework.aop.config.AopConfigUtils#registerAspectJAnnotationAutoProxyCreatorIfNecessary
		# org.springframework.aop.config.AopConfigUtils#registerOrEscalateApcAsRequired
		# org.springframework.beans.factory.support.BeanDefinitionRegistry#registerBeanDefinition
	
# ConfigBeanDefinitionParser处理
	# 解析不同子标签
		# pointcut
			# org.springframework.aop.config.ConfigBeanDefinitionParser#parsePointcut
		# advisor
			# org.springframework.aop.config.ConfigBeanDefinitionParser#parseAdvisor
		# aspect
			# org.springframework.aop.config.ConfigBeanDefinitionParser#parseAspect
			# 解析为对应AbstractAspectJAdvice切面增强器
				# org.springframework.aop.aspectj.AbstractAspectJAdvice
            	# aop:before
            		# org.springframework.aop.aspectj.AspectJMethodBeforeAdvice
            	# aop:after
            		# org.springframework.aop.aspectj.AspectJAfterAdvice
            	# aop:after-returning
            		# org.springframework.aop.aspectj.AspectJAfterReturningAdvice
            	# aop:after-throwing
               		# org.springframework.aop.aspectj.AspectJAfterThrowingAdvice
               	# aop:around
               		# org.springframework.aop.aspectj.AspectJAroundAdvice
               		
	# 方法中注册对应BeanDefinition到BeanFactory
		# org.springframework.beans.factory.xml.XmlReaderContext#registerWithGeneratedName
```

### AnnotationAwareAspectJAutoProxyCreator处理

- 抽象父类AbstractAutoProxyCreator，实现了
  - InstantiationAwareBeanPostProcessor接口的postProcessBeforeInstantiation
  - BeanPostProcessor的postProcessAfterInitialization

#### postProcessBeforeInstantiation前置处理

```bash
# 入口
	# org.springframework.beans.factory.support.AbstractBeanFactory#createBean
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#resolveBeforeInstantiation
	# org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#applyBeanPostProcessorsBeforeInstantiation
	# org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation


# 由上述分析可知，对于注解配置和xml配置是不同的
	# 注解配置
		# 前面分析过程仅仅是通过@EnableAspectJAutoProxy注册了AnnotationAwareAspectJAutoProxyCreator
	# xml配置
		# 通过解析aop:aspectj-autoproxy标签，注册了AnnotationAwareAspectJAutoProxyCreator
		# 通过解析aop:config，注册了对应AbstractAspectJAdvice的切面增强器
		
# postProcessBeforeInitialization处理		
	# 入口
        # org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#postProcessBeforeInstantiation
    # 过程
    	# 1.判断是否基础设施类
    		# org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#isInfrastructureClass
    		# 1. 主要判断是否以下类型
    			# org.aopalliance.aop.Advice
    			# org.springframework.aop.Advisor
				# org.springframework.aop.framework.AopInfrastructureBean
			# 2. 判断是否有@Aspect注解
				# org.springframework.aop.aspectj.annotation.AbstractAspectJAdvisorFactory#hasAspectAnnotation
			# 结果判断
				# 是，保存false
					# this.advisedBeans.put(cacheKey, Boolean.FALSE);
				# 不是，继续shouldSkip判断
		# 2. 是否应该跳过判断
			# org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#shouldSkip
			# 1. 先获取所有Advisor
				# org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator#findCandidateAdvisors
				# 1. 如果是xml配置，则super.findCandidateAdvisors()会获取前面解析的Advisor
					# org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator#findCandidateAdvisors
	        	# 2. 如果是注解配置，则super.findCandidateAdvisors()获取不到，调用buildAspectJAdvisors进行解析获取
	        		# org.springframework.aop.aspectj.annotation.BeanFactoryAspectJAdvisorsBuilder#buildAspectJAdvisors
		
# 所以对于注解配置，postProcessBeforeInitialization需要注册对应AbstractAspectJAdvice的切面增强器
    # 入口
        # org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#postProcessBeforeInstantiation
        # org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#shouldSkip
        # org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator#findCandidateAdvisors
        # org.springframework.aop.aspectj.annotation.BeanFactoryAspectJAdvisorsBuilder#buildAspectJAdvisors
    # 过程
    	# 1. 获取当前所有注册的Object类型的BeanNames
    		# org.springframework.beans.factory.BeanFactoryUtils#beanNamesForTypeIncludingAncestors
    		# org.springframework.beans.factory.support.DefaultListableBeanFactory#doGetBeanNamesForType
    	# 2. 获取其中是Advice的Bean
    		# 判断方法
    			# org.springframework.aop.aspectj.annotation.AspectJAdvisorFactory#isAspect
    			# org.springframework.aop.aspectj.annotation.AbstractAspectJAdvisorFactory#hasAspectAnnotation
    			# org.springframework.core.annotation.AnnotationUtils#findAnnotation
    	# 3. 解析对应Bean获取Advisor
    		# org.springframework.aop.aspectj.annotation.AspectJAdvisorFactory#getAdvisors
    		
# @Advice注解类解析过程
# 1. 获取切面方法，不包括@Pointcut注释方法
	# org.springframework.aop.aspectj.annotation.ReflectiveAspectJAdvisorFactory#getAdvisorMethods
	
# 2. 解析方法，获取对应Advisor
	# org.springframework.aop.aspectj.annotation.ReflectiveAspectJAdvisorFactory#getAdvisor
	
# 3. 将创建的Advisor缓存
	# this.advisorsCache.put(beanName, classAdvisors);
```

### postProcessAfterInitialization后置处理

```bash
# 此处就是进行代理的入口
	# org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#postProcessAfterInitialization
	# org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#wrapIfNecessary
	
# 先是一系列判断，针对无需进行aop代理的对象跳过
# 对于需要进行代理的对象，进行代理
# 过程
	# 1. 获取获取增强器
		# org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#getAdvicesAndAdvisorsForBean
	# 2. 创建代理对象返回
		# org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#createProxy
```





