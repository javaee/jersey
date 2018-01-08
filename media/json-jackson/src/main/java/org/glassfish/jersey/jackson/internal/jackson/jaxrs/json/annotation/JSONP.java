package org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* Note: applicable to annotations to allow bundling (if support added
 * to JAX-RS bundle itself), as well as methods to indicate that return
 * type is to be wrapped.
 * Other types are not allowed, since there is no current usage for those;
 * input can't be wrapped (so no need for parameters); fields are not
 * exposed through JAX-RS; and we do not allow 'default wrapping' for
 * types.
 *<p>
 * Note on properties: if either {@link #prefix()} or {@link #suffix()}
 * is non-empty, they are used as literal prefix and suffix to use.
 * Otherwise {@link #value()} is used as the function name, followed
 * by opening parenthesis, value, and closing parenthesis.
 *<p>
 * Example usage:
 *<pre>
 *  class Wrapper {
 *     @JSONP("myFunc") public int value = 3;
 *  }
 *</pre>
 *  would serialize as:
 *<pre>
 *  myFunc({"value":3})
 *<pre>
 *  whereas
 *</pre>
 *<pre>
 *  class Wrapper {
 *     @JSONP(prefix="call(", suffix=")+3") public int value = 1;
 *  }
 *</pre>
 *  would serialize as:
 *<pre>
 *  call({"value":1})+3
 *<pre>
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@com.fasterxml.jackson.annotation.JacksonAnnotation
public @interface JSONP
{
    /**
     * Method used for JSONP, unless {@link #prefix()} or
     * {@link #suffix()} return non-empty Strings.
     */
    public String value() default "";
    
    /**
     * Prefix String used for JSONP if not empty: will be included
     * verbatim before JSON value.
     */
    public String prefix() default "";

    /**
     * Suffix String used for JSONP if not empty: will be included
     * verbatim after JSON value.
     */
    public String suffix() default "";

    /**
     * Helper class for encapsulating information from {@link JSONP}
     * annotation instance.
     */
    public static class Def {
        public final String method;
        public final String prefix;
        public final String suffix;

        public Def(String m) {
            method = m;
            prefix = null;
            suffix = null;
        }
        
        public Def(JSONP json) {
            method = emptyAsNull(json.value());
            prefix = emptyAsNull(json.prefix());
            suffix = emptyAsNull(json.suffix());
        }

        private final static String emptyAsNull(String str) {
            if (str == null || str.length() == 0) {
                return null;
            }
            return str;
        }
    }
}
