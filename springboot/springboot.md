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



**注意**：获取`spring.factories`文件是在`target/classes/META-INF/`目录下，所以不是代码的`resources`目录表存在就可以加载，需要看是否打包进入



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



## SpringBoot数据源访问

### 数据源自动配置

#### 自动配置依赖

使用`SpringBoot`数据源自动配置，我们需要引入依赖`spring-boot-starter-jdbc`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```

`spring-boot-starter-jdbc`引入依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-jdbc</artifactId>
    </dependency>
</dependencies>
```

我们查看`spring-boot-starter-jdbc`依赖内容，其主要包括三部分

- `spring-boot-starter`
  - 自动配置`starter`
- `HikariCP`
  - `HikariCP`数据源相关依赖
- `spring-jdbc`
  - `spring`数据访问相关依赖



#### DataSourceAutoConfiguration

我们引入了数据源自动配置依赖，此时我们需要关注的是相关的自动配置类，它就是`DataSourceAutoConfiguration`

```
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
```



我们查看`DataSourceAutoConfiguration`类

```java
// org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })
@EnableConfigurationProperties(DataSourceProperties.class)
@Import({ DataSourcePoolMetadataProvidersConfiguration.class, DataSourceInitializationConfiguration.class })
public class DataSourceAutoConfiguration {
    
    @Configuration(proxyBeanMethods = false)
	@Conditional(PooledDataSourceCondition.class)
	@ConditionalOnMissingBean({ DataSource.class, XADataSource.class })
	@Import({ DataSourceConfiguration.Hikari.class, DataSourceConfiguration.Tomcat.class,
			DataSourceConfiguration.Dbcp2.class, DataSourceConfiguration.Generic.class,
			DataSourceJmxConfiguration.class })
	protected static class PooledDataSourceConfiguration {

	}
}
```



关于自动配置我们首要关注的两点：

- `DataSourceAutoConfiguration`类上通过`@EnableConfigurationProperties`引入`DataSourceProperties`属性配置类
- `PooledDataSourceConfiguration`内部类通过`@Import`引入`DataSourceConfiguration`的数据源相关的内部类



我们首先查看`DataSourceProperties`配置类

#### DataSourceProperties

```java
// org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
@ConfigurationProperties(prefix = "spring.datasource")
public class DataSourceProperties implements BeanClassLoaderAware, InitializingBean {
    // 数据库连接信息
    private String driverClassName;
    private String url;
    private String username;
    private String password;
    // 数据源类型
    private Class<? extends DataSource> type;
}
```



`DataSourceProperties`类主要通过`@ConfigurationProperties`注入前缀为`spring.datasource`的属性，保存数据库连接信息

所以我们通常使用自动配置数据源时，需要配置`spring.datasource`前缀的属性。例：

```properties
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://10.199.245.227:3307/tax_platform_admin?useUnicode=true&characterEncoding=utf-8&useSSL=true&serverTimezone=UTC
spring.datasource.username=apms
spring.datasource.password=apms123123
```



由于使用`mysql`数据库连接，所以要引入`mysql`数据库连接依赖

```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>
```



我们继续分析`DataSourceConfiguration`类

#### DataSourceConfiguration

```java
abstract class DataSourceConfiguration {

   @SuppressWarnings("unchecked")
   protected static <T> T createDataSource(DataSourceProperties properties, Class<? extends DataSource> type) {
      return (T) properties.initializeDataSourceBuilder().type(type).build();
   }

   /**
    * Tomcat 池数据源配置。
    */
   @Configuration(proxyBeanMethods = false)
   @ConditionalOnClass(org.apache.tomcat.jdbc.pool.DataSource.class)
   @ConditionalOnMissingBean(DataSource.class)
   @ConditionalOnProperty(name = "spring.datasource.type", havingValue = "org.apache.tomcat.jdbc.pool.DataSource",
         matchIfMissing = true)
   static class Tomcat {

      @Bean
      @ConfigurationProperties(prefix = "spring.datasource.tomcat")
      org.apache.tomcat.jdbc.pool.DataSource dataSource(DataSourceProperties properties) {
         org.apache.tomcat.jdbc.pool.DataSource dataSource = createDataSource(properties,
               org.apache.tomcat.jdbc.pool.DataSource.class);
         DatabaseDriver databaseDriver = DatabaseDriver.fromJdbcUrl(properties.determineUrl());
         String validationQuery = databaseDriver.getValidationQuery();
         if (validationQuery != null) {
            dataSource.setTestOnBorrow(true);
            dataSource.setValidationQuery(validationQuery);
         }
         return dataSource;
      }

   }

   /**
    * Hikari 数据源配置。
    */
   @Configuration(proxyBeanMethods = false)
   @ConditionalOnClass(HikariDataSource.class)
   @ConditionalOnMissingBean(DataSource.class)
   @ConditionalOnProperty(name = "spring.datasource.type", havingValue = "com.zaxxer.hikari.HikariDataSource",
         matchIfMissing = true)
   static class Hikari {

      @Bean
      @ConfigurationProperties(prefix = "spring.datasource.hikari")
      HikariDataSource dataSource(DataSourceProperties properties) {
         HikariDataSource dataSource = createDataSource(properties, HikariDataSource.class);
         if (StringUtils.hasText(properties.getName())) {
            dataSource.setPoolName(properties.getName());
         }
         return dataSource;
      }

   }

   /**
    * DBCP 数据源配置。
    */
   @Configuration(proxyBeanMethods = false)
   @ConditionalOnClass(org.apache.commons.dbcp2.BasicDataSource.class)
   @ConditionalOnMissingBean(DataSource.class)
   @ConditionalOnProperty(name = "spring.datasource.type", havingValue = "org.apache.commons.dbcp2.BasicDataSource",
         matchIfMissing = true)
   static class Dbcp2 {

      @Bean
      @ConfigurationProperties(prefix = "spring.datasource.dbcp2")
      org.apache.commons.dbcp2.BasicDataSource dataSource(DataSourceProperties properties) {
         return createDataSource(properties, org.apache.commons.dbcp2.BasicDataSource.class);
      }

   }

   /**
    * 通用数据源配置。
    */
   @Configuration(proxyBeanMethods = false)
   @ConditionalOnMissingBean(DataSource.class)
   @ConditionalOnProperty(name = "spring.datasource.type")
   static class Generic {

      @Bean
      DataSource dataSource(DataSourceProperties properties) {
         return properties.initializeDataSourceBuilder().build();
      }

   }

}
```



`DataSourceConfiguration`是一个抽象类，且其并不是配置类，所以我们主要分析其中的内部类



#### 默认数据源

在`SpringBoot`数据源自动配置中，存在几个默认的数据源，它们分别对应`DataSourceConfiguration`类中不同的配置类

| 数据源                      | 内部配置类                           |
| --------------------------- | ------------------------------------ |
| HikariCP                    | DataSourceConfiguration.Hikari.class |
| Commons DBCP2               | DataSourceConfiguration.Dbcp2.class  |
| Tomcat JDBC Connection Pool | DataSourceConfiguration.Tomcat.class |



在三个内部配置类上，均通过`Condition`条件注解进行条件注入，其中都通过`@ConditionalOnClass`注解定义仅当当前数据源对应类存在时加载配置



从引入的`spring-boot-starter-jdbc`依赖中，我们可以发现`SpringBoot`默认引入`HikariCP`依赖，所以`SpringBoot`默认注入`HikariDataSource`数据源

我们可以通过自动注入的方式获取数据源进行数据操作

```java
@RunWith(SpringRunner.class)
@SpringBootTest
class SpringBootMytestDatasourceApplicationTests {

    // 通过@Autowired自动注入数据源
	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoads() throws SQLException {
		Connection connection = dataSource.getConnection();
		System.out.println(connection);
	}

}
```



#### 切换默认数据源

虽然`SpringBoot`默认引入`HikariCP`数据源依赖，我们也可以通过替换数据源依赖切换为其他两种默认数据源

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
    <!--排除默认HikariCP依赖-->
    <exclusions>
        <exclusion>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- 1. 引入Tomcat JDBC Connection Pool依赖-->
<dependency>
    <groupId>org.apache.tomcat</groupId>
    <artifactId>tomcat-jdbc</artifactId>
</dependency>

<!-- 2. 引入Commons DBCP2依赖-->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-dbcp2</artifactId>
</dependency>
```



#### 数据库连接信息注入

`DataSourceConfiguration`的内部配置类均通过`dataSource`方法，创建一个`DataSource`对象，且其都注入一个`DataSourceProperties`对象作为参数

所以我们可以普遍使用`spring.datasource`前缀配置数据库连接信息

但是对于默认的数据源，在`dataSource`方法上都存在各自独特的`@ConfigurationProperties`注解前缀，我们也可以使用它们进行数据源连接信息配置

| 数据源                      | 前缀                     |
| --------------------------- | ------------------------ |
| HikariCP                    | spring.datasource.hikari |
| Commons DBCP2               | spring.datasource.dbcp2  |
| Tomcat JDBC Connection Pool | spring.datasource.tomcat |



#### 数据源类型

通过分析`DataSourceConfiguration`的内部配置类，它们都存在一个`@ConditionalOnProperty(name = "spring.datasource.type")`注解

主要通过`spring.datasource.type`属性控制数据源类型

对于默认数据源配置时，`spring.datasource.type`属性可以省略，因为在`@ConditionalOnProperty`注解中`matchIfMissing = true`



对应于默认数据源，其`type`不同

| 数据源                      | spring.datasource.type                   |
| --------------------------- | ---------------------------------------- |
| HikariCP                    | com.zaxxer.hikari.HikariDataSource       |
| Commons DBCP2               | org.apache.commons.dbcp2.BasicDataSource |
| Tomcat JDBC Connection Pool | org.apache.tomcat.jdbc.pool.DataSource   |



#### `Druid`连接池的配置

通常我们项目使用时，不一定都使用`SpringBoot`默认的三种数据源，此时将使用自定义的数据源类型

例如我们通常使用`Druid`连接池，那么我们将如何配置使用



##### 1. 引入依赖

`SpringBoot`存在`druid`连接池的自动配置`starter`

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid-spring-boot-starter</artifactId>
    <version>1.1.10</version>
</dependency>
```



##### 2. 配置连接属性

```yaml
spring:
    datasource:
        username: root
        password: root
        url: jdbc:mysql:///springboot_h?useUnicode=true&characterEncoding=utf8&useSSL=true&serverTimezone=UTC
        driver-class-name: com.mysql.cj.jdbc.Driver
        initialization-mode: always
        
        # 使用druid数据源
        type: com.alibaba.druid.pool.DruidDataSource
        
        # 数据源其他配置
        initialSize: 5
        minIdle: 5
        maxActive: 20
        maxWait: 60000
        timeBetweenEvictionRunsMillis: 60000
        minEvictableIdleTimeMillis: 300000
        validationQuery: SELECT 1 FROM DUAL
        testWhileIdle: true
        testOnBorrow: false
        testOnReturn: false
        poolPreparedStatements: true
        
        # 配置监控统计拦截的filters，去掉后监控界面sql无法统计，'wall'用于防火墙
        filters: stat,wall,log4j
        maxPoolPreparedStatementPerConnectionSize: 20
        useGlobalDataSourceStat: true
        connectionProperties:
			druid.stat.mergeSql=true;druid.stat.slowSqlMillis=500
```

