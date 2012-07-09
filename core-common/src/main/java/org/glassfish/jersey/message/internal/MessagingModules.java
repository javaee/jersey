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

import javax.inject.Singleton;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;

import org.glassfish.hk2.api.PerThread;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.jersey.internal.ServiceFinderModule;
import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.spi.HeaderDelegateProvider;

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
            bind(BuilderHelper.link(SourceProvider.StreamSourceReader.class).to(MessageBodyReader.class).in(Singleton.class).build());
            bind(BuilderHelper.link(SourceProvider.SaxSourceReader.class).to(MessageBodyReader.class).in(Singleton.class).build());
            bind(BuilderHelper.link(SourceProvider.DomSourceReader.class).to(MessageBodyReader.class).in(Singleton.class).build());
            bind(BuilderHelper.link(XmlRootObjectJaxbProvider.App.class).to(MessageBodyReader.class).in(Singleton.class).build());
            bind(BuilderHelper.link(XmlRootObjectJaxbProvider.Text.class).to(MessageBodyReader.class).in(Singleton.class).build());
            bind(BuilderHelper.link(XmlRootObjectJaxbProvider.General.class).to(MessageBodyReader.class).in(Singleton.class).build());
            /*
             * TODO: com.sun.jersey.core.impl.provider.entity.EntityHolderReader
             */

            install(new ServiceFinderModule<MessageBodyReader>(MessageBodyReader.class));

            // Message body writers
            bind(BuilderHelper.link(StreamingOutputProvider.class).to(MessageBodyWriter.class).in(Singleton.class).build());
            bind(BuilderHelper.link(SourceProvider.SourceWriter.class).to(MessageBodyWriter.class).in(Singleton.class).build());

            install(new ServiceFinderModule<MessageBodyWriter>(MessageBodyWriter.class));

            install(new ServiceFinderModule<HeaderDelegateProvider>(HeaderDelegateProvider.class));

            // XML factory injection points
            bind(BuilderHelper.link(DocumentBuilderFactoryInjectionProvider.class).to(DocumentBuilderFactory.class).in(PerThread.class).buildFactory());
            bind(BuilderHelper.link(SaxParserFactoryInjectionProvider.class).to(SAXParserFactory.class).in(PerThread.class).buildFactory());
            bind(BuilderHelper.link(XmlInputFactoryInjectionProvider.class).to(XMLInputFactory.class).in(PerThread.class).buildFactory());
            bind(BuilderHelper.link(TransformerFactoryInjectionProvider.class).to(TransformerFactory.class).in(PerThread.class).buildFactory());
        }

        private <T extends MessageBodyReader & MessageBodyWriter> void bindSingletonWorker(Class<T> worker) {
            bind(BuilderHelper.link(worker).
                    to(MessageBodyReader.class).
                    to(MessageBodyWriter.class).
                    in(Singleton.class).build());
        }
    }

    public static class HeaderDelegateProviders extends AbstractModule {

        @Override
        protected void configure() {
            bind(BuilderHelper.link(CacheControlProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class).build());
            bind(BuilderHelper.link(CookieProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class).build());
            bind(BuilderHelper.link(DateProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class).build());
            bind(BuilderHelper.link(EntityTagProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class).build());
            bind(BuilderHelper.link(LinkProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class).build());
            bind(BuilderHelper.link(LocaleProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class).build());
            bind(BuilderHelper.link(MediaTypeProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class).build());
            bind(BuilderHelper.link(NewCookieProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class).build());
            bind(BuilderHelper.link(StringHeaderProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class).build());
            bind(BuilderHelper.link(UriProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class).build());
        }
    }
}
