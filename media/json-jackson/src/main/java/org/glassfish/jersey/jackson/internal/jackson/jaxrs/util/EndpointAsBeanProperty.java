package org.glassfish.jersey.jackson.internal.jackson.jaxrs.util;

import java.lang.annotation.Annotation;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.PropertyMetadata;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.AnnotationMap;

/**
 * {@link BeanProperty} implementation used for passing annotations
 * from JAX-RS endpoint into Jackson. This tries to imitate behavior
 * one would get if actual resource method was used as POJO property;
 * ideally this would be how implementation works but due JAX-RS API
 * limitations, we are only given annotations associated, and that
 * has to do.
 *<p>
 * NOTE: not yet used by JAX-RS provider, directly, as of Jackson 2.6.
 */
public class EndpointAsBeanProperty
    extends BeanProperty.Std
{
    public final static PropertyName ENDPOINT_NAME = new PropertyName("JAX-RS/endpoint");

    private final static AnnotationMap NO_ANNOTATIONS = new AnnotationMap();

    protected transient Annotation[] _rawAnnotations;
    
    public AnnotationMap _annotations;

    public EndpointAsBeanProperty(PropertyName name, JavaType type, Annotation[] annotations)
    {
        // TODO: find and pass wrapper; isRequired marker?
        super(name, type, /*PropertyName wrapperName*/ null,
                null, null, PropertyMetadata.STD_OPTIONAL);
        _rawAnnotations = annotations;
        _annotations = null;
    }

    protected EndpointAsBeanProperty(EndpointAsBeanProperty base, JavaType newType)
    {
        super(base, newType);
        _rawAnnotations = base._rawAnnotations;
        _annotations = base._annotations;
    }

    @Override
    public Std withType(JavaType type) {
        if (_type == type) {
            return this;
        }
        return new Std(_name, type, _wrapperName, _contextAnnotations, _member, _metadata);
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> acls) {
        return annotations().get(acls);
    }

    protected AnnotationMap annotations() {
        AnnotationMap am = _annotations;
        if (am == null) {
            Annotation[] raw = _rawAnnotations;
            if (raw == null || raw.length == 0) {
                am = NO_ANNOTATIONS;
            } else {
                am = new AnnotationMap();
                for (Annotation a : raw) {
                    am.add(a);
                }
            }
            _annotations = am;
        }
        return am;
    }
}
