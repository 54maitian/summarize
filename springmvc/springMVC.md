# Servlet知识

## Servlet

一个Servlet就是一个在web服务器中运行的小型java程序，而Servlet接口就是这些小程序的一个规范，定义了所有Servlet需要具有的功能

```java
//javax.servlet.Servlet
public interface Servlet {
    /* 第一次请求Servlet时,Servlet容器会调用这个方法.在后续的请求中不会在调用 */
    public void init(ServletConfig config) throws ServletException;

    /* 该方法返回由Servlet容器传给init方法的ServletConfig.
    	为了让getServletConfig返回非null值,应该为传给init方法的ServletConfig赋给一个类级变量.*/
    public ServletConfig getServletConfig();

    /* 每次请求Servlet时都会调用此方法,必须在这里编写要Servlet完成的响应代码. 
    	第一次调用Servlet时会调用init()和service()方法,之后请求只调用service()方法*/
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException;

    /* 该方法返回Servlet的描述,可以是任意字符串,甚至为null.*/
    public String getServletInfo();
    
    /* Servlet要销毁时调用此方法,通常发生在卸载应用程序,或者关闭Servlet容器的时候.*/
    public void destroy();
}
```

## GenericServlet

GenericServlet是一个Servlet接口的抽象实现类，对其中的 init() 和 destroy() 和 service() 提供了默认实现

主要作用：

- 将 init() 中的 ServletConfig 赋给一个类级变量，可以由 getServletConfig 获得
- 为 Servlet 所有方法提供默认实现
- 可以直接调用 ServletConfig 中的方法

```java
//javax.servlet.GenericServlet
public abstract class GenericServlet implements Servlet, ServletConfig, java.io.Serializable {
    //存储ServletConfig
    private transient ServletConfig config;
    
    /* 实现init方法，保存ServletConfig */
    public void init(ServletConfig config) throws ServletException {
        this.config = config;
        this.init();
    }
    
    /* 可以获取ServletConfig，进而调用ServletConfig中方法 */
    public ServletConfig getServletConfig() {
		return config;
    }
}
```

## HttpServlet

httpservlet类扩展了genericservlet并且提供了servlet接口中具体于http的实现，它更象一个其他所有的servlet都要扩展的类

HttpServlet也是一个抽象类

实现了Servlet的service方法，在其中判断请求方法类型，调用不同的doXXX方法：

- doPost：处理post请求
- doGet：处理get请求
- ...

```java
//javax.servlet.http.HttpServlet
public abstract class HttpServlet extends GenericServlet{
    
    /* 更新不同请求方法，进行请求的分发 */
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String method = req.getMethod();

        if (method.equals(METHOD_GET)) {
            long lastModified = getLastModified(req);
            if (lastModified == -1) {
                doGet(req, resp);
            } else {
                long ifModifiedSince = req.getDateHeader(HEADER_IFMODSINCE);
                if (ifModifiedSince < lastModified) {
                    maybeSetLastModified(resp, lastModified);
                    doGet(req, resp);
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                }
            }

        } else if (method.equals(METHOD_HEAD)) {
            long lastModified = getLastModified(req);
            maybeSetLastModified(resp, lastModified);
            doHead(req, resp);

        } else if (method.equals(METHOD_POST)) {
            doPost(req, resp);
            
        } else if (method.equals(METHOD_PUT)) {
            doPut(req, resp);
            
        } else if (method.equals(METHOD_DELETE)) {
            doDelete(req, resp);
            
        } else if (method.equals(METHOD_OPTIONS)) {
            doOptions(req,resp);
            
        } else if (method.equals(METHOD_TRACE)) {
            doTrace(req,resp);
            
        } else {
            //请注意，这意味着没有 servlet 支持在此服务器上的任何地方请求的任何方法
            String errMsg = lStrings.getString("http.method_not_implemented");
            Object[] errArgs = new Object[1];
            errArgs[0] = method;
            errMsg = MessageFormat.format(errMsg, errArgs);
            
            resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, errMsg);
        }
    }
}
```

## 自定义Servlet

由上述可知，如果想自定义一个Servlet，则必须实现顶级接口Servlet

而HttpServlet/GenericServlet作为Servlet的抽象实现，也是比较倾向的抽象父类

现在一般都是实现HttpServlet来自定义Servlet，优势：

- 无需实现所有Servlet的方法
- 可以根据自己的请求方式，确定自己实现的处理方法
- 传入的参数类型为HttpServletRequest/HttpServletResponse，优于ServletRequest/ServletResponse，可以处理Http相关信息

## Web服务应用程序目录结构

```bash
# web服务目录结构示例
# src
	# main	主要业务代码存放路径
	# webapp 	存放的是需要部署到服务器的文件
		# index.html 	门户页面
		# JSP文件以及静态资源文件 
		# WEB-INF
			# web.xml	web服务配置文件，完成用户请求的逻辑名称到真正的servlet类的映射
			# classes	用于存放java字节码文件
			# lib		用于存放该工程用到的库，例如servlet-api.jar等等
		
		
# 注意
	# 凡是客户端能访问的资源(html或.jpg)必须跟WEB-INF在同一目录，即放在Web根目录(webapp)下的资源，从客户端是可以通过URL地址直接访问的。
	# 而WEB-INF目录下的资源，则是外界无法直接访问，由web服务器负责调用
```

web服务部署到Tomcat中，有以下几种方式：

- 一种部署方法是将应用程序目录直接复制到Tomcate的webapps目录下
- 可以通过在Tomcat的conf目录下编辑server.xml文件来部署应用程序
- 为了不用编辑server.xml,而单独部署一个XML文件到conf\Catalina\localhost目录下
- 将应用程序打包成war文件来进行部署.war文件是指以war作为扩展名的jar文件.

## web.xml配置文件

```xml
<!DOCTYPE web-app PUBLIC
		"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN"
		"http://java.sun.com/dtd/web-app_2_3.dtd" >

<web-app>
	<!--名称-->
	<display-name>Archetype Created Web Application</display-name>

	<!--描述-->
	<description></description>

	<!--是否可分布式处理 -->
	<distributable></distributable>

	<!--对应ServletContext参数，可以在servlet中用 getServletContext().getInitParameter("参数名称") 来取得-->
	<context-param>
		<param-name>参数名称</param-name>
		<param-value>参数值</param-value>
	</context-param>

	<!--过滤器声明-->
	<filter>
		<filter-name>setCharacterEncoding</filter-name>
		<filter-class>com.myTest.setCharacterEncodingFilter</filter-class>
		<!--参数定义-->
		<init-param>
			<param-name>encoding</param-name>
			<param-value>GB2312</param-value>
		</init-param>
	</filter>

	<!--过滤器映射-->
	<filter-mapping>
		<filter-name>setCharacterEncoding</filter-name>
		<url-pattern>/*</url-pattern>
	</filter-mapping>

	<!--servlet声明-->
	<servlet>
		<servlet-name>ShoppingServlet</servlet-name>
		<servlet-class>com.myTest.ShoppingServlet</servlet-class>
		<!--参数定义-->
		<init-param>
			<param-name>encoding</param-name>
			<param-value>GB2312</param-value>
		</init-param>
	</servlet>

	<!--servlet映射-->
	<servlet-mapping>
		<servlet-name>ShoppingServlet</servlet-name>
		<url-pattern>/shop/ShoppingServlet</url-pattern>
	</servlet-mapping>

	<!--首页列单-->
	<welcome-file-list>
		<welcome-file>index.jsp</welcome-file>
		<welcome-file>index.html</welcome-file>
	</welcom-file-list>

</web-app>
```

## 发布Servlet

我们定义好Servlet后，自然是想发布供请求访问，此时需要在web项目中发布，具体有以下两种发布方式：

