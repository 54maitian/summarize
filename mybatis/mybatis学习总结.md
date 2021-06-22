# Mybatis学习总结

## JDBC原生

JDBC是一套java语言访问数据库的规范API，我们可以使用其来访问不同数据库

### 依赖

```xml
<!-- mysql数据库连接,其他数据库有其对应连接 -->
<dependency>
    <groupId>org.wisdom-framework</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>5.1.34_1</version>
</dependency>
```

### 常用类和接口

| 接口/类                    | 描述                                                    |
| -------------------------- | ------------------------------------------------------- |
| java.sql.DriverManager     | 驱动管理类，可以通过getConnection方法获取Connection连接 |
| java.sql.Connection        | 数据库连接接口                                          |
| java.sql.Statement         | 用于接收参数、执行SQL并返回结果的接口                   |
| java.sql.PreparedStatement | Statement实现接口，区别在于可以进行SQL的预编译          |
| java.sql.ResultSet         | SQL执行的结果集                                         |

### JDBC编程步骤

```bash
# 具体实现驱动类及url
	# Mysql
		# com.mysql.jdbc.Driver
		# jdbc:mysql://dbip:port/databasename
	# Oracle
		# oracle.jdbc.driver.OracleDriver
		# jdbc:oracle:thin:@dbip:port:databasename
# 1.加载驱动程序	
	# DriverManager.registerDriver(new com.mysql.jdbc.Driver())
		# 不推荐
			# 导致驱动被注册2次
			# 强烈依赖数据库的驱动jar
	# Class.forName(“com.mysql.jdbc.Driver”);
# 2.获取数据库连接
	# DriverManager.getConnection
# 3.获取SQL执行对象
	# Connection.createStatement
# 4.参数设置
	# Statement
		# 需要提供替换参数后的sql
	# PreparedStatement
		# 使用 ? 表示占位符
		# 使用setXXX(index, paramValue)进行参数设置
			# index从1开始
# 5.SQL执行
	# execute方法
# 6.获取结果集
	# getResultSet方法
# 7.解析结果集
	# while循环
		# 使用next()移动光标，获取当前行数据
		# 使用getXXX()方法获取行数据结果中对应列项
			# 可以使用columnIndex通过角标获取
				# 角标从1开始
			# 可以使用columnName通过列名获取
# 8.关闭连接			
```

### 示例代码

```java
public static void main(String[] args) throws Exception {
    //加载驱动
    Class.forName("com.mysql.jdbc.Driver");
    //获取连接
    Connection connection = DriverManager.getConnection("url", "username", "password");
    //获取SQL执行对象
    //Statement statement = connection.createStatement();
    PreparedStatement statement = connection.prepareStatement("select name, age from person where id = ? and sex = ?");
    //preparedStatement需要参数设置后进行sql执行
    statement.setInt(1, 1);
    statement.setString(2, "男");
    //sql执行
    //statement.execute("select name, age from person where id = 1 and sex = 男");
    //获取结果集
    ResultSet resultSet = statement.getResultSet();
    //循环解析结果集
    while (resultSet.next()) {
        //通过脚标获取
        String name = resultSet.getString(1);
        //通过列名获取
        String age = resultSet.getString("age");
    }

    //关闭连接
    resultSet.close();
    statement.close();
    connection.close();
}
```

## Mybatis

MyBatis 是一款优秀的持久层框架，它支持自定义 SQL、存储过程以及高级映射

### 依赖

```xml
<!-- mybatis依赖 -->
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.5</version>
</dependency>
<!-- mysql数据库连接,其他数据库有其对应连接 -->
<dependency>
    <groupId>org.wisdom-framework</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>5.1.34_1</version>
</dependency>
```

### 分析

```bash
# JDBC原生操作的缺陷
	# 需要手动进行连接的创建和销毁
	# 需要手动进行事务管理
	# 获取结果集后需要手动进行结果集分析
# JDBC操作的重点
	# 获取数据库连接
	# 参数解析后设置参数值
	# 获取对应sql进行执行
	# 进行结果集的分析、封装
```

### Mybatis操作

```java
public static void main(String[] args) throws Exception {
    //加载主配置文件
    InputStream resource = Resources.getResourceAsStream("mybatis-config.xml");
    //解析主配置文件流创建sqlSessionFactory
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(resource);
    //获取sqlSession
    SqlSession sqlSession = sqlSessionFactory.openSession();
    //获取mapper对象
    TransferMapper mapper = sqlSession.getMapper(TransferMapper.class);
    //执行操作
    TransferAmount transferAmount = mapper.selectBySerial("6029621011001");
    System.out.println(transferAmount);
}
```

### 主配置文件

配置基础数据

- 数据库连接信息
- mapper对应位置
- 一些mybatis内部机制控制信息

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">

<configuration>
    <!-- 注册属性文件 -->
    <properties resource="jdbc.properties"/>
    <!--自定义属性-->
    <properties>
        <property name="" value=""/>
    </properties>

    <!--参数配置，会改变mybatis默认的行为，一般使用默认值即可；具体内容不在此处讲述-->
    <settings>
        <setting name="" value=""/>
    </settings>

    <!--数据类型映射处理器，一般使用内置处理器即可-->
    <typeHandlers>
        <package name=""/>
        <typeHandler handler="" javaType="" jdbcType=""/>
    </typeHandlers>


    <!--配置别名，在mapper配置文件中使用别名替换全限定类名-->
    <!--mybatis有一些基本类型的内置别名-->
    <typeAliases>
        <!--包扫描别名配置，别名为类名首字母小写-->
        <package name="com.hcx.beans"/>
        <!--自定义类对应别名-->
        <typeAlias type="mapper.TransferMapper" alias=""/>
    </typeAliases>

    <!-- 配置MyBatis运行环境 -->
    <environments default="development">
        <environment id="development">
            <!--配置事务管理器-->
            <!--JDBC：使用jdbc事务管理机制，程序中需要显式的对事务进行提交或回滚-->
            <!--MANAGED：由容器来管理事务的整个生命周期（如Spring容器）-->
            <transactionManager type="JDBC"></transactionManager>

            <!--配置数据源-->
            <!--UNPOOLED: 不使用连接池；POOLED：使用连接池；JDNI：数据源可以定义到应用的外部，通过JDNI容器获取数据库连接-->
            <dataSource type="POOLED">
                <property name="driver" value="${jdbc.dirver}"/>
                <property name="url" value="${jdbc.url}"/>
                <property name="username" value="${jdbc.user}"/>
                <property name="password" value="${jdbc.password}"/>
            </dataSource>
        </environment>
    </environments>

    <!-- 注册映射文件 -->
    <mappers>
        <!--使用包扫描配置，会注册对应mapper接口，需要mapper接口对应xml配置层级与mapper所在包一致-->
        <package name="包名"/>

        <!--配置单个mapper-->
        <!--resource：指定一个xml文件-->
        <!--class：指定一个mapper接口，需要mapper接口对应xml配置层级与mapper所在包一致-->
        <mapper resource="com/hcx/dao/mapper.xml" class="" url=""/>
    </mappers>
</configuration>
```

### Mapper配置文件

配置SQL信息

- 具体SQL
- SQL参数
- 动态SQL配置
- SQL结果集映射
- 是否二级缓存控制

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<!-- namespace：此mapper命名空间，一般对应mapper接口全限定类名 -->
<!-- namespace + '.' + sqlId：mappedStatement全局唯一的statementId -->
<mapper namespace="">
<!--开启二级缓存；type：可以使用第三方缓存实现来存储二级缓存数据，实现org.apache.ibatis.cache.Cache接口-->
    <cache type=""/>

    <!-- 可重复使用的SQL，使用<include>标签引用；
        id：唯一标志-->
    <sql id="select">
        select * from user
    </sql>

    <resultMap type="Orders" id="orders">
        <!--字段结果映射-->
        <id property="id" column="id"/>
        <result property="userId" column="user_id"/>
        <!--一对一对象映射-->
        <association property=""/>
        <!--一对多对象映射-->
        <collection property=""/>
    </resultMap>
    
    <select id="" resultMap="orders">
        <!--引用SQL -->
        <include refid="select"></include>

        <!-- where标签:一个where条件语句，通常和<if>标签混合使用，可以处理语法中多余的and -->
        <where>
            <!-- if标签:执行一个判断语句，成立才会执行标签体内的sql语句
                test:写上条件判断语句
                注意:这里每一个if前面都尽量加上and，如果你是第一个条件，框架会自动帮你把and截取，如果是第二个if就不能省略and
             -->
            <if test="id != null and id != ''">
                and id in
                <!-- foreach:循环语句，通常多用于参数是集合时，需要对参数进行遍历出来，再进行赋值查询
                                collection:参数类型中的集合、数组的名字，例：下面的ids就是QueryVo这个类中的list集合的名字
                                item:为遍历该集合起一个变量名，遍历出来的每一个字，都赋值到这个item中
                                open:在sql语句前面添加的sql片段
                                close:在sql语句后面添加的sql片段
                                separator:指定遍历元素之前用什么分隔符
                             -->
                <foreach collection="ids" item="id" open="id in(" close=")" separator=",">
                    #{id}
                </foreach>
            </if>
        </where>
    </select>
    
</mapper>
```

#### 属性介绍

##### select/updte/delete/insert

