package org.glassfish.jersey.client.proxy;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

// There has to be a better way...
public class MyMessageBodyReader implements MessageBodyReader<MyMessage> {
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public MyMessage readFrom(Class<MyMessage> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        InputStreamReader reader = new InputStreamReader(entityStream, "UTF-8");
        final int bufferSize = 1024;
        final char[] buffer = new char[bufferSize];
        StringBuilder builder = new StringBuilder();

        int readCharacters = reader.read(buffer, 0, buffer.length);
        while (readCharacters > 0) {
            builder.append(buffer, 0, readCharacters);
            readCharacters = reader.read(buffer, 0, buffer.length);
        }

        return new MyMessage(builder.toString());
    }
}