- 配置在web.xml文件中

  ```xml
  <!DOCTYPE web-app PUBLIC
    "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN"
    "http://java.sun.com/dtd/web-app_2_3.dtd" >
  
  <web-app>
      <display-name>Archetype Created Web Application</display-name>
  
      <!--配置一个Servlet服务-->
      <servlet>
          <!--自定义Servlet名称-->
          <servlet-name>springmvc</servlet-name>
          <!--Servlet对应实现类-->
          <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
          <!--可以定义一些参数-->
          <init-param>
              <param-name>contextConfigLocation</param-name>
              <param-value>classpath*:springmvc.xml</param-value>
          </init-param>
      </servlet>
  
      <!--Servlet服务的映射，每个Servlet都要有一个对应的映射-->
      <servlet-mapping>
          <!--Servlet定义名称-->
          <servlet-name>springmvc</servlet-name>
          <!--
            方式一：带后缀，比如*.action  *.do *.aaa，拦截具体后缀的请求
                   该种方式比较精确、方便，在以前和现在企业中都有很大的使用比例
            方式二：/ 不会拦截 .jsp，但是会拦截.html等静态资源（静态资源：除了servlet和jsp之外的js、css、png等）
                  为什么配置为/ 会拦截静态资源？？？
                      因为tomcat容器中有一个web.xml（父），你的项目中也有一个web.xml（子），是一个继承关系
                            父web.xml中有一个DefaultServlet,  url-pattern 是一个 /
                            此时我们自己的web.xml中也配置了一个 / ,覆写了父web.xml的配置
                  为什么不拦截.jsp呢？
                      因为父web.xml中有一个JspServlet，这个servlet拦截.jsp文件，而我们并没有覆写这个配置，
                      所以springmvc此时不拦截jsp，jsp的处理交给了tomcat
  
                  如何解决/拦截静态资源这件事？
  
            方式三：/* 拦截所有，包括.jsp
          -->
          <!--拦截匹配规则的url请求，进入springmvc框架处理-->
          <url-pattern>/</url-pattern>
  </web-app>
  ```

- 使用@WebServlet注解

  - 在servlet3.0以后，我们可以不用再web.xml里面配置servlet，只需要加上@WebServlet注解就可以修改该servlet的属性了

  下面是@WebServlet的属性列表：

  | 属性名         | **类型**       | **描述**                                                     |
  | :------------- | :------------- | :----------------------------------------------------------- |
  | name           | String         | 指定Servlet 的 name 属性，等价于 <servlet-name>。如果没有显式指定，则该 Servlet 的取值即为类的全限定名。 |
  | value          | String[]       | 该属性等价于 urlPatterns 属性。两个属性不能同时使用。        |
  | urlPatterns    | String[]       | 指定一组 Servlet 的 URL 匹配模式。等价于<url-pattern>标签。  |
  | loadOnStartup  | int            | 指定 Servlet 的加载顺序，等价于 <load-on-startup>标签。      |
  | initParams     | WebInitParam[] | 指定一组 Servlet 初始化参数，等价于<init-param>标签。        |
  | asyncSupported | boolean        | 声明 Servlet 是否支持异步操作模式，等价于<async-supported> 标签。 |
  | description    | String         | 该 Servlet 的描述信息，等价于 <description>标签。            |
  | displayName    | String         | 该 Servlet 的显示名，通常配合工具使用，等价于 <display-name>标签。 |

  - 示例

  ```java
  @WebServlet(description = "a enter for wechat", urlPatterns = { "/aaa"},loadOnStartup=1)  
  public class WeChatIndexServlet extends HttpServlet {}
  ```

## 类库分析

### ServletConfig

对应于每个配置的Servlet对象的配置对象，在容器初始化Servlet时传递给Servlet对象

```java
//javax.servlet.ServletConfig
public interface ServletConfig {
    /* 获取当前Servlet对象名称 */
    public String getServletName();
    /* 获取当前web服务的ServletContext */
    public ServletContext getServletContext();
    /* 获取当前Servlet对象初始化参数 */
    public String getInitParameter(String name);
}
```

### ServletContext

https://blog.csdn.net/acm_lkl/article/details/78650091

- 即Servlet上下文是servlet与servlet容器之间的直接通信的接口
- Servlet容器启动一个Servlet服务时，创建唯一ServletContext
- 可以使用ServletContext存储全局唯一的一些数据
- 可以从ServletContext中访问webapp的资源

## IEAD配置tomcat

### 普通项目转为web项目

![image-20210709213048337](D:\学习整理\summarize\spring\图片\image-20210709213048337.png)

选择项目后

![image-20210709213158094](D:\学习整理\summarize\spring\图片\image-20210709213158094.png)



配置web路径信息

![image-20210709213311970](D:\学习整理\summarize\spring\图片\image-20210709213311970.png)

添加Artifacts

![image-20210709213405865](D:\学习整理\summarize\spring\图片\image-20210709213405865.png)

将当前项目依赖加载到lib中

![image-20210709213459807](D:\学习整理\summarize\spring\图片\image-20210709213459807.png)

添加tomcat启动

![image-20210709213538671](D:\学习整理\summarize\spring\图片\image-20210709213538671.png)

tomcat配置

![image-20210709213635342](D:\学习整理\summarize\spring\图片\image-20210709213635342.png)

配置deployment

![image-20210709213740296](D:\学习整理\summarize\spring\图片\image-20210709213740296.png)

## 理解ContextLoaderListener

### 监听器

监听器也就是Listener，用于监听Web服务的变化，包括但不限于监听客户端的请求、服务端的操作等

通过监听器，可以自动激发一些操作

#### 监听器分类

##### ServletContext监听器

- ServletContextListener
  - 监听ServletContext对象
- ServletContextAttributeListener
  - 监听对ServletContext属性的操作，比如增加、删除、修改

##### HttpSession监听器

-  HttpSessionListener
  - 监听Session对象
- HttpSessionAttributeListener
  - 监听Session中的属性操作
- HttpSessionActivationListener
  - 监听HTTP会话的active和passivate情况，passivate是指非活动的session被写入持久设备（比如硬盘），active相反。

注意：`HttpSessionActivationListener`不需要web.xml配置文件

```
实现了HttpSessionActivationListener接口的 JavaBean 对象可以感知自己被活化和钝化的事件

活化:javabean对象和Session一起被反序列化(活化)到内存中（硬盘到内存）；
钝化:javabean对象和Session一起序列化到硬盘中（内存到硬盘）；
javabean对象存在Session中，当服务器把session序列化到硬盘上时，如果Session中的javabean对象实现了Serializable接口
那么服务器会把session中的javabean对象一起序列化到硬盘上，javabean对象和Session一起被序列化到硬盘中的这个操作称之为钝化
如果Session中的javabean对象没有实现Serializable接口，那么服务器会先把Session中没有实现Serializable接口的javabean对象移除
然后再把Session序列化(钝化)到硬盘中；

当绑定到 HttpSession对象中的javabean对象将要随 HttpSession对象被钝化之前，
web服务器调用该javabean对象的 sessionWillPassivate方法，
这样javabean对象就可以知道自己将要和 HttpSession对象一起被序列化(钝化)到硬盘中
当绑定到HttpSession对象中的javabean对象将要随 HttpSession对象被活化之后，
web服务器调用该javabean对象的sessionDidActive方法，
这样javabean对象就可以知道自己将要和 HttpSession对象一起被反序列化(活化)回到内存中 
```

##### ServletRequest监听器

- ServletRequestListener
  - 监听Request对象
- ServletRequestAttributeListener
  - 监听Requset中的属性操作

### ServletContextListener

监听ServletContext对象的生命周期，实质就是监听Web应用的生命周期

```java
//javax.servlet.ServletContextListener
public interface ServletContextListener extends EventListener {

    /* 当Servlet 容器启动Web 应用时调用该方法。
    	在调用完该方法之后，容器再对Filter 初始化，并且对那些在Web 应用启动时就需要被初始化的Servlet 进行初始化 */
    public default void contextInitialized(ServletContextEvent sce) {}

    /* 当Servlet 容器终止Web 应用时调用该方法。在调用该方法之前，容器会先销毁所有的Servlet 和Filter 过滤器。 */
    public default void contextDestroyed(ServletContextEvent sce) {}
}
```

### ContextLoaderListener

实现了ServletContextListener接口，用于在Web应用中整合spring

- 在Web应用程序启动时载入Ioc容器，完成实际的`WebApplicationContext`，也就是Ioc容器的初始化工作

```java
//org.springframework.web.context.ContextLoaderListener
public class ContextLoaderListener extends ContextLoader implements ServletContextListener {
    @Override
	public void contextInitialized(ServletContextEvent event) {
        //初始化IOC容器，具体代码在其父类ContextLoader中
		initWebApplicationContext(event.getServletContext());
	}
}
```

### IOC容器创建