可以发现，在上述配置中，我们通过`spring.datasource.type=com.alibaba.druid.pool.DruidDataSource`配置了数据源类型



其对应于`DataSourceConfiguration`中第四个内部类`Generic`，`Generic`主要用于实现除三种默认数据源以外的其他类型数据源配置

```java
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(DataSource.class)
@ConditionalOnProperty(name = "spring.datasource.type")
static class Generic {

    @Bean
    DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

}
```



此时我们发现数据源可用，但是存在问题：对于我们配置的属性，并没有全部在自动注入的`Datasource`中体现

![image-20210823211353083](.\图片\自动配置自定义数据源部分属性不生效)

原因是对于自定义数据源配置，它并没有将所有属性与`DataSourceProperties`对象属性进行映射绑定，所以将存在部分属性无法与配置一致

此时我们应当编写代码整合配置



##### 3. 整合druid数据源

```java
public class DruidConfig {
    
    @ConfigurationProperties(prefix = "spring.datasource")
    @Bean
    public DataSource druid(){
        return new DruidDataSource();
    }
}
```

通过`@ConfigurationProperties(prefix = "spring.datasource")`注解，将`spring.datasource`为前缀的属性配置注入到返回的`DataSource`中



##### 4. 日志适配器

此时调试时将出现报错，分析为在数据源配置的`yaml`文件中存在如下内容

```yaml
# 配置监控统计拦截的filters，去掉后监控界面sql无法统计，'wall'用于防火墙
filters: stat,wall,log4j
```



报错原因是：`springBoot2.0`以后使用的日志框架已经不再使用`log4j`了。此时应该引入相应的适配器

```xml
<!--引入适配器-->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-log4j12</artifactId>
</dependency>
```



## SpringBoot整合Mybatis

由前面分析大致可知，我们先要引入自动配置依赖

### 自动配置依赖

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>1.3.2</version>
</dependency>
```



我们可以发现`Mybatis`自动配置依赖：

- 是`xxx-spring-boot-starter`的自定义`starter`风格
- 由配置对应`version`

原因是，`SpringBoot`官方并没有实现整合`Mybatis`的自动配置`starter`，所以`Mybatis`官方就自己整了一个自定义`starter`



我们继续分析这个自定义`starter`

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <!-- 引入 数据源自动配置starter -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-autoconfigure</artifactId>
    </dependency>
    <dependency>
        <groupId>org.mybatis</groupId>
        <artifactId>mybatis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.mybatis</groupId>
        <artifactId>mybatis-spring</artifactId>
    </dependency>
</dependencies>
```

我们发现其引入了`spring-boot-starter-jdbc`，结合上章分析，我们可以通过配置`spring.datasource`向IOC容器中注册`DataSource`



### spring.factories配置文件

由于`Mybatis`不是官方实现`starter`，所以在官方的`spring.factories`配置文件中没有配置其自动配置类，所以`mybatis`官方在其自定义`starter`中添加了自己的`spring.factories`配置文件

```bash
# Auto Configure
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration
```

它就表明了`mybatis`的自动配置类是`MybatisAutoConfiguration`



### MybatisAutoConfiguration

```java
@org.springframework.context.annotation.Configuration
@EnableConfigurationProperties(MybatisProperties.class)
public class MybatisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        // 省略...
    }
}
```



我们分析`MybatisAutoConfiguration`类中，我们主要关注两点

- 添加了`@Configuration`，是一个配置类

- 通过`@EnableConfigurationProperties`注解，向IOC容器注册`MybatisProperties`实例
- 通过添加`@Bean`的`sqlSessionFactory`方法，向IOC容器注册`SqlSessionFactory`实例



#### 1. 配置属性加载

我们先分析一下`MybatisProperties`类

##### MybatisProperties

```java
@ConfigurationProperties(prefix = MybatisProperties.MYBATIS_PREFIX)
public class MybatisProperties {
    // 属性前缀为mybatis
    public static final String MYBATIS_PREFIX = "mybatis";
    
    // mybatis配置文件位置
    private String configLocation;
    
    // mapper.xml配置文件位置
    private String[] mapperLocations;
    
    // Configuration实例引用
    private Configuration configuration;
}
```

`MybatisProperties`类主要通过`@ConfigurationProperties`注解，注入了以`mybatis`为前缀的属性



#### 2. SqlSessionFactory创建

下面我们来分析一下`sqlSessionFactory`方法

##### sqlSessionFactory方法

```java
// org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration#sqlSessionFactory
@Bean
@ConditionalOnMissingBean
public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
    // 1. 创建SqlSessionFactoryBean实例
    SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
    // 设置数据源
    factory.setDataSource(dataSource);
    
    // 2. 加载mybatis配置文件资源
    // 如果配置了mybatis.configLocation属性，则加载对应，加载对应mybatis配置文件
    if (StringUtils.hasText(this.properties.getConfigLocation())) {
        // 保存到SqlSessionFactoryBean实例中
        factory.setConfigLocation(this.resourceLoader.getResource(this.properties.getConfigLocation()));
    }
    
    // 3. 获取Configuration
    Configuration configuration = this.properties.getConfiguration();
    if (configuration == null && !StringUtils.hasText(this.properties.getConfigLocation())) {
        // 3.1 如果没有配置mybatis.configLocation，则创建默认Configuration
        // 3.2 如果有配置mybatis.configLocation，则将通过SqlSessionFactoryBean#buildSqlSessionFactory中处理创建configuration实例
        configuration = new Configuration();
    }
    
    // 4.将Configuration保存到SqlSessionFactoryBean实例中
    factory.setConfiguration(configuration);

    // 4. 属性设置到SqlSessionFactoryBean实例
    if (this.properties.getConfigurationProperties() != null) {
      factory.setConfigurationProperties(this.properties.getConfigurationProperties());
    }
    if (!ObjectUtils.isEmpty(this.interceptors)) {
      factory.setPlugins(this.interceptors);
    }
    if (this.databaseIdProvider != null) {
      factory.setDatabaseIdProvider(this.databaseIdProvider);
    }
    if (StringUtils.hasLength(this.properties.getTypeAliasesPackage())) {
      factory.setTypeAliasesPackage(this.properties.getTypeAliasesPackage());
    }
    if (StringUtils.hasLength(this.properties.getTypeHandlersPackage())) {
      factory.setTypeHandlersPackage(this.properties.getTypeHandlersPackage());
    }
    if (!ObjectUtils.isEmpty(this.properties.resolveMapperLocations())) {
      factory.setMapperLocations(this.properties.resolveMapperLocations());
    }

    // 5. 由SqlSessionFactoryBean#getObject获取SqlSessionFactory实例，注册到IOC容器中
    return factory.getObject();
}
```

`sqlSessionFactory`主要是构建了一个`SqlSessionFactoryBean`实例，并通过其`getObject`方法获取`SqlSessionFactory`实例注册到IOC容器中



#### 3. @Mapper加载

此时已经注册了`SqlSessionFactory`实例，那么我们将如何扫描对应的`mapper`接口，注册`mapper`代理对象到IOC容器中？

`Mybatis`对于此提供了两种方案：

- 通过在`mapper`接口上添加`@Mapper`注解实现
- 通过在配置类上添加`@MapperScan`注解实现

下面我们分别分析



##### @Mapper

```java
// org.apache.ibatis.annotations.Mapper
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
public @interface Mapper {}
```

我们可以发现`@Mapper`注解只是一个空注解，用于标志使用，那么我们需要分析如何加载添加了`@Mapper`注解的`mapper`接口



在`MybatisAutoConfiguration`自动配置类中，存在一个静态内部类`MapperScannerRegistrarNotFoundConfiguration`

##### MapperScannerRegistrarNotFoundConfiguration

```java
//org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration.MapperScannerRegistrarNotFoundConfiguration
@org.springframework.context.annotation.Configuration
@Import({ AutoConfiguredMapperScannerRegistrar.class })
@ConditionalOnMissingBean(MapperFactoryBean.class)
public static class MapperScannerRegistrarNotFoundConfiguration {
   @PostConstruct
   public void afterPropertiesSet() {
   		logger.debug("No {} found.", MapperFactoryBean.class.getName());
   }
}
```

虽然`MapperScannerRegistrarNotFoundConfiguration`有个`afterPropertiesSet`方法，但是却没有做任何工作

所以我们分析其上注解

- `@Configuration`
  - 标志为配置类
- `@ConditionalOnMissingBean(MapperFactoryBean.class)`
  - 在IOC容器中没有`MapperFactoryBean`类型`bean`时，将加载此配置类
- `@Import({ AutoConfiguredMapperScannerRegistrar.class })`
  - 通过`@Import`注解导入`AutoConfiguredMapperScannerRegistrar`类



所以我们主要分析`AutoConfiguredMapperScannerRegistrar`类

##### AutoConfiguredMapperScannerRegistrar

```java
// org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration.AutoConfiguredMapperScannerRegistrar
public static class AutoConfiguredMapperScannerRegistrar
    implements BeanFactoryAware, ImportBeanDefinitionRegistrar, ResourceLoaderAware {

    private BeanFactory beanFactory;

    private ResourceLoader resourceLoader;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		// 创建一个ClassPathMapperScanner实例
        ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry);

        try {
            if (this.resourceLoader != null) {
                // 设置类加载器
                scanner.setResourceLoader(this.resourceLoader);
            }
			// 1. 获取扫描的包
            List<String> packages = AutoConfigurationPackages.get(this.beanFactory);
            if (logger.isDebugEnabled()) {
                for (String pkg : packages) {
                    logger.debug("Using auto-configuration base package '{}'", pkg);
                }
            }
			
            // 设置扫描的注解类
            scanner.setAnnotationClass(Mapper.class);
            // 2. 注册过滤器
            scanner.registerFilters();
            // 3. 进行包扫描
            scanner.doScan(StringUtils.toStringArray(packages));
        } catch (IllegalStateException ex) {
            logger.debug("Could not determine auto-configuration package, automatic mapper scanning disabled.", ex);
        }
    }
}
```

`AutoConfiguredMapperScannerRegistrar`实现了`ImportBeanDefinitionRegistrar`接口，所以将通过其`registerBeanDefinitions`注册`BeanDefinition`，在`registerBeanDefinitions`方法中，我们需要关注三点：

- 获取扫描的包
- 设置过滤注解类
- 执行包扫描



###### 3.1 获取扫描的包

在上述代码中，我们发现其通过`AutoConfigurationPackages.get(this.beanFactory)`这样一段代码获取扫描的基础包

实际调用

```java
public abstract class AutoConfigurationPackages {

    private static final String BEAN = AutoConfigurationPackages.class.getName();

    public static List<String> get(BeanFactory beanFactory) {
        try {
            // 从IOC容器中获取类型为BasePackages的bean，并调用其get方法
            return beanFactory.getBean(BEAN, BasePackages.class).get();
        }
        catch (NoSuchBeanDefinitionException ex) {
            throw new IllegalStateException("Unable to retrieve @EnableAutoConfiguration base packages");
        }
    }
}
```



由上可知，该步骤执行了两个

