/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.internal.inject;

import java.util.Map;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.uri.ExtendedUriInfo;

/**
 * A HttpContext makes it possible for a web resource implementation class to
 * access and manipulate HTTP request and response information directly. Typically
 * a HttpContext is injected on to a resource class using the
 * annotation {@link javax.ws.rs.core.Context}.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public interface HttpContext /*TODO keep or remove: extends Traceable*/ {
    /**
     * Get the extended URI information.
     * @return the extended URI information.
     */
    ExtendedUriInfo getUriInfo();

    /**
     * Get the HTTP request information.
     * @return the HTTP request information
     */
    Request getRequest();

    /**
     * Get the HTTP response information.
     * @return the HTTP response information
     */
    Response getResponse();

    /**
     * Get the mutable properties.
     * <p>
     * Care should be taken not to clear the properties or remove properties
     * that are unknown otherwise unspecified behavior may result.
     *
     * @return the properties.
     */
    Map<String, Object> getProperties();
}