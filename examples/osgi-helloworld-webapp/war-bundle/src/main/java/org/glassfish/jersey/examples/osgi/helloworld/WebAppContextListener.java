/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.osgi.helloworld;

import java.util.HashMap;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * This is to make sure we signal the application has been deployed/un-deployed
 * via the OSGi EventAdmin service.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class WebAppContextListener implements BundleActivator, ServletContextListener {

    static EventAdmin ea;

    BundleContext bc;
    ServiceReference eaRef;

    static synchronized EventAdmin getEa() {
        return ea;
    }

    static synchronized void setEa(EventAdmin ea) {
        WebAppContextListener.ea = ea;
    }

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        if (getEa() != null) {
            final String contextPath = sce.getServletContext().getContextPath();
            getEa().sendEvent(new Event("jersey/test/DEPLOYED", new HashMap<String, String>() {{
                put("context-path", contextPath);
            }}));
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        if (getEa() != null) {
            getEa().sendEvent(new Event("jersey/test/UNDEPLOYED", new HashMap<String, String>() {{
                put("context-path", sce.getServletContext().getContextPath());
            }}));
        }
    }

    @Override
    public void start(BundleContext context) throws Exception {
        bc = context;
        eaRef = bc.getServiceReference(EventAdmin.class.getName());
        if (eaRef != null) {
            setEa((EventAdmin) bc.getService(eaRef));
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (eaRef != null) {
            setEa(null);
            bc.ungetService(eaRef);
        }
    }
}
