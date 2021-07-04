# 循环依赖问题

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
		# 放置完整的SpringBean：已经走完spring的生命周期
	# earlySingletonObjects
		# 二级缓存
		# 放置早期暴露的SpringBean，没有走完整个生命周期
		# 早期暴露的目的：如果一个Bean被多个Bean依赖，则无需再次由三级缓存创建代理对象并保存到二级缓存中，而是直接可以从二级缓存中获取
		# 不取消二级缓存的目的：由于一级缓存都是保存完整的SpringBean，如果取消二级缓存，将提前暴露的不完整的SpringBean也保存在此，则导致一级缓存属性不唯一，违背单一原则
	# singletonFactories
		# 三级缓存
		# 单例工厂对象
		# 如果Bean需要进行AOP代理，则需要通过ObjectFactory来创建代理对象返回
```

## DefaultSingletonBeanRegistry

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

## ObjectFactory

- 一个对象工厂
- 三级缓存的实体对象

```java
//org.springframework.beans.factory.ObjectFactory
@FunctionalInterface
public interface ObjectFactory<T> {
	T getObject() throws BeansException;
}
```

## 过程分析

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

#### 