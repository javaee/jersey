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

package org.glassfish.jersey.tests.integration.tracing;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
* @author Libor Kramolis (libor.kramolis at oracle.com)
*/
@Path("/root")
public class Resource {

    @Path("{name}")
    @GET
    public Message get(@PathParam("name") String name) {
        return new Message(new StringBuffer(name).reverse().toString());
    }

    @POST
    public Message post(Message post) {
        return new Message(new StringBuffer(post.getText()).reverse().toString());
    }

    @Path("sub-resource-method")
    @POST
    public Message postSub(Message post) {
        return new Message(new StringBuffer(post.getText()).reverse().toString());
    }

    @Path("sub-resource-locator")
    public SubResource getSubLoc() {
        return new SubResource();
    }

    @Path("sub-resource-locator-null")
    public SubResource getSubLocNull() {
        return null;
    }

    @GET
    @Path("runtime-exception")
    public Message getRuntimeException() {
        throw new RuntimeException("Something does not work ...");
    }

    @GET
    @Path("mapped-exception")
    public Message getMappedException() {
        throw new TestException("This could be client fault ...");
    }

}
