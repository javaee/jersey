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
package org.glassfish.jersey.media.json.internal.reader;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.glassfish.jersey.media.json.internal.*;
import org.glassfish.jersey.media.json.internal.writer.Stax2JacksonWriter;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;

import com.sun.xml.bind.v2.runtime.JAXBContextImpl;

/**
 *
 * @author Jakub Podlesak
 */
public class Jackson2StaxReaderTest extends TestCase {

    public Jackson2StaxReaderTest(String testName) {
        super(testName);
    }

    public void testAttrAndCharData() throws Exception {
        _testBean(AttrAndCharDataBean.class, AttrAndCharDataBean.createTestInstance());
    }

    public void testComplexBeanWithAttributes() throws Exception {
        _testBean(ComplexBeanWithAttributes.class, ComplexBeanWithAttributes.createTestInstance());
    }

    public void testComplexBeanWithAttributes2() throws Exception {
        _testBean(ComplexBeanWithAttributes2.class, ComplexBeanWithAttributes2.createTestInstance());
    }

    public void testComplexBeanWithAttributes3() throws Exception {
        _testBean(ComplexBeanWithAttributes3.class, ComplexBeanWithAttributes3.createTestInstance());
    }

    public void testEncodedContentBean() throws Exception {
        _testBean(EncodedContentBean.class, EncodedContentBean.createTestInstance());
    }

    public void testListAndNonListBean() throws Exception {
        _testBean(ListAndNonListBean.class, ListAndNonListBean.createTestInstance());
    }

    public void testListEmptyBean() throws Exception {
        _testBean(ListEmptyBean.class, ListEmptyBean.createTestInstance());
    }

    public void testListWrapperBean() throws Exception {
        _testBean(ListWrapperBean.class, ListWrapperBean.createTestInstance());
    }

    public void testPureCharDataBean() throws Exception {
        _testBean(PureCharDataBean.class, PureCharDataBean.createTestInstance());
    }

    public void testSimpleBean() throws Exception {
        _testBean(SimpleBean.class, SimpleBean.createTestInstance());
    }

    public void testSimpleBeanWithAttributes() throws Exception {
        _testBean(SimpleBeanWithAttributes.class, SimpleBeanWithAttributes.createTestInstance());
    }

    public void testSimpleBeanWithJustOneAttribute() throws Exception {
        _testBean(SimpleBeanWithJustOneAttribute.class, SimpleBeanWithJustOneAttribute.createTestInstance());
    }

    public void testSimpleBeanWithJustOneAttributeAndValue() throws Exception {
        _testBean(SimpleBeanWithJustOneAttributeAndValue.class, SimpleBeanWithJustOneAttributeAndValue.createTestInstance());
    }

    public void testTreeModelBean() throws Exception {
        _testBean(TreeModel.class, TreeModel.createTestInstance());
    }

    public void testTwoListsWrapperBean() throws Exception {
        _testBean(TwoListsWrapperBean.class, TwoListsWrapperBean.createTestInstance());
    }

    public void testUser() throws Exception {
        _testBean(User.class, User.createTestInstance());
    }

    public void testUserTable() throws Exception {
        _testBean(UserTable.class, UserTable.createTestInstance());
    }

    private void _testBean(Class clazz, Object bean) throws Exception {
        Map<String, Object> props = new HashMap<String, Object>();

        props.put(JAXBContextImpl.RETAIN_REFERENCE_TO_INFO, Boolean.TRUE);
        Class[] classes = new Class[]{clazz};

        JAXBContext ctx = JAXBContext.newInstance(classes, props);

        JsonFactory factory = new JsonFactory();
        Writer sWriter = new StringWriter();
        JsonGenerator g;

        g = factory.createJsonGenerator(sWriter);

        Marshaller marshaller = ctx.createMarshaller();
//        marshaller.marshal(bean, System.out);
        marshaller.marshal(bean, new Stax2JacksonWriter(g));

        g.flush();

        String jsonExpression = sWriter.toString();
        System.out.println(jsonExpression);

        JsonParser parser = factory.createJsonParser(new StringReader(jsonExpression));

        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        Object unmarshalledBean = unmarshaller.unmarshal(new Jackson2StaxReader(parser));

        System.out.println(String.format("Unmarshalled bean = %s", unmarshalledBean));

        assertEquals(bean, unmarshalledBean);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
