package org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg;

import java.io.IOException;

import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectReader;

/**
 * @since 2.3
 */
public abstract class ObjectReaderModifier
{
    /**
     * Method called to let modifier make any changes it wants to to objects
     * used for reading request objects for specified endpoint.
     * 
     * @param endpoint End point for which reader is used
     * @param httpHeaders HTTP headers sent with request (read-only)
     * @param resultType Type that input is to be bound to
     * @param r ObjectReader as constructed for endpoint, type to handle
     * @param p Parser to use for reading content
     */
    public abstract ObjectReader modify(EndpointConfigBase<?> endpoint,
            MultivaluedMap<String,String> httpHeaders,
            JavaType resultType, ObjectReader r, JsonParser p)
        throws IOException;
}
