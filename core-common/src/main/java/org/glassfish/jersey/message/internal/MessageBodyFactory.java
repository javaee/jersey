/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.io.Closeable;
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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.transform.Source;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.ReflectionHelper.DeclaringClassInterfacePair;
import org.glassfish.jersey.internal.util.collection.DataStructures;
import org.glassfish.jersey.internal.util.collection.KeyComparator;
import org.glassfish.jersey.internal.util.collection.KeyComparatorHashMap;
import org.glassfish.jersey.internal.util.collection.KeyComparatorLinkedHashMap;
import org.glassfish.jersey.message.AbstractEntityProviderModel;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.MessageProperties;
import org.glassfish.jersey.message.ReaderModel;
import org.glassfish.jersey.message.WriterModel;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import org.jvnet.hk2.annotations.Optional;

import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Sets;
import jersey.repackaged.com.google.common.primitives.Primitives;

/**
 * A factory for managing {@link MessageBodyReader}, {@link MessageBodyWriter} instances.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class MessageBodyFactory implements MessageBodyWorkers {

    private static final Logger LOGGER = Logger.getLogger(MessageBodyFactory.class.getName());

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
    public static final KeyComparator<MediaType> MEDIA_TYPE_KEY_COMPARATOR =
            new KeyComparator<MediaType>() {
                private static final long serialVersionUID = 1616819828630827763L;

                @Override
                public boolean equals(final MediaType mt1, final MediaType mt2) {
                    // treat compatible types as equal
                    return mt1.isCompatible(mt2);
                }

                @Override
                public int hash(final MediaType mt) {
                    // treat compatible types as equal
                    return mt.getType().toLowerCase().hashCode() + mt.getSubtype().toLowerCase().hashCode();
                }
            };
    /**
     * Compares entity providers by the provided class (most specific first)
     * and then by the declared supported media types, if the provided classes
     * are the same.
     */
    private static final Comparator<AbstractEntityProviderModel<?>> WORKER_BY_TYPE_COMPARATOR =
            new Comparator<AbstractEntityProviderModel<?>>() {

                @Override
                public int compare(final AbstractEntityProviderModel<?> o1, final AbstractEntityProviderModel<?> o2) {
                    final Class<?> o1ProviderClassParam = o1.providedType();
                    final Class<?> o2ProviderClassParam = o2.providedType();

                    if (o1ProviderClassParam == o2ProviderClassParam) {
                        // Compare producible media types.
                        return compare(o2.declaredTypes(), o1.declaredTypes());
                    } else if (o1ProviderClassParam.isAssignableFrom(o2ProviderClassParam)) {
                        return 1;
                    } else if (o2ProviderClassParam.isAssignableFrom(o1ProviderClassParam)) {
                        return -1;
                    }
                    return 0;
                }

                private int compare(List<MediaType> mediaTypeList1, List<MediaType> mediaTypeList2) {
                    mediaTypeList1 = mediaTypeList1.isEmpty() ? MediaTypes.WILDCARD_TYPE_SINGLETON_LIST : mediaTypeList1;
                    mediaTypeList2 = mediaTypeList2.isEmpty() ? MediaTypes.WILDCARD_TYPE_SINGLETON_LIST : mediaTypeList2;

                    return MediaTypes.MEDIA_TYPE_LIST_COMPARATOR.compare(mediaTypeList2, mediaTypeList1);
                }
            };

    private final ServiceLocator serviceLocator;

    private final Boolean legacyProviderOrdering;

    private final List<ReaderModel> readers;
    private final List<WriterModel> writers;

    private final Map<MediaType, List<MessageBodyReader>> readersCache =
            new KeyComparatorHashMap<MediaType, List<MessageBodyReader>>(MEDIA_TYPE_KEY_COMPARATOR);
    private final Map<MediaType, List<MessageBodyWriter>> writersCache =
            new KeyComparatorHashMap<MediaType, List<MessageBodyWriter>>(MEDIA_TYPE_KEY_COMPARATOR);

    private static final int LOOKUP_CACHE_INITIAL_CAPACITY = 32;
    private static final float LOOKUP_CACHE_LOAD_FACTOR = 0.75f;
    private final Map<Class<?>, List<ReaderModel>> mbrTypeLookupCache = DataStructures.createConcurrentMap(
            LOOKUP_CACHE_INITIAL_CAPACITY, LOOKUP_CACHE_LOAD_FACTOR, DataStructures.DEFAULT_CONCURENCY_LEVEL);
    private final Map<Class<?>, List<WriterModel>> mbwTypeLookupCache = DataStructures.createConcurrentMap(
            LOOKUP_CACHE_INITIAL_CAPACITY, LOOKUP_CACHE_LOAD_FACTOR, DataStructures.DEFAULT_CONCURENCY_LEVEL);

    private final Map<Class<?>, List<MediaType>> typeToMediaTypeReadersCache = DataStructures.createConcurrentMap(
            LOOKUP_CACHE_INITIAL_CAPACITY, LOOKUP_CACHE_LOAD_FACTOR, DataStructures.DEFAULT_CONCURENCY_LEVEL);
    private final Map<Class<?>, List<MediaType>> typeToMediaTypeWritersCache = DataStructures.createConcurrentMap(
            LOOKUP_CACHE_INITIAL_CAPACITY, LOOKUP_CACHE_LOAD_FACTOR, DataStructures.DEFAULT_CONCURENCY_LEVEL);

    private final Map<ModelLookupKey, List<ReaderModel>> mbrLookupCache = DataStructures.createConcurrentMap(
            LOOKUP_CACHE_INITIAL_CAPACITY, LOOKUP_CACHE_LOAD_FACTOR, DataStructures.DEFAULT_CONCURENCY_LEVEL);
    private final Map<ModelLookupKey, List<WriterModel>> mbwLookupCache = DataStructures.createConcurrentMap(
            LOOKUP_CACHE_INITIAL_CAPACITY, LOOKUP_CACHE_LOAD_FACTOR, DataStructures.DEFAULT_CONCURENCY_LEVEL);

    /**
     * Create new message body workers factory.
     *
     * @param locator       service locator.
     * @param configuration configuration. Optional - can be null.
     */
    @Inject
    public MessageBodyFactory(final ServiceLocator locator, @Optional final Configuration configuration) {
        this.serviceLocator = locator;
        this.legacyProviderOrdering = configuration != null
                && PropertiesHelper.isProperty(configuration.getProperty(MessageProperties.LEGACY_WORKERS_ORDERING));

        // Initialize readers
        this.readers = new ArrayList<ReaderModel>();
        final Set<MessageBodyReader> customMbrs = Providers.getCustomProviders(locator, MessageBodyReader.class);
        final Set<MessageBodyReader> mbrs = Providers.getProviders(locator, MessageBodyReader.class);

        addReaders(readers, customMbrs, true);
        mbrs.removeAll(customMbrs);
        addReaders(readers, mbrs, false);

        if (legacyProviderOrdering) {
            Collections.sort(readers, new LegacyWorkerComparator<MessageBodyReader>(MessageBodyReader.class));

            for (final ReaderModel model : readers) {
                for (final MediaType mt : model.declaredTypes()) {
                    List<MessageBodyReader> readerList = readersCache.get(mt);

                    if (readerList == null) {
                        readerList = new ArrayList<MessageBodyReader>();
                        readersCache.put(mt, readerList);
                    }
                    readerList.add(model.provider());
                }
            }
        }

        // Initialize writers
        this.writers = new ArrayList<WriterModel>();

        final Set<MessageBodyWriter> customMbws = Providers.getCustomProviders(locator, MessageBodyWriter.class);
        final Set<MessageBodyWriter> mbws = Providers.getProviders(locator, MessageBodyWriter.class);

        addWriters(writers, customMbws, true);
        mbws.removeAll(customMbws);
        addWriters(writers, mbws, false);

        if (legacyProviderOrdering) {
            Collections.sort(writers, new LegacyWorkerComparator<MessageBodyWriter>(MessageBodyWriter.class));

            for (final AbstractEntityProviderModel<MessageBodyWriter> model : writers) {
                for (final MediaType mt : model.declaredTypes()) {
                    List<MessageBodyWriter> writerList = writersCache.get(mt);

                    if (writerList == null) {
                        writerList = new ArrayList<MessageBodyWriter>();
                        writersCache.put(mt, writerList);
                    }
                    writerList.add(model.provider());
                }
            }
        }
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

        DeclarationDistanceComparator(final Class<T> declared) {
            this.declared = declared;
        }

        @Override
        public int compare(final T o1, final T o2) {
            final int d1 = getDistance(o1);
            final int d2 = getDistance(o2);
            return d2 - d1;
        }

        private int getDistance(final T t) {
            Integer distance = distanceMap.get(t.getClass());
            if (distance != null) {
                return distance;
            }

            final DeclaringClassInterfacePair p = ReflectionHelper.getClass(
                    t.getClass(), declared);

            final Class[] as = ReflectionHelper.getParameterizedClassArguments(p);
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
     * {@link AbstractEntityProviderModel} comparator
     * which works as it is described in JAX-RS 2.x specification.
     *
     * Pairs are sorted by distance from required type, media type and custom/provided (provided goes first).
     *
     * @param <T> MessageBodyReader or MessageBodyWriter.
     * @see DeclarationDistanceComparator
     * @see #MEDIA_TYPE_KEY_COMPARATOR
     */
    private static class WorkerComparator<T> implements Comparator<AbstractEntityProviderModel<T>> {

        final Class wantedType;
        final MediaType wantedMediaType;

        private WorkerComparator(final Class wantedType, final MediaType wantedMediaType) {
            this.wantedType = wantedType;
            this.wantedMediaType = wantedMediaType;
        }

        @Override
        public int compare(final AbstractEntityProviderModel<T> modelA, final AbstractEntityProviderModel<T> modelB) {

            final int distance = compareTypeDistances(modelA.providedType(), modelB.providedType());
            if (distance != 0) {
                return distance;
            }

            final int mediaTypeComparison = getMediaTypeDistance(wantedMediaType, modelA.declaredTypes())
                    - getMediaTypeDistance(wantedMediaType, modelB.declaredTypes());
            if (mediaTypeComparison != 0) {
                return mediaTypeComparison;
            }

            if (modelA.isCustom() ^ modelB.isCustom()) {
                return (modelA.isCustom()) ? -1 : 1;
            }
            return 0;
        }

        private int getMediaTypeDistance(final MediaType wanted, final List<MediaType> mtl) {
            if (wanted == null) {
                return 0;
            }

            int distance = 2;

            for (final MediaType mt : mtl) {
                if (MediaTypes.typeEqual(wanted, mt)) {
                    return 0;
                }

                if (distance > 1 && MediaTypes.typeEqual(MediaTypes.getTypeWildCart(wanted), mt)) {
                    distance = 1;
                }
            }

            return distance;
        }

        private int compareTypeDistances(final Class<?> providerClassParam1, final Class<?> providerClassParam2) {
            return getTypeDistance(providerClassParam1) - getTypeDistance(providerClassParam2);
        }

        private int getTypeDistance(final Class<?> classParam) {
            // cache?

            Class<?> tmp1 = wantedType;
            Class<?> tmp2 = classParam;

            final Iterator<Class<?>> it1 = getClassHierarchyIterator(tmp1);
            final Iterator<Class<?>> it2 = getClassHierarchyIterator(tmp2);

            int distance = 0;
            while (!wantedType.equals(tmp2) && !classParam.equals(tmp1)) {
                distance++;

                if (!wantedType.equals(tmp2)) {
                    tmp2 = it2.hasNext() ? it2.next() : null;
                }

                if (!classParam.equals(tmp1)) {
                    tmp1 = it1.hasNext() ? it1.next() : null;
                }

                if (tmp2 == null && tmp1 == null) {
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
            while (!unprocessed.isEmpty()) {
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
     * {@link AbstractEntityProviderModel} comparator which
     * works as it is described in JAX-RS 1.x specification.
     *
     * Pairs are sorted by custom/provided (custom goes first), media type and declaration distance.
     *
     * @param <T> MessageBodyReader or MessageBodyWriter.
     * @see DeclarationDistanceComparator
     * @see #MEDIA_TYPE_KEY_COMPARATOR
     */
    private static class LegacyWorkerComparator<T> implements Comparator<AbstractEntityProviderModel<T>> {

        final DeclarationDistanceComparator<T> distanceComparator;

        private LegacyWorkerComparator(final Class<T> type) {
            distanceComparator = new DeclarationDistanceComparator<T>(type);
        }

        @Override
        public int compare(final AbstractEntityProviderModel<T> modelA, final AbstractEntityProviderModel<T> modelB) {

            if (modelA.isCustom() ^ modelB.isCustom()) {
                return (modelA.isCustom()) ? -1 : 1;
            }
            final MediaType mtA = modelA.declaredTypes().get(0);
            final MediaType mtB = modelB.declaredTypes().get(0);

            final int mediaTypeComparison = MediaTypes.PARTIAL_ORDER_COMPARATOR.compare(mtA, mtB);
            if (mediaTypeComparison != 0 && !mtA.isCompatible(mtB)) {
                return mediaTypeComparison;
            }
            return distanceComparator.compare(modelA.provider(), modelB.provider());
        }
    }

    private static class ModelLookupKey {

        final Class<?> clazz;
        final MediaType mediaType;

        private ModelLookupKey(final Class<?> clazz, final MediaType mediaType) {
            this.clazz = clazz;
            this.mediaType = mediaType;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final ModelLookupKey that = (ModelLookupKey) o;

            return !(clazz != null ? !clazz.equals(that.clazz) : that.clazz != null)
                    && !(mediaType != null ? !mediaType.equals(that.mediaType) : that.mediaType != null);
        }

        @Override
        public int hashCode() {
            int result = clazz != null ? clazz.hashCode() : 0;
            result = 31 * result + (mediaType != null ? mediaType.hashCode() : 0);
            return result;
        }
    }

    private static void addReaders(final List<ReaderModel> models, final Set<MessageBodyReader> readers, final boolean custom) {
        for (final MessageBodyReader provider : readers) {
            final List<MediaType> values = MediaTypes.createFrom(provider.getClass().getAnnotation(Consumes.class));
            models.add(new ReaderModel(provider, values, custom));
        }
    }

    private static void addWriters(final List<WriterModel> models, final Set<MessageBodyWriter> writers, final boolean custom) {
        for (final MessageBodyWriter provider : writers) {
            final List<MediaType> values = MediaTypes.createFrom(provider.getClass().getAnnotation(Produces.class));
            models.add(new WriterModel(provider, values, custom));
        }
    }

    // MessageBodyWorkers
    @Override
    public Map<MediaType, List<MessageBodyReader>> getReaders(final MediaType mediaType) {
        final Map<MediaType, List<MessageBodyReader>> subSet =
                new KeyComparatorLinkedHashMap<MediaType, List<MessageBodyReader>>(MEDIA_TYPE_KEY_COMPARATOR);

        getCompatibleProvidersMap(mediaType, readers, subSet);
        return subSet;
    }

    @Override
    public Map<MediaType, List<MessageBodyWriter>> getWriters(final MediaType mediaType) {
        final Map<MediaType, List<MessageBodyWriter>> subSet =
                new KeyComparatorLinkedHashMap<MediaType, List<MessageBodyWriter>>(MEDIA_TYPE_KEY_COMPARATOR);

        getCompatibleProvidersMap(mediaType, writers, subSet);
        return subSet;
    }

    @Override
    public String readersToString(final Map<MediaType, List<MessageBodyReader>> readers) {
        return toString(readers);
    }

    @Override
    public String writersToString(final Map<MediaType, List<MessageBodyWriter>> writers) {
        return toString(writers);
    }

    private <T> String toString(final Map<MediaType, List<T>> set) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        for (final Map.Entry<MediaType, List<T>> e : set.entrySet()) {
            pw.append(e.getKey().toString()).println(" ->");
            for (final T t : e.getValue()) {
                pw.append("  ").println(t.getClass().getName());
            }
        }
        pw.flush();
        return sw.toString();
    }

    @Override
    public <T> MessageBodyReader<T> getMessageBodyReader(final Class<T> c, final Type t,
                                                         final Annotation[] as,
                                                         final MediaType mediaType) {
        return getMessageBodyReader(c, t, as, mediaType, null);
    }

    @Override
    public <T> MessageBodyReader<T> getMessageBodyReader(final Class<T> c, final Type t,
                                                         final Annotation[] as,
                                                         final MediaType mediaType,
                                                         final PropertiesDelegate propertiesDelegate) {

        MessageBodyReader<T> p = null;
        if (legacyProviderOrdering) {
            if (mediaType != null) {
                p = _getMessageBodyReader(c, t, as, mediaType, mediaType, propertiesDelegate);
                if (p == null) {
                    p = _getMessageBodyReader(c, t, as, mediaType, MediaTypes.getTypeWildCart(mediaType), propertiesDelegate);
                }
            }
            if (p == null) {
                p = _getMessageBodyReader(c, t, as, mediaType, MediaType.WILDCARD_TYPE, propertiesDelegate);
            }
        } else {
            p = _getMessageBodyReader(c, t, as, mediaType, readers, propertiesDelegate);
        }

        return p;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MediaType> getMessageBodyReaderMediaTypes(final Class<?> type,
                                                          final Type genericType,
                                                          final Annotation[] annotations) {
        final Set<MediaType> readableMediaTypes = Sets.newLinkedHashSet();

        for (final ReaderModel model : readers) {
            boolean readableWorker = false;

            for (final MediaType mt : model.declaredTypes()) {
                if (model.isReadable(type, genericType, annotations, mt)) {
                    readableMediaTypes.add(mt);
                    readableWorker = true;
                }

                if (!readableMediaTypes.contains(MediaType.WILDCARD_TYPE)
                        && readableWorker
                        && model.declaredTypes().contains(MediaType.WILDCARD_TYPE)) {
                    readableMediaTypes.add(MediaType.WILDCARD_TYPE);
                }
            }
        }

        final List<MediaType> mtl = Lists.newArrayList(readableMediaTypes);
        Collections.sort(mtl, MediaTypes.PARTIAL_ORDER_COMPARATOR);
        return mtl;
    }

    @SuppressWarnings("unchecked")
    private <T> boolean isCompatible(final AbstractEntityProviderModel<T> model, final Class c, final MediaType mediaType) {
        if (model.providedType().equals(Object.class)
                || // looks weird. Could/(should?) be separated to Writer/Reader check
                model.providedType().isAssignableFrom(c)
                || c.isAssignableFrom(model.providedType())
                ) {
            for (final MediaType mt : model.declaredTypes()) {
                if (mediaType == null) {
                    return true;
                }
                if (mediaType.isCompatible(mt)) {
                    return true;
                }
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private <T> MessageBodyReader<T> _getMessageBodyReader(final Class<T> c, final Type t,
                                                           final Annotation[] as,
                                                           final MediaType mediaType,
                                                           final List<ReaderModel> models,
                                                           final PropertiesDelegate propertiesDelegate) {

        // Ensure a parameter-less lookup type to prevent excessive memory consumption
        // reported in JERSEY-2297
        final MediaType lookupType = mediaType == null || mediaType.getParameters().isEmpty()
                ? mediaType
                : new MediaType(mediaType.getType(), mediaType.getSubtype());

        final ModelLookupKey lookupKey = new ModelLookupKey(c, lookupType);
        List<ReaderModel> readers = mbrLookupCache.get(lookupKey);
        if (readers == null) {
            readers = new ArrayList<ReaderModel>();

            for (final ReaderModel model : models) {
                if (isCompatible(model, c, mediaType)) {
                    readers.add(model);
                }
            }
            Collections.sort(readers, new WorkerComparator<MessageBodyReader>(c, mediaType));
            mbrLookupCache.put(lookupKey, readers);
        }

        if (readers.isEmpty()) {
            return null;
        }

        final TracingLogger tracingLogger = TracingLogger.getInstance(propertiesDelegate);
        MessageBodyReader<T> selected = null;
        final Iterator<ReaderModel> iterator = readers.iterator();
        while (iterator.hasNext()) {
            final ReaderModel model = iterator.next();
            if (model.isReadable(c, t, as, mediaType)) {
                selected = (MessageBodyReader<T>) model.provider();
                tracingLogger.log(MsgTraceEvent.MBR_SELECTED, selected);
                break;
            }
            tracingLogger.log(MsgTraceEvent.MBR_NOT_READABLE, model.provider());
        }

        if (tracingLogger.isLogEnabled(MsgTraceEvent.MBR_SKIPPED)) {
            while (iterator.hasNext()) {
                final ReaderModel model = iterator.next();
                tracingLogger.log(MsgTraceEvent.MBR_SKIPPED, model.provider());
            }
        }

        return selected;
    }

    @SuppressWarnings("unchecked")
    private <T> MessageBodyReader<T> _getMessageBodyReader(final Class<T> c, final Type t,
                                                           final Annotation[] as,
                                                           final MediaType mediaType, final MediaType lookup,
                                                           final PropertiesDelegate propertiesDelegate) {

        final List<MessageBodyReader> readers = readersCache.get(lookup);

        if (readers == null) {
            return null;
        }

        final TracingLogger tracingLogger = TracingLogger.getInstance(propertiesDelegate);
        MessageBodyReader<T> selected = null;
        final Iterator<MessageBodyReader> iterator = readers.iterator();
        while (iterator.hasNext()) {
            final MessageBodyReader p = iterator.next();
            if (isReadable(p, c, t, as, mediaType)) {
                selected = (MessageBodyReader<T>) p;
                tracingLogger.log(MsgTraceEvent.MBR_SELECTED, selected);
                break;
            }
            tracingLogger.log(MsgTraceEvent.MBR_NOT_READABLE, p);
        }

        if (tracingLogger.isLogEnabled(MsgTraceEvent.MBR_SKIPPED)) {
            while (iterator.hasNext()) {
                final MessageBodyReader p = iterator.next();
                tracingLogger.log(MsgTraceEvent.MBR_SKIPPED, p);
            }
        }

        return selected;
    }

    @Override
    public <T> MessageBodyWriter<T> getMessageBodyWriter(final Class<T> c, final Type t,
                                                         final Annotation[] as,
                                                         final MediaType mediaType) {
        return getMessageBodyWriter(c, t, as, mediaType, null);
    }

    @Override
    public <T> MessageBodyWriter<T> getMessageBodyWriter(final Class<T> c, final Type t,
                                                         final Annotation[] as,
                                                         final MediaType mediaType,
                                                         final PropertiesDelegate propertiesDelegate) {
        MessageBodyWriter<T> p = null;

        if (legacyProviderOrdering) {
            if (mediaType != null) {
                p = _getMessageBodyWriter(c, t, as, mediaType, mediaType, propertiesDelegate);
                if (p == null) {
                    p = _getMessageBodyWriter(c, t, as, mediaType, MediaTypes.getTypeWildCart(mediaType), propertiesDelegate);
                }
            }
            if (p == null) {
                p = _getMessageBodyWriter(c, t, as, mediaType, MediaType.WILDCARD_TYPE, propertiesDelegate);
            }
        } else {
            p = _getMessageBodyWriter(c, t, as, mediaType, writers, propertiesDelegate);
        }

        return p;
    }

    @SuppressWarnings("unchecked")
    private <T> MessageBodyWriter<T> _getMessageBodyWriter(final Class<T> c, final Type t,
                                                           final Annotation[] as,
                                                           final MediaType mediaType,
                                                           final List<WriterModel> models,
                                                           final PropertiesDelegate propertiesDelegate) {
        // Ensure  a parameter-less lookup type to prevent excessive memory consumption
        // reported in JERSEY-2297
        final MediaType lookupType = mediaType == null || mediaType.getParameters().isEmpty()
                ? mediaType
                : new MediaType(mediaType.getType(), mediaType.getSubtype());

        final ModelLookupKey lookupKey = new ModelLookupKey(c, lookupType);
        List<WriterModel> writers = mbwLookupCache.get(lookupKey);
        if (writers == null) {

            writers = new ArrayList<WriterModel>();

            for (final WriterModel model : models) {
                if (isCompatible(model, c, mediaType)) {
                    writers.add(model);
                }
            }
            Collections.sort(writers, new WorkerComparator<MessageBodyWriter>(c, mediaType));
            mbwLookupCache.put(lookupKey, writers);
        }

        if (writers.isEmpty()) {
            return null;
        }

        final TracingLogger tracingLogger = TracingLogger.getInstance(propertiesDelegate);
        MessageBodyWriter<T> selected = null;
        final Iterator<WriterModel> iterator = writers.iterator();
        while (iterator.hasNext()) {
            final WriterModel model = iterator.next();
            if (model.isWriteable(c, t, as, mediaType)) {
                selected = (MessageBodyWriter<T>) model.provider();
                tracingLogger.log(MsgTraceEvent.MBW_SELECTED, selected);
                break;
            }
            tracingLogger.log(MsgTraceEvent.MBW_NOT_WRITEABLE, model.provider());
        }

        if (tracingLogger.isLogEnabled(MsgTraceEvent.MBW_SKIPPED)) {
            while (iterator.hasNext()) {
                final WriterModel model = iterator.next();
                tracingLogger.log(MsgTraceEvent.MBW_SKIPPED, model.provider());
            }
        }

        return selected;
    }

    @SuppressWarnings("unchecked")
    private <T> MessageBodyWriter<T> _getMessageBodyWriter(final Class<T> c, final Type t,
                                                           final Annotation[] as,
                                                           final MediaType mediaType, final MediaType lookup,
                                                           final PropertiesDelegate propertiesDelegate) {
        final List<MessageBodyWriter> writers = writersCache.get(lookup);

        if (writers == null) {
            return null;
        }

        final TracingLogger tracingLogger = TracingLogger.getInstance(propertiesDelegate);
        MessageBodyWriter<T> selected = null;
        final Iterator<MessageBodyWriter> iterator = writers.iterator();
        while (iterator.hasNext()) {
            final MessageBodyWriter p = iterator.next();
            if (isWriteable(p, c, t, as, mediaType)) {
                selected = (MessageBodyWriter<T>) p;
                tracingLogger.log(MsgTraceEvent.MBW_SELECTED, selected);
                break;
            }
            tracingLogger.log(MsgTraceEvent.MBW_NOT_WRITEABLE, p);
        }

        if (tracingLogger.isLogEnabled(MsgTraceEvent.MBW_SKIPPED)) {
            while (iterator.hasNext()) {
                final MessageBodyWriter p = iterator.next();
                tracingLogger.log(MsgTraceEvent.MBW_SKIPPED, p);
            }
        }
        return selected;
    }

    private static <T> void getCompatibleProvidersMap(
            final MediaType mediaType,
            final List<? extends AbstractEntityProviderModel<T>> set,
            final Map<MediaType, List<T>> subSet) {

        if (mediaType.isWildcardType()) {
            getCompatibleProvidersList(mediaType, set, subSet);
        } else if (mediaType.isWildcardSubtype()) {
            getCompatibleProvidersList(mediaType, set, subSet);
            getCompatibleProvidersList(MediaType.WILDCARD_TYPE, set, subSet);
        } else {
            getCompatibleProvidersList(mediaType, set, subSet);
            getCompatibleProvidersList(
                    MediaTypes.getTypeWildCart(mediaType),
                    set, subSet);
            getCompatibleProvidersList(MediaType.WILDCARD_TYPE, set, subSet);
        }

    }

    private static <T> void getCompatibleProvidersList(
            final MediaType mediaType,
            final List<? extends AbstractEntityProviderModel<T>> set,
            final Map<MediaType, List<T>> subSet) {

        final List<T> providers = new ArrayList<T>();

        for (final AbstractEntityProviderModel<T> model : set) {
            if (model.declaredTypes().contains(mediaType)) {
                providers.add(model.provider());
            }
        }

        if (!providers.isEmpty()) {
            subSet.put(mediaType, Collections.unmodifiableList(providers));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MediaType> getMessageBodyWriterMediaTypes(final Class<?> c, final Type t, final Annotation[] as) {
        final Set<MediaType> writeableMediaTypes = Sets.newLinkedHashSet();

        for (final WriterModel model : writers) {
            boolean writeableWorker = false;

            for (final MediaType mt : model.declaredTypes()) {
                if (model.isWriteable(c, t, as, mt)) {
                    writeableMediaTypes.add(mt);
                    writeableWorker = true;
                }

                if (!writeableMediaTypes.contains(MediaType.WILDCARD_TYPE)
                        && writeableWorker
                        && model.declaredTypes().contains(MediaType.WILDCARD_TYPE)) {
                    writeableMediaTypes.add(MediaType.WILDCARD_TYPE);
                }
            }
        }

        final List<MediaType> mtl = Lists.newArrayList(writeableMediaTypes);
        Collections.sort(mtl, MediaTypes.PARTIAL_ORDER_COMPARATOR);
        return mtl;
    }

    private static final Function<WriterModel, MessageBodyWriter> MODEL_TO_WRITER =
            new Function<WriterModel, MessageBodyWriter>() {
                @Override
                public MessageBodyWriter apply(final WriterModel input) {
                    return input.provider();
                }
            };

    @Override
    @SuppressWarnings("unchecked")
    public List<MessageBodyWriter> getMessageBodyWritersForType(final Class<?> type) {
        return Lists.transform(getWritersModelsForType(type), MODEL_TO_WRITER);
    }

    @Override
    public List<WriterModel> getWritersModelsForType(final Class<?> type) {
        final List<WriterModel> writerModels = mbwTypeLookupCache.get(type);
        if (writerModels != null) {
            return writerModels;
        }
        return processMessageBodyWritersForType(type);
    }

    private List<WriterModel> processMessageBodyWritersForType(final Class<?> clazz) {
        final List<WriterModel> suitableWriters = Lists.newArrayList();

        if (Response.class.isAssignableFrom(clazz)) {
            suitableWriters.addAll(writers);
        } else {
            final Class<?> wrapped = Primitives.wrap(clazz);
            for (final WriterModel model : writers) {

                if (model.providedType() == null
                        || model.providedType() == clazz
                        || model.providedType().isAssignableFrom(wrapped)) {

                    suitableWriters.add(model);
                }
            }
        }
        // Type -> Writer.
        Collections.sort(suitableWriters, WORKER_BY_TYPE_COMPARATOR);
        mbwTypeLookupCache.put(clazz, suitableWriters);

        // Type -> MediaType.
        typeToMediaTypeWritersCache.put(clazz, getMessageBodyWorkersMediaTypesByType(suitableWriters));

        return suitableWriters;
    }

    @Override
    public List<MediaType> getMessageBodyWriterMediaTypesByType(final Class<?> type) {
        if (!typeToMediaTypeWritersCache.containsKey(type)) {
            processMessageBodyWritersForType(type);
        }
        return typeToMediaTypeWritersCache.get(type);
    }

    @Override
    public List<MediaType> getMessageBodyReaderMediaTypesByType(final Class<?> type) {
        if (!typeToMediaTypeReadersCache.containsKey(type)) {
            processMessageBodyReadersForType(type);
        }
        return typeToMediaTypeReadersCache.get(type);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<MediaType> getMessageBodyWorkersMediaTypesByType(
            final List<? extends AbstractEntityProviderModel<T>> workerModels) {

        final Set<MediaType> mediaTypeSet = Sets.newHashSet();
        for (final AbstractEntityProviderModel<T> model : workerModels) {
            mediaTypeSet.addAll(model.declaredTypes());
        }

        final List<MediaType> mediaTypes = Lists.newArrayList(mediaTypeSet);
        Collections.sort(mediaTypes, MediaTypes.PARTIAL_ORDER_COMPARATOR);
        return mediaTypes;
    }

    private static final Function<ReaderModel, MessageBodyReader> MODEL_TO_READER =
            new Function<ReaderModel, MessageBodyReader>() {
                @Override
                public MessageBodyReader apply(final ReaderModel input) {
                    return input.provider();
                }
            };

    @Override
    @SuppressWarnings("unchecked")
    public List<MessageBodyReader> getMessageBodyReadersForType(final Class<?> type) {
        return Lists.transform(getReaderModelsForType(type), MODEL_TO_READER);
    }

    @Override
    public List<ReaderModel> getReaderModelsForType(final Class<?> type) {
        if (!mbrTypeLookupCache.containsKey(type)) {
            processMessageBodyReadersForType(type);
        }

        return mbrTypeLookupCache.get(type);
    }

    private List<ReaderModel> processMessageBodyReadersForType(final Class<?> clazz) {
        final List<ReaderModel> suitableReaders = Lists.newArrayList();

        final Class<?> wrapped = Primitives.wrap(clazz);
        for (final ReaderModel reader : readers) {
            if (reader.providedType() == null
                    || reader.providedType() == clazz
                    || reader.providedType().isAssignableFrom(wrapped)) {
                suitableReaders.add(reader);
            }
        }

        // Type -> Writer.
        Collections.sort(suitableReaders, WORKER_BY_TYPE_COMPARATOR);
        mbrTypeLookupCache.put(clazz, suitableReaders);

        // Type -> MediaType.
        typeToMediaTypeReadersCache.put(clazz, getMessageBodyWorkersMediaTypesByType(suitableReaders));

        return suitableReaders;
    }

    @Override
    @SuppressWarnings("unchecked")
    public MediaType getMessageBodyWriterMediaType(
            final Class<?> c, final Type t, final Annotation[] as, final List<MediaType> acceptableMediaTypes) {

        for (final MediaType acceptable : acceptableMediaTypes) {
            for (final WriterModel model : writers) {
                for (final MediaType mt : model.declaredTypes()) {
                    if (mt.isCompatible(acceptable) && model.isWriteable(c, t, as, acceptable)) {
                        return MediaTypes.mostSpecific(mt, acceptable);
                    }
                }
            }

        }
        return null;
    }

    @Override
    public Object readFrom(final Class<?> rawType,
                           final Type type,
                           final Annotation[] annotations,
                           final MediaType mediaType,
                           final MultivaluedMap<String, String> httpHeaders,
                           final PropertiesDelegate propertiesDelegate,
                           final InputStream entityStream,
                           final Iterable<ReaderInterceptor> readerInterceptors,
                           final boolean translateNce) throws WebApplicationException, IOException {

        final ReaderInterceptorExecutor executor = new ReaderInterceptorExecutor(
                rawType,
                type,
                annotations,
                mediaType,
                httpHeaders,
                propertiesDelegate,
                entityStream,
                this,
                readerInterceptors,
                translateNce,
                serviceLocator);

        final TracingLogger tracingLogger = TracingLogger.getInstance(propertiesDelegate);
        final long timestamp = tracingLogger.timestamp(MsgTraceEvent.RI_SUMMARY);

        try {
            final Object instance = executor.proceed();
            if (!(instance instanceof Closeable) && !(instance instanceof Source)) {
                final InputStream stream = executor.getInputStream();
                if (stream != entityStream && stream != null) {
                    // We only close stream if it differs from the received entity stream,
                    // otherwise we let the caller close the stream.
                    ReaderWriter.safelyClose(stream);
                }
            }

            return instance;
        } finally {
            tracingLogger.logDuration(MsgTraceEvent.RI_SUMMARY, timestamp, executor.getProcessedCount());
        }
    }

    @Override
    public OutputStream writeTo(final Object t,
                                final Class<?> rawType,
                                final Type type,
                                final Annotation[] annotations,
                                final MediaType mediaType,
                                final MultivaluedMap<String, Object> httpHeaders,
                                final PropertiesDelegate propertiesDelegate,
                                final OutputStream entityStream,
                                final Iterable<WriterInterceptor> writerInterceptors)
            throws IOException, WebApplicationException {

        final WriterInterceptorExecutor executor = new WriterInterceptorExecutor(
                t,
                rawType,
                type,
                annotations,
                mediaType,
                httpHeaders,
                propertiesDelegate,
                entityStream,
                this,
                writerInterceptors,
                serviceLocator);

        final TracingLogger tracingLogger = TracingLogger.getInstance(propertiesDelegate);
        final long timestamp = tracingLogger.timestamp(MsgTraceEvent.WI_SUMMARY);

        try {
            executor.proceed();
        } finally {
            tracingLogger.logDuration(MsgTraceEvent.WI_SUMMARY, timestamp, executor.getProcessedCount());
        }

        return executor.getOutputStream();
    }

    /**
     * Safely invokes {@link javax.ws.rs.ext.MessageBodyWriter#isWriteable isWriteable} method on the supplied provider.
     *
     * Any exceptions will be logged at finer level.
     *
     * @param provider    message body writer on which the {@code isWriteable} should be invoked.
     * @param type        the class of instance that is to be written.
     * @param genericType the type of instance to be written, obtained either
     *                    by reflection of a resource method return type or via inspection
     *                    of the returned instance. {@link javax.ws.rs.core.GenericEntity}
     *                    provides a way to specify this information at runtime.
     * @param annotations an array of the annotations attached to the message entity instance.
     * @param mediaType   the media type of the HTTP entity.
     * @return {@code true} if the type is supported, otherwise {@code false}.
     */
    public static boolean isWriteable(
            final MessageBodyWriter<?> provider,
            final Class<?> type,
            final Type genericType,
            final Annotation[] annotations,
            final MediaType mediaType) {
        try {
            return provider.isWriteable(type, genericType, annotations, mediaType);
        } catch (final Exception ex) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, LocalizationMessages.ERROR_MBW_ISWRITABLE(provider.getClass().getName()), ex);
            }
        }
        return false;
    }

    /**
     * Safely invokes {@link javax.ws.rs.ext.MessageBodyReader#isReadable isReadable} method on the supplied provider.
     *
     * Any exceptions will be logged at finer level.
     *
     * @param provider    message body reader on which the {@code isReadable} should be invoked.
     *                    Safely invokes {@link javax.ws.rs.ext.MessageBodyReader#isReadable isReadable} method on the underlying
     *                    provider.
     * @param type        the class of instance to be produced.
     * @param genericType the type of instance to be produced. E.g. if the
     *                    message body is to be converted into a method parameter, this will be
     *                    the formal type of the method parameter as returned by
     *                    {@code Method.getGenericParameterTypes}.
     * @param annotations an array of the annotations on the declaration of the
     *                    artifact that will be initialized with the produced instance. E.g. if the
     *                    message body is to be converted into a method parameter, this will be
     *                    the annotations on that parameter returned by
     *                    {@code Method.getParameterAnnotations}.
     * @param mediaType   the media type of the HTTP entity, if one is not
     *                    specified in the request then {@code application/octet-stream} is
     *                    used.
     * @return {@code true} if the type is supported, otherwise {@code false}.
     */
    public static boolean isReadable(final MessageBodyReader<?> provider,
                                     final Class<?> type,
                                     final Type genericType,
                                     final Annotation[] annotations,
                                     final MediaType mediaType) {
        try {
            return provider.isReadable(type, genericType, annotations, mediaType);
        } catch (final Exception ex) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, LocalizationMessages.ERROR_MBR_ISREADABLE(provider.getClass().getName()), ex);
            }
        }
        return false;
    }
}