- 从IOC容器中获取`beanName`为`org.springframework.boot.autoconfigure.AutoConfigurationPackages`，类型为`BasePackages`的`bean`
- 调用该`bean`的`get`方法



那么这个`bean`是怎么注册到IOC容器？在我们前面分析主配置类时，在`@SpringBootApplication`注解中，通过`@AutoConfigurationPackage`注解中的`@Import`注解，注册了一个`AutoConfigurationPackages.Registrar.class`，在其`registerBeanDefinitions`方法调用中，向IOC容器注册了一个类型为`BasePackages`的`BeanDefinition`，且其对应的`packages`就是对应`@SpringBootApplication`注解中`@ComponentScan`的`basePackages`属性



**结论：**所以我们在此获取的`packages`就是主配置类的`basePackages`属性值，并且，如果没有配置时，将取主配置类所在包



###### 3.2 设置过滤类

在上述代码中，先通过`scanner.setAnnotationClass(Mapper.class)`设置我们扫描的注解类型为`@Mapper`

之后调用`scanner.registerFilters()`方法，我们分析

```java
// org.mybatis.spring.mapper.ClassPathMapperScanner#setAnnotationClass
public void setAnnotationClass(Class<? extends Annotation> annotationClass) {
    this.annotationClass = annotationClass;
}

// org.mybatis.spring.mapper.ClassPathMapperScanner#registerFilters
public void registerFilters() {
    if (this.annotationClass != null) {
        addIncludeFilter(new AnnotationTypeFilter(this.annotationClass));
        acceptAllInterfaces = false;
    }
}
```

可知，我们将通过包含过滤器，扫描主配置类所在包下的添加了`@Mapper`注解的类



###### 3.3  进行包扫描

在前面分析`Spring`整合`Mybatis`过程中，我们已经分析了`ClassPathMapperScanner#doScan`方法，其主要作用就是将扫描的接口类注册到IOC容器中，类型为`MapperFactoryBean`

我们创建`ClassPathMapperScanner`时，并没有设置任何对应于`sqlSessionFactory、sqlSessionTemplate`的信息，所以将会将对应`BeanDefinition`的`autowireMode`设置为`AUTOWIRE_BY_TYPE`，并且我们通过`MybatisAutoConfiguration`注册了`SqlSessionFactory`，所以将自动执行其`setSqlSessionFactory`方法进行自动注入

且对应`mapper.xml`配置将通过`Configuration#addMapper`调用时进行扫描，所以需要确保对应`mapper`接口和`.xml`配置文件层级一致



#### 4. @MapperScan扫描

可能需要对每个`Mapper`接口都添加`@Mapper`注解比较繁琐，那么此时的替代方案就是通过`@MapperScan`注解进行扫描

```java
// org.mybatis.spring.annotation.MapperScan
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(MapperScannerRegistrar.class)
public @interface MapperScan {
    String[] value() default {};
    String[] basePackages() default {};
}
```

`@MapperScan`主要通过`@Import`注解导入了`MapperScannerRegistrar`类型实例，我们分析一下



##### MapperScannerRegistrar

```java
// org.mybatis.spring.annotation.MapperScannerRegistrar
public class MapperScannerRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        // 1. 获取@MapperScan注解属性
        AnnotationAttributes annoAttrs = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(MapperScan.class.getName()));

        // 2. 创建ClassPathMapperScanner实例
        ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry);

        // this check is needed in Spring 3.1
        if (resourceLoader != null) {
            scanner.setResourceLoader(resourceLoader);
        }
		
        // 分析@MapperScan注解属性，获取扫描范围
        Class<? extends Annotation> annotationClass = annoAttrs.getClass("annotationClass");
        if (!Annotation.class.equals(annotationClass)) {
            scanner.setAnnotationClass(annotationClass);
        }

        Class<?> markerInterface = annoAttrs.getClass("markerInterface");
        if (!Class.class.equals(markerInterface)) {
            scanner.setMarkerInterface(markerInterface);
        }

        Class<? extends BeanNameGenerator> generatorClass = annoAttrs.getClass("nameGenerator");
        if (!BeanNameGenerator.class.equals(generatorClass)) {
            scanner.setBeanNameGenerator(BeanUtils.instantiateClass(generatorClass));
        }

        Class<? extends MapperFactoryBean> mapperFactoryBeanClass = annoAttrs.getClass("factoryBean");
        if (!MapperFactoryBean.class.equals(mapperFactoryBeanClass)) {
            scanner.setMapperFactoryBean(BeanUtils.instantiateClass(mapperFactoryBeanClass));
        }

        scanner.setSqlSessionTemplateBeanName(annoAttrs.getString("sqlSessionTemplateRef"));
        scanner.setSqlSessionFactoryBeanName(annoAttrs.getString("sqlSessionFactoryRef"));

        List<String> basePackages = new ArrayList<String>();
        for (String pkg : annoAttrs.getStringArray("value")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }
        for (String pkg : annoAttrs.getStringArray("basePackages")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }
        for (Class<?> clazz : annoAttrs.getClassArray("basePackageClasses")) {
            basePackages.add(ClassUtils.getPackageName(clazz));
        }
        // 4. 注册过滤器
        scanner.registerFilters();
        // 5. 进行扫描
        scanner.doScan(StringUtils.toStringArray(basePackages));
    }
}
```



`MapperScannerRegistrar`实现了`ImportBeanDefinitionRegistrar`接口，所以执行其`registerBeanDefinitions`方法，实际调用内容也就是通过`ClassPathMapperScanner`进行`mapper`扫描，我们不再重复分析



## 动态数据源

对于多数据库项目而言，经常不是只使用一个数据库，而对于`mybatis`操作而言，一个数据源对应一个数据库连接，我们进行`mapper`操作时，将从`DataSource`中获取`Connection`连接进行数据库操作，此时，将如何从多个数据库中进行数据获取？



**思考**：想从多个数据库中进行数据获取，则需要多个数据库连接配置，则对应创建多个数据源`DataSource`对象

但是在`Mybatis`分析中，对应于全局`Configuration`对象，其中保存了`DataSource`数据源实例，所以我们无法在操作时动态替换`Configuration`对象中的`DataSource`，那么就需要从另一个方面入手：

使用个包装的`DataSource`实例，由它管理多个数据源对象，在进行数据库操作时，能动态获取所需数据源对象，并从中获取对应`Connection`实例进行数据库操作



在`Spring-jdbc`包中，存在这样一个抽象类`AbstractRoutingDataSource`，我们查看其类图

![image-20210831213047164](D:\学习整理\summarize\springboot\图片\AbstractRoutingDataSource类图)

它实现了`javax.sql.DataSource`接口，所以它的子类可以作为一个`DataSource`实例保存到`Configuration`中



### AbstractRoutingDataSource

下面我们分析其代码

```java
// org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource
public abstract class AbstractRoutingDataSource extends AbstractDataSource implements InitializingBean {
    // 目标数据源
	private Map<Object, Object> targetDataSources;
    
    // 转换的数据源
	private Map<Object, DataSource> resolvedDataSources;
    
    // 默认数据源
	private DataSource resolvedDefaultDataSource;
}
```



### 1. 初始化数据源

由于`AbstractRoutingDataSource`实现了`InitializingBean`接口，我们分析其`afterPropertiesSet`方法

#### afterPropertiesSet

```java
// org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource#afterPropertiesSet
public void afterPropertiesSet() {
    // 确保targetDataSources不能为空
    if (this.targetDataSources == null) {
        throw new IllegalArgumentException("Property 'targetDataSources' is required");
    }
    // 创建resolvedDataSources
    this.resolvedDataSources = new HashMap<>(this.targetDataSources.size());
    // 遍历targetDataSources
    this.targetDataSources.forEach((key, value) -> {
        // 1. 通过resolveSpecifiedLookupKey方法进行key的转换，默认实现是不做处理的
        Object lookupKey = resolveSpecifiedLookupKey(key);
        // 2. 通过resolveSpecifiedDataSource进行数据源对象转换
        DataSource dataSource = resolveSpecifiedDataSource(value);
        // 3. 将转换结果保存到resolvedDataSources中
        this.resolvedDataSources.put(lookupKey, dataSource);
    });
    if (this.defaultTargetDataSource != null) {
        this.resolvedDefaultDataSource = resolveSpecifiedDataSource(this.defaultTargetDataSource);
    }
}
```

在`afterPropertiesSet`的处理中，主要是将`targetDataSources`集合中实例转换后保存到`resolvedDataSources`集合中，因为`targetDataSources`集合的`value`为`Object`类型，并不是`DataSource`实例



### 2. 连接获取

#### getConnection

对于一个数据源来说，我们通常的操作就是通过`getConnection`方法获取数据连接`Connection`实例，下面我们查看一下`getConnection`方法

```java
// org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource#getConnection()
public Connection getConnection() throws SQLException {
    return determineTargetDataSource().getConnection();
}
```



在`getConnection`方法中，它主要通过`determineTargetDataSource`方法获取数据源实例，再进行连接获取

#### determineTargetDataSource

```java
protected DataSource determineTargetDataSource() {
    Assert.notNull(this.resolvedDataSources, "DataSource router not initialized");
    // 1. 通过determineCurrentLookupKey方法获取数据源对应key
    Object lookupKey = determineCurrentLookupKey();
    // 2. 由获取的key值，从resolvedDataSources中获取对应数据源
    DataSource dataSource = this.resolvedDataSources.get(lookupKey);
    // 3. 如果获取不到，则使用默认数据源
    if (dataSource == null && (this.lenientFallback || lookupKey == null)) {
        dataSource = this.resolvedDefaultDataSource;
    }
    if (dataSource == null) {
        throw new IllegalStateException("Cannot determine target DataSource for lookup key [" + lookupKey + "]");
    }
    return dataSource;
}
```



在`determineTargetDataSource`中，主要注意的方法就是`determineCurrentLookupKey`，我们由其获取数据源对应`key`值

#### determineCurrentLookupKey

```java
// org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource#determineCurrentLookupKey
protected abstract Object determineCurrentLookupKey();
```



`determineCurrentLookupKey`方法实质是一个抽象方法，由子类具体实现



### 3.  如何使用

#### 3.1 数据源连接配置

对于多数据源而言，首先就是其对应有多个数据库连接信息，所以我们需要在配置文件中通过不同标志的`key`配置对应的连接信息，供后续数据源创建使用



#### 3.2 注册数据源

在配置了多个数据库连接信息后，我们就要提取对应配置信息，创建对应的数据源实例，注册到IOC容器中



#### 3.3 创建AbstractRoutingDataSource实例

由于我们需要通过`AbstractRoutingDataSource`实例进行多数据源的整合

- 创建子类，继承`AbstractRoutingDataSource`，实现具体方法(视具体业务使用而定)
- 在创建`AbstractRoutingDataSource`实例时，通过`setTargetDataSources`方法，将多数据源挂载到`AbstractRoutingDataSource`实例中
- 由于此时存在多个数据源，创建`SqlSessionFactoryBean`时进行自动注入将会冲突，需要将`AbstractRoutingDataSource`实例添加`@Primary`注解首选



#### 3.4 取消SpringBoot数据源自动配置

通过在`@SpringBootApplication`注解中添加`exclude`属性，排除`DataSourceAutoConfiguration`自动配置类



### 总结

