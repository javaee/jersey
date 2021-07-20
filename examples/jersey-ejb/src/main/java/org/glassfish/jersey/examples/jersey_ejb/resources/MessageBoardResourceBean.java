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

package org.glassfish.jersey.examples.jersey_ejb.resources;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.glassfish.jersey.examples.jersey_ejb.entities.Message;
import org.glassfish.jersey.examples.jersey_ejb.exceptions.CustomNotFoundException;

/**
 * A stateless EJB bean to handle REST requests to the messages resource.
 * Messages are stored in the injected EJB singleton instance.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@Stateless
public class MessageBoardResourceBean {

    @Context
    private UriInfo ui;
    @EJB
    MessageHolderSingletonBean singleton;

    /**
     * Returns a list of all messages stored in the internal message holder.
     */
    @GET
    public List<Message> getMessages() {
        return singleton.getMessages();
    }

    @POST
    public Response addMessage(String msg) throws URISyntaxException {
        Message m = singleton.addMessage(msg);

        URI msgURI = ui.getRequestUriBuilder().path(Integer.toString(m.getUniqueId())).build();

        return Response.created(msgURI).build();
    }

    @Path("{msgNum}")
    @GET
    public Message getMessage(@PathParam("msgNum") int msgNum) {
        Message m = singleton.getMessage(msgNum);

        if (m == null) {
            // This exception will be passed through to the JAX-RS runtime
            // No other runtime exception will behave this way unless the
            // exception is annotated with javax.ejb.ApplicationException
            throw new NotFoundException();
        }

        return m;

    }

    @Path("{msgNum}")
    @DELETE
    public void deleteMessage(@PathParam("msgNum") int msgNum) throws CustomNotFoundException {
        boolean deleted = singleton.deleteMessage(msgNum);

        if (!deleted) {
            // This exception will be mapped to a 404 response
            throw new CustomNotFoundException();
        }
    }
}





