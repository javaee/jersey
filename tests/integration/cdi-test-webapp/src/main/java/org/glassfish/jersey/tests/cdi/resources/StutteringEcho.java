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
package org.glassfish.jersey.tests.cdi.resources;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.enterprise.context.ApplicationScoped;

/**
 * Echo implementation to stutter given input n-times.
 * The stutter factor could be set via JAX-RS interface.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@Stuttering
@ApplicationScoped
@Path("stutter-service-factor")
public class StutteringEcho implements EchoService {

    private static final int MIN_FACTOR = 2;
    private int factor = MIN_FACTOR;

    @Override
    public String echo(String s) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < factor; i++) {
            result.append(s);
        }
        return result.toString();
    }

    @PUT
    public void setFactor(String factor) {
        this.factor = ensureValidInput(factor);
    }

    @GET
    public String getFactor() {
        return Integer.toString(factor);
    }

    private int ensureValidInput(String factor) throws WebApplicationException {
        try {
            final int newValue = Integer.parseInt(factor);
            if (newValue < MIN_FACTOR) {
                throw createWebAppException(String.format("New factor can not be lesser then %d!", MIN_FACTOR));
            }
            return newValue;
        } catch (NumberFormatException nfe) {
            throw createWebAppException(String.format("Error parsing %s as an integer!", factor));
        }
    }

    private WebApplicationException createWebAppException(String message) {

        return new WebApplicationException(

                Response.status(Response.Status.BAD_REQUEST)
                        .type(MediaType.TEXT_PLAIN)
                        .entity(Entity.text(message)).build());
    }
}
