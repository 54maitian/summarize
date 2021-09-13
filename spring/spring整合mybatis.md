# Spring整合Mybatis

我们通常不是单独使用`Mybatis`框架，而是通过`Spring`整合`Mybatis`进行使用



在单独使用`Mybatis`框架时，我们需要自行创建`SqlSessionFactoryBuilder`对象，并调用其`build`方法解析对应`mybatis.xml`配置文件获取对应`sqlSessionFactory`为我们提供工作

而在`Spring`整合`Mybatis`时，则无需我们手动进行`sqlSessionFactory`的创建，我们通过配置来实现这一目的



## Spring整合Mybatis主配置

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



由上述可知，通过整合配置文件，我们主要向IOC容器注入了三个对象

- `DruidDataSource`
  - 数据源
- `SqlSessionFactoryBean`
  - `session`工厂
- `MapperScannerConfigurer`



数据源我们无需再进行分析，那么我们就分析一下其他两个对象



## SqlSessionFactoryBean

```java
// org.mybatis.spring.SqlSessionFactoryBean
public class SqlSessionFactoryBean implements FactoryBean<SqlSessionFactory>, InitializingBean {
    // mapper.xml文件资源
    private Resource[] mapperLocations;
    // 数据源
    private DataSource dataSource;
    
    private SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();
    private SqlSessionFactory sqlSessionFactory;
}
```

由上述代码可知，`SqlSessionFactoryBean`主要实现了`FactoryBean、InitializingBean`接口，那么根据`Spring`分析流程，我们先分析

实现的`InitializingBean#afterPropertiesSet`方法



### afterPropertiesSet

```java
// org.mybatis.spring.SqlSessionFactoryBean#afterPropertiesSet
public void afterPropertiesSet() throws Exception {
	this.sqlSessionFactory = buildSqlSessionFactory();
}
```

我们发现`afterPropertiesSet`方法主要通过调用`buildSqlSessionFactory`来创建一个`sqlSessionFactory`对象，保存到当期实例中



#### buildSqlSessionFactory

```java
// org.mybatis.spring.SqlSessionFactoryBean#buildSqlSessionFactory
protected SqlSessionFactory buildSqlSessionFactory() throws Exception {
    // 创建一个Configuration对象
    final Configuration targetConfiguration = new Configuration();
    
    // 省略中间属性设置部分...
    
    if (this.mapperLocations != null) {
        // 遍历mapper.xml资源文件
        for (Resource mapperLocation : this.mapperLocations) {
          if (mapperLocation == null) {
            continue;
          }
          // 创建XMLMapperBuilder对象，并调用parse方法解析对应xml资源，将数据保存到Configuration中
          XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(mapperLocation.getInputStream(),
                targetConfiguration, mapperLocation.toString(), targetConfiguration.getSqlFragments());
          xmlMapperBuilder.parse();
        }
    }
    // 通过SqlSessionFactoryBuilder#build构建DefaultSqlSessionFactory对象并返回
    return this.sqlSessionFactoryBuilder.build(targetConfiguration);
}
```



继续分析实现自`FactoryBean`接口的`getObject`方法

### getObject

```java
// org.mybatis.spring.SqlSessionFactoryBean#getObject
public SqlSessionFactory getObject() throws Exception {
    if (this.sqlSessionFactory == null) {
        afterPropertiesSet();
    }
	// 返回当前实例保存的DefaultSqlSessionFactory对象
    return this.sqlSessionFactory;
}
```



### 小结

`SqlSessionFactoryBean`先通过`InitializingBean#afterPropertiesSet`方法，解析`mapper.xml`配置资源，构建`DefaultSqlSessionFactory`对象，再通过`FactoryBean#getObject`方法



我们继续分析`MapperScannerConfigurer`对象

## MapperScannerConfigurer

```java
// org.mybatis.spring.mapper.MapperScannerConfigurer
public class MapperScannerConfigurer implements BeanDefinitionRegistryPostProcessor, InitializingBean {
    // mapper接口扫描包
    private String basePackage;
    // 引入SqlSessionFactory对象
    private SqlSessionFactory sqlSessionFactory;
    // 持有SqlSessionTemplate
    private SqlSessionTemplate sqlSessionTemplate;
}
```



