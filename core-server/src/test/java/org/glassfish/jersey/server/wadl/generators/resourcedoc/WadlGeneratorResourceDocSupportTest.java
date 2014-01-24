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
package org.glassfish.jersey.server.wadl.generators.resourcedoc;

import java.io.StringWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ws.rs.POST;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.wadl.WadlGenerator;
import org.glassfish.jersey.server.wadl.internal.ApplicationDescription;
import org.glassfish.jersey.server.wadl.internal.WadlBuilder;
import org.glassfish.jersey.server.wadl.internal.WadlGeneratorImpl;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.WadlGeneratorResourceDocSupport;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.AnnotationDocType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.ClassDocType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.MethodDocType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.NamedValueType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.ParamDocType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.ResourceDocType;

import org.junit.Test;

import com.sun.research.ws.wadl.Application;

import jersey.repackaged.com.google.common.collect.Lists;

public class WadlGeneratorResourceDocSupportTest {
    @Test
    public void wadlIsGeneratedWithUnknownCustomParameterAnnotation() throws JAXBException {
        /* Set up a ClassDocType that has something for a custom-annotated parameter */
        ClassDocType cdt = new ClassDocType();
        cdt.setClassName(TestResource.class.getName());

        MethodDocType mdt = new MethodDocType();
        mdt.setMethodName("method");
        cdt.getMethodDocs().add(mdt);

        ParamDocType pdt = new ParamDocType("x", "comment about x");
        mdt.getParamDocs().add(pdt);

        AnnotationDocType adt = new AnnotationDocType();
        adt.setAnnotationTypeName(CustomParam.class.getName());
        adt.getAttributeDocs().add(new NamedValueType("value", "x"));

        pdt.getAnnotationDocs().add(adt);

        ResourceDocType rdt = new ResourceDocType();
        rdt.getDocs().add(cdt);


        /* Generate WADL for that class */
        WadlGenerator wg = new WadlGeneratorResourceDocSupport(new WadlGeneratorImpl(), rdt);

        WadlBuilder wb = new WadlBuilder(wg, false, null);
        Resource resource = Resource.from(TestResource.class);
        ApplicationDescription app = wb.generate(Lists.newArrayList(resource));


        /* Confirm that it can be marshalled without error */
        StringWriter sw = new StringWriter();

        JAXBContext context = JAXBContext.newInstance(Application.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        m.marshal(app.getApplication(), sw);
    }

    public static class TestResource {
        @POST
        public String method(@CustomParam("x") Object param) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * An annotation IntrospectionModeller doesn't know about.
     */
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CustomParam {
        String value();
    }
}