动态数据源，实际就是通过`AbstractRoutingDataSource`子类封装多个数据源，根据业务在`Mybatis`进行数据库操作时，从中获取不同的数据源进行处理

具体对应的`key`值的存储于匹配，实际是根据项目而定，并不是固定的



## SpringBoot缓存

在`SpringBoot`中，我们可以简单的通过注解就可以实现方法返回结果的缓存



### 依赖

我们需要添加缓存自动配置`starter`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```



### Cache注解

对于Spring缓存的使用，我们通常通过注解的方式进行引用，那么我们就来分析一下具体的缓存注解



#### @Cacheable

```java
// org.springframework.cache.annotation.Cacheable
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Cacheable {
    
}
```

`@Cacheable`的作用：

- 根据方法结合方法入参作为key信息，将方法返回结果进行缓存，在下次查询时，如果缓存中存在，则无需进行方法调用，直接从缓存中获取结果进行返回
- 通常用于**查询**方法上



#### @CachePut

```java
// org.springframework.cache.annotation.CachePut
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface CachePut {
    
}
```

`@CachePut`的作用：

- 使用该注解注释的方法，每次都会执行，并且将方法返回结果缓存
- 通常用于**新增**方法
- 目的是将新增数据缓存，供其他查询方法使用



#### @CacheEvict

```java
// org.springframework.cache.annotation.CacheEvict
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface CacheEvict {
    
}
```

`@CacheEvict`的作用：

- 使用该注解注释的方法，每次都会执行，并且将缓存中指定key值清除
- 通常用于**更新/删除**方法
- 目的是将修改的数据从缓存中清除



#### @Caching

```java
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Caching {

	Cacheable[] cacheable() default {};

	CachePut[] put() default {};

	CacheEvict[] evict() default {};

}
```

从`@Caching`内容可知，`@Caching`注解主要用于整合上述的三个缓存注解，实现同一方法上可以使用多个相同的缓存注解



#### 注解属性

前面发现我在介绍缓存注解时，并没有一一介绍注解的相关属性，是因为上述三个缓存注解拥有大致相同的注解属性，所以结合一起进行介绍

##### value/cacheNames

```java
@AliasFor("cacheNames")
String[] value() default {};

@AliasFor("value")
String[] cacheNames() default {};
```

- `value`与`cacheNames`是等同的 

- 主要用于标志当前缓存的命名空间
- 可以是多个值



##### key

```java
String key() default "";
```

- `key`用于标志缓存信息在缓存中的标志`key`键
- `key`可以结合使用 `SpEL` 标签
- `key`是可选的



###### key对应SpEL标签

在定义`key`时，我们可以使用方法参数及方法参数对应属性(不可以对应包装对象)作为`key`的内容

| 参数     | 参数示例   | 对应表达式格式      | 表达式示例 |
| -------- | ---------- | ------------------- | ---------- |
| 普通参数 | Integer id | #参数名             | #id        |
| 普通参数 | Integer id | #p序号，序号从0开始 | #p0        |
| 包装对象 | User user  | #参数名.属性名      | #user.name |
| 包装对象 | User user  | #p序号.属性名       | #p0.name   |



###### Spring预设root对象使用

| 属性名称    | 描述                        | 示例                 |
| ----------- | --------------------------- | -------------------- |
| methodName  | 当前方法名                  | #root.methodName     |
| method      | 当前方法                    | #root.method.name    |
| target      | 当前被调用的对象            | #root.target         |
| targetClass | 当前被调用的对象的class     | #root.targetClass    |
| args        | 当前方法参数组成的数组      | #root.args[0]        |
| caches      | 当前被调用的方法使用的Cache | #root.caches[0].name |



##### keyGenerator

```java
String keyGenerator() default "";
```

- 用于使用`beanName`引用IOC容器中`org.springframework.cache.interceptor.KeyGenerator`类型`bean`
- 与`key`属性互斥，替代`key`的自定义生成规则，而是使用`KeyGenerator`实例自动生成对应`key`值



##### cacheManager

```java
String cacheManager() default "";
```

- 用于使用`beanName`引用IOC容器中`org.springframework.cache.CacheManager`类型`bean`

- 指定缓存管理器



##### cacheResolver

```java
String cacheResolver() default "";
```

- 用于使用`beanName`引用IOC容器中`org.springframework.cache.interceptor.CacheResolver`类型`bean`
- 指定获取解析器



### Cache

```java
// 
public interface Cache {
    // 获取缓存name命名空间
	String getName();
    
    // 返回缓存底层机制
    Object getNativeCache();
    
    // 获取缓存中key对应值
    <T> T get(Object key, @Nullable Class<T> type);
    
    // 向缓存中存储
    void put(Object key, @Nullable Object value);
}
```



### CacheManager

```java
// org.springframework.cache.CacheManager
public interface CacheManager {
	
    // 根据name命名空间，获取缓存Cache实例
	Cache getCache(String name);
	
    // 获取当前管理的缓存的集合
	Collection<String> getCacheNames();

}
```

`CacheManager`是Spring中用于管理缓存`Cache`的的顶级接口









### 1. 缓存启用



#### @EnableCaching

在使用时，我们需要通过`@EnableCaching`注解来开启缓存功能，我们先分析此注解

```java
// org.springframework.cache.annotation.EnableCaching
@Import(CachingConfigurationSelector.class)
public @interface EnableCaching {
    // 是否基于CGLIB动态代理，默认为false，表示使用JDK动态代理
    boolean proxyTargetClass() default false;
    
    // 指示如何使用缓存
    AdviceMode mode() default AdviceMode.PROXY;
}
```

`@EnableCaching`注解主要通过`@Import`注解导入`CachingConfigurationSelector`类，我们继续分析



#### CachingConfigurationSelector

我们先查看其类图

![image-20210907203006975](D:\学习整理\summarize\springboot\图片\CachingConfigurationSelector类图)

我们可以发现其实现了`ImportSelector`接口，所以我主要分析其`selectImports`方法，其具体实现在其抽象父类`AdviceModeImportSelector`中

##### AdviceModeImportSelector

```java
// org.springframework.context.annotation.AdviceModeImportSelector
public abstract class AdviceModeImportSelector<A extends Annotation> implements ImportSelector {
    
    // 实现自ImportSelector接口
    public final String[] selectImports(AnnotationMetadata importingClassMetadata) {
        // 1. 获取引入的注解类，即@EnableCaching
        Class<?> annType = GenericTypeResolver.resolveTypeArgument(getClass(), AdviceModeImportSelector.class);
        // 2. 获取注解属性
        AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(importingClassMetadata, annType);
        // 3. 获取@EnableCaching注解的mode属性
        AdviceMode adviceMode = attributes.getEnum(getAdviceModeAttributeName());
        // 4. 调用重载selectImports方法
        String[] imports = selectImports(adviceMode);
        // 5. 返回导入类
        return imports;
    }
}
```

在`selectImports`中，主要先获取`@EnableCaching`注解的`mode`属性，再调用重载`selectImports`方法



具体重载在`CachingConfigurationSelector`类中

```java
// org.springframework.cache.annotation.CachingConfigurationSelector
public class CachingConfigurationSelector extends AdviceModeImportSelector<EnableCaching> {

    public String[] selectImports(AdviceMode adviceMode) {
        switch (adviceMode) {
            case PROXY:
                return getProxyImports();
            case ASPECTJ:
                return getAspectJImports();
            default:
                return null;
        }
    }

    private String[] getProxyImports() {
        List<String> result = new ArrayList<>(3);
        result.add(AutoProxyRegistrar.class.getName());
        result.add(ProxyCachingConfiguration.class.getName());
        if (jsr107Present && jcacheImplPresent) {
            result.add(PROXY_JCACHE_CONFIGURATION_CLASS);
        }
        return StringUtils.toStringArray(result);
    }

    private String[] getAspectJImports() {
        List<String> result = new ArrayList<>(2);
        result.add(CACHE_ASPECT_CONFIGURATION_CLASS_NAME);
        if (jsr107Present && jcacheImplPresent) {
            result.add(JCACHE_ASPECT_CONFIGURATION_CLASS_NAME);
        }
        return StringUtils.toStringArray(result);
    }
}
```

由于默认`mode`属性值为`AdviceMode.PROXY`，所以默认走`getProxyImports`，将导入一下两个类

- `AutoProxyRegistrar`
- `ProxyCachingConfiguration`



### 2. 引入解析

#### AutoProxyRegistrar

```java
// org.springframework.context.annotation.ImportBeanDefinitionRegistrar
public class AutoProxyRegistrar implements ImportBeanDefinitionRegistrar {
    
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        boolean candidateFound = false;
        Set<String> annTypes = importingClassMetadata.getAnnotationTypes();
        for (String annType : annTypes) {
            AnnotationAttributes candidate = AnnotationConfigUtils.attributesFor(importingClassMetadata, annType);
            if (candidate == null) {
                continue;
            }
            Object mode = candidate.get("mode");
            Object proxyTargetClass = candidate.get("proxyTargetClass");
            if (mode != null && proxyTargetClass != null && AdviceMode.class == mode.getClass() &&
                Boolean.class == proxyTargetClass.getClass()) {
                candidateFound = true;
                if (mode == AdviceMode.PROXY) {
                    AopConfigUtils.registerAutoProxyCreatorIfNecessary(registry);
                    if ((Boolean) proxyTargetClass) {
                        AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
                        return;
                    }
                }
            }
        }
    }
}
```

`AutoProxyRegistrar`实现了`ImportBeanDefinitionRegistrar`接口，所以我们主要关注其`registerBeanDefinitions`方法的处理，查看上述逻辑

如果是`AdviceMode.PROXY`模式，主要工作为调用`AopConfigUtils#registerAutoProxyCreatorIfNecessary`

结合我们对于AOP的分析，`AopConfigUtils#registerAutoProxyCreatorIfNecessary`方法的主要作用是向IOC容器注册了`AnnotationAwareAspectJAutoProxyCreator`类型`BeanDefinition`，其主要目的用于开启AOP功能，加载解析`Advisor`，并使用其进行代理创建



#### ProxyCachingConfiguration

```java
// org.springframework.cache.annotation.ProxyCachingConfiguration
public class ProxyCachingConfiguration extends AbstractCachingConfiguration {
    @Bean(name = CacheManagementConfigUtils.CACHE_ADVISOR_BEAN_NAME)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public BeanFactoryCacheOperationSourceAdvisor cacheAdvisor() {
        BeanFactoryCacheOperationSourceAdvisor advisor = new BeanFactoryCacheOperationSourceAdvisor();
        advisor.setCacheOperationSource(cacheOperationSource());
        advisor.setAdvice(cacheInterceptor());
        if (this.enableCaching != null) {
            advisor.setOrder(this.enableCaching.<Integer>getNumber("order"));
        }
        return advisor;
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public CacheOperationSource cacheOperationSource() {
        return new AnnotationCacheOperationSource();
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public CacheInterceptor cacheInterceptor() {
        CacheInterceptor interceptor = new CacheInterceptor();
        interceptor.configure(this.errorHandler, this.keyGenerator, this.cacheResolver, this.cacheManager);
        interceptor.setCacheOperationSource(cacheOperationSource());
        return interceptor;
    }
}
```