```bash
# 入口
	# org.springframework.web.context.ContextLoader#initWebApplicationContext
	
# 过程
	# 1. 判断是否已经存在WebApplicationContext，不可重复创建
		# 通过key：WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE，在ServletContext中获取
	
	# 2. 进行IOC容器创建
		# org.springframework.web.context.ContextLoader#createWebApplicationContext
		# 1. 获取容器类
			# org.springframework.web.context.ContextLoader#determineContextClass
			# 如果有配置参数contextClass，则使用配置的容器类
				# org.springframework.web.context.ContextLoader#CONTEXT_CLASS_PARAM
			# 如果没有，则获取默认配置类
				# org.springframework.web.context.support.XmlWebApplicationContext
				# 默认通过解析xml配置文件进行容器初始化
		# 2. 实例化容器
			# org.springframework.beans.BeanUtils#instantiateClass
			# 获取构造
				# java.lang.Class#getDeclaredConstructor
			# 实例化
				# java.lang.reflect.Constructor#newInstance
				
	# 3. 加载spring配置，初始化容器
		# org.springframework.web.context.ContextLoader#configureAndRefreshWebApplicationContext
		# 获取配置contextConfigLocation，即spring配置文件
			# org.springframework.web.context.ContextLoader#CONFIG_LOCATION_PARAM
		# 调用refresh方法
			# org.springframework.context.support.AbstractApplicationContext#refresh
			# 过程即spring初始化，不在赘述
			
	# 4. 将获取的IOC容器存放到ServletContext中
		# javax.servlet.ServletContext#setAttribute
		# key值
			# WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
			
	# 5. 将IOC容器存放，使用当前线程的线程上下文ClassLoader来映射
		# 存放容器
			# org.springframework.web.context.ContextLoader#currentContextPerThread = new ConcurrentHashMap<>(1);
		# key
			# Thread.currentThread().getContextClassLoader()
			
# 注意
	# 只配置contextConfigLocation参数，因为使用默认XmlWebApplicationContext作为容器，所以解析xml配置文件
	# 如果要解析 配置类，则需要进行以下两点
		# contextConfigLocation
			# 配置类全限定类名
		# contextClass
			# org.springframework.web.context.support.AnnotationConfigWebApplicationContext
			# 使用注解配置容器
	# web服务中获取webApplication容器
		# WebApplicationContextUtils.getWebApplicationContext(servletContext)
		# servletContext可从request对象中获取
```

# SpringMVC

## web.xml

```xml
<!DOCTYPE web-app PUBLIC
        "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN"
        "http://java.sun.com/dtd/web-app_2_3.dtd" >

<web-app>
    <display-name>Archetype Created Web Application</display-name>

    <servlet>
        <servlet-name>springmvc</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>classpath*:springmvc.xml</param-value>
        </init-param>
    </servlet>

    <servlet-mapping>
        <servlet-name>springmvc</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>
</web-app>

```

## SpringMVC配置文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:mvc="http://www.springframework.org/schema/mvc"
       xsi:schemaLocation="
        http://www.springframework.org/schema/beans
        https://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/context
        https://www.springframework.org/schema/context/spring-context.xsd
        http://www.springframework.org/schema/mvc
        https://www.springframework.org/schema/mvc/spring-mvc.xsd
">

    <!-- 扫描controller -->
    <context:component-scan base-package="com.learn.mvc.controller"></context:component-scan>

	<!-- 文件上传表单的视图解析器 -->
	<!--配置CommonsMultipartResolver实现类-->
	<bean id="multipartResolver" class="org.springframework.web.multipart.commons.CommonsMultipartResolver">
		<!-- 允许的文件最大大小，单位为字节 -->
		<property name="maxUploadSize" value="204800" />
	</bean>

    <!--配置视图解析器-->
    <bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="prefix" value="/WEB-INF/jsp/"></property>
        <property name="suffix" value=".jsp"></property>
    </bean>

    <!--自动最合适的处理映射器、处理适配器-->
    <mvc:annotation-driven/>
</beans>
```

## 核心构件

### DispatcherServlet

```bash
# 继承结构
	# DispatcherServlet -> FrameworkServlet -> HttpServletBean -> HttpServlet -> GenericServlet -> Servlet

# 所以DispatcherServlet是一个HttpServlet，配置在web.xml中可以拦截请求

# 处理入口
	# (在第一次接受请求时，Servlet对象进行初始化)
		# javax.servlet.GenericServlet#init
		# org.springframework.web.servlet.HttpServletBean#init
		# org.springframework.web.servlet.HttpServletBean#initServletBean
		# org.springframework.web.servlet.FrameworkServlet#initServletBean
		# 1. 获取当前web服务对应WebApplicationContext
			# org.springframework.web.context.support.WebApplicationContextUtils#getWebApplicationContext
			
		# 2. 创建当前Servlet对应WebApplicationContext
			# org.springframework.web.servlet.FrameworkServlet#createWebApplicationContext
			# 和前面创建WebApplicationContext一样，都是先获取contextClass参数，不然就创建默认XmlWebApplicationContext
			# 不同点
				# 此时将Web服务对应WebApplicationContext作为parent设置到当前Servlet对应WebApplicationContext
					# org.springframework.context.ConfigurableApplicationContext#setParent
					
		# 3. 进行配置解析
			# org.springframework.web.servlet.FrameworkServlet#configureAndRefreshWebApplicationContext
			# org.springframework.context.ConfigurableApplicationContext#refresh
```

#### 初始化

在第一次接受请求时，Servlet对象进行初始化，具体过程

```bash
# 入口
    # javax.servlet.GenericServlet#init
    # org.springframework.web.servlet.HttpServletBean#init
    # org.springframework.web.servlet.HttpServletBean#initServletBean
    # org.springframework.web.servlet.FrameworkServlet#initServletBean
    
# 过程    
    # 1. 获取当前web服务对应WebApplicationContext
   		# org.springframework.web.context.support.WebApplicationContextUtils#getWebApplicationContext

    # 2. 创建当前Servlet对应WebApplicationContext
        # org.springframework.web.servlet.FrameworkServlet#createWebApplicationContext
        # 和前面创建WebApplicationContext一样，都是先获取contextClass参数，不然就创建默认XmlWebApplicationContext
        # 不同点
        	# 此时将Web服务对应WebApplicationContext作为parent设置到当前Servlet对应WebApplicationContext
        		# org.springframework.context.ConfigurableApplicationContext#setParent

    # 3. 进行配置解析
        # org.springframework.web.servlet.FrameworkServlet#configureAndRefreshWebApplicationContext
        # org.springframework.context.ConfigurableApplicationContext#refresh
	
	# 4. 初始化springMVC的对象
		# org.springframework.web.servlet.DispatcherServlet#onRefresh
		# org.springframework.web.servlet.DispatcherServlet#initStrategies
```

#### initStrategies方法

```java
protected void initStrategies(ApplicationContext context) {
   initMultipartResolver(context);  //初始化multipartResolver
   initLocaleResolver(context);//初始化localeResolver
   initThemeResolver(context);//初始化themResolver
   initHandlerMappings(context);//初始化handerMappings
   initHandlerAdapters(context);//初始化handlerAdapters
   initHandlerExceptionResolvers(context);
   initRequestToViewNameTranslator(context);
   initViewResolvers(context);//初始化视图解析器
   initFlashMapManager(context);
}
```

#### 请求拦截处理

DispatcherServlet作为一个Servlet，所有映射请求都会由service方法进行拦截

```bash
# 由分析可知，service方法调用链为
# javax.servlet.http.HttpServlet#service
	# 将ServletRequest/ServletResponse替换为HttpServletRequest/HttpServletResponse
# org.springframework.web.servlet.FrameworkServlet#doService
# javax.servlet.http.HttpServlet#service
	# 根据请求方法进行请求的分发
# org.springframework.web.servlet.FrameworkServlet#(doGet/doPost等方法)
	# org.springframework.web.servlet.FrameworkServlet#processRequest
	# org.springframework.web.servlet.FrameworkServlet#doService
	# org.springframework.web.servlet.DispatcherServlet#doDispatch
		# 进行DispatcherServlet的请求处理入口