对于`MapperScannerConfigurer`来说，其实现的`InitializingBean#afterPropertiesSet`并没有进行太多的处理，所以我们主要分析其实现的`BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry`方法



### postProcessBeanDefinitionRegistry

```java
public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
	// 创建ClassPathMapperScanner对象
    ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry);
    
    // 省略中间的属性设置部分...
    scanner.setSqlSessionFactory(this.sqlSessionFactory);
    scanner.setSqlSessionTemplate(this.sqlSessionTemplate);
    
    // 通过ClassPathMapperScanner#scan进行mapper扫描
    scanner.scan(
        StringUtils.tokenizeToStringArray(this.basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
  }
```

我们发现`postProcessBeanDefinitionRegistry`将主要工作委托给`ClassPathMapperScanner`，通过其`scan`方法扫描我们对应`mapper`接口包，我们分析一下



### ClassPathMapperScanner

```java
// org.mybatis.spring.mapper.ClassPathMapperScanner
public class ClassPathMapperScanner extends ClassPathBeanDefinitionScanner {
    // 用于存储SqlSessionFactory实例/其对应引用beanName
    private SqlSessionFactory sqlSessionFactory;
    private String sqlSessionFactoryBeanName;
	// 用于存储SqlSessionTemplate实例/其对应引用beanName
    private SqlSessionTemplate sqlSessionTemplate;
    private String sqlSessionTemplateBeanName;
}
```



我们查看一下`ClassPathMapperScanner`的类图

![image-20210825083151833](D:\学习整理\summarize\spring\图片\ClassPathMapperScanner类图)

其继承了`ClassPathBeanDefinitionScanner、ClassPathScanningCandidateComponentProvider`



下面我们来分析一下其`scan`方法的具体调用历程

#### 1. scan调用跟踪

```java
// 1. org.springframework.context.annotation.ClassPathBeanDefinitionScanner#scan
public int scan(String... basePackages) {
	// doScan方法
    doScan(basePackages);

    if (this.includeAnnotationConfig) {
        AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
    }

    return (this.registry.getBeanDefinitionCount() - beanCountAtScanStart);
}

// 2. org.mybatis.spring.mapper.ClassPathMapperScanner#doScan
public Set<BeanDefinitionHolder> doScan(String... basePackages) {
    // 2.1 调用父类doScan方法扫描包获取beanDefinitions
    Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);

    if (beanDefinitions.isEmpty()) {
    } else {
        // 2.2 处理获取的beanDefinitions
        processBeanDefinitions(beanDefinitions);
    }
    return beanDefinitions;
}
```



#### 2. doScan调用

```java
// org.springframework.context.annotation.ClassPathBeanDefinitionScanner#doScan
protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
    Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<>();
    // 遍历扫描包
    for (String basePackage : basePackages) {
        // 1. 调用findCandidateComponents方法获取包下目标BeanDefinition
        Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
        for (BeanDefinition candidate : candidates) {
            ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(candidate);
            candidate.setScope(scopeMetadata.getScopeName());
            String beanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);
            if (candidate instanceof AbstractBeanDefinition) {
                postProcessBeanDefinition((AbstractBeanDefinition) candidate, beanName);
            }
            if (candidate instanceof AnnotatedBeanDefinition) {
                AnnotationConfigUtils.processCommonDefinitionAnnotations((AnnotatedBeanDefinition) candidate);
            }
            if (checkCandidate(beanName, candidate)) {
                BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
                definitionHolder =
                    AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
                beanDefinitions.add(definitionHolder);
                // 2. 将目标BeanDefinition注册到IOC容器中
                registerBeanDefinition(definitionHolder, this.registry);
            }
        }
    }
    return beanDefinitions;
}
```

由上述代码分析，主要关注点就在于`findCandidateComponents`方法如何从包下获取目标`BeanDefinition`



##### findCandidateComponents

