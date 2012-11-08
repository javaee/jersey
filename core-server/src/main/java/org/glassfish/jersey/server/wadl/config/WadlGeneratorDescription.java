/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
import java.util.Properties;

import org.glassfish.jersey.server.wadl.WadlGenerator;

/**
 * This is the model for the definition of wadl generators via configuration properties.<br />
 * The properties refer to the properties of the {@link WadlGenerator} implementation with
 * the specified {@link WadlGeneratorDescription#getGeneratorClass()}. The {@link WadlGenerator} properties
 * are populated with the provided properties like this:
 * <ul>
 * <li>The types match exactly:<br/>if the WadlGenerator property is of type <code>org.example.Foo</code> and the
 * provided property value is of type <code>org.example.Foo</code></li>
 * <li>Types that provide a constructor for the provided type (mostly java.lang.String)</li>
 * <li>The WadlGenerator property is of type {@link InputStream}: The stream is loaded from the
 * property value (provided by the {@link WadlGeneratorDescription}) via
 * {@link ClassLoader#getResourceAsStream(String)}. It will be closed after {@link WadlGenerator#init()} was called.
 * </li>
 *
 * <li><strong>Deprecated, will be removed in future versions:</strong><br/>
 * The WadlGenerator property is of type {@link File} and the provided property value is a {@link String}:<br/>
 * the provided property value can contain the prefix <em>classpath:</em> to denote, that the
 * path to the file is relative to the classpath. In this case, the property value is stripped by
 * the prefix <em>classpath:</em> and the {@link File} is created via
 * <pre><code>new File( generator.getClass().getResource( strippedFilename ).toURI() )</code></pre>
 * Notice that the filename is loaded from the classpath in this case, e.g. <em>classpath:test.xml</em>
 * refers to a file in the package of the class ({@link WadlGeneratorDescription#getGeneratorClass()}). The
 * file reference <em>classpath:/test.xml</em> refers to a file that is in the root of the classpath.
 * </li>
 *
 * </ul>
 *
 * @author Martin Grotzke (martin.grotzke at freiheit.com)
 */
public class WadlGeneratorDescription {

    private Class<? extends WadlGenerator> generatorClass;
    private Properties properties;
    private Class<?> configuratorClass;

    public WadlGeneratorDescription() {
    }

    public WadlGeneratorDescription(Class<? extends WadlGenerator> generatorClass, Properties properties) {
        this.generatorClass = generatorClass;
        this.properties = properties;
    }

    /**
     * @return the generatorClass
     */
    public Class<? extends WadlGenerator> getGeneratorClass() {
        return generatorClass;
    }
    /**
     * @param generatorClass the generatorClass to set
     */
    public void setGeneratorClass(Class<? extends WadlGenerator> generatorClass) {
        this.generatorClass = generatorClass;
    }
    /**
     * @return the properties
     */
    public Properties getProperties() {
        return properties;
    }
    /**
     * @param properties the properties to set
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * Return {@link WadlGeneratorConfig} that was used to produce current description instance.
     * The result could be null if the config was not set on this instance.
     *
     * @return config
     */
    public Class<?> getConfiguratorClass() {
        return configuratorClass;
    }

    /**
     * Set {@link WadlGeneratorConfig} class to be associated with current instance.
     *
     * @param configuratorClass
     */
    void setConfiguratorClass(Class<?> configuratorClass) {
        this.configuratorClass = configuratorClass;
    }
}
