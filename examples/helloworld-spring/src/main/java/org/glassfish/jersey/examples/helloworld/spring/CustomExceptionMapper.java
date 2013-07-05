package org.glassfish.jersey.examples.helloworld.spring;

import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Logger;

@Provider
@Component
public class CustomExceptionMapper implements ExceptionMapper<IllegalArgumentException> {
    private static final Logger LOGGER = Logger.getLogger(CustomExceptionMapper.class.getName());

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        LOGGER.fine("toResponse: "+this);
        return Response.serverError().build();
    }
}
