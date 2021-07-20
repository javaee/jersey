package org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg;

import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Based on ideas from [Issue#32], this class allows "overriding" of {@link ObjectWriter}
 * that JAX-RS Resource will use; usually this is done from a Servlet or JAX-RS filter
 * before execution reaches resource.
 * 
 * @since 2.3
 */
public class ObjectWriterInjector
{
   protected static final ThreadLocal<ObjectWriterModifier> _threadLocal = new ThreadLocal<ObjectWriterModifier>();

   /**
    * Simple marker used to optimize out {@link ThreadLocal} access in cases
    * where this feature is not being used
    */
   protected static final AtomicBoolean _hasBeenSet = new AtomicBoolean(false);

   private ObjectWriterInjector() { }
   
   public static void set(ObjectWriterModifier mod) {
       _hasBeenSet.set(true);
       _threadLocal.set(mod);
   }

   public static ObjectWriterModifier get() {
       return _hasBeenSet.get() ? _threadLocal.get() : null;
   }
   
   public static ObjectWriterModifier getAndClear() {
       ObjectWriterModifier mod = get();
       if (mod != null) {
           _threadLocal.remove();
       }
       return mod;
   }
}