`ProxyCachingConfiguration`是一个配置类，其主要通过`@Bean`注解向IOC容器导入了以下三个`bean`

- `BeanFactoryCacheOperationSourceAdvisor`
- `CacheOperationSource`
- `CacheInterceptor`

并且`BeanFactoryCacheOperationSourceAdvisor`中组合了其他两个`bean`



### 3. 代理解析

#### BeanFactoryCacheOperationSourceAdvisor

我们先查看类图

![image-20210907204529894](D:\学习整理\summarize\springboot\图片\BeanFactoryCacheOperationSourceAdvisor类图)

我们发现`BeanFactoryCacheOperationSourceAdvisor`实现了`Advisor`接口，所以它将被`AnnotationAwareAspectJAutoProxyCreator`扫描进行处理

基于前面AOP分析过程，我们需要关注`BeanFactoryCacheOperationSourceAdvisor`作为一个`Advisor`，它将匹配哪些方法，并且将如何进行代理工作



#### 1. 进行方法匹配

在前面分析中，我们了解，对于`Advisor`进行方法匹配，实际是通过其包装的`Pointcut`实现，所以我们查看`getPointcut`方法

```java
// org.springframework.cache.interceptor.BeanFactoryCacheOperationSourceAdvisor
public class BeanFactoryCacheOperationSourceAdvisor extends AbstractBeanFactoryPointcutAdvisor {
    
    // 持有CacheOperationSource实例
	private CacheOperationSource cacheOperationSource;
    
    // 通过匿名内部类创建了CacheOperationSourcePointcut的子类实例
    private final CacheOperationSourcePointcut pointcut = new CacheOperationSourcePointcut() {
		protected CacheOperationSource getCacheOperationSource() {
			return cacheOperationSource;
		}
	};
    
    // 设置CacheOperationSource实例
    public void setCacheOperationSource(CacheOperationSource cacheOperationSource) {
		this.cacheOperationSource = cacheOperationSource;
	}
    
    // 获取Pointcut
    public Pointcut getPointcut() {
		return this.pointcut;
	}
}
```

分析上述代码，我们发现其`pointcut`是一个通过匿名内部类创建的`CacheOperationSourcePointcut`的子类实例，其对应`CacheOperationSource`来源何处？

在`ProxyCachingConfiguration#cacheAdvisor`配置方法中，通过`advisor.setCacheOperationSource(cacheOperationSource())`调用，实际获取的

`CacheOperationSource`是`AnnotationCacheOperationSource`实例



我们先分析一下`CacheOperationSourcePointcut`

##### CacheOperationSourcePointcut

```java
// org.springframework.cache.interceptor.CacheOperationSourcePointcut
abstract class CacheOperationSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {
    
    protected abstract CacheOperationSource getCacheOperationSource();
}
```

`CacheOperationSourcePointcut`是一个抽象类，其具有一个抽象方法，所以可以通过匿名内部类的方式创建其实例



对于`Pointcut`的匹配过程，分为两步

- 获取其`ClassFilter`进行匹配
- 获取其`MethodMatcher`进行匹配

我们一一分析



###### 1. 通过ClassFilter#matches进行匹配

`CacheOperationSourcePointcut`中`getClassFilter`在其父类`StaticMethodMatcherPointcut`中

```java
// org.springframework.aop.support.StaticMethodMatcherPointcut
public abstract class StaticMethodMatcherPointcut extends StaticMethodMatcher implements Pointcut {
    // 持有ClassFilter
    private ClassFilter classFilter;
    
    // 构造默认设置classFilter
    public StaticMethodMatcherPointcut() {
        this.classFilter = ClassFilter.TRUE;
    }
    
    // 获取ClassFilter
    public ClassFilter getClassFilter() {
        return this.classFilter;
    }
}

// org.springframework.aop.ClassFilter
public interface ClassFilter {
    ClassFilter TRUE = TrueClassFilter.INSTANCE;

    boolean matches(Class<?> var1);
}

// org.springframework.aop.TrueClassFilter
final class TrueClassFilter implements ClassFilter, Serializable {
    public static final TrueClassFilter INSTANCE = new TrueClassFilter();
    
    public boolean matches(Class<?> clazz) {
        return true;
    }
}
```

分析上述代码，我们可知其`ClassFilter#matches`将固定返回`true`，所以匹配过程无关类，所有类都可以通过，那么我们继续分析`MethodMatcher`



###### 2. 通过MethodMatcher#matches进行匹配

在进行分析之前，我们先查看一下`CacheOperationSourcePointcut`类图

![](D:\学习整理\summarize\springboot\图片\CacheOperationSourcePointcut类图)

我们可以发现`CacheOperationSourcePointcut`本身实现了`MethodMatcher`接口



```java
// org.springframework.aop.support.StaticMethodMatcherPointcut
public abstract class StaticMethodMatcherPointcut extends StaticMethodMatcher implements Pointcut {
    public final MethodMatcher getMethodMatcher() {
        return this;
    }
}
```



其`getMethodMatcher`返回其本身，所以我们分析其`matches`方法

```java
abstract class CacheOperationSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {
    // matches判断，实现自MethodMatcher接口
    public boolean matches(Method method, Class<?> targetClass) {
		CacheOperationSource cas = getCacheOperationSource();
		return (cas != null && !CollectionUtils.isEmpty(cas.getCacheOperations(method, targetClass)));
	}
}
```

我们可以发现其通过`getCacheOperationSource`获取`CacheOperationSource`，再调用`CacheOperationSource#getCacheOperations`进行判断

而`getCacheOperations`是其抽象方法，前面我们创建`CacheOperationSourcePointcut`实例是通过匿名内部类实现，所以最终获取的`CacheOperationSource`实例是`AnnotationCacheOperationSource`实例



所以接下来的匹配判断工作就委托给了`AnnotationCacheOperationSource#getCacheOperations`

##### AnnotationCacheOperationSource

```java
// org.springframework.cache.annotation.AnnotationCacheOperationSource
public class AnnotationCacheOperationSource extends AbstractFallbackCacheOperationSource implements Serializable {
    // 缓存注解解析器列表
    private final Set<CacheAnnotationParser> annotationParsers;
    
    public AnnotationCacheOperationSource() {
		this(true);
	}
    
    public AnnotationCacheOperationSource(boolean publicMethodsOnly) {
		this.publicMethodsOnly = publicMethodsOnly;
        // 构造时默认赋值annotationParsers，存在一个SpringCacheAnnotationParser实例
		this.annotationParsers = Collections.singleton(new SpringCacheAnnotationParser());
	}
}
```



我们分析其`getCacheOperations`方法，其具体实现在抽象父类`AbstractFallbackCacheOperationSource`中

###### AbstractFallbackCacheOperationSource

```java
// org.springframework.cache.interceptor.AbstractFallbackCacheOperationSource
public abstract class AbstractFallbackCacheOperationSource implements CacheOperationSource {
    
    public Collection<CacheOperation> getCacheOperations(Method method, @Nullable Class<?> targetClass) {
        // 排除Object类中方法
		if (method.getDeclaringClass() == Object.class) {
			return null;
		}
		
        // 封装缓存cacheKey
		Object cacheKey = getCacheKey(method, targetClass);
        // 尝试从缓存中获取
		Collection<CacheOperation> cached = this.attributeCache.get(cacheKey);

		if (cached != null) {
			return (cached != NULL_CACHING_ATTRIBUTE ? cached : null);
		}
		else {
            // 调用computeCacheOperations方法解析
			Collection<CacheOperation> cacheOps = computeCacheOperations(method, targetClass);
			if (cacheOps != null) {
				this.attributeCache.put(cacheKey, cacheOps);
			}
			else {
				this.attributeCache.put(cacheKey, NULL_CACHING_ATTRIBUTE);
			}
			return cacheOps;
		}
	}
    
    
    private Collection<CacheOperation> computeCacheOperations(Method method, @Nullable Class<?> targetClass) {
        // 主要关注逻辑，其他逻辑省略
        Collection<CacheOperation> opDef = findCacheOperations(specificMethod);
		if (opDef != null) {
			return opDef;
		}
    }
    
    // 具体处理
    protected Collection<CacheOperation> findCacheOperations(Method method) {
		return determineCacheOperations(parser -> parser.parseCacheAnnotations(method));
	}
    
    protected Collection<CacheOperation> determineCacheOperations(CacheOperationProvider provider) {
        Collection<CacheOperation> ops = null;
        for (CacheAnnotationParser parser : this.annotationParsers) {
            // 遍历CacheAnnotationParser进行解析
            Collection<CacheOperation> annOps = provider.getCacheOperations(parser);
            if (annOps != null) {
                if (ops == null) {
                    ops = annOps;
                }
                else {
                    Collection<CacheOperation> combined = new ArrayList<>(ops.size() + annOps.size());
                    combined.addAll(ops);
                    combined.addAll(annOps);
                    ops = combined;
                }
            }
        }
        return ops;
    }
}
```

分析`AbstractFallbackCacheOperationSource#getCacheOperations`可知，其最终将判断委托给了`CacheAnnotationParser`进行解析处理，其实际是调用

`SpringCacheAnnotationParser#parseCacheAnnotations`



###### SpringCacheAnnotationParser

```java
// org.springframework.cache.annotation.SpringCacheAnnotationParser
public class SpringCacheAnnotationParser implements CacheAnnotationParser, Serializable {
    private static final Set<Class<? extends Annotation>> CACHE_OPERATION_ANNOTATIONS = new LinkedHashSet<>(8);

	static {
		CACHE_OPERATION_ANNOTATIONS.add(Cacheable.class);
		CACHE_OPERATION_ANNOTATIONS.add(CacheEvict.class);
		CACHE_OPERATION_ANNOTATIONS.add(CachePut.class);
		CACHE_OPERATION_ANNOTATIONS.add(Caching.class);
	}
    
    public Collection<CacheOperation> parseCacheAnnotations(Method method) {
		DefaultCacheConfig defaultConfig = new DefaultCacheConfig(method.getDeclaringClass());
        // 调用链1
		return parseCacheAnnotations(defaultConfig, method);
	}

	@Nullable
	private Collection<CacheOperation> parseCacheAnnotations(DefaultCacheConfig cachingConfig, AnnotatedElement ae) {
		Collection<CacheOperation> ops = parseCacheAnnotations(cachingConfig, ae, false);
		if (ops != null && ops.size() > 1) {
			// 调用链2
			Collection<CacheOperation> localOps = parseCacheAnnotations(cachingConfig, ae, true);
			if (localOps != null) {
				return localOps;
			}
		}
		return ops;
	}

	@Nullable
	private Collection<CacheOperation> parseCacheAnnotations(
			DefaultCacheConfig cachingConfig, AnnotatedElement ae, boolean localOnly) {
		// 调用链3
		Collection<? extends Annotation> anns = (localOnly ?
				AnnotatedElementUtils.getAllMergedAnnotations(ae, CACHE_OPERATION_ANNOTATIONS) :
				AnnotatedElementUtils.findAllMergedAnnotations(ae, CACHE_OPERATION_ANNOTATIONS));
		if (anns.isEmpty()) {
			return null;
		}

		final Collection<CacheOperation> ops = new ArrayList<>(1);
		anns.stream().filter(ann -> ann instanceof Cacheable).forEach(
				ann -> ops.add(parseCacheableAnnotation(ae, cachingConfig, (Cacheable) ann)));
		anns.stream().filter(ann -> ann instanceof CacheEvict).forEach(
				ann -> ops.add(parseEvictAnnotation(ae, cachingConfig, (CacheEvict) ann)));
		anns.stream().filter(ann -> ann instanceof CachePut).forEach(
				ann -> ops.add(parsePutAnnotation(ae, cachingConfig, (CachePut) ann)));
		anns.stream().filter(ann -> ann instanceof Caching).forEach(
				ann -> parseCachingAnnotation(ae, cachingConfig, (Caching) ann, ops));
		return ops;
	}
}
```

