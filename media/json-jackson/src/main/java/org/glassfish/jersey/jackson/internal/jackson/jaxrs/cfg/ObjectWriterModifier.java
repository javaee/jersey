package org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg;

import java.io.IOException;

import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * @since 2.3
 */
public abstract class ObjectWriterModifier
{
    /**
     * Method called to let modifier make any changes it wants to to objects
     * used for writing response for specified endpoint.
     * 
     * @param responseHeaders HTTP headers being returned with response (mutable)
     */
    public abstract ObjectWriter modify(EndpointConfigBase<?> endpoint,
            MultivaluedMap<String,Object> responseHeaders,
            Object valueToWrite, ObjectWriter w, JsonGenerator g)
        throws IOException;
}