| 属性             | 描述                                                         |
| ---------------- | ------------------------------------------------------------ |
| id               | 命名空间中的唯一标识符，可被用来代表这条语句。               |
| parameterType    | 将要传入语句的参数的完全限定类名或别名。 这个属性是可选的，因为 MyBatis 可以通过 TypeHandler 推断出具体传入语句的参数，默认值为 unset。 |
| parameterMap     | 这是引用外部 parameterMap 的已经被废弃的方法。 使用内联参数映射和 parameterType 属性。 |
| flushCache       | 将其设置为 true，任何时候只要语句被调用， 都会导致本地缓存和二级缓存都会被清空， 默认值：true（对应插入、更新和删除语句）。 |
| timeout          | 这个设置是在抛出异常之前，驱动程序等待数据库返回请求结果的秒数。 默认值为 unset（依赖驱动）。 |
| statementType    | STATEMENT，PREPARED 或 CALLABLE 的一个。 这会让 MyBatis 分别使用 Statement，PreparedStatement 或 CallableStatement，默认值：PREPARED。 |
| useGeneratedKeys | （仅对 insert 和 update 有用）这会令 MyBatis 使用 JDBC 的 getGeneratedKeys 方法来取出由数据库内部生成的主键（比如：像 MySQL 和 SQL Server 这样的关系数据库管理系统的自动递增字段），默认值：false。 |
| keyProperty      | （仅对 insert 和 update 有用）唯一标记一个属性， MyBatis 会通过 getGeneratedKeys 的返回值 或者通过 insert 语句的 selectKey 子元素设置它的键值， 默认：unset。如果希望得到多个生成的列，也可以是逗号分隔的属性名称列表。 |
| keyColumn        | （仅对 insert 和 update 有用）通过生成的键值设置表中的列名， 这个设置仅在某些数据库（像 PostgreSQL）是必须的， 当主键列不是表中的第一列的时候需要设置。 如果希望得到多个生成的列，也可以是逗号分隔的属性名称列表。 |
| databaseId       | 如果配置了 databaseIdProvider，MyBatis 会加载所有的不带 databaseId 或匹配当前 databaseId 的语句； 如果带或者不带的语句都有，则不带的会被忽略。 |

##### resultMap

| 属性        | 描述                                                         |
| ----------- | ------------------------------------------------------------ |
| id          | 当前命名空间中的一个唯一标识，用于标识一个result map.        |
| type        | 类的完全限定名, 或者一个类型别名 (内置的别名可以参考上面的表格). |
| autoMapping | 如果设置这个属性，MyBatis将会为这个ResultMap开启或者关闭自动映射。 这个属性会覆盖全局的属性 autoMappingBehavior。默认值为：unset。 |

###### id/result

| 属性        | 描述                                                         |
| ----------- | ------------------------------------------------------------ |
| property    | 映射到列结果的字段或属性。如果用来匹配的 JavaBeans 存在给定名字的属性，那么它将会被使用。 否则 MyBatis 将会寻找给定名称 property 的字段。 无论是哪一种情形，你都可以使用通常的点式分隔形式进行复杂属性导航。 比如,你可以这样映射一些简单的东西: “username” , 或者映射到一些复杂的东西: “address.street.number” 。 |
| column      | 数据库中的列名,或者是列的别名。 一般情况下，这和 传递给 resultSet.getString(columnName) 方法的参数一样。 |
| javaType    | 一个 Java 类的完全限定名,或一个类型别名(参考上面内建类型别名 的列表) 。 如果你映射到一个 JavaBean,MyBatis 通常可以断定类型。 然而,如果你映射到的是 HashMap,那么你应该明确地指定 javaType 来保证期望的行为。 |
| jdbcType    | JDBC 类型，所支持的 JDBC 类型参见这个表格之后的“支持的 JDBC 类型”。 只需要在可能执行插入、更新和删除的允许空值的列上指定 JDBC 类型。 这是 JDBC 的要求而非 MyBatis 的要求。 如果你直接面向 JDBC 编程,你需要对可能为 null 的值指定这个类型。 |
| typeHandler | 我们在前面讨论过的默认类型处理器。 使用这个属性,你可以覆盖默 认的类型处理器。 这个属性值是一个类型处理 器实现类的完全限定名，或者是类型别名。 |

###### constructor

| 属性        | 描述                                                         |
| ----------- | ------------------------------------------------------------ |
| column      | 数据库中的列名,或者是列的别名。 一般情况下，这和 传递给 resultSet.getString(columnName) 方法的参数一样。 |
| javaType    | 一个 Java 类的完全限定名,或一个类型别名(参考上面内建类型别名的列表)。 如果你映射到一个 JavaBean,MyBatis 通常可以断定类型。 然而,如 果你映射到的是 HashMap,那么你应该明确地指定 javaType 来保证期望的 行为。 |
| jdbcType    | JDBC 类型，所支持的 JDBC 类型参见这个表格之前的“支持的 JDBC 类型”。 只需要在可能执行插入、更新和删除的允许空值的列上指定 JDBC 类型。 这是 JDBC 的要求而非 MyBatis 的要求。 如果你直接面向 JDBC 编程,你需要对可能为 null 的值指定这个类型。 |
| typeHandler | 我们在前面讨论过的默认类型处理器。 使用这个属性,你可以覆盖默 认的类型处理器。 这个属性值是一个类型处理 器实现类的完全限定名，或者是类型别名。 |
| select      | 用于加载复杂类型属性的映射语句的 ID,它会从 column 属性中指定的列检索数据， 作为参数传递给此 select 语句。具体请参考 Association 标签。 |
| resultMap   | ResultMap 的 ID，可以将嵌套的结果集映射到一个合适的对象树中， 功能和 select 属性相似，它可以实现将多表连接操作的结果映射成一个单一的ResultSet。 这样的ResultSet将会将包含重复或部分数据重复的结果集正确的映射到嵌套的对象树中。 为了实现它, MyBatis允许你 “串联” ResultMap,以便解决嵌套结果集的问题。 想了解更多内容，请参考下面的Association元素。 |
| name        | 构造方法形参的名字。从3.4.3版本开始，通过指定具体的名字， 你可以以任意顺序写入arg元素。参看上面的解释。 |

###### association/collection

- 嵌套结果:使用嵌套结果映射来处理重复的联合结果的子集

  | 属性        | 描述                                                         |
  | ----------- | ------------------------------------------------------------ |
  | property    | 映射到列结果的字段或属性。 如果用来匹配的 JavaBeans 存在给定名字的属性，那么它将会被使用。 否则 MyBatis 将会寻找与给定名称相同的字段。 这两种情形你可以使用通常点式的复杂属性导航。 比如,你可以这样映射 一 些 东 西 :“ username ”, 或 者 映 射 到 一 些 复 杂 的 东 西 : “address.street.number” 。 |
  | javaType    | 一个 Java 类的完全限定名,或一个类型别名(参考上面内建类型别名的列 表) 。 如果你映射到一个 JavaBean,MyBatis 通常可以断定类型。 然而,如 javaType 果你映射到的是 HashMap,那么你应该明确地指定 javaType 来保证所需的 行为。 |
  | jdbcType    | 在这个表格之前的所支持的 JDBC 类型列表中的类型。 JDBC 类型是仅仅 需要对插入, 更新和删除操作可能为空的列进行处理。 这是 JDBC 的需要, jdbcType 而不是 MyBatis 的。 如果你直接使用 JDBC 编程,你需要指定这个类型-但 仅仅对可能为空的值。 |
  | typeHandler | 我们在前面讨论过默认的类型处理器。 使用这个属性,你可以覆盖默认的 typeHandler 类型处理器。 这个属性值是类的完全限定名或者是一个类型处理器的实现, 或者是类型别名。 |

- 嵌套查询:通过执行另外一个 SQL 映射语句来返回预期的复杂类型

  | 属性      | 描述                                                         |
  | --------- | ------------------------------------------------------------ |
  | column    | 来自数据库的列名,或重命名的列标签。 这和通常传递给 resultSet.getString(columnName)方法的字符串是相同的。 column 注 意 : 要 处 理 复 合 主 键 , 你 可 以 指 定 多 个 列 名 通 过 column= ” {prop1=col1,prop2=col2} ” 这种语法来传递给嵌套查询语 句。 这会引起 prop1 和 prop2 以参数对象形式来设置给目标嵌套查询语句。 |
  | select    | 另外一个映射语句的 ID,可以加载这个属性映射需要的复杂类型。 获取的 在列属性中指定的列的值将被传递给目标 select 语句作为参数。 表格后面 有一个详细的示例。 select 注 意 : 要 处 理 复 合 主 键 , 你 可 以 指 定 多 个 列 名 通 过 column= ” {prop1=col1,prop2=col2} ” 这种语法来传递给嵌套查询语 句。 这会引起 prop1 和 prop2 以参数对象形式来设置给目标嵌套查询语句。 |
  | fetchType | 可选的。有效值为 lazy和eager。 如果使用了，它将取代全局配置参数lazyLoadingEnabled。 |

### Mapper接口

```java

```

## Mybatis源码分析

### 配置解析

#### 涉及类库

##### Resources

- 通过类加载其加载主配置文件为输入流

```java
//org.apache.ibatis.io.Resources
public class Resources {
    //返回类路径上的资源为stream流
    public static InputStream getResourceAsStream(String resource) throws IOException {}
}
```

##### SqlSessionFactoryBuilder

- 接收主配置文件流，创建XMLConfigBuilder并调用其parse方法解析主配置文件
- 借助XMLConfigBuilder解析后的Configuration，创建SqlSessionFactory
  - 实现：org.apache.ibatis.session.defaults.DefaultSqlSessionFactory
- 全局唯一

```java
//org.apache.ibatis.session.SqlSessionFactoryBuilder
public class SqlSessionFactoryBuilder {
    public SqlSessionFactory build(InputStream inputStream, String environment, Properties properties) {
        //创建XMLConfigBuilder，调用parse解析inputStream内容，返回Configuration对象
        XMLConfigBuilder parser = new XMLConfigBuilder(inputStream, environment, properties);
        return build(parser.parse());
    }

    /* 创建SqlSessionFactory，类型为DefaultSqlSessionFactory */
    public SqlSessionFactory build(Configuration config) {
        return new DefaultSqlSessionFactory(config);
    }
}
```

##### XMLConfigBuilder

- 构造创建Configuration对象
- 解析主配置文件信息，将其中内容提取、保存到Configuration中
- 根据mappers配置不同，通过不同的方式调用解析对应mapper资源
- 全局唯一

```java
//org.apache.ibatis.builder.xml.XMLConfigBuilder
public class XMLConfigBuilder extends BaseBuilder {
    //configuration对象，构造时创建
    protected final Configuration configuration = new Configuration();

    /* 解析主配置文件信息 */
    private void parseConfiguration(XNode root) {
        //解析mappers元素
        mapperElement(root.evalNode("mappers"));
    }
    /* 解析mappers元素对应资源入口 */
    private void mapperElement(XNode parent) throws Exception {
        //方式一：通过XMLMapperBuilder解析对应mapper.xml配置文件
        XMLMapperBuilder mapperParser = new XMLMapperBuilder(...);
        mapperParser.parse();
        //方式二：通过configuration对象，解析对应mapper接口
        configuration.addMapper(mapperInterface);
    }
}
```

