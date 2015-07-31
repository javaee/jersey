/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.jersey.server.mvc.freemarker;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.jersey.server.mvc.MvcFeature;

/**
 * {@link Feature} used to add support for {@link MvcFeature MVC} and Freemarker templates.
 * <p/>
 * Note: This feature also registers {@link MvcFeature}.
 *
 * @author Michal Gajdos
 * @author Jeff Wilde (jeff.wilde at complicatedrobot.com)
 */
@ConstrainedTo(RuntimeType.SERVER)
public final class FreemarkerMvcFeature implements Feature {

    private static final String SUFFIX = ".freemarker";

    /**
     * {@link String} property defining the base path to Freemarker templates. If set, the value of the property is added in front
     * of the template name defined in:
     * <ul>
     * <li>{@link org.glassfish.jersey.server.mvc.Viewable Viewable}</li>
     * <li>{@link org.glassfish.jersey.server.mvc.Template Template}, or</li>
     * <li>{@link org.glassfish.jersey.server.mvc.ErrorTemplate ErrorTemplate}</li>
     * </ul>
     * <p/>
     * Value can be absolute providing a full path to a system directory with templates or relative to current
     * {@link javax.servlet.ServletContext servlet context}.
     * <p/>
     * There is no default value.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String TEMPLATE_BASE_PATH = MvcFeature.TEMPLATE_BASE_PATH + SUFFIX;

    /**
     * If {@code true} then enable caching of Freemarker templates to avoid multiple compilation.
     * <p/>
     * The default value is {@code false}.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     *
     * @since 2.5
     */
    public static final String CACHE_TEMPLATES = MvcFeature.CACHE_TEMPLATES + SUFFIX;

    /**
     * Property used to pass user-configured {@link org.glassfish.jersey.server.mvc.freemarker.FreemarkerConfigurationFactory}.
     * <p/>
     * The default value is not set.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     * <p/>
     * This property will also accept an instance of {@link freemarker.template.Configuration Configuration} directly, to
     * support backwards compatibility. If you want to set custom {@link freemarker.template.Configuration configuration} then set
     * {@link freemarker.cache.TemplateLoader template loader} to multi loader of:
     * {@link freemarker.cache.WebappTemplateLoader} (if applicable), {@link freemarker.cache.ClassTemplateLoader} and
     * {@link freemarker.cache.FileTemplateLoader} keep functionality of resolving templates.
     * <p/>
     * If no value is set, a {@link org.glassfish.jersey.server.mvc.freemarker.FreemarkerDefaultConfigurationFactory factory}
     * with the above behaviour is used by default in the
     * {@link org.glassfish.jersey.server.mvc.freemarker.FreemarkerViewProcessor} class.
     * <p/>
     *
     * @since 2.5
     */
    public static final String TEMPLATE_OBJECT_FACTORY = MvcFeature.TEMPLATE_OBJECT_FACTORY + SUFFIX;

    /**
     * Property defines output encoding produced by {@link org.glassfish.jersey.server.mvc.spi.TemplateProcessor}.
     * The value must be a valid encoding defined that can be passed
     * to the {@link java.nio.charset.Charset#forName(String)} method.
     *
     * <p/>
     * The default value is {@code UTF-8}.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     * <p/>
     *
     * @since 2.7
     */
    public static final String ENCODING = MvcFeature.ENCODING + SUFFIX;

    @Override
    public boolean configure(final FeatureContext context) {
        final Configuration config = context.getConfiguration();

        if (!config.isRegistered(FreemarkerViewProcessor.class)) {
            // Template Processor.
            context.register(FreemarkerViewProcessor.class);

            // MvcFeature.
            if (!config.isRegistered(MvcFeature.class)) {
                context.register(MvcFeature.class);
            }

            return true;
        }
        return false;
    }
}
