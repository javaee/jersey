package org.glassfish.jersey.client.proxy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SpecialFormatter {
    String SPECIAL_FORMATTER_PROPERTY_KEY = "com.example.property.key";

    String value();
}