可知`parseCacheAnnotations`具体判断依据就是方法上是否有如下注解

- `Cacheable`
- `CacheEvict`
- `CachePut`
- `Caching`

注解解析后封装为`CacheOperation`实例



###### CacheOperation

```java
// org.springframework.cache.interceptor.CacheOperation
public abstract class CacheOperation implements BasicOperation {
    private final String name;

    private final Set<String> cacheNames;

    private final String key;

    private final String keyGenerator;

    private final String cacheManager;

    private final String cacheResolver;
}
```

`CacheOperation`实际就是将对应缓存注解的属性信息进行封装



##### 小结

所以对于判断`BeanFactoryCacheOperationSourceAdvisor`这个`Advisor`适配的的方法，其实际就是判断是否有SpringBoot 缓存相关的注解



#### 2. 代理方法的处理

前面分析了`Advisor`将适配哪些方法，并对其进行代理，现在我们将分析代理的具体处理逻辑

由于`BeanFactoryCacheOperationSourceAdvisor`是一个`PointcutAdvisor`子类，所以根据AOP分析可知，我们应该关注其封装的`Advice`

结合前面`ProxyCachingConfiguration`中代码：`advisor.setAdvice(cacheInterceptor())`，所以我们应该具体分析`CacheInterceptor`



##### CacheInterceptor

```java
// org.springframework.cache.interceptor.CacheInterceptor
public class CacheInterceptor extends CacheAspectSupport implements MethodInterceptor, Serializable {

    @Override
    @Nullable
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
		
        // 1. 通过lambda表达式，创建一个CacheOperationInvoker，其invoke方法主要触发调用原始方法
        CacheOperationInvoker aopAllianceInvoker = () -> {
            try {
                return invocation.proceed();
            }
            catch (Throwable ex) {
                throw new CacheOperationInvoker.ThrowableWrapper(ex);
            }
        };

        try {
            // 2. 调用重载invoke方法
            return execute(aopAllianceInvoker, invocation.getThis(), method, invocation.getArguments());
        }
        catch (CacheOperationInvoker.ThrowableWrapper th) {
            throw th.getOriginal();
        }
    }

}
```

`CacheInterceptor`实现了`MethodInterceptor`接口，所以需要分析其`invoke`方法



我们查看重载的invoke方法，具体代码在其父类`CacheAspectSupport`中

```java
// org.springframework.cache.interceptor.CacheAspectSupport#execute
protected Object execute(CacheOperationInvoker invoker, Object target, Method method, Object[] args) {
    if (this.initialized) {
        // 1. 获取目标类Class
        Class<?> targetClass = getTargetClass(target);
        // 2. 获取CacheOperationSource，当前获取为AnnotationCacheOperationSource实例
        CacheOperationSource cacheOperationSource = getCacheOperationSource();
        if (cacheOperationSource != null) {
            // 3. 调用getCacheOperations获取对于缓存注解的解析结果，具体分析在前面
            Collection<CacheOperation> operations = cacheOperationSource.getCacheOperations(method, targetClass);
            if (!CollectionUtils.isEmpty(operations)) {
                // 4.1 通过CacheOperationContexts构造，从缓存中获取对应缓存实例
                // 4.2 进行缓存数据处理
                return execute(invoker, method,
                               new CacheOperationContexts(operations, method, args, target, targetClass));
            }
        }
    }

    return invoker.invoke();
}
```

在`execute`方法中，将调用另一个重载`execute`方法，此方法用于对缓存数据的处理

而在此之前，我们需要关注缓存`Cache`实例的获取，它在对应`CacheOperationContexts`的构造中



##### 2.1 缓存的获取

```java
// 1. 先分析CacheOperationContexts
// org.springframework.cache.interceptor.CacheAspectSupport.CacheOperationContexts
private class CacheOperationContexts {
    
    // 存储多个CacheOperationContext
	private final MultiValueMap<Class<? extends CacheOperation>, CacheOperationContext> contexts;
    
    public CacheOperationContexts(Collection<? extends CacheOperation> operations, Method method,
                                  Object[] args, Object target, Class<?> targetClass) {

        this.contexts = new LinkedMultiValueMap<>(operations.size());
        for (CacheOperation op : operations) {
            // 遍历当前CacheOperation集合
            // 通过getOperationContext方法，获取CacheOperationContext
            this.contexts.add(op.getClass(), getOperationContext(op, method, args, target, targetClass));
        }
        this.sync = determineSyncFlag(method);
    }
}


// 2. 调用getOperationContext获取CacheOperationContext实例
// org.springframework.cache.interceptor.CacheAspectSupport#getOperationContext
protected CacheOperationContext getOperationContext(
    CacheOperation operation, Method method, Object[] args, Object target, Class<?> targetClass) {
	// 通过getCacheOperationMetadata获取CacheOperationMetadata实例
    CacheOperationMetadata metadata = getCacheOperationMetadata(operation, method, targetClass);
    // 通过构造创建CacheOperationContext实例
    return new CacheOperationContext(metadata, args, target);
}


// 3. 通过getCacheOperationMetadata
// org.springframework.cache.interceptor.CacheAspectSupport#getCacheOperationMetadata
protected CacheOperationMetadata getCacheOperationMetadata(
    CacheOperation operation, Method method, Class<?> targetClass) {
	// 1. 获取CacheOperationCacheKey
    CacheOperationCacheKey cacheKey = new CacheOperationCacheKey(operation, method, targetClass);
    
    // 2. 尝试从缓存中获取
    CacheOperationMetadata metadata = this.metadataCache.get(cacheKey);
    if (metadata == null) {
        // 缓存未命中
        
        // 3. 获取对应KeyGenerator-key的生成器
        KeyGenerator operationKeyGenerator;
        // 3.1 如果CacheOperation中KeyGenerator属性值
        if (StringUtils.hasText(operation.getKeyGenerator())) {
            // 3.2 如有值，则以对应值为beanName从容器中获取KeyGenerator
            operationKeyGenerator = getBean(operation.getKeyGenerator(), KeyGenerator.class);
        }
        else {
            operationKeyGenerator = getKeyGenerator();
        }
        
        // 4. 获取CacheResolver缓存解析器
        CacheResolver operationCacheResolver;
        if (StringUtils.hasText(operation.getCacheResolver())) {
            operationCacheResolver = getBean(operation.getCacheResolver(), CacheResolver.class);
        }
        else if (StringUtils.hasText(operation.getCacheManager())) {
            CacheManager cacheManager = getBean(operation.getCacheManager(), CacheManager.class);
            operationCacheResolver = new SimpleCacheResolver(cacheManager);
        }
        else {
            operationCacheResolver = getCacheResolver();
            Assert.state(operationCacheResolver != null, "No CacheResolver/CacheManager set");
        }
        metadata = new CacheOperationMetadata(operation, method, targetClass,
                                              operationKeyGenerator, operationCacheResolver);
        this.metadataCache.put(cacheKey, metadata);
    }
    return metadata;
}




// 4. 构建CacheOperationContext实例
protected class CacheOperationContext implements CacheOperationInvocationContext<CacheOperation> {
    // 封装Cache集合
    private final Collection<? extends Cache> caches;

    public CacheOperationContext(CacheOperationMetadata metadata, Object[] args, Object target) {
        this.metadata = metadata;
        this.args = extractArgs(metadata.method, args);
        this.target = target;
        // 通过CacheAspectSupport#getCaches方法获取Cache集合
        // 由于通过CacheOperationContext是CacheAspectSupport的内部类，所以通过 类名.this 区分调用
        this.caches = CacheAspectSupport.this.getCaches(this, metadata.cacheResolver);
        this.cacheNames = createCacheNames(this.caches);
    }
}
```



在上述分析中，最终可知通过`CacheAspectSupport#getCaches`方法调用获取`Cache`集合

###### getCaches

```java
// org.springframework.cache.interceptor.CacheAspectSupport#getCaches
protected Collection<? extends Cache> getCaches(
    CacheOperationInvocationContext<CacheOperation> context, CacheResolver cacheResolver) {

    Collection<? extends Cache> caches = cacheResolver.resolveCaches(context);
    return caches;
}


// org.springframework.cache.interceptor.AbstractCacheResolver#resolveCaches
public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
    // 1. 获取命名空间names
    Collection<String> cacheNames = getCacheNames(context);
    if (cacheNames == null) {
        return Collections.emptyList();
    }
    Collection<Cache> result = new ArrayList<>(cacheNames.size());
    for (String cacheName : cacheNames) {
        // 2.1 先通过getCacheManager获取到CacheManager
        // 2.2 通过CacheManager#getCache获取命名空间对应Cache实例
        Cache cache = getCacheManager().getCache(cacheName);
        if (cache == null) {
            throw new IllegalArgumentException("Cannot find cache named '" +
                                               cacheName + "' for " + context.getOperation());
        }
        result.add(cache);
    }
    return result;
}
```



###### 小结

经过上述分析，我们可知

- 将通过`CacheManager#getCache`获取`name`命名空间对应`Cache`实例，所以是通过`CacheManager`来实现对应不同命名空间对应的`Cache`实例的管理
- 获取到的`Cache`实例封装在`CacheOperationContexts`实例中

`CacheOperationContexts`内容具体体现

![image-20210909202030869](D:\学习整理\summarize\springboot\图片\CacheOperationContexts调试示例)





##### 2.2 缓存的处理

我们继续分析获取缓存`Cache`之后，将如何进行数据处理

```java
// org.springframework.cache.interceptor.CacheAspectSupport#execute
private Object execute(final CacheOperationInvoker invoker, Method method, CacheOperationContexts contexts) {

    // 1. 处理任何提前驱逐，根据@CacheEvict注解信息
    processCacheEvicts(contexts.get(CacheEvictOperation.class), true,
                       CacheOperationExpressionEvaluator.NO_RESULT);

    // 2. 根据@Cacheable注解信息，检查我们是否有符合条件的缓存项
    Cache.ValueWrapper cacheHit = findCachedItem(contexts.get(CacheableOperation.class));

    // Collect puts from any @Cacheable miss, if no cached item is found
    List<CachePutRequest> cachePutRequests = new LinkedList<>();
    if (cacheHit == null) {
        collectPutRequests(contexts.get(CacheableOperation.class),
                           CacheOperationExpressionEvaluator.NO_RESULT, cachePutRequests);
    }

    Object cacheValue;
    Object returnValue;

    // 3. 缓存判断
    if (cacheHit != null && !hasCachePut(contexts)) {
        // 3.1 缓存命中，从缓存中获取方法结果
        cacheValue = cacheHit.get();
        returnValue = wrapCacheValue(method, cacheValue);
    }
    else {
        // 3.2 如果我们没有缓存命中，则调用该实例方法
        returnValue = invokeOperation(invoker);
        cacheValue = unwrapReturnValue(returnValue);
    }

    // 4. 收集任何明确的@CachePuts注解信息
    collectPutRequests(contexts.get(CachePutOperation.class), cacheValue, cachePutRequests);

    // 5. 处理来自@CachePut 或@Cacheable 未命中的任何收集的放置请求
    for (CachePutRequest cachePutRequest : cachePutRequests) {
        // 根据@CachePut 或@Cacheable注解信息，将方法结果进行缓存
        cachePutRequest.apply(cacheValue);
    }

    // 6. 处理任何后置驱逐，根据@CacheEvict注解信息
    processCacheEvicts(contexts.get(CacheEvictOperation.class), false, cacheValue);

    // 7. 返回结果
    return returnValue;
}
```

