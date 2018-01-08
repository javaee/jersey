package org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Annotation that can be used enable and/or disable various
 * features for <code>ObjectReader</code>s and <code>ObjectWriter</code>s.
 * 
 * @deprecated Since 2.2, use shared {@link com.fasterxml.jackson.jaxrs.annotation.JacksonFeatures} instead
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@com.fasterxml.jackson.annotation.JacksonAnnotation
@Deprecated // since 2.2
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
