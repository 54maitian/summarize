# Mybatis重点

```
- Mybatis架构流程可查看架构流程图
```

# Mapper代理

```bash
# 使用Mybatis进行操作时，有两种操作方式
	# 通过指定statementId字符串进行操作
	# 通过获取mapper接口的代理对象，借用代理对象进行操作
```

## Mapper代理工厂创建

```java
//mapper注册器：org.apache.ibatis.binding.MapperProxyFactory
public <T> void addMapper(Class<T> type) {
    if (type.isInterface()) {
        if (hasMapper(type)) {
            throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
        }
        boolean loadCompleted = false;
        try {
            //根据mapper类型，创建代理工厂并存储：Map<Class<?>, MapperProxyFactory<?>> knownMappers
            knownMappers.put(type, new MapperProxyFactory<T>(type));
            MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
            parser.parse();
            loadCompleted = true;
        } finally {
            if (!loadCompleted) {
                knownMappers.remove(type);
            }
        }
    }
}
```

## Mapper代理对象创建

- SqlSession

  ```java
  <T> T getMapper(Class<T> type)
  ```

- MapperRegistry

  ```java
  public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
      final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
      if (mapperProxyFactory == null) {
          throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
      }
      try {
          return mapperProxyFactory.newInstance(sqlSession);
      } catch (Exception e) {
          throw new BindingException("Error getting mapper instance. Cause: " + e, e);
      }
  }
  ```

- MapperProxyFactory

  ```java
  protected T newInstance(MapperProxy<T> mapperProxy) {
      return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
  }
  
  public T newInstance(SqlSession sqlSession) {
      final MapperProxy<T> mapperProxy = new MapperProxy<T>(sqlSession, mapperInterface, methodCache);
      return newInstance(mapperProxy);
  }
  ```

## Mapper方法调用代理

```java
//实质是调用SqlSession进行JDBC操作
public class MapperProxy<T> implements InvocationHandler{
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            if (Object.class.equals(method.getDeclaringClass())) {
                return method.invoke(this, args);
            } else if (isDefaultMethod(method)) {
                return invokeDefaultMethod(proxy, method, args);
            }
        } catch (Throwable t) {
            throw ExceptionUtil.unwrapThrowable(t);
        }
        final MapperMethod mapperMethod = cachedMapperMethod(method);
        return mapperMethod.execute(sqlSession, args);
    }
}
```

# 一级缓存

```bash
# 一级默认开启
# 对应SqlSession
# 类型为org.apache.ibatis.cache.impl.PerpetualCache
	# 底层为HashMap存储
```

## PerpetualCache

```java
public class PerpetualCache implements Cache {
    private Map<Object, Object> cache = new HashMap<Object, Object>();
}
```

## 容器创建

```java
//创建Executer时，默认在BaseExecutor抽象父类的构造中创建
public abstract class BaseExecutor implements Executor {
    protected BaseExecutor(Configuration configuration, Transaction transaction) {
    //创建一级缓存容器
  }
}
```

## CacheKey创建

```java
public class CacheKey implements Cloneable, Serializable {
    //主要通过此list集合存储对应请求参数
    private List<Object> updateList;
}
```

```java
//org.apache.ibatis.executor.BaseExecutor
@Override
public CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
    if (closed) {
        throw new ExecutorException("Executor was closed.");
    }
    CacheKey cacheKey = new CacheKey();
    //statementId
    cacheKey.update(ms.getId());
    //分页参数
    cacheKey.update(rowBounds.getOffset());
    cacheKey.update(rowBounds.getLimit());
    //解析后sql
    cacheKey.update(boundSql.getSql());
    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    TypeHandlerRegistry typeHandlerRegistry = ms.getConfiguration().getTypeHandlerRegistry();
    //参数列表
    for (ParameterMapping parameterMapping : parameterMappings) {
        if (parameterMapping.getMode() != ParameterMode.OUT) {
            Object value;
            String propertyName = parameterMapping.getProperty();
            if (boundSql.hasAdditionalParameter(propertyName)) {
                value = boundSql.getAdditionalParameter(propertyName);
            } else if (parameterObject == null) {
                value = null;
            } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                value = parameterObject;
            } else {
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                value = metaObject.getValue(propertyName);
            }
            cacheKey.update(value);
        }
    }
    if (configuration.getEnvironment() != null) {
        // 数据Environment的id
        cacheKey.update(configuration.getEnvironment().getId());
    }
    return cacheKey;
}
```

