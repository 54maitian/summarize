package config;


import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @author T00032266
 * @DateTime 2021/5/18
 */
@Configuration
@ComponentScan("model")
//配置类上开启aop注解驱动
@EnableAspectJAutoProxy
public class MyConfiguration {



}
