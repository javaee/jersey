package org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg;

import com.fasterxml.jackson.databind.cfg.ConfigFeature;

/**
 * Enumeration that defines simple on/off features that can be
 * used on all Jackson JAX-RS providers, regardless of
 * underlying data format.
 */
public enum JaxRSFeature implements ConfigFeature
{
    /*
    /**********************************************************
    /* Input handling
    /**********************************************************
     */

    /**
     * Feature related to
     * <a href="https://github.com/FasterXML/jackson-jaxrs-providers/issues/49">Issue #49</a>:
     * whether empty input is considered legal or not.
     * If set to true, empty content is allowed and will be read as Java 'null': if false,
     * an {@link java.io.IOException} will be thrown.
     *<p>
     * NOTE: in case of JAX-RS 2.0, specific exception will be <code>javax.ws.rs.core.NoContentException</code>;
     * but this is not defined in JAX-RS 1.x.
     */
    ALLOW_EMPTY_INPUT(true),

    /*
    /**********************************************************
    /* HTTP headers
    /**********************************************************
     */
    
    /**
     * Feature that can be enabled to make provider automatically
     * add "nosniff" (see
     * <a href="http://security.stackexchange.com/questions/20413/how-can-i-prevent-reflected-xss-in-my-json-web-services">this entry</a>
     * for details
     *<p>
     * Feature is disabled by default.
     */
    ADD_NO_SNIFF_HEADER(false),

    /*
    /**********************************************************
    /* Caching, related
    /**********************************************************
     */

    /**
     * Feature that may be enabled to force dynamic lookup of <code>ObjectMapper</code>
     * via JAX-RS Provider interface, regardless of whether <code>MapperConfigurator<code>
     * has explicitly configured mapper or not; if disabled, static configuration will
     * take precedence.
     * Note that if this feature is enabled, it typically makes sense to also disable
     * {@link JaxRSFeature#CACHE_ENDPOINT_READERS} and {@link JaxRSFeature#CACHE_ENDPOINT_WRITERS}
     * since caching would prevent lookups.
     *<p>
     * Feature is disabled by default.
     *
     * @since 2.8
     */
    DYNAMIC_OBJECT_MAPPER_LOOKUP(false),
    
    /**
     * [jaxrs-providers#86]: Feature that determines whether provider will cache endpoint
     * definitions for reading or not (including caching of actual <code>ObjectReader</code> to use).
     * Feature may be disabled if reconfiguration or alternate isntance of <code>ObjectMapper</code> is needed.
     *<p>
     * Note that disabling of the feature may add significant amount of overhead for processing.
     *<p>
     * Feature is enabled by default.
     *
     * @since 2.8
     */
    CACHE_ENDPOINT_READERS(true),

    /**
     * [jaxrs-providers#86]: Feature that determines whether provider will cache endpoint
     * definitions for writing or not (including caching of actual <code>ObjectWriter</code> to use).
     * Feature may be disabled if reconfiguration or alternate isntance of <code>ObjectMapper</code> is needed.
     *<p>
     * Note that disabling of the feature may add significant amount of overhead for processing.
     *<p>
     * Feature is enabled by default.
     *
     * @since 2.8
     */
    CACHE_ENDPOINT_WRITERS(true),

    /*
    /**********************************************************
    /* Other
    /**********************************************************
     */
    
    ;

    private final boolean _defaultState;
    
    private JaxRSFeature(boolean defaultState) {
        _defaultState = defaultState;
    }

    public static int collectDefaults() {
        int flags = 0;
        for (JaxRSFeature f : values()) {
            if (f.enabledByDefault()) { flags |= f.getMask(); }
        }
        return flags;
    }
    
    @Override
    public boolean enabledByDefault() { return _defaultState; }

    @Override
    public int getMask() { return (1 << ordinal()); }

    @Override
    public boolean enabledIn(int flags) { return (flags & getMask()) != 0; }    
}
