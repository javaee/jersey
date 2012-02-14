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
package org.glassfish.jersey.media.json.internal;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;
import junit.framework.TestCase;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import org.glassfish.jersey.media.json.JsonConfiguration;
import org.glassfish.jersey.media.json.JsonJaxbContext;
import org.glassfish.jersey.media.json.JsonMarshaller;
import org.glassfish.jersey.media.json.JsonUnmarshaller;

/**
 *
 * @author Jakub Podlesak
 */
public class JSONJAXBRoudtripTest extends TestCase {

    private static final String PKG_NAME = "org/glassfish/jersey/media/json/internal/";
    Collection<Object> beans;
    Class[] classes;

    @Override
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        String beanClasses = JSONTestHelper.getResourceAsString(PKG_NAME, "jaxb.index");
        Collection classCollection = new LinkedList<Class>();
        StringTokenizer tokenizer = new StringTokenizer(beanClasses);
        //StringTokenizer tokenizer = new StringTokenizer("SimpleBeanWithAttributes");//beanClasses);
        beans = new LinkedList<Object>();
        while (tokenizer.hasMoreTokens()) {
            String className = tokenizer.nextToken();
            if (!"".equals(className)) {
                Class beanClass = Class.forName(PKG_NAME.replace('/', '.') + className);
                classCollection.add(beanClass);
                Method testBeanCreator = beanClass.getDeclaredMethod("createTestInstance");
                Object testBean = testBeanCreator.invoke(null);
                beans.add(testBean);
            }
        }
        classes = (Class[]) classCollection.toArray(new Class[0]);
    }

    public void testDefaultConfig() throws Exception {
        System.out.println("DEFAULT CONFIG");
        allBeansTest(new JsonJaxbContext(classes), beans);
    }

    public void testInternalNotation() throws Exception {
        System.out.println("INTERNAL NOTATION");
        allBeansTest(new JsonJaxbContext(JsonConfiguration.mapped().rootUnwrapping(false).build(), classes), beans);
    }

    public void testInternalNotationDeprecatedConfig() throws Exception {
        System.out.println("INTERNAL NOTATION DEPRECATED CONFIG");
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(JsonJaxbContext.JSON_NOTATION, JsonJaxbContext.JSONNotation.MAPPED);
        props.put(JsonJaxbContext.JSON_ROOT_UNWRAPPING, Boolean.FALSE);
        allBeansTest(new JsonJaxbContext(classes, props), beans);
    }

    public void testInternalNotationAttrAsElems() throws Exception {
        System.out.println("INTERNAL NOTATION WITH SOME ATTR AS ELEMS");
        allBeansTest(new JsonJaxbContext(JsonConfiguration.mapped().rootUnwrapping(true).attributeAsElement("i", "j").build(), classes), beans);
    }

    public void testInternalNotationAttrAsElemsDeprecatedConfig() throws Exception {
        System.out.println("INTERNAL NOTATION WITH SOME ATTR AS ELEMS DEPRECATED CONFIG");
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(JsonJaxbContext.JSON_NOTATION, JsonJaxbContext.JSONNotation.MAPPED);
        props.put(JsonJaxbContext.JSON_ROOT_UNWRAPPING, Boolean.TRUE);
        props.put(JsonJaxbContext.JSON_ATTRS_AS_ELEMS, new HashSet<String>(2) {

            {
                add("i");
                add("j");
            }
        });
        allBeansTest(new JsonJaxbContext(classes, props), beans);
    }

    public void testJettisonBadgerfishNotation() throws Exception {
        System.out.println("BADGERFISH NOTATION");
        allBeansTest(new JsonJaxbContext(JsonConfiguration.badgerFish().build(), classes), beans);
    }

    public void testJettisonBadgerfishNotationDeprecatedConfig() throws Exception {
        System.out.println("BADGERFISH NOTATION DEPRECATED CONFIG");
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(JsonJaxbContext.JSON_NOTATION, JsonJaxbContext.JSONNotation.BADGERFISH);
        allBeansTest(new JsonJaxbContext(classes, props), beans);
    }

    public void testNaturalNotation() throws Exception {
        System.out.println("NATURAL NOTATION");
        allBeansTest(new JsonJaxbContext(JsonConfiguration.natural().build(), classes), beans);
    }

    public void testNaturalNotationFormatted() throws Exception {
        System.out.println("NATURAL NOTATION FORMATTED");
        allBeansTest(new JsonJaxbContext(JsonConfiguration.natural().humanReadableFormatting(true).build(), classes), beans);
    }

    public void testNaturalNotationDeprecatedConfig() throws Exception {
        System.out.println("NATURAL NOTATION DEPRECATED CONFIG");
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(JsonJaxbContext.JSON_NOTATION, JsonJaxbContext.JSONNotation.NATURAL);
        allBeansTest(new JsonJaxbContext(classes, props), beans);
    }

    public void testJettisonMappedNotation() throws Exception {
        System.out.println("MAPPED (JETTISON) NOTATION");
        Map<String, Object> props = new HashMap<String, Object>();
        allBeansTest(new JsonJaxbContext(JsonConfiguration.mappedJettison().build(), classes), beans);
    }

    public void testJettisonMappedNotationDeprecatedConfig() throws Exception {
        System.out.println("MAPPED (JETTISON) NOTATION DEPRECATED CONFIG");
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(JsonJaxbContext.JSON_NOTATION, "MAPPED_JETTISON");
        allBeansTest(new JsonJaxbContext(classes, props), beans);
    }

    public synchronized void allBeansTest(JsonJaxbContext context, Collection<Object> beans) throws Exception {

        JsonMarshaller marshaller = context.createJSONMarshaller();
        JsonUnmarshaller unmarshaller = context.createJSONUnmarshaller();

        for (Object originalBean : beans) {
            printAsXml(originalBean);

            StringWriter sWriter = new StringWriter();
            marshaller.marshallToJSON(originalBean, sWriter);

            System.out.println(sWriter.toString());
            assertEquals(originalBean, unmarshaller.unmarshalFromJSON(new StringReader(sWriter.toString()), originalBean.getClass()));
        }
    }

    private void printAsXml(Object originalBean) throws JAXBException, PropertyException {
        System.out.println("Checking " + originalBean.toString());
        JAXBContext ctx = JAXBContext.newInstance(originalBean.getClass());
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.marshal(originalBean, System.out);
    }
}