```

##### doDispatch方法

```java
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
    HttpServletRequest processedRequest = request;
    HandlerExecutionChain mappedHandler = null;
    boolean multipartRequestParsed = false;
    WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
    try {
        ModelAndView mv = null;
        Exception dispatchException = null;

        try {
            // 检查是否文件上传请求
            processedRequest = checkMultipart(request);
            multipartRequestParsed = (processedRequest != request);

            // 确定当前请求的处理程序。
            //获取handler处理器，此处返回HandlerExecutionChain：封装了Handler、Interceptor
            mappedHandler = getHandler(processedRequest);
            if (mappedHandler == null) {
                //如果没有对应mappedHandler，返回404
                noHandlerFound(processedRequest, response);
                return;
            }

            // 确定当前请求的处理程序适配器.
            HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

            // 处理 last-modified 请求头
            String method = request.getMethod();
            boolean isGet = "GET".equals(method);
            if (isGet || "HEAD".equals(method)) {
                long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
                if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
                    return;
                }
            }

            if (!mappedHandler.applyPreHandle(processedRequest, response)) {
                return;
            }

            // 实际调用处理程序。
            mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

            if (asyncManager.isConcurrentHandlingStarted()) {
                return;
            }

            //结果视图处理
            applyDefaultViewName(processedRequest, mv);
            mappedHandler.applyPostHandle(processedRequest, response, mv);
        }
        catch (Exception ex) {
            dispatchException = ex;
        }
        catch (Throwable err) {
            // As of 4.3, we're processing Errors thrown from handler methods as well,
            // making them available for @ExceptionHandler methods and other scenarios.
            dispatchException = new NestedServletException("Handler dispatch failed", err);
        }
        //页面渲染调用
        processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
    }
    catch (Exception ex) {
        //异常处理
        triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
    }
    catch (Throwable err) {
        triggerAfterCompletion(processedRequest, response, mappedHandler,
                               new NestedServletException("Handler processing failed", err));
    }
    finally {
        if (asyncManager.isConcurrentHandlingStarted()) {
            // Instead of postHandle and afterCompletion
            if (mappedHandler != null) {
                mappedHandler.applyAfterConcurrentHandlingStarted(processedRequest, response);
            }
        }
        else {
            // Clean up any resources used by a multipart request.
            if (multipartRequestParsed) {
                cleanupMultipart(processedRequest);
            }
        }
    }
}
```

### AnnotationDrivenBeanDefinitionParser

```bash
# springMVC配置文件里，有一个特殊配置元素
	# mvc:annotation-driven

# 由SpringIOC容器解析过程可知，对应于不同xml元素，会获取不同的解析parser进行处理
	# 处理mvc:annotation-driven元素的即是AnnotationDrivenBeanDefinitionParser
	
# AnnotationDrivenBeanDefinitionParser主要作用是注册默认SpringMVC组件用于后续处理使用	
# 入口
	# org.springframework.web.servlet.config.AnnotationDrivenBeanDefinitionParser#parse
```

默认组件：

- HandlerMapping
  - RequestMappingHandlerMapping
    - 用于将请求映射到带注释的控制器
  - BeanNameUrlHandlerMapping
    - 将URL路径映射到控制器 Bean 名称
- HandlerAdapter
  - RequestMappingHandlerAdapter
    - 用于处理带有注释的控制器方法的请求
  - HttpRequestHandlerAdapter
    - 用于使用HttpRequestHandlers处理请求
  - SimpleControllerHandlerAdapter
    - 用于使用基于接口的Controllers处理请求
- HandlerExceptionResolver
  - ExceptionHandlerExceptionResolver	
    - 通过ExceptionHandler处理异常
  - ResponseStatusExceptionResolver
    - 处理ResponseStatus注释的异常
  - DefaultHandlerExceptionResolver
    - 解决已知类型的异常
- AntPathMatcher
- UrlPathHelper

## springMVC组件

### MultipartResolver(多文件解析器) 

主要作用：

- 将内容类型(`Content-Type` 为 `multipart/*` 的请求的解析器接口
- `MultipartResolver` 会将 `HttpServletRequest` 封装成 `MultipartHttpServletRequest`

- 从`request`请求中获取文件内容，提取为`MultipartFile`参数

`MultipartResolver`是一个接口

```java
// org.springframework.web.multipart.MultipartResolver
public interface MultipartResolver {

	/* 判断当前请求是否为 multipart 请求*/
	boolean isMultipart(HttpServletRequest request);

	/*将 HttpServletRequest 请求封装成 MultipartHttpServletRequest 对象*/
	MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException;

	/*清理处理 multipart 产生的资源，例如临时文件*/
	void cleanupMultipart(MultipartHttpServletRequest request);
}
```

#### 依赖

SpringMVC中使用`MultipartResolver`时需要引入文件上传的依赖

```xml
<dependency>  
    <groupId>commons-fileupload</groupId> 
    <artifactId>commons-fileupload</artifactId>  
    <version>1.3.1</version>
</dependency>
<dependency>
    <groupId>commons-io</groupId>
    <artifactId>commons-io</artifactId>
    <version>1.3.2</version>
</dependency>
```

#### 配置

从上述`AnnotationDrivenBeanDefinitionParser`的处理可知，并没有默认初始化`MultipartResolver`

所以我们在使用时，需要配置所需的`MultipartResolver`

```xml
<!-- 文件上传表单的视图解析器 -->
<!--配置CommonsMultipartResolver实现类-->
<bean id="multipartResolver" class="org.springframework.web.multipart.commons.CommonsMultipartResolver">
    <!-- 允许的文件最大大小，单位为字节 -->
    <property name="maxUploadSize" value="204800" />
</bean>
```

#### 使用

在SpringMVC中使用`MultipartResolver`接收上传的文件内容，不需要手动进行文件提取

仅仅需要在`Controller`的方法参数中添加`MultipartFile`参数即可

```java
@RequestMapping(value = "/upload")
//使用MultipartFile类型接收上传文件，如果同时上传多个文件，可以使用Map集合封装
public ModelAndView upload(MultipartFile uploadFile, HttpSession session) throws IOException {}
```

#### 请求处理源码解析

正常使用时，我们需要先加载`MultipartResolver`

1. 由于我们在配置文件中配置了`multipartResolver`，所以就会在`DispatcherServlet`的初始化创建IOC容器时，创建并保存到容器中
2. 在初始化过程中，会调用`initStrategies`方法，此时将调用`initMultipartResolver`方法将`multipartResolver`注册到`DispatcherServlet`中

```java
//org.springframework.web.servlet.DispatcherServlet#initMultipartResolver
private void initMultipartResolver(ApplicationContext context) {
    //从容器中获取 id：multipartResolver，类型：MultipartResolver 的MultipartResolver注册到当前DispatcherServlet上
    this.multipartResolver = context.getBean(MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);
}
```

在请求时，进行`multipartResolver`的处理：

```java
入口：
	org.springframework.web.servlet.DispatcherServlet#doDispatch
	org.springframework.web.servlet.DispatcherServlet#checkMultipart
    
protected HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
    //通过isMultipart检查是否文件上传请求
    if (this.multipartResolver != null && this.multipartResolver.isMultipart(request)) {
        //如果是文件上传请求，则通过multipartResolver处理request对象，响应包装的MultipartHttpServletRequest对象
        return this.multipartResolver.resolveMultipart(request);
    }
    return request;
}    
```

`isMultipart`处理，实质是调用了`ServletFileUpload.isMultipartContent(request)`

```java
//org.apache.commons.fileupload.servlet.ServletFileUpload#isMultipartContent
public static final boolean isMultipartContent(
        HttpServletRequest request) {
    //非post请求，不是文件上传请求
    if (!"post".equals(request.getMethod().toLowerCase())) {
        return false;
    }
    String contentType = request.getContentType();
    if (contentType == null) {
        return false;
    }
    //contentType需要以 multipart/ 为前缀，表示是文件上传请求
    if (contentType.toLowerCase().startsWith(MULTIPART)) {
        return true;
    }
    return false;
}
```

`resolveMultipart`处理，实质是调用了`CommonsMultipartResolver#parseRequest`

```java
//org.springframework.web.multipart.commons.CommonsMultipartResolver#parseRequest
protected MultipartParsingResult parseRequest(HttpServletRequest request) throws MultipartException {
    //获取当前请求的编码：javax.servlet.ServletRequest#getCharacterEncoding，没有则使用默认编码：ISO-8859-1
    String encoding = determineEncoding(request);
    //获取当前编码对应的 文件上传对象
    FileUpload fileUpload = prepareFileUpload(encoding);
    try {
        //从请求中获取文件列表
        List<FileItem> fileItems = ((ServletFileUpload) fileUpload).parseRequest(request);
        //根据当前文件列表信息，获取MultipartParsingResult
        return parseFileItems(fileItems, encoding);
    }
}
```

