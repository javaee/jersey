package org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg;

import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.ObjectReader;

/**
 * Based on ideas from [Issue#32], this class allows registering a
 * modifier ({@link ObjectReaderModifier}) that can be used to
 * reconfigure {@link ObjectReader}
 * that JAX-RS Resource will use for reading input into Java Objects.
 * Usually this class is accessed from a Servlet or JAX-RS filter
 * before execution reaches resource.
 * 
 * @since 2.3
 */
public class ObjectReaderInjector
{
    protected static final ThreadLocal<ObjectReaderModifier> _threadLocal = new ThreadLocal<ObjectReaderModifier>();

    /**
     * Simple marker used to optimize out {@link ThreadLocal} access in cases
     * where this feature is not being used
     */
    protected static final AtomicBoolean _hasBeenSet = new AtomicBoolean(false);

    private ObjectReaderInjector() { }
    
    public static void set(ObjectReaderModifier mod) {
        _hasBeenSet.set(true);
        _threadLocal.set(mod);
    }

    public static ObjectReaderModifier get() {
        return _hasBeenSet.get() ? _threadLocal.get() : null;
    }
    
    public static ObjectReaderModifier getAndClear() {
        ObjectReaderModifier mod = get();
        if (mod != null) {
            _threadLocal.remove();
        }
        return mod;
    }
}
