# SpringBoot源码

## 依赖管理

在使用`SpringBoot`开发项目时，我们发现无需进行`SpringBoot`相关依赖的版本设置，就可以直接使用，且无需关注版本冲突问题

这得益于`SpringBoot`的两个核心依赖包

- `spring-boot-starter-parent`
- `spring-boot-starter-web`

### spring-boot-starter-parent

我们在构建`SpringBoot`项目时，必须在`pom.xml`中添加`spring-boot-starter-parent`的父依赖

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.2.9.RELEASE</version>
    <relativePath/> <!-- lookup parent from repository -->
</parent>
```

我们可以通过`Alt + 左键`点击进入`spring-boot-starter-parent`的`pom.xml`文件中，其中通过不同的配置实现功能



#### 第一部分：properties节点

```xml
<properties>
    <main.basedir>${basedir}/../../..</main.basedir>
    <!-- 1.java版本-->
    <java.version>1.8</java.version>
    <resource.delimiter>@</resource.delimiter> <!-- delimiter that doesn't clash with Spring ${} placeholders -->
    <!-- 2.工程代码的编译源文件编码格式-->
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <!-- 3.工程编译后的文件编码格式-->
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    <!-- 4.Maven打包编译的版本-->
    <maven.compiler.source>${java.version}</maven.compiler.source>
    <maven.compiler.target>${java.version}</maven.compiler.target>
</properties>
```



#### 第二部分：build节点

`build`节点中的`resources`节点

```xml
<resources>
    <resource>
        <directory>${basedir}/src/main/resources</directory>
        <filtering>true</filtering>
        <includes>
            <include>**/application*.yml</include>
            <include>**/application*.yaml</include>
            <include>**/application*.properties</include>
        </includes>
    </resource>
    <resource>
        <directory>${basedir}/src/main/resources</directory>
        <excludes>
            <exclude>**/application*.yml</exclude>
            <exclude>**/application*.yaml</exclude>
            <exclude>**/application*.properties</exclude>
        </excludes>
    </resource>
</resources>
```

`resources`节点，定义了资源过滤，针对于`application`的`yml/yaml/properties`，可以支持环境配置，比如`application-dev.yml`



`build`节点中的`pluginManagement`节点

- 引入了插件
- 对插件的版本依赖进行控制



#### 第三部分：父依赖spring-boot-dependencies

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-dependencies</artifactId>
    <version>${revision}</version>
    <relativePath>../../spring-boot-dependencies</relativePath>
</parent>
```



### spring-boot-dependencies

`spring-boot-dependencies`是`spring-boot-starter-parent`的父依赖，它主要用于依赖的版本控制



`spring-boot-dependencies`主要通过两个节点实现版本控制

- `properties`节点
  - 定义了大量版本信息的属性
- `dependencyManagement`节点
  - 基于`Maven`知识，实现依赖的版本控制



### 小结

`SpringBoot`中通过父依赖`spring-boot-starter-parent、spring-boot-dependencies`来实现对于不同版本的`SpringBoot`项目的对于插件、依赖的管理控制



## SpringBoot主配置类解析

`SpringBoot`通常存在一个主配置类，它一般具有固定格式

```java
//标识主配置类
@SpringBootApplication
public class SpringBootMytestApplication {

	public static void main(String[] args) {
		// 调用静态方法SpringApplication#run，并传入当前主配置类作为参数
		SpringApplication.run(SpringBootMytestApplication.class, args);
	}

}
```



由上述可知，主配置类有一下特点：

- 通过`@SpringBootApplication`注解标志为主配置类
- 调用静态方法`SpringApplication#run`，并传入当前主配置类作为参数

我们逐个分析，下面我们先来分析一下`@SpringBootApplication`注解



## SpringBoot主配置类注解解析

### @SpringBootApplication

`@SpringBootApplication`是标志`SpringBoot`主配置类的注解

```java
// org.springframework.boot.autoconfigure.SpringBootApplication
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootConfiguration	//标注类为配置类
@EnableAutoConfiguration	//标注自动配置功能
//注解扫描
@ComponentScan(excludeFilters = { @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
		@Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })
public @interface SpringBootApplication {
    
    /* 需要进行组件扫描的包 */
	@AliasFor(annotation = ComponentScan.class, attribute = "basePackages")
	String[] scanBasePackages() default {};

	/* 需要进行组件扫描的配置类Class */
	@AliasFor(annotation = ComponentScan.class, attribute = "basePackageClasses")
	Class<?>[] scanBasePackageClasses() default {};


	@AliasFor(annotation = EnableAutoConfiguration.class)
	Class<?>[] exclude() default {};

	@AliasFor(annotation = EnableAutoConfiguration.class)
	String[] excludeName() default {};

	@AliasFor(annotation = Configuration.class)
	boolean proxyBeanMethods() default true;
    
}
```



我们发现`@SpringBootApplication`实际主要集成了三个注解

- `@SpringBootConfiguration`
- `@EnableAutoConfiguration`
- `@ComponentScan`

下面我们一一分析



### @SpringBootConfiguration

```java
// org.springframework.boot.SpringBootConfiguration
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Configuration
public @interface SpringBootConfiguration {

	@AliasFor(annotation = Configuration.class)
	boolean proxyBeanMethods() default true;

}
```

我们可以发现`@SpringBootConfiguration`主要就是集成了`@Configuration`，标志此类为一个`full`模式配置类

是一个`@Configuration`注解的变种，主要用于标志`SpringBoot`项目主配置类



### @EnableAutoConfiguration

```java
// org.springframework.boot.autoconfigure.EnableAutoConfiguration
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@AutoConfigurationPackage
@Import(AutoConfigurationImportSelector.class)
public @interface EnableAutoConfiguration {

	String ENABLED_OVERRIDE_PROPERTY = "spring.boot.enableautoconfiguration";

	Class<?>[] exclude() default {};

	String[] excludeName() default {};

}
```

`@EnableAutoConfiguration`主要功能

- 集成了`@AutoConfigurationPackage`注解
- 通过`@Import`注解引入了`AutoConfigurationImportSelector`类



### AutoConfigurationImportSelector

`AutoConfigurationImportSelector`由`@EnableAutoConfiguration`注解通过`@Import`注解引入

`AutoConfigurationImportSelector`是一个`DeferredImportSelector`实现类，由Spring源码分析流程，我们进行分析

```java
public class AutoConfigurationImportSelector implements DeferredImportSelector, BeanClassLoaderAware,
ResourceLoaderAware, BeanFactoryAware, EnvironmentAware, Ordered {
    
    /* 实现自DeferredImportSelector接口 */
	public Class<? extends Group> getImportGroup() {
		return AutoConfigurationGroup.class;
	}
}
```

由实现的`getImportGroup`接口可知，进行具体工作的是`AutoConfigurationGroup`，我们继续分析



#### AutoConfigurationGroup

```java
// org.springframework.boot.autoconfigure.AutoConfigurationImportSelector.AutoConfigurationGroup
private static class AutoConfigurationGroup
    implements DeferredImportSelector.Group, BeanClassLoaderAware, BeanFactoryAware, ResourceLoaderAware {
    
    // 保存配置类，key：引入的配置类全限定类名，value：主配置类注解元数据
    private final Map<String, AnnotationMetadata> entries = new LinkedHashMap<>();

    // 保存封装的AutoConfigurationEntry实例
    private final List<AutoConfigurationEntry> autoConfigurationEntries = new ArrayList<>();
    
}
```

`AutoConfigurationGroup`是`AutoConfigurationImportSelector`的静态内部类，实现了`DeferredImportSelector.Group`接口

所以我们要先分析其`process`方法



##### process

```java
// org.springframework.boot.autoconfigure.AutoConfigurationImportSelector.AutoConfigurationGroup#process
public void process(AnnotationMetadata annotationMetadata, DeferredImportSelector deferredImportSelector) {
    //当前Group只处理AutoConfigurationImportSelector类的DeferredImportSelector
    Assert.state(deferredImportSelector instanceof AutoConfigurationImportSelector,
                 () -> String.format("Only %s implementations are supported, got %s",
                                     AutoConfigurationImportSelector.class.getSimpleName(),
                                     deferredImportSelector.getClass().getName()));

    // 1. 获取自动配置类保存到AutoConfigurationEntry对象中
    AutoConfigurationEntry autoConfigurationEntry = ((AutoConfigurationImportSelector) deferredImportSelector)
        .getAutoConfigurationEntry(getAutoConfigurationMetadata(), annotationMetadata);

    // 2. 将autoConfigurationEntry封装到autoConfigurationEntries中
    this.autoConfigurationEntries.add(autoConfigurationEntry);
    // 3. 将获取的导入配置类保存到entries中
    for (String importClassName : autoConfigurationEntry.getConfigurations()) {
        this.entries.putIfAbsent(importClassName, annotationMetadata);
    }
}
```

由上述代码分析，我们主要**关注**如何通过`AutoConfigurationImportSelector#getAutoConfigurationEntry`方法获取自动配置类



##### getAutoConfigurationEntry

```java
// org.springframework.boot.autoconfigure.AutoConfigurationImportSelector#getAutoConfigurationEntry
protected AutoConfigurationEntry getAutoConfigurationEntry(AutoConfigurationMetadata autoConfigurationMetadata,
                                                           AnnotationMetadata annotationMetadata) {
    
    // 1. 获取@EnableAutoConfiguration注解的注解信息，exclude、excludeName
    AnnotationAttributes attributes = getAttributes(annotationMetadata);

    // 2. 获取spring.factories文件中对应于EnableAutoConfiguration类为key的所有自动配置类
    List<String> configurations = getCandidateConfigurations(annotationMetadata, attributes);

    // 3. 利用LinkedHashSet过滤重复配置
    configurations = removeDuplicates(configurations);

    // 4. 得到要排除的自动配置类，比如注解属性exclude的配置类
    Set<String> exclusions = getExclusions(annotationMetadata, attributes);
    checkExcludedClasses(configurations, exclusions);
    configurations.removeAll(exclusions);
    configurations = filter(configurations, autoConfigurationMetadata);
    fireAutoConfigurationImportEvents(configurations, exclusions);
    return new AutoConfigurationEntry(configurations, exclusions);
}
```



分析各部分处理

###### getAttributes 

```java
// org.springframework.boot.autoconfigure.AutoConfigurationImportSelector#getAttributes
protected AnnotationAttributes getAttributes(AnnotationMetadata metadata) {
    // 1.获取配置注解的className，即EnableAutoConfiguration
    String name = getAnnotationClass().getName();
    // 2.从注解中提取对应@EnableAutoConfiguration注解的属性
    AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(name, true));
    // 3.响应
    return attributes;
}

// 标志自动配置的注解类
protected Class<?> getAnnotationClass() {
    return EnableAutoConfiguration.class;
}
```

`getAttributes`方法主要用于获取主配置类注解元数据中对应于 `@EnableAutoConfiguration`注解的属性

- `exclude`
- `excludeName`



###### getCandidateConfigurations

