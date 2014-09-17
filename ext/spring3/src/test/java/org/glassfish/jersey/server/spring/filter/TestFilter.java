package org.glassfish.jersey.server.spring.filter;

import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import org.springframework.stereotype.Component;

@Component
@Singleton
public class TestFilter implements ContainerRequestFilter {

    @Inject
    private Counter counter;

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        counter.inc();
    }

}