##### Configuration

- 主配置对象，对应主配置文件配置信息
- 全局唯一，存储一些全局控制的对象/容器
- 通过Configuration进行一些特殊行为操作

```java
//org.apache.ibatis.session.Configuration
public class Configuration {
    //二级缓存全局控制参数
    protected boolean cacheEnabled = true;
    //mapper注册对象
    protected final MapperRegistry mapperRegistry = new MapperRegistry(this);
    //二级缓存容器，key为namespace
    protected final Map<String, Cache> caches = new StrictMap<>("Caches collection");
    //已加载的资源(mapper类、mapper.xml)
    protected final Set<String> loadedResources = new HashSet<>();
    //MappedStatement存储容器，key为statementId = namespace + '.' + sqlId
    protected final Map<String, MappedStatement> mappedStatements = new StrictMap<MappedStatement>();

    /* 判断一个资源是否已加载，避免重复加载 */
    public boolean isResourceLoaded(String resource) {
        return loadedResources.contains(resource);
    }
    /* 创建Executor对象 */
    public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
        //常用类型
        executor = new SimpleExecutor(this, transaction);
        //根据二级缓存参数，将Executor包装为CachingExecutor
        if (cacheEnabled) {
            executor = new CachingExecutor(executor);
        }
        //如果有插件，则使用插件进行包装增强
        executor = (Executor) interceptorChain.pluginAll(executor);
    }
    /* 借助mapperRegistry注册mapper */
    public <T> void addMapper(Class<T> type) {
        mapperRegistry.addMapper(type);
    }
	
    /* 传入sqlSession，借助mapperRegistry根据Class获取mapper接口的代理对象 */
    public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
        return mapperRegistry.getMapper(type, sqlSession);
    }
}
```

##### XMLMapperBuilder

- 对应每个mapper.xml配置文件资源

- 分析XPathParser解析mapper配置文件的XNode对象
  - 解析操作语句(select|insert|update|delete)，创建对应MappedStatement保存到Configuration中
  - 解析cache元素，借助builderAssistant创建二级缓存容器

```java
//org.apache.ibatis.builder.xml.XMLMapperBuilder
public class XMLMapperBuilder extends BaseBuilder {
    protected final Configuration configuration;
    //一个解析助手
    private final MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(configuration, resource);

    /* 解析mapper文件入口 */
    public void parse() {
        //解析mapper文件
        configurationElement(parser.evalNode("/mapper"));
        //注册对应mapper接口
        bindMapperForNamespace();
    }

    /* 解析mapper.xml配置文件 */
    private void configurationElement(XNode context) {
        //获取namespace
        String namespace = context.getStringAttribute("namespace");
        //设置namespace
        builderAssistant.setCurrentNamespace(namespace);
        //解析cache标签，进行二级缓存处理
        cacheElement(context.evalNode("cache"));
        //解析每个操作语句
        buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
    }
	
    /* 解析操作语句节点 */
    private void buildStatementFromContext(List<XNode> list, String requiredDatabaseId) {
        for (XNode context : list) {
            //创建XMLStatementBuilder，解析XNode节点创建对应的MappedStatement保存到Configuration中
            final XMLStatementBuilder statementParser = new XMLStatementBuilder(...);
            statementParser.parseStatementNode();
        }
    }
}
```

##### MapperBuilderAssistant

- 对应于每个mapper资源解析对象
  - XMLMapperBuilder
  - MapperAnnotationBuilder

- mapper配置解析助手
  - MappedStatement的创建和保存
  - 二级缓存的容器创建
    - 对应于每个namespace
    - 通过currentCache临时对象，将容器保存到Configuration和MappedStatement中

```java
public class MapperBuilderAssistant extends BaseBuilder {
    protected final Configuration configuration;
    //命名空间，对应于xml中namespace/mapper接口全限定类名
    private String currentNamespace;
    //二级缓存对象临时引用，用于添加到Configuration和MappedStatement中
    private Cache currentCache;
    
    //二级缓存容器创建
    public Cache useNewCache(...){
        //创建二级缓存容器Cache
        Cache cache = new CacheBuilder(currentNamespace).build();
        //保存到configuration中
        configuration.addCache(cache);
        //保存引用，用于后续将二级缓存容器保存到MappedStatement中
        currentCache = cache;
    }
    
    //创建MappedStatement
    public MappedStatement addMappedStatement(...){
        //创建MappedStatement，并将二级缓存currentCache注入
        MappedStatement.Builder statementBuilder = new MappedStatement().cache(currentCache).Builder.build();
        //将MappedStatement添加到configuration中
        configuration.addMappedStatement(statement);
    }
}
```

##### XMLStatementBuilder

- 对应于每个操作语句(select|insert|update|delete)
- 解析每个操作语句元素内容
  - 将信息传递给MapperBuilderAssistant创建MappedStatement并保存

```java
//org.apache.ibatis.builder.xml.XMLStatementBuilder
public class XMLStatementBuilder extends BaseBuilder {
    //mapper解析助手
    private final MapperBuilderAssistant builderAssistant;
    //对应select|insert|update|delete节点
    private final XNode context;
    
    /* XNode元素解析 */
    public void parseStatementNode() {
        //分析XNode元素，将解析结果传递给builderAssistant创建MappedStatement
        builderAssistant.addMappedStatement(...)
    }
}
```

##### MappedStatement

- 对应于每个操作语句(select|insert|update|delete)
- 操作语句解析的最终结果对象

```java
//org.apache.ibatis.mapping.MappedStatement
public final class MappedStatement {
    //唯一标志：namespace + '.' + sqlId
    private String id;
    //二级缓存容器
    private Cache cache;
    //保存sql信息
    private SqlSource sqlSource;
    //是否开启二级缓存全局配置
    protected boolean cacheEnabled = true;
}
```

##### MapperRegistry

- 对应于每个mapperClass资源

- mapper接口注册、解析入口

```java
//org.apache.ibatis.binding.MapperRegistry
public class MapperRegistry {
    //配置主体
    private final Configuration config;
    //mapper注册容器map，key为mapperClass，value为mapperClass对应的MapperProxyFactory
    private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<>();
    
    //注册mapperClass接口
    public <T> void addMapper(Class<T> type) {
        //注册到容器中
        knownMappers.put(type, new MapperProxyFactory<>(type));
        //借助MapperAnnotationBuilder对象解析对应mapper接口
        MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
        parser.parse();
    }
    
    /* 接收sqlSession，获取mapper接口对应代理对象 */
    public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
        //获取注册的mapper代理工厂
        final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
        //返回代理工厂创建的代理对象
        return mapperProxyFactory.newInstance(sqlSession);
    }
}
```

##### MapperAnnotationBuilder

- 对应于每个mapperClass资源
- 解析具体mapper接口，分析接口中的注释
  - 解析接口中对应mybatis注释，进行二级缓存的容器创建
  - 解析其中方法上注释，创建对应MappedStatement并保存

```java
//org.apache.ibatis.builder.annotation.MapperAnnotationBuilder
public class MapperAnnotationBuilder {
    //需要分析的注释类型集合
    //Select、Update、Insert、Delete、SelectProvider、UpdateProvider、InsertProvider、DeleteProvider
    private static final Set<Class<? extends Annotation>> statementAnnotationTypes;
    //
    private final Configuration configuration;
    private final MapperBuilderAssistant assistant;
    //对应接口类型
    private final Class<?> type;
	
    /* 解析方法 */
    public void parse() {
        //尝试加载对应xml资源
        loadXmlResource();
        //根据注释处理二级缓存容器
        parseCache();
        //解析具体方法
        for (Method method : type.getMethods()) {
            parseStatement(method);
        }
    }
}
```

#### 解析分析

参照JDBC原生操作，我们可以知道，进行数据库操作，主要是两点

- 获取数据库连接
  - 数据库连接信息
- 获取sql信息
  - sql主体
  - sql参数
  - sql结果解析

对应到我们上述mybatis配置

- 主配置文件
  - 数据库连接信息
  - 一些作为操作框架的高级行为控制信息
  - 体现为Configuration
- mapper资源
  - 体现为MappedStatement
- mapper.xml配置文件
  - sql信息
  - 二级缓存控制元素Cache
- mapper接口
  - 提供mapper代理对象的实现类
  - 通过注解替代mapper.xml配置文件

#### 过程解析

##### 入口

```bash
# 1. Resources加载主配置文件为文件流
	# InputStream - org.apache.ibatis.io.Resources#getResourceAsStream
	
# 2. 新建SqlSessionFactoryBuilder，调用其build方法获取sqlSessionFactory
	# SqlSessionFactory - org.apache.ibatis.session.SqlSessionFactoryBuilder#build
		# 创建XMLConfigBuilder，借用其解析文件流，获得Configuration
		# 创建DefaultSqlSessionFactory返回	
```

##### 主配置解析

```bash
# 解析主配置文件，返回对应Configuration对象
# XMLConfigBuilder
	# 构造创建Configuration
	# XPathParser解析InputStream为XNode
	# 解析主配置文件configuration标签内容
		# org.apache.ibatis.builder.xml.XMLConfigBuilder#parseConfiguration
			# 解析配置对应mapper资源入口
				# org.apache.ibatis.builder.xml.XMLConfigBuilder#mapperElement
```

##### Mapper资源解析

解析mapper资源，用于创建MappedStatement对象

mapper资源解析根据mappers配置资源不同，有两种解析方式

###### mapper.xml