```java
// org.springframework.boot.autoconfigure.AutoConfigurationImportSelector#getCandidateConfigurations
protected List<String> getCandidateConfigurations(AnnotationMetadata metadata, AnnotationAttributes attributes) {
    //获取文件 META-INF/spring.factories 中以 org.springframework.boot.autoconfigure.EnableAutoConfiguration 为key的所有配置类的集合
    List<String> configurations = SpringFactoriesLoader.loadFactoryNames(getSpringFactoriesLoaderFactoryClass(),
                                                                         getBeanClassLoader());
    return configurations;
}

// org.springframework.core.io.support.SpringFactoriesLoader
public final class SpringFactoriesLoader {
    
    // SpringBoot自动配置的配置文件
    public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring.factories";
    
    /* 加载自动配置的Class集合 */
    public static List<String> loadFactoryNames(Class<?> factoryType, @Nullable ClassLoader classLoader) {
        // spring.factories文件中的key值，此处是@EnableAutoConfiguration注解的全限定类名
        // org.springframework.boot.autoconfigure.EnableAutoConfiguration
        String factoryTypeName = factoryType.getName();
        // 1.通过loadSpringFactories方法，加载所有 META-INF目录下spring.factories配置文件中信息，并组装为map集合
        // 2.获取目标factoryTypeName对应的key的配置类集合
        return (List)loadSpringFactories(classLoader).getOrDefault(factoryTypeName, Collections.emptyList());
    }
}
```



此处需要解释一下`spring.factories`文件，`spring.factories`文件主要用于自动配置使用，其内容格式如下

![image-20210812214601497](D:\学习整理\summarize\springboot\图片\spring_factories文件格式展示)

对应于`SpringBoot`项目，其中存在多个`spring.factories`文件，而对应于`EnableAutoConfiguration`配置的自动装配的配置，在：

`spring-boot-2.2.9\spring-boot-project\spring-boot-autoconfigure\src\main\resources\META-INF\spring.factories`中



`SpringFactoriesLoader`类是专门用于加载项目中的`spring.factories`文件的，其`loadFactoryNames`方法将加载所有`spring.factories`文件的内容，并将其以`key-value`的形式封装为`Map<String, List<String>>`集合

`SpringBoot`项目中对应于自动装配的`key`就是对应于`@EnableAutoConfiguration`的全限定类名



**小结**

我们通过`getCandidateConfigurations`方法，从`spring.factories`文件中提取了我们想要的自动装配的配置类的集合

再通过`removeDuplicates`方法进行去重，实际就是借用了`Set`集合



###### 剔除无用配置类

我们前面通过`getCandidateConfigurations`方法获取了配置在`spring.factories`文件中的自动装配配置类集合，但是对于不同项目情景，不是所有的配置类都是必须注册到IOC容器中进行工作的，所以我们要对其进行过滤处理，剔除无用的配置类

```java
protected AutoConfigurationEntry getAutoConfigurationEntry(AutoConfigurationMetadata autoConfigurationMetadata,
                                                           AnnotationMetadata annotationMetadata) {
    // 剔除@EnableAutoConfiguration注解配置的剔除类和spring.autoconfigure.exclude配置的剔除类
    Set<String> exclusions = getExclusions(annotationMetadata, attributes);
    // 检查需要排除的类的配置是否正确
    checkExcludedClasses(configurations, exclusions);
    // 将所有要排除的配置类移除
    configurations.removeAll(exclusions);
    // 不是所有配置类都需要加载，所以需要过滤，此处通过Condition系列注解进行过滤
    configurations = filter(configurations, autoConfigurationMetadata); 
}
```



- 剔除@EnableAutoConfiguration注解配置的剔除类
- 剔除由属性`spring.autoconfigure.exclude`配置的剔除类

```java
protected Set<String> getExclusions(AnnotationMetadata metadata, AnnotationAttributes attributes) {
    Set<String> excluded = new LinkedHashSet<>();
    // 剔除@EnableAutoConfiguration注解的exclude属性配置
    excluded.addAll(asList(attributes, "exclude"));
    // 剔除@EnableAutoConfiguration注解的excludeName属性配置
    excluded.addAll(Arrays.asList(attributes.getStringArray("excludeName")));
    // 剔除PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE：spring.autoconfigure.exclude 属性配置剔除类
    excluded.addAll(getExcludeAutoConfigurationsProperty());
    return excluded;
}

private List<String> getExcludeAutoConfigurationsProperty() {
    if (getEnvironment() instanceof ConfigurableEnvironment) {
        Binder binder = Binder.get(getEnvironment());
        return binder.bind(PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE, String[].class).map(Arrays::asList)
            .orElse(Collections.emptyList());
    }
    String[] excludes = getEnvironment().getProperty(PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE, String[].class);
    return (excludes != null) ? Arrays.asList(excludes) : Collections.emptyList();
}
```



- 检查需要排除的类的配置是否正确

```java
// org.springframework.boot.autoconfigure.AutoConfigurationImportSelector#checkExcludedClasses
private void checkExcludedClasses(List<String> configurations, Set<String> exclusions) {
    List<String> invalidExcludes = new ArrayList<>(exclusions.size());
    for (String exclusion : exclusions) {
        // 获取非配置类的排除类
        if (ClassUtils.isPresent(exclusion, getClass().getClassLoader()) && !configurations.contains(exclusion)) {
            invalidExcludes.add(exclusion);
        }
    }
    if (!invalidExcludes.isEmpty()) {
        // 如果存在非配置类的排除类，则报错
        handleInvalidExcludes(invalidExcludes);
    }
}

protected void handleInvalidExcludes(List<String> invalidExcludes) {
    StringBuilder message = new StringBuilder();
    for (String exclude : invalidExcludes) {
        message.append("\t- ").append(exclude).append(String.format("%n"));
    }
    throw new IllegalStateException(String.format(
        "The following classes could not be excluded because they are not auto-configuration classes:%n%s",
        message));
}
```



- 通过`Condition`系列注解进行配置类的过滤，剔除无需加载的配置类

```java
// org.springframework.boot.autoconfigure.AutoConfigurationImportSelector#filter
private List<String> filter(List<String> configurations, AutoConfigurationMetadata autoConfigurationMetadata) {
    long startTime = System.nanoTime();
    // 将配置类集合转为数组
    String[] candidates = StringUtils.toStringArray(configurations);
    // 定义skip数组，是否需要跳过。注意skip数组与candidates数组顺序一一对应
    boolean[] skip = new boolean[candidates.length];
    boolean skipped = false;
    // 通过getAutoConfigurationImportFilters方法从spring.factories文件中获取对应AutoConfigurationImportFilter
    for (AutoConfigurationImportFilter filter : getAutoConfigurationImportFilters()) {
        // 通过Aware方法，将beanClassLoader,beanFactory等注入到filter对象中
        invokeAwareMethods(filter);
        // 通过match方法获取匹配的配置类
        boolean[] match = filter.match(candidates, autoConfigurationMetadata);
        for (int i = 0; i < match.length; i++) {
            // 如果有不匹配的
            if (!match[i]) {
                // 将不匹配的配置类对应skip数组的位置设置为true
                skip[i] = true;
                // 将配置类数组中元素置空
                candidates[i] = null;
                // 将skipped标志置为true
                skipped = true;
            }
        }
    }
    // 如果没有不匹配的，则返回配置类集合
    if (!skipped) {
        return configurations;
    }
    // 将过滤后的配置类组装为数据返回
    List<String> result = new ArrayList<>(candidates.length);
    for (int i = 0; i < candidates.length; i++) {
        if (!skip[i]) {
            result.add(candidates[i]);
        }
    }
    if (logger.isTraceEnabled()) {
        int numberFiltered = configurations.size() - result.size();
        logger.trace("Filtered " + numberFiltered + " auto configuration class in "
                     + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) + " ms");
    }
    return new ArrayList<>(result);
}


protected List<AutoConfigurationImportFilter> getAutoConfigurationImportFilters() {
    // 从spring.factories文件中获取对应org.springframework.boot.autoconfigure.AutoConfigurationImportFilter的配置
    // org.springframework.boot.autoconfigure.condition.OnBeanCondition
    // org.springframework.boot.autoconfigure.condition.OnClassCondition
    // org.springframework.boot.autoconfigure.condition.OnWebApplicationCondition
    return SpringFactoriesLoader.loadFactories(AutoConfigurationImportFilter.class, this.beanClassLoader);
}
```

由上述代码可知，实质是通过从`spring.factories`文件中获取的三个实现了`AutoConfigurationImportFilter`的类对配置类进行过滤

- `OnBeanCondition`
- `OnClassCondition`
- `OnWebApplicationCondition`

具体判断实际是判断配置类上对应的条件注解(如果存在)

- `@ConditionalOnClass`
- `@ConditionalOnBean`
- `@ConditionalOnWebApplication`

如果满足，则返回true，表示匹配成功，否则匹配失败



###### 关于条件注解的讲解

`@Conditional`是Spring4新提供的注解，它的作用是按照一定的条件进行判断，满足条件给容器注册 bean

- @ConditionalOnBean：仅仅在当前上下文中存在某个对象时，才会实例化一个Bean。 
- @ConditionalOnClass：某个class位于类路径上，才会实例化一个Bean。 
- @ConditionalOnExpression：当表达式为true的时候，才会实例化一个Bean。基于SpEL表达式 的条件判断。 
- @ConditionalOnMissingBean：仅仅在当前上下文中不存在某个对象时，才会实例化一个Bean。 
- @ConditionalOnMissingClass：某个class类路径上不存在的时候，才会实例化一个Bean。
-  @ConditionalOnNotWebApplication：不是web应用，才会实例化一个Bean。 
- @ConditionalOnWebApplication：当项目是一个Web项目时进行实例化。
-  @ConditionalOnNotWebApplication：当项目不是一个Web项目时进行实例化。 
- @ConditionalOnProperty：当指定的属性有指定的值时进行实例化。 
- @ConditionalOnJava：当JVM版本为指定的版本范围时触发实例化。 
- @ConditionalOnResource：当类路径下有指定的资源时触发实例化。 
- @ConditionalOnJndi：在JNDI存在的条件下触发实例化。 
- @ConditionalOnSingleCandidate：当指定的Bean在容器中只有一个，或者有多个但是指定了首 选的Bean时触发实例化。



###### fireAutoConfigurationImportEvents

```java
private void fireAutoConfigurationImportEvents(List<String> configurations, Set<String> exclusions) {
    // 从spring.factories文件中获取配置的AutoConfigurationImportListener
    List<AutoConfigurationImportListener> listeners = getAutoConfigurationImportListeners();
    if (!listeners.isEmpty()) {
        AutoConfigurationImportEvent event = new AutoConfigurationImportEvent(this, configurations, exclusions);
        for (AutoConfigurationImportListener listener : listeners) {
            // 通过Aware注入beanClassLoader、beanFactory等
            invokeAwareMethods(listener);
            // 目的是告诉ConditionEvaluationReport条件评估报告器对象来记录符合条件的自动配置类
            // 该事件什么时候会被触发？--> 在刷新容器时调用invokeBeanFactoryPostProcessors后置处理器时触发
            listener.onAutoConfigurationImportEvent(event);
        }
    }
}

protected List<AutoConfigurationImportListener> getAutoConfigurationImportListeners() {
    // 从spring.factories文件中获取对应org.springframework.boot.autoconfigure.AutoConfigurationImportListener的配置
    // org.springframework.boot.autoconfigure.condition.ConditionEvaluationReportAutoConfigurationImportListener
    return SpringFactoriesLoader.loadFactories(AutoConfigurationImportListener.class, this.beanClassLoader);
}
```