```java
// org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider#findCandidateComponents
public Set<BeanDefinition> findCandidateComponents(String basePackage) {
    return scanCandidateComponents(basePackage);
}

// org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider#scanCandidateComponents
private Set<BeanDefinition> scanCandidateComponents(String basePackage) {
    Set<BeanDefinition> candidates = new LinkedHashSet<>();
    // 1. 获取目标包下所有.class资源
    String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
        resolveBasePackage(basePackage) + '/' + this.resourcePattern;
    Resource[] resources = getResourcePatternResolver().getResources(packageSearchPath);
    // 2. 遍历扫描的.class资源
    for (Resource resource : resources) {
        MetadataReader metadataReader = getMetadataReaderFactory().getMetadataReader(resource);
        // 3. 通过过滤器匹配.class资源
        if (isCandidateComponent(metadataReader)) {
            // 4. 将目标.class资源封装为BeanDefinition
            ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);
            sbd.setSource(resource);
            // 5. 通过isCandidateComponent方法判断是否所需关注的mapper接口
            if (isCandidateComponent(sbd)) {
                candidates.add(sbd);
            }
        }
        return candidates;
    }
}
```

我们发现其通过扫描`basePackage`包下所有`.class`资源，并通过两个重载的`isCandidateComponent`方法对资源进行过滤判断，获取符合的`BeanDefinition`

我们查看一下这两个重载方法



##### isCandidateComponent

```java
// org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider#isCandidateComponent
protected boolean isCandidateComponent(MetadataReader metadataReader) throws IOException {
    // 排除过滤器匹配
    for (TypeFilter tf : this.excludeFilters) {
        if (tf.match(metadataReader, getMetadataReaderFactory())) {
            return false;
        }
    }
    // 包含过滤器匹配，至少要符合一个包含过滤器的匹配
    for (TypeFilter tf : this.includeFilters) {
        if (tf.match(metadataReader, getMetadataReaderFactory())) {
            return isConditionMatch(metadataReader);
        }
    }
    return false;
}

// org.mybatis.spring.mapper.ClassPathMapperScanner#isCandidateComponent
protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
    // 目标类需要：1. 是一个接口，2. 是一个非内部类
    return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
}
```



##### 小结

综上代码分析，`doScan`方法主要作用是扫描`basePackages`包，将包下独立接口封装为`BeanDefinition`返回



#### 3. processBeanDefinitions处理

前面我们通过`doScan`扫描获取了目标`BeanDefinitions`，那么此时将通过`processBeanDefinitions`方法对其进行处理，那么它具体做了什么处理？

```java
// org.mybatis.spring.mapper.ClassPathMapperScanner#processBeanDefinitions
private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
    AbstractBeanDefinition definition;
    // 获取注册器，即DefaultListableBeanFactory实例
    BeanDefinitionRegistry registry = getRegistry();
    // 遍历获取的beanDefinitions
    for (BeanDefinitionHolder holder : beanDefinitions) {
        definition = (AbstractBeanDefinition) holder.getBeanDefinition();
		// 获取其对应类
        String beanClassName = definition.getBeanClassName();
		// * 将原类添加到definition的构造参数中
        definition.getConstructorArgumentValues().addGenericArgumentValue(beanClassName); 
        // * 将definition对应类替换为mapperFactoryBeanClass，即：MapperFactoryBean.class
        definition.setBeanClass(this.mapperFactoryBeanClass);
		
        // 属性补充
        definition.getPropertyValues().add("addToConfig", this.addToConfig);
        definition.setAttribute(FACTORY_BEAN_OBJECT_TYPE, beanClassName);
		
        // 向BeanDefinition中设置对应sqlSessionFactory信息
        boolean explicitFactoryUsed = false;
        if (StringUtils.hasText(this.sqlSessionFactoryBeanName)) {
            definition.getPropertyValues().add("sqlSessionFactory",
                                               new RuntimeBeanReference(this.sqlSessionFactoryBeanName));
            explicitFactoryUsed = true;
        } else if (this.sqlSessionFactory != null) {
            definition.getPropertyValues().add("sqlSessionFactory", this.sqlSessionFactory);
            explicitFactoryUsed = true;
        }
        
		// 向BeanDefinition中设置对应sqlSessionTemplate信息
        if (StringUtils.hasText(this.sqlSessionTemplateBeanName)) {
            if (explicitFactoryUsed) {
                LOGGER.warn(
                    () -> "Cannot use both: sqlSessionTemplate and sqlSessionFactory together. sqlSessionFactory is ignored.");
            }
            definition.getPropertyValues().add("sqlSessionTemplate",
                                               new RuntimeBeanReference(this.sqlSessionTemplateBeanName));
            explicitFactoryUsed = true;
        } else if (this.sqlSessionTemplate != null) {
            if (explicitFactoryUsed) {
                LOGGER.warn(
                    () -> "Cannot use both: sqlSessionTemplate and sqlSessionFactory together. sqlSessionFactory is ignored.");
            }
            definition.getPropertyValues().add("sqlSessionTemplate", this.sqlSessionTemplate);
            explicitFactoryUsed = true;
        }

		// 省略...
        
        if (!explicitFactoryUsed) {
            definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        }
    }
}
```

