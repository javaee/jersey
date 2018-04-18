package org.glassfish.jersey.client.proxy;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class MyMessageBodyWriter implements MessageBodyWriter<MyMessage> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public long getSize(MyMessage request, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {

        return getContent(request, getAnnotation(annotations, SpecialFormatter.class)).length();
    }

    @Override
    public void writeTo(MyMessage string, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        String beanBody = getContent(string, getAnnotation(annotations, SpecialFormatter.class));
        entityStream.write(beanBody.getBytes());
    }

    @SuppressWarnings("unchecked")
    private <T extends Annotation> T getAnnotation(Annotation[] annotations, Class<T> annotationClass) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == annotationClass) {
                return (T) annotation;
            }
        }
        return null;
    }


    private String getContent(MyMessage request, SpecialFormatter formatter) {
        if (formatter == null) {
            return request.getValue();
        }

        return String.format("%s-%s", request.getValue(), formatter.value());
    }
}
