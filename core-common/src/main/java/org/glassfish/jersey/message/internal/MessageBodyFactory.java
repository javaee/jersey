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
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.MessageProperties;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import org.jvnet.hk2.annotations.Optional;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;

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

    /**
     * Compares message body workers by providing class (most specific first) and assigned media types if provider classes are
     * the same.
     */
    private static final Comparator<WorkerModel<?>> WORKER_BY_TYPE_COMPARATOR =
            new Comparator<WorkerModel<?>>() {

                @Override
                public int compare(final WorkerModel<?> o1, final WorkerModel<?> o2) {
                    final Class<?> o1ProviderClassParam = o1.providerClassParam;
                    final Class<?> o2ProviderClassParam = o2.providerClassParam;

                    if (o1ProviderClassParam == o2ProviderClassParam) {
                        // Compare producible media types.
                        return compare(o2.types, o1.types);
                    } else if (o1ProviderClassParam.isAssignableFrom(o2ProviderClassParam)) {
                        return 1;
                    } else if (o2ProviderClassParam.isAssignableFrom(o1ProviderClassParam)) {
                        return -1;
                    }
                    return 0;
                }

                private int compare(List<MediaType> mediaTypeList1, List<MediaType> mediaTypeList2) {
                    mediaTypeList1 = mediaTypeList1.isEmpty() ? MediaTypes.GENERAL_MEDIA_TYPE_LIST : mediaTypeList1;
                    mediaTypeList2 = mediaTypeList2.isEmpty() ? MediaTypes.GENERAL_MEDIA_TYPE_LIST : mediaTypeList2;

                    return MediaTypes.MEDIA_TYPE_LIST_COMPARATOR.compare(mediaTypeList2, mediaTypeList1);
                }
            };

    private final Boolean legacyProviderOrdering;

    private final List<MbrModel> readers;
    private final List<MbwModel> writers;

    private final Map<MediaType, List<MessageBodyReader>> readersCache =
            new KeyComparatorHashMap<MediaType, List<MessageBodyReader>>(MEDIA_TYPE_COMPARATOR);
    private final Map<MediaType, List<MessageBodyWriter>> writersCache =
            new KeyComparatorHashMap<MediaType, List<MessageBodyWriter>>(MEDIA_TYPE_COMPARATOR);

    private final Map<Class<?>, List<MessageBodyReader>> mbrTypeLookupCache =
            DataStructures.createConcurrentMap(32, 0.75f, DataStructures.DEFAULT_CONCURENCY_LEVEL);
    private final Map<Class<?>, List<MessageBodyWriter>> mbwTypeLookupCache =
            DataStructures.createConcurrentMap(32, 0.75f, DataStructures.DEFAULT_CONCURENCY_LEVEL);

    private final Map<Class<?>, List<MediaType>> typeToMediaTypeReadersCache =
            DataStructures.createConcurrentMap(32, 0.75f, DataStructures.DEFAULT_CONCURENCY_LEVEL);
    private final Map<Class<?>, List<MediaType>> typeToMediaTypeWritersCache =
            DataStructures.createConcurrentMap(32, 0.75f, DataStructures.DEFAULT_CONCURENCY_LEVEL);

    private final Map<ModelLookupKey, List<MbrModel>> mbrLookupCache =
            DataStructures.createConcurrentMap(32, 0.75f, DataStructures.DEFAULT_CONCURENCY_LEVEL);
    private final Map<ModelLookupKey, List<MbwModel>> mbwLookupCache =
            DataStructures.createConcurrentMap(32, 0.75f, DataStructures.DEFAULT_CONCURENCY_LEVEL);

    private static class WorkerModel<T> {

        public final T provider;
        public final List<MediaType> types;
        public final Boolean custom;
        public final Class<?> providerClassParam;

        protected WorkerModel(
                final T provider, final List<MediaType> types, final Boolean custom, Class<T> providerType) {
            this.provider = provider;
            this.types = types;
            this.custom = custom;
            this.providerClassParam = getProviderClassParam(provider, providerType);
        }

        private static Class<?> getProviderClassParam(Object provider, Class<?> providerType) {
            final ReflectionHelper.DeclaringClassInterfacePair pair =
                    ReflectionHelper.getClass(provider.getClass(), providerType);
            final Class[] classArgs = ReflectionHelper.getParameterizedClassArguments(pair);

            return classArgs != null ? classArgs[0] : Object.class;
        }
    }

    private static class MbrModel extends WorkerModel<MessageBodyReader> {

        public MbrModel(MessageBodyReader provider, List<MediaType> types, Boolean custom) {
            super(provider, types, custom, MessageBodyReader.class);
        }

        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return MbrModel.isReadable(provider, type, genericType, annotations, mediaType);
        }

        @SuppressWarnings("unchecked")
        public static boolean isReadable(MessageBodyReader provider,
                                         Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            try {
                return provider.isReadable(type, genericType, annotations, mediaType);
            } catch (Exception ex) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, LocalizationMessages.ERROR_MBR_ISREADABLE(provider.getClass().getName()), ex);
                }
            }
            return false;
        }
    }

    private static class MbwModel extends WorkerModel<MessageBodyWriter> {

        public MbwModel(MessageBodyWriter provider, List<MediaType> types, Boolean custom) {
            super(provider, types, custom, MessageBodyWriter.class);
        }

        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return MbwModel.isWriteable(provider, type, genericType, annotations, mediaType);
        }

        @SuppressWarnings("unchecked")
        public static boolean isWriteable(MessageBodyWriter provider,
                                          Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            try {
                return provider.isWriteable(type, genericType, annotations, mediaType);
            } catch (Exception ex) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, LocalizationMessages.ERROR_MBW_ISWRITABLE(provider.getClass().getName()), ex);
                }
            }
            return false;
        }
    }

    /**
     * Create new message body workers factory.
     *
     * @param locator       service locator.
     * @param configuration configuration. Optional - can be null.
     */
    @Inject
    public MessageBodyFactory(ServiceLocator locator, @Optional Configuration configuration) {
        this.legacyProviderOrdering = configuration != null
                && PropertiesHelper.isProperty(configuration.getProperty(MessageProperties.LEGACY_WORKERS_ORDERING));


        // Initialize readers
        this.readers = new ArrayList<MbrModel>();
        final Set<MessageBodyReader> customMbrs = Providers.getCustomProviders(locator, MessageBodyReader.class);
        final Set<MessageBodyReader> mbrs = Providers.getProviders(locator, MessageBodyReader.class);

        addReaders(readers, customMbrs, true);
        mbrs.removeAll(customMbrs);
        addReaders(readers, mbrs, false);

        if (legacyProviderOrdering) {
            Collections.sort(readers, new LegacyWorkerComparator<MessageBodyReader>(MessageBodyReader.class));

            for (MbrModel model : readers) {
                for (MediaType mt : model.types) {
                    List<MessageBodyReader> readerList = readersCache.get(mt);

                    if (readerList == null) {
                        readerList = new ArrayList<MessageBodyReader>();
                        readersCache.put(mt, readerList);
                    }
                    readerList.add(model.provider);
                }
            }
        }

        // Initialize writers
        this.writers = new ArrayList<MbwModel>();

        final Set<MessageBodyWriter> customMbws = Providers.getCustomProviders(locator, MessageBodyWriter.class);
        final Set<MessageBodyWriter> mbws = Providers.getProviders(locator, MessageBodyWriter.class);

        addWriters(writers, customMbws, true);
        mbws.removeAll(customMbws);
        addWriters(writers, mbws, false);

        if (legacyProviderOrdering) {
            Collections.sort(writers, new LegacyWorkerComparator<MessageBodyWriter>(MessageBodyWriter.class));

            for (WorkerModel<MessageBodyWriter> model : writers) {
                for (MediaType mt : model.types) {
                    List<MessageBodyWriter> writerList = writersCache.get(mt);

                    if (writerList == null) {
                        writerList = new ArrayList<MessageBodyWriter>();
                        writersCache.put(mt, writerList);
                    }
                    writerList.add(model.provider);
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
     * {@link org.glassfish.jersey.message.internal.MessageBodyFactory.WorkerModel} comparator
     * which works as it is described in JAX-RS 2.x specification.
     *
     * Pairs are sorted by distance from required type, media type and custom/provided (provided goes first).
     *
     * @param <T> MessageBodyReader or MessageBodyWriter.
     * @see DeclarationDistanceComparator
     * @see #MEDIA_TYPE_COMPARATOR
     */
    private static class WorkerComparator<T> implements Comparator<WorkerModel<T>> {

        final Class wantedType;
        final MediaType wantedMediaType;

        private WorkerComparator(Class wantedType, MediaType wantedMediaType) {
            this.wantedType = wantedType;
            this.wantedMediaType = wantedMediaType;
        }

        @Override
        public int compare(WorkerModel<T> modelA, WorkerModel<T> modelB) {

            final int distance = compareTypeDistances(modelA.providerClassParam, modelB.providerClassParam);
            if (distance != 0) {
                return distance;
            }

            final int mediaTypeComparison =
                    getMediaTypeDistance(wantedMediaType, modelA.types) - getMediaTypeDistance(wantedMediaType, modelB.types);
            if (mediaTypeComparison != 0) {
                return mediaTypeComparison;
            }

            if (modelA.custom ^ modelB.custom) {
                return (modelA.custom) ? -1 : 1;
            }
            return 0;
        }

        private int getMediaTypeDistance(MediaType wanted, List<MediaType> mtl) {
            if (wanted == null) {
                return 0;
            }

            int distance = 2;

            for (MediaType mt : mtl) {
                if (MediaTypes.typeEqual(wanted, mt)) {
                    return 0;
                }

                if (distance > 1 && MediaTypes.typeEqual(MediaTypes.getTypeWildCart(wanted), mt)) {
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
     * {@link org.glassfish.jersey.message.internal.MessageBodyFactory.WorkerModel} comparator which
     * works as it is described in JAX-RS 1.x specification.
     *
     * Pairs are sorted by custom/provided (custom goes first), media type and declaration distance.
     *
     * @param <T> MessageBodyReader or MessageBodyWriter.
     * @see DeclarationDistanceComparator
     * @see #MEDIA_TYPE_COMPARATOR
     */
    private static class LegacyWorkerComparator<T> implements Comparator<WorkerModel<T>> {

        final DeclarationDistanceComparator<T> distanceComparator;

        private LegacyWorkerComparator(Class<T> type) {
            distanceComparator = new DeclarationDistanceComparator<T>(type);
        }

        @Override
        public int compare(WorkerModel<T> modelA, WorkerModel<T> modelB) {

            if (modelA.custom ^ modelB.custom) {
                return (modelA.custom) ? -1 : 1;
            }
            final int mediaTypeComparison = MEDIA_TYPE_COMPARATOR.compare(modelA.types.get(0), modelB.types.get(0));
            if (mediaTypeComparison != 0) {
                return mediaTypeComparison;
            }
            return distanceComparator.compare(modelA.provider, modelB.provider);
        }
    }

    private static class ModelLookupKey {
        final Class<?> clazz;
        final MediaType mediaType;

        private ModelLookupKey(Class<?> clazz, MediaType mediaType) {
            this.clazz = clazz;
            this.mediaType = mediaType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ModelLookupKey that = (ModelLookupKey) o;

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

    private static void addReaders(List<MbrModel> models, Set<MessageBodyReader> readers, boolean custom) {
        for (MessageBodyReader provider : readers) {
            List<MediaType> values = MediaTypes.createFrom(provider.getClass().getAnnotation(Consumes.class));
            models.add(new MbrModel(provider, values, custom));
        }
    }

    private static void addWriters(List<MbwModel> models, Set<MessageBodyWriter> writers, boolean custom) {
        for (MessageBodyWriter provider : writers) {
            List<MediaType> values = MediaTypes.createFrom(provider.getClass().getAnnotation(Produces.class));
            models.add(new MbwModel(provider, values, custom));
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
        return getMessageBodyReader(c, t, as, mediaType, null);
    }

    @Override
    public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> c, Type t,
                                                         Annotation[] as,
                                                         MediaType mediaType,
                                                         PropertiesDelegate propertiesDelegate) {

        MessageBodyReader<T> p = null;
        if (legacyProviderOrdering) {
            if (mediaType != null) {
                p = _getMessageBodyReader(c, t, as, mediaType, mediaType, propertiesDelegate);
                if (p == null) {
                    p = _getMessageBodyReader(c, t, as, mediaType, MediaTypes.getTypeWildCart(mediaType), propertiesDelegate);
                }
            }
            if (p == null) {
                p = _getMessageBodyReader(c, t, as, mediaType, MediaTypes.GENERAL_MEDIA_TYPE, propertiesDelegate);
            }
        } else {
            p = _getMessageBodyReader(c, t, as, mediaType, readers, propertiesDelegate);
        }

        return p;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MediaType> getMessageBodyReaderMediaTypes(Class<?> type, Type genericType, Annotation[] annotations) {
        final Set<MediaType> readableMediaTypes = Sets.newLinkedHashSet();

        for (MbrModel model : readers) {
            boolean readableWorker = false;

            for (MediaType mt : model.types) {
                if (model.isReadable(type, genericType, annotations, mt)) {
                    readableMediaTypes.add(mt);
                    readableWorker = true;
                }

                if (!readableMediaTypes.contains(MediaType.WILDCARD_TYPE)
                        && readableWorker
                        && model.types.contains(MediaType.WILDCARD_TYPE)) {
                    readableMediaTypes.add(MediaType.WILDCARD_TYPE);
                }
            }
        }

        final List<MediaType> mtl = Lists.newArrayList(readableMediaTypes);
        Collections.sort(mtl, MediaTypes.MEDIA_TYPE_COMPARATOR);
        return mtl;
    }

    @SuppressWarnings("unchecked")
    private <T> boolean isCompatible(WorkerModel<T> model, Class c, MediaType mediaType) {
        if (model.providerClassParam.equals(Object.class) ||
                // looks weird. Could/(should?) be separated to Writer/Reader check
                model.providerClassParam.isAssignableFrom(c) ||
                c.isAssignableFrom(model.providerClassParam)
                ) {
            for (MediaType mt : model.types) {
                if (mediaType == null) {
                    return true;
                }

                if (MediaTypes.typeEqual(mediaType, mt) ||
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
                                                           List<MbrModel> models,
                                                           PropertiesDelegate propertiesDelegate) {

        // Making a new mediatype with parameters stripped. Otherwise the cache can fill up 
        // just because the mediatype objects have unique parameters
        MediaType mediaTypeKey = new MediaType(mediaType.getType(), mediaType.getSubtype()); 
        List<MbrModel> readers = mbrLookupCache.get(new ModelLookupKey(c, mediaTypeKey));
        if (readers == null) {
            readers = new ArrayList<MbrModel>();

            for (MbrModel model : models) {
                if (isCompatible(model, c, mediaType)) {
                    readers.add(model);
                }
            }
            Collections.sort(readers, new WorkerComparator<MessageBodyReader>(c, mediaType));
            mbrLookupCache.put(new ModelLookupKey(c, mediaTypeKey), readers);
        }

        if (readers.isEmpty()) {
            return null;
        }

        final TracingLogger tracingLogger = TracingLogger.getInstance(propertiesDelegate);
        MessageBodyReader <T> selected = null;
        final Iterator<MbrModel> iterator = readers.iterator();
        while (iterator.hasNext()) {
            final MbrModel model = iterator.next();
            if (model.isReadable(c, t, as, mediaType)) {
                selected = (MessageBodyReader<T>) model.provider;
                tracingLogger.log(MsgTraceEvent.MBR_SELECTED, selected);
                break;
            }
            tracingLogger.log(MsgTraceEvent.MBR_NOT_READABLE, model.provider);
        }

        if (tracingLogger.isLogEnabled(MsgTraceEvent.MBR_SKIPPED)) {
            while (iterator.hasNext()) {
                final MbrModel model = iterator.next();
                tracingLogger.log(MsgTraceEvent.MBR_SKIPPED, model.provider);
            }
        }

        return selected;
    }

    @SuppressWarnings("unchecked")
    private <T> MessageBodyReader<T> _getMessageBodyReader(Class<T> c, Type t,
                                                           Annotation[] as,
                                                           MediaType mediaType, MediaType lookup,
                                                           PropertiesDelegate propertiesDelegate) {

        List<MessageBodyReader> readers = readersCache.get(lookup);

        if (readers == null) {
            return null;
        }

        final TracingLogger tracingLogger = TracingLogger.getInstance(propertiesDelegate);
        MessageBodyReader<T> selected = null;
        final Iterator<MessageBodyReader> iterator = readers.iterator();
        while (iterator.hasNext()) {
            final MessageBodyReader p = iterator.next();
            if (MbrModel.isReadable(p, c, t, as, mediaType)) {
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
    public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> c, Type t,
                                                         Annotation[] as,
                                                         MediaType mediaType) {
        return getMessageBodyWriter(c, t, as, mediaType, null);
    }

    @Override
    public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> c, Type t,
                                                         Annotation[] as,
                                                         MediaType mediaType,
                                                         PropertiesDelegate propertiesDelegate) {
        MessageBodyWriter<T> p = null;

        if (legacyProviderOrdering) {
            if (mediaType != null) {
                p = _getMessageBodyWriter(c, t, as, mediaType, mediaType, propertiesDelegate);
                if (p == null) {
                    p = _getMessageBodyWriter(c, t, as, mediaType, MediaTypes.getTypeWildCart(mediaType), propertiesDelegate);
                }
            }
            if (p == null) {
                p = _getMessageBodyWriter(c, t, as, mediaType, MediaTypes.GENERAL_MEDIA_TYPE, propertiesDelegate);
            }
        } else {
            p = _getMessageBodyWriter(c, t, as, mediaType, writers, propertiesDelegate);
        }

        return p;
    }

    @SuppressWarnings("unchecked")
    private <T> MessageBodyWriter<T> _getMessageBodyWriter(Class<T> c, Type t,
                                                           Annotation[] as,
                                                           MediaType mediaType,
                                                           List<MbwModel> models,
                                                           PropertiesDelegate propertiesDelegate) {

        // Making a new mediatype with parameters stripped. Otherwise the cache can fill up 
        // just because the mediatype objects have unique parameters
        MediaType mediaTypeKey = new MediaType(mediaType.getType(), mediaType.getSubtype()); 
        List<MbwModel> writers = mbwLookupCache.get(new ModelLookupKey(c, mediaTypeKey));
        if (writers == null) {

            writers = new ArrayList<MbwModel>();

            for (MbwModel model : models) {
                if (isCompatible(model, c, mediaType)) {
                    writers.add(model);
                }
            }
            Collections.sort(writers, new WorkerComparator<MessageBodyWriter>(c, mediaType));
            mbwLookupCache.put(new ModelLookupKey(c, mediaTypeKey), writers);
        }

        if (writers.isEmpty()) {
            return null;
        }

        final TracingLogger tracingLogger = TracingLogger.getInstance(propertiesDelegate);
        MessageBodyWriter<T> selected = null;
        final Iterator<MbwModel> iterator = writers.iterator();
        while (iterator.hasNext()) {
            final MbwModel model = iterator.next();
            if (model.isWriteable(c, t, as, mediaType)) {
                selected = (MessageBodyWriter<T>) model.provider;
                tracingLogger.log(MsgTraceEvent.MBW_SELECTED, selected);
                break;
            }
            tracingLogger.log(MsgTraceEvent.MBW_NOT_WRITEABLE, model.provider);
        }

        if (tracingLogger.isLogEnabled(MsgTraceEvent.MBW_SKIPPED)) {
            while (iterator.hasNext()) {
                final MbwModel model = iterator.next();
                tracingLogger.log(MsgTraceEvent.MBW_SKIPPED, model.provider);
            }
        }

        return selected;
    }

    @SuppressWarnings("unchecked")
    private <T> MessageBodyWriter<T> _getMessageBodyWriter(Class<T> c, Type t,
                                                           Annotation[] as,
                                                           MediaType mediaType, MediaType lookup,
                                                           PropertiesDelegate propertiesDelegate) {
        List<MessageBodyWriter> writers = writersCache.get(lookup);

        if (writers == null) {
            return null;
        }

        final TracingLogger tracingLogger = TracingLogger.getInstance(propertiesDelegate);
        MessageBodyWriter <T> selected = null;
        final Iterator<MessageBodyWriter> iterator = writers.iterator();
        while (iterator.hasNext()) {
            final MessageBodyWriter p = iterator.next();
            if (MbwModel.isWriteable(p, c, t, as, mediaType)) {
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

    private <T> void getCompatibleProvidersMap(MediaType mediaType,
                                               List<? extends WorkerModel<T>> set,
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
                                                List<? extends WorkerModel<T>> set,
                                                Map<MediaType, List<T>> subSet) {

        List<T> providers = new ArrayList<T>();

        for (WorkerModel<T> model : set) {
            if (model.types.contains(mediaType)) {
                providers.add(model.provider);
            }
        }

        if (!providers.isEmpty()) {
            subSet.put(mediaType, Collections.unmodifiableList(providers));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MediaType> getMessageBodyWriterMediaTypes(Class<?> c, Type t, Annotation[] as) {
        final Set<MediaType> writeableMediaTypes = Sets.newLinkedHashSet();

        for (MbwModel model : writers) {
            boolean writeableWorker = false;

            for (MediaType mt : model.types) {
                if (model.isWriteable(c, t, as, mt)) {
                    writeableMediaTypes.add(mt);
                    writeableWorker = true;
                }

                if (!writeableMediaTypes.contains(MediaType.WILDCARD_TYPE)
                        && writeableWorker
                        && model.types.contains(MediaType.WILDCARD_TYPE)) {
                    writeableMediaTypes.add(MediaType.WILDCARD_TYPE);
                }
            }
        }

        final List<MediaType> mtl = Lists.newArrayList(writeableMediaTypes);
        Collections.sort(mtl, MediaTypes.MEDIA_TYPE_COMPARATOR);
        return mtl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MessageBodyWriter> getMessageBodyWritersForType(final Class<?> clazz) {
        if (!mbwTypeLookupCache.containsKey(clazz)) {
            processMessageBodyWritersForType(clazz);
        }
        return mbwTypeLookupCache.get(clazz);
    }

    private void processMessageBodyWritersForType(final Class<?> clazz) {
        final List<WorkerModel<MessageBodyWriter>> suitableWriters = Lists.newArrayList();

        if (Response.class.isAssignableFrom(clazz)) {
            suitableWriters.addAll(writers);
        } else {
            for (final WorkerModel<MessageBodyWriter> workerPair : writers) {
                final Class<?> wrapped = Primitives.wrap(clazz);

                if (workerPair.providerClassParam == null
                        || workerPair.providerClassParam.isAssignableFrom(wrapped)
                        || workerPair.providerClassParam == clazz) {

                    suitableWriters.add(workerPair);
                }
            }
        }

        // Type -> MediaType.
        typeToMediaTypeWritersCache.put(clazz, getMessageBodyWorkersMediaTypesByType(suitableWriters));

        // Type -> Writer.
        Collections.sort(suitableWriters, WORKER_BY_TYPE_COMPARATOR);

        final List<MessageBodyWriter> writers = Lists.newArrayList();
        for (final WorkerModel<MessageBodyWriter> workerPair : suitableWriters) {
            writers.add(workerPair.provider);
        }
        mbwTypeLookupCache.put(clazz, writers);
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
    private <T> List<MediaType> getMessageBodyWorkersMediaTypesByType(final List<? extends WorkerModel<T>> workerModels) {
        final Set<MediaType> mediaTypeSet = Sets.newHashSet();
        for (final WorkerModel<T> model : workerModels) {
            mediaTypeSet.addAll(model.types);
        }

        final List<MediaType> mediaTypes = Lists.newArrayList(mediaTypeSet);
        Collections.sort(mediaTypes, MediaTypes.MEDIA_TYPE_COMPARATOR);
        return mediaTypes;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MessageBodyReader> getMessageBodyReadersForType(final Class<?> clazz) {
        if (!mbrTypeLookupCache.containsKey(clazz)) {
            processMessageBodyReadersForType(clazz);
        }

        return mbrTypeLookupCache.get(clazz);
    }

    private void processMessageBodyReadersForType(final Class<?> clazz) {
        final List<MbrModel> suitableReaders = Lists.newArrayList();

        for (MbrModel reader : readers) {
            final Class<?> wrapped = Primitives.wrap(clazz);

            if (reader.providerClassParam == null
                    || reader.providerClassParam.isAssignableFrom(wrapped)
                    || reader.providerClassParam == clazz) {
                suitableReaders.add(reader);
            }
        }

        // Type -> MediaType.
        typeToMediaTypeReadersCache.put(clazz, getMessageBodyWorkersMediaTypesByType(suitableReaders));

        // Type -> Writer.
        Collections.sort(suitableReaders, WORKER_BY_TYPE_COMPARATOR);

        final List<MessageBodyReader> readers = Lists.newArrayList();
        for (final MbrModel reader : suitableReaders) {
            readers.add(reader.provider);
        }
        mbrTypeLookupCache.put(clazz, readers);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MediaType getMessageBodyWriterMediaType(Class<?> c, Type t,
                                                   Annotation[] as, List<MediaType> acceptableMediaTypes) {
        for (MediaType acceptable : acceptableMediaTypes) {

            for (MbwModel model : writers) {
                for (MediaType mt : model.types) {
                    if (mt.isCompatible(acceptable)
                            && model.isWriteable(c, t, as, acceptable)) {
                        return MediaTypes.mostSpecific(mt, acceptable);
                    }
                }
            }

        }
        return null;
    }

    @Override
    public Object readFrom(Class<?> rawType,
                           Type type,
                           Annotation[] annotations,
                           MediaType mediaType,
                           MultivaluedMap<String, String> httpHeaders,
                           PropertiesDelegate propertiesDelegate,
                           InputStream entityStream,
                           Iterable<ReaderInterceptor> readerInterceptors,
                           boolean translateNce) throws WebApplicationException, IOException {
        ReaderInterceptorExecutor executor = new ReaderInterceptorExecutor(rawType, type, annotations, mediaType,
                httpHeaders, propertiesDelegate, entityStream, this, readerInterceptors, translateNce);
        final TracingLogger tracingLogger = TracingLogger.getInstance(propertiesDelegate);
        final long timestamp = tracingLogger.timestamp(MsgTraceEvent.RI_SUMMARY);
        try {
            Object instance = executor.proceed();
            if (!(instance instanceof Closeable) && !(instance instanceof Source)) {
                final InputStream stream = executor.getInputStream();
                if (stream != null) {
                    stream.close();
                }
            }

            return instance;
        } finally {
            tracingLogger.logDuration(MsgTraceEvent.RI_SUMMARY, timestamp, executor.getProcessedCount());
        }
    }

    @Override
    public OutputStream writeTo(Object t,
                                Class<?> rawType,
                                Type type,
                                Annotation[] annotations,
                                MediaType mediaType,
                                MultivaluedMap<String, Object> httpHeaders,
                                PropertiesDelegate propertiesDelegate,
                                OutputStream entityStream,
                                Iterable<WriterInterceptor> writerInterceptors)
            throws IOException, WebApplicationException {
        WriterInterceptorExecutor executor = new WriterInterceptorExecutor(t, rawType, type, annotations, mediaType,
                httpHeaders, propertiesDelegate, entityStream, this, writerInterceptors);
        final TracingLogger tracingLogger = TracingLogger.getInstance(propertiesDelegate);
        final long timestamp = tracingLogger.timestamp(MsgTraceEvent.WI_SUMMARY);
        try {
            executor.proceed();
        } finally {
            tracingLogger.logDuration(MsgTraceEvent.WI_SUMMARY, timestamp, executor.getProcessedCount());
        }

        return executor.getOutputStream();
    }
}
