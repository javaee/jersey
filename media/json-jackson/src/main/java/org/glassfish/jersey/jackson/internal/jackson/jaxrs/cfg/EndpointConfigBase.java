package org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg;

import java.lang.annotation.Annotation;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.annotation.JacksonFeatures;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.MapperConfig;

/**
 * Container class for figuring out annotation-based configuration
 * for JAX-RS end points.
 */
public abstract class EndpointConfigBase<THIS extends EndpointConfigBase<THIS>>
{
    // // General configuration

    /**
     * @since 2.6
     */
    protected final MapperConfig<?> _config;

    protected Class<?> _activeView;

    protected String _rootName;

    // // Deserialization-only config

    protected DeserializationFeature[] _deserEnable;
    protected DeserializationFeature[] _deserDisable;

    protected ObjectReader _reader;

    // // Serialization-only config

    protected SerializationFeature[] _serEnable;
    protected SerializationFeature[] _serDisable;

    protected ObjectWriter _writer;

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */

    protected EndpointConfigBase(MapperConfig<?> config) {
        _config = config;
    }
    
    @Deprecated // since 2.6
    protected EndpointConfigBase() {
        _config = null;
    }

    @SuppressWarnings("unchecked")
    protected THIS add(Annotation[] annotations, boolean forWriting)
    {
        if (annotations != null) {
            for (Annotation annotation : annotations) {
                addAnnotation(annotation.annotationType(), annotation, forWriting);
            }
        }
        return (THIS) this;
    }

    protected void addAnnotation(Class<? extends Annotation> type,
            Annotation annotation, boolean forWriting)
    {
        if (type == JsonView.class) {
            // Can only use one view; but if multiple defined, use first (no exception)
            Class<?>[] views = ((JsonView) annotation).value();
            _activeView = (views.length > 0) ? views[0] : null;
        } else if (type == JacksonFeatures.class) {
            JacksonFeatures feats = (JacksonFeatures) annotation;
            if (forWriting) {
                _serEnable = nullIfEmpty(feats.serializationEnable());
                _serDisable = nullIfEmpty(feats.serializationDisable());
            } else {
                _deserEnable = nullIfEmpty(feats.deserializationEnable());
                _deserDisable = nullIfEmpty(feats.deserializationDisable());
            }
        } else if (type == JsonRootName.class) {
            _rootName = ((JsonRootName) annotation).value();
        } else if (type == JacksonAnnotationsInside.class) {
            // skip; processed below (in parent), so encountering here is of no use
        } else {
            // For all unrecognized types, check meta-annotation(s) to see if they are bundles
            JacksonAnnotationsInside inside = type.getAnnotation(JacksonAnnotationsInside.class);
            if (inside != null) {
                add(type.getAnnotations(), forWriting);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected THIS initReader(ObjectReader reader)
    {
        if (_activeView != null) {
            reader = reader.withView(_activeView);
        }
        if (_rootName != null) {
            reader = reader.withRootName(_rootName);
        }
        // Then deser features
        if (_deserEnable != null) {
            reader = reader.withFeatures(_deserEnable);
        }
        if (_deserDisable != null) {
            reader = reader.withoutFeatures(_deserDisable);
        }
        _reader = reader;
        return (THIS) this;
    }

    @SuppressWarnings("unchecked")
    protected THIS initWriter(ObjectWriter writer)
    {
        if (_activeView != null) {
            writer = writer.withView(_activeView);
        }
        if (_rootName != null) {
            writer = writer.withRootName(_rootName);
        }
        // Then features
        if (_serEnable != null) {
            writer = writer.withFeatures(_serEnable);
        }
        if (_serDisable != null) {
            writer = writer.withoutFeatures(_serDisable);
        }
        _writer = writer;
        return (THIS) this;
    }
    
    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    /**
     * @since 2.3
     */
    public String getRootName() {
        return _rootName;
    }

    /**
     * @since 2.3
     */
    public Class<?> getActiveView() {
        return _activeView;
    }
    
    public final ObjectReader getReader() {
        if (_reader == null) { // sanity check, should never happen
            throw new IllegalStateException();
        }
        return _reader;
    }

    public final ObjectWriter getWriter() {
        if (_writer == null) { // sanity check, should never happen
            throw new IllegalStateException();
        }
        return _writer;
    }

    /*
    /**********************************************************
    /* Value modifications
    /**********************************************************
     */

    public abstract Object modifyBeforeWrite(Object value);

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected static <T> T[] nullIfEmpty(T[] arg) {
        if (arg == null || arg.length == 0) {
            return null;
        }
        return arg;
    }
}
