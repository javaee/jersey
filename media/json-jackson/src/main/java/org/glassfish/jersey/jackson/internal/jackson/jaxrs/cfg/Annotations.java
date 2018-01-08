package org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg;

/**
 * Enumeration that defines standard annotation sets available for configuring
 * data binding aspects.
 * 
 * @since 2.2 (earlier located in actual datatype-specific modules)
 */
public enum Annotations {
    /**
     * Standard Jackson annotations, defined in Jackson core and databind
     * packages
     */
    JACKSON,

    /**
     * Standard JAXB annotations, used in a way that approximates expected
     * definitions (since JAXB defines XML aspects, not all features map
     * well to JSON handling)
     */
    JAXB
    ;
}
