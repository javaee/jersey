package org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Helper class used to encapsulate details of configuring an
 * {@link ObjectMapper} instance to be used for data binding, as
 * well as accessing it.
 */
public abstract class MapperConfiguratorBase<IMPL extends MapperConfiguratorBase<IMPL,MAPPER>,
    MAPPER extends ObjectMapper
>
{
    /**
     * Mapper provider was constructed with if any, or that was constructed
     * due to a call to explicitly configure mapper.
     * If defined (explicitly or implicitly) it will be used, instead
     * of using provider-based lookup.
     */
    protected MAPPER _mapper;

    /**
     * If no mapper was specified when constructed, and no configuration
     * calls are made, a default mapper is constructed. The difference
     * between default mapper and regular one is that default mapper
     * is only used if no mapper is found via provider lookup.
     */
    protected MAPPER _defaultMapper;

    /**
     * Annotations set to use by default; overridden by explicit call
     * to {@link #setAnnotationsToUse}
     */
    protected Annotations[] _defaultAnnotationsToUse;
    
    /**
     * To support optional dependency to Jackson JAXB annotations module
     * (needed iff JAXB annotations are used for configuration)
     */
    protected Class<? extends AnnotationIntrospector> _jaxbIntrospectorClass;
    
    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */
    
    public MapperConfiguratorBase(MAPPER mapper, Annotations[] defaultAnnotations)
    {
        _mapper = mapper;
        _defaultAnnotationsToUse = defaultAnnotations;
    }

    /*
    /**********************************************************
    /* Abstract methods to implement
    /***********************************************************
     */
    
    /**
     * Method that locates, configures and returns {@link ObjectMapper} to use
     */
    public abstract MAPPER getConfiguredMapper();

    public abstract MAPPER getDefaultMapper();

    /**
     * Helper method that will ensure that there is a configurable non-default
     * mapper (constructing an instance if one didn't yet exit), and return
     * that mapper.
     */
    protected abstract MAPPER mapper();

    protected abstract AnnotationIntrospector _resolveIntrospectors(Annotations[] annotationsToUse);
    
    /*
    /***********************************************************
    /* Configuration methods
    /***********************************************************
     */

    public synchronized final void setMapper(MAPPER m) {
        _mapper = m;
    }

    public synchronized final void setAnnotationsToUse(Annotations[] annotationsToUse) {
        _setAnnotations(mapper(), annotationsToUse);
    }

    public synchronized final void configure(DeserializationFeature f, boolean state) {
        mapper().configure(f, state);
    }

    public synchronized final void configure(SerializationFeature f, boolean state) {
        mapper().configure(f, state);
    }

    public synchronized final void configure(JsonParser.Feature f, boolean state) {
        mapper().configure(f, state);
    }

    public synchronized final void configure(JsonGenerator.Feature f, boolean state) {
        mapper().configure(f, state);
    }

    /*
    /***********************************************************
    /* Helper methods for sub-classes
    /***********************************************************
     */

    protected final void _setAnnotations(ObjectMapper mapper, Annotations[] annotationsToUse)
    {
        AnnotationIntrospector intr;
        if (annotationsToUse == null || annotationsToUse.length == 0) {
            intr = AnnotationIntrospector.nopInstance();
        } else {
            intr = _resolveIntrospectors(annotationsToUse);
        }
        mapper.setAnnotationIntrospector(intr);
    }
}