###### 小结

由上述分析，`process`方法主要是从`spring.factories`文件中获取自动配置类，并对其进行一系列剔除、过滤动作，保留所需的自动配置类



##### selectImports

```java
// org.springframework.boot.autoconfigure.AutoConfigurationImportSelector.AutoConfigurationGroup#selectImports
public Iterable<Entry> selectImports() {
    if (this.autoConfigurationEntries.isEmpty()) {
        return Collections.emptyList();
    }
    // 获取所有的排除类
    Set<String> allExclusions = this.autoConfigurationEntries.stream()
        .map(AutoConfigurationEntry::getExclusions).flatMap(Collection::stream).collect(Collectors.toSet());
    // 获取所有的配置类
    Set<String> processedConfigurations = this.autoConfigurationEntries.stream()
        .map(AutoConfigurationEntry::getConfigurations).flatMap(Collection::stream)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    // 剔除动作
    processedConfigurations.removeAll(allExclusions);

    // 通过@Order注解对自动配置类进行排序，返回最终的自动配置类
    return sortAutoConfigurations(processedConfigurations, getAutoConfigurationMetadata()).stream()
        .map((importClassName) -> new Entry(this.entries.get(importClassName), importClassName))
        .collect(Collectors.toList());
}
```



#### 总结

`@EnableAutoConfiguration`注解通过`@Import`导入`AutoConfigurationImportSelector`类，实现了`SpringBoot`项目的自动配置功能，其实现原理

- 从`spring.factories`配置文件中加载自动配置类
  - 对应key：`org.springframework.boot.autoconfigure.EnableAutoConfiguration`
- 通过`@EnableAutoConfiguration`注解的`exclude`属性进行自动配置类的剔除
  - 或者是`spring.autoconfigure.exclude`属性配置
- 从`spring.factories`配置文件中获取`AutoConfigurationImportFilter`对自动配置类进行过滤
  - 对应key：`org.springframework.boot.autoconfigure.AutoConfigurationImportFilter`
  - 通过`@ConditionalOnBean、@ConditionalOnClass、@ConditionalOnWebApplication`注解进行过滤
- 从`spring.factories`配置文件中获取`AutoConfigurationImportListener`触发 `AutoConfigurationImportEvent` 事件，告诉 `ConditionEvaluationReport` 条件评 估报告器对象来分别记录符合条件和 `exclude` 的自动配置类
  - 对应key：`org.springframework.boot.autoconfigure.AutoConfigurationImportListener`

- 将最终得到的自动配置类注册到IOC容器中



### @AutoConfigurationPackage

```java
// org.springframework.boot.autoconfigure.AutoConfigurationPackage
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(AutoConfigurationPackages.Registrar.class)
public @interface AutoConfigurationPackage {

}
```

`@AutoConfigurationPackage`主要就是通过`@Import`注解引入了`AutoConfigurationPackages.Registrar`内部类



### AutoConfigurationPackages.Registrar

```java
// org.springframework.boot.autoconfigure.AutoConfigurationPackages.Registrar
static class Registrar implements ImportBeanDefinitionRegistrar, DeterminableImports {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        register(registry, new PackageImport(metadata).getPackageName());
    }

    @Override
    public Set<Object> determineImports(AnnotationMetadata metadata) {
        return Collections.singleton(new PackageImport(metadata));
    }

}
```

可知，`AutoConfigurationPackages.Registrar`实现了`ImportBeanDefinitionRegistrar`接口，所以我们需要分析其`registerBeanDefinitions`注册了什么`bean`到IOC容器中



在`registerBeanDefinitions`方法中，存在一段代码`new PackageImport(metadata).getPackageName()`

由于`@AutoConfigurationPackage`注解在主配置类上，而`AutoConfigurationPackages.Registrar`由`@AutoConfigurationPackage`注解导入

所以获取的`PackageName`就是主配置类所在的包



下面继续分析`register`方法

```java
public static void register(BeanDefinitionRegistry registry, String... packageNames) {
    // 判断IOC容器中是否已存在AutoConfigurationPackages.class为key的对象
    if (registry.containsBeanDefinition(BEAN)) {
        BeanDefinition beanDefinition = registry.getBeanDefinition(BEAN);
        ConstructorArgumentValues constructorArguments = beanDefinition.getConstructorArgumentValues();
        constructorArguments.addIndexedArgumentValue(0, addBasePackages(constructorArguments, packageNames));
    }
    else {
        // 创建一个BeanDefinition
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        // 设置其beanClass为org.springframework.boot.autoconfigure.AutoConfigurationPackages.BasePackages
        beanDefinition.setBeanClass(BasePackages.class);
        beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, packageNames);
        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        // 已AutoConfigurationPackages.class为key，将BeanDefinition注册到IOC容器
        registry.registerBeanDefinition(BEAN, beanDefinition);
    }
}

private static String[] addBasePackages(ConstructorArgumentValues constructorArguments, String[] packageNames) {
    String[] existing = (String[]) constructorArguments.getIndexedArgumentValue(0, String[].class).getValue();
    Set<String> merged = new LinkedHashSet<>();
    merged.addAll(Arrays.asList(existing));
    merged.addAll(Arrays.asList(packageNames));
    return StringUtils.toStringArray(merged);
}
```



由上述代码可知，`register`方法主要就是向IOC容器中注册`org.springframework.boot.autoconfigure.AutoConfigurationPackages.BasePackages`类型的`BeanDefinition`，并且将主配置类所在包设置到其属性中，那么我们应该分析一下`BasePackages`这个内部类

```java
static final class BasePackages {
    // 保存基础包
    private final List<String> packages;
    
    List<String> get() {
        return this.packages;
    }
}
```



可知`BasePackages`主要就是保存主配置类所在的基础包，供后续使用

它主要用于提供了 `@AutoConfigurationPackage` 这个注解的类所在的包路径，保存自动配置 类以供之后的使用，比如给 JPA entity 扫描器用来扫描开发人员通过注解 `@Entity` 定义的 `entity` 类



### @ComponentScan

`@ComponentScan`注解不用再细说了，主要就是进行组件扫描

由于其没有配置对应`basePackages`等包信息，将取其注解所在类所在的包，所以主配置类通常在业务包的最外层



### 总结

对于`@SpringBootApplication`注解的分析

- `@SpringBootConfiguration`
  - 仅标注此类为配置类
- `@ComponentScan`
  - 实现组件自动扫描
- `@EnableAutoConfiguration`
  - `@AutoConfigurationPackage`
    - 主要注册一个`org.springframework.boot.autoconfigure.AutoConfigurationPackages.BasePackages`，并保存主配置类所在基础包
  - `AutoConfigurationImportSelector`
    - 实现`SpringBoot`自动配置的导入选择器



## SpringBoot主配置类run方法解析

前面分析了SpringBoot主配置类通过`@SpringBootApplication`注解提供的功能，但是它只是一个注解，并不能提供实质的功能，需要解析处理

下面我们来分析主配置类的第二个部件：`SpringApplication#run`



```java
// org.springframework.boot.SpringApplication#run(java.lang.Class<?>, java.lang.String...)
public static ConfigurableApplicationContext run(Class<?> primarySource, String... args) {
    return run(new Class<?>[] { primarySource }, args);
}

public static ConfigurableApplicationContext run(Class<?>[] primarySources, String[] args) {
    return new SpringApplication(primarySources).run(args);
}
```



由上述代码可知，在`run`方法中有两个主要部分

- `SpringApplication`构造
- 重载的`run`方法调用

下面我们一一分析



### SpringApplication构造

我们具体分析一下`SpringApplication`

```java
public class SpringApplication {
    // 资源加载器
    private ResourceLoader resourceLoader;
    
    // 主要资源Class，一般为主配置类
    private Set<Class<?>> primarySources;
    
    // application容器类型
    private WebApplicationType webApplicationType;
    
    // application容器刷新动作前的初始化器
    private List<ApplicationContextInitializer<?>> initializers;
    
    // application容器的监听器
    private List<ApplicationListener<?>> listeners;
    
    // main 方法的类名
    private Class<?> mainApplicationClass;
    
    
    public SpringApplication(Class<?>... primarySources) {
		this(null, primarySources);
	}
    
    public SpringApplication(ResourceLoader resourceLoader, Class<?>... primarySources) {
		// 将resourceLoader置空
		this.resourceLoader = resourceLoader;
		// 必须传入主配置类Class
		Assert.notNull(primarySources, "PrimarySources must not be null");
		// 将传入的主配置类资源通过Set集合去重，保存到primarySources属性中
		this.primarySources = new LinkedHashSet<>(Arrays.asList(primarySources));
		// 推断应用的类型，后面通过类型初始化对应环境
		this.webApplicationType = WebApplicationType.deduceFromClasspath();
		// 从spring.factories配置文件中获取ApplicationContextInitializer、ApplicationListener对应配置，设置到属性中
		setInitializers((Collection) getSpringFactoriesInstances(ApplicationContextInitializer.class));
		setListeners((Collection) getSpringFactoriesInstances(ApplicationListener.class));
		// 根据调用栈，推断出 main 方法的所在类名
		this.mainApplicationClass = deduceMainApplicationClass();
	}
}
```



由上述代码，我们主要需要分析的方法有

- `WebApplicationType#deduceFromClasspath`
- `getSpringFactoriesInstances`
- `deduceMainApplicationClass`
  - 获取程序入口`main`方法所在的类名



#### deduceFromClasspath

`deduceFromClasspath`方法主要用于推断应用的类型，用于后续的环境初始化

```java
public enum WebApplicationType {
    private static final String[] SERVLET_INDICATOR_CLASSES = { "javax.servlet.Servlet",
                                   "org.springframework.web.context.ConfigurableWebApplicationContext" };

    private static final String WEBMVC_INDICATOR_CLASS = "org.springframework.web.servlet.DispatcherServlet";

    private static final String WEBFLUX_INDICATOR_CLASS = "org.springframework.web.reactive.DispatcherHandler";

    private static final String JERSEY_INDICATOR_CLASS = "org.glassfish.jersey.servlet.ServletContainer";

    private static final String SERVLET_APPLICATION_CONTEXT_CLASS = "org.springframework.web.context.WebApplicationContext";

    private static final String REACTIVE_APPLICATION_CONTEXT_CLASS = "org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext";

    static WebApplicationType deduceFromClasspath() {
        // 存在DispatcherHandler，不存在DispatcherServlet、ServletContainer，为REACTIVE类型
        if (ClassUtils.isPresent(WEBFLUX_INDICATOR_CLASS, null)
            && !ClassUtils.isPresent(WEBMVC_INDICATOR_CLASS, null)
            && !ClassUtils.isPresent(JERSEY_INDICATOR_CLASS, null)) {
            return WebApplicationType.REACTIVE;
        }
        // SERVLET_INDICATOR_CLASSES中有一不存在，为NONE类
        for (String className : SERVLET_INDICATOR_CLASSES) {
            if (!ClassUtils.isPresent(className, null)) {
                return WebApplicationType.NONE;
            }
        }
        // 默认为SERVLET
        return WebApplicationType.SERVLET;
    }
}
```