我们主要分析一下`parseFileItems`方法

```java
//org.springframework.web.multipart.commons.CommonsFileUploadSupport#parseFileItems
protected MultipartParsingResult parseFileItems(List<FileItem> fileItems, String encoding) {
    MultiValueMap<String, MultipartFile> multipartFiles = new LinkedMultiValueMap<>();
    Map<String, String[]> multipartParameters = new HashMap<>();
    Map<String, String> multipartParameterContentTypes = new HashMap<>();

    // 遍历处理fileItems列表
    for (FileItem fileItem : fileItems) {
        // 使用CommonsMultipartFile包装fileItem对象
        CommonsMultipartFile file = createMultipartFile(fileItem);
        multipartFiles.add(file.getName(), file);
    }
	//封装MultipartParsingResult
    return new MultipartParsingResult(multipartFiles, multipartParameters, multipartParameterContentTypes);
}
```

最后将`MultipartParsingResult`转换为`MultipartHttpServletRequest`

```java
new DefaultMultipartHttpServletRequest(request, parsingResult.getMultipartFiles(),
					parsingResult.getMultipartParameters(), parsingResult.getMultipartParameterContentTypes());
```

#### 总结

```
multipartResolver主要作用：
	1. 判断当前请求是否文件上传请求
	2. 如果是文件上传请求，则需要提取请求中文件内容，最后封装到MultipartHttpServletRequest的multipartFiles属性中
	
multipartFiles：MultiValueMap<String, MultipartFile>
	key：文件名称
	value：当前文件对应MultipartFile对象
```

### HandlerMapping(处理器映射器)

主要作用：

- 初始化解析当前项目中的`handler`，并注册保存
- 根据当前请求对象，查找对应处理`handler`
- 解析当前请求，获取其中信息，然后将找到的 `Handler` 和所有匹配的 `HandlerInterceptor`（拦截器）封装创建 `HandlerExecutionChain` 对象

`HandlerMapping`是一个接口

```java
//org.springframework.web.servlet.HandlerMapping
public interface HandlerMapping {
    //获取当前request匹配的HandlerExecutionChain
    HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception;
}
```

由上述`AnnotationDrivenBeanDefinitionParser`的处理可知，将会默认加载

- RequestMappingHandlerMapping
- BeanNameUrlHandlerMapping

我们常用的是`RequestMappingHandlerMapping`，所以具体分析一下

#### 类图

![image-20210720202925684](D:\学习整理\summarize\spring\图片\RequestMappingHandlerMapping类图)

#### 初始化

由于`RequestMappingHandlerMapping`实现了`InitializingBean`接口，所以可以通过`afterPropertiesSet`方法进行初始化操作

实际的处理动作实在其抽象父类`AbstractHandlerMethodMapping.initHandlerMethods`中

```java
//RequestMappingHandlerMapping
public void afterPropertiesSet() {
    //初始化config
    this.config = new RequestMappingInfo.BuilderConfiguration();
    this.config.setUrlPathHelper(getUrlPathHelper());
    this.config.setPathMatcher(getPathMatcher());
    this.config.setSuffixPatternMatch(this.useSuffixPatternMatch);
    this.config.setTrailingSlashMatch(this.useTrailingSlashMatch);
    this.config.setRegisteredSuffixPatternMatch(this.useRegisteredSuffixPatternMatch);
    this.config.setContentNegotiationManager(getContentNegotiationManager());
	//调用父类afterPropertiesSet
    super.afterPropertiesSet();
}

//AbstractHandlerMethodMapping
public void afterPropertiesSet() {
    initHandlerMethods();
}

//AbstractHandlerMethodMapping
protected void initHandlerMethods() {
    //获取容器中所有Object对象
    for (String beanName : getCandidateBeanNames()) {
        //beanName不以 scopedTarget. 为前缀；主要作用是排除代理域
        if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
            //检测当前Bean对象，提取HandlerMethod
            processCandidateBean(beanName);
        }
    }
    //getHandlerMethods：从mappingRegistry中获取注册的HandlerMethod
    handlerMethodsInitialized(getHandlerMethods());
}
```

下面我们来具体分析一下

`getCandidateBeanNames`方法

```java
//通过detectHandlerMethodsInAncestorContexts参数判断，是否仅仅取当前DispatcherServlet对应的webApplication容器中对象，还是还要从父容器(web.xml中通过ContextLoaderListener创建的webApplication)中获取对象
//注意：detectHandlerMethodsInAncestorContexts默认值为false
protected String[] getCandidateBeanNames() {
    return (this.detectHandlerMethodsInAncestorContexts ?
            BeanFactoryUtils.beanNamesForTypeIncludingAncestors(obtainApplicationContext(), Object.class) :
            obtainApplicationContext().getBeanNamesForType(Object.class));
}
```

重点分析一下`processCandidateBean`处理

```java
//org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#processCandidateBean
protected void processCandidateBean(String beanName) {
    Class<?> beanType = null;
    //从容器中获取当前beanName对应Class类型
    beanType = obtainApplicationContext().getType(beanName);
    //通过isHandler判断是否 handler 类
    if (beanType != null && isHandler(beanType)) {
        //解析handler的Class
        detectHandlerMethods(beanName);
    }
}
方法小结：获取类对应Class，判断其是否 handler 类，如果是，则解析对应 handler 方法
    

//org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping#isHandler
protected boolean isHandler(Class<?> beanType) {
    //获取当前Class上对应Controller/RequestMapping注解
    return (AnnotatedElementUtils.hasAnnotation(beanType, Controller.class) ||
            AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping.class));
}
方法小结：如果类上有Controller/RequestMapping类型注解，则表示是 handler 处理类


//org.springframework.core.annotation.AnnotatedElementUtils#hasAnnotation
public static boolean hasAnnotation(AnnotatedElement element, Class<? extends Annotation> annotationType) {
    // 直接从元素上获取目标注解
    if (element.isAnnotationPresent(annotationType)) {
        return true;
    }
    //没有则通过org.springframework.core.annotation.AnnotatedElementUtils#searchWithFindSemantics进行跟踪搜索
    return Boolean.TRUE.equals(searchWithFindSemantics(element, annotationType, null, alwaysTrueAnnotationProcessor));
}
方法小结：判断Class上是否有目标注解，优先判断直接注解；如果没有，则向上搜索


//重点还是在于detectHandlerMethods方法：
//org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#detectHandlerMethods
protected void detectHandlerMethods(Object handler) {
    //获取处理类Class。注意：此处处理了CGLIB代理，如果是CGLIB代理类，则获取其父类(即我们的目标类)
	Class<?> userType = ClassUtils.getUserClass(handlerType);
    //通过selectMethods获取目标Class中对应handler方法
	Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
			(MethodIntrospector.MetadataLookup<T>) method -> {
				try {
                    //将handler方法解析为RequestMappingInfo
					return getMappingForMethod(method, userType);
				}
				catch (Throwable ex) {
					throw new IllegalStateException("Invalid mapping on handler class [" +
							userType.getName() + "]: " + method, ex);
				}
			});
	//注册handlerMethod
	methods.forEach((method, mapping) -> {
        //获取处理方法
		Method invocableMethod = AopUtils.selectInvocableMethod(method, userType);
        //注册HandlerMethod
		registerHandlerMethod(handler, invocableMethod, mapping);
	});
}
方法小结：
    1. 通过ClassUtils.getUserClass方法，处理CGLIB代理的影响，获取目标处理类
    2. 通过selectMethods方法，查找目标处理类中的处理方法
    3. 通过getMappingForMethod方法，将目标处理方法封装
    4. 通过registerHandlerMethod方法，将解析后的HandlerMethod注册到mappingRegistry中


//分析selectMethods方法
//org.springframework.core.MethodIntrospector#selectMethods
public static <T> Map<Method, T> selectMethods(Class<?> targetType, final MetadataLookup<T> metadataLookup) {
    //存储解析结果
    final Map<Method, T> methodMap = new LinkedHashMap<>();
    //存储需要处理的Class类
    Set<Class<?>> handlerTypes = new LinkedHashSet<>();
    //保存具体的类
    Class<?> specificHandlerType = null;
	
    //前面根据ClassUtils.getUserClass，已经处理了CGLIB代理；此处处理JDK代理影响
    //如果不是JDK代理类，则保存当前类
    if (!Proxy.isProxyClass(targetType)) {
        specificHandlerType = ClassUtils.getUserClass(targetType);
        handlerTypes.add(specificHandlerType);
    }
    
    //获取当前类的所有接口，如果是JDK代理，则获取代理的目标接口；接口方法上也可能有 @RequestMapping 注解
    //通过set集合进行去重
    handlerTypes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetType));

    for (Class<?> currentHandlerType : handlerTypes) {
        final Class<?> targetClass = (specificHandlerType != null ? specificHandlerType : currentHandlerType);
		
        //doWithMethods获取目标类中方法，调用回调，但是会剔除一些无需处理的方法，例如toString()等等
        ReflectionUtils.doWithMethods(currentHandlerType, method -> {
            Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
            //通过回调函数，获取RequestMappingInfo
            T result = metadataLookup.inspect(specificMethod);
            if (result != null) {
                Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
                if (bridgedMethod == specificMethod || metadataLookup.inspect(bridgedMethod) == null) {
                    //将RequestMappingInfo保存到map中
                    methodMap.put(specificMethod, result);
                }
            }
        }, ReflectionUtils.USER_DECLARED_METHODS);
    }

    return methodMap;
}
方法小结：selectMethods只是用于避免JDK代理/无用方法剔除，具体进行 method -> RequestMappingInfo，是通过回调调用getMappingForMethod方法实现
    

//org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping#getMappingForMethod    
protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
    //从当前方法上获取RequestMappingInfo
    RequestMappingInfo info = createRequestMappingInfo(method);
    if (info != null) {
        //尝试从类上获取RequestMappingInfo
        RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);
        if (typeInfo != null) {
            //如果类上也有 RequestMappingInfo，将类/方法上的RequestMappingInfo整合
            info = typeInfo.combine(info);
        }
    }
    return info;
}    
方法小结： 
    1. 获取处理方法上@RequestMapping注解信息
    2. 获取处理类上@RequestMapping注解信息
    3. 将两者组合
    4. 进行前缀处理
    5. 将获取的RequestMappingInfo返回

    
//org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping#createRequestMappingInfo   
private RequestMappingInfo createRequestMappingInfo(AnnotatedElement element) {
    //获取当前元素上@RequestMapping注解信息
    RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
    //创建RequestMappingInfo，并返回
    return (requestMapping != null ? createRequestMappingInfo(requestMapping, condition) : null);
}    
方法小结：主要就是解析@RequestMapping信息，封装为RequestMappingInfo
    
//org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#registerHandlerMethod    
protected void registerHandlerMethod(Object handler, Method method, T mapping) {
    //注册方法到mappingRegistry中，封装的HandlerMethod包括处理类对象 handler、处理方法 method；key 为 mapping信息
    this.mappingRegistry.register(mapping, handler, method);
}  
    
总结：
    processCandidateBean的总体处理作用
    	1. 获取当前所有容器中的Object对象
    	2. 进行包括CGLIB、JDK代理的处理
    	3. 提取所有@RequestMapping注解信息，封装为RequestMappingInfo
    	4. 注册HandlerMethod到mappingRegistry中
```