上面代码包括了对于`@Cacheable、@CachePut、@CacheEvict`的处理，我们一一分析



###### processCacheEvicts

`processCacheEvicts`方法用于处理`@CacheEvict`注解信息，并且我们发现其有两次调用，这时根据`@CacheEvict`注解中`beforeInvocation`属性控制

```java
boolean beforeInvocation() default false;
```

`beforeInvocation`默认为`false`，表示在 方法调用/缓存命中 之后进行调用；而`beforeInvocation`设置为true时，则表示在方法调用之前清理缓存，无论方法是否调用异常，缓存都将被清理



我们分析`processCacheEvicts`具体调用

```java
// org.springframework.cache.interceptor.CacheAspectSupport#processCacheEvicts
private void processCacheEvicts(
    Collection<CacheOperationContext> contexts, boolean beforeInvocation, @Nullable Object result) {
    for (CacheOperationContext context : contexts) {
        // 1. 获取具体CacheEvictOperation实例进行判断，其包装了@CacheEvict注解信息
        CacheEvictOperation operation = (CacheEvictOperation) context.metadata.operation;
        if (beforeInvocation == operation.isBeforeInvocation() && isConditionPassing(context, result)) {
            // 2. 判断通过，则调用performCacheEvict进行缓存清理
            performCacheEvict(context, operation, result);
        }
    }
}

// org.springframework.cache.interceptor.CacheAspectSupport#performCacheEvict
private void performCacheEvict(
    CacheOperationContext context, CacheEvictOperation operation, @Nullable Object result) {

    Object key = null;
    for (Cache cache : context.getCaches()) {
        // 对应@CacheEvict注解allEntries属性
        if (operation.isCacheWide()) {
            logInvalidating(context, operation, null);
            // 此处进行缓存的整个清理
            doClear(cache);
        }
        else {
            if (key == null) {
                // 获取具体缓存key
                key = generateKey(context, result);
            }
            logInvalidating(context, operation, key);
            // 清除具体key对应缓存数据
            doEvict(cache, key);
        }
    }
}

// 根据key，清理缓存Cache中对应缓存数据
// org.springframework.cache.interceptor.AbstractCacheInvoker#doEvict
protected void doEvict(Cache cache, Object key) {
    try {
        cache.evict(key);
    }
    catch (RuntimeException ex) {
        getErrorHandler().handleCacheEvictError(ex, cache, key);
    }
}

// 清除缓存Cache实例中所有缓存数据
// org.springframework.cache.interceptor.AbstractCacheInvoker#doClear
protected void doClear(Cache cache) {
    try {
        cache.clear();
    }
    catch (RuntimeException ex) {
        getErrorHandler().handleCacheClearError(ex, cache);
    }
}
```

经过上述分析可知，具体缓存清理动作，由缓存`Cache`实例处理



###### findCachedItem

`findCachedItem`用于从缓存`Cache`中获取具体的缓存数据

```java
// org.springframework.cache.interceptor.CacheAspectSupport#findCachedItem
private Cache.ValueWrapper findCachedItem(Collection<CacheOperationContext> contexts) {
    Object result = CacheOperationExpressionEvaluator.NO_RESULT;
    for (CacheOperationContext context : contexts) {
        if (isConditionPassing(context, result)) {
            // 1. 获取缓存key
            Object key = generateKey(context, result);
            // 2. 获取对应缓存数据
            Cache.ValueWrapper cached = findInCaches(context, key);
            if (cached != null) {
                return cached;
            }
            else {
                if (logger.isTraceEnabled()) {
                    logger.trace("No cache entry for key '" + key + "' in cache(s) " + context.getCacheNames());
                }
            }
        }
    }
    return null;
}

// org.springframework.cache.interceptor.CacheAspectSupport#findInCaches
private Cache.ValueWrapper findInCaches(CacheOperationContext context, Object key) {
    for (Cache cache : context.getCaches()) {
        // 调用doGet获取缓存数据
        Cache.ValueWrapper wrapper = doGet(cache, key);
        if (wrapper != null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Cache entry for key '" + key + "' found in cache '" + cache.getName() + "'");
            }
            return wrapper;
        }
    }
    return null;
}

// org.springframework.cache.interceptor.AbstractCacheInvoker#doGet
protected Cache.ValueWrapper doGet(Cache cache, Object key) {
    try {
        return cache.get(key);
    }
    catch (RuntimeException ex) {
        getErrorHandler().handleCacheGetError(ex, cache, key);
        return null;  // If the exception is handled, return a cache miss
    }
}
```

分析可知，实质是通过`Cache#get`进行缓存数据的获取



###### CachePutRequest#apply

`CachePutRequest#apply`用户将方法防护结果缓存

```java
// org.springframework.cache.interceptor.CacheAspectSupport.CachePutRequest#apply
public void apply(@Nullable Object result) {
    if (this.context.canPutToCache(result)) {
        for (Cache cache : this.context.getCaches()) {
            doPut(cache, this.key, result);
        }
    }
}

// org.springframework.cache.interceptor.AbstractCacheInvoker#doPut
protected void doPut(Cache cache, Object key, @Nullable Object result) {
    try {
        cache.put(key, result);
    }
    catch (RuntimeException ex) {
        getErrorHandler().handleCachePutError(ex, cache, key, result);
    }
}
```

经过上述可知，实质是通过`Cache#put`进行数据缓存



###### 小结

经过上述分析可知，对于缓存的处理，实质底层是通过封装在`CacheOperationContext`中的`Cache`实例进行缓存处理



### 4. CacheManager分析

在上述分析中，我们分析具体缓存信息，封装为`CacheOperation`实例，并通过对应`name`从`CacheManager`中获取命名空间对应`Cache`实例，之后再通过`Cache`实例进行缓存的处理，那么这个`CacheManager`实例从何而来？如何从IOC容器中获取？我们一一分析



我们说使用`SpringCache`，需要引入`spring-boot-starter-cache`这个`starter`依赖，其中它引入了`spring-boot-autoconfigure`

在`spring-boot-autoconfigure`项目的`spring.factories`文件中，对于`EnableAutoConfiguration`自动配置中，存在一个类，它就是`CacheAutoConfiguration`



#### 4.1 CacheManager从何而来

##### CacheAutoConfiguration

```java
// org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(CacheManager.class)
@ConditionalOnBean(CacheAspectSupport.class)
@ConditionalOnMissingBean(value = CacheManager.class, name = "cacheResolver")
@EnableConfigurationProperties(CacheProperties.class)
@AutoConfigureAfter({ CouchbaseAutoConfiguration.class, HazelcastAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class, RedisAutoConfiguration.class })
@Import({ CacheConfigurationImportSelector.class, CacheManagerEntityManagerFactoryDependsOnPostProcessor.class })
public class CacheAutoConfiguration {
    
    
    // 静态内部类CacheConfigurationImportSelector
    static class CacheConfigurationImportSelector implements ImportSelector {
		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
            // 1. 获取CacheType枚举对应SpringBoot支持的缓存类型
			CacheType[] types = CacheType.values();
			String[] imports = new String[types.length];
			for (int i = 0; i < types.length; i++) {
                // 2. 获取不同缓存类型对应的配置类
				imports[i] = CacheConfigurations.getConfigurationClass(types[i]);
			}
			return imports;
		}

	}
}  
```

在`CacheAutoConfiguration`中，我们首先需要关注的是其通过`@Import`注解导入了`CacheConfigurationImportSelector`类，`CacheConfigurationImportSelector`类是`CacheAutoConfiguration`的静态内部类，其实现了`ImportSelector`接口，所以我们查看其`selectImports`

方法



在`selectImports`中，主要通过`CacheConfigurations#getConfigurationClass`获取缓存配置类，我们分析一下

##### CacheConfigurations

```java
// org.springframework.boot.autoconfigure.cache.CacheConfigurations
final class CacheConfigurations {

	private static final Map<CacheType, Class<?>> MAPPINGS;

	static {
		// 静态初始化设置缓存类型对应缓存配置类
		Map<CacheType, Class<?>> mappings = new EnumMap<>(CacheType.class);
		mappings.put(CacheType.GENERIC, GenericCacheConfiguration.class);
		mappings.put(CacheType.EHCACHE, EhCacheCacheConfiguration.class);
		mappings.put(CacheType.HAZELCAST, HazelcastCacheConfiguration.class);
		mappings.put(CacheType.INFINISPAN, InfinispanCacheConfiguration.class);
		mappings.put(CacheType.JCACHE, JCacheCacheConfiguration.class);
		mappings.put(CacheType.COUCHBASE, CouchbaseCacheConfiguration.class);
		mappings.put(CacheType.REDIS, RedisCacheConfiguration.class);
		mappings.put(CacheType.CAFFEINE, CaffeineCacheConfiguration.class);
		mappings.put(CacheType.SIMPLE, SimpleCacheConfiguration.class);
		mappings.put(CacheType.NONE, NoOpCacheConfiguration.class);
		MAPPINGS = Collections.unmodifiableMap(mappings);
	}

	private CacheConfigurations() {
	}
	
	// 获取缓存类型对应缓存配置类
	static String getConfigurationClass(CacheType cacheType) {
		Class<?> configurationClass = MAPPINGS.get(cacheType);
		Assert.state(configurationClass != null, () -> "Unknown cache type " + cacheType);
		return configurationClass.getName();
	}

    // 获取缓存配置类对应缓存类型
	static CacheType getType(String configurationClassName) {
		for (Map.Entry<CacheType, Class<?>> entry : MAPPINGS.entrySet()) {
			if (entry.getValue().getName().equals(configurationClassName)) {
				return entry.getKey();
			}
		}
		throw new IllegalStateException("Unknown configuration class " + configurationClassName);
	}

}
```

通过`CacheConfigurations#getConfigurationClass`将获取一系列`CacheConfiguration`配置类，一般没有特殊配置时，将使用`SimpleCacheConfiguration`





##### SimpleCacheConfiguration

```java
// org.springframework.boot.autoconfigure.cache.SimpleCacheConfiguration
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(CacheManager.class)
@Conditional(CacheCondition.class)
class SimpleCacheConfiguration {

	@Bean
	ConcurrentMapCacheManager cacheManager(CacheProperties cacheProperties,
			CacheManagerCustomizers cacheManagerCustomizers) {
		ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
		List<String> cacheNames = cacheProperties.getCacheNames();
		if (!cacheNames.isEmpty()) {
			cacheManager.setCacheNames(cacheNames);
		}
		return cacheManagerCustomizers.customize(cacheManager);
	}

}
```