判断解析

- `REACTIVE`
  - 存在`DispatcherHandler`
  - 不存在`DispatcherServlet、ServletContainer`
- `NONE`
  - `Servlet、ConfigurableWebApplicationContext`必须全部存在，否则为`NONE`
- 默认为`SERVLET`

通常为`SERVLET`类型



#### getSpringFactoriesInstances

```java
// org.springframework.boot.SpringApplication#getSpringFactoriesInstances
private <T> Collection<T> getSpringFactoriesInstances(Class<T> type) {
    return getSpringFactoriesInstances(type, new Class<?>[] {});
}

private <T> Collection<T> getSpringFactoriesInstances(Class<T> type, Class<?>[] parameterTypes, Object... args) {
    ClassLoader classLoader = getClassLoader();
    // 通过SpringFactoriesLoader#loadFactoryNames从spring.factories配置文件中获取传入type对应配置类
    Set<String> names = new LinkedHashSet<>(SpringFactoriesLoader.loadFactoryNames(type, classLoader));
    // 通过createSpringFactoriesInstances将获取的配置类实例化
    List<T> instances = createSpringFactoriesInstances(type, parameterTypes, classLoader, args, names);
    // 通过@Order注解对配置类排序
    AnnotationAwareOrderComparator.sort(instances);
    return instances;
}

private <T> List<T> createSpringFactoriesInstances(Class<T> type, Class<?>[] parameterTypes,
                                                   ClassLoader classLoader, Object[] args, Set<String> names) {
    List<T> instances = new ArrayList<>(names.size());
    for (String name : names) {
        try {
            Class<?> instanceClass = ClassUtils.forName(name, classLoader);
            Assert.isAssignable(type, instanceClass);
            // 获取构造器
            Constructor<?> constructor = instanceClass.getDeclaredConstructor(parameterTypes);
            // 通过构造器进行实例化
            T instance = (T) BeanUtils.instantiateClass(constructor, args);
            instances.add(instance);
        }
        catch (Throwable ex) {
            throw new IllegalArgumentException("Cannot instantiate " + type + " : " + name, ex);
        }
    }
    return instances;
}
```



由上述代码可知`getSpringFactoriesInstances`方法作用

- 从`spring.factories`文件中获取传入的`type`对应的配置类
- 将对应配置类实例化
- 将实例化后的配置类通过`@Order`排序



在`SpringApplication`构造方法中，通过`getSpringFactoriesInstances`方法分别获取了

- `ApplicationContextInitializer`
- `ApplicationListener`

对应配置类，并将其实例化后保存到

- `SpringApplication#initializers`
- `SpringApplication#listeners`

中



#### 小结

`SpringApplication`构造方法中，主要初始化了一些数据，供后续`run`方法执行时使用



### run方法

```java
// org.springframework.boot.SpringApplication#run
public ConfigurableApplicationContext run(String... args) {
    // 记录程序运行时间
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    ConfigurableApplicationContext context = null;
    Collection<SpringBootExceptionReporter> exceptionReporters = new ArrayList<>();
    configureHeadlessProperty();

    // 1.从spring.factories配置文件中获取SpringApplicationRunListener.class对应配置类，并将其实例化
    // 封装为一个SpringApplicationRunListeners实例对象
    SpringApplicationRunListeners listeners = getRunListeners(args);
    // 启动监听
    listeners.starting();
    try {
        ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);
        // 2. 构造应用上下文环境
        ConfigurableEnvironment environment = prepareEnvironment(listeners, applicationArguments);
        // 处理需要忽略的Bean
        configureIgnoreBeanInfo(environment);
        // 打印banner
        Banner printedBanner = printBanner(environment);
        // 3. 根据前面获取的容器类型，创建application容器
        context = createApplicationContext();
        // 从spring.factories配置文件中获取SpringBootExceptionReporter.class对应配置，并实例化
        exceptionReporters = getSpringFactoriesInstances(SpringBootExceptionReporter.class,
                                                         new Class[] { ConfigurableApplicationContext.class }, context);
        // 4、刷新应用上下文前的准备阶段
        prepareContext(context, environment, listeners, applicationArguments, printedBanner);
        // 5. 刷新上下文
        refreshContext(context);
        // 6. 刷新应用上下文后的扩展接口
        afterRefresh(context, applicationArguments);
        // 时间记录停止
        stopWatch.stop();
        if (this.logStartupInfo) {
            new StartupInfoLogger(this.mainApplicationClass).logStarted(getApplicationLog(), stopWatch);
        }
        // 发布容器启动完成事件
        listeners.started(context);
        callRunners(context, applicationArguments);
    }
    catch (Throwable ex) {
        handleRunFailure(context, ex, exceptionReporters, listeners);
        throw new IllegalStateException(ex);
    }

    try {
        listeners.running(context);
    }
    catch (Throwable ex) {
        handleRunFailure(context, ex, exceptionReporters, null);
        throw new IllegalStateException(ex);
    }
    return context;
}
```



分析上述代码，`run`方法调用过程中有六个重要部分

- 第一步：获取并启动监听器 
- 第二步：构造应用上下文环境 
- 第三步：初始化应用上下文 
- 第四步：刷新应用上下文前的准备阶段 
- 第五步：刷新应用上下文 
- 第六步：刷新应用上下文后的扩展接口



#### 第一步：获取并启动监听器 

```java
// 1.从spring.factories配置文件中获取SpringApplicationRunListener.class对应配置类，并将其实例化
// 封装为一个SpringApplicationRunListeners实例对象
SpringApplicationRunListeners listeners = getRunListeners(args);
// 启动监听
listeners.starting();
```



`getRunListeners`方法获取监听器，并封装为`SpringApplicationRunListeners`实例

`SpringApplicationRunListeners#starting`启动监听

```java
private SpringApplicationRunListeners getRunListeners(String[] args) {
    Class<?>[] types = new Class<?>[] { SpringApplication.class, String[].class };
    // 通过getSpringFactoriesInstances方法从spring.factories配置文件中获取SpringApplicationRunListener.class对应配置类
    // 封装为SpringApplicationRunListeners实例
    // SpringApplicationRunListener负责在SpringBoot启动的不同阶段，广播不同消息，传递给ApplicationListener监听器
    return new SpringApplicationRunListeners(logger,
         getSpringFactoriesInstances(SpringApplicationRunListener.class, types, this, args));
}

// org.springframework.boot.SpringApplicationRunListeners
class SpringApplicationRunListeners {
   	// 上下文运行监听器
    private final List<SpringApplicationRunListener> listeners;
    
    SpringApplicationRunListeners(Log log, Collection<? extends SpringApplicationRunListener> listeners) {
		this.log = log;
		this.listeners = new ArrayList<>(listeners);
	}
    
    // 启动监听
    void starting() {
		for (SpringApplicationRunListener listener : this.listeners) {
            // 遍历执行SpringApplicationRunListener#starting方法
			listener.starting();
		}
	}
}
```

实质上获取的`SpringApplicationRunListener`只有一个：

![image-20210817204657701](.\图片\spring-factories-SpringApplicationRunListener.png)





#### 第二步：构造应用上下文环境

```java
// 2. 构造应用上下文环境
ConfigurableEnvironment environment = prepareEnvironment(listeners, applicationArguments);

// org.springframework.boot.SpringApplication#prepareEnvironment
private ConfigurableEnvironment prepareEnvironment(SpringApplicationRunListeners listeners,
                                                   ApplicationArguments applicationArguments) {
    // 1. 根据容器类型创建对应Environment对象
    ConfigurableEnvironment environment = getOrCreateEnvironment();
    // 2. 获取参数args和环境信息中的profiles激活的信息
    configureEnvironment(environment, applicationArguments.getSourceArgs());
    ConfigurationPropertySources.attach(environment);
    // 3. 通过ConfigFileApplicationListener(由spring.factories中对应ApplicationListener.class获取)监听器
    // 加载默认的application.propertis，以及当前激活的profiles对应配置文件，保存到environment中
    listeners.environmentPrepared(environment);
    bindToSpringApplication(environment);
    if (!this.isCustomEnvironment) {
        environment = new EnvironmentConverter(getClassLoader()).convertEnvironmentIfNecessary(environment,
                                                                                               deduceEnvironmentClass());
    }
    ConfigurationPropertySources.attach(environment);
    return environment;
}
```



可以看出`prepareEnvironment`主要实现`环境信息`的加载，包括系统环境变量以及自行配置的配置信息

它主要通过三部分代码实现



##### getOrCreateEnvironment

```java
private ConfigurableEnvironment getOrCreateEnvironment() {
    if (this.environment != null) {
        return this.environment;
    }
    switch (this.webApplicationType) {
        case SERVLET:
            return new StandardServletEnvironment();
        case REACTIVE:
            return new StandardReactiveWebEnvironment();
        default:
            return new StandardEnvironment();
    }
}
```

可以发现其根据在`SpringApplication`构造时获取的`webApplicationType`，创建不同的`ConfigurableEnvironment`实例

我们通常为`SERVLET`，所以创建`StandardServletEnvironment`类型实例





##### configureEnvironment

```java
protected void configureEnvironment(ConfigurableEnvironment environment, String[] args) {
   if (this.addConversionService) {
      ConversionService conversionService = ApplicationConversionService.getSharedInstance();
      environment.setConversionService((ConfigurableConversionService) conversionService);
   }
   // 将run方法参数args封装为SimpleCommandLinePropertySource添加到environment中
   configurePropertySources(environment, args);
   // 解析spring.profiles.active激活的信息，保存到environment中
   configureProfiles(environment, args);
}


protected void configurePropertySources(ConfigurableEnvironment environment, String[] args) {
    // 获取environment中的sources
    MutablePropertySources sources = environment.getPropertySources();
    // 如果args参数存在
    if (this.addCommandLineProperties && args.length > 0) {
        String name = CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME;
        if (sources.contains(name)) {
            PropertySource<?> source = sources.get(name);
            CompositePropertySource composite = new CompositePropertySource(name);
            composite.addPropertySource(
                new SimpleCommandLinePropertySource("springApplicationCommandLineArgs", args));
            composite.addPropertySource(source);
            sources.replace(name, composite);
        }
        else {
            // 封装为SimpleCommandLinePropertySource对象，保存到sources中
            sources.addFirst(new SimpleCommandLinePropertySource(args));
        }
    }
}

protected void configureProfiles(ConfigurableEnvironment environment, String[] args) {
    // 获取激活的profiles
    Set<String> profiles = new LinkedHashSet<>(this.additionalProfiles);
    profiles.addAll(Arrays.asList(environment.getActiveProfiles()));
    // 保存到environment中
    environment.setActiveProfiles(StringUtils.toStringArray(profiles));
}
```

`configureEnvironment`主要与获取`args`参数封装为`SimpleCommandLinePropertySource`保存到`environment`中，并解析其中对应`spring.profiles.active`的激活信息，保存到`environment`中



##### environmentPrepared

