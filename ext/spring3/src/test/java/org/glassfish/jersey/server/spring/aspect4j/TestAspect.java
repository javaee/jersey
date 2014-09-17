package org.glassfish.jersey.server.spring.aspect4j;

import javax.inject.Singleton;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
@Singleton
public class TestAspect {

    private int interceptions = 0;

    @Before("execution(* org.glassfish.jersey.server.spring.aspect4j.*.*())")
    public void intercept(JoinPoint joinPoint) {
        interceptions++;
    }

    public int getInterceptions() {
        return interceptions;
    }

    public void reset() {
        interceptions = 0;
    }
}