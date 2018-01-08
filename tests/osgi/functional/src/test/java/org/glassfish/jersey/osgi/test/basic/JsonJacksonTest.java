/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.osgi.test.util.Helper;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

/**
 * @author Michal Gajdos
 */
public class JsonJacksonTest extends AbstractJsonOsgiIntegrationTest {

    @Configuration
    public static Option[] configuration() {
        final List<Option> options = new ArrayList<>();

        options.addAll(Helper.getCommonOsgiOptions());
        options.addAll(Helper.expandedList(
                // vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),

                mavenBundle().groupId("org.glassfish.jersey.media").artifactId("jersey-media-json-jackson").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.jersey.ext").artifactId("jersey-entity-filtering").versionAsInProject(),

                // jersey-json dependencies
                mavenBundle().groupId("com.fasterxml.jackson.core").artifactId("jackson-core").versionAsInProject(),
                mavenBundle().groupId("com.fasterxml.jackson.core").artifactId("jackson-databind").versionAsInProject(),
                mavenBundle().groupId("com.fasterxml.jackson.core").artifactId("jackson-annotations").versionAsInProject(),
                mavenBundle().groupId("com.fasterxml.jackson.jaxrs").artifactId("jackson-jaxrs-base").versionAsInProject(),
                mavenBundle().groupId("com.fasterxml.jackson.jaxrs").artifactId("jackson-jaxrs-json-provider")
                        .versionAsInProject(),
                mavenBundle().groupId("com.fasterxml.jackson.module").artifactId("jackson-module-jaxb-annotations")
                        .versionAsInProject()
        ));

        return Helper.asArray(options);
    }

    @Override
    protected Feature getJsonProviderFeature() {
        return new JacksonFeature();
    }
}
