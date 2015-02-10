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

package org.glassfish.jersey.server.monitoring;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Application MX Bean.
 *
 * @author Miroslav Fuksa
 */
public interface ApplicationMXBean {
    /**
     * Get the application name.
     *
     * @return Application name.
     */
    public String getApplicationName();

    /**
     * Get the {@link javax.ws.rs.core.Application application class} used for configuration of Jersey application.
     *
     * @return Application class name.
     */
    public String getApplicationClass();

    /**
     * Get the map of configuration properties converted to strings.
     *
     * @return Map property keys to property string values.
     */
    public Map<String, String> getProperties();

    /**
     * Get the start time of the application (when application was initialized).
     *
     * @return Application start time.
     */
    public Date getStartTime();

    /**
     * Get a set of string names of resource classes registered by the user.
     *
     * @return Set of classes full names (with package names).
     * @see org.glassfish.jersey.server.monitoring.ApplicationEvent#getRegisteredClasses() for specification
     *      of returned classes.
     */
    public Set<String> getRegisteredClasses();

    /**
     * Get a set of string names of classes of user registered instances.
     *
     * @return Set of user registered instances converted to their class full names (with package names).
     * @see org.glassfish.jersey.server.monitoring.ApplicationEvent#getRegisteredInstances()
     *      for specification of returned instances.
     */
    public Set<String> getRegisteredInstances();

    /**
     * Get classes of registered providers.
     *
     * @return Set of provider class full names (with packages names).
     * @see org.glassfish.jersey.server.monitoring.ApplicationEvent#getProviders() for specification
     *      of returned classes.
     */
    public Set<String> getProviderClasses();
}
