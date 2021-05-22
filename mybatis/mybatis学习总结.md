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
<!-- namespace + sqlId：mappedStatement全局唯一的statementId -->
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

### 源码分析

#### 涉及类库

- Resources
  - org.apache.ibatis.io.Resources
  - 通过类加载其加载主配置文件为输入流
- SqlSessionFactoryBuilder
  - org.apache.ibatis.session.SqlSessionFactoryBuilder
  - 接收文件输入流，借助XMLConfigBuilder解析后返回的Configuration，创建对应的SqlSessionFactory
- XMLConfigBuilder
  - org.apache.ibatis.builder.xml.XMLConfigBuilder
  - 分析XPathParser解析主配置文件的XNode对象，创建Configuration返回
- XMLMapperBuilder
  - org.apache.ibatis.builder.xml.XMLMapperBuilder
  - 分析XPathParser解析mapper配置文件的XNode对象，创建MappedStatement保存到Configuration中
- MapperBuilderAssistant
  - org.apache.ibatis.builder.MapperBuilderAssistant

##### 



##### 





##### 





##### 





##### 









