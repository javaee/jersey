/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.feedcombiner.binder;

import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Singleton;

import org.glassfish.jersey.examples.feedcombiner.ApplicationProperties;
import org.glassfish.jersey.examples.feedcombiner.generator.IdGenerator;
import org.glassfish.jersey.examples.feedcombiner.generator.SequenceIdGenerator;
import org.glassfish.jersey.examples.feedcombiner.manager.FeedDataStoreManager;
import org.glassfish.jersey.examples.feedcombiner.manager.FeedTaskFactory;
import org.glassfish.jersey.examples.feedcombiner.manager.FeedTaskFactoryImpl;
import org.glassfish.jersey.examples.feedcombiner.model.CombinedFeed;
import org.glassfish.jersey.examples.feedcombiner.service.CombinedFeedService;
import org.glassfish.jersey.examples.feedcombiner.service.CrudService;
import org.glassfish.jersey.examples.feedcombiner.store.DataStoreObserver;
import org.glassfish.jersey.examples.feedcombiner.store.InMemoryDataStore;
import org.glassfish.jersey.examples.feedcombiner.store.ObservableDataStore;

import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class ApplicationBinder extends AbstractBinder {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationBinder.class.getName());

    private final Properties properties;

    private final ObservableDataStore datastore;

    private final boolean activeScheduler;

    public ApplicationBinder(ObservableDataStore datastore, String propertiesFileName) {
        this(datastore, propertiesFileName, true);
    }

    public ApplicationBinder(ObservableDataStore datastore, String propertiesFileName, boolean activeScheduler) {
        this.datastore = datastore;
        this.properties = getProperties(propertiesFileName);
        this.activeScheduler = activeScheduler;
    }

    private static Properties getProperties(String path) {
        try (InputStream inputStream = ApplicationBinder.class.getClassLoader().getResourceAsStream(path)) {
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        } catch (Exception e) {
            LOG.warn("Property file '" + path + "' not found in the classpath", e);
        }
        return null;
    }

    @Override
    protected void configure() {
        install(new PropertiesBinder(properties),
                new ResourcePartBinder(datastore));

        if (activeScheduler) {
            install(new SchedulerPartBinder(datastore, properties));
        }
    }

    public static class SchedulerPartBinder extends AbstractBinder {

        private final ObservableDataStore dataStore;

        private final Properties properties;

        public SchedulerPartBinder(ObservableDataStore datastore, Properties properties) {
            this.dataStore = datastore;
            this.properties = properties;
        }

        @Override
        protected void configure() {
            FeedTaskFactory feedTaskFactory = new FeedTaskFactoryImpl(dataStore);

            FeedDataStoreManager dataStoreManager;
            if (properties.containsKey(ApplicationProperties.SCHEDULER_POOL_SIZE)) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
                        Integer.parseInt((String) properties.get(ApplicationProperties.SCHEDULER_POOL_SIZE)));
                dataStoreManager = new FeedDataStoreManager(feedTaskFactory, scheduler);
            } else {
                dataStoreManager = new FeedDataStoreManager(feedTaskFactory);
            }

            dataStore.addObserver(dataStoreManager);

            bind(feedTaskFactory)
                    .to(FeedTaskFactory.class);

            bind(dataStoreManager)
                    .to(FeedDataStoreManager.class)
                    .to(DataStoreObserver.class);
        }

    }

    public static class ResourcePartBinder extends AbstractBinder {

        private final InMemoryDataStore datastore;

        public ResourcePartBinder(InMemoryDataStore datastore) {
            this.datastore = datastore;
        }

        @Override
        protected void configure() {
            bind(datastore)
                    .to(InMemoryDataStore.class);

            bind(CombinedFeedService.class)
                    .to(new TypeLiteral<CrudService<CombinedFeed>>() {})
                    .in(Singleton.class);

            bind(SequenceIdGenerator.class)
                    .to(IdGenerator.class)
                    .in(Singleton.class);
        }

    }

    public static class PropertiesBinder extends AbstractBinder {

        private final Properties properties;

        public PropertiesBinder(Properties properties) {
            this.properties = properties;
        }

        @Override
        protected void configure() {
            if (properties != null) {
                for (String name : properties.stringPropertyNames()) {
                    String value = properties.getProperty(name);
                    bind(value).to(String.class).named(name);
                }
            }
        }
    }
}
