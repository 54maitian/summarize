# SpringBoot前置知识

## 约定优于配置

概念：约定优于配置(Convention over Configuration)，又称按约定编程，是一种软件设计规范。

本质上是对系统、类库或框架中一些东西假定一个大众化合理的默认值(缺省值)。

我理解就是对于一些东西，由对应约定好的默认值。如果无需改动，则可直接使用约定的值，否则自行配置

优点：减少配置项

## SpringBoot主要特性

1. `SpringBoot Starter`：他将常用的依赖分组进行了整合，将其合并到一个依赖中，这样就可以一次 性添加到项目的Maven或Gradle构建中
   1. 无需手动进行依赖引入、依赖冲突的解决
2. 使编码变得简单，SpringBoot采用 JavaConfig的方式对Spring进行配置，并且提供了大量的注解， 极大的提高了工作效率
3. 自动配置：SpringBoot的自动配置特性利用了Spring对条件化配置的支持，合理地推测应用所需的 bean并自动化配置他们
4. 使部署变得简单，SpringBoot内置了三种Servlet容器，Tomcat，Jetty,undertow.我们只需要一个 Java的运行环境就可以跑SpringBoot的项目了，SpringBoot的项目可以打成一个jar包

## SpringBoot项目创建

我们可以通过`Spring Initializr`的方式进行构建，构建的项目默认层级

![image-20210728200528773](D:\学习整理\summarize\springboot\图片\springboot项目默认层级)

## 热部署

通过热部署依赖启动器`spring-boot-devtools`进行项目热部署，实现项目的热加载

优点：修改代码后无需每次进行项目重启，代码就能生效

### 依赖

```xml
<!-- 引入热部署依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
</dependency>
```

### IEAD配置

![idea热部署配置1](D:\学习整理\summarize\springboot\图片\idea热部署配置1)

![image-20210728200450226](D:\学习整理\summarize\springboot\图片\idea热部署配置2)

### 热部署原理

通过插件，编译期自动检测代码改动，进行改动部分代码的编译、自动替换.class文件

插件重启快速的原因：通过不同类加载器进行变更的.class文件加载

- 第三方jar包：`base-classloader`
- 自己写的代码：`restartClassLoader`

所以对于开发人员自己编写代码的修改，只需要使用`restartClassLoader`进行加载重启即可，就比较快速

### 排除资源

可能对于开发人员编写的部分资源进行修改后，无需重启，所以需要排除这部分资源的监控

可以通过`spring.devtools.restart.exclude`参数进行排除

```properties
#排除 /static /public 下文件
spring.devtools.restart.exclude=static/**,public/**
```

## 全局配置文件

`SpringBoot`约定通过 用一个`application.properties`或者`application.yaml`的文件作为全局配置文件

### 配置文件位置及其优先级

1. 项目根目录的config文件夹中
2. 项目根目录中
3. resources中的cofnig文件夹中
4. resources中

```
–file:./config/
–file:./
–classpath:/config/
–classpath:/
```

图解：

![image-20210728200609763](D:\学习整理\summarize\springboot\图片\全局配置文件加载位置)

### 配置规则

```
1. SpringBoot会尝试加载上述所有位置的全局配置文件，进行解析，最后打包进jar包
2. 对于不冲突的属性，则会共同存在-互补配置
3. 对于冲突的属性，则以加载规则优先加载的为主，不会进行覆盖动作
4. 对于相同位置同时存在application.properties 和 application.yaml，此时加载优先级
	2.4.0版本前：properties 优先于 yaml
	2.4.0版本(包括)后：yaml 优先于 properties
	
注：.yaml 后缀与 .yml 后缀等同
```

如果不按默认约定规则设定配置文件名称，则需要通过参数指定具体配置文件名称

```bash
java -jar myproject.jar --spring.config.name=myproject
```

同时也可以指定具体配置文件同约定全局配置文件进行 `互补配置`

```bash
java -jar run-0.0.1-SNAPSHOT.jar --spring.config.location=D:/application.properties
```

### 配置文件对应对象格式

#### Java实体

```java
public class Pet {
    private String type;
    private String name;
}

@Component
@ConfigurationProperties(prefix = "person")
public class Person {
    private int id; //id
    private String name; //名称
    private List hobby; //爱好
    private String[] family; //家庭成员
    private Map map;
    private Pet pet; //宠物
}
```

#### properties文件

```properties
#int
person.id=1
#String
person.name=tom
#List
person.hobby=吃饭,睡觉
#Array
person.family=father,mather
#map
person.map.k1=v1
person.map.k2=v2
#实体对象
person.pet.type=dog
person.pet.name=旺财
```

