package org.glassfish.jersey.examples.hello.spring.annotations.annotations;

import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;

/**
 * A simple package based configuration
 *
 * @author Geoffroy Warin (http://geowarin.github.io)
 */
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        register(RequestContextFilter.class);
        packages("org.glassfish.jersey.examples.hello.spring.annotations.annotations");
        register(LoggingFilter.class);
    }
}