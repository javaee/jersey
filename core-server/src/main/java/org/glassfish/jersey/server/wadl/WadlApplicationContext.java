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

package org.glassfish.jersey.server.wadl;

import javax.ws.rs.core.UriInfo;

import javax.xml.bind.JAXBContext;

import org.glassfish.jersey.server.wadl.internal.ApplicationDescription;

import com.sun.research.ws.wadl.Application;

/**
 * A context to obtain WADL-based information.
 *
 * @author Paul Sandoz
 */
public interface WadlApplicationContext {
//    /**
//     * Get a WADL builder initiated with the configured {@link WadlGenerator}
//     * for the Web application.
//     *
//     * @return the WADL builder.
//     */
//    WadlBuilder getWadlBuilder();

//    /**
//     * Get a new instance of a  {@link ApplicationDescription} corresponding to all
//     * the root resource classes.
//     *
//     * @return the application description, the contents may be modified.
//     */
//    ApplicationDescription getApplication();

    /**
     * Get a new instance of a {@link ApplicationDescription} corresponding to all
     * the root resource classes, and configure the base URI.
     *
     * @param ui the URI information from which the base URI is set on the
     *           WADL application.
     * @param detailedWadl flag indicating whether or not detailed WADL should be generated.
     * @return the application description, the contents may be modified.
     */
    ApplicationDescription getApplication(UriInfo ui, boolean detailedWadl);


    /**
     * Get a new instance of {@link Application} for a particular resource.
     *
     * @param info     the URI information from which the base URI is set on the
     *                 WADL application.
     * @param resource the resource to build the Application for
     * @param detailedWadl flag indicating whether or not detailed WADL should be generated.
     * @return the application for this resource
     */
    Application getApplication(UriInfo info,
                               org.glassfish.jersey.server.model.Resource resource, boolean detailedWadl);

    /**
     * Get the default JAXB context associated with the {@link WadlGenerator}
     * for the Web application.
     *
     * @return the default JAXB context.
     */
    JAXBContext getJAXBContext();

//    /**
//     * Get the default JAXB context path to create a {@link JAXBContext}.
//     *
//     * @return the default JAXB context.
//     */
//    String getJAXBContextPath();

    /**
     * Enable/disable WADL generation.
     *
     * @param wadlGenerationEnabled if wadlGenerationEnabled is true and
     *                              {@link org.glassfish.jersey.server.ServerProperties#WADL_FEATURE_DISABLE}
     *                              is false, WADL generation is enabled. In all other cases is disabled.
     */
    void setWadlGenerationEnabled(boolean wadlGenerationEnabled);

    /**
     * Get WADL generation status.
     *
     * @return true when WADL generation is enabled. Does not take
     *         {@link org.glassfish.jersey.server.ServerProperties#WADL_FEATURE_DISABLE}
     */
    boolean isWadlGenerationEnabled();
}