```bash
# 加载mapper.xml资源
	# 对应配置
		# mapper标签
			# resource/url

# 主类：XMLMapperBuilder
	# org.apache.ibatis.builder.xml.XMLMapperBuilder#parse
		# 1.判断此资源是否已经加载，已经加载过则跳过
			# org.apache.ibatis.session.Configuration#isResourceLoaded
		# 2.借助XPathParser解析文件流
		# 3.解析mapper元素
			# org.apache.ibatis.builder.xml.XMLMapperBuilder#configurationElement
		# 4.尝试加载对应mapperClass资源
			# org.apache.ibatis.builder.xml.XMLMapperBuilder#bindMapperForNamespace

# mapper文件对应重点元素
	# cache
		# 控制二级缓存容器创建
		# 解析入口
			# org.apache.ibatis.builder.xml.XMLMapperBuilder#cacheElement
	# select|insert|update|delete
		# 对应MappedStatement
		# 解析入口
			# org.apache.ibatis.builder.xml.XMLMapperBuilder#buildStatementFromContext
				# 借助XMLStatementBuilder解析对应节点

# XMLStatementBuilder解析节点
	# org.apache.ibatis.builder.xml.XMLStatementBuilder#parseStatementNode
	# 解析节点对应信息
	# 借助MapperBuilderAssistant创建MappedStatement，并保存到Configuration中
```

###### mapperClass

```bash
# 加载mapperClass资源
	# 对应配置
		# mapper标签
			# class
		# package标签

# 借助Configuration添加mapper
	# org.apache.ibatis.session.Configuration#addMapper
	
# 实质是调用MapperRegistry添加mapper
	# org.apache.ibatis.binding.MapperRegistry#addMapper
	# 1.创建mapperClass对应MapperProxyFactory，并注册到MapperRegistry中
		# knownMappers.put(type, new MapperProxyFactory<>(type));
	# 2.创建MapperAnnotationBuilder解析mapper接口
	
# MapperAnnotationBuilder解析mapper接口
	# org.apache.ibatis.builder.annotation.MapperAnnotationBuilder#parse
	# 1.判断此资源是否已经加载，已经加载过则跳过
		# org.apache.ibatis.session.Configuration#isResourceLoaded
	# 2.尝试加载mapper接口对应的mapper.xml资源
		# org.apache.ibatis.builder.annotation.MapperAnnotationBuilder#loadXmlResource
	# 3.查找@CacheNamespace注解，进行二级缓存容器创建
		# org.apache.ibatis.builder.annotation.MapperAnnotationBuilder#parseCache
	# 4.解析mapper接口的method，如果有配置对应注解，则解析注解并创建MappedStatement
		# org.apache.ibatis.builder.annotation.MapperAnnotationBuilder#parseStatement
```

##### 总结

```bash
# 解析配置主要是为了获得两个对象
	# Configuration
		# 数据库连接信息
		# Mybatis行为控制信息
	# MappedStatement
		# SQL信息
		# 参数信息
		# 结果集信息
# 不论mapper资源是Class/xml，我们发现其解析时都会尝试加载其对应资源
	# Configuration的loadedResources容器存储已经解析的资源名称可以避免循环解析
	# 资源互相进行加载时，需要保证两点
		# mapper接口文件与resource的xml文件保持相同层级
		# mapper接口文件与resource的xml文件保持相同名称
```

### 数据库操作

上述配置加载解析，其目的是为后续进行数据库操作提供必要信息

- 数据库连接信息
- SQL信息(操作数据库的详细信息)

#### 设计类库

##### SqlSessionFactory

- SqlSession工厂，是提供创建SqlSession方法的顶级接口
- org.apache.ibatis.session.SqlSessionFactory

##### DefaultSqlSessionFactory

- SqlSessionFactory接口的实现类
- 提供获取SqlSession的具体实现

```java
//org.apache.ibatis.session.defaults.DefaultSqlSessionFactory
public class DefaultSqlSessionFactory implements SqlSessionFactory {
    /* 创建sqlSession */
    public SqlSession openSession() {
        return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, false);
    }

    /* 创建sqlSession具体工作 */
    private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit) {
        //创建executor对象
        final Executor executor = configuration.newExecutor(tx, execType);
        //返回DefaultSqlSession对象
        return new DefaultSqlSession(configuration, executor, autoCommit);
    }
}
```

##### SqlSession

- Mybatis操作的主要接口
- org.apache.ibatis.session.SqlSession
- 提供mybatis操作功能
  - 执行statementId对应sql
  - 获取mapper代理对象
  - 事务管理

##### DefaultSqlSession

- Mybatis原生操作时，SqlSession接口具体实现

```java
public class DefaultSqlSession implements SqlSession {
    private final Configuration configuration;
    //执行器
    private final Executor executor;

    /* 操作方法示例 */
    public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
        //根据statementId从Configuration中获取对应MappedStatement
        MappedStatement ms = configuration.getMappedStatement(statement);
        //传入MappedStatement，调用executor对应方法
        return executor.query(ms, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER);
    }
	
    /* 借助configuration获取mapper代理对象 */
    public <T> T getMapper(Class<T> type) {
        return configuration.getMapper(type, this);
    }
}
```

##### Executor

- JDBC操作执行器顶级接口
- org.apache.ibatis.executor.Executor
- 提供操作方法
  - update
    - 对应insert|update|delete操作
  - query
    - select操作

##### BaseExecutor

- Executor接口抽象实现类
- JDBC操作入口
- 提供前置功能
  - 一级缓存控制
  - 事务控制

```java
public abstract class BaseExecutor implements Executor {
    //事务对象
    protected Transaction transaction;
    //一级缓存容器
    protected PerpetualCache localCache;

    /* 查询方法 */
    public <E> List<E> query(...) throws SQLException {
        List<E> list;
        try {
            //尝试从一级缓存中获取查询结果
            list = resultHandler == null ? (List<E>) localCache.getObject(key) : null;
            if (list != null) {
                handleLocallyCachedOutputParameters(ms, key, parameter, boundSql);
            } else {
                //获取不到，执行查询
                list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);
            }
            return list;
        }
    }

    /* 一级缓存不命中，执行数据库查询*/
    private <E> List<E> queryFromDatabase(...) throws SQLException {
        //执行查询
        List<E> list; = doQuery(ms, parameter, rowBounds, resultHandler, boundSql);
        //缓存结果到一级缓存
        localCache.putObject(key, list);
        return list;
    }
}
```

##### SimpleExecutor

- Executor接口实现类
- 进行数据库操作的对象

```java
//org.apache.ibatis.executor.SimpleExecutor
public class SimpleExecutor extends BaseExecutor {
	
    /* 操作方法示例 */
    public <E> List<E> doQuery(...) throws SQLException {
        Statement stmt = null;
        try {
            //获取Configuration对象
            Configuration configuration = ms.getConfiguration();
            //创建StatementHandler
            StatementHandler handler = configuration.newStatementHandler(...);
            //获取Statement对象(包括获取连接、参数处理设置)
            stmt = prepareStatement(handler, ms.getStatementLog());
            //执行JDBC操作(包括JDBC执行、结果集处理)
            return handler.query(stmt, resultHandler);
        } finally {
            //关闭连接
            closeStatement(stmt);
        }
    }
    
    prepareStatement
}
```

##### StatementHandler

- 真正执行JDBC操作的执行者的顶级接口
- 提供功能方法
  - 获取Statement
  - 借助ParameterHandler进行参数预处理
  - 借助ResultSetHandler进行结果集解析封装

##### BaseStatementHandler

- StatementHandler抽象实现类

```java
//org.apache.ibatis.executor.statement.BaseStatementHandler
public abstract class BaseStatementHandler implements StatementHandler {
    protected final Configuration configuration;
    //结果集处理器
    protected final ResultSetHandler resultSetHandler;
    //参数处理器
    protected final ParameterHandler parameterHandler;
    //SQL信息
    protected BoundSql boundSql;

    /* 借助parameterHandler进行参数设置 */
    public void parameterize(Statement statement) throws SQLException {
        parameterHandler.setParameters((PreparedStatement) statement);
    }

    /* 由Connection创建Statement对象 */
    public Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException {
        statement = instantiateStatement(connection);
    }
	
    /* 执行JDBC操作，再借助resultSetHandler进行结果集映射 */
    public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
        String sql = boundSql.getSql();
        statement.execute(sql);
        return resultSetHandler.handleResultSets(statement);
    }
}
```

#### 过程解析

```bash
# 获取SqlSession
	# org.apache.ibatis.session.SqlSessionFactory#openSession
	# org.apache.ibatis.session.defaults.DefaultSqlSessionFactory#openSession()
	# 1. 获取Executor对象
		# org.apache.ibatis.session.Configuration#newExecutor
	# 2. 创建SqlSession返回
		# org.apache.ibatis.session.defaults.DefaultSqlSession
		
# 执行数据库操作(以查询为例)
	# 1. sqlSession根据statementId调用方法
		# org.apache.ibatis.session.defaults.DefaultSqlSession#selectList
	# 2. 根据statementId从Configuration中获取对应MappedStatement
		# org.apache.ibatis.session.Configuration#getMappedStatement
	# 3. 借助sqlSession中executor执行查询
		# org.apache.ibatis.executor.Executor#query
		# org.apache.ibatis.executor.BaseExecutor#query
	# 4. 根据入参信息创建CacheKey作为缓存标志
		# org.apache.ibatis.executor.BaseExecutor#createCacheKey
	# 5. 根据CacheKey从一级缓存中获取结果，如果没有，则执行JDBC查询
		# org.apache.ibatis.executor.SimpleExecutor#doQuery
	# 6. 创建StatementHandler对象
		# org.apache.ibatis.session.Configuration#newStatementHandler
	# 7. 使用StatementHandler进行前置处理
		# org.apache.ibatis.executor.SimpleExecutor#prepareStatement
            # 7.1 获取数据库连接
            # org.apache.ibatis.executor.BaseExecutor#getConnection
            # 7.2 由连接创建预处理对象Statement
            # org.apache.ibatis.executor.statement.StatementHandler#prepare
            # 7.3 使用parameterHandler进行入参处理
            # org.apache.ibatis.executor.statement.StatementHandler#parameterize
	# 8. 执行查询
		# org.apache.ibatis.executor.statement.CallableStatementHandler#query
			# 8.1 执行查询
				# java.sql.PreparedStatement#execute
			# 8.2 获取结果集
				# org.apache.ibatis.executor.resultset.ResultSetHandler#handleResultSets
			# 8.3 借助resultSetHandler进行结果集处理
				# org.apache.ibatis.executor.resultset.ResultSetHandler#handleOutputParameters	
                
# 注
	# 此处对于parameterHandler、resultSetHandler的处理过程没有具体分析
```

