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
```

### SqlSessionFactoryBean

```java
//org.mybatis.spring.SqlSessionFactoryBean
public class SqlSessionFactoryBean
    implements FactoryBean<SqlSessionFactory>, InitializingBean, ApplicationListener<ApplicationEvent> {
    //配置类资源
    private Resource configLocation;
    //mapper.xml配置资源
    private Resource[] mapperLocations;
    
    //Configuration配置对象
    private Configuration configuration;
    //数据源
    private DataSource dataSource;
    //默认创建一个SqlSessionFactoryBuilder对象
    private SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();
    //持有SqlSessionFactory对象
    private SqlSessionFactory sqlSessionFactory;
    
    /* 继承自InitializingBean接口， */
    public void afterPropertiesSet() throws Exception {
        //调用方法创建sqlSessionFactory实例
        this.sqlSessionFactory = buildSqlSessionFactory();
    }
    
    /* 创建SqlSessionFactory */
    protected SqlSessionFactory buildSqlSessionFactory() throws Exception {
        //获取当前configuration
        targetConfiguration = this.configuration;
        //配置解析，补充Configuration
        ...
        //通过sqlSessionFactoryBuilder创建SqlSessionFactory
        return this.sqlSessionFactoryBuilder.build(targetConfiguration);
    }
    
    /* 继承自FactoryBean接口，返回创建的sqlSessionFactory，注册到BeanFactory */
    public SqlSessionFactory getObject() throws Exception {
        return this.sqlSessionFactory;
    }
}
```

#### 分析

```bash
# 1. 我们配置SqlSessionFactoryBean到Spring配置文件
# 2. Spring容器初始化加载创建SqlSessionFactoryBean对象
	# 1. 初始化时，由于SqlSessionFactoryBean实现InitializingBean接口，则调用afterPropertiesSet方法
		# 根据配置，创建SqlSessionFactory对象
	# 2. 在实例化之后，由于SqlSessionFactoryBean实现FactoryBean接口
		# 在getObjectForBeanInstance方法中，调用getObject方法，获取创建的sqlSessionFactory保存到BeanFactory中
```

### MapperScannerConfigurer

```java
//org.mybatis.spring.mapper.MapperScannerConfigurer
public class MapperScannerConfigurer
    implements BeanDefinitionRegistryPostProcessor, InitializingBean, ApplicationContextAware, BeanNameAware {
    //mapper对应包
    private String basePackage;
    //SqlSessionFactory的id
    private String sqlSessionFactoryBeanName;

    /*实现自BeanDefinitionRegistryPostProcessor接口*/
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        //在BeanFactory创建后，通过扫描，将mapper接口对应BeanDefinition添加到BeanFactory中
        //创建扫描器
        ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry);
        //扫描包
        scanner.scan(this.basePackage);
    }

    /* 执行扫描，org.mybatis.spring.mapper.ClassPathMapperScanner */
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        //扫描包下mapper接口对应定义，并注册到BeanFactory中
        Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);
        //处理扫描到的BeanDefinition
        processBeanDefinitions(beanDefinitions);
    }

    /* 处理BeanDefinition */
    private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
        //1.获取BeanDefinition对应BeanClassName
        String beanClassName = definition.getBeanClassName();
        //2. 将beanClassName设置为构造函数参数
        definition.getConstructorArgumentValues().addGenericArgumentValue(beanClassName); // issue #59
        //3. 将BeanDefinition的BeanClass替换为MapperFactoryBean
        definition.setBeanClass(this.mapperFactoryBeanClass);
        //4. 添加sqlSessionFactory属性
        definition.getPropertyValues().add("sqlSessionFactory", new RuntimeBeanReference(this.sqlSessionFactoryBeanName));
    }
}   
```

#### 分析

```bash
# MapperScannerConfigurer的主要作用
	# 通过postProcessBeanDefinitionRegistry方法，将配置包下的mapperClass加载为BeanDefinition注册到BeanFactory中
	# 将BeanDefinition的BeanClass替换为MapperFactoryBean，使用MapperFactoryBean去返回对应mapper代理实例
		# 添加属性'sqlSessionFactory'对应引用的sqlSessionFactory对象
		# 添加构造器属性：当前对应MapperClass
```

### MapperFactoryBean

```java
//org.mybatis.spring.mapper.MapperFactoryBean
public class MapperFactoryBean<T> extends SqlSessionDaoSupport implements FactoryBean<T> {
    //当前MapperFactoryBean对应的mapperClass
    private Class<T> mapperInterface;
	
    /* 1. 构造设置mapperInterface */
    public MapperFactoryBean(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }

    /* 继承自FactoryBean接口 */
    public T getObject() throws Exception {
        return getSqlSession().getMapper(this.mapperInterface);
    }