为何使用`SimpleCacheConfiguration`配置类，因为其只需要IOC容器中没有`CacheManager`类型`bean`即可

`SimpleCacheConfiguration`中通过`cacheManager`方法向IOC容器中注册的是`ConcurrentMapCacheManager`



##### ConcurrentMapCacheManager

```java
// org.springframework.cache.concurrent.ConcurrentMapCacheManager
public class ConcurrentMapCacheManager implements CacheManager, BeanClassLoaderAware {
    // 通过ConcurrentMap存储Cache实例，key值为name命名空间
    private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>(16);

    // 设置命名空间
    public void setCacheNames(@Nullable Collection<String> cacheNames) {
        if (cacheNames != null) {
            for (String name : cacheNames) {
                // 将通过createConcurrentMapCache创建Cache实例
                this.cacheMap.put(name, createConcurrentMapCache(name));
            }
            this.dynamic = false;
        }
        else {
            this.dynamic = true;
        }
    }
	
    // 根据命名空间，创建Cache实例
    protected Cache createConcurrentMapCache(String name) {
        SerializationDelegate actualSerialization = (isStoreByValue() ? this.serialization : null);
        // 创建ConcurrentMapCache类型实例
        return new ConcurrentMapCache(name, new ConcurrentHashMap<>(256), isAllowNullValues(), actualSerialization);
    }
}
```

可知`ConcurrentMapCacheManager`管理的`Cache`实例为`ConcurrentMapCache`类型



##### ConcurrentMapCache

```java
public class ConcurrentMapCache extends AbstractValueAdaptingCache {
	private final String name;
    private final ConcurrentMap<Object, Object> store;
}
```

`ConcurrentMapCache`内部通过`ConcurrentMap`管理缓存数据



#### 4.2 如何从IOC容器中获取CacheManager

在前面分析中，我们通过`CacheInterceptor`作为`Advisor`进行缓存的代理处理，关注`CacheInterceptor`类的继承层级，我们发现其实现了`SmartInitializingSingleton`接口



##### SmartInitializingSingleton

```java
public interface SmartInitializingSingleton {
	void afterSingletonsInstantiated();
}
```

`SmartInitializingSingleton`是一个接口，主要用于Spring容器刷新时，所有单例`bean`实例化后的回调动作

那么它如何进行工作呢？



在Spring 生命周期中，我们通过`DefaultListableBeanFactory#preInstantiateSingletons`进行单例`bean`的实例化工作，实际在所有`bean`实例化后，还有一个动作

```java
// org.springframework.beans.factory.support.DefaultListableBeanFactory#preInstantiateSingletons
public void preInstantiateSingletons() throws BeansException {

    // 单例bean实例化，省略...

    for (String beanName : beanNames) {
        Object singletonInstance = getSingleton(beanName);
        if (singletonInstance instanceof SmartInitializingSingleton) {
            final SmartInitializingSingleton smartSingleton = (SmartInitializingSingleton) singletonInstance;
			// 调用SmartInitializingSingleton#afterSingletonsInstantiated进行实例化后的回调
            smartSingleton.afterSingletonsInstantiated();
        }
    }
}
```



由于`CacheInterceptor`实现了`SmartInitializingSingleton`接口，所以我们分析其`afterSingletonsInstantiated`方法，它在其父类`CacheAspectSupport`中



##### afterSingletonsInstantiated

```java
// org.springframework.cache.interceptor.CacheAspectSupport
public abstract class CacheAspectSupport extends AbstractCacheInvoker
    implements BeanFactoryAware, InitializingBean, SmartInitializingSingleton {

    private SingletonSupplier<CacheResolver> cacheResolver;


    public void afterSingletonsInstantiated() {
        if (getCacheResolver() == null) {
            // 从IOC容器中获取CacheManager实例
            setCacheManager(this.beanFactory.getBean(CacheManager.class));
        }
        this.initialized = true;
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheResolver = SingletonSupplier.of(new SimpleCacheResolver(cacheManager));
    }
}
```

`afterSingletonsInstantiated`方法中主要是从IOC容器中获取`CacheManager`实例，并且封装到`SingletonSupplier`中



#### 小结



我们可知，是通过`CacheAutoConfiguration`自动配置类导入的`CacheConfigurationImportSelector`类，向IOC容器中导入了多个`CacheConfiguration`缓存配置类，这些缓存配置类中通过`@Bean`注解注释的`cacheManager`方法向IOC容器中注册`CacheManager`实例

之后在`CacheInterceptor`实例化后，调用实现自`SmartInitializingSingleton`接口的`afterSingletonsInstantiated`方法，从IOC容器中获取`CacheManager`实例，供`CacheInterceptor`使用



### Redis缓存使用

前面可知，使用通用`SimpleCacheConfiguration`配置类引入的`ConcurrentMapCacheManager`缓存管理器，其实际是通过内存中的`ConcurrentMap`集合实现数据的缓存，这将占用大量内存资源，所以我们通常考虑使用其他介质进行数据缓存，`Redis`就是一个不错的缓存容器

那么我们如何使用`Redis`缓存替代`ConcurrentMapCacheManager`管理的内存缓存呢？



在`SimpleCacheConfiguration`配置类上，存在这样一个注解`@ConditionalOnMissingBean(CacheManager.class)`，所以我们只要向容器中注册一个管理`Redis`的`CacheManager`实例，就可以替代`ConcurrentMapCacheManager`



在`CacheConfigurations#getConfigurationClass`引入的`CacheConfiguration`配置类中，存在一个关于`Redis`的配置类，它就是`RedisCacheConfiguration`



#### RedisCacheConfiguration

```java
// org.springframework.boot.autoconfigure.cache.RedisCacheConfiguration
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RedisConnectionFactory.class)
@AutoConfigureAfter(RedisAutoConfiguration.class)
@ConditionalOnBean(RedisConnectionFactory.class)
@ConditionalOnMissingBean(CacheManager.class)
@Conditional(CacheCondition.class)
class RedisCacheConfiguration {

    @Bean
    RedisCacheManager cacheManager(CacheProperties cacheProperties, CacheManagerCustomizers cacheManagerCustomizers,
                                   ObjectProvider<org.springframework.data.redis.cache.RedisCacheConfiguration> redisCacheConfiguration,
                                   ObjectProvider<RedisCacheManagerBuilderCustomizer> redisCacheManagerBuilderCustomizers,
                                   RedisConnectionFactory redisConnectionFactory, ResourceLoader resourceLoader) {
        RedisCacheManagerBuilder builder = RedisCacheManager.builder(redisConnectionFactory).cacheDefaults(
            determineConfiguration(cacheProperties, redisCacheConfiguration, resourceLoader.getClassLoader()));
        List<String> cacheNames = cacheProperties.getCacheNames();
        if (!cacheNames.isEmpty()) {
            builder.initialCacheNames(new LinkedHashSet<>(cacheNames));
        }
        redisCacheManagerBuilderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
        return cacheManagerCustomizers.customize(builder.build());
    }

    private org.springframework.data.redis.cache.RedisCacheConfiguration determineConfiguration(
        CacheProperties cacheProperties,
        ObjectProvider<org.springframework.data.redis.cache.RedisCacheConfiguration> redisCacheConfiguration,
        ClassLoader classLoader) {
        return redisCacheConfiguration.getIfAvailable(() -> createConfiguration(cacheProperties, classLoader));
    }

    private org.springframework.data.redis.cache.RedisCacheConfiguration createConfiguration(
        CacheProperties cacheProperties, ClassLoader classLoader) {
        Redis redisProperties = cacheProperties.getRedis();
        org.springframework.data.redis.cache.RedisCacheConfiguration config = org.springframework.data.redis.cache.RedisCacheConfiguration
            .defaultCacheConfig();
        config = config.serializeValuesWith(
            SerializationPair.fromSerializer(new JdkSerializationRedisSerializer(classLoader)));
        if (redisProperties.getTimeToLive() != null) {
            config = config.entryTtl(redisProperties.getTimeToLive());
        }
        if (redisProperties.getKeyPrefix() != null) {
            config = config.prefixKeysWith(redisProperties.getKeyPrefix());
        }
        if (!redisProperties.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        if (!redisProperties.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }
        return config;
    }

}
```

通过带有`@Bean`注解的`RedisCacheConfiguration#cacheManager`方法，我们可以向IOC容器中注册一个`RedisCacheManager`，





#### RedisCacheManager

```java
// org.springframework.data.redis.cache.RedisCacheManager
public class RedisCacheManager extends AbstractTransactionSupportingCacheManager {
    
    // 创建RedisCache缓存容器
    protected RedisCache createRedisCache(String name, @Nullable RedisCacheConfiguration cacheConfig) {
        return new RedisCache(name, this.cacheWriter, cacheConfig != null ? cacheConfig : this.defaultCacheConfig);
    }
}
```



#### RedisCache

`RedisCacheManager`中使用`RedisCache`作为缓存容器

```java
// org.springframework.data.redis.cache.RedisCache
public class RedisCache extends AbstractValueAdaptingCache {
    private final String name;
    private final RedisCacheWriter cacheWriter;
    private final RedisCacheConfiguration cacheConfig;

    // 获取缓存数据
    public synchronized <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper result = this.get(key);
        if (result != null) {
            return result.get();
        } else {
            T value = valueFromLoader(key, valueLoader);
            this.put(key, value);
            return value;
        }
    }

    // 数据缓存
    public void put(Object key, @Nullable Object value) {
        Object cacheValue = this.preProcessCacheValue(value);

        this.cacheWriter.put(this.name, this.createAndConvertCacheKey(key), this.serializeCacheValue(cacheValue), this.cacheConfig.getTtl());
    }
}
```



默认的`RedisCacheManager`使用了`JdkSerializationRedisSerializer`序列化方式，不利于使用，所以我们可以自己向IOC容器中注册一个`RedisCacheManager`，实现自己想要的序列化机制

#### 自定义RedisCacheManager

```java
@Bean
public RedisCacheManager cacheManager(RedisConnectionFactory
                                      redisConnectionFactory) {
    // 分别创建String和JSON格式序列化对象，对缓存数据key和value进行转换
    // key使用StringRedisSerializer序列化
    // value使用Jackson2JsonRedisSerializer序列化
    RedisSerializer<String> strSerializer = new StringRedisSerializer();
    Jackson2JsonRedisSerializer jacksonSeial =
        new Jackson2JsonRedisSerializer(Object.class);
    
    // 解决查询缓存转换异常的问题
    ObjectMapper om = new ObjectMapper();
    om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
    om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
    jacksonSeial.setObjectMapper(om);
    
    // 定制缓存数据序列化方式及时效
    RedisCacheConfiguration config =
        RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofDays(1))
        .serializeKeysWith(RedisSerializationContext.SerializationPair
                           .fromSerializer(strSerializer))
        .serializeValuesWith(RedisSerializationContext.SerializationPair
                             .fromSerializer(jacksonSeial))
        .disableCachingNullValues();
    RedisCacheManager cacheManager = RedisCacheManager
        .builder(redisConnectionFactory).cacheDefaults(config).build();
    return cacheManager;
}
```