由上述代码可知`processBeanDefinitions`方法主要作用是将获取的`BeanDefinition`替换为`MapperFactoryBean`类型，并封装对应`sqlSessionFactory`信息



##### **注意**

对于判断标志`explicitFactoryUsed`，如果上述都符不符合，即没有配置对应`sqlSessionFactory、sqlSessionTemplate`信息时，有一个特殊处理：

将`BeanDefinition`的`autowiredMode`设置为`AUTOWIRE_BY_TYPE`，由`Spring`分析可知，此时将进行根据类型的自动注入



### 小结

`ClassPathMapperScanner`是`Mybatis`与`Spring`整合的关键扫描类

综合分析`MapperScannerConfigurer#postProcessBeanDefinitionRegistry`具体调用，其主要实现两件事

- 扫描`basePackages`包，获取其中的`mapper`接口，封装为`BeanDefinition`，并注册到IOC容器中
- 将获取的`BeanDefinition`处理为`MapperFactoryBean`类型





前面分析了我们配置在`applicationContex.xml`中的`SqlSessionFactoryBean、MapperScannerConfigurer`对象，此时我们

- 在IOC容器中存在了`SqlSessionFactory`对象
- 并且将目标`basePackages`包下`mapper`接口封装为`MapperFactoryBean`类型的`BeanDefinition`，并且保存到IOC容器中

那么它们将如何为我们提供`Mybatis`的数据库操作功能呢？那么我们需要分析一下`MapperFactoryBean`的作用



## MapperFactoryBean

我们查看`MapperFactoryBean`类图

![image-20210825212137879](D:\学习整理\summarize\spring\图片\MapperFactoryBean类图)

### 1. 实例化过程分析

我们发现其继承了`SqlSessionDaoSupport`抽象父类，我们结合两者分析一下其初构造过程

```java
// org.mybatis.spring.mapper.MapperFactoryBean
public class MapperFactoryBean<T> extends SqlSessionDaoSupport implements FactoryBean<T> {
    // 当前包装的mapper接口的Class
    private Class<T> mapperInterface;
    
    // 构造设置mapperInterface
    public MapperFactoryBean(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }
}

// org.mybatis.spring.support.SqlSessionDaoSupport
public abstract class SqlSessionDaoSupport extends DaoSupport {
    // 持有SqlSessionTemplate引用
    private SqlSessionTemplate sqlSessionTemplate;

    /* 定义setSqlSessionFactory方法 */ 
    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        if (this.sqlSessionTemplate == null || sqlSessionFactory != this.sqlSessionTemplate.getSqlSessionFactory()) {
            this.sqlSessionTemplate = createSqlSessionTemplate(sqlSessionFactory);
        }
    }

    /* 构建SqlSessionTemplate实例 */ 
    protected SqlSessionTemplate createSqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
```

我们分析一下，在前面`MapperScannerConfigurer`的处理中，我们将对应`mapperInterface`的`BeanDefinition`设置为`MapperFactoryBean`类型，并且做了两件事：

- 通过构造参数，保存了`mapperInterface`
- 通过属性，保存了构建的`SqlSessionFactory`实例

由此，在IOC容器创建`MapperFactoryBean`实例时：

- 调用有参构建创建实例，则将`mapperInterface`设置到`MapperFactoryBean`实例中
- 调用`setXXX`设置属性，即调用`setSqlSessionFactory`
  - 而在`setSqlSessionFactory`方法中，通过调用`createSqlSessionTemplate`方法，创建了`SqlSessionTemplate`实例，并封装了`sqlSessionFactory`

所以，在`MapperFactoryBean`实例中，`mapperInterface、sqlSessionTemplate`都将被赋值



### 2. 初始化过程分析

我们发现`MapperFactoryBean`一样实现了`InitializingBean`接口

