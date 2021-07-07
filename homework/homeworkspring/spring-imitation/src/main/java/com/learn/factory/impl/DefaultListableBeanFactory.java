package com.learn.factory.impl;

import com.learn.aware.BeanFactoryAware;
import com.learn.config.BeanDefinition;
import com.learn.config.BeanPostProcessor;
import com.learn.config.MergedBeanDefinitionPostProcessor;
import com.learn.config.support.impl.DefaultSingletonBeanRegistry;
import com.learn.factory.BeanFactory;
import com.learn.factory.FactoryBean;
import com.learn.init.InitializingBean;
import com.learn.model.Attr;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author T00032266
 * @DateTime 2021/6/29
 */
public class DefaultListableBeanFactory extends DefaultSingletonBeanRegistry implements BeanFactory {

    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap(256);

    private volatile List<String> beanDefinitionNames = new ArrayList(256);

    private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();


    private final Set<String> registeredSingletons = new LinkedHashSet<>(256);


    /**
     * 注册BeanDefinition
     */
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        this.beanDefinitionMap.put(beanName, beanDefinition);
        this.beanDefinitionNames.add(beanName);
    }


    /**
     * 名称获取对象
     */
    public Object getBean(String name) throws Exception {
        Object singleton = getSingleton(name);
        if (singleton != null) {
            return singleton;
        }
        return doGetBean(name, null);
    }


    public <T> T getBean(Class<T> requiredType) throws Exception {
        String[] beanNamesForType = getBeanNamesForType(requiredType);
        if (beanNamesForType.length > 1) {
            throw new RuntimeException("多于");
        }
        return (T)getBean(beanNamesForType[0]);
    }



    public <T> T doGetBean(String name, final Class<T> requiredType) throws Exception{
        Object bean;
        //单例池中获取
        bean = getSingleton(name);
        if (bean == null) {
            BeanDefinition mbd = beanDefinitionMap.get(name);
            if (mbd == null) {
                return null;
            }
            bean = createBean(name, mbd, null);
            //存储到单例池
            addSingleton(name, bean);
            //标志已经创建
            registeredSingletons.add(name);
        }
        return (T) bean;
    }


    /**
     * 创建bean实例
     */
    private Object createBean(String name, BeanDefinition mbd, Object[] args) throws Exception {
        //创建bean
        Object bean = createBeanInstance(mbd, args);
        applyMergedBeanDefinitionPostProcessors(name, bean, mbd);
        //属性设置
        populateBean(name, mbd, bean);
        //初始化操作
        bean = initializeBean(name, bean, mbd);
        return bean;
    }


    /**
     * MergedBeanDefinitionPostProcessor后置处理实现@Autowired
     */
    private void applyMergedBeanDefinitionPostProcessors(String name, Object bean, BeanDefinition mbd) throws Exception {
        for (BeanPostProcessor beanPostProcessor : this.beanPostProcessors) {
            if (MergedBeanDefinitionPostProcessor.class.isAssignableFrom(beanPostProcessor.getClass())) {
                MergedBeanDefinitionPostProcessor processor = (MergedBeanDefinitionPostProcessor)beanPostProcessor;
                processor.postProcessMergedBeanDefinition(mbd, mbd.getBeanClass(), name);
            }
        }
    }

    /**
     * TODO 初始化操作
     */
    private Object initializeBean(String name, Object bean, BeanDefinition mbd) throws Exception {

        doInitialize(bean);

        applyBeanPostProcessorsAfterInitialization(bean, name);
        return bean;
    }

    /**
     * 初始化
     */
    public void doInitialize(Object bean) throws Exception {
        Class<?> aClass = bean.getClass();
        if (InitializingBean.class.isAssignableFrom(aClass)) {
            ((InitializingBean)bean).afterPropertiesSet();
        }

        //此处特殊处理了BeanFactoryAware，没有进行后置处理
        if (BeanFactoryAware.class.isAssignableFrom(aClass)) {
            ((BeanFactoryAware)bean).setBeanFactory(this);
        }
    }


    /**
     * BeanPostProcessor后置处理实现@Autowired
     */
    private void applyBeanPostProcessorsAfterInitialization(Object bean, String name) throws Exception {
        for (BeanPostProcessor beanPostProcessor : this.beanPostProcessors) {
            beanPostProcessor.postProcessAfterInitialization(bean, name);
        }
    }


    /**
     * TODO 属性设置
     */
    private void populateBean(String name, BeanDefinition mbd, Object bean) throws Exception {
        Class<?> aClass = bean.getClass();
        List<Attr> attrList = mbd.getAttrList();
        for (Attr attr : attrList) {
            String attrName = attr.getAttrName();
            Object attrValue = attr.getAttrValue();
            Field field = aClass.getDeclaredField(attrName);
            field.setAccessible(true);
            field.set(bean, attrValue);
        }
    }

    public Object createBeanInstance(BeanDefinition mbd, Object[] args) throws Exception {
        Class beanClass = mbd.getBeanClass();
        String factoryMethodName = mbd.getFactoryMethodName();
        String factoryBeanName = mbd.getFactoryBeanName();

        if (factoryMethodName != null) {
            Method method = beanClass.getDeclaredMethod(factoryMethodName, null);
            if (factoryBeanName != null) {
                //实例方法
                Object bean = getBean(factoryBeanName);
                return method.invoke(bean, args);
            } else {
                //静态方法
                return method.invoke(null, args);
            }
        } else {
            //构造
            Constructor declaredConstructor = beanClass.getDeclaredConstructor();
            Object instance = declaredConstructor.newInstance(args);
            // FactoryBean处理
            if (FactoryBean.class.isAssignableFrom(beanClass)) {
                return ((FactoryBean)instance).getObject();
            }
            return instance;
        }

    }


    /**
     * 根据类型获取对应beanNames
     */
    public String[] getBeanNamesForType(Class<?> type) throws Exception {
        List<String> result = new ArrayList<>();

        for (String beanDefinitionName : this.beanDefinitionNames) {
            BeanDefinition beanDefinition = this.beanDefinitionMap.get(beanDefinitionName);
            String factoryMethodName = beanDefinition.getFactoryMethodName();
            String factoryBeanName = beanDefinition.getFactoryBeanName();
            Class beanClass = beanDefinition.getBeanClass();

            if (factoryMethodName != null) {
                if (factoryBeanName != null) {
                    //实例方法
                    Object bean = getBean(factoryBeanName);
                    if (type.isAssignableFrom(bean.getClass().getMethod(factoryMethodName).getReturnType())) {
                        result.add(beanDefinitionName);
                    }
                } else {
                    //静态方法
                    if (type.isAssignableFrom(beanClass.getMethod(factoryMethodName).getReturnType())) {
                        result.add(beanDefinitionName);
                    }
                }
            } else {
                if (type.isAssignableFrom(beanClass)) {
                    result.add(beanDefinitionName);
                } else if (FactoryBean.class.isAssignableFrom(beanClass)) {
                    // FactoryBean处理
                    Class objectType = ((FactoryBean) getBean(beanDefinitionName)).getObjectType();
                    if (type.isAssignableFrom(objectType)) {
                        result.add(beanDefinitionName);
                    }
                }
            }


        }
        String[] strings = new String[result.size()];
        return result.toArray(strings);
    }

    @Override
    public String[] getBeanDefinitionNames() {
        String[] strings = new String[this.beanDefinitionNames.size()];
        return this.beanDefinitionNames.toArray(strings);
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        return this.beanDefinitionMap.get(beanName);
    }

    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
        this.beanPostProcessors.add(beanPostProcessor);
    }

    public Set<String> getRegisteredSingletons() {
        return registeredSingletons;
    }
}
