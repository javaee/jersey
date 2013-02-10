/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.util.KeyComparator;
import org.glassfish.jersey.internal.util.KeyComparatorHashMap;
import org.glassfish.jersey.internal.util.KeyComparatorLinkedHashMap;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.ReflectionHelper.DeclaringClassInterfacePair;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.MessageProperties;
import org.glassfish.jersey.model.internal.RankedComparator;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import org.jvnet.hk2.annotations.Optional;

import com.google.common.collect.Lists;

/**
 * A factory for managing {@link MessageBodyReader}, {@link MessageBodyWriter}, {@link ReaderInterceptor}
 * and {@link WriterInterceptor} instances.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class MessageBodyFactory implements MessageBodyWorkers {

    /**
     * Message body factory injection binder.
     */
    public static class Binder extends AbstractBinder {
        @Override
        protected void configure() {
            bindAsContract(MessageBodyFactory.class).to(MessageBodyWorkers.class).in(Singleton.class);
        }
    }

    /**
     * Media type comparator.
     */
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

                // move to separate comparator?
                @Override
                public int compare(MediaType o1, MediaType o2) {
                    if (equals(o1, o2)) {
                        return 0;
                    } else if (o1.isWildcardType() ^ o2.isWildcardType()) {
                        return (o1.isWildcardType()) ? 1 : -1;
                    } else if (o1.isWildcardSubtype() ^ o2.isWildcardSubtype()) {
                        return (o1.isWildcardSubtype()) ? 1 : -1;
                    }
                    return 0;
                }
            };

    private final ServiceLocator locator;
    private final Boolean legacyProviderOrdering;

    private List<ReaderInterceptor> readerInterceptors;
    private List<WriterInterceptor> writerInterceptors;

    private List<MessageBodyWorkerPair<MessageBodyReader>> readers;
    private List<MessageBodyWorkerPair<MessageBodyWriter>> writers;

    private final Map<MediaType, List<MessageBodyReader>> readersCache =
            new KeyComparatorHashMap<MediaType, List<MessageBodyReader>>(MEDIA_TYPE_COMPARATOR);
    private final Map<MediaType, List<MessageBodyWriter>> writersCache =
            new KeyComparatorHashMap<MediaType, List<MessageBodyWriter>>(MEDIA_TYPE_COMPARATOR);

    private final Map<TypeMediaTypePair, List<MessageBodyWorkerPair<MessageBodyReader>>> mbrLookupCache =
            new ConcurrentHashMap<TypeMediaTypePair, List<MessageBodyWorkerPair<MessageBodyReader>>>();
    private final Map<TypeMediaTypePair, List<MessageBodyWorkerPair<MessageBodyWriter>>> mbwLookupCache =
            new ConcurrentHashMap<TypeMediaTypePair, List<MessageBodyWorkerPair<MessageBodyWriter>>>();


    @Override
    public List<ReaderInterceptor> getReaderInterceptors() {
        return readerInterceptors;
    }

    @Override
    public List<WriterInterceptor> getWriterInterceptors() {
        return writerInterceptors;
    }

    private static class MessageBodyWorkerPair<T> {
        final T provider;
        final List<MediaType> types;
        final Boolean custom;
        Class<?> providerClassParam = null;

        private MessageBodyWorkerPair(T provider, List<MediaType> types, Boolean custom) {
            this.provider = provider;
            this.types = types;
            this.custom = custom;
        }
    }

    /**
     * Create new message body workers factory.
     *
     * @param locator service locator.
     * @param configuration configuration. Optional - can be null.
     */
    @Inject
    public MessageBodyFactory(ServiceLocator locator, @Optional Configuration configuration) {
        this.locator = locator;
        this.legacyProviderOrdering = configuration != null
                        && PropertiesHelper.isProperty(configuration.getProperty(MessageProperties.LEGACY_WORKERS_ORDERING));

        initReaders();
        initWriters();
        initInterceptors();
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
     *            comparing instances.
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

    /**
     * {@link MessageBodyWorkerPair} comparator which works as it is described in JAX-RS 2.x specification.
     *
     * Pairs are sorted by distance from required type, media type and custom/provided (provided goes first).
     *
     * @param <T> MessageBodyReader or MessageBodyWriter.
     * @see DeclarationDistanceComparator
     * @see #MEDIA_TYPE_COMPARATOR
     */
    private static class WorkerComparator<T> implements Comparator<MessageBodyWorkerPair<T>> {

        final Class wantedType;
        final MediaType wantedMediaType;

        private WorkerComparator(Class wantedType, MediaType wantedMediaType) {
            this.wantedType = wantedType;
            this.wantedMediaType = wantedMediaType;
        }

        @Override
        public int compare(MessageBodyWorkerPair<T> mbwp1, MessageBodyWorkerPair<T> mbwp2) {

            final int distance = compareTypeDistances(mbwp1.providerClassParam, mbwp2.providerClassParam);
            if (distance != 0) {
                return distance;
            }

            final int mediaTypeComparison = getMediaTypeDistance(wantedMediaType, mbwp1.types) - getMediaTypeDistance(wantedMediaType, mbwp2.types);
            if (mediaTypeComparison != 0) {
                return mediaTypeComparison;
            }

            if (mbwp1.custom ^ mbwp2.custom) {
                return (mbwp1.custom) ? -1 : 1;
            }
            return 0;
        }

        private int getMediaTypeDistance(MediaType wanted, List<MediaType> mtl) {
            if(wanted == null) {
                return 0;
            }

            int distance = 2;

            for(MediaType mt : mtl) {
                if(MediaTypes.typeEqual(wanted, mt)) {
                    return 0;
                }

                if(distance > 1 && MediaTypes.typeEqual(MediaTypes.getTypeWildCart(wanted), mt)) {
                    distance = 1;
                }
            }

            return distance;
        }

        private int compareTypeDistances(Class<?> providerClassParam1, Class<?> providerClassParam2) {
            return getTypeDistance(providerClassParam1) - getTypeDistance(providerClassParam2);
        }

        private int getTypeDistance(Class<?> classParam) {
            // cache?

            Class<?> tmp1 = wantedType;
            Class<?> tmp2 = classParam;

            final Iterator<Class<?>> it1 = getClassHierarchyIterator(tmp1);
            final Iterator<Class<?>> it2 = getClassHierarchyIterator(tmp2);

            int distance = 0;
            while (!wantedType.equals(tmp2) && !classParam.equals(tmp1)) {
                distance++;

                if(!wantedType.equals(tmp2)) {
                    tmp2 = it2.hasNext() ? it2.next() : null;
                }

                if(!classParam.equals(tmp1)) {
                    tmp1 = it1.hasNext() ? it1.next() : null;
                }

                if(tmp2 == null && tmp1 == null) {
                    return Integer.MAX_VALUE;
                }
            }

            return distance;
        }

        private Iterator<Class<?>> getClassHierarchyIterator(final Class<?> classParam) {
            if (classParam == null) {
                return Collections.<Class<?>>emptyList().iterator();
            }

            final ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
            final LinkedList<Class<?>> unprocessed = new LinkedList<Class<?>>();

            unprocessed.add(classParam);
            while(!unprocessed.isEmpty()) {
                final Class<?> clazz = unprocessed.removeFirst();

                classes.add(clazz);
                unprocessed.addAll(Arrays.asList(clazz.getInterfaces()));

                final Class<?> superclazz = clazz.getSuperclass();
                if (superclazz != null) {
                    unprocessed.add(superclazz);
                }
            }

            return classes.iterator();
        }
    }

    /**
     * {@link MessageBodyWorkerPair} comparator which works as it is described in JAX-RS 1.x specification.
     *
     * Pairs are sorted by custom/provided (custom goes first), media type and declaration distance.
     *
     * @param <T> MessageBodyReader or MessageBodyWriter.
     * @see DeclarationDistanceComparator
     * @see #MEDIA_TYPE_COMPARATOR
     */
    private static class LegacyWorkerComparator<T> implements Comparator<MessageBodyWorkerPair<T>> {

        final DeclarationDistanceComparator<T> distanceComparator;

        private LegacyWorkerComparator(Class<T> type) {
            distanceComparator = new DeclarationDistanceComparator<T>(type);
        }

        @Override
        public int compare(MessageBodyWorkerPair<T> mbwp1, MessageBodyWorkerPair<T> mbwp2) {

            if (mbwp1.custom ^ mbwp2.custom) {
                return (mbwp1.custom) ? -1 : 1;
            }
            final int mediaTypeComparison = MEDIA_TYPE_COMPARATOR.compare(mbwp1.types.get(0), mbwp2.types.get(0));
            if (mediaTypeComparison != 0) {
                return mediaTypeComparison;
            }
            return distanceComparator.compare(mbwp1.provider, mbwp2.provider);
        }
    }

    private static class TypeMediaTypePair {
        final Class<?> clazz;
        final MediaType mediaType;

        private TypeMediaTypePair(Class<?> clazz, MediaType mediaType) {
            this.clazz = clazz;
            this.mediaType = mediaType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TypeMediaTypePair that = (TypeMediaTypePair) o;

            return !(clazz != null ? !clazz.equals(that.clazz) : that.clazz != null) &&
                    !(mediaType != null ? !mediaType.equals(that.mediaType) : that.mediaType != null);
        }

        @Override
        public int hashCode() {
            int result = clazz != null ? clazz.hashCode() : 0;
            result = 31 * result + (mediaType != null ? mediaType.hashCode() : 0);
            return result;
        }
    }

    private void initInterceptors() {
        // TODO: only "global" interceptors should be taken into account here ?

        this.readerInterceptors = Collections.unmodifiableList(
                Lists.newArrayList(
                        Providers.getAllProviders(locator, ReaderInterceptor.class, new RankedComparator<ReaderInterceptor>())
                )
        );

        this.writerInterceptors = Collections.unmodifiableList(
                Lists.newArrayList(
                    Providers.getAllProviders(locator, WriterInterceptor.class, new RankedComparator<WriterInterceptor>())
                )
        );
    }

    private void initReaders() {
        this.readers = new ArrayList<MessageBodyWorkerPair<MessageBodyReader>>();

        final Set<MessageBodyReader> customProviders = Providers.getCustomProviders(locator, MessageBodyReader.class);
        final Set<MessageBodyReader> providers = Providers.getProviders(locator, MessageBodyReader.class);

        initReaders(readers, customProviders, true);
        providers.removeAll(customProviders);
        initReaders(readers, providers, false);

        if(legacyProviderOrdering) {
            Collections.sort(readers, new LegacyWorkerComparator<MessageBodyReader>(MessageBodyReader.class));

            for(MessageBodyWorkerPair<MessageBodyReader> messageBodyWorkerPair : readers) {
                for(MediaType mt : messageBodyWorkerPair.types) {
                    List<MessageBodyReader> readerList = readersCache.get(mt);

                    if(readerList == null) {
                        readerList = new ArrayList<MessageBodyReader>();
                        readersCache.put(mt, readerList);
                    }
                    readerList.add(messageBodyWorkerPair.provider);
                }
            }
        }
    }

    private void initReaders(List<MessageBodyWorkerPair<MessageBodyReader>> readers, Set<MessageBodyReader> providersSet,
                             boolean custom) {
        for (MessageBodyReader provider : providersSet) {
            List<MediaType> values = MediaTypes.createFrom(
                    provider.getClass().getAnnotation(Consumes.class));
            final MessageBodyWorkerPair<MessageBodyReader> readerPair = new MessageBodyWorkerPair<MessageBodyReader>(provider, values, custom);
            readers.add(readerPair);
        }
    }

    private void initWriters() {
        this.writers = new ArrayList<MessageBodyWorkerPair<MessageBodyWriter>>();

        final Set<MessageBodyWriter> customProviders = Providers.getCustomProviders(locator, MessageBodyWriter.class);
        final Set<MessageBodyWriter> providers = Providers.getProviders(locator, MessageBodyWriter.class);

        initWriters(writers, customProviders, true);
        providers.removeAll(customProviders);
        initWriters(writers, providers, false);

        if(legacyProviderOrdering) {
            Collections.sort(writers, new LegacyWorkerComparator<MessageBodyWriter>(MessageBodyWriter.class));

            for(MessageBodyWorkerPair<MessageBodyWriter> messageBodyWorkerPair : writers) {
                for(MediaType mt : messageBodyWorkerPair.types) {
                    List<MessageBodyWriter> writerList = writersCache.get(mt);

                    if(writerList == null) {
                        writerList = new ArrayList<MessageBodyWriter>();
                        writersCache.put(mt, writerList);
                    }
                    writerList.add(messageBodyWorkerPair.provider);
                }
            }
        }

    }

    private void initWriters(List<MessageBodyWorkerPair<MessageBodyWriter>> writers, Set<MessageBodyWriter> providersSet,
                             boolean custom) {
        for (MessageBodyWriter provider : providersSet) {
            List<MediaType> values = MediaTypes.createFrom(
                    provider.getClass().getAnnotation(Produces.class));
            final MessageBodyWorkerPair<MessageBodyWriter> workerPair = new MessageBodyWorkerPair<MessageBodyWriter>(provider, values, custom);

            writers.add(workerPair);
        }
    }

    // MessageBodyWorkers
    @Override
    public Map<MediaType, List<MessageBodyReader>> getReaders(MediaType mediaType) {
        Map<MediaType, List<MessageBodyReader>> subSet =
                new KeyComparatorLinkedHashMap<MediaType, List<MessageBodyReader>>(
                        MEDIA_TYPE_COMPARATOR);

        getCompatibleProvidersMap(mediaType, readers, subSet);
        return subSet;
    }

    @Override
    public Map<MediaType, List<MessageBodyWriter>> getWriters(MediaType mediaType) {
        Map<MediaType, List<MessageBodyWriter>> subSet =
                new KeyComparatorLinkedHashMap<MediaType, List<MessageBodyWriter>>(
                        MEDIA_TYPE_COMPARATOR);

        getCompatibleProvidersMap(mediaType, writers, subSet);
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

        MessageBodyReader<T> p = null;
        if(legacyProviderOrdering) {
            if (mediaType != null) {
                p = _getMessageBodyReader(c, t, as, mediaType, mediaType);
                if (p == null) {
                    p = _getMessageBodyReader(c, t, as, mediaType,
                            MediaTypes.getTypeWildCart(mediaType));
                }
            }
            if (p == null) {
                p = _getMessageBodyReader(c, t, as, mediaType, MediaTypes.GENERAL_MEDIA_TYPE);
            }
        } else {
            p = _getMessageBodyReader(c, t, as, mediaType, readers);
        }

        return p;
    }

    @Override
    public <T> List<MediaType> getMessageBodyReaderMediaTypes(Class<T> type, Type genericType, Annotation[] annotations) {
        List<MediaType> mtl = new ArrayList<MediaType>();

        for (MessageBodyWorkerPair<MessageBodyReader> mbrp : readers) {
            for (MediaType mt : mbrp.types) {
                if (mbrp.provider.isReadable(type, genericType, annotations, mt)) {
                    mtl.addAll(mbrp.types);
                }
            }
        }

        Collections.sort(mtl, MediaTypes.MEDIA_TYPE_COMPARATOR);
        return mtl;
    }

    private <T> boolean isCompatible(Class<T> workerClass, MessageBodyWorkerPair<T> messageBodyWorkerPair, Class c,
                                     MediaType mediaType) {

        if(messageBodyWorkerPair.providerClassParam == null) {
            DeclaringClassInterfacePair p = ReflectionHelper.getClass(
                    messageBodyWorkerPair.provider.getClass(), workerClass);

            Class[] classArgs = ReflectionHelper.getParameterizedClassArguments(p);
            messageBodyWorkerPair.providerClassParam = (classArgs != null) ? classArgs[0] : null;
        }

        if(messageBodyWorkerPair.providerClassParam == null) {
            messageBodyWorkerPair.providerClassParam = Object.class;
        }

        if(messageBodyWorkerPair.providerClassParam.equals(Object.class) ||
                // looks weird. Could/(should?) be separated to Writer/Reader check
                messageBodyWorkerPair.providerClassParam.isAssignableFrom(c) ||
                c.isAssignableFrom(messageBodyWorkerPair.providerClassParam)
                ) {
            for(MediaType mt : messageBodyWorkerPair.types) {
                if(mediaType == null) {
                    return true;
                }

                if(MediaTypes.typeEqual(mediaType, mt) ||
                        MediaTypes.typeEqual(MediaTypes.getTypeWildCart(mediaType), mt) ||
                        MediaTypes.typeEqual(MediaTypes.GENERAL_MEDIA_TYPE, mt)) {
                    return true;
                }
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private <T> MessageBodyReader<T> _getMessageBodyReader(Class<T> c, Type t,
                                                           Annotation[] as,
                                                           MediaType mediaType,
                                                           List<MessageBodyWorkerPair<MessageBodyReader>> workers) {

        List<MessageBodyWorkerPair<MessageBodyReader>> readers = mbrLookupCache.get(new TypeMediaTypePair(c, mediaType));
        if(readers == null) {
            readers = new ArrayList<MessageBodyWorkerPair<MessageBodyReader>>();

            for(MessageBodyWorkerPair<MessageBodyReader> mbwp : workers) {
                if(isCompatible(MessageBodyReader.class, mbwp, c, mediaType)) {
                    readers.add(mbwp);
                }
            }
            Collections.sort(readers, new WorkerComparator<MessageBodyReader>(c, mediaType));
            mbrLookupCache.put(new TypeMediaTypePair(c, mediaType), readers);
        }

        if(readers.isEmpty()) {
            return null;
        }

        for(MessageBodyWorkerPair<MessageBodyReader> mbwp : readers) {
            if(mbwp.provider.isReadable(c, t, as, mediaType)) {
                return mbwp.provider;
            }
        }

        return null;
    }


    @SuppressWarnings("unchecked")
    private <T> MessageBodyReader<T> _getMessageBodyReader(Class<T> c, Type t,
                                                           Annotation[] as,
                                                           MediaType mediaType, MediaType lookup) {

        List<MessageBodyReader> readers = readersCache.get(lookup);

        if(readers == null) {
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
        MessageBodyWriter<T> p = null;

        if(legacyProviderOrdering) {
            if (mediaType != null) {
                p = _getMessageBodyWriter(c, t, as, mediaType, mediaType);
                if (p == null) {
                    p = _getMessageBodyWriter(c, t, as, mediaType,
                            MediaTypes.getTypeWildCart(mediaType));
                }
            }
            if (p == null) {
                p = _getMessageBodyWriter(c, t, as, mediaType, MediaTypes.GENERAL_MEDIA_TYPE);
            }
        } else {
            p = _getMessageBodyWriter(c, t, as, mediaType, writers);
        }

        return p;
    }

    @SuppressWarnings("unchecked")
    private <T> MessageBodyWriter<T> _getMessageBodyWriter(Class<T> c, Type t,
                                                           Annotation[] as,
                                                           MediaType mediaType,
                                                           List<MessageBodyWorkerPair<MessageBodyWriter>> workers) {

        List<MessageBodyWorkerPair<MessageBodyWriter>> writers = mbwLookupCache.get(new TypeMediaTypePair(c, mediaType));
        if(writers == null) {

            writers = new ArrayList<MessageBodyWorkerPair<MessageBodyWriter>>();

            for(MessageBodyWorkerPair<MessageBodyWriter> mbwp : workers) {
                if(isCompatible(MessageBodyWriter.class, mbwp, c, mediaType)) {
                    writers.add(mbwp);
                }
            }
            Collections.sort(writers, new WorkerComparator<MessageBodyWriter>(c, mediaType));
            mbwLookupCache.put(new TypeMediaTypePair(c, mediaType), writers);
        }

        if(writers.isEmpty()) {
            return null;
        }


        for(MessageBodyWorkerPair<MessageBodyWriter> mbwp : writers) {
            if(mbwp.provider.isWriteable(c, t, as, mediaType)) {
                return mbwp.provider;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> MessageBodyWriter<T> _getMessageBodyWriter(Class<T> c, Type t,
                                                           Annotation[] as,
                                                           MediaType mediaType, MediaType lookup) {
        List<MessageBodyWriter> writers = writersCache.get(lookup);

        if(writers == null) {
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
                                               List<MessageBodyWorkerPair<T>> set,
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
                                                List<MessageBodyWorkerPair<T>> set,
                                                Map<MediaType, List<T>> subSet) {

        List<T> providers = new ArrayList<T>();

        for(MessageBodyWorkerPair<T> mbpp : set) {
            if(mbpp.types.contains(mediaType)) {
                providers.add(mbpp.provider);
            }
        }

        if (!providers.isEmpty()) {
            subSet.put(mediaType, Collections.unmodifiableList(providers));
        }
    }

    @Override
    public <T> List<MediaType> getMessageBodyWriterMediaTypes(Class<T> c, Type t,
                                                              Annotation[] as) {
        List<MediaType> mtl = new ArrayList<MediaType>();

        for (MessageBodyWorkerPair<MessageBodyWriter> mbwp : writers) {
            for (MediaType mt : mbwp.types) {
                if (mbwp.provider.isWriteable(c, t, as, mt)) {
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

            for (MessageBodyWorkerPair<MessageBodyWriter> mbwp : writers) {
                for (MediaType mt : mbwp.types) {
                    if (mt.isCompatible(acceptable)
                            && mbwp.provider.isWriteable(c, t, as, acceptable)) {
                        return MediaTypes.mostSpecific(mt, acceptable);
                    }
                }
            }

        }
        return null;
    }

    @Override
    public <T> Object readFrom(Class<T> rawType, Type type, Annotation[] annotations, MediaType mediaType,
                               MultivaluedMap<String, String> httpHeaders, PropertiesDelegate propertiesDelegate, InputStream entityStream,
                               boolean intercept) throws WebApplicationException, IOException {

        ReaderInterceptorExecutor executor = new ReaderInterceptorExecutor(rawType, type, annotations, mediaType,
                httpHeaders, propertiesDelegate, entityStream, this, intercept);
        return executor.proceed();
    }

    @Override
    public <T> OutputStream writeTo(Object t, Class<T> rawType, Type type, Annotation[] annotations, MediaType mediaType,
                                    MultivaluedMap<String, Object> httpHeaders, PropertiesDelegate propertiesDelegate, OutputStream entityStream,
                                    MessageBodySizeCallback sizeCallback, boolean intercept) throws IOException, WebApplicationException {

        return writeTo(t, rawType, type, annotations, mediaType, httpHeaders, propertiesDelegate, entityStream, sizeCallback, intercept, true);
    }

    @Override
    public <T> OutputStream writeTo(Object t, Class<T> rawType, Type type, Annotation[] annotations, MediaType mediaType,
                                    MultivaluedMap<String, Object> httpHeaders, PropertiesDelegate propertiesDelegate, OutputStream entityStream,
                                    MessageBodySizeCallback sizeCallback, boolean intercept, boolean writeEntity) throws IOException, WebApplicationException {

        WriterInterceptorExecutor executor = new WriterInterceptorExecutor(t, rawType, type, annotations, mediaType,
                httpHeaders, propertiesDelegate, entityStream, this, sizeCallback, intercept, writeEntity);
        executor.proceed();
        return executor.getOutputStream();
    }
}