## 缓存创建

```java
//在进行查询时，如果未命中，此时需要进行JDBC操作获取结果，并将其存储到一级缓存中
//org.apache.ibatis.executor.BaseExecutor
private <E> List<E> queryFromDatabase(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
    List<E> list;
    localCache.putObject(key, EXECUTION_PLACEHOLDER);
    try {
        list = doQuery(ms, parameter, rowBounds, resultHandler, boundSql);
    } finally {
        localCache.removeObject(key);
    }
    //缓存结果数据
    localCache.putObject(key, list);
    if (ms.getStatementType() == StatementType.CALLABLE) {
        localOutputParameterCache.putObject(key, parameter);
    }
    return list;
}
```

## 缓存获取

```java
//在查询前，从缓存中获取
//org.apache.ibatis.executor.BaseExecutor
list = resultHandler == null ? (List<E>) localCache.getObject(key) : null;
```

# 二级缓存

```bash
# 二级缓存默认关闭
	# 对应主配置
		# mybatis配置文件中
			# CacheEnable参数
				# 默认值为true
	# mapper配置
		# 注解方式
			# 解析org.apache.ibatis.annotations.CacheNamespace注解
		# xml配置
			# 解析<cache/>标签
		# 可通过type设置不同二级缓存容器
	# 对应查询配置
		# <select useCache="true"/>
		
# 二级缓存对应nameSpace级别
	# 对应每个mapper接口，有其对应的二级缓存容器
# 二级缓存不是即时生效的
	# 需要sqlSession进行commit/close时，缓存才生效 
```

## 容器创建

```bash
# mapper有两种解析方式
	# mapper接口解析
        # MapperAnnotationBuilder
            # parseCache()
    # mapper配置文件解析
    	# XMLMapperBuilder
    		# configurationElement()
    			# cacheElement(context.evalNode("cache"))
# 两种方式最终都会调用MapperBuilderAssistant.useNewCache进行缓存容器创建    
```

```java
//org.apache.ibatis.builder.MapperBuilderAssistant
public Cache useNewCache(Class<? extends Cache> typeClass,
                         Class<? extends Cache> evictionClass,
                         Long flushInterval,
                         Integer size,
                         boolean readWrite,
                         boolean blocking,
                         Properties props) {
    //默认使用PerpetualCache类型作为二级缓存容器，可自定义其他二级缓存容器
    //以currentNamespace作为id标志
    Cache cache = new CacheBuilder(currentNamespace)
        .implementation(valueOrDefault(typeClass, PerpetualCache.class))
        .addDecorator(valueOrDefault(evictionClass, LruCache.class))
        .clearInterval(flushInterval)
        .size(size)
        .readWrite(readWrite)
        .blocking(blocking)
        .properties(props)
        .build();
    //在configuration中以id作为key值存储
    configuration.addCache(cache);
    //此处为临时保存引用，后续放入MappedStatement中
    // MapperBuilderAssistant.addMappedStatement()方法中，通过currentCache，将二级缓存存储到MappedStatement中
    currentCache = cache;
    return cache;
}
```

## 缓存存储位置

```java
//Configuration中
public class Configuration {
    protected final Map<String, Cache> caches = new StrictMap<Cache>("Caches collection");
    public void addCache(Cache cache) {
        caches.put(cache.getId(), cache);
    }
}

//MappedStatement中
public final class MappedStatement {
    private Cache cache;
    public Cache getCache() {
        return cache;
    }
}
```

## 缓存创建

```java
//开启二级缓存后，将会由CachingExecutor对Executor进行包装
public class Configuration {
    public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
        executorType = executorType == null ? defaultExecutorType : executorType;
        executorType = executorType == null ? ExecutorType.SIMPLE : executorType;
        Executor executor;
        if (ExecutorType.BATCH == executorType) {
            executor = new BatchExecutor(this, transaction);
        } else if (ExecutorType.REUSE == executorType) {
            executor = new ReuseExecutor(this, transaction);
        } else {
            executor = new SimpleExecutor(this, transaction);
        }
        //此处根据cacheEnabled参数判断进行包装
        if (cacheEnabled) {
            executor = new CachingExecutor(executor);
        }
        executor = (Executor) interceptorChain.pluginAll(executor);
        return executor;
    }
}
```

