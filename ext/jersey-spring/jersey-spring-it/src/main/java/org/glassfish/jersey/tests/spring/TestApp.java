package org.glassfish.jersey.tests.spring;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.spring.SpringLifecycleListener;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

public class TestApp extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        System.out.println("XX getClasses()");
        final HashSet<Class<?>> classes = Sets.newHashSet();
        classes.add(TestResource.class);
        classes.add(SpringLifecycleListener.class);
        return classes;
    }

    @Path("testresource")
    public static class TestResource {
        
        @Autowired
        private GreetingService greetingService;
        
        @GET
        public String get() {
            System.out.println("XX GET()");
            String msg = greetingService.greet("world");
            return msg;
        }
    }
}
