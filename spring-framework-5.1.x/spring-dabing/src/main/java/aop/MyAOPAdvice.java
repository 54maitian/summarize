package aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * @author T00032266
 * @DateTime 2021/5/20
 */
//声明一个advice增强类
@Aspect
@Component
public class MyAOPAdvice {

	//使用一个空方法声明切点
	@Pointcut("execution(* *..*.*(..))")
	public void pointcut(){};

	//前置通知
	//jp 连接点的基本信息
	//result 获取连接点的返回对象
	@Before("pointcut()")
	public void before(JoinPoint jp) {
		System.out.println("前置通知");
	}

	//最终通知
	@After("pointcut()")
	public void after() {
		System.out.println("前置通知");
	}

	//后置通知
	@AfterReturning(value = "pointcut()",returning = "msg")
	public void afterReturning(JoinPoint jp,Object msg) {
		System.out.println("前置通知");
	}

	//异常通知
	@AfterThrowing(value = "pointcut()",throwing = "ex")
	public void afterThrowing(Throwable ex) {
		System.out.println("前置通知");
	}

	//环绕通知
	// pjp 对连接点的方法内容进行整体控制
	//@Around("pointcut()")
	public Object  around(ProceedingJoinPoint pjp) throws Throwable {
		System.out.println("环绕before");
		Object proceed = null;
		try {
			proceed = pjp.proceed();
			System.out.println("环绕afterReturning");
		} catch (Throwable throwable) {
			System.out.println("环绕afterThrowing");
			throw throwable;
		} finally {
			System.out.println("环绕after");
		}

		return proceed;
	}
}
