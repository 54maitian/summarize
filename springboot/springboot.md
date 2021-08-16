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



### @SpringBootApplication

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



我们前面通过`getCandidateConfigurations`方法获取了配置在`spring.factories`文件中的自动装配配置类集合，但是对于不同项目情景，不是所有的配置类都是必须注册到IOC容器中进行工作的，所以我们要对其进行过滤处理，剔除无用的配置类



