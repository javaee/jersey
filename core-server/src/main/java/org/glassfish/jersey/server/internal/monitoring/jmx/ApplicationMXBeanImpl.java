/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.internal.monitoring.jmx;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.monitoring.ApplicationMXBean;
import org.glassfish.jersey.server.monitoring.ApplicationInfo;

import jersey.repackaged.com.google.common.collect.Maps;
import jersey.repackaged.com.google.common.collect.Sets;

/**
 * MXBean implementing {@link org.glassfish.jersey.server.monitoring.ApplicationMXBean} MXBean interface.
 *
 * @author Miroslav Fuksa
 */
public class ApplicationMXBeanImpl implements ApplicationMXBean {

    private final String applicationName;
    private final String applicationClass;
    private final Map<String, String> configurationProperties;
    private final Date startTime;
    private final Set<String> providers;
    private final Set<String> registeredClasses;
    private final Set<String> registeredInstances;

    /**
     * Create a new application MXBean and register it to the mbean server using {@code mBeanExposer}.
     *
     * @param applicationInfo Application info which should be exposed.
     * @param mBeanExposer MBean exposer.
     * @param parentName {@link javax.management.ObjectName Object name} prefix of parent mbeans.
     */
    public ApplicationMXBeanImpl(final ApplicationInfo applicationInfo, final MBeanExposer mBeanExposer,
                                 final String parentName) {
        this.providers = Sets.newHashSet();
        this.registeredClasses = Sets.newHashSet();
        this.registeredInstances = Sets.newHashSet();

        for (final Class<?> provider : applicationInfo.getProviders()) {
            this.providers.add(provider.getName());
        }

        for (final Class<?> registeredClass : applicationInfo.getRegisteredClasses()) {
            this.registeredClasses.add(registeredClass.toString());
        }

        for (final Object registeredInstance : applicationInfo.getRegisteredInstances()) {
            this.registeredInstances.add(registeredInstance.getClass().getName());
        }

        final ResourceConfig resourceConfig = applicationInfo.getResourceConfig();
        this.applicationName = resourceConfig.getApplicationName();
        this.applicationClass = resourceConfig.getApplication().getClass().getName();
        this.configurationProperties = Maps.newHashMap();
        for (final Map.Entry<String, Object> entry : resourceConfig.getProperties().entrySet()) {
            final Object value = entry.getValue();
            String stringValue;
            try {
                stringValue = (value == null) ? "[null]" : value.toString();
            } catch (final Exception e) { // See JERSEY-2053: Sometimes toString() throws exception...
                stringValue = LocalizationMessages.PROPERTY_VALUE_TOSTRING_THROWS_EXCEPTION(
                        e.getClass().getName(), e.getMessage());
            }
            configurationProperties.put(entry.getKey(), stringValue);
        }
        this.startTime = new Date(applicationInfo.getStartTime().getTime());

        mBeanExposer.registerMBean(this, parentName + ",global=Configuration");
    }

    @Override
    public String getApplicationName() {
        return applicationName;
    }

    @Override
    public String getApplicationClass() {
        return applicationClass;
    }

    @Override
    public Map<String, String> getProperties() {
        return configurationProperties;
    }

    @Override
    public Date getStartTime() {
        return startTime;
    }

    @Override
    public Set<String> getRegisteredClasses() {
        return registeredClasses;
    }

    @Override
    public Set<String> getRegisteredInstances() {
        return registeredInstances;
    }

    @Override
    public Set<String> getProviderClasses() {
        return providers;
    }
}
