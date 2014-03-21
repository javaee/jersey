package org.glassfish.jersey.server.spring.profiles;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertEquals;

public class SpringProfilesTest {

    @Test
    public void shouldGetDefaultBean() {
        System.setProperty("spring.profiles.active", "");
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("org.glassfish.jersey.server.spring.profiles");
        assertEquals("default", context.getBean(TestService.class).test());
    }

    @Test
    public void shouldGetDevProfileBean() {
        System.setProperty("spring.profiles.active", "dev");
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("org.glassfish.jersey.server.spring.profiles");
        assertEquals("dev", context.getBean(TestService.class).test());
    }
}