    //检查配置
    protected void checkDaoConfig() {
        super.checkDaoConfig();
		//获取当前sqlSessionTemplate获取Configuration对象
        Configuration configuration = getSqlSession().getConfiguration();
		//使用Configuration添加mapper
        configuration.addMapper(this.mapperInterface);
    }
}
```

#### SqlSessionDaoSupport

```java
//org.mybatis.spring.support.SqlSessionDaoSupport
public abstract class SqlSessionDaoSupport extends DaoSupport {
    //持有sqlSessionTemplate引用
    private SqlSessionTemplate sqlSessionTemplate;

    //设置SqlSessionFactory对象属性时，创建sqlSessionTemplate
    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        if (this.sqlSessionTemplate == null || sqlSessionFactory != this.sqlSessionTemplate.getSqlSessionFactory()) {
            this.sqlSessionTemplate = createSqlSessionTemplate(sqlSessionFactory);
        }
    }
	
    //创建SqlSessionTemplate
    protected SqlSessionTemplate createSqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
```

#### DaoSupport

```java
//org.springframework.dao.support.DaoSupport
public abstract class DaoSupport implements InitializingBean {
    //继承自InitializingBean的初始化方法
	public final void afterPropertiesSet() throws IllegalArgumentException, BeanInitializationException {
        //检查配置
        checkDaoConfig();
    }
}
```

#### 分析

```bash
# 按MapperFactoryBean对象创建的过程

# 1.实例化
	# 执行MapperFactoryBean构造，设置mapperInterface
	
# 2. 属性设置
	# 执行setSqlSessionFactory，创建SqlSessionTemplate
	
# 3. 初始化
	# 执行afterPropertiesSet，内部执行checkDaoConfig进行mapper添加
		# 此步骤可以使SqlSessionFactoryBean无需配置对应mapper.xml资源，转而在此步骤借用addMapper进行解析
		
# 4. 最后执行getObject
	# 获取的mapper接口代理对象，注册到Spring容器中，供注入使用
```

### SqlSessionTemplate

```java
//org.mybatis.spring.SqlSessionTemplate
public class SqlSessionTemplate implements SqlSession, DisposableBean {
    //持有SqlSessionFactory对象
    private final SqlSessionFactory sqlSessionFactory;
    //当前获取的SqlSession代理对象
    private final SqlSession sqlSessionProxy;

    /* 构造方法，创建sqlSessionProxy代理对象 */
    public SqlSessionTemplate(SqlSessionFactory sqlSessionFactory, ExecutorType executorType,
                              PersistenceExceptionTranslator exceptionTranslator) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.executorType = executorType;
        this.exceptionTranslator = exceptionTranslator;
        this.sqlSessionProxy = (SqlSession) newProxyInstance(SqlSessionFactory.class.getClassLoader(),
                                                             new Class[] { SqlSession.class }, new SqlSessionInterceptor());
    }

	//传入自身作为SqlSession，通过Configuration对象获取mapper代理对象
    public <T> T getMapper(Class<T> type) {
        return getConfiguration().getMapper(type, this);
    }

    /*内部类，实现InvocationHandler接口*/
    private class SqlSessionInterceptor implements InvocationHandler {
        //代理方法
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            //获取sqlSession
            SqlSession sqlSession = getSqlSession(SqlSessionTemplate.this.sqlSessionFactory,
                   SqlSessionTemplate.this.executorType, SqlSessionTemplate.this.exceptionTranslator);
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
}
```

#### 分析

```bash
# mapper对象获取
	# SqlSessionTemplate调用getMapper是，传入的SqlSession是其本身
	# 由于Mybatis代理方式，最终调用SqlSession进行方法执行
		# 由于传入的SqlSession为SqlSessionTemplate自身，所以调用SqlSessionTemplate对应方法
	# SqlSessionTemplate调用方法时，都是借用sqlSessionProxy代理对象执行
		# 代理对象执行方法时，通过getSqlSession获取对应SqlSession进行结果获取
```

# Spring声明式事务

## 配置

```bash
# 配置类
	# 使用@EnableTransactionManagement注解表示开启事务支持

# 使用
	# 方法上添加@Transactional注解
	# 注意：此方法对应对象需为spring管理对象
```

## 分析

```bash
# @EnableTransactionManagement
	# 使用@Import注解引入类TransactionManagementConfigurationSelector
```

#### TransactionManagementConfigurationSelector

- org.springframework.transaction.annotation.TransactionManagementConfigurationSelector

- 实现ImportSelector接口
  - org.springframework.context.annotation.ImportSelector
- 通过selectImports方法注册
  - AutoProxyRegistrar
  - ProxyTransactionManagementConfiguration

### 涉及类库

#### AutoProxyRegistrar

- 实现ImportBeanDefinitionRegistrar接口

```java
//org.springframework.context.annotation.AutoProxyRegistrar
public class AutoProxyRegistrar implements ImportBeanDefinitionRegistrar { 
    
