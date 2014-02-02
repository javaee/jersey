package org.glassfish.jersey.examples.hello.spring.annotations.annotations;

import org.glassfish.jersey.test.JerseyTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.ws.rs.core.Application;

/**
 * Testing our service with our annotation context being passed directly to jersey-spring
 *
 * @author Geoffroy Warin (http://geowarin.github.io)
 */
public class SpringRequestResourceTest extends JerseyTest {

    @Override
    protected Application configure() {
        ApplicationContext context = new AnnotationConfigApplicationContext(SpringAnnotationConfig.class);
        return new JerseyConfig()
                .property("contextConfig", context);
    }

    @Test
    public void testGreet() throws Exception {
        final String greeting = target("spring-hello").request().get(String.class);
        Assert.assertEquals("hello, world 1!", greeting);
        final String greeting2 = target("spring-hello").request().get(String.class);
        Assert.assertEquals("hello, world 2!", greeting2);
    }
}