#### 注册

与其他组件一致，注册入口都是在`initStrategies`方法中，注册`HandlerMapping`的方法为：`initHandlerMappings`

```java
private void initHandlerMappings(ApplicationContext context) {
    this.handlerMappings = null;
    //判断是否检测所有HandlerMappings，还是只检测名称为handlerMapping的；默认值为true
    if (this.detectAllHandlerMappings) {
        //在 ApplicationContext 中查找所有 HandlerMappings，包括祖先上下文。
        Map<String, HandlerMapping> matchingBeans =
            BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerMapping.class, true, false);
        if (!matchingBeans.isEmpty()) {
            this.handlerMappings = new ArrayList<>(matchingBeans.values());
            // 将handlerMappings排序
            AnnotationAwareOrderComparator.sort(this.handlerMappings);
        }
    }
    else {
        	//获取名称为：handlerMapping，类型为：HandlerMapping 的处理器映射器
            HandlerMapping hm = context.getBean(HANDLER_MAPPING_BEAN_NAME, HandlerMapping.class);
            this.handlerMappings = Collections.singletonList(hm);
    }

    //如果前面没有获取，则注册一个默认的HandlerMapping
    if (this.handlerMappings == null) {
        this.handlerMappings = getDefaultStrategies(context, HandlerMapping.class);
    }
}
```

#### 请求处理源码解析

前面`RequestMappingHandlerMapping`初始化时，已经将当前项目中的`handler`都解析注册了，所以在请求是可以根据请求路径，映射获取 `handler`进行处理

 处理入口是：`org.springframework.web.servlet.DispatcherServlet#getHandler`

```java
//org.springframework.web.servlet.DispatcherServlet#doDispatch
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
    //尝试获取mappedHandler
    HandlerExecutionChain mappedHandler = getHandler(processedRequest);
    if (mappedHandler == null) {
        //如果没有对应mappedHandler，返回404
        noHandlerFound(processedRequest, response);
        return;
    }
}

//遍历已注册的handlerMapping，尝试根据请求获取对应HandlerExecutionChain
//org.springframework.web.servlet.DispatcherServlet#getHandler
protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
    if (this.handlerMappings != null) {
        for (HandlerMapping mapping : this.handlerMappings) {
            HandlerExecutionChain handler = mapping.getHandler(request);
            if (handler != null) {
                return handler;
            }
        }
    }
    return null;
}
```

所以我们此时要解析一下`RequestMappingHandlerMapping.getHandler`方法

实质是调用到抽象父类`AbstractHandlerMapping.getHandler`方法

```java
//org.springframework.web.servlet.handler.AbstractHandlerMapping#getHandler
public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
    //获取与当前request映射的HandlerMethod
    Object handler = getHandlerInternal(request);
    //封装为HandlerExecutionChain
    HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);
    return executionChain;
}
```

由上可知，具体的映射处理是在`getHandlerInternal`方法中，此方法由`AbstractHandlerMethodMapping`实现

```java
//org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#getHandlerInternal
protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
    //通过UrlPathHelper获取当前请求path
    //request.getContextPath() + request.getRequestURI() 获取请求path
    String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
    //根据请求和请求路径，获取对应HandlerMethod
    //主要进行了HandlerInterceptor的获取封装
    HandlerMethod handlerMethod = lookupHandlerMethod(lookupPath, request);
    return (handlerMethod != null ? handlerMethod.createWithResolvedBean() : null);
}

//org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#lookupHandlerMethod
protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {
    //从mappingRegistry获取当前lookupPath对应requestMappingInfo
    List<T> directPathMatches = this.mappingRegistry.getMappingsByUrl(lookupPath);
    //从mappingRegistry根据requestMappingInfo获取对应HandlerMethod，并添加到matches中
    addMatchingMappings(directPathMatches, matches, request);
    Match bestMatch = matches.get(0);
    //获取HandlerMethod
    return bestMatch.handlerMethod;
}

总结：
    1. 获取请求URL
    2. 获取对应HandlerMethod
    3. 获取对应HandlerInterceptor
    4. 封装HandlerExecutionChain，内容包括HandlerMethod、HandlerInterceptor
```

### HandlerAdapter(处理器适配器)

注册过程同样在`AnnotationDrivenBeanDefinitionParser`中，默认注册的处理器适配器有：

- RequestMappingHandlerAdapter
- HttpRequestHandlerAdapter
- SimpleControllerHandlerAdapter

`HandlerAdapter`是一个接口

```java
//org.springframework.web.servlet.HandlerAdapter
public interface HandlerAdapter {
	/* 判断当前HandlerAdapter是否支持目标Handler */
	boolean supports(Object handler);

	/* 进行handle调用处理，获取ModelAndView*/
	ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception;
}
```

我们主要分析与`RequestMappingHandlerMapping`配套的`RequestMappingHandlerAdapter`

#### 类图

![image-20210720205418954](D:\学习整理\summarize\spring\图片\RequestMappingHandlerAdapter类图)

#### 注册

与其他组件一致，注册入口都是在`initStrategies`方法中，注册`handlerAdapter`的方法为：`initHandlerAdapters`

