/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.wadl.config;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.ProcessingException;

import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.wadl.WadlGenerator;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * Provides a configured {@link org.glassfish.jersey.server.wadl.WadlGenerator} with all decorations (the default
 * wadl generator decorated by other generators).
 * <h3>Creating a WadlGeneratorConfig</strong><h3/>
 * <p>
 * If you want to create an instance at runtime you can configure the
 * WadlGenerator class and property names/values. A new instance of the
 * Generator is created for each generation action.
 *
 * The first option would look like this:
 * <pre>
 *  WadlGeneratorConfig config = WadlGeneratorConfig
 *     .generator( MyWadlGenerator.class )
 *     .prop( "someProperty", "someValue" )
 *     .generator( MyWadlGenerator2.class )
 *     .prop( "someProperty", "someValue" )
 *     .prop( "anotherProperty", "anotherValue" )
 *     .build();
 * </pre>
 * </p>
 * <p>
 * If you want to specify the {@link WadlGeneratorConfig} in the web.xml you have
 * to subclass it and set the servlet init-param {@link org.glassfish.jersey.server.ServerProperties#WADL_GENERATOR_CONFIG}
 * to the name of your subclass. This class might look like this:
 * <pre>
 *  class MyWadlGeneratorConfig extends WadlGeneratorConfig {
 *
 *      public List&lt;WadlGeneratorDescription&gt; configure() {
 *          return generator( MyWadlGenerator.class )
 *                     .prop( "foo", propValue )
 *                     .generator( MyWadlGenerator2.class )
 *                     .prop( "bar", propValue2 )
 *                     .descriptions();
 *      }
 *
 * }
 * </pre>
 * </p>
 *
 * <h3>Configuring the WadlGenerator<h3/>
 * <p>
 * The {@link org.glassfish.jersey.server.wadl.WadlGenerator} properties will be populated with the provided properties like this:
 * <ul>
 * <li>The types match exactly:<br/>
 * if the WadlGenerator property is of type {@code org.example.Foo} and the
 * provided property value is of type {@code org.example.Foo}</li>
 * <li>Types that provide a constructor for the provided type (mostly java.lang.String)</li>
 * <li>java.io.InputStream: The {@link InputStream} can e.g. represent a file. The stream is loaded from the
 * property value (provided by the {@link WadlGeneratorDescription}) via
 * {@link ClassLoader#getResourceAsStream(String)}. It will be closed after {@link org.glassfish.jersey.server.wadl.WadlGenerator#init()} was called.
 * </li>
 *
 * <li><strong>Deprecated, will be removed in future versions:</strong><br/>
 * The WadlGenerator property is of type {@link File} and the provided property value is a {@link String}:<br/>
 * the provided property value can contain the prefix <em>classpath:</em> to denote, that the
 * path to the file is relative to the classpath. In this case, the property value is stripped by
 * the prefix <em>classpath:</em> and the {@link File} is created via
 * <pre>
 *  new File( generator.getClass().getResource( strippedFilename ).toURI() )
 * </pre>
 * Notice that the filename is loaded from the classpath in this case, e.g. <em>classpath:test.xml</em>
 * refers to a file in the package of the class ({@link WadlGeneratorDescription#getGeneratorClass()}). The
 * file reference <em>classpath:/test.xml</em> refers to a file that is in the root of the classpath.
 * </li>
 * </ul>
 * </p>
 * <h3>Existing {@link org.glassfish.jersey.server.wadl.WadlGenerator} implementations:</h3>
 * <p>
 * <ul>
 * <li>{@link org.glassfish.jersey.server.wadl.internal.generators.WadlGeneratorApplicationDoc}</li>
 * <li>{@link org.glassfish.jersey.server.wadl.internal.generators.WadlGeneratorGrammarsSupport}</li>
 * <li>{@link org.glassfish.jersey.server.wadl.internal.generators.WadlGeneratorJAXBGrammarGenerator}</li>
 * <li>{@link org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.WadlGeneratorResourceDocSupport}</li>
 * </ul>
 * </p>
 * <p>
 * A common example for a {@link WadlGeneratorConfig} would be this:
 * <pre>
 *  class MyWadlGeneratorConfig extends WadlGeneratorConfig {
 *
 *      public List&lt;WadlGeneratorDescription&gt; configure() {
 *          return generator( WadlGeneratorApplicationDoc.class )
 *              .prop( "applicationDocsStream", "application-doc.xml" )
 *              .generator( WadlGeneratorGrammarsSupport.class )
 *              .prop( "grammarsStream", "application-grammars.xml" )
 *              .generator( WadlGeneratorResourceDocSupport.class )
 *              .prop( "resourceDocStream", "resourcedoc.xml" )
 *              .descriptions();
 *              .descriptions();
 *      }
 *
 *  }
 * </pre>
 * </p>
 *
 * @author Martin Grotzke (martin.grotzke at freiheit.com)
 */
public abstract class WadlGeneratorConfig {

    public WadlGeneratorConfig() {
    }

    public abstract List configure();

    /**
     * Create a new instance of {@link org.glassfish.jersey.server.wadl.WadlGenerator}, based on the {@link WadlGeneratorDescription}s
     * provided by {@link #configure()}.
     *
     * @return the initialized {@link org.glassfish.jersey.server.wadl.WadlGenerator}
     */
    public WadlGenerator createWadlGenerator(final ServiceLocator locator) {
        final WadlGenerator wadlGenerator;
        final List<WadlGeneratorDescription> wadlGeneratorDescriptions;
        try {
            wadlGeneratorDescriptions = configure();
        } catch (final Exception e) {
            throw new ProcessingException(LocalizationMessages.ERROR_WADL_GENERATOR_CONFIGURE(), e);
        }
        for (final WadlGeneratorDescription desc : wadlGeneratorDescriptions) {
            desc.setConfiguratorClass(this.getClass());
        }
        try {
            wadlGenerator = WadlGeneratorLoader.loadWadlGeneratorDescriptions(locator, wadlGeneratorDescriptions);
        } catch (final Exception e) {
            throw new ProcessingException(LocalizationMessages.ERROR_WADL_GENERATOR_LOAD(), e);

        }
        return wadlGenerator;
    }

    /**
     * Start to build an instance of {@link WadlGeneratorConfig}:
     * <pre><code>generator(&lt;class&gt;)
     *      .prop(&lt;name&gt;, &lt;value&gt;)
     *      .prop(&lt;name&gt;, &lt;value&gt;)
     * .generator(&lt;class&gt;)
     *      .prop(&lt;name&gt;, &lt;value&gt;)
     *      .prop(&lt;name&gt;, &lt;value&gt;)
     *      .build()</code></pre>
     *
     * @param generatorClass the class of the wadl generator to configure
     * @return an instance of {@link WadlGeneratorConfigDescriptionBuilder}.
     */
    public static WadlGeneratorConfigDescriptionBuilder generator(final Class<? extends WadlGenerator> generatorClass) {
        return new WadlGeneratorConfigDescriptionBuilder().generator(generatorClass);
    }

    public static class WadlGeneratorConfigDescriptionBuilder {

        private List<WadlGeneratorDescription> _descriptions;
        private WadlGeneratorDescription _description;

        public WadlGeneratorConfigDescriptionBuilder() {
            _descriptions = new ArrayList<>();
        }

        public WadlGeneratorConfigDescriptionBuilder generator(final Class<? extends WadlGenerator> generatorClass) {
            if (_description != null) {
                _descriptions.add(_description);
            }
            _description = new WadlGeneratorDescription();
            _description.setGeneratorClass(generatorClass);
            return this;
        }

        /**
         * Specify the property value for the current {@link WadlGenerator}.
         * <p>
         * The {@link WadlGenerator} property type can be of any type with
         * the following contraints:
         * <p>
         * If the {@link WadlGenerator} property type is equal to the
         * property value type then the property value is set as the
         * {@link WadlGenerator} property value.
         * </p>
         * <p>
         * If the {@link WadlGenerator} property type has a constructor with
         * a single {@link String} parameter type and the property value is a
         * {@link String} then an instance of the property type is constructed
         * with the property value and that instance is set as the
         * {@link WadlGenerator} property value.
         * </p>
         * <p>
         * If the {@link WadlGenerator} property type is of type {@link File},
         * then the specified property value must be a {@link String} starting
         * with the prefix <em>classpath:</em> to denote, that the File shall
         * be loaded from the classpath like this:
         * <pre><code>new File( generator.getClass().getResource( strippedFilename ).toURI() )</code></pre>
         * Notice that the file is loaded as a resource from the classpath
         * in this case, therefore <em>classpath:test.xml</em>
         * refers to a file in the package of the specified
         * <code>&lt;classname&gt;</code>. The file reference
         * <em>classpath:/test.xml</em> refers to a file that is in the root
         * of the classpath.
         * </p>
         * <p>
         * If the {@link WadlGenerator} property type is of type
         * {@link InputStream}, then the specified property value must be a
         * {@link String} and the instance of {@link InputStream} is obtained
         * with {@link ClassLoader#getResourceAsStream(String)} using the
         * current threads context classloader.
         * The {@link InputStream} will be closed after
         * {@link WadlGenerator#init()} was called and therefore must not be
         * closed by the {@link WadlGenerator} using this stream.
         * </p>
         *
         * @param propName  the property name
         * @param propValue the stringified property value
         * @return this builder instance
         */
        public WadlGeneratorConfigDescriptionBuilder prop(final String propName, final Object propValue) {
            if (_description.getProperties() == null) {
                _description.setProperties(new Properties());
            }
            _description.getProperties().put(propName, propValue);
            return this;
        }

        public List<WadlGeneratorDescription> descriptions() {
            if (_description != null) {
                _descriptions.add(_description);
            }
            return _descriptions;
        }

        public WadlGeneratorConfig build() {
            if (_description != null) {
                _descriptions.add(_description);
            }
            return new WadlGeneratorConfigImpl(_descriptions);
        }

    }

    static class WadlGeneratorConfigImpl extends WadlGeneratorConfig {

        public List<WadlGeneratorDescription> _descriptions;

        public WadlGeneratorConfigImpl(
                final List<WadlGeneratorDescription> descriptions) {
            _descriptions = descriptions;
        }

        @Override
        public List<WadlGeneratorDescription> configure() {
            return _descriptions;
        }

    }

//    public static class WadlGeneratorConfigBuilder {
//
//        private List<WadlGenerator> _generators;
//
//        public WadlGeneratorConfigBuilder() {
//            _generators = new ArrayList<WadlGenerator>();
//        }
//
//        public WadlGeneratorConfigBuilder generator( WadlGenerator generator ) {
//            if ( generator == null ) {
//                throw new IllegalArgumentException( "The wadl generator must not be null." );
//            }
//            _generators.add( generator );
//            return this;
//        }
//
//        public WadlGeneratorConfig build() {
//            try {
//                final WadlGenerator wadlGenerator = WadlGeneratorLoader.loadWadlGenerators( _generators );
//                return new WadlGeneratorConfigGeneratorImpl( wadlGenerator ) {
//
//                };
//            } catch ( Exception e ) {
//                throw new RuntimeException( "Could not load wadl generators.", e );
//            }
//        }
//
//    }

//    static class WadlGeneratorConfigGeneratorImpl extends WadlGeneratorConfig {
//
//        private final WadlGenerator _wadlGenerator;
//
//        public WadlGeneratorConfigGeneratorImpl(WadlGenerator wadlGenerator) {
//            _wadlGenerator = wadlGenerator;
//        }
//
//        @Override
//        public List<WadlGeneratorDescription> configure() {
//            throw new UnsupportedOperationException( "Must not be called" );
//        }
//
//        /* (non-Javadoc)
//         * @see com.sun.jersey.server.impl.wadl.config.WadlGeneratorConfig#createWadlGenerator()
//         */
//        @Override
//        public synchronized WadlGenerator createWadlGenerator() {
//            return _wadlGenerator;
//        }
//
//    }

}