我们先分析其实现的`InitializingBean#afterPropertiesSet`方法



#### afterPropertiesSet

`MapperFactoryBean`的`afterPropertiesSet`是继承自其抽象父类`DaoSupport`



##### DaoSupport

```java
public abstract class DaoSupport implements InitializingBean {
	public final void afterPropertiesSet() throws IllegalArgumentException, BeanInitializationException {
		checkDaoConfig();
	}
}
```

在`DaoSupport#afterPropertiesSet`，我们主要关注其调用的`checkDaoConfig`方法



##### checkDaoConfig

`checkDaoConfig`方法的具体实现在`MapperFactoryBean`类中

```java
protected void checkDaoConfig() {
	// 1. 从SqlSessionTemplate中获取Configuration
    Configuration configuration = getSqlSession().getConfiguration();
    // 2. Configuration实例调用addMapper注册当前mapper接口
    configuration.addMapper(this.mapperInterface);
}
```



#### 小结

初始化调用`afterPropertiesSet`主要作用就是通过`addMapper`注册当前`mapper`接口

**注**：结合我们对于`Mybatis`源码的分析，通过这一步骤，我们可以使`SqlSessionFactoryBean`无需配置对应`mapper.xml`资源，转而在此步骤借用addMapper进行解析



### 3. IOC容器实例获取

`MapperFactoryBean`还实现了`FactoryBean`接口，所以我们分析一下其`getObject`接口



#### getObject

```java
// org.mybatis.spring.mapper.MapperFactoryBean#getObject
public T getObject() throws Exception {
    return getSqlSession().getMapper(this.mapperInterface);
}

// org.mybatis.spring.support.SqlSessionDaoSupport#getSqlSession
public SqlSession getSqlSession() {
    return this.sqlSessionTemplate;
}
```



再查看一下其由IOC容器返回的实例类型，所以分析一下`getObjectType`方法

#### getObjectType

```java
// org.mybatis.spring.mapper.MapperFactoryBean#getObjectType
public Class<T> getObjectType() {
    return this.mapperInterface;
}
```



#### 小结

我们通过实现自`FactoryBean`接口的方法，可以知道两点：

- 通过`getObjectType`方法实现可知，我们由IOC容器获取实例时，返回的实例类型是包装的`mapperInterface`类型
  - 所以我们在`Spring`整合`Mybatis`使用时，可以直接通过`@Autowired`注解进行`mapper`实例导入
- 通过`getObject`方法实现可知，我们从IOC容器中获取的`mapperInterface`实例，实际是通过`sqlSessionTemplate#getMapper`来实现的





## SqlSessionTemplate

由前面分析可知，`sqlSessionTemplate`可以通过`getMapper`方法，获取对应`mapperInterface`实例，那么它是如何实现这一功能？

所以我们将分析`SqlSessionTemplate`

```java
// org.mybatis.spring.SqlSessionTemplate
public class SqlSessionTemplate implements SqlSession, DisposableBean {
    private final SqlSessionFactory sqlSessionFactory;
    private final SqlSession sqlSessionProxy;
}
```

从上可知，`SqlSessionTemplate`实现了`SqlSession`接口，所以其可以作为一个`SqlSession`进行工作



由前面分析可知，在IOC容器实例化`MapperFactoryBean`，在进行属性设置时，在`setSqlSessionFactory`方法中，创建了`SqlSessionTemplate`实例，现在我们分析一下`SqlSessionTemplate`构造过程

```java
// org.mybatis.spring.SqlSessionTemplate#SqlSessionTemplate
public SqlSessionTemplate(SqlSessionFactory sqlSessionFactory, ExecutorType executorType,
                          PersistenceExceptionTranslator exceptionTranslator) {
	// 保存sqlSessionFactory
    this.sqlSessionFactory = sqlSessionFactory;
    this.executorType = executorType;
    this.exceptionTranslator = exceptionTranslator;
    // 通过JDK动态代理创建一个SqlSession的代理对象
    this.sqlSessionProxy = (SqlSession) newProxyInstance(SqlSessionFactory.class.getClassLoader(),
                                                         new Class[] { SqlSession.class }, new SqlSessionInterceptor());
}
```



而在IOC容器进行`mapper`注入时，调用了`MapperFactoryBean#getObject`获取`mapper`实例，现在看来，是通过`SqlSessionTemplate#getMapper`方法实现

