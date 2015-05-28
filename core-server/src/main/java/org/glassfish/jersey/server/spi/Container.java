/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.spi;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.spi.Contract;

/**
 * Jersey container service contract.
 *
 * The purpose of the container is to configure and host a single Jersey
 * application.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 *
 * @see org.glassfish.jersey.server.ApplicationHandler
 */
@Contract
@ConstrainedTo(RuntimeType.SERVER)
public interface Container {

    /**
     * Default container port number for HTTP protocol.
     *
     * @since 2.18
     */
    public static final int DEFAULT_HTTP_PORT = 80;
    /**
     * Default container port number for HTTPS protocol.
     *
     * @since 2.18
     */
    public static final int DEFAULT_HTTPS_PORT = 443;

    /**
     * Return an immutable representation of the current {@link ResourceConfig
     * configuration}.
     *
     * @return current configuration of the hosted Jersey application.
     */
    public ResourceConfig getConfiguration();

    /**
     * Get the Jersey server-side application handler associated with the container.
     *
     * @return Jersey server-side application handler associated with the container.
     */
    public ApplicationHandler getApplicationHandler();

    /**
     * Reload the hosted Jersey application using the current {@link ResourceConfig
     * configuration}.
     */
    public void reload();

    /**
     * Reload the hosted Jersey application using a new {@link ResourceConfig
     * configuration}.
     *
     * @param configuration new configuration used for the reload.
     */
    public void reload(ResourceConfig configuration);
}
