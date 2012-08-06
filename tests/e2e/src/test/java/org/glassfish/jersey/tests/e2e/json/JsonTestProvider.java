/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.json;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Feature;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.jackson.JacksonBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.jettison.JettisonBinder;
import org.glassfish.jersey.jettison.JettisonConfiguration;
import org.glassfish.jersey.jettison.JettisonFeature;
import org.glassfish.jersey.moxy.json.MoxyJsonBinder;
import org.glassfish.jersey.moxy.json.MoxyJsonConfiguration;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;

import org.glassfish.hk2.utilities.Binder;

/**
 * Common class for JSON providers that should be used for testing JSON capabilities.
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public abstract class JsonTestProvider {

    public static final Collection<JsonTestProvider> JAXB_PROVIDERS = new LinkedHashSet<JsonTestProvider>() {{
        add(new JacksonJsonTestProvider());
        add(new JettisonMappedJsonTestProvider());
        add(new JettisonBadgerfishJsonTestProvider());
        add(new MoxyJsonTestProvider());
    }};

    //  TODO add MoxyJsonTestProvider once MOXy supports POJO
    public static final Collection<JsonTestProvider> POJO_PROVIDERS = new LinkedHashSet<JsonTestProvider>() {{
        add(new JacksonJsonTestProvider());
    }};

    public static class JacksonJsonTestProvider extends JsonTestProvider {

        public JacksonJsonTestProvider() {
            setBinder(new JacksonBinder());
            setFeature(new JacksonFeature());
        }

    }

    public static class JettisonMappedJsonTestProvider extends JsonTestProvider {

        public JettisonMappedJsonTestProvider() {
            final JettisonConfiguration jsonConfiguration = JettisonConfiguration.mappedJettison().xml2JsonNs(new HashMap<String,
                    String>() {{
                put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
                put("http://example.com", "example");
                put("http://test.jaxb.com", "jaxb");
            }}).build();

            setBinder(new JettisonBinder());
            setFeature(new JettisonFeature());
            setConfiguration(jsonConfiguration);
        }

    }

    public static class JettisonBadgerfishJsonTestProvider extends JsonTestProvider {

        public JettisonBadgerfishJsonTestProvider() {
            setBinder(new JettisonBinder());
            setFeature(new JettisonFeature());

            setConfiguration(JettisonConfiguration.badgerFish().build());
        }

    }

    public static class MoxyJsonTestProvider extends JsonTestProvider {

        public MoxyJsonTestProvider() {
            setBinder(new MoxyJsonBinder());
            setFeature(new MoxyJsonFeature());
            getProviders().add(new MoxyJsonConfigurationContextResolver());
        }

    }

    private Binder binder;
    private Feature feature;
    private JettisonConfiguration configuration;
    private Set<Object> providers = new LinkedHashSet<Object>();

    public JettisonConfiguration getConfiguration() {
        return configuration;
    }

    protected void setConfiguration(final JettisonConfiguration configuration) {
        this.configuration = configuration;
    }

    public Feature getFeature() {
        return feature;
    }

    protected void setFeature(final Feature feature) {
        this.feature = feature;
    }

    public Binder getBinder() {
        return binder;
    }

    protected void setBinder(final Binder binder) {
        this.binder = binder;
    }

    public Set<Object> getProviders() {
        return providers;
    }

    @Provider
    protected final static class MoxyJsonConfigurationContextResolver implements ContextResolver<MoxyJsonConfiguration> {

        @Override
        public MoxyJsonConfiguration getContext(Class<?> objectType) {
            final MoxyJsonConfiguration configuration = new MoxyJsonConfiguration();

            Map<String, String> namespacePrefixMapper = new HashMap<String, String>(1);
            namespacePrefixMapper.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
            namespacePrefixMapper.put("http://example.com", "example");
            namespacePrefixMapper.put("http://test.jaxb.com", "jaxb");

            configuration.setNamespacePrefixMapper(namespacePrefixMapper);
            configuration.setNamespaceSeparator(':');

            return configuration;
        }
    }

}
