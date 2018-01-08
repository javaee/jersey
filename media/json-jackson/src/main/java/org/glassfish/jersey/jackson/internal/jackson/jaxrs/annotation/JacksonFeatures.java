package org.glassfish.jersey.jackson.internal.jackson.jaxrs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Annotation that can be used enable and/or disable various
 * features for <code>ObjectReader</code>s and <code>ObjectWriter</code>s.
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@com.fasterxml.jackson.annotation.JacksonAnnotation
public @interface JacksonFeatures
{
    /**
     * Deserialization features to enable.
     */
    public DeserializationFeature[] deserializationEnable() default { };

    /**
     * Deserialization features to disable.
     */
    public DeserializationFeature[] deserializationDisable() default { };
    
    /**
     * Serialization features to enable.
     */
    public SerializationFeature[] serializationEnable() default { };

    /**
     * Serialization features to disable.
     */
    public SerializationFeature[] serializationDisable() default { };
}
