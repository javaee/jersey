/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server.scanning;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Configurable;

import javax.inject.Inject;

import org.glassfish.jersey.tests.e2e.server.scanning.ext.Ext1WriterInterceptor;
import org.glassfish.jersey.tests.e2e.server.scanning.ext.Ext2WriterInterceptor;
import org.glassfish.jersey.tests.e2e.server.scanning.ext.Ext3WriterInterceptor;
import org.glassfish.jersey.tests.e2e.server.scanning.ext.Ext4WriterInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
* @author Michal Gajdos (michal.gajdos at oracle.com)
*/
@Path("/")
public class Resource {

    private final Configurable config;

    @Inject
    public Resource(final Configurable config) {
        this.config = config;
    }

    @GET
    public String get() {
        assertEquals(1, config.getFeatures().size());
        assertEquals(2, config.getProviderClasses().size());
        assertEquals(2, config.getProviderInstances().size());

        assertEquals(CustomFeature.class, config.getFeatures().iterator().next().getClass());
        assertTrue(config.getProviderClasses().contains(Ext2WriterInterceptor.class));
        assertTrue(config.getProviderClasses().contains(Ext3WriterInterceptor.class));
        assertTrue(config.getProviderInstances().contains(Ext1WriterInterceptor.INSTANCE));
        assertTrue(config.getProviderInstances().contains(Ext4WriterInterceptor.INSTANCE));

        return "get";
    }
}