## Mybatis重点

### Mapper代理

使用Mybatis进行数据库操作，原生使用是根据具体Statementid进行对应方法调用，这种使用有一下缺点：

- 需要给定对应StatementId字符串，不方便
- 需要知道方法的具体操作类型(select|insert|update|delete)，来调用其对应操作方法
- 需要进行结果对象的强制类型转换

```java
//获取sqlSession
SqlSession sqlSession = sqlSessionFactory.openSession();
//查询列表时调用selectList
List<Object> objects = sqlSession.selectList("mapper.TransferMapper.selectList");
//查询单个元素时调用selectOne
TransferAmount transferAmount = (TransferAmount)sqlSession.selectOne("mapper.TransferMapper.selectOne");
//执行insert|update|delete操作时调用update
sqlSession.update("mapper.TransferMapper.updateAmount");
```

此时Mybatis提供了一种代理来替代上述比较繁琐的操作，这就是Mapper代理

- 通过获取mapperClass的代理对象，进行方法调用
- 使用mapperClass接口方法映射xml配置的MappedStatement，代理对象调用方法时根据映射信息，获取StatementId调用SqlSession的不同方法

#### 涉及类库

##### MapperRegistry

```bash
# 前面讲述配置解析有对应描述
# mapper接口注册、解析入口
```

##### MapperProxyFactory

- mapper代理工厂，创建代理对象实例

- 对应于每个mapperClass资源

```java
//org.apache.ibatis.binding.MapperProxyFactory
public class MapperRegistry {
    //mapperClass
    private final Class<T> mapperInterface;
    
    /* 接收sqlSession，创建MapperProxy */
    public T newInstance(SqlSession sqlSession) {
        final MapperProxy<T> mapperProxy = new MapperProxy<>(sqlSession, mapperInterface, methodCache);
        return newInstance(mapperProxy);
    }

    /* 使用动态代理 创建 代理对象实例 */
    protected T newInstance(MapperProxy<T> mapperProxy) {
        return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
    }
}
```

##### MapperProxy

- InvocationHandler实现类
- 通过invoke方法创建MapperMethodInvoker实现方法调用

```java
//org.apache.ibatis.binding.MapperProxy
public class MapperProxy<T> implements InvocationHandler, Serializable {
    //获取sqlSession
    private final SqlSession sqlSession;
    private final Class<T> mapperInterface;
    //方法对应MapperMethodInvoker的缓存，可以避免相同方法调用时重复创建
    private final Map<Method, MapperMethodInvoker> methodCache;
    
    /* 继承方法进行代理对象方法调用 */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Object.class.equals(method.getDeclaringClass())) {
            return method.invoke(this, args);
        } else {
            return cachedInvoker(method).invoke(proxy, method, args, sqlSession);
        }
    }
    
    /* 创建方法对应MapperMethodInvoker，并缓存 */
    private MapperMethodInvoker cachedInvoker(Method method) throws Throwable {
        if (m.isDefault()) {
            return new DefaultMethodInvoker(getMethodHandleJava8(method));
        }else {
            return new PlainMethodInvoker(new MapperMethod(mapperInterface, method, sqlSession.getConfiguration()));
        }
    }
	
    /* MapperProxy内部接口 */
    interface MapperMethodInvoker {
        Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable;
    }
	
    /* MapperProxy静态内部类，实现了MapperMethodInvoker接口 */
    private static class PlainMethodInvoker implements MapperMethodInvoker {
        //当前MapperMethod
        private final MapperMethod mapperMethod;
		
        /* 借助MapperMethod代理实现方法调用 */
        public Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable {
            return mapperMethod.execute(sqlSession, args);
        }
    }
}
```

##### MapperMethod

- 方法代理，根据方法对应MappedStatement的类型调用SqlSession不同的方法

```java
//org.apache.ibatis.binding.MapperMethod
public class MapperMethod {
    //当前代理方法信息
    private final MethodSignature method;
    //根据当前代理方法从Configuration中获取的MappedStatement信息
    private final SqlCommand command;

    /* 根据sql类型判断进行不同方法调用 */
    public Object execute(SqlSession sqlSession, Object[] args) {
        switch (command.getType()) {
            case INSERT: {
                sqlSession.insert(command.getName(), param);
                break;
            },
            case UPDATE: {
                sqlSession.update(command.getName(), param);
                break;
            },
            case DELETE: {
                sqlSession.delete(command.getName(), param);
                break;
            },
            case SELECT: {
                sqlSession.selectOne(command.getName(), param);
                break;
            }
        }
    }
}
```

#### 过程解析

```bash
# 代理注册
	# 解析配置时，如果解析了mapperClass资源，则将其注册到Configuration中的MapperRegistry中
		# org.apache.ibatis.binding.MapperRegistry#addMapper
	# 实质是创建mapperClass对应MapperProxyFactory进行缓存
	
# 代理对象获取
	# 1. 获取mapperClass对应MapperProxyFactory
		# org.apache.ibatis.session.defaults.DefaultSqlSession#getMapper
			# org.apache.ibatis.session.Configuration#getMapper
				# org.apache.ibatis.binding.MapperRegistry#getMapper
	# 2. MapperProxyFactory创建代理对象
		# org.apache.ibatis.binding.MapperProxyFactory#newInstance
		
# 代理对象方法调用，实质是java动态代理的使用
	# 1. 由于是使用InvocationHandler的实现类MapperProxy创建代理对象，所以执行代理方法会调用到MapperProxy中
		# org.apache.ibatis.binding.MapperProxy#invoke
	# 2. MapperProxy的invoke方法中创建对应MapperMethodInvoker进行方法调用
		# 创建对应MapperMethodInvoker
			# org.apache.ibatis.binding.MapperProxy#cachedInvoker
		# 方法调用
			# org.apache.ibatis.binding.MapperProxy.PlainMethodInvoker#invoke
	# 3. MapperMethodInvoker借助MapperMethod进行方法调用
    	# org.apache.ibatis.binding.MapperMethod#execute
    	
# MapperMethod
	# 创建时接收Configuration、mapperInterface、method
		# 1. 获取对应的StatementId
		# 2. 获取对应MappedStatement的处理类型
	# 执行代理方法
		# 1. 根据方法类型判断
		# 2. 通过前置处理方法入参为单一对象
			# org.apache.ibatis.binding.MapperMethod.MethodSignature#convertArgsToSqlCommandParam
		# 3. 调用SqlSession具体的方法
		
# 实质
	# 通过动态代理创建代理对象
	# 代理对象调用方法时
		# 通过mybatis规则中mapperClass与mapper.xml资源映射关系，获取当前代理方法对应的SQL信息(MappedStatement中)
		# 根据方法类型判断，使用传入的sqlSession进行具体方法调用
```

### 一级缓存

```bash
# 缓存用于提高查询效率
# 一级缓存默认开启
```

#### 涉及类库

##### BaseExecutor

- Executor抽象父类

- 一级缓存管理对象，持有一级缓存容器对象
- 可以根据mappedStatement及参数信息创建CacheKey

```java
//org.apache.ibatis.executor.BaseExecutor
public abstract class BaseExecutor implements Executor {
    //一级缓存容器对象
    protected PerpetualCache localCache = new PerpetualCache("LocalCache");

    /* 查询操作时，进行一级缓存处理 */
    public <E> List<E> query(...) {
        //尝试从一级缓存中获取结果
        list = resultHandler == null ? (List<E>) localCache.getObject(key) : null;
        if (list != null) {
            //一级缓存中存在则处理后直接返回
            handleLocallyCachedOutputParameters(ms, key, parameter, boundSql);
        } else {
            //一级缓存未命中，则进行查询
            list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);
        }
        return list;
    }
	
    /* 查询数据库 */
    private <E> List<E> queryFromDatabase(...) {
        //执行具体查询
        list = doQuery(ms, parameter, rowBounds, resultHandler, boundSql);
        //缓存结果到一级缓存
        localCache.putObject(key, list);
    }
    
    /* 根据传入信息创建对应CacheKey */
    public CacheKey createCacheKey(...) {
		//具体处理
    }
}
```

##### Cache

- Mybatis缓存容器对象接口
- org.apache.ibatis.cache.Cache

##### PerpetualCache

- 缓存容器对象
- Cache接口实现类，一级缓存固定此实现类

```java
//org.apache.ibatis.cache.impl.PerpetualCache
public class PerpetualCache implements Cache {
    //缓存存储集合，key为CacheKey
    private final Map<Object, Object> cache = new HashMap<>();
    
    /* 存储 */
    public void putObject(Object key, Object value) {
        cache.put(key, value);
    }
    
     /* 获取 */
    public Object getObject(Object key) {
        return cache.get(key);
    }
    
     /* 清除 */
    public Object removeObject(Object key) {
        return cache.remove(key);
    }
}
```

##### CacheKey

- 缓存对象的key值对象

```java
//
public class CacheKey implements Cloneable, Serializable {
    //key值的存储信息
	private List<Object> updateList;
}
```

#### 过程解析

```bash
# 容器创建
	# sqlSessionFactory创建sqlSession时，会创建其对应Executor对象
		# org.apache.ibatis.session.SqlSessionFactory#openSession()
		# org.apache.ibatis.session.defaults.DefaultSqlSessionFactory#openSessionFromDataSource
		# org.apache.ibatis.session.Configuration#newExecutor
	# Executor创建时，在其抽象父类BaseExecutor的构造中会创建PerpetualCache对象
	# 注
		# sqlSession -- Executor -- PerpetualCache
		# 所以一级缓存是对应sqlSession，同一个sqlSession进行方法调用/创建的mapper代理对象共享一个一级缓存容器
		
# 缓存管理
	# 在BaseExecutor中，进行JDBC操作前，会尝试从一级缓存中获取结果集
		# org.apache.ibatis.executor.BaseExecutor#query
		# 一级缓存命中，直接返回
		# 一级缓存未命中
			# org.apache.ibatis.executor.BaseExecutor#queryFromDatabase
			# 进行JDBC操作
				# org.apache.ibatis.executor.BaseExecutor#doQuery
			# 将结果集缓存
				# org.apache.ibatis.cache.impl.PerpetualCache#putObject
```

