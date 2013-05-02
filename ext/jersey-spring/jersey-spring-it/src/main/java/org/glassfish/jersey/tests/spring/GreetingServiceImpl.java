package org.glassfish.jersey.tests.spring;

public class GreetingServiceImpl implements GreetingService {
    @Override
    public String greet(String who) {
        return "hello, "+who+"!";
    }
}