```java
private void initHandlerAdapters(ApplicationContext context) {
    this.handlerAdapters = null;
	
    //通过detectAllHandlerAdapters参数判断，是否获取所有HandlerAdapter类型对象，还是仅获取beanName为handlerAdapter的
    if (this.detectAllHandlerAdapters) {
        // 获取所有HandlerAdapter类型对象，包括父IOC容器
        Map<String, HandlerAdapter> matchingBeans =
            BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerAdapter.class, true, false);
        if (!matchingBeans.isEmpty()) {
            this.handlerAdapters = new ArrayList<>(matchingBeans.values());
            AnnotationAwareOrderComparator.sort(this.handlerAdapters);
        }
    }
    else {
        //获取beanName为handlerAdapter，类型为HandlerAdapter的 HandlerAdapter对象
        HandlerAdapter ha = context.getBean(HANDLER_ADAPTER_BEAN_NAME, HandlerAdapter.class);
        this.handlerAdapters = Collections.singletonList(ha);

    }
	
    //如果获取不到，注册默认的HandlerAdapter对象
    if (this.handlerAdapters == null) {
        this.handlerAdapters = getDefaultStrategies(context, HandlerAdapter.class);
    }
}
```

#### 请求处理源码分析

`HandlerAdapter`对请求的拦截处理包括两步：

1. 根据前面获取的`HandlerExecutionChain`，获取匹配的`HandlerAdapter`
2. 调用`HandlerAdapter.handle`方法进行处理

**第一步**分析入口：`getHandlerAdapter`方法

```java
//org.springframework.web.servlet.DispatcherServlet#doDispatch
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
    // 确定当前请求的处理程序适配器.
    HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
    // 实际调用处理程序。
    mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
}

//org.springframework.web.servlet.DispatcherServlet
protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
    if (this.handlerAdapters != null) {
        //遍历获取支持当前handler的HandlerAdapter
        for (HandlerAdapter adapter : this.handlerAdapters) {
            if (adapter.supports(handler)) {
                return adapter;
            }
        }
    }
}
```

由于我们分析的是`RequestMappingHandlerAdapter`，所以就分析一下`RequestMappingHandlerAdapter.supports`方法

实质是调用抽象父类中`AbstractHandlerMethodAdapter.supports`

```java
//org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter#supports
public final boolean supports(Object handler) {
    return (handler instanceof HandlerMethod && supportsInternal((HandlerMethod) handler));
}

//org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#supportsInternal
protected boolean supportsInternal(HandlerMethod handlerMethod) {
    return true;
}

总结：由上述方法可知，AbstractHandlerMethodAdapter专门处理HandlerMethod
```

**第二步**分析入口：`RequestMappingHandlerAdapter.handle`

实质是调用了`RequestMappingHandlerAdapter.invokeHandlerMethod`

```java
//org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#handleInternal
protected ModelAndView invokeHandlerMethod(HttpServletRequest request,
                                           HttpServletResponse response, HandlerMethod handlerMethod) {
    
	ServletWebRequest webRequest = new ServletWebRequest(request, response);
    ModelAndViewContainer mavContainer = new ModelAndViewContainer();
    
    //方法调用入口，并将返回值封装为一个ModelAndView对象
    invocableMethod.invokeAndHandle(webRequest, mavContainer);

    return getModelAndView(mavContainer, modelFactory, webRequest);
}
方法小结：此方法分为两个重要部分
    1. invokeAndHandle：进行参数处理、方法调用、返回值处理
    2. getModelAndView：提取ModelAndView对象返回
    


//org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod#invokeAndHandle
public void invokeAndHandle(ServletWebRequest webRequest, ModelAndViewContainer mavContainer) {
    //先进行参数处理，再进行HandlerMethod方法调用
    Object returnValue = invokeForRequest(webRequest, mavContainer, providedArgs);
    //处理返回值，将结果保存到mavContainer中
    this.returnValueHandlers.handleReturnValue(returnValue, getReturnValueType(returnValue), mavContainer, webRequest);
}
方法小结：此方法分为两个重要部分
    1. invokeForRequest：进行参数处理、方法调用
    2. handleReturnValue：进行返回值处理
```

下面我们分析`invokeForRequest`方法

```java
//org.springframework.web.method.support.InvocableHandlerMethod#invokeForRequest
public Object invokeForRequest(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer,
                               Object... providedArgs) throws Exception {

    //参数获取，使用参数解析器进行处理
    Object[] args = getMethodArgumentValues(request, mavContainer, providedArgs);
    //反射进行方法调用
    return doInvoke(args);
}

//org.springframework.web.method.support.InvocableHandlerMethod#getMethodArgumentValues
protected Object[] getMethodArgumentValues(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer,
                                           Object... providedArgs) throws Exception {
    //获取当前方法参数列表
    MethodParameter[] parameters = getMethodParameters();
    if (ObjectUtils.isEmpty(parameters)) {
        //没有参数，提供空数组
        return EMPTY_ARGS;
    }

    Object[] args = new Object[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
        //使用参数解析器，进行参数解析
        //参数解析器使用后面分析
        args[i] = this.resolvers.resolveArgument(parameter, mavContainer, request, this.dataBinderFactory);
    }
    return args;
}
```

继续分析`handleReturnValue`方法

```java
public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
                              ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
	//选择对应的HandlerMethodReturnValueHandler
    HandlerMethodReturnValueHandler handler = selectHandler(returnValue, returnType);
	//调用handleReturnValue进行返回值处理，具体分析看后面
    handler.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
}
```

#### 参数解析器

对于参数解析器的分析，我们主要分析处理`@RequestBody`和`@RequestBody`的`RequestResponseBodyMethodProcessor`

类图

![image-20210720215434302](D:\学习整理\summarize\spring\图片\RequestResponseBodyMethodProcessor类图)

**步骤1**：参数解析器中，主要通过`MessageConverter`进行参数的转换，所以先要进行`MessageConverter`的加载

##### `HttpMessageConverter`接口

```java
public interface HttpMessageConverter<T> {
	/* 该方法指定转换器可以读取的对象类型，即转换器可将请求信息转换为clazz类型的对象，同时指定支持的MIME类型(text/html、application/json等) */
	boolean canRead(Class<?> clazz, @Nullable MediaType mediaType);

	/* 该方法指定转换器可以讲clazz类型的对象写到响应流当中，响应流支持的媒体类型在mediaType中定义 */
	boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType);

	/* 该方法返回当前转换器支持的媒体类型 */
	List<MediaType> getSupportedMediaTypes();

	/* 该方法将请求信息转换为T类型的对象 */
	T read(Class<? extends T> clazz, HttpInputMessage inputMessage);

	/* 该方法将T类型的对象写到响应流当中，同事指定响应的媒体类型为contentType */
	void write(T t, @Nullable MediaType contentType, HttpOutputMessage outputMessage);
}
```

对于`RequestResponseBodyMethodProcessor`的加载，是通过`AnnotationDrivenBeanDefinitionParser`加载解析`<mvc:annotation-driven/>`标签实现

在`RequestResponseBodyMethodProcessor`中，会根据类加载其对特定类的加载情况，设置默认静态属性值

```java
class AnnotationDrivenBeanDefinitionParser implements BeanDefinitionParser {
    private static final boolean javaxValidationPresent;
    private static boolean romePresent;
    private static final boolean jaxb2Present;
    private static final boolean jackson2Present;
    private static final boolean jackson2XmlPresent;
    private static final boolean jackson2SmilePresent;
    private static final boolean jackson2CborPresent;
    private static final boolean gsonPresent;

    static {
        ClassLoader classLoader = AnnotationDrivenBeanDefinitionParser.class.getClassLoader();
        javaxValidationPresent = ClassUtils.isPresent("javax.validation.Validator", classLoader);
        romePresent = ClassUtils.isPresent("com.rometools.rome.feed.WireFeed", classLoader);
        jaxb2Present = ClassUtils.isPresent("javax.xml.bind.Binder", classLoader);
        jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
            ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
        jackson2XmlPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader);
        jackson2SmilePresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
        jackson2CborPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.cbor.CBORFactory", classLoader);
        gsonPresent = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
    }
}
```

而在`parse`方法中，会调用`getMessageConverters`方法实现对`MessageConverter`对象的加载，再设置到对应`handlerAdapter`中

