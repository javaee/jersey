/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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
import java.util.LinkedList;
import java.util.List;

import org.glassfish.jersey.tests.e2e.json.entity.AnotherArrayTestBean;
import org.glassfish.jersey.tests.e2e.json.entity.AttrAndCharDataBean;
import org.glassfish.jersey.tests.e2e.json.entity.ComplexBeanWithAttributes;
import org.glassfish.jersey.tests.e2e.json.entity.ComplexBeanWithAttributes2;
import org.glassfish.jersey.tests.e2e.json.entity.ComplexBeanWithAttributes3;
import org.glassfish.jersey.tests.e2e.json.entity.ComplexBeanWithAttributes4;
import org.glassfish.jersey.tests.e2e.json.entity.EmptyElementBean;
import org.glassfish.jersey.tests.e2e.json.entity.EmptyElementContainingBean;
import org.glassfish.jersey.tests.e2e.json.entity.EncodedContentBean;
import org.glassfish.jersey.tests.e2e.json.entity.FakeArrayBean;
import org.glassfish.jersey.tests.e2e.json.entity.IntArray;
import org.glassfish.jersey.tests.e2e.json.entity.ListAndNonListBean;
import org.glassfish.jersey.tests.e2e.json.entity.ListEmptyBean;
import org.glassfish.jersey.tests.e2e.json.entity.ListWrapperBean;
import org.glassfish.jersey.tests.e2e.json.entity.MyResponse;
import org.glassfish.jersey.tests.e2e.json.entity.NamespaceBean;
import org.glassfish.jersey.tests.e2e.json.entity.NamespaceBeanWithAttribute;
import org.glassfish.jersey.tests.e2e.json.entity.NullStringBean;
import org.glassfish.jersey.tests.e2e.json.entity.Person;
import org.glassfish.jersey.tests.e2e.json.entity.PureCharDataBean;
import org.glassfish.jersey.tests.e2e.json.entity.RegisterMessage;
import org.glassfish.jersey.tests.e2e.json.entity.SimpleBean;
import org.glassfish.jersey.tests.e2e.json.entity.SimpleBeanWithAttributes;
import org.glassfish.jersey.tests.e2e.json.entity.SimpleBeanWithJustOneAttribute;
import org.glassfish.jersey.tests.e2e.json.entity.SimpleBeanWithJustOneAttributeAndValue;
import org.glassfish.jersey.tests.e2e.json.entity.TreeModel;
import org.glassfish.jersey.tests.e2e.json.entity.TwoListsWrapperBean;
import org.glassfish.jersey.tests.e2e.json.entity.User;
import org.glassfish.jersey.tests.e2e.json.entity.UserTable;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Michal Gajdos
 */
@RunWith(Parameterized.class)
public class JaxbTest extends AbstractJsonTest {

    private static final Class<?>[] CLASSES = {
            AnotherArrayTestBean.class,
            AttrAndCharDataBean.class,
            ComplexBeanWithAttributes.class,
            ComplexBeanWithAttributes2.class,
            ComplexBeanWithAttributes3.class,
            ComplexBeanWithAttributes4.class,
            EmptyElementBean.class,
            EmptyElementContainingBean.class,
            EncodedContentBean.class,
            FakeArrayBean.class,
            IntArray.class,
            ListAndNonListBean.class,
            ListEmptyBean.class,
            ListWrapperBean.class,
            MyResponse.class,
            NamespaceBean.class,
            NamespaceBeanWithAttribute.class,
            NullStringBean.class,
            Person.class,
            PureCharDataBean.class,
            RegisterMessage.class,
            SimpleBean.class,
            SimpleBeanWithAttributes.class,
            SimpleBeanWithJustOneAttribute.class,
            SimpleBeanWithJustOneAttributeAndValue.class,
            TreeModel.class,
            TwoListsWrapperBean.class,
            User.class,
            UserTable.class
    };

    public JaxbTest(final JsonTestSetup jsonTestSetup) throws Exception {
        super(jsonTestSetup);
    }

    @Parameterized.Parameters()
    public static Collection<JsonTestSetup[]> getJsonProviders() throws Exception {
        final List<JsonTestSetup[]> jsonTestSetups = new LinkedList<>();

        for (final JsonTestProvider jsonProvider : JsonTestProvider.JAXB_PROVIDERS) {
            for (final Class<?> entityClass : CLASSES) {
                jsonTestSetups.add(new JsonTestSetup[]{new JsonTestSetup(entityClass, jsonProvider)});
            }
        }

        return jsonTestSetups;
    }
}
