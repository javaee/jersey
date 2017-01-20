package org.glassfish.jersey.client.proxy;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

/**
 * Passes annotations from the method to the context annotations, giving access to the method
 * annotations in the message body writer.
 */
public class SpecialWriterInterceptor implements WriterInterceptor {

    @SuppressWarnings("unchecked")
    @Override public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        List<Annotation> methodAnnotations =
                (List<Annotation>) context.getProperty(SpecialFormatter.SPECIAL_FORMATTER_PROPERTY_KEY);

        if (methodAnnotations != null) {
            int newSize = context.getAnnotations().length + methodAnnotations.size();
            int originalSize = context.getAnnotations().length;
            Annotation[] newAnnotations = Arrays.copyOf(context.getAnnotations(), newSize);
            for (int i = originalSize; i < newSize; i++) {
                newAnnotations[i] = methodAnnotations.get(i - originalSize);
            }

            context.setAnnotations(newAnnotations);
        }
        context.proceed();
    }
}
