package org.glassfish.jersey.examples.helloworld.spring;

public class GreetingServiceImpl implements GreetingService {
    @Override
    public String greet(String who) {
        return "hello, "+who+"!";
    }
}