### 二级缓存

#### 涉及类库

##### CachingExecutor

- 进行缓存管理的Executor实现类
- 只进行二级缓存管理
  - 持有进行具体JDBC操作的Executor对象

```java
//org.apache.ibatis.executor.CachingExecutor
public class CachingExecutor implements Executor {
    //具体JDBC操作的Executor对象
    private final Executor delegate;
    //二级缓存的管理器对象
    private final TransactionalCacheManager tcm = new TransactionalCacheManager();

    /* 查询操作时进行二级缓存控制 */
    public <E> List<E> query(...) {
        //从MappedStatement中获取对应二级缓存容器对象(Cache实现类)
        Cache cache = ms.getCache();
        //如果当前MappedStatement有对应Cache容器对象，则进行二级缓存控制
        if (cache != null) {
            //尝试从二级缓存中获取结果集
            List<E> list = (List<E>) tcm.getObject(cache, key);
            if (list == null) {
                //二级缓存未命中，则调用被包装的Executor执行JDBC操作
                list = delegate.query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
                //将获取结果集保存至二级缓存
                tcm.putObject(cache, key, list); 
            }
            return list;
        }
        //当前MappedStatement没有对应Cache容器对象，跳过二级缓存控制
        return delegate.query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
    }
    
    /* 二级缓存刷新 */
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

##### Configuration

- 持有一个保存二级缓存容器对象的map集合

```java
public class Configuration {
    //key值为mapperClass全限定类名/mapper.xml的namespace
    protected final Map<String, Cache> caches = new StrictMap<>("Caches collection");
    //二级缓存全局控制参数，默认为true
    protected boolean cacheEnabled = true;

    /* 添加二级缓存容器对象 */
    public void addCache(Cache cache) {
        caches.put(cache.getId(), cache);
    }
    
    /* 获取二级缓存容器对象 */
    public Cache getCache(String id) {
        return caches.get(id);
    }

    /* 创建Executor对象时，受全局参数控制 */
    public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
        Executor executor = new SimpleExecutor(this, transaction);
        //如果开启全局二级缓存，则会使用CachingExecutor包装具体Executor
        if (cacheEnabled) {
            executor = new CachingExecutor(executor);
        }
        executor = (Executor) interceptorChain.pluginAll(executor);
        return executor;
    }
}
```

##### MappedStatement

- 持有当前MappedStatement所属nameSpace对应的二级缓存容器对象

```java
public final class MappedStatement {
    //二级缓存容器对象
    private Cache cache;

    /* 设置二级缓存容器对象 */
    public Builder cache(Cache cache) {
        mappedStatement.cache = cache;
        return this;
    }
    
    /* 获取二级缓存容器对象 */
    public Cache getCache() {
        return cache;
    }
}
```

##### TransactionalCacheManager

- 事务缓存管理器

```java
//org.apache.ibatis.cache.TransactionalCacheManager
public class TransactionalCacheManager {
    //事务缓存存储集合，key为二级缓存容器对象
    private final Map<Cache, TransactionalCache> transactionalCaches = new HashMap<>();

    /* 获取二级缓存容器对象 */
    private TransactionalCache getTransactionalCache(Cache cache) {
        //如果当前二级缓存没有对应事务缓存对象，则使用构造创建
        return transactionalCaches.computeIfAbsent(cache, TransactionalCache::new);
    }
    /* 获取二级缓存中对应CacheKey的缓存结果 */
    public Object getObject(Cache cache, CacheKey key) {
        return getTransactionalCache(cache).getObject(key);
    }
    /* 将查询结果已CacheKey为值缓存到二级缓存中 */
    public void putObject(Cache cache, CacheKey key, Object value) {
        getTransactionalCache(cache).putObject(key, value);
    }
    /* 清除当前二级缓存容器对应的缓存数据 */
    public void clear(Cache cache) {
        getTransactionalCache(cache).clear();
    }
    /* 刷新事务缓存数据到二级缓存中 */
    public void commit() {
        for (TransactionalCache txCache : transactionalCaches.values()) {
            txCache.commit();
        }
    }
}
```

##### TransactionalCache

- 事务缓存容器

```java
//org.apache.ibatis.cache.decorators.TransactionalCache
public class TransactionalCache implements Cache {
    //ms对应的真实Cache容器对象
    private Cache delegate;
    //事务结果缓存map
    private final Map<Object, Object> entriesToAddOnCommit;

    /* 将查询结果缓存到二级缓存中 */
    public void putObject(Object key, Object object) {
        //此时仅仅保存在事务结果缓存map中
        entriesToAddOnCommit.put(key, object);
    }
    /* 从二级缓存中获取结果 */
    public Object getObject(Object key) {
        // 从真实二级缓存容器对象中获取缓存数据
        Object object = delegate.getObject(key);
        return object;
    }
    /* 刷新事务缓存数据到二级缓存中 */
    private void flushPendingEntries() {
        for (Map.Entry<Object, Object> entry : entriesToAddOnCommit.entrySet()) {
            delegate.putObject(entry.getKey(), entry.getValue());
        }
    }
}
```

##### MapperBuilderAssistant

- mapper解析助手

```java
public class MapperBuilderAssistant extends BaseBuilder {
    //二级缓存对象临时引用
    private Cache currentCache;
    
}
```

#### 涉及配置

##### 主配置文件

```xml
<settings>
    <!-- 对应Configuration中cacheEnabled属性 -->
    <setting name = "cacheEnabled" value = "true" />
</settings>
```

##### Mapper配置文件

```xml
<mapper namespace="mapper.TransferMapper">
    <!-- 二级缓存配置标签；type：对应二级缓存实现类 -->
    <cache type="具体实现类"/>
    <!-- flushCache：当前操作是否需要刷新二级缓存 -->
    <update id=""  flushCache="false"/>
</mapper>
```

##### 注解配置

@CacheNamespace注解

#### 过程解析

##### 配置控制

```bash
# 主配置文件中对应cacheEnabled参数配置
	# 对应Configuration中cacheEnabled属性
		# 默认为true，表示全局开启二级缓存
	# 影响
		# 在我们获取SqlSession对象时，会创建其对应Executor对象
			# org.apache.ibatis.session.Configuration#newExecutor
            # 此时根据cacheEnabled参数，会使用CachingExecutor包装具体Executor
            # 如此在sqlSession借助Executor进行数据库操作时，会先经过CachingExecutor，此时可以通过CachingExecutor实现二级缓存的全局控制

# mapper文件配置的cache标签
	# 1. 在解析mapper.xml配置文件时，如果存在cache标签
		# org.apache.ibatis.builder.xml.XMLMapperBuilder#configurationElement
		# org.apache.ibatis.builder.xml.XMLMapperBuilder#cacheElement
		# 1. 获取二级缓存类型type参数
		# 2. 创建二级缓存
			# org.apache.ibatis.builder.MapperBuilderAssistant#useNewCache
			# 此时做了几件事
				# 1. 创建二级缓存对象，id为当前nameSpace
				# 2. 将二级缓存cache对象添加到Configuration中
					# org.apache.ibatis.session.Configuration#addCache
				# 3. 将cache对象保存到临时引用
					# currentCache = cache;
	# 2. 解析单个操作标签(select|insert|update|delete)时
    	# org.apache.ibatis.builder.xml.XMLMapperBuilder#buildStatementFromContext
        # org.apache.ibatis.builder.xml.XMLStatementBuilder#parseStatementNode
        # 此时借助builderAssistant创建MappedStatement
        	# org.apache.ibatis.builder.MapperBuilderAssistant#addMappedStatement
        	# 1. 创建MappedStatement对象
        	# 2. 从currentCache临时引用中获取二级缓存cache对象保存到MappedStatement对象中

# 总述
	# cacheEnabled参数配置
		# 控制了是否使用CachingExecutor包装具体Executor对象，达到二级缓存的全局控制
	# cache标签
		# 1. Configuration中对应caches默认创建空集合，是全局二级缓存存储map，key为nameSpace
			# 二级缓存对象时namespace级别由此而来
		# 2. 如果配置了cache标签，则会创建当前namespace对应的cache对象保存到Configuration中，并保存到currentCache临时引用
		# 3. 解析操作语句标签时，创建对应MappedStatement对象，此时通过currentCache临时引用获取当前nameSpace第一个Cache对象
			# 同一mapper.xml中对应MappedStatement对象共享同一个二级缓存cache
			
# 结论
	# Configuration中的二级缓存map是固定创建的
	# 如果配置了cacheEnabled，但是没有配置cache标签
		# 1. 会使用CachingExecutor包装具体Executor对象
		# 2. 不会创建nameSpace对应cache对象保存到Configuration和MappedStatement中
		# 3. CachingExecutor由于获取不到MappedStatement中的cache对象，不会进行二级缓存操作
	# 如果配置了cache标签，但是cacheEnabled为false
		# 1. 不会使用CachingExecutor包装具体Executor对象
		# 2. 创建nameSpace对应cache对象保存到Configuration和MappedStatement中
		# 3. 由于没有CachingExecutor进行处理，所以不会向cache对象中缓存查询结果
```

##### 流程分析

```bash
# 假定cacheEnabled为true，且配置了cache标签
	# 使用CachingExecutor包装具体Executor对象
	# 创建nameSpace对应cache对象保存到Configuration和MappedStatement中
	
# 缓存查询结果
	# sqlSession执行查询时调用executor的query方法
		# org.apache.ibatis.session.defaults.DefaultSqlSession#selectList
		# org.apache.ibatis.executor.Executor#query
	# 由于此时使用CachingExecutor包装具体Executor对象，则调用CachingExecutor的query方法
		# org.apache.ibatis.executor.CachingExecutor#query
		# 1. 获取MappedStatement中cache对象
			# org.apache.ibatis.mapping.MappedStatement#getCache
		# 2. 借助TransactionalCacheManager获取cache对象对应TransactionalCache对象
			# org.apache.ibatis.cache.TransactionalCacheManager#getObject
				# org.apache.ibatis.cache.TransactionalCacheManager#getTransactionalCache
		# 3. 尝试获取cache对象中缓存结果
			# org.apache.ibatis.cache.decorators.TransactionalCache#getObject
			# 获取缓存结果是从TransactionalCache对象持有的cache对象中获取，也就是解析配置时创建的二级缓存容器
		# 4. 缓存未命中时，进行JDBC操作获取结果集
		# 5. 借助TransactionalCacheManager缓存结果集数据
			# org.apache.ibatis.cache.TransactionalCacheManager#putObject
				# org.apache.ibatis.cache.decorators.TransactionalCache#putObject
				# 此时结果是缓存在TransactionalCache的entriesToAddOnCommit事务缓冲集合中