```java
// org.springframework.boot.SpringApplicationRunListeners#environmentPrepared
void environmentPrepared(ConfigurableEnvironment environment) {
    for (SpringApplicationRunListener listener : this.listeners) {
        // 通过EventPublishingRunListener#environmentPrepared发布消息，给ApplicationListener处理
        listener.environmentPrepared(environment);
    }
}
```

可以发现，前面通过`org.springframework.boot.context.event.EventPublishingRunListener`发布消息供`ApplicationListener`处理



跟踪调用`environmentPrepared`方法

```java
// org.springframework.context.event.SimpleApplicationEventMulticaster#multicastEvent
public void multicastEvent(final ApplicationEvent event, @Nullable ResolvableType eventType) {
    ResolvableType type = (eventType != null ? eventType : resolveDefaultEventType(event));
    Executor executor = getTaskExecutor();
    // 通过getApplicationListeners获取对应响应消息的ApplicationListener实例
    for (ApplicationListener<?> listener : getApplicationListeners(event, type)) {
        if (executor != null) {
            executor.execute(() -> invokeListener(listener, event));
        }
        else {
            // 调用监听动作
            invokeListener(listener, event);
        }
    }
}
```

我们需要关注的是 `ConfigFileApplicationListener` 这个监听器 实现了对于`application`配置文件的加载

具体可以分析`org.springframework.boot.context.config.ConfigFileApplicationListener`



##### 小结

第二步实现了上下文环境`ConfigurableEnvironment`对象的实例化及信息填充，其中重点是

- 解析`args`参数及`environment`环境信息，获取`spring.profiles.active`对应激活信息
- 通过 `ConfigFileApplicationListener` 这个监听器，实现对应 `application` 配置文件的信息加载



#### 第三步：初始化应用上下文

```java
// 3. 根据前面获取的容器类型，创建application容器
context = createApplicationContext();

// org.springframework.boot.SpringApplication#createApplicationContext
protected ConfigurableApplicationContext createApplicationContext() {
    Class<?> contextClass = this.applicationContextClass;
    if (contextClass == null) {
        try {
            // 根据容器类型webApplicationType，获取不同Class
            switch (this.webApplicationType) {
                case SERVLET:
                    contextClass = Class.forName(DEFAULT_SERVLET_WEB_CONTEXT_CLASS);
                    break;
                case REACTIVE:
                    contextClass = Class.forName(DEFAULT_REACTIVE_WEB_CONTEXT_CLASS);
                    break;
                default:
                    contextClass = Class.forName(DEFAULT_CONTEXT_CLASS);
            }
        }
        catch (ClassNotFoundException ex) {
            throw new IllegalStateException(
                "Unable create a default ApplicationContext, please specify an ApplicationContextClass", ex);
        }
    }
    // 将对应contextClass实例化
    return (ConfigurableApplicationContext) BeanUtils.instantiateClass(contextClass);
}
```

 实质就是根据不同容器类型实例化不同`ConfigurableApplicationContext`上下文



通常容器类型为`SERVLET`，所以实例化`AnnotationConfigServletWebServerApplicationContext`类型上下文，我们查看其构造函数

##### AnnotationConfigServletWebServerApplicationContext

```java
public class AnnotationConfigServletWebServerApplicationContext extends ServletWebServerApplicationContext
		implements AnnotationConfigRegistry {
    
    public AnnotationConfigServletWebServerApplicationContext() {
		this.reader = new AnnotatedBeanDefinitionReader(this);
		this.scanner = new ClassPathBeanDefinitionScanner(this);
	}
    
}
```

我们再查看其类图

![image-20210817210803348](.\图片\AnnotationConfigServletWebServerApplicationContext类图.png)



可以发现其继承了类`GenericApplicationContext`，并且观察其构造方法，可以得知，其运行方式与`AnnotationConfigApplicationContext`相似

所以在构造时，同时触发`GenericApplicationContext`类的构造器，就创建了IOC容器`BeanFactory`



#### 第四步：刷新应用上下文前的准备阶段 

```java
// 4、刷新应用上下文前的准备阶段
prepareContext(context, environment, listeners, applicationArguments, printedBanner);


private void prepareContext(ConfigurableApplicationContext context, ConfigurableEnvironment environment,
                            SpringApplicationRunListeners listeners, ApplicationArguments applicationArguments, Banner printedBanner) {
    // 1. 属性设置准备
    // 设置容器环境到上下文中
    context.setEnvironment(environment);
    // 执行容器后置处理
    postProcessApplicationContext(context);
    // 执行初始化器
    applyInitializers(context);
    // 向各个ApplicationListener发送容器已经准备好的事件
    listeners.contextPrepared(context);
    if (this.logStartupInfo) {
        logStartupInfo(context.getParent() == null);
        logStartupProfileInfo(context);
    }
    // 获取BeanFactory
    ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
    // 将 main函数中的args参数封装为单例Bean，注册到IOC容器中
    beanFactory.registerSingleton("springApplicationArguments", applicationArguments);
    if (printedBanner != null) {
        beanFactory.registerSingleton("springBootBanner", printedBanner);
    }
    if (beanFactory instanceof DefaultListableBeanFactory) {
        ((DefaultListableBeanFactory) beanFactory)
        .setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
    }
    if (this.lazyInitialization) {
        context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor());
    }
    // 2. 将主配置类的BeanDefinition注册到IOC容器中
    // 通过getAllSources，获取主配置类
    Set<Object> sources = getAllSources();
    Assert.notEmpty(sources, "Sources must not be empty");
    // 加载主配置类到IOC容器中
    load(context, sources.toArray(new Object[0]));
    listeners.contextLoaded(context);
}
```



`prepareContext`方法实质上主要做了两件事情

- 为`ConfigurableApplicationContext`上下文填充属性
- 向IOC容器中注册Bean
  - **重点**：将主配置类的`BeanDefinition`注册到IOC容器中
    - 通过`getAllSources`获取构造时保存的主配置类
    - 通过`load`方法将主配置类的`BeanDefinition`注册到IOC容器中



#### 第五步：刷新应用上下文 

```java
// 5. 刷新上下文
refreshContext(context);

// org.springframework.boot.SpringApplication#refreshContext
private void refreshContext(ConfigurableApplicationContext context) {
    refresh(context);
    if (this.registerShutdownHook) {
        try {
            context.registerShutdownHook();
        }
        catch (AccessControlException ex) {
            // Not allowed in some environments.
        }
    }
}

// org.springframework.boot.SpringApplication#refresh
protected void refresh(ApplicationContext applicationContext) {
    Assert.isInstanceOf(AbstractApplicationContext.class, applicationContext);
    ((AbstractApplicationContext) applicationContext).refresh();
}
```

我们发现其调用过程中，最终调用到了`AbstractApplicationContext#refresh`方法，就是`Spring`源码内容，解析方式同`AnnotationConfigApplicationContext`，此处不再赘述

实质解析过程就是通过`ConfigurationClassPostProcessor`后置处理器，以主配置类为入口，进行配置解析，主要通过解析主配置类上`@SpringBootApplication`注解



#### 第六步：刷新应用上下文后的扩展接口

```java
// 6. 刷新应用上下文后的扩展接口
afterRefresh(context, applicationArguments);

protected void afterRefresh(ConfigurableApplicationContext context, ApplicationArguments args) {}
```

我们发现`afterRefresh`在此处是一个空方法，它实际就是一个模板方法，供需要的时候进行扩展的接口方法



## 自定义starter

`SpringBoot starter`机制：`SpringBoot`提 供了针对日常企业应用研发各种场景的`spring-boot-starter`依赖模块。所有这些依赖模块都遵循着 约定成俗的默认配置，并允许我们调整这些配置，即遵循“约定大于配置”的理念



### 为什么要自定义starter

在我们的日常开发工作中，经常会有一些独立于业务之外的配置模块，我们经常将其放到一个特定 的包下，然后如果另一个工程需要复用这块功能的时候，需要将代码硬拷贝到另一个工程，重新集 成一遍，麻烦至极。如果我们将这些可独立于业务代码之外的功配置模块封装成一个个`starter`， 复用的时候只需要将其在`pom`中引用依赖即可，再由`SpringBoot`为我们完成自动装配，就非常轻 松了



### 自定义starter命名规则

`SpringBoot`提供的`starter`命名规则为：`spring-boot-starter-xxx`

所以为了区分于`SpringBoot`提供的`starter`，自定义`starter`推荐的命名规则为：`xxx-spring-boot-starter`



### 自定义starter实现

实现一个自定义`starter`主要有以下几步

可参考：https://xiaoym.gitee.io/2020/12/10/spring-boot-self-starter/



#### 1. 引入依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-autoconfigure</artifactId>
        <version>2.2.9.RELEASE</version>
    </dependency>
</dependencies>
```



#### 2. 定义配置类

```java
@Configuration
public class MyConfiguration {
	
	@Bean
	public TestJavaBean testJavaBean() {
		return new TestJavaBean();
	}
	
	@Configuration
	class InnerConfiguration {
		
	}
}
```

一般配置类内容有两种

- 通过`@Bean`注解的方法注册`bean`
- 通过内部配置类注册bean



#### 3. 添加spring.factories文件

由于`SpringBoot`自动配置机制，它将加载所有`jar`包下对应 `/META-INF/spring.factories` 文件，并解析其内容获取自动配置类

所以我们可以在自定义`starter`包下，也添加`spring.factories`文件

![image-20210818085437641](.\图片\spring-factories文件位置.png)

对应文件内容

```yml
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.learn.test.MyConfiguration
```



这样一个自定义`starter`就算实现好了



### 使用自定义starter

使用自定义`starter`与使用`SpringBoot`生态下的`starter`一致，直接引入其`jar`包依赖即可

对于配置类中注册的`bean`，也可以通过`@Autowired`进行注入使用



### 热插拔使用

我们需要注意一点，上述我们自定义`starter`对应配置类是没有添加对应`Condition`注解进行条件判断的，这表示只要引入了我们的依赖，其中的配置类就将生效。但在实际使用过程中，可能不是任何场景下都需要使用我们的配置类，所以此时我们需要一个可以控制配置类是否生效的开关

通常思路就是通过`Condition`注解结合 变量 进行控制，在`SpringBoot`中通常使用一种 `@EnableXXX` 注解来替代，下面我们来示范一下它的实现原理



#### 1. 创建一个条件类

```java
public class ConfigMarker {
}
```



#### 2. 创建一个Enable注解

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({ConfigMarker.class})
public @interface EnableRegisterServer {
}
```

可以发现`EnableRegisterServer`通过`@Import`注解导入了`ConfigMarker`的`BeanDefinition`到IOC容器中



#### 3. 配置类中添加Condition条件

```java
@Configuration
@ConditionalOnBean(ConfigMarker.class)
public class MyConfiguration {}
```

通过`@ConditionalOnBean`注解，实现了通过`ConfigMarker`类型`bean`的存在与否控制此配置类是否生效



#### 4. 使用

在引入自定义`starter`的项目的配置类中，如果添加了`@EnableRegisterServer`，则表示需要启用我们的自定义`starter`中的配置类



#### 总结