    /* 实现ImportBeanDefinitionRegistrar接口方法，注册BeanDefinition*/
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        //调用方法注册InfrastructureAdvisorAutoProxyCreator类对应BeanDefinition
        AopConfigUtils.registerAutoProxyCreatorIfNecessary(registry);
    }
}
```

#### InfrastructureAdvisorAutoProxyCreator

- 实现BeanPostProcessor接口

```java
//org.springframework.aop.framework.autoproxy.InfrastructureAdvisorAutoProxyCreator
public class InfrastructureAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator {
	
    /* 实现自BeanPostProcessor接口，进行初始化后处理，继承自AbstractAdvisorAutoProxyCreator抽象父类 */
    public Object postProcessAfterInitialization(@Nullable Object bean, String beanName) {
        //...
        //必要时创建代理对象返回
        return wrapIfNecessary(bean, beanName, cacheKey);
    }
    
    /* 必要时创建代理对象返回 */
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        // 获取Advice，spring声明式事务处理Advice：BeanFactoryTransactionAttributeSourceAdvisor
		Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
		if (specificInterceptors != DO_NOT_PROXY) {
			this.advisedBeans.put(cacheKey, Boolean.TRUE);
            //使用Aop代理，创建cglib代理对象
			Object proxy = createProxy(
					bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}

		this.advisedBeans.put(cacheKey, Boolean.FALSE);
		return bean;
    }
}
```

#### ProxyTransactionManagementConfiguration

```java
//org.springframework.transaction.annotation.ProxyTransactionManagementConfiguration
@Configuration
public class ProxyTransactionManagementConfiguration extends AbstractTransactionManagementConfiguration {

    @Bean(name = TransactionManagementConfigUtils.TRANSACTION_ADVISOR_BEAN_NAME)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public BeanFactoryTransactionAttributeSourceAdvisor transactionAdvisor() {
        BeanFactoryTransactionAttributeSourceAdvisor advisor = new BeanFactoryTransactionAttributeSourceAdvisor();
        advisor.setTransactionAttributeSource(transactionAttributeSource());
        advisor.setAdvice(transactionInterceptor());
        if (this.enableTx != null) {
            advisor.setOrder(this.enableTx.<Integer>getNumber("order"));
        }
        return advisor;
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public TransactionAttributeSource transactionAttributeSource() {
        return new AnnotationTransactionAttributeSource();
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public TransactionInterceptor transactionInterceptor() {
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionAttributeSource(transactionAttributeSource());
        if (this.txManager != null) {
            interceptor.setTransactionManager(this.txManager);
        }
        return interceptor;
    }

}
```

### 过程分析

```bash
# 1. 由TransactionManagementConfigurationSelector注册两个类
    # AutoProxyRegistrar
    # ProxyTransactionManagementConfiguration
        
# 2. AutoProxyRegistrar实现ImportBeanDefinitionRegistrar接口
	# 通过调用registerBeanDefinitions方法，注册InfrastructureAdvisorAutoProxyCreator类BeanDefinition
	
# 3.ProxyTransactionManagementConfiguration是一个配置类
	# 通过@Bean注解，注册BeanDefinition
		# BeanFactoryTransactionAttributeSourceAdvisor
		# TransactionAttributeSource
		# TransactionInterceptor
		
# 4. InfrastructureAdvisorAutoProxyCreator实现BeanPostProcessor接口
	# 调用postProcessAfterInitialization方法
		# 内部调用wrapIfNecessary方法
    # 过程
    	# 1. 获取对应Advice
    		# getAdvicesAndAdvisorsForBean
    		# 为ProxyTransactionManagementConfiguration注册的BeanFactoryTransactionAttributeSourceAdvisor
    	# 2. 通过Advice，创建类对应Cglib代理对象

# 判断类匹配BeanFactoryTransactionAttributeSourceAdvisor这个Advice
	# 入口
		# org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#getAdvicesAndAdvisorsForBean
	# 分析
		# 1. BeanFactoryTransactionAttributeSourceAdvisor通过匿名内部类创建TransactionAttributeSourcePointcut
			# 设置transactionAttributeSource为TransactionAttributeSource
		# 2. getAdvicesAndAdvisorsForBean方法内部调用canApply方法判断Advice是否适配类
			# org.springframework.aop.support.AopUtils#canApply
		# 3. 获取TransactionAttributeSourcePointcut对应MethodMatcher
			# org.springframework.aop.Pointcut#getMethodMatcher
			# 由于TransactionAttributeSourcePointcut自身实现了MethodMatcher接口
				# 获取的MethodMatcher为其自身
		# 4. 遍历类的方法，调用MethodMatcher.matches方法判断
			# 获取TransactionAttributeSource进行判断，调用getTransactionAttribute方法
				# org.springframework.transaction.interceptor.TransactionAttributeSource#getTransactionAttribute
			# 通过SpringTransactionAnnotationParser的parseTransactionAnnotation方法判断
				# 如果类方法有@Transactional注解，则适配
```