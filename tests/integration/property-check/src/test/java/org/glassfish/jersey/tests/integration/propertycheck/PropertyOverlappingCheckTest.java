/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.integration.propertycheck;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.internal.util.PropertiesClass;
import org.glassfish.jersey.internal.util.Property;
import org.glassfish.jersey.internal.util.PropertyAlias;
import org.glassfish.jersey.jetty.connector.JettyClientProperties;
import org.glassfish.jersey.media.multipart.MultiPartProperties;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.message.MessageProperties;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.internal.InternalServerProperties;
import org.glassfish.jersey.server.oauth1.OAuth1ServerProperties;
import org.glassfish.jersey.servlet.ServletProperties;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * Test, that there are no properties with overlapping names in known Jersey {@code *Properties} and {@code *Feature}
 * classes.
 * <p>
 * For technical reasons, we do not want the property names to <i>overlap</i>.
 * In other words, no property should contain a <i>namespace prefix</i>, that is already used as a concrete property name,
 * such as {@code a.b} and {@code a.b.c}.
 * </p>
 * <p>
 * Additionally, the test also reports all the duplicates property names found throughout the checked files.
 * </p>
 * <p>
 * Note that the list of files is hardcoded directly in this test in a static array
 * (to avoid the necessity of writing custom class loader for this test).
 * If a java class containing properties should by included in the check, it has to be added here.
 * Also note that the test is relying on {@link Property}, {@link PropertiesClass} and {@link PropertyAlias} annotations
 * to recognize individual properties.
 * </p>
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class PropertyOverlappingCheckTest {

    private static final Logger log = Logger.getLogger(PropertyOverlappingCheckTest.class.getName());

    private static final Class<?>[] classes = new Class[] {
            JettyClientProperties.class,
            ApacheClientProperties.class,
            OAuth1ServerProperties.class,
            ServletProperties.class,
            CommonProperties.class,
            MessageProperties.class,
            ServerProperties.class,
            InternalServerProperties.class,
            ClientProperties.class,
            MultiPartProperties.class,
            TestProperties.class,
            SseFeature.class
    };

    private static class ProblemReport {

        private final String parentProperty;
        private final String classNameParent;
        private final String childProperty;
        private final String classNameChild;
        private final boolean duplicate;

        private ProblemReport(String parentProperty,
                              String classNameParent,
                              String childProperty,
                              String classNameChild,
                              boolean duplicate) {
            this.parentProperty = parentProperty;
            this.classNameParent = classNameParent;
            this.childProperty = childProperty;
            this.classNameChild = classNameChild;
            this.duplicate = duplicate;
        }

        private ProblemReport(String parentProperty, String classNameParent, String childProperty, String classNameChild) {
            this(parentProperty, classNameParent, childProperty, classNameChild, false);
        }

        public String getParentProperty() {
            return parentProperty;
        }

        public String getClassNameParent() {
            return classNameParent;
        }

        public String getChildProperty() {
            return childProperty;
        }

        public String getClassNameChild() {
            return classNameChild;
        }

        public boolean isDuplicate() {
            return duplicate;
        }
    }

    @Test
    public void test() throws IllegalAccessException {
        List<String> allPropertyNames = new ArrayList<>();
        Map<String, String> propertyToClassMap = new HashMap<>();
        List<ProblemReport> problems = new ArrayList<>();

        // iterate over all the string fields of above declared classes
        for (Class<?> clazz : classes) {
            final boolean checkFieldPropertyAnnotation = clazz.getAnnotation(PropertiesClass.class) == null;
            Field[] fields = clazz.getFields();
            for (Field field : fields) {
                if (checkFieldPropertyAnnotation && field.getAnnotation(Property.class) == null) {
                    // skip fields not annotated with @Property in classes not annotated with @PropertiesClass
                    continue;
                }
                if (field.getAnnotation(PropertyAlias.class) != null) {
                    // skip property aliases
                    continue;
                }
                if (field.getType().isAssignableFrom(String.class)) {
                    String propertyValue = (String) field.get(null);
                    allPropertyNames.add(propertyValue);
                    // check if there is already such property in the map; report a problem if true or store the
                    // property-to-class relationship into the map for later use
                    String propertyMapEntry = propertyToClassMap.get(propertyValue);
                    if (propertyToClassMap.get(propertyValue) != null) {
                        //                        log.info("Duplicate property found: " + propertyValue + " in " +
                        // propertyMapEntry + " and "
                        //                                + clazz.getName() + ". Test won't fail because of this, as the check
                        // is currently disabled.");
                        // this cannot cause the test to fail, as there are aliases in ClientProperties and ServerProperties,
                        // which are by definition equal to those defined in CommonProperties
                        problems.add(new ProblemReport(propertyValue, propertyMapEntry, propertyValue, clazz.getName(), true));
                    } else {
                        propertyToClassMap.put(propertyValue, clazz.getName());
                    }
                }
            }
        }
        // sort the properties by name (natural), so that if two properties have overlapping names,
        // they will appear one after another
        Collections.sort(allPropertyNames);

        String previousProperty = "";
        for (String property : allPropertyNames) {
            // is the property overlapping with the previous one?
            // do not consider overlapping such as foo.bar vs foo.barbar, just foo.bar vs foo.bar.bar
            if (property.startsWith(previousProperty + ".")) {
                problems.add(new ProblemReport(previousProperty, propertyToClassMap.get(previousProperty),
                        property, propertyToClassMap.get(property)));
            } else {
                // the "pointer" is moved only if there was no overlapping detected in this iteration
                // as this would potentially hide the 2nd (or n-th) property overlapping with the same one
                previousProperty = property;
            }
        }

        if (!problems.isEmpty()) {
            log.severe("Property naming problems detected: ");
            for (ProblemReport problem : problems) {
                if (problem.isDuplicate()) {
                    log.severe("Duplicate property name: \n  property: " + problem.getParentProperty()
                            + "\n  class1: " + problem.getClassNameParent()
                            + "\n  class2: " + problem.getClassNameChild() + "\n");
                } else {
                    log.severe("Overlapping property names: \n  property1: "
                            + problem.getParentProperty() + "\n  in: " + problem.getClassNameParent()
                            + "\n  property2: "
                            + problem.getChildProperty() + "\n  in " + problem.getClassNameChild() + "\n");
                }
            }
        }
        // fail if problems detected
        assertTrue(problems.isEmpty());
    }
}
