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

/*
 * Portions contributed by Joseph Walton (Atlassian)
 */

package org.glassfish.jersey.jaxb.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;

import javax.inject.Provider;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.SAXParserFactory;

import org.glassfish.jersey.model.internal.CommonConfig;
import org.glassfish.jersey.model.internal.ComponentBag;

import org.junit.Test;

public class JaxbStringReaderProviderTest {

    @Test
    public void stringReaderDoesNotReadExternalDtds() {

        Provider<SAXParserFactory> saxParserFactoryProvider = new Provider<SAXParserFactory>() {
            final SaxParserFactoryInjectionProvider spf = new SaxParserFactoryInjectionProvider(
                    new CommonConfig(RuntimeType.SERVER, ComponentBag.INCLUDE_ALL));

            @Override
            public SAXParserFactory get() {
                return spf.get();
            }
        };

        JaxbStringReaderProvider.RootElementProvider provider = new JaxbStringReaderProvider.RootElementProvider(
                saxParserFactoryProvider, new Providers() {
            @Override
            public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> type,
                                                                 Type genericType,
                                                                 Annotation[] annotations,
                                                                 MediaType mediaType) {
                return null;
            }

            @Override
            public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> type,
                                                                 Type genericType,
                                                                 Annotation[] annotations,
                                                                 MediaType mediaType) {
                return null;
            }

            @Override
            public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> type) {
                return null;
            }

            @Override
            public <T> ContextResolver<T> getContextResolver(Class<T> contextType, MediaType mediaType) {
                return null;
            }
        });

        String content = "<!DOCTYPE x SYSTEM 'file:///no-such-file'> <rootObject/>";

        provider.getConverter(RootObject.class, null, null).fromString(content);
    }

    @XmlRootElement
    static class RootObject {
    }
}