```java
public BeanDefinition parse(Element element, ParserContext context) {
    //解析获取MessageConverter列表
	ManagedList<?> messageConverters = getMessageConverters(element, source, context);
    //将messageConverters 设置到 handlerAdapter 的 messageConverters 属性中，此时保存在BeanDefinition中
    handlerAdapterDef.getPropertyValues().add("messageConverters", messageConverters);
}
```

对于处理`@RequestBody`和`@RequestBody`，需要通过`jackson`进行处理，所以在`getMessageConverters`方法中

```java
//org.springframework.web.servlet.config.AnnotationDrivenBeanDefinitionParser#getMessageConverters
private ManagedList<?> getMessageConverters(Element element, @Nullable Object source, ParserContext context) {
    //如果能jackson2XmlPresent参数为true，则加载MappingJackson2XmlHttpMessageConverter用于处理`@RequestBody`和`@RequestBody`
    if (jackson2XmlPresent) {
        Class<?> type = MappingJackson2XmlHttpMessageConverter.class;
        RootBeanDefinition jacksonConverterDef = createConverterDefinition(type, source);
        GenericBeanDefinition jacksonFactoryDef = createObjectMapperFactoryDefinition(source);
        jacksonFactoryDef.getPropertyValues().add("createXmlMapper", true);
        jacksonConverterDef.getConstructorArgumentValues().addIndexedArgumentValue(0, jacksonFactoryDef);
        messageConverters.add(jacksonConverterDef);
    }
}
```

**步骤2**：在处理请求时，对`@RequestBody`和`@RequestBody`功能进行实现

我们先分析`@RequestBody`的处理，处理入口：`RequestResponseBodyMethodProcessor#resolveArgument`

##### `@RequestBody`处理

该方法继承自`HandlerMethodArgumentResolver`接口

```java
//org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor#resolveArgument
public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
                              NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
    parameter = parameter.nestedIfOptional();
	//通过messageConverter进行参数转换
    Object arg = readWithMessageConverters(webRequest, parameter, parameter.getNestedGenericParameterType());
    String name = Conventions.getVariableNameForParameter(parameter);

    return adaptArgumentIfNecessary(arg, parameter);
}  
```

下面我们分析一下`readWithMessageConverters`方法的具体处理

```java
//org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor#readWithMessageConverters
protected <T> Object readWithMessageConverters(HttpInputMessage inputMessage, MethodParameter parameter,
                                               Type targetType) {
    //获取请求内容类型，此时是：application/json;charset=UTF-8
    MediaType contentType = inputMessage.getHeaders().getContentType();
    //获取handler处理类Class
    Class<?> contextClass = parameter.getContainingClass();
    //获取参数的目标类型Class
    Class<T> targetClass = (targetType instanceof Class ? (Class<T>) targetType : null);
    //获取当前请求方式:POST/GET...
    HttpMethod httpMethod = (inputMessage instanceof HttpRequest ? ((HttpRequest) inputMessage).getMethod() : null);

    Object body = NO_VALUE;
    try {
        //遍历获取适配的MessageConverter进行参数解析，获取body对象
        for (HttpMessageConverter<?> converter : this.messageConverters) {
            //通过canRead方法判断当前MessageConverter是否能处理目标参数
            if (converter.canRead(targetClass, contentType)) {
                //通过MessageConverter.read进行参数解析
                body = ((HttpMessageConverter<T>) converter).read(targetClass, msgToUse));
                //进行切面处理
                body = getAdvice().afterBodyRead(body, msgToUse, parameter, targetType, converterType);
                break;
            }
        }
    }
    catch (IOException ex) {
        throw new HttpMessageNotReadableException("I/O error while reading input message", ex, inputMessage);
    }

    //如果没有适配的MessageConverter，将不会进行参数解析，无法获取body对象，则报错
    if (body == NO_VALUE) {
        throw new HttpMediaTypeNotSupportedException(contentType, this.allSupportedMediaTypes);
    }
    
    //响应body，即解析后参数
    return body;
}
```

**步骤3**：分析`MappingJackson2XmlHttpMessageConverter.read`的处理

具体实现在抽象父类中：`AbstractJackson2HttpMessageConverter#read`

```java
//org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter
public Object read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
    throws IOException, HttpMessageNotReadableException {
	
    JavaType javaType = getJavaType(type, contextClass);
    return readJavaType(javaType, inputMessage);
}

private Object readJavaType(JavaType javaType, HttpInputMessage inputMessage) throws IOException {
    return this.objectMapper.readValue(inputMessage.getBody(), javaType);
}

总结：就是通过objectMapper，将请求body中json字符串转换为目标类型对象
```

##### `HandlerMethodReturnValueHandler`接口

```java
//org.springframework.web.method.support.HandlerMethodReturnValueHandler
public interface HandlerMethodReturnValueHandler {
    /* 判断是否支持此响应类型 */
    boolean supportsReturnType(MethodParameter returnType);
	/* 进行返回值处理 */
    void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
                           ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception;
}
```

##### `@RequestBody`的处理

由于`MappingJackson2XmlHttpMessageConverter`实现了`HandlerMethodReturnValueHandler`接口，所以通过`handleReturnValue`方法进行处理

```java
//org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor#handleReturnValue
public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
                              ModelAndViewContainer mavContainer, NativeWebRequest webRequest) {
	//设置不需要视图解析器进行解析，通过requestHandled参数控制
    mavContainer.setRequestHandled(true);
    //创建请求、响应对象
    ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
    ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);

    // 通过MessageConverter.write方法进行响应值处理
    writeWithMessageConverters(returnValue, returnType, inputMessage, outputMessage);
}

writeWithMessageConverters的实质其实是调用 MappingJackson2XmlHttpMessageConverter#write进行响应输出
最终通过objectMapper将响应对象转换为json字符串后，添加到response对象中
```

# SpringMVC 执行过程

1. 用户请求发送到**前端控制器 DispatcherServlet**。
2. 前端控制器 DispatcherServlet 接收到请求后，DispatcherServlet 会使用 HandlerMapping 来处理，**HandlerMapping 会查找到具体进行处理请求的 Handler 对象**。
3. HandlerMapping 找到对应的 Handler 之后，并不是返回一个 Handler 原始对象，而是一个 Handler 执行链（HandlerExecutionChain），在这个执行链中包括了拦截器和处理请求的 Handler。HandlerMapping 返回一个执行链给 DispatcherServlet。
4. DispatcherServlet 接收到执行链之后，会**调用 Handler 适配器去执行 Handler**。
5. Handler 适配器执行完成 Handler（也就是 Controller）之后会得到一个 ModelAndView，并返回给 DispatcherServlet。
6. DispatcherServlet 接收到 HandlerAdapter 返回的 ModelAndView 之后，会根据其中的视图名调用 ViewResolver。
7. **ViewResolver 根据逻辑视图名解析成一个真正的 View 视图**，并返回给 DispatcherServlet。
8. DispatcherServlet 接收到视图之后，会根据上面的 ModelAndView 中的 model 来进行视图中数据的填充，也就是所谓的**视图渲染**。
9. 渲染完成之后，DispatcherServlet 就可以将结果返回给用户了。

# 错误处理

## HttpMediaTypeNotSupportedException

错误信息：

```
org.springframework.web.HttpMediaTypeNotSupportedException: Content type 'application/json;charset=UTF-8' not supported
```

### 原因

没有引入`jackson`对应jar包，加载不到类：`com.fasterxml.jackson.dataformat.xml.XmlMapper`，导致不会添加`MappingJackson2XmlHttpMessageConverter`到`RequestMappingHandlerMapping`中，所以在参数解析时，缺少能解析对应`contentType`为`application/json;charset=UTF-8`的`MessageConverter`，所以报错

位置：`AbstractMessageConverterMethodArgumentResolver#readWithMessageConverters`

![image-20210721111255140](D:\学习整理\summarize\spring\图片\HttpMediaTypeNotSupportException报错位置)

### 解决方案

Spring源码中引用`jackson`依赖是在`spring-web`模块中，此模块中`gradle`配置为`optional`，修改为`compile`

## 反射通过Parameter获取函数参数名称为arg0

原因是编辑级别低于1.8，得到的参数名称是无意义的`arg0、arg1`

遗憾的是，保留参数名这一选项由编译开关`javac -parameters`打开，默认是关闭的。

所以需要使编辑级别高于1.8

### idea设置编辑级别

![image-20210723211655859](D:\学习整理\summarize\spring\图片\idea设置编辑级别)