实质还是通过`Condition`注解实现条件控制，只不过是通过`EnableXXX`注解使其更加直观



## 内嵌Tomcat

对于`SpringBoot`项目，我们仅仅只需通过`run`方法启动，无需进行项目部署，就可以提供`web`服务，它实际是借助内嵌`Servlet`容器

`SpringBoot`支持三种内嵌`Servlet`容器

- `Tomcat`
- `Jetty`
- `Undertow`



`SpringBoot`项目默认内嵌`Tomcat`容器，我们通过引入`spring-boot-starter-web`模板，就可以使用默认的`Tomcat`容器

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```



实质上`spring-boot-starter-web`模块只是一个空模板，并无任何Java代码，它主要通过`pom.xml`文件引入`Tomcat、SpringMVC`相关依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-json</artifactId>
    </dependency>
    <dependency>
        <!--引入tomcat相关依赖-->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-tomcat</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
        <exclusions>
            <exclusion>
                <groupId>org.apache.tomcat.embed</groupId>
                <artifactId>tomcat-embed-el</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-web</artifactId>
    </dependency>
    <dependency>
        <!--引入SpringMVC依赖-->
        <groupId>org.springframework</groupId>
        <artifactId>spring-webmvc</artifactId>
    </dependency>
</dependencies>
```



而`spring-boot-starter-tomcat`模块同`spring-boot-starter-web`模板一样，主要用于引入`Tomcat`相关依赖包

```xml
<dependencies>
    <dependency>
        <groupId>jakarta.annotation</groupId>
        <artifactId>jakarta.annotation-api</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.tomcat.embed</groupId>
        <artifactId>tomcat-embed-core</artifactId>
        <exclusions>
            <exclusion>
                <groupId>org.apache.tomcat</groupId>
                <artifactId>tomcat-annotations-api</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>org.apache.tomcat.embed</groupId>
        <artifactId>tomcat-embed-el</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.tomcat.embed</groupId>
        <artifactId>tomcat-embed-websocket</artifactId>
    </dependency>
</dependencies>
```



### 切换Servlet容器

`Tomcat`作为`SpringBoot`默认的`Servlet`容器，我们也可以切换为其他`Servlet`容器

那么我们改如何做：

- 将默认的`Tomcat`依赖剔除
- 引入其他`Servlet`容器依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <!--通过exclusion剔除spring-boot-starter-web中的spring-boot-starter-tomcat依赖starter-->
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <!--引入jetty的启动starter-->
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jetty</artifactId>
</dependency>
```



### 内嵌Tomcat原理



#### 自动配置类解析

由前面分析可知，在`SpringBoot`项目启动时，将从`spring.factories`配置中获取对应于`EnableAutoConfiguration`的自动配置类，其中就有一个与内嵌`Tomcat`相关的启动类：`ServletWebServerFactoryAutoConfiguration`

```
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration,\
```



我们查看一下`ServletWebServerFactoryAutoConfiguration`配置类

##### ServletWebServerFactoryAutoConfiguration

```java
@Configuration(proxyBeanMethods = false)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnClass(ServletRequest.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties(ServerProperties.class)
@Import({ ServletWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar.class,
		ServletWebServerFactoryConfiguration.EmbeddedTomcat.class,
		ServletWebServerFactoryConfiguration.EmbeddedJetty.class,
		ServletWebServerFactoryConfiguration.EmbeddedUndertow.class })
public class ServletWebServerFactoryAutoConfiguration {}
```



我们不关注其中通过`@Bean`注解注册的`bean`，主要分析一下`ServletWebServerFactoryAutoConfiguration`类上的注解

- `@ConditionalOnClass(ServletRequest.class)`
  - 表明需要存在`ServletRequest`这个类，此配置类才生效
- `@ConditionalOnWebApplication(type = Type.SERVLET)`
  - 表明需要容器类型为`SERVLET`时，此配置类才生效
  - 容器类型在`SpringApplication`构造时确定
- `@EnableConfigurationProperties(ServerProperties.class)`
  - 加载了`ServerProperties`类，并且通过`@ConfigurationProperties`注解实现属性注入
- `@Import`
  - 通过`@Import`注解导入了类



我们主要分析一下后两者的作用



##### ServerProperties

```java
// org.springframework.boot.autoconfigure.web.ServerProperties
@ConfigurationProperties(prefix = "server", ignoreUnknownFields = true)
public class ServerProperties {

    // 服务器 HTTP 端口
    private Integer port;
}
```

`ServerProperties`类通过`server`前缀，注入了关于`Servlet`服务的信息，最主要的就是`port端口`，所以我们通常需要配置 `server.port=xxx`在`application.yml`配置文件中，表明我们启动服务对应的服务端口

其他属性先不一一分析



我们继续分析通过`@Import`注解导入的类，其中主要关注的是

- `ServletWebServerFactoryConfiguration.EmbeddedTomcat.class`
- `ServletWebServerFactoryConfiguration.EmbeddedJetty.class`
- `ServletWebServerFactoryConfiguration.EmbeddedUndertow.class`

我们可以发现其三者都是`ServletWebServerFactoryConfiguration`的内部类，我们可以直接分析`ServletWebServerFactoryConfiguration`

##### ServletWebServerFactoryConfiguration

```java
// org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryConfiguration
@Configuration(proxyBeanMethods = false)
class ServletWebServerFactoryConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ Servlet.class, Tomcat.class, UpgradeProtocol.class })
	@ConditionalOnMissingBean(value = ServletWebServerFactory.class, search = SearchStrategy.CURRENT)
	static class EmbeddedTomcat {

		@Bean
		TomcatServletWebServerFactory tomcatServletWebServerFactory(
				ObjectProvider<TomcatConnectorCustomizer> connectorCustomizers,
				ObjectProvider<TomcatContextCustomizer> contextCustomizers,
				ObjectProvider<TomcatProtocolHandlerCustomizer<?>> protocolHandlerCustomizers) {
			TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
			factory.getTomcatConnectorCustomizers()
					.addAll(connectorCustomizers.orderedStream().collect(Collectors.toList()));
			factory.getTomcatContextCustomizers()
					.addAll(contextCustomizers.orderedStream().collect(Collectors.toList()));
			factory.getTomcatProtocolHandlerCustomizers()
					.addAll(protocolHandlerCustomizers.orderedStream().collect(Collectors.toList()));
			return factory;
		}

	}

	/**
	 * Nested configuration if Jetty is being used.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ Servlet.class, Server.class, Loader.class, WebAppContext.class })
	@ConditionalOnMissingBean(value = ServletWebServerFactory.class, search = SearchStrategy.CURRENT)
	static class EmbeddedJetty {

		@Bean
		JettyServletWebServerFactory JettyServletWebServerFactory(
				ObjectProvider<JettyServerCustomizer> serverCustomizers) {
			JettyServletWebServerFactory factory = new JettyServletWebServerFactory();
			factory.getServerCustomizers().addAll(serverCustomizers.orderedStream().collect(Collectors.toList()));
			return factory;
		}

	}

	/**
	 * Nested configuration if Undertow is being used.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ Servlet.class, Undertow.class, SslClientAuthMode.class })
	@ConditionalOnMissingBean(value = ServletWebServerFactory.class, search = SearchStrategy.CURRENT)
	static class EmbeddedUndertow {

		@Bean
		UndertowServletWebServerFactory undertowServletWebServerFactory(
				ObjectProvider<UndertowDeploymentInfoCustomizer> deploymentInfoCustomizers,
				ObjectProvider<UndertowBuilderCustomizer> builderCustomizers) {
			UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory();
			factory.getDeploymentInfoCustomizers()
					.addAll(deploymentInfoCustomizers.orderedStream().collect(Collectors.toList()));
			factory.getBuilderCustomizers().addAll(builderCustomizers.orderedStream().collect(Collectors.toList()));
			return factory;
		}

	}

}
```



我们可以发现，在外部类`ServletWebServerFactoryConfiguration`上，并无任一`Condition`条件注解，所以此配置类一致生效

而对应于其中三个内部类`EmbeddedTomcat、EmbeddedJetty、EmbeddedUndertow`

- 它们都是通过`@Configuration`注解注释的配置类

- 它们分别对应`SpringBoot`支持的三种`Servlet`容器

- 并且我们可以发现这三个内部类上都通过`@ConditionalOnClass`实现条件判断
  - 目的是通过对应于不同的`Servlet`容器依赖，生效不同的配置类
- 它们内部都通过`@Bean`注解注释的方法注册了一个`bean`，分别是
  - `TomcatServletWebServerFactory`
  - `JettyServletWebServerFactory`
  - `UndertowServletWebServerFactory`
  - 它们都是实现了`ServletWebServerFactory`接口的子类



##### ServletWebServerFactory接口

```java
@FunctionalInterface
public interface ServletWebServerFactory {

	/* 获取一个暂停的WebServer的实例，但在调用WebServer.start()方法前，服务无法连接 */
	WebServer getWebServer(ServletContextInitializer... initializers);
}
```



#### run方法过程解析

上述自动配置类，在`SpringBoot`解析过程中，都是通过`ConfigurationClassPostProcessor`这个后置处理器进行解析，那么我们结合前面对于`SpringBoot`的`run`方法的解析过程，此时的调用链为：

```
1. org.springframework.boot.SpringApplication#run
2. org.springframework.boot.SpringApplication#refreshContext
3. org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext#refresh
4. org.springframework.context.support.AbstractApplicationContext#refresh
```



在`AbstractApplicationContext#refresh`方法中，已经经过了`invokeBeanFactoryPostProcessors`方法的后置处理，在IOC容器中已经配置好我们所需的`BeanDefinition`，下面关于内嵌`Tomcat`，我们主要分析以下两个方法

```java
// org.springframework.context.support.AbstractApplicationContext#refresh
public void refresh() throws BeansException, IllegalStateException {
    // 省略...
    
    onRefresh();
    
    // 省略...
    
    finishRefresh();
    
    // 省略...
}
```



下面我们来分析以下这两个方法



##### onRefresh

`onRefresh`方法是抽象类`AbstractApplicationContext`中的一个模板方法

由于`SpringBoot`中使用的`Application`上下文为`AnnotationConfigServletWebServerApplicationContext`，所以执行执行的`onRefresh`方法在其父类`ServletWebServerApplicationContext`中实现

```java
// org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext#onRefresh
protected void onRefresh() {
    super.onRefresh();
    try {
        createWebServer();
    }
    catch (Throwable ex) {
        throw new ApplicationContextException("Unable to start web server", ex);
    }
}
```



我们不用去关注其`super.onRefresh()`方法调用，我们主要分析`createWebServer`方法



###### createWebServer

```java
// org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext#createWebServer
private void createWebServer() {
    WebServer webServer = this.webServer;
    ServletContext servletContext = getServletContext();
    if (webServer == null && servletContext == null) {
        // 第一次调用时webServer、servletContext都为空，所以进入
        // 1. 通过getWebServerFactory获取对应ServletWebServerFactory实例
        ServletWebServerFactory factory = getWebServerFactory();
        // 2. 调用ServletWebServerFactory#getWebServer获取WebServer实例
        this.webServer = factory.getWebServer(getSelfInitializer());
    }
    else if (servletContext != null) {
        try {
            getSelfInitializer().onStartup(servletContext);
        }
        catch (ServletException ex) {
            throw new ApplicationContextException("Cannot initialize servlet context", ex);
        }
    }
    initPropertySources();
}
```





