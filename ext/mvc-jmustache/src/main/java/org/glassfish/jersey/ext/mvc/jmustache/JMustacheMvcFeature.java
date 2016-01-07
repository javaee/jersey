package org.glassfish.jersey.ext.mvc.jmustache;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.jersey.server.mvc.MvcFeature;

/**
 * {@link Feature} used to add support for {@link MvcFeature MVC} and Mustache
 * templates with JMustache Implementation.
 * <p/>
 * Note: This feature also registers {@link MvcFeature}.
 *
 * @author Anshul Sao <anshul.sao at capillarytech.com>
 */
@ConstrainedTo(RuntimeType.SERVER)
public class JMustacheMvcFeature implements Feature {

    private static final String SUFFIX = ".jmustache";

    /**
     * {@link String} property defining the base path to Mustache templates. If
     * set, the value of the property is added in front of the template name
     * defined in:
     * <ul>
     * <li>{@link org.glassfish.jersey.server.mvc.Viewable Viewable}</li>
     * <li>{@link org.glassfish.jersey.server.mvc.Template Template}, or</li>
     * <li>{@link org.glassfish.jersey.server.mvc.ErrorTemplate ErrorTemplate}</li>
     * </ul>
     * <p/>
     * Value can be absolute providing a full path to a system directory with
     * templates or relative to current
     * {@link javax.servlet.ServletContext servlet context}.
     * <p/>
     * There is no default value.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String TEMPLATE_BASE_PATH =
        MvcFeature.TEMPLATE_BASE_PATH + SUFFIX;

    /**
     * If {@code true} then enable caching of Mustache templates to avoid
     * multiple compilation.
     * <p/>
     * The default value is {@code false}.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     *
     */
    public static final String CACHE_TEMPLATES =
        MvcFeature.CACHE_TEMPLATES + SUFFIX;

    /**
     * Property used to pass user-configured
     * {@link com.github.mustachejava.MustacheFactory factory} able to create
     * {@link com.github.mustachejava.Mustache Mustache templates}.
     * <p/>
     * The default value is not set.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.     
     */
    public static final String TEMPLATE_OBJECT_FACTORY =
        MvcFeature.TEMPLATE_OBJECT_FACTORY + SUFFIX;

    /**
     * Property defines output encoding produced by
     * {@link org.glassfish.jersey.server.mvc.spi.TemplateProcessor}. The value
     * must be a valid encoding defined that can be passed to the
     * {@link java.nio.charset.Charset#forName(String)} method.
     *
     * <p/>
     * The default value is {@code UTF-8}.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     * <p/>
     */
    public static final String ENCODING = MvcFeature.ENCODING + SUFFIX;

    @Override
    public boolean configure(final FeatureContext context) {
        final Configuration config = context.getConfiguration();

        if (!config.isRegistered(JMustacheTemplateProcessor.class)) {
            // Template Processor.
            context.register(JMustacheTemplateProcessor.class);

            // MvcFeature.
            if (!config.isRegistered(MvcFeature.class)) {
                context.register(MvcFeature.class);
            }

            return true;
        }
        return false;
    }
}
