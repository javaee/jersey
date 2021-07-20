/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.internal.spi;

import javax.ws.rs.core.FeatureContext;

/**
 * A service provider contract for JAX-RS and Jersey components that need to be automatically discovered and registered in
 * {@link javax.ws.rs.core.Configuration runtime configurations}.
 * <p/>
 * A component implementing this contract becomes auto-discoverable by adding a new entry with fully qualified name of its
 * implementation class name to a {@code org.glassfish.jersey.internal.spi.AutoDiscoverable} file in the {@code
 * META-INF/services} directory.
 * <p/>
 * Almost all Jersey {@code AutoDiscoverable} implementations have
 * {@link #DEFAULT_PRIORITY} {@link javax.annotation.Priority priority} set.
 *
 * @author Michal Gajdos
 */
public interface AutoDiscoverable {

    /**
     * Default common priority of Jersey build-in auto-discoverables.
     * Use lower number on your {@code AutoDiscoverable} implementation to run it before Jersey auto-discoverables
     * and vice versa.
     */
    public static final int DEFAULT_PRIORITY = 2000;

    /**
     * A call-back method called when an auto-discoverable component is to be configured in a given runtime configuration scope.
     * <p>
     * Note that as with {@link javax.ws.rs.core.Feature JAX-RS features}, before registering new JAX-RS components in a
     * given configurable context, an auto-discoverable component should verify that newly registered components are not
     * already registered in the configurable context.
     * </p>
     *
     * @param context configurable context in which the auto-discoverable should be configured.
     */
    public void configure(FeatureContext context);
}
