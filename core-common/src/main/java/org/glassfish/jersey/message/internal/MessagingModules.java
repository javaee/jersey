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
package org.glassfish.jersey.message.internal;

import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;

import org.glassfish.jersey.internal.ServiceFinderModule;
import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.spi.HeaderDelegateProvider;

import org.glassfish.hk2.scopes.PerThread;
import org.glassfish.hk2.scopes.Singleton;

/**
 * Binding definitions for the default set of message related providers (readers,
 * writers, header delegates).
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class MessagingModules {

    public static class MessageBodyProviders extends AbstractModule {

        @Override
        protected void configure() {

            // Message body providers (both readers & writers)
            bindSingletonWorker(ByteArrayProvider.class);
            bindSingletonWorker(DataSourceProvider.class);
            bindSingletonWorker(DocumentProvider.class);
            bindSingletonWorker(FileProvider.class);
            bindSingletonWorker(FormMultivaluedMapProvider.class);
            bindSingletonWorker(FormProvider.class);
            bindSingletonWorker(InputStreamProvider.class);
            bindSingletonWorker(ReaderProvider.class);
            bindSingletonWorker(RenderedImageProvider.class);
            bindSingletonWorker(StringMessageProvider.class);

            bindSingletonWorker(XmlJaxbElementProvider.App.class);
            bindSingletonWorker(XmlJaxbElementProvider.Text.class);
            bindSingletonWorker(XmlJaxbElementProvider.General.class);

            bindSingletonWorker(XmlCollectionJaxbProvider.App.class);
            bindSingletonWorker(XmlCollectionJaxbProvider.Text.class);
            bindSingletonWorker(XmlCollectionJaxbProvider.General.class);

            bindSingletonWorker(XmlRootElementJaxbProvider.App.class);
            bindSingletonWorker(XmlRootElementJaxbProvider.Text.class);
            bindSingletonWorker(XmlRootElementJaxbProvider.General.class);

            // Message body readers
            bind(MessageBodyReader.class).to(SourceProvider.StreamSourceReader.class).in(Singleton.class);
            bind(MessageBodyReader.class).to(SourceProvider.SaxSourceReader.class).in(Singleton.class);
            bind(MessageBodyReader.class).to(SourceProvider.DomSourceReader.class).in(Singleton.class);
            bind(MessageBodyReader.class).to(XmlRootObjectJaxbProvider.App.class).in(Singleton.class);
            bind(MessageBodyReader.class).to(XmlRootObjectJaxbProvider.Text.class).in(Singleton.class);
            bind(MessageBodyReader.class).to(XmlRootObjectJaxbProvider.General.class).in(Singleton.class);
            /*
             * TODO: com.sun.jersey.core.impl.provider.entity.EntityHolderReader
             */

            install(new ServiceFinderModule<MessageBodyReader>(MessageBodyReader.class));

            // Message body writers
            bind(MessageBodyWriter.class).to(StreamingOutputProvider.class).in(Singleton.class);
            bind(MessageBodyWriter.class).to(SourceProvider.SourceWriter.class).in(Singleton.class);

            install(new ServiceFinderModule<MessageBodyWriter>(MessageBodyWriter.class));

            install(new ServiceFinderModule<HeaderDelegateProvider>(HeaderDelegateProvider.class));

            // XML factory injection points
            bind(DocumentBuilderFactory.class).toFactory(DocumentBuilderFactoryInjectionProvider.class).in(PerThread.class);
            bind(SAXParserFactory.class).toFactory(SaxParserFactoryInjectionProvider.class).in(PerThread.class);
            bind(XMLInputFactory.class).toFactory(XmlInputFactoryInjectionProvider.class).in(PerThread.class);
            bind(TransformerFactory.class).toFactory(TransformerFactoryInjectionProvider.class).in(PerThread.class);
        }

        private <T extends MessageBodyReader & MessageBodyWriter> void bindSingletonWorker(Class<T> worker) {
            bind().to(worker).in(Singleton.class);
            bind(MessageBodyReader.class).to(worker).in(Singleton.class);
            bind(MessageBodyWriter.class).to(worker).in(Singleton.class);
        }
    }

    public static class HeaderDelegateProviders extends AbstractModule {

        @Override
        protected void configure() {
            bind(HeaderDelegateProvider.class).to(CacheControlProvider.class).in(Singleton.class);
            bind(HeaderDelegateProvider.class).to(CookieProvider.class).in(Singleton.class);
            bind(HeaderDelegateProvider.class).to(DateProvider.class).in(Singleton.class);
            bind(HeaderDelegateProvider.class).to(EntityTagProvider.class).in(Singleton.class);
            bind(HeaderDelegateProvider.class).to(LinkProvider.class).in(Singleton.class);
            bind(HeaderDelegateProvider.class).to(LocaleProvider.class).in(Singleton.class);
            bind(HeaderDelegateProvider.class).to(MediaTypeProvider.class).in(Singleton.class);
            bind(HeaderDelegateProvider.class).to(NewCookieProvider.class).in(Singleton.class);
            bind(HeaderDelegateProvider.class).to(StringHeaderProvider.class).in(Singleton.class);
            bind(HeaderDelegateProvider.class).to(UriProvider.class).in(Singleton.class);
        }
    }
}