###### getWebServerFactory

我们先分析一下`getServletContext`方法

```java
// org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext#getWebServerFactory
protected ServletWebServerFactory getWebServerFactory() {
    // 从IOC容器中获取对应ServletWebServerFactory类型的beanNames
    String[] beanNames = getBeanFactory().getBeanNamesForType(ServletWebServerFactory.class);
    // 通过IOC容器，创建一个ServletWebServerFactory实例
    return getBeanFactory().getBean(beanNames[0], ServletWebServerFactory.class);
}
```

可知，`getServletContext`方法就是从IOC容器中已注册的`BeanDefinition`中获取、并创建一个对应`ServletWebServerFactory`的实例

而前面通过`ServletWebServerFactoryConfiguration`配置类的内部配置类，引入的就是`ServletWebServerFactory`类型子类

此处我们分析内嵌`Tomcat`，则获取的实例为`TomcatServletWebServerFactory`



###### getWebServer

后续调用了`ServletWebServerFactory#getWebServer`，那么我们此时应该分析`TomcatServletWebServerFactory#getWebServer`

```java
// org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory#getWebServer
public WebServer getWebServer(ServletContextInitializer... initializers) {
    if (this.disableMBeanRegistry) {
        Registry.disableRegistry();
    }
    // 1. 创建Tomcat实例
    Tomcat tomcat = new Tomcat();
    // 2. 设置Tomcat相应的属性信息
    File baseDir = (this.baseDirectory != null) ? this.baseDirectory : createTempDir("tomcat");
    tomcat.setBaseDir(baseDir.getAbsolutePath());
    Connector connector = new Connector(this.protocol);
    connector.setThrowOnFailure(true);
    tomcat.getService().addConnector(connector);
    customizeConnector(connector);
    tomcat.setConnector(connector);
    tomcat.getHost().setAutoDeploy(false);
    configureEngine(tomcat.getEngine());
    for (Connector additionalConnector : this.additionalTomcatConnectors) {
        tomcat.getService().addConnector(additionalConnector);
    }
    prepareContext(tomcat.getHost(), initializers);
    // 3. 获取TomcatWebServer
    return getTomcatWebServer(tomcat);
}
```



我们可以发现上述方法主要分为三部分：

- 创建Tomcat实例
- 设置Tomcat相应的属性信息
- 获取TomcatWebServer

第一部分是主要的，它创建了一个`Tomcat`实例，但是问题是仅仅实例化`Tomcat`，它并不能够工作，为我们提供服务

而第二部分只是一些属性设置，所以我们需要分析第三部分：`getTomcatWebServer`方法



###### getTomcatWebServer

```java
// org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory#getTomcatWebServer
protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {
    return new TomcatWebServer(tomcat, getPort() >= 0);
}
```

我们发现`getTomcatWebServer`方法中，只是构建了一个`TomcatWebServer`实例，我们需要继续分析`TomcatWebServer`的构造



```java
// org.springframework.boot.web.embedded.tomcat.TomcatWebServer#TomcatWebServer(org.apache.catalina.startup.Tomcat, boolean)
public TomcatWebServer(Tomcat tomcat, boolean autoStart) {
    Assert.notNull(tomcat, "Tomcat Server must not be null");
    this.tomcat = tomcat;
    this.autoStart = autoStart;
    initialize();
}

private void initialize() throws WebServerException {
    // 省略...
    this.tomcat.start();
    // 省略...
}
```

我们发现在`TomcatWebServer`的构造中调用了`initialize`方法，而在`initialize`方法中，存在一个关键代码：通过`this.tomcat.start()`启动了`Tomcat`实例



###### 小结

至此，我们发现通过`onRefresh`方法处理，我们创建了`Servlet`容器(此处分析针对于`Tomcat`)，并且通过`start`方法启动了它，让其进行工作



## 自动配置SpringMVC

由前一步分析了如何内嵌`Tomcat`，并且将对应`Servlet`容器进行启动

但是存在一个问题：启动了`Servlet`容器，并没有注册对应`Servlet`实例提供服务，那我们不是启动一个没有服务的`Servlet`容器，也就无法让其为我们提供服务

在`SpringMVC`源码分析时，我们了解到`SpringMVC`主要通过一个`DispatcherServlet`实例来为我们提供服务，那么我们主要分析方向就是`SpringBoot`是如何自动配置`DispatcherServlet`实例



### Servlet3.0

但是单单创建`DispatcherServlet`实例，并不能直接提供服务能力

在正常的`web`项目中，我们通常需要将`Servlet`配置在`/WEB-INF/web.xml`配置文件中，`Servlet`才能被`Servlet`容器发现，并拦截请求

而在`SpringBoot`项目中，我们使用的是内嵌`Servlet`容器，并没有对应的`web.xml`配置文件，那么此时我们如何将自动配置的`DispatcherServlet`注册到我们内嵌的`Servlet`容器中呢？这里就需要了解一下`Servlet3.0`规范

参照：https://www.cnblogs.com/ryelqy/p/10238155.html



`Servlet3.0`提供了许多新特性，便于我们进行`Servlet`开发，我们一一了解

#### 1. 异步处理支持

在`Servlet3.0`之前，普通`Servlet`处理请求的流程：

- `Servlet`接收请求，开启一个线程进行请求处理
- 对请求数据进行预处理
- 将预处理后的数据调用业务处理接口，进行业务处理
- 业务处理完毕，返回响应
- 线程结束

可知，在`Servlet`对一个请求的处理，就有对应的一个线程，并且需要等待所有业务处理完毕、请求结束，线程才会结束。在业务处理时，线程一直处于阻塞状态

`Servlet3.0`提供了异步处理支持，大致流程：

- `Servlet`接收请求，开启一个线程进行请求处理
- 对请求数据进行预处理
- 此时，不同于`3.0`以前，将不是由此时的线程继续进行业务接口的调用，而是将请求交给一个异步线程来执行业务处理，当前线程返回`Servlet`容器
- 业务处理完毕，返回响应，异步线程结束

**两者最大的区别** 在于：在`3.0`以后，`Servlet`线程将不用阻塞等待业务处理结束，对于`Servlet`容器来说，可以支持更大的并发量

**注意**：

- 异步处理适用于`Servlet`和过滤器两种组件
- 异步处理并**不是默认开启**



#### 2. 新增注解支持

在`Servlet3.0`以前，对于一个`web`应用，`web.xml`配置文件是必须的。但在`3.0`以后，通过新增注解，就可以取代传统的`web.xml`配置



##### @WebServlet

`@WebServlet` 用于将一个类声明为 `Servlet`，该注解将会在部署时被容器处理，容器将根据具体的属性配置将相应的类部署为 `Servlet`

`@WebServlet`属性：

| **属性名**     | **类型**       | **描述**                                                     |
| -------------- | -------------- | ------------------------------------------------------------ |
| name           | String         | 指定 Servlet 的 name 属性，等价于 <servlet-name>。如果没有显式指定，则该 Servlet 的取值即为类的全限定名。 |
| value          | String[]       | 该属性等价于 urlPatterns 属性。两个属性不能同时使用。        |
| urlPatterns    | String[]       | 指定一组 Servlet 的 URL 匹配模式。等价于 <url-pattern> 标签。 |
| loadOnStartup  | int            | 指定 Servlet 的加载顺序，等价于 <load-on-startup> 标签。     |
| initParams     | WebInitParam[] | 指定一组 Servlet 初始化参数，等价于 <init-param> 标签。      |
| asyncSupported | boolean        | 声明 Servlet 是否支持异步操作模式，等价于 <async-supported> 标签。 |
| description    | String         | 该 Servlet 的描述信息，等价于 <description> 标签。           |
| displayName    | String         | 该 Servlet 的显示名，通常配合工具使用，等价于 <display-name> 标签。 |

所有属性均为可选属性，但是 `vlaue` 或者 `urlPatterns` 通常是必需的，且二者不能共存，如果同时指定，通常是忽略 value 的取值



##### @WebFilter

`@WebFilter` 用于将一个类声明为过滤器，该注解将会在部署时被容器处理，容器将根据具体的属性配置将相应的类部署为过滤器

`@WebFilter`属性：

| **属性名**      | **类型**       | **描述**                                                     |
| --------------- | -------------- | ------------------------------------------------------------ |
| filterName      | String         | 指定过滤器的 name 属性，等价于 <filter-name>                 |
| value           | String[]       | 该属性等价于 urlPatterns 属性。但是两者不应该同时使用。      |
| urlPatterns     | String[]       | 指定一组过滤器的 URL 匹配模式。等价于 <url-pattern> 标签。   |
| servletNames    | String[]       | 指定过滤器将应用于哪些 Servlet。取值是 @WebServlet 中的 name 属性的取值，或者是 web.xml 中 <servlet-name> 的取值。 |
| dispatcherTypes | DispatcherType | 指定过滤器的转发模式。具体取值包括： ASYNC、ERROR、FORWARD、INCLUDE、REQUEST。 |
| initParams      | WebInitParam[] | 指定一组过滤器初始化参数，等价于 <init-param> 标签。         |
| asyncSupported  | boolean        | 声明过滤器是否支持异步操作模式，等价于 <async-supported> 标签。 |
| description     | String         | 该过滤器的描述信息，等价于 <description> 标签。              |
| displayName     | String         | 该过滤器的显示名，通常配合工具使用，等价于 <display-name> 标签。 |

所有属性均为可选属性，但是 `value、urlPatterns、servletNames` 三者必需至少包含一个，且 `value` 和 `urlPatterns` 不能共存，如果同时指定，通常忽略 `value` 的取值



##### @WebInitParam

配合 `@WebServlet` 或者 `@WebFilter` 使用。它的作用是为 `Servlet` 或者过滤器指定初始化参数，这等价于 `web.xml` 中 `<servlet>` 和 `<filter>` 的 `<init-param>` 子标签

`@WebInitParam`属性：

| **属性名**  | **类型** | **是否可选** | **描述**                               |
| ----------- | -------- | ------------ | -------------------------------------- |
| name        | String   | 否           | 指定参数的名字，等价于 <param-name>。  |
| value       | String   | 否           | 指定参数的值，等价于 <param-value>。   |
| description | String   | 是           | 关于参数的描述，等价于 <description>。 |



##### @WebListener

该注解用于将类声明为监听器

`@WebListener`属性

| **属性名** | **类型** | **是否可选** | **描述**           |
| ---------- | -------- | ------------ | ------------------ |
| value      | String   | 是           | 该监听器的描述信息 |

被 `@WebListener` 标注的类必须实现以下至少一个接口：

- ServletContextListener
- ServletContextAttributeListener
- ServletRequestListener
- ServletRequestAttributeListener
- HttpSessionListener
- HttpSessionAttributeListener



##### @MultipartConfig

该注解主要是为了辅助 `Servlet 3.0` 中 `HttpServletRequest` 提供的对上传文件的支持。该注解标注在 `Servlet` 上面，以表示该 `Servlet` 希望处理的请求的 MIME 类型是 `multipart/form-data`