```java
// 	org.mybatis.spring.SqlSessionTemplate#getMapper
public <T> T getMapper(Class<T> type) {
    return getConfiguration().getMapper(type, this);
}

public Configuration getConfiguration() {
    return this.sqlSessionFactory.getConfiguration();
}
```

由上述代码可知，实质是通过`sqlSessionFactory`获取`Configuration`对象，再调用其`getMapper`方法获取对应`mapper`的代理实例



在分析`Mybatis`的`Mapper`代理时，了解到实际进行数据库操作的作为`getMapper`参数的`SqlSession`对象

这里我们需要**注意**，在`SqlSessionTemplate#getMapper`方法中，传入的`SqlSession`对象是`SqlSessionTemplate`实例本身

所以我们通过`mapper`代理实例进行方法调用时，实质的处理对象为`SqlSessionTemplate`实例本身



那么`SqlSessionTemplate`实例是如何进行工作的？我们以查询为例，分析`SqlSessionTemplate#select`

```java
// org.mybatis.spring.SqlSessionTemplate#select
public void select(String statement, ResultHandler handler) {
    this.sqlSessionProxy.select(statement, handler);
}
```



我们发现其实际是将请求委托给`sqlSessionProxy`对象，前面在构造中，`sqlSessionProxy`是一个JDK动态代理实例，其对应拦截类为`SqlSessionInterceptor`

我们分析一下此类



### SqlSessionInterceptor

```java
// org.mybatis.spring.SqlSessionTemplate.SqlSessionInterceptor
private class SqlSessionInterceptor implements InvocationHandler {
    //代理方法
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //获取sqlSession
        SqlSession sqlSession = getSqlSession(SqlSessionTemplate.this.sqlSessionFactory,
                                              SqlSessionTemplate.this.executorType, 				SqlSessionTemplate.this.exceptionTranslator);
        //执行方法获取对象
        Object result = method.invoke(sqlSession, args);
        //如果没有开启事务，则立即提交事务
        if (!isSqlSessionTransactional(sqlSession, SqlSessionTemplate.this.sqlSessionFactory)) {
            sqlSession.commit(true);
        }
        //返回结果
        return result;
    }
}

// org.mybatis.spring.SqlSessionUtils#getSqlSession
public static SqlSession getSqlSession(SqlSessionFactory sessionFactory, ExecutorType executorType,
                                       PersistenceExceptionTranslator exceptionTranslator) {

    SqlSessionHolder holder = (SqlSessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);

    SqlSession session = sessionHolder(executorType, holder);
    if (session != null) {
        return session;
    }

    // 通过sessionFactory#openSession创建SqlSession实例
    session = sessionFactory.openSession(executorType);

    registerSessionHolder(sessionFactory, executorType, exceptionTranslator, session);

    return session;
}
```



其`SqlSessionInterceptor`实现了`InvocationHandler`接口，其`invoke`方法大致逻辑：

- 通过`getSqlSession`方法获取`SqlSession`实例
- 通过获取的`SqlSession`实例，真正执行数据库操作



## 总结

综上所述，`Spring`整合`Mybatis`大致描述

- 向IOC容器中注册两个`BeanDefinition`
  - `SqlSessionFactoryBean`
    - 通过`afterPropertiesSet`初始化操作构建`SqlSessionFactory`实例
    - 通过`getObject`从IOC容器中获取`SqlSessionFactory`实例
  - `MapperScannerConfigurer`
    - 通过`postProcessBeanDefinitionRegistry`扫描给定的`mapper`接口的`basePackage`，获取`mapper`接口对应`BeanDefinition`
    - 将获取的`BeanDefinition`封装为`MapperFactoryBean`类型

- `MapperFactoryBean`
  - 通过属性设置时调用`setSqlSessionFactory`创建`SqlSessionTemplate`实例
  - 通过`afterPropertiesSet`初始化处理，将`mapper`接口注册到`Configuration`中
  - 提供`getMapper`方法从`Configuration`获取`mapper`代理对象，使IOC容器可以通过`@Autowired`注入`mapper`代理实例
- `SqlSessionTemplate`
  - `SqlSession`的子类
  - 通过创建的`sqlSessionProxy`代理对象，实现`mapper`代理实例方法的拦截增强