# 二级缓存生效
	# 由上述的查询结果缓存过程可知
		# 1. 查询结果最先缓存在TransactionalCache的entriesToAddOnCommit事务缓冲集合中
		# 2. 查询是从TransactionalCache对象持有的cache对象中获取
		# 3. 所以此时二级缓存是无效的，无法获取对应的结果集
	# TransactionalCache存在方法flushPendingEntries可以将entriesToAddOnCommit事务缓冲集合中对象添加到cache对象中
		# org.apache.ibatis.cache.decorators.TransactionalCache#flushPendingEntries
	# 回溯代码可以发现，在sqlSession调用方法时可以触发缓存刷新
		# org.apache.ibatis.session.defaults.DefaultSqlSession#commit
		# org.apache.ibatis.session.defaults.DefaultSqlSession#close

# 结论
	# 1. 二级缓存对应namespace
		# 同一namespace层级下MappedStatement共享同一cache对象
	# 2. 二级缓存存储在Configuration和MappedStatement中
	# 3. 二级缓存不同于一个缓存，不是即时生效的
		# 通过TransactionalCacheManager、TransactionalCache来控制二级缓存的事务控制
		
# 注
	# 此处仅分析大致过程，细节需后续学习
```

##### 缓存序列化

```bash
# 常态来说一/二级缓存具体实现都是PerpetualCache中的map集合来存储查询结果，但对于结果集的处理略有不同
	# 一级缓存
		# 一级缓存直接使用PerpetualCache，没有包装，所以进行结果集存储时是直接调用map集合存储
			# org.apache.ibatis.cache.impl.PerpetualCache#putObject
				# java.util.Map#put
	# 二级缓存
		# 二级缓存不同于一级缓存，它对应的PerpetualCache是有使用装饰者模式包装的，具体代码
			# org.apache.ibatis.builder.MapperBuilderAssistant#useNewCache
				# org.apache.ibatis.mapping.CacheBuilder#build
					# org.apache.ibatis.mapping.CacheBuilder#newCacheDecoratorInstance
		# 由于二级缓存是被包装的PerpetualCache，那么在事务提交时，缓存结果保存到二级缓存中时，不是直接由PerpetualCache处理，而是先调用包装类的方法
			# 此处重点的方法是
				# org.apache.ibatis.cache.decorators.SerializedCache#putObject
				# org.apache.ibatis.cache.decorators.SerializedCache#serialize
			# 此处将查询结果进行序列化后才保存到PerpetualCache中
# 结论
	# 一级缓存存入和取出的是同一对象
	# 二级缓存取出的不是存入的对象，而是存入对象序列化的结果
		# 二级缓存存储结果，其对应类要实现Serializable序列化接口
```

#### 第三方缓存

Mybatis中缓存存在形式为Cache接口实现类

- 一级缓存固定为PerpetualCache
- 二级缓存默认为PerpetualCache
  - 可以通过cache标签的type来使用第三方cache替代PerpetualCache
    - 可以自定义二级缓存实现类
  - 具体代码在org.apache.ibatis.session.Configuration#newExecutor中可查看

- 使用默认PerpetualCache作为二级缓存存在局限性
  - 不可以跨域使用
    - cache存储容器还是对应于程序唯一的Configuration对象

**我们可以使用Mybatis整合Redis**

- redis可以实现跨域存储

##### 依赖

```xml
<dependency> 
    <groupId>org.mybatis.caches</groupId> 
    <artifactId>mybatis-redis</artifactId> 
    <version>1.0.0-beta2</version> 
</dependency>
```

##### Redis配置文件

```properties
# redis.properties
redis.host=localhost
redis.port=6379
redis.connectionTimeout=5000
redis.password=
redis.database=0
```

##### Mybatis配置

- xml

  ```xml
  <cache type="org.mybatis.caches.redis.RedisCache" /> 
  ```

- mapperClass

  ```java
  @CacheNamespace(implementation = RedisCache.class)
  ```

##### 思路

```bash
# RedisConfigurationBuilder
	# org.mybatis.caches.redis.RedisConfigurationBuilder
	# 解析redis.properties配置文件，获取redis连接信息
# RedisCache
	# org.mybatis.caches.redis.RedisCache
	# Cache接口实现类
	# 将查询结果通过Jedis存储到Redis数据库中
```

### 插件机制

插件，通常是提供增强操作的

#### 配置

```xml
<plugins>
    <plugin interceptor="插件类全限定类名"></plugin>
</plugins>
```

#### 注解

```bash
# 实现接口Interceptor
# 添加注解
	# @Intercepts({@Signature(type= Executor.class, method = "update", args = {MappedStatement.class,Object.class})})
	# @Intercepts
		# 声明多个@Signature
	# @Signature
		# type
			# 拦截的对象类型
		# method
			# 对应拦截的方法
        # args
			# 对应拦截方法的参数列表
			# 避免方法重载导致的异常
```

#### 涉及类库

##### Interceptor

- 拦截器接口

- org.apache.ibatis.plugin.Interceptor

##### InterceptorChain

- 拦截器链

```java
//InterceptorChain
public class InterceptorChain {
	//拦截器集合
    private final List<Interceptor> interceptors = new ArrayList<>();

    /* 进行拦截包装 */
    public Object pluginAll(Object target) {
        for (Interceptor interceptor : interceptors) {
            target = interceptor.plugin(target);
        }
        return target;
    }
    /* 添加拦截器 */
    public void addInterceptor(Interceptor interceptor) {
        interceptors.add(interceptor);
    }
	/* 获取拦截器 */
    public List<Interceptor> getInterceptors() {
        return Collections.unmodifiableList(interceptors);
    }
}
```

##### Plugin

- InvocationHandler接口实现类

```java
//org.apache.ibatis.plugin.Plugin
public class Plugin implements InvocationHandler {
    //被包装对象
    private final Object target;
    //拦截器
    private final Interceptor interceptor;
	
    /* 通过动态代理将对象包装 */
    public static Object wrap(Object target, Interceptor interceptor) {
        Map<Class<?>, Set<Method>> signatureMap = getSignatureMap(interceptor);
        Class<?> type = target.getClass();
        Class<?>[] interfaces = getAllInterfaces(type, signatureMap);
        if (interfaces.length > 0) {
            return Proxy.newProxyInstance(
                type.getClassLoader(),
                interfaces,
                new Plugin(target, interceptor, signatureMap));
        }
        return target;
    }

    /* invoke方法调用 */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            Set<Method> methods = signatureMap.get(method.getDeclaringClass());
            //如果此方法为当前Interceptor拦截器的目标方法，则进行拦截调用
            if (methods != null && methods.contains(method)) {
                return interceptor.intercept(new Invocation(target, method, args));
            }
            return method.invoke(target, args);
        } catch (Exception e) {
            throw ExceptionUtil.unwrapThrowable(e);
        }
    }
	
    //解析当前Interceptor的@Intercepts注解，获取需要拦截的方法集合
    private static Map<Class<?>, Set<Method>> getSignatureMap(Interceptor interceptor) {
        Intercepts interceptsAnnotation = interceptor.getClass().getAnnotation(Intercepts.class);
        // issue #251
        if (interceptsAnnotation == null) {
            throw new PluginException("No @Intercepts annotation was found in interceptor " + interceptor.getClass().getName());
        }
        Signature[] sigs = interceptsAnnotation.value();
        Map<Class<?>, Set<Method>> signatureMap = new HashMap<>();
        //获取@Signature注解集合获取对应方法
        for (Signature sig : sigs) {
            Set<Method> methods = signatureMap.computeIfAbsent(sig.type(), k -> new HashSet<>());
            try {
                Method method = sig.type().getMethod(sig.method(), sig.args());
                methods.add(method);
            } catch (NoSuchMethodException e) {
                throw new PluginException("Could not find method on " + sig.type() + " named " + sig.method() + ". Cause: " + e, e);
            }
        }
        return signatureMap;
    }
}
```

#### 流程解析

```bash
# 拦截器创建
	# 拦截器配置在主配置文件中，当主配置文件解析时，会创建拦截器
		# org.apache.ibatis.builder.xml.XMLConfigBuilder#pluginElement
			# 根据拦截器类型创建实例
				# (Interceptor) resolveClass(interceptor).getDeclaredConstructor().newInstance();
			# 将拦截器添加到Configuration中
				# org.apache.ibatis.session.Configuration#addInterceptor

# 拦截器使用
	# 通过分析Plugin类可知，拦截增强实际就是通过动态代理创建代理对象实现增强
	# 而Mybatis体系中对应于JDBC操作有比较重要的四个对象，而插件就是通过将以下四个对象进行代理实现
		# Executor
			# 代理触发点
				# org.apache.ibatis.session.Configuration#newExecutor
				# org.apache.ibatis.plugin.InterceptorChain#pluginAll
			# 代理对象增强方法
				# update, query, flushStatements, commit, rollback, getTransaction, close, isClosed
		# ParameterHandler
			# 代理触发点
				# org.apache.ibatis.session.Configuration#newParameterHandler
				# org.apache.ibatis.plugin.InterceptorChain#pluginAll
			# 代理对象增强方法
				# getParameterObject, setParameters
		# ResultSetHandler
			# 代理触发点
				# org.apache.ibatis.session.Configuration#newResultSetHandler
				# org.apache.ibatis.plugin.InterceptorChain#pluginAll
			# 代理对象增强方法
				# handleResultSets, handleOutputParameters
		# StatementHandler
			# 代理触发点
				# org.apache.ibatis.session.Configuration#newExecutor
				# org.apache.ibatis.plugin.InterceptorChain#pluginAll
			# 代理对象增强方法
				# prepare, parameterize, batch, update, query
				