## 二级缓存机制

### CachingExecutor

```java
//内置一个TransactionalCacheManager，通过其进行二级缓存的事物管理
public class CachingExecutor implements Executor {
    //被包装的Executor
    private Executor delegate;
    private TransactionalCacheManager tcm = new TransactionalCacheManager();
    
    @Override
    public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql)
        throws SQLException {
        //获取ms中Cache对象
        Cache cache = ms.getCache();
        if (cache != null) {
            flushCacheIfRequired(ms);
            if (ms.isUseCache() && resultHandler == null) {
                ensureNoOutParams(ms, parameterObject, boundSql);
                @SuppressWarnings("unchecked")
                //由tcm进行缓存的获取管理
                List<E> list = (List<E>) tcm.getObject(cache, key);
                if (list == null) {
                    //未命中二级缓存时，通过被包装的Executor进行数据获取
                    list = delegate.<E> query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
                     //由tcm进行缓存的存储管理
                    tcm.putObject(cache, key, list);
                }
                return list;
            }
        }
        return delegate.<E> query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
    }

    private void flushCacheIfRequired(MappedStatement ms) {
        Cache cache = ms.getCache();
        //如果参数flushCacheRequired为true，表示更新操作需要刷新二级缓存
        if (cache != null && ms.isFlushCacheRequired()) {  
            //进行二级缓存刷新
            tcm.clear(cache);
        }
    }
}
```

### TransactionalCacheManager

```java
public class TransactionalCacheManager {
    //保存不同的Cache对应的TransactionalCache，此处Cache对象为ms中Cache对象
    //所以对应一个nameSpace，有其对应的TransactionalCache
    private Map<Cache, TransactionalCache> transactionalCaches = new HashMap<Cache, TransactionalCache>();

    //根据CacheKey从二级缓存中获取数据
    public Object getObject(Cache cache, CacheKey key) {
        return getTransactionalCache(cache).getObject(key);
    }
	
    //根据参数存储结果到二级缓存中
    public void putObject(Cache cache, CacheKey key, Object value) {
        getTransactionalCache(cache).putObject(key, value);
    }
	
    //获取Cache对应TransactionalCache，如果没有则创建并保存
    private TransactionalCache getTransactionalCache(Cache cache) {
        TransactionalCache txCache = transactionalCaches.get(cache);
        if (txCache == null) {
            txCache = new TransactionalCache(cache);
            transactionalCaches.put(cache, txCache);
        }
        return txCache;
    }
    
    //二级缓存刷新
    public void clear(Cache cache) {
        getTransactionalCache(cache).clear();
    }
}
```

### TransactionalCache

```java
public class TransactionalCache implements Cache {
    //ms对应的真实Cache容器对象
    private Cache delegate;
    //一个临时的缓存存储对象
    private Map<Object, Object> entriesToAddOnCommit;

    public Object getObject(Object key) {
        // 缓存的获取是从真实Cache容器中获取
        Object object = delegate.getObject(key);
        if (object == null) {
            entriesMissedInCache.add(key);
        }
        // issue #146
        if (clearOnCommit) {
            return null;
        } else {
            return object;
        }
    }

    public void putObject(Object key, Object object) {
        //缓存的存储只是存储在临时缓存容器中
        entriesToAddOnCommit.put(key, object);
    }

    private void flushPendingEntries() {
        //在SqlSession调用commit/close方法时，会将临时缓存容器中缓存数据刷新到真实缓存Cache对象中
        for (Map.Entry<Object, Object> entry : entriesToAddOnCommit.entrySet()) {
            delegate.putObject(entry.getKey(), entry.getValue());
        }
        for (Object entry : entriesMissedInCache) {
            if (!entriesToAddOnCommit.containsKey(entry)) {
                delegate.putObject(entry, null);
            }
        }
    }
    
    public void clear() {
        //缓存刷新，是将临时缓存容器清空
        clearOnCommit = true;
        entriesToAddOnCommit.clear();
    }
}
```

# 插件机制

```bash
# Mybatis插件实质
	# 通过org.apache.ibatis.plugin.Interceptor拦截器进行方法的拦截、增强
# 允许拦截的类及其方法
	# Executor
		# update()，query()，flushStatement()，commit()，rollback()，getTransaction()，close()，isClosed()等方法；
	# ParameterHandler
		# getParameterObject()，setParameters()等方法；
	# ResultSetHandler
		# handleResultSets()，handleOutputParameters()；
	# ResultSetHandler
		# handleResultSets()，handleOutputParameters()；	
```