#### yml/yaml文件

```yaml
person:
  #int
  id: 1
  #String
  name: tom
  #List/Array
  hobby:
    - 吃饭
    - 睡觉
  family:
    father,
    mather
  #Array特有
# family: [father,mather]
  #map
  map:
    k1: v1
    k2: v2
# map: {k1: v1,k2: v2}
  #实体对象
  pet:
    type: dog
    name: 旺财
```

### profile

`springboot`除了加载默认`application`文件，还会根据`spring.profile.active`的配置，默认加载`application-{active}`文件

### properties文件乱码

http://www.leftso.com/blog/579.html;jsessionid=8C75EA4634168502F536B86A9BB34495

IDEA配置

![image-20210728205622226](D:\学习整理\summarize\springboot\图片\properties加载乱码)

#### 注意

idea配置修改后不生效原因：配置修改后`properties`文件需要重新创建

## 属性注入

我们可以通过 `@Value`注解和`@ConfigurationProperties`注解，将全局配置文件中的配置的属性注入到`JavaBean`的属性中

### @Value

- `JavaBean`需要为IOC容器管理对象
- 无需`Setter`方法

`@Value`配置取值有两种方式

- `@Value(“${property: default_value}”)`
  - 获取外部配置文件对应的`property`
  - `default_value`为缺省值
- `@Value(“#{obj.property?: default_value}”)`
  - 不是获取配置文件属性，而是获取对应`obj对象/对象属性`
    - 通过`beanName`从IOC容器中获取对象
  - 可以嵌套`${}`，如：`"#{'${property}'}"`

```java
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Value {
    String value();
}
```

#### 示例

```java
public class JdbcProperties {
    //使用${}
    @Value("${jdbc.className}")
    private String className;

    @Value("${jdbc.url}")
    private String url;

    //使用#{}，person为spring管理对象
    @Value("#{person.name}")
    private String username;
	
    //嵌套使用#{'${property}'}
    @Value("#{'${jdbc.password}'}")
    private String password;
}
```

### @ConfigurationProperties

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Indexed
public @interface ConfigurationProperties {
    @AliasFor("prefix")
    String value() default "";

    @AliasFor("value")
    String prefix() default "";

    boolean ignoreInvalidFields() default false;

    boolean ignoreUnknownFields() default true;
}
```

使用`@ConfigurationProperties`有两种方式

- 配合`@Component`注解，将`@ConfigurationProperties`注释`JavaBean`交由IOC容器管理
- 在配置类上使用`@EnableConfigurationProperties`，将对应`@ConfigurationProperties`注释`JavaBean`加载到IOC容器中
  - 注意：此时`JavaBean`对应`beanName`不是首字母小写的方式

## 日志框架

通常情况下，日志是由一个抽象层+实现层的组合来搭建的

- 抽象层：定义日志使用API、规范
- 实现层：具体日志功能实现jar包

| 日志-抽象层                                                  | 日志-实现层                                       |
| ------------------------------------------------------------ | ------------------------------------------------- |
| JCL（Jakarta Commons Logging）、SLF4J（Simple Logging Facade for Java）、jboss-logging | jul（java.util.logging）、 log4j、logback、log4j2 |

Spring 框架选择使用了 JCL 作为默认日志输出。而 `Spring Boot` 默认选择了 SLF4J 结合 LogBack

### 统一日志框架使用

对于我们开发来说，`SpringBoot`默认选择`SLF4J`作为日志抽象层，实现层使用`LogBack`，所以我们日常开发时，就使用`SLF4J`规范进行开发，无需关注其具体实现方式

而`SpringBoot`使用时，可能会集成很多其他框架，也许这些框架都遵循`SLF4J`抽象层规则，但是其实现内核不同。此时将影响我们的日志使用

例如：`Spring（commons logging）、Hibernate（jboss-logging）、 mybatis....`

所以我们为了避免不同实现层的影响，需要统一项目中日志框架，使用的手段有：

- 排除系统中的其他日志框架
- 使用中间包替换要替换的日志框架
- 导入我们选择的 SLF4J 实现

### 排除系统中的其他日志框架

通过`maven`中的`exclusions`标签

```xml
<dependency>
    <groupId></groupId>
    <artifactId></artifactId>
    <version></version>
    <exclusions>
        <exclusion>
            <!--排除当前依赖jar包中对应日志实现框架jar包-->
            <artifactId>commons-logging</artifactId>
            <groupId>commons-logging</groupId>
        </exclusion>
    </exclusions>
</dependency>
```

### 统一框架引入替换包

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-logging</artifactId>
    <version>2.4.0.RELEASE</version>
</dependency>
```