# pluginAll方法分析
	# org.apache.ibatis.plugin.InterceptorChain#pluginAll
	# 遍历所有拦截器，调用其plugin方法
		# org.apache.ibatis.plugin.Interceptor#plugin
		# 调用了Plugin类的wrap方法
			# org.apache.ibatis.plugin.Plugin#wrap

# wrap方法分析
	# 1. 解析当前Interceptor的Intercepts注解，获取当前Interceptor拦截的目标方法
		# org.apache.ibatis.plugin.Plugin#getSignatureMap
	# 2. 判断当前被代理对象是否存在其type的拦截方法配置
		# org.apache.ibatis.plugin.Plugin#getAllInterfaces
	# 3. 根据上述判断成立，则使用动态代理包装被代理对象
		# 判断成立，则使用动态代理包装被代理对象
			# java.lang.reflect.Proxy#newProxyInstance
		# 判断不成立，则直接返回被代理对象 
```

#### 自定义拦截器

```java
@Intercepts({@Signature(
    type= Executor.class,
    method = "update",
    args = {MappedStatement.class,Object.class})})
public class ExamplePlugin implements Interceptor {
    public Object intercept(Invocation invocation) throws Throwable {
        return invocation.proceed();
    }
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
    public void setProperties(Properties properties) {
    }
}
```

### 延时加载机制

```bash
# 在使用resultMap对应association/collection时，如果使用嵌套查询的方式，则可以实现延时加载
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

#### 配置

##### 主配置文件

```xml
<settings>
    <!-- 打开延迟加载的开关 -->
    <setting name="lazyLoadingEnabled" value="true"/>
    <!-- 将积极加载改为消极加载，即延迟加载 -->
    <setting name="aggressiveLazyLoading" value="false"/>
</settings>
```

##### mapper配置文件

```xml
<resultMap id="userAccountMap" type="Account">
    <id property="id" column="id"></id>
    <!-- 配置封装 User 的内容
            select：查询用户的唯一标识
            column：用户根据id查询的时候，需要的参数值
			fetchType：默认值为lazy表示延迟加载，此处配置可覆盖全局配置
        -->
    <association property="user" column="uid" fetchType="lazy" javaType="User" select="cn.ideal.mapper.UserMapper.findById"></association>
</resultMap>

```

#### 涉及类库

##### DefaultResultSetHandler

- ResultSetHandler接口实现类

```java
//org.apache.ibatis.executor.resultset.DefaultResultSetHandler
public class DefaultResultSetHandler implements ResultSetHandler {
    
    /* 创建结果对象 */
    private Object createResultObject(...) throws SQLException {
        this.useConstructorMappings = false; // reset previous mapping result
        final List<Class<?>> constructorArgTypes = new ArrayList<>();
        final List<Object> constructorArgs = new ArrayList<>();
        Object resultObject = createResultObject(rsw, resultMap, constructorArgTypes, constructorArgs, columnPrefix);
        if (resultObject != null && !hasTypeHandlerForResultObject(rsw, resultMap.getType())) {
            final List<ResultMapping> propertyMappings = resultMap.getPropertyResultMappings();
            for (ResultMapping propertyMapping : propertyMappings) {
                // 判断是否嵌套查询(使用select属性)、是否延时加载
                if (propertyMapping.getNestedQueryId() != null && propertyMapping.isLazy()) {
                    //延时加载，通过动态代理创建代理对象
                    resultObject = configuration.getProxyFactory().createProxy(...);
                    break;
                }
            }
        }
        this.useConstructorMappings = resultObject != null && !constructorArgTypes.isEmpty(); // set current mapping result
        return resultObject;
    }
}
```

##### JavassistProxyFactory

```java
//org.apache.ibatis.executor.loader.javassist.JavassistProxyFactory
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

#### 原理解析

```bash
# 解析配置
	# 主配置解析
		# 1. 解析settings标签，获取配置对应properties
			# org.apache.ibatis.builder.xml.XMLConfigBuilder#settingsAsProperties
		# 2. 根据properties，设置Configuration中对应属性值
			# org.apache.ibatis.builder.xml.XMLConfigBuilder#settingsElement
	# mapper配置解析
    	# 1. 解析resultMap标签
    		# org.apache.ibatis.builder.xml.XMLMapperBuilder#resultMapElements
    	# 2. 获取resultMap对应子标签属性内容
    		# org.apache.ibatis.builder.xml.XMLMapperBuilder#buildResultMappingFromContext
    			# 根据全局lazyLoadingEnabled参数和association/collection的lazy属性，获取当前association/collection是否延迟加载
    	# 3. 借助builderAssistant创建ResultMapping对象
    		# org.apache.ibatis.builder.MapperBuilderAssistant#buildResultMapping
    		
# 结果集解析
	# StatementHandler处理结果集时，借助DefaultResultSetHandler进行结果集解析封装，获取对应结果对象
	# DefaultResultSetHandler解析处理时，根据是否延迟加载判断，确认是否对返回结果对象进行包装
		# org.apache.ibatis.executor.resultset.DefaultResultSetHandler#createResultObject
		
# 代理对象进行属性获取时，则会触发包装对象的代理方法，执行对应sql获取结果进行赋值		
```

### 事务管理

```bash
# Mybatis事务管理

# Mybatis事务管理对象
	# TransactionFactory
		# org.apache.ibatis.transaction.TransactionFactory
		# 负责Transaction对象的创建
	# Transaction
		# 包装connection连接，进行事务管理

# Mybatis的管理对象分为两种，其对应的管理对象也不同
	# 依赖于从数据源得到的连接来管理事务
		# JdbcTransactionFactory
		# JdbcTransaction
	# 让容器来管理事务的整个生命周期
		# ManagedTransactionFactory
		# ManagedTransaction

# 原生使用过程
# 1. 解析environments标签，创建TransactionFactory
	# 入口
		# org.apache.ibatis.builder.xml.XMLConfigBuilder#environmentsElement
		# org.apache.ibatis.builder.xml.XMLConfigBuilder#transactionManagerElement
	# 过程
		# 1. 创建TransactionFactory
			# 1. 根据配置的type值获取对应TransactionFactory的类
				# org.apache.ibatis.builder.BaseBuilder#resolveClass
				# org.apache.ibatis.type.TypeAliasRegistry#resolveAlias
					# 实质是从typeAliases(Map<String, Class<?>>) 中获取
			# 2. 获取无参构造创建实例
            	# java.lang.Class#getDeclaredConstructor
            	# java.lang.reflect.Constructor#newInstance
	
		# 2. 解析dataSource标签获取数据源工厂DataSourceFactory
			# org.apache.ibatis.builder.xml.XMLConfigBuilder#dataSourceElement
			 	# 1. 根据配置的type值获取对应DataSourceFactory的类
       				# org.apache.ibatis.builder.BaseBuilder#resolveClass
        		# 2. 获取无参构造创建实例
       				# java.lang.Class#getDeclaredConstructor
        			# java.lang.reflect.Constructor#newInstance
        			
        # 3. 获取数据源
        	# org.apache.ibatis.datasource.DataSourceFactory#getDataSource
        	
        # 4. 将获取的TransactionFactory和DataSource包装到Environment对象中，设置为Configuration属性

# 2. 创建SqlSession时，封装Transaction到Executor中
	# 入口
		# org.apache.ibatis.session.defaults.DefaultSqlSessionFactory#openSessionFromDataSource
	# 过程
		# 1. 从configuration的environment中获取TransactionFactory
			# org.apache.ibatis.session.defaults.DefaultSqlSessionFactory#getTransactionFactoryFromEnvironment
		# 2. 创建Transaction对象
			# org.apache.ibatis.transaction.TransactionFactory#newTransaction
		# 3. 创建Executer时，封装Transaction到Executor中
			# org.apache.ibatis.session.Configuration#newExecutor
			# org.apache.ibatis.executor.BaseExecutor#BaseExecutor
	
# 3. executer对象执行操作时，获取connection连接，开启事务	
	# 入口
		# org.apache.ibatis.executor.SimpleExecutor#doUpdate
		# org.apache.ibatis.executor.BaseExecutor#getConnection
	# 过程
		# 1. 从Executor中的Transaction获取connection
        	# org.apache.ibatis.transaction.Transaction#getConnection
        # 2. 原生JDBC操作获取connection
        	# org.apache.ibatis.transaction.jdbc.JdbcTransaction#openConnection
        # 3. 从DataSource中获取连接
        	# javax.sql.DataSource#getConnection()
        # 4. 开启事务，即设置AutoCommit自动提交为false
        	# org.apache.ibatis.transaction.jdbc.JdbcTransaction#setDesiredAutoCommit
        	# java.sql.Connection#setAutoCommit
	
# 4. 在SqlSession提交/关闭时，事务提交
	# 入口
		# org.apache.ibatis.session.defaults.DefaultSqlSession#commit(boolean)
	# 过程
		# 1. 获取executor进行提交
			# org.apache.ibatis.executor.Executor#commit
		# 2. 调用Transaction进行提交
			# org.apache.ibatis.transaction.Transaction#commit
		# 3. connection进行提交
			# java.sql.Connection#commit
	
# 5. 在SqlSession调用rollback方法进行事务回滚	
	# 入口
		# org.apache.ibatis.session.defaults.DefaultSqlSession#rollback(boolean)
	# 过程
		# 1. 获取executor进行回滚
			# org.apache.ibatis.executor.Executor#rollback
		# 2. 获取Transaction进行回滚
			# org.apache.ibatis.transaction.Transaction#rollback
		# 3. connection进行回滚
			# java.sql.Connection#rollback()
			
# 总结
	# mybatis使用原生jdbc操作事务时
		# 1. 通过解析environment子标签transactionManager标签，创建TransactionFactory，保存到Configuration中
		# 2. 创建sqlSession时
			# 操作创建Executor，由TransactionFactory创建Transaction对象，并保存到Executor中
				# Transaction负责获取connection连接并对其进行管理，创建时开启事务
		# 3. Executor执行操作时，通过Transaction对象，进行连接获取、事务提交/回滚
```