## 拦截设置

### 配置解析

```java
//org.apache.ibatis.builder.xml.XMLConfigBuilder
private void parseConfiguration(XNode root) {
    try {
        //插件配置解析
        pluginElement(root.evalNode("plugins"));
    } catch (Exception e) {
        throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
    }
}

private void pluginElement(XNode parent) throws Exception {
    if (parent != null) {
        for (XNode child : parent.getChildren()) {
            String interceptor = child.getStringAttribute("interceptor");
            Properties properties = child.getChildrenAsProperties();
            Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).newInstance();
            interceptorInstance.setProperties(properties);
            //插件添加
            configuration.addInterceptor(interceptorInstance);
        }
    }
}
```

## 拦截增强

```java
public class InterceptorChain {
	//拦截器链
    private final List<Interceptor> interceptors = new ArrayList<Interceptor>();

    //进行增强
    public Object pluginAll(Object target) {
        for (Interceptor interceptor : interceptors) {
            target = interceptor.plugin(target);
        }
        return target;
    }

    //添加拦截器
    public void addInterceptor(Interceptor interceptor) {
        interceptors.add(interceptor);
    }

    public List<Interceptor> getInterceptors() {
        return Collections.unmodifiableList(interceptors);
    }
}
```

### Executor

```java
//org.apache.ibatis.session.Configuration#newExecutor
public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
    executorType = executorType == null ? defaultExecutorType : executorType;
    executorType = executorType == null ? ExecutorType.SIMPLE : executorType;
    Executor executor;
    if (ExecutorType.BATCH == executorType) {
      executor = new BatchExecutor(this, transaction);
    } else if (ExecutorType.REUSE == executorType) {
      executor = new ReuseExecutor(this, transaction);
    } else {
      executor = new SimpleExecutor(this, transaction);
    }
    if (cacheEnabled) {
      executor = new CachingExecutor(executor);
    }
    //进行拦截增强
    executor = (Executor) interceptorChain.pluginAll(executor);
    return executor;
  }
```

### StatementHandler

```java
//org.apache.ibatis.session.Configuration#newStatementHandler
public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    StatementHandler statementHandler = new RoutingStatementHandler(executor, mappedStatement, parameterObject, rowBounds, resultHandler, boundSql);
    //进行拦截增强
    statementHandler = (StatementHandler) interceptorChain.pluginAll(statementHandler);
    return statementHandler;
}
```

### ParameterHandler

```java
//org.apache.ibatis.session.Configuration#newParameterHandler
public ParameterHandler newParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
    ParameterHandler parameterHandler = mappedStatement.getLang().createParameterHandler(mappedStatement, parameterObject, boundSql);
    //进行拦截增强
    parameterHandler = (ParameterHandler) interceptorChain.pluginAll(parameterHandler);
    return parameterHandler;
}
```

### ResultSetHandler

```java
//org.apache.ibatis.session.Configuration#newResultSetHandler
public ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds, ParameterHandler parameterHandler,
                                            ResultHandler resultHandler, BoundSql boundSql) {
    ResultSetHandler resultSetHandler = new DefaultResultSetHandler(executor, mappedStatement, parameterHandler, resultHandler, boundSql, rowBounds);
    //进行拦截增强
    resultSetHandler = (ResultSetHandler) interceptorChain.pluginAll(resultSetHandler);
    return resultSetHandler;
}
```

## 拦截器创建

```bash
# 实现接口Interceptor
# 添加注解
	# @Intercepts({@Signature(type= Executor.class, method = "update", args = {MappedStatement.class,Object.class})})
	# @Intercepts
		# 声明多个@Signature
	# @Signature
		# type
			# 拦截的类型
		# method
			# 对应拦截的方法
        # args
			# 对应拦截方法的参数列表
			# 避免方法重载导致的异常
```

# 延时加载机制

