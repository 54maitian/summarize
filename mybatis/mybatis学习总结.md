# Mybatis学习总结

## JDBC原生

JDBC是一套java语言访问数据库的规范API，我们可以使用其来访问不同数据库

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

#### 涉及类库

#### MapperRegistry

```bash
# 前面讲述配置解析有对应描述
```

#### MapperProxyFactory

- mapper代理工厂

- 对应于每个mapperClass资源

```java
//org.apache.ibatis.binding.MapperProxyFactory
public class MapperRegistry {
    
}
```

