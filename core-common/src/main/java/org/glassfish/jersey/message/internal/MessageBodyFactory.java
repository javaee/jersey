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
package org.glassfish.jersey.message.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.internal.ServiceProviders;
import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.util.KeyComparator;
import org.glassfish.jersey.internal.util.KeyComparatorHashMap;
import org.glassfish.jersey.internal.util.KeyComparatorLinkedHashMap;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.ReflectionHelper.DeclaringClassInterfacePair;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.process.internal.RequestScope;

import org.glassfish.hk2.Factory;
import org.glassfish.hk2.Scope;
import org.glassfish.hk2.TypeLiteral;

import org.jvnet.hk2.annotations.Inject;

/**
 * A factory for managing {@link MessageBodyReader} and {@link MessageBodyWriter}
 * instances.
 * <p/>
 * Note: {@link MessageBodyReader} and {@link MessageBodyWriter} implementation
 * must not inject the instance of this type directly, e.g. {@code @Inject MessageBodyWorkers w;}.
 * Instead a {@link Factory}-based injection should be used to prevent
 * cycles in the injection framework caused by the eager initialization of the
 * providers in the current factory implementation:
 * {@code @Inject Factory<MessageBodyWorkers> w;}
 *
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
// FIXME: Remove the note from the javadoc once the issue is fixed.
public class MessageBodyFactory implements MessageBodyWorkers {

    public static class Module extends AbstractModule {

        private static class InjectionFactory extends ReferencingFactory<MessageBodyWorkers> {

            public InjectionFactory(@Inject Factory<Ref<MessageBodyWorkers>> referenceFactory) {
                super(referenceFactory);
            }
        }
        //
        private final Class<? extends Scope> refScope;

        public Module(Class<? extends Scope> refScope) {
            this.refScope = refScope;
        }

        @Override
        protected void configure() {
            bind(MessageBodyWorkers.class)
                    .toFactory(InjectionFactory.class)
                    .in(RequestScope.class);
            bind(new TypeLiteral<Ref<MessageBodyWorkers>>() {})
                    .toFactory(ReferencingFactory.<MessageBodyWorkers>referenceFactory())
                    .in(refScope);
        }
    }
    //
    public static final KeyComparator<MediaType> MEDIA_TYPE_COMPARATOR =
            new KeyComparator<MediaType>() {

                private static final long serialVersionUID = 2727819828630827763L;

                @Override
                public boolean equals(MediaType x, MediaType y) {
                    return x.getType().equalsIgnoreCase(y.getType())
                            && x.getSubtype().equalsIgnoreCase(y.getSubtype());
                }

                @Override
                public int hash(MediaType k) {
                    return k.getType().toLowerCase().hashCode()
                            + k.getSubtype().toLowerCase().hashCode();
                }

                @Override
                public int compare(MediaType o1, MediaType o2) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            };
    private ServiceProviders serviceProviders;
    private Map<MediaType, List<MessageBodyReader>> readerProviders;
    private Map<MediaType, List<MessageBodyWriter>> writerProviders;
    private List<MessageBodyReaderPair> readerListProviders;
    private List<MessageBodyWriterPair> writerListProviders;
    private Map<MediaType, List<MessageBodyReader>> customReaderProviders;
    private Map<MediaType, List<MessageBodyWriter>> customWriterProviders;
    private List<MessageBodyReaderPair> customReaderListProviders;
    private List<MessageBodyWriterPair> customWriterListProviders;

    private static class MessageBodyWriterPair {

        final MessageBodyWriter<?> mbw;
        final List<MediaType> types;

        MessageBodyWriterPair(MessageBodyWriter<?> mbw, List<MediaType> types) {
            this.mbw = mbw;
            this.types = types;
        }
    }

    private static class MessageBodyReaderPair {

        final MessageBodyReader<?> mbr;
        final List<MediaType> types;

        MessageBodyReaderPair(MessageBodyReader<?> mbr, List<MediaType> types) {
            this.mbr = mbr;
            this.types = types;
        }
    }

    public MessageBodyFactory(ServiceProviders serviceProviders) {
        this.serviceProviders = serviceProviders;

        initReaders();
        initWriters();
    }

    /**
     * Compares 2 instances implementing/inheriting the same super-type and returns
     * which of the two instances has the super-type declaration closer in it's
     * inheritance hierarchy tree.
     * <p/>
     * The comparator is optimized to cache results of the previous distance declaration
     * computations.
     *
     * @param <T> common super-type used for computing the declaration distance and
     *     comparing instances.
     */
    private static class DeclarationDistanceComparator<T> implements Comparator<T> {

        private final Class<T> declared;
        private final Map<Class, Integer> distanceMap = new HashMap<Class, Integer>();

        DeclarationDistanceComparator(Class<T> declared) {
            this.declared = declared;
        }

        @Override
        public int compare(T o1, T o2) {
            int d1 = getDistance(o1);
            int d2 = getDistance(o2);
            return d2 - d1;
        }

        private int getDistance(T t) {
            Integer distance = distanceMap.get(t.getClass());
            if (distance != null) {
                return distance;
            }

            DeclaringClassInterfacePair p = ReflectionHelper.getClass(
                    t.getClass(), declared);

            Class[] as = ReflectionHelper.getParameterizedClassArguments(p);
            Class a = (as != null) ? as[0] : null;
            distance = 0;
            while (a != null && a != Object.class) {
                distance++;
                a = a.getSuperclass();
            }

            distanceMap.put(t.getClass(), distance);
            return distance;
        }
    }

    private void initReaders() {
        this.customReaderProviders = new KeyComparatorHashMap<MediaType, List<MessageBodyReader>>(
                MEDIA_TYPE_COMPARATOR);
        this.customReaderListProviders = new ArrayList<MessageBodyReaderPair>();
        this.readerProviders = new KeyComparatorHashMap<MediaType, List<MessageBodyReader>>(
                MEDIA_TYPE_COMPARATOR);
        this.readerListProviders = new ArrayList<MessageBodyReaderPair>();

        initReaders(customReaderProviders, customReaderListProviders, serviceProviders.getCustom(MessageBodyReader.class));
        initReaders(readerProviders, readerListProviders, serviceProviders.getDefault(MessageBodyReader.class));
    }

    private void initReaders(Map<MediaType, List<MessageBodyReader>> mediaToProvidersMap,
            List<MessageBodyReaderPair> listProviders, Set<MessageBodyReader> providersSet) {
        for (MessageBodyReader provider : providersSet) {
            List<MediaType> values = MediaTypes.createFrom(
                    provider.getClass().getAnnotation(Consumes.class));
            for (MediaType type : values) {
                registerProviderForMediaType(mediaToProvidersMap, provider, type);
            }
            listProviders.add(new MessageBodyReaderPair(provider, values));
        }

        final DeclarationDistanceComparator<MessageBodyReader> dc = new DeclarationDistanceComparator<MessageBodyReader>(MessageBodyReader.class);
        for (Map.Entry<MediaType, List<MessageBodyReader>> e : mediaToProvidersMap.entrySet()) {
            Collections.sort(e.getValue(), dc);
        }
        Collections.sort(listProviders, new Comparator<MessageBodyReaderPair>() {

            @Override
            public int compare(MessageBodyReaderPair p1, MessageBodyReaderPair p2) {
                return dc.compare(p1.mbr, p2.mbr);
            }
        });
    }

    private void initWriters() {
        this.customWriterProviders = new KeyComparatorHashMap<MediaType, List<MessageBodyWriter>>(
                MEDIA_TYPE_COMPARATOR);
        this.customWriterListProviders = new ArrayList<MessageBodyWriterPair>();

        this.writerProviders = new KeyComparatorHashMap<MediaType, List<MessageBodyWriter>>(
                MEDIA_TYPE_COMPARATOR);
        this.writerListProviders = new ArrayList<MessageBodyWriterPair>();

        initWriters(customWriterProviders, customWriterListProviders, serviceProviders.getCustom(MessageBodyWriter.class));
        initWriters(writerProviders, writerListProviders, serviceProviders.getDefault(MessageBodyWriter.class));
    }

    private void initWriters(Map<MediaType, List<MessageBodyWriter>> mediaToProvidersMap,
            List<MessageBodyWriterPair> listProviders, Set<MessageBodyWriter> providersSet) {
        for (MessageBodyWriter provider : providersSet) {
            List<MediaType> values = MediaTypes.createFrom(
                    provider.getClass().getAnnotation(Produces.class));
            for (MediaType type : values) {
                registerProviderForMediaType(mediaToProvidersMap, provider, type);
            }

            listProviders.add(new MessageBodyWriterPair(provider, values));
        }

        final DeclarationDistanceComparator<MessageBodyWriter> dc =
                new DeclarationDistanceComparator<MessageBodyWriter>(MessageBodyWriter.class);
        for (Map.Entry<MediaType, List<MessageBodyWriter>> e : mediaToProvidersMap.entrySet()) {
            Collections.sort(e.getValue(), dc);
        }

        Collections.sort(listProviders, new Comparator<MessageBodyWriterPair>() {

            @Override
            public int compare(MessageBodyWriterPair p1, MessageBodyWriterPair p2) {
                return dc.compare(p1.mbw, p2.mbw);
            }
        });
    }

    private <T> void registerProviderForMediaType(Map<MediaType, List<T>> mediaToProviderMap,
            T provider, MediaType mediaType) {
        if (!mediaToProviderMap.containsKey(mediaType)) {
            mediaToProviderMap.put(mediaType, new ArrayList<T>());
        }

        List<T> providers = mediaToProviderMap.get(mediaType);
        providers.add(provider);
    }

    // MessageBodyWorkers
    @Override
    public Map<MediaType, List<MessageBodyReader>> getReaders(MediaType mediaType) {
        Map<MediaType, List<MessageBodyReader>> subSet =
                new KeyComparatorLinkedHashMap<MediaType, List<MessageBodyReader>>(
                MEDIA_TYPE_COMPARATOR);

        if (!customReaderProviders.isEmpty()) {
            getCompatibleProvidersMap(mediaType, customReaderProviders, subSet);
        }
        getCompatibleProvidersMap(mediaType, readerProviders, subSet);
        return subSet;
    }

    @Override
    public Map<MediaType, List<MessageBodyWriter>> getWriters(MediaType mediaType) {
        Map<MediaType, List<MessageBodyWriter>> subSet =
                new KeyComparatorLinkedHashMap<MediaType, List<MessageBodyWriter>>(
                MEDIA_TYPE_COMPARATOR);

        if (!customWriterProviders.isEmpty()) {
            getCompatibleProvidersMap(mediaType, customWriterProviders, subSet);
        }
        getCompatibleProvidersMap(mediaType, writerProviders, subSet);
        return subSet;
    }

    @Override
    public String readersToString(Map<MediaType, List<MessageBodyReader>> readers) {
        return toString(readers);
    }

    @Override
    public String writersToString(Map<MediaType, List<MessageBodyWriter>> writers) {
        return toString(writers);
    }

    private <T> String toString(Map<MediaType, List<T>> set) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        for (Map.Entry<MediaType, List<T>> e : set.entrySet()) {
            pw.append(e.getKey().toString()).println(" ->");
            for (T t : e.getValue()) {
                pw.append("  ").println(t.getClass().getName());
            }
        }
        pw.flush();
        return sw.toString();
    }

    @Override
    public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> c, Type t,
            Annotation[] as,
            MediaType mediaType) {

        MessageBodyReader<T> reader;

        if (!customReaderProviders.isEmpty()) {
            reader = _getMessageBodyReader(c, t, as, mediaType, customReaderProviders);
            if (reader != null) {
                return reader;
            }
        }
        reader = _getMessageBodyReader(c, t, as, mediaType, readerProviders);

        return reader;
    }

    @Override
    public <T> List<MediaType> getMessageBodyReaderMediaTypes(Class<T> type, Type genericType, Annotation[] annotations) {
        List<MediaType> mtl = new ArrayList<MediaType>();
        for (MessageBodyReaderPair mbrp : customReaderListProviders) {
            for (MediaType mt : mbrp.types) {
                if (mbrp.mbr.isReadable(type, genericType, annotations, mt)) {
                    mtl.add(mt);
                }
            }
        }
        for (MessageBodyReaderPair mbrp : readerListProviders) {
            for (MediaType mt : mbrp.types) {
                if (mbrp.mbr.isReadable(type, genericType, annotations, mt)) {
                    mtl.addAll(mbrp.types);
                }
            }
        }

        Collections.sort(mtl, MediaTypes.MEDIA_TYPE_COMPARATOR);
        return mtl;
    }

    private <T> MessageBodyReader<T> _getMessageBodyReader(Class<T> c, Type t,
            Annotation[] as,
            MediaType mediaType,
            Map<MediaType, List<MessageBodyReader>> providers) {
        MessageBodyReader<T> p = null;
        if (mediaType != null) {
            p = _getMessageBodyReader(c, t, as, mediaType, mediaType, providers);
            if (p == null) {
                p = _getMessageBodyReader(c, t, as, mediaType,
                        MediaTypes.getTypeWildCart(mediaType), providers);
            }
        }
        if (p == null) {
            p = _getMessageBodyReader(c, t, as, mediaType, MediaTypes.GENERAL_MEDIA_TYPE, providers);
        }

        return p;
    }

    @SuppressWarnings("unchecked")
    private <T> MessageBodyReader<T> _getMessageBodyReader(Class<T> c, Type t,
            Annotation[] as,
            MediaType mediaType, MediaType lookup,
            Map<MediaType, List<MessageBodyReader>> providers) {

        List<MessageBodyReader> readers = providers.get(lookup);
        if (readers == null) {
            return null;
        }
        for (MessageBodyReader<?> p : readers) {
            if (p.isReadable(c, t, as, mediaType)) {
                return (MessageBodyReader<T>) p;
            }
        }
        return null;
    }

    @Override
    public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> c, Type t,
            Annotation[] as,
            MediaType mediaType) {

        MessageBodyWriter<T> p;

        if (!customWriterProviders.isEmpty()) {
            p = _getMessageBodyWriter(c, t, as, mediaType, customWriterProviders);
            if (p != null) {
                return p;
            }
        }
        p = _getMessageBodyWriter(c, t, as, mediaType, writerProviders);

        return p;
    }

    private <T> MessageBodyWriter<T> _getMessageBodyWriter(Class<T> c, Type t,
            Annotation[] as,
            MediaType mediaType,
            Map<MediaType, List<MessageBodyWriter>> providers) {

        MessageBodyWriter<T> p = null;

        if (mediaType != null) {
            p = _getMessageBodyWriter(c, t, as, mediaType, mediaType, providers);
            if (p == null) {
                p = _getMessageBodyWriter(c, t, as, mediaType,
                        MediaTypes.getTypeWildCart(mediaType), providers);
            }
        }
        if (p == null) {
            p = _getMessageBodyWriter(c, t, as, mediaType, MediaTypes.GENERAL_MEDIA_TYPE, providers);
        }

        return p;
    }

    @SuppressWarnings("unchecked")
    private <T> MessageBodyWriter<T> _getMessageBodyWriter(Class<T> c, Type t,
            Annotation[] as,
            MediaType mediaType, MediaType lookup,
            Map<MediaType, List<MessageBodyWriter>> providers) {
        List<MessageBodyWriter> writers = providers.get(lookup);
        if (writers == null) {
            return null;
        }
        for (MessageBodyWriter<?> p : writers) {
            if (p.isWriteable(c, t, as, mediaType)) {
                return (MessageBodyWriter<T>) p;
            }
        }

        return null;
    }

    private <T> void getCompatibleProvidersMap(MediaType mediaType,
            Map<MediaType, List<T>> set,
            Map<MediaType, List<T>> subSet) {
        if (mediaType.isWildcardType()) {
            getCompatibleProvidersList(mediaType, set, subSet);
        } else if (mediaType.isWildcardSubtype()) {
            getCompatibleProvidersList(mediaType, set, subSet);
            getCompatibleProvidersList(MediaTypes.GENERAL_MEDIA_TYPE, set, subSet);
        } else {
            getCompatibleProvidersList(mediaType, set, subSet);
            getCompatibleProvidersList(
                    MediaTypes.getTypeWildCart(mediaType),
                    set, subSet);
            getCompatibleProvidersList(MediaTypes.GENERAL_MEDIA_TYPE, set, subSet);
        }

    }

    private <T> void getCompatibleProvidersList(MediaType mediaType,
            Map<MediaType, List<T>> set,
            Map<MediaType, List<T>> subSet) {
        List<T> readers = set.get(mediaType);
        if (readers != null) {
            subSet.put(mediaType, Collections.unmodifiableList(readers));
        }
    }

    @Override
    public <T> List<MediaType> getMessageBodyWriterMediaTypes(Class<T> c, Type t,
            Annotation[] as) {
        List<MediaType> mtl = new ArrayList<MediaType>();
        for (MessageBodyWriterPair mbwp : customWriterListProviders) {
            for (MediaType mt : mbwp.types) {
                if (mbwp.mbw.isWriteable(c, t, as, mt)) {
                    mtl.add(mt);
                }
            }
        }
        for (MessageBodyWriterPair mbwp : writerListProviders) {
            for (MediaType mt : mbwp.types) {
                if (mbwp.mbw.isWriteable(c, t, as, mt)) {
                    mtl.addAll(mbwp.types);
                }
            }
        }

        Collections.sort(mtl, MediaTypes.MEDIA_TYPE_COMPARATOR);
        return mtl;
    }

    @Override
    public <T> MediaType getMessageBodyWriterMediaType(Class<T> c, Type t,
            Annotation[] as, List<MediaType> acceptableMediaTypes) {
        for (MediaType acceptable : acceptableMediaTypes) {
            for (MessageBodyWriterPair mbwp : customWriterListProviders) {
                for (MediaType mt : mbwp.types) {
                    if (mt.isCompatible(acceptable)
                            && mbwp.mbw.isWriteable(c, t, as, acceptable)) {
                        return MediaTypes.mostSpecific(mt, acceptable);
                    }
                }
            }
            for (MessageBodyWriterPair mbwp : writerListProviders) {
                for (MediaType mt : mbwp.types) {
                    if (mt.isCompatible(acceptable)
                            && mbwp.mbw.isWriteable(c, t, as, acceptable)) {
                        return MediaTypes.mostSpecific(mt, acceptable);
                    }
                }
            }

        }
        return null;
    }
}