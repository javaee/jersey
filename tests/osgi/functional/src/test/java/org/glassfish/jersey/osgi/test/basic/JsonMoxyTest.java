/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.osgi.test.basic;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.jersey.moxy.json.MoxyJsonConfig;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.osgi.test.util.Helper;

import org.eclipse.persistence.jaxb.BeanValidationMode;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackage;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

/**
 * @author Michal Gajdos
 */
public class JsonMoxyTest extends AbstractJsonOsgiIntegrationTest {

    @Configuration
    public static Option[] configuration() {
        final List<Option> options = new ArrayList<>();

        options.addAll(Helper.getCommonOsgiOptions());
        options.addAll(Helper.expandedList(
                // vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),

                bootDelegationPackage("javax.xml.bind"),
                bootDelegationPackage("javax.xml.bind.*"),
                // validation
                bootDelegationPackage("javax.xml.parsers"),
                bootDelegationPackage("javax.xml.parsers.*"),

                // moxy dependencies
                mavenBundle().groupId("org.glassfish.jersey.media").artifactId("jersey-media-moxy").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.jersey.ext").artifactId("jersey-entity-filtering").versionAsInProject(),
                mavenBundle().groupId("org.eclipse.persistence").artifactId("org.eclipse.persistence.moxy").versionAsInProject(),
                mavenBundle().groupId("org.eclipse.persistence").artifactId("org.eclipse.persistence.core").versionAsInProject(),
                mavenBundle().groupId("org.eclipse.persistence").artifactId("org.eclipse.persistence.asm").versionAsInProject(),
                mavenBundle().groupId("org.glassfish").artifactId("javax.json").versionAsInProject(),

                // validation
                mavenBundle().groupId("org.hibernate").artifactId("hibernate-validator").versionAsInProject(),
                mavenBundle().groupId("org.jboss.logging").artifactId("jboss-logging").versionAsInProject(),
                mavenBundle().groupId("com.fasterxml").artifactId("classmate").versionAsInProject(),
                mavenBundle().groupId("javax.el").artifactId("javax.el-api").versionAsInProject()
        ));

        return Helper.asArray(options);
    }

    @Override
    protected Feature getJsonProviderFeature() {
        // Turn off BV otherwise the test is not stable.
        return new Feature() {

            @Override
            public boolean configure(final FeatureContext context) {
                context.register(new MoxyJsonConfig()
                        .property(MarshallerProperties.BEAN_VALIDATION_MODE, BeanValidationMode.NONE)
                        .resolver());

                return true;
            }
        };
    }
}
