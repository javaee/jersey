package org.glassfish.jersey.jackson.internal.jackson.jaxrs.json;

import java.lang.annotation.Annotation;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg.EndpointConfigBase;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.annotation.JSONP;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.annotation.JacksonFeatures;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.fasterxml.jackson.databind.util.JSONWrappedObject;

/**
 * Container class for figuring out annotation-based configuration
 * for JAX-RS end points.
 */
public class JsonEndpointConfig
    extends EndpointConfigBase<JsonEndpointConfig>
{
    // // Serialization-only config

    protected JSONP.Def _jsonp;
    
    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */

    protected JsonEndpointConfig(MapperConfig<?> config) {
        super(config);
    }

    public static JsonEndpointConfig forReading(ObjectReader reader,
                                                Annotation[] annotations)
    {
        return new JsonEndpointConfig(reader.getConfig())
            .add(annotations, false)
            .initReader(reader);
    }

    public static JsonEndpointConfig forWriting(ObjectWriter writer,
                                                Annotation[] annotations,
                                                String defaultJsonpMethod)
    {
        JsonEndpointConfig config =  new JsonEndpointConfig(writer.getConfig());
        if (defaultJsonpMethod != null) {
            config._jsonp = new JSONP.Def(defaultJsonpMethod);
        }
        return config
            .add(annotations, true)
            .initWriter(writer)
        ;
    }

    /*
    /**********************************************************
    /* Abstract method impls, overrides
    /**********************************************************
     */

    @SuppressWarnings("deprecation")
    @Override
    protected void addAnnotation(Class<? extends Annotation> type,
            Annotation annotation, boolean forWriting)
    {
        if (type == JSONP.class) {
            if (forWriting) {
                _jsonp = new JSONP.Def((JSONP) annotation);
            }
        } else if (type == JacksonFeatures.class) {
            JacksonFeatures feats = (JacksonFeatures) annotation;
            if (forWriting) {
                _serEnable = nullIfEmpty(feats.serializationEnable());
                _serDisable = nullIfEmpty(feats.serializationDisable());
            } else {
                _deserEnable = nullIfEmpty(feats.deserializationEnable());
                _deserDisable = nullIfEmpty(feats.deserializationDisable());
            }
        } else {
            super.addAnnotation(type, annotation, forWriting);
        }
    }

    @Override
    public Object modifyBeforeWrite(Object value) {
        return applyJSONP(value);
    }
    
    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    /**
     * Method that will add JSONP wrapper object, if and as
     * configured by collected annotations.
     */
    public Object applyJSONP(Object value)
    {
        if (_jsonp != null) {
            // full prefix+suffix?
            if (_jsonp.prefix != null || _jsonp.suffix != null) {
                return new JSONWrappedObject(_jsonp.prefix, _jsonp.suffix, value);
            }
            if (_jsonp.method != null) {
                return new JSONPObject(_jsonp.method, value);
            }
        }
        return value;
    }
}