```bash
# 延时加载概念
	# 在使用ResultHandler获取请求的结果对象时
		# 如果是配置为resultMap
		# resultMap对应子节点存在fetchType="lazy"的懒加载配置
	# 则不直接返回查询结果对象
    	# 而是返回包装的结果对象
    	# 在包装的结果对象进行目标数据获取时，才调用对应sql进行数据获取，再设置值
    		# 达到延时加载的效果
# 默认使用JavassistProxyFactory实现延时加载
	# Configuration
		# ProxyFactory proxyFactory = new JavassistProxyFactory();
		
```

## DefaultResultSetHandler

```java
//org.apache.ibatis.executor.resultset.DefaultResultSetHandler
private Object createResultObject(ResultSetWrapper rsw, ResultMap resultMap, ResultLoaderMap lazyLoader, String columnPrefix) throws SQLException {
    this.useConstructorMappings = false; // reset previous mapping result
    final List<Class<?>> constructorArgTypes = new ArrayList<Class<?>>();
    final List<Object> constructorArgs = new ArrayList<Object>();
    Object resultObject = createResultObject(rsw, resultMap, constructorArgTypes, constructorArgs, columnPrefix);
    if (resultObject != null && !hasTypeHandlerForResultObject(rsw, resultMap.getType())) {
        //获取resultMap配置解析结果
        final List<ResultMapping> propertyMappings = resultMap.getPropertyResultMappings();
        for (ResultMapping propertyMapping : propertyMappings) {
            // 判断子节点是否有嵌套的nestedQueryId，是否配置为懒加载
            if (propertyMapping.getNestedQue有对应的sqlryId() != null && propertyMapping.isLazy()) {
                //符合条件则通过代理工厂创建代理结果对象
                resultObject = configuration.getProxyFactory().createProxy(resultObject, lazyLoader, configuration, objectFactory, constructorArgTypes, constructorArgs);
                break;
            }
        }
    }
    this.useConstructorMappings = (resultObject != null && !constructorArgTypes.isEmpty()); // set current mapping result
    return resultObject;
}
```

## JavassistProxyFactory

```java
public class JavassistProxyFactory implements org.apache.ibatis.executor.loader.ProxyFactory {
    @Override
    public Object createProxy(Object target, ResultLoaderMap lazyLoader, Configuration configuration, ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
        return EnhancedResultObjectProxyImpl.createProxy(target, lazyLoader, configuration, objectFactory, constructorArgTypes, constructorArgs);
    }

    public Object createDeserializationProxy(Object target, Map<String, ResultLoaderMap.LoadPair> unloadedProperties, ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
        return EnhancedDeserializationProxyImpl.createProxy(target, unloadedProperties, objectFactory, constructorArgTypes, constructorArgs);
    }
    
    //内部类，实现了MethodHandler接口
    private static class EnhancedResultObjectProxyImpl implements MethodHandler {
        //代理对象的类
        private final Class<?> type;
        //触发方法列表，调用此方法时，将会加载全部懒加载属性
        private final Set<String> lazyLoadTriggerMethods;
    }
    
    @Override
    //具体处理没有详细分析
    public Object invoke(Object enhanced, Method method, Method methodProxy, Object[] args) throws Throwable {
        final String methodName = method.getName();
        try {
            synchronized (lazyLoader) {
                if (WRITE_REPLACE_METHOD.equals(methodName)) {
                    Object original;
                    if (constructorArgTypes.isEmpty()) {
                        original = objectFactory.create(type);
                    } else {
                        original = objectFactory.create(type, constructorArgTypes, constructorArgs);
                    }
                    PropertyCopier.copyBeanProperties(type, enhanced, original);
                    if (lazyLoader.size() > 0) {
                        return new JavassistSerialStateHolder(original, lazyLoader.getProperties(), objectFactory, constructorArgTypes, constructorArgs);
                    } else {
                        return original;
                    }
                } else {
                    if (lazyLoader.size() > 0 && !FINALIZE_METHOD.equals(methodName)) {
                        if (aggressive || lazyLoadTriggerMethods.contains(methodName)) {
                            lazyLoader.loadAll();
                        } else if (PropertyNamer.isGetter(methodName)) {
                            final String property = PropertyNamer.methodToProperty(methodName);
                            if (lazyLoader.hasLoader(property)) {
                                lazyLoader.load(property);
                            }
                        }
                    }
                }
            }
            return methodProxy.invoke(enhanced, args);
        } catch (Throwable t) {
            throw ExceptionUtil.unwrapThrowable(t);
        }
    }
}
}
```





