`@MultipartConfig`属性

| 属性名            | 类型   | 是否可选 | 描述                                                         |
| ----------------- | ------ | -------- | ------------------------------------------------------------ |
| fileSizeThreshold | int    | 是       | 当数据量大于该值时，内容将被写入文件。                       |
| location          | String | 是       | 存放生成的文件地址。                                         |
| maxFileSize       | long   | 是       | 允许上传的文件最大值。默认值为 -1，表示没有限制。            |
| maxRequestSize    | long   | 是       | 针对该 multipart/form-data 请求的最大数量，默认值为 -1，表示没有限制。 |



#### 3. ServletContext 的性能增强

`Servlet 3.0`中，`ServletContext`对象支持在运行时动态部署 `Servlet`、过滤器、监听器，以及为 `Servlet` 和过滤器增加 `URL` 映射等

以 `Servlet` 为例，过滤器与监听器与之类似。`ServletContext` 为动态配置 `Servlet` 增加了如下方法：

- 动态部署Servlet
  - `ServletRegistration.Dynamic addServlet(String servletName,Class<? extends Servlet> servletClass)`
  - `ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet)`
  - `ServletRegistration.Dynamic addServlet(String servletName, String className)`



至此，我们需要分析的点有：

- 如何自动配置`DispatcherServlet`实例
- 如何`DispatcherServlet`部署到内嵌的`Servlet`容器中



### 自动配置DispatcherServlet

对于如何自动配置`DispatcherServlet`，我们需要关注的是自动配置类：`DispatcherServletAutoConfiguration`

```
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration,/
```



我们分析`DispatcherServletAutoConfiguration`

#### DispatcherServletAutoConfiguration

```java
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@AutoConfigureAfter(ServletWebServerFactoryAutoConfiguration.class)
public class DispatcherServletAutoConfiguration {
    public static final String DEFAULT_DISPATCHER_SERVLET_BEAN_NAME = "dispatcherServlet";
    public static final String DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME = "dispatcherServletRegistration";

    @Configuration(proxyBeanMethods = false)
    @Conditional(DefaultDispatcherServletCondition.class)
    @ConditionalOnClass(ServletRegistration.class)
    @EnableConfigurationProperties({ HttpProperties.class, WebMvcProperties.class })
    protected static class DispatcherServletConfiguration {

        @Bean(name = DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
        public DispatcherServlet dispatcherServlet(HttpProperties httpProperties, WebMvcProperties webMvcProperties) {
            DispatcherServlet dispatcherServlet = new DispatcherServlet();
            dispatcherServlet.setDispatchOptionsRequest(webMvcProperties.isDispatchOptionsRequest());
            dispatcherServlet.setDispatchTraceRequest(webMvcProperties.isDispatchTraceRequest());
            dispatcherServlet.setThrowExceptionIfNoHandlerFound(webMvcProperties.isThrowExceptionIfNoHandlerFound());
            dispatcherServlet.setPublishEvents(webMvcProperties.isPublishRequestHandledEvents());
            dispatcherServlet.setEnableLoggingRequestDetails(httpProperties.isLogRequestDetails());
            return dispatcherServlet;
        }

        @Bean
        @ConditionalOnBean(MultipartResolver.class)
        @ConditionalOnMissingBean(name = DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME)
        public MultipartResolver multipartResolver(MultipartResolver resolver) {
            // Detect if the user has created a MultipartResolver but named it incorrectly
            return resolver;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Conditional(DispatcherServletRegistrationCondition.class)
    @ConditionalOnClass(ServletRegistration.class)
    @EnableConfigurationProperties(WebMvcProperties.class)
    @Import(DispatcherServletConfiguration.class)
    protected static class DispatcherServletRegistrationConfiguration {

        @Bean(name = DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME)
        @ConditionalOnBean(value = DispatcherServlet.class, name = DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
        public DispatcherServletRegistrationBean dispatcherServletRegistration(DispatcherServlet dispatcherServlet,
                                                                               WebMvcProperties webMvcProperties, ObjectProvider<MultipartConfigElement> multipartConfig) {
            DispatcherServletRegistrationBean registration = new DispatcherServletRegistrationBean(dispatcherServlet,
                                                                                                   webMvcProperties.getServlet().getPath());
            registration.setName(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME);
            registration.setLoadOnStartup(webMvcProperties.getServlet().getLoadOnStartup());
            multipartConfig.ifAvailable(registration::setMultipartConfig);
            return registration;
        }

    }
}
```



由`DispatcherServletAutoConfiguration`代码分析，其中通过两个内部配置类中`@Bean`注解注释的方法，主要向IOC容器中注册了以下两个`BeanDefinition`：

- `DispatcherServlet`
- `DispatcherServletRegistrationBean`



所以，我们就自动配置了`DispatcherServlet`实例到IOC容器中



### 部署DispatcherServlet

我们虽然注册了`DispatcherServlet`到IOC容器，但是我们需要将其部署到内嵌`Servlet`容器中，让其为我们的程序服务

在前面我们分析内嵌`Servlet`容器时，由分析到`createWebServer`方法，我们关注其中部分代码

```java
// org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext#createWebServer
private void createWebServer() {
    // 第一次调用时webServer、servletContext都为空，所以进入
    // 1. 通过getWebServerFactory获取对应ServletWebServerFactory实例
    ServletWebServerFactory factory = getWebServerFactory();
    // 2. 调用ServletWebServerFactory#getWebServer获取WebServer实例
    this.webServer = factory.getWebServer(getSelfInitializer());
}
```



其中有一个通过`getSelfInitializer`方法获取`ServletContextInitializer`实例

#### ServletContextInitializer

```java
@FunctionalInterface
public interface ServletContextInitializer {
    void onStartup(ServletContext servletContext) throws ServletException;
}
```

`ServletContextInitializer`接口类似于`WebApplicationInitializer`接口，都存在一个`onStartup`方法，在`Servlet`容器启动时，可以对`ServletContext`进行初始化操作

不同的是：

- `ServletContextInitializer`由`Spring`控制
- `WebApplicationInitializer`由`Servlet`容器控制



#### getSelfInitializer

```java
// org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext#getSelfInitializer
private org.springframework.boot.web.servlet.ServletContextInitializer getSelfInitializer() {
    return this::selfInitialize;
}
```

我们发现`getSelfInitializer`通过方法引用返回了一个`lambda`表达式表示的`ServletContextInitializer`实例，`selfInitialize`具体细节我们后续分析



#### getWebServer

跟踪`getWebServer`方法调用链，我们内嵌的是`Tomcat`，所以关注`TomcatServletWebServerFactory#getWebServer`

```java
// org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory#getWebServer
public WebServer getWebServer(ServletContextInitializer... initializers) {
    // 准备ServletContext
    prepareContext(tomcat.getHost(), initializers);
}

// org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory#prepareContext
protected void prepareContext(Host host, ServletContextInitializer[] initializers) {
    // 封装ServletContextInitializer
    ServletContextInitializer[] initializersToUse = mergeInitializers(initializers);
    // 配置ServletContext
    configureContext(context, initializersToUse);
}

// org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory#configureContext
protected void configureContext(Context context, ServletContextInitializer[] initializers) {
    // 将ServletContextInitializer封装为TomcatStarter
    TomcatStarter starter = new TomcatStarter(initializers);
    // 将TomcatStarter添加到ServletContext中
    context.addServletContainerInitializer(starter, NO_CLASSES);
}
```

至此，我们将获取的`ServletContextInitializer`添加到了`Context`中，则在`Servlet`容器启动时，将调用`ServletContextInitializer#onStartup`方法



我们获取的`ServletContextInitializer`是对应于`selfInitialize`方法的`lambda`表达式实例，所以我们需要分析`selfInitialize`方法

#### selfInitialize

```java
private void selfInitialize(ServletContext servletContext) throws ServletException {
    prepareWebApplicationContext(servletContext);
    registerApplicationScope(servletContext);
    WebApplicationContextUtils.registerEnvironmentBeans(getBeanFactory(), servletContext);
    // 调用getServletContextInitializerBeans方法，从IOC容器中获取ServletContextInitializer
    for (ServletContextInitializer beans : getServletContextInitializerBeans()) {
        // 调用onStartup方法
        beans.onStartup(servletContext);
    }
}
```



对于`getServletContextInitializerBeans`方法，不做具体的代码分析，其主要作用就是从IOC容器中获取对应于`ServletContextInitializer`类型的实例

此时，将返回我们通过`DispatcherServletAutoConfiguration`自动配置类注册的`DispatcherServletRegistrationBean`实例了



#### DispatcherServletRegistrationBean

我们先查看其类图

![image-20210819215338014](D:\学习整理\summarize\springboot\图片\DispatcherServletRegistrationBean类图)



我们可以发起`DispatcherServletRegistrationBean`实现了`ServletContextInitializer`接口，而前面我们通过自动配置类，将其注册到了IOC容器中，所以此时通过`getServletContextInitializerBeans`方法，我们将获取到对应`DispatcherServletRegistrationBean`实例，继而调用其`onStartup`方法



`DispatcherServletRegistrationBean`中`onStartup`方法在其抽象父类`RegistrationBean`中，我们跟踪其调用，忽略无用代码

```java
// org.springframework.boot.web.servlet.RegistrationBean
public abstract class RegistrationBean implements ServletContextInitializer, Ordered {
    public final void onStartup(ServletContext servletContext) throws ServletException {
		register(description, servletContext);
	}
}

// org.springframework.boot.web.servlet.DynamicRegistrationBean#register
protected final void register(String description, ServletContext servletContext) {
    D registration = addRegistration(description, servletContext);
}

// org.springframework.boot.web.servlet.ServletRegistrationBean#addRegistration
protected ServletRegistration.Dynamic addRegistration(String description, ServletContext servletContext) {
    // 获取Servlet名称
    String name = getServletName();
    // 通过ServletContext#addServlet将Servlet注册到Servlet容器中
    return servletContext.addServlet(name, this.servlet);
}
```



此时我们再回头查看`DispatcherServletAutoConfiguration`中代码

```java
// 通过参数注入DispatcherServlet
public DispatcherServletRegistrationBean dispatcherServletRegistration(DispatcherServlet dispatcherServlet,
                WebMvcProperties webMvcProperties, ObjectProvider<MultipartConfigElement> multipartConfig) {
    
    // 通过构造将DispatcherServlet保存到DispatcherServletRegistrationBean中
    DispatcherServletRegistrationBean registration = new DispatcherServletRegistrationBean(dispatcherServlet,
                                              webMvcProperties.getServlet().getPath());
    
    // 设置ServletName为dispatcherServlet
    registration.setName(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME);
    registration.setLoadOnStartup(webMvcProperties.getServlet().getLoadOnStartup());
    multipartConfig.ifAvailable(registration::setMultipartConfig);
    return registration;
}
```



### 小结

至此，我们实现

- 通过自动配置方式注册了`DispatcherServlet`到IOC容器中

- 通过`ServletContext#addServlet`将`DispatcherServlet`实例部署到了`Servlet`容器中