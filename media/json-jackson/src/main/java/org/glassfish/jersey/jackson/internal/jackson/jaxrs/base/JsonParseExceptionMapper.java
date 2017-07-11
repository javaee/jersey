package org.glassfish.jersey.jackson.internal.jackson.jaxrs.base;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import com.fasterxml.jackson.core.JsonParseException;

/**
 * Implementation of {@link ExceptionMapper} to send down a "400 Bad Request"
 * in the event unparsable JSON is received.
 *<p>
 * Note that {@link javax.ws.rs.ext.Provider} annotation was include up to
 * Jackson 2.7, but removed from 2.8 (as per [jaxrs-providers#22]
 *
 * @since 2.2
 */
public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {
    @Override
    public Response toResponse(JsonParseException exception) {
        return Response.status(Response.Status.BAD_REQUEST).entity(exception.getMessage()).type("text/plain").build();
    }
}
