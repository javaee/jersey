/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.beanvalidation.webapp.resource;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

import org.glassfish.jersey.examples.beanvalidation.webapp.constraint.AtLeastOneContact;
import org.glassfish.jersey.examples.beanvalidation.webapp.constraint.HasId;
import org.glassfish.jersey.examples.beanvalidation.webapp.domain.ContactCard;
import org.glassfish.jersey.examples.beanvalidation.webapp.service.StorageService;

/**
 * Contact card basic resource class. Provides support for inserting, retrieving and deleting contact cards.
 * <p/>
 * See validation annotations (input method parameters, field, return values).
 *
 * @author Michal Gajdos
 */
@Path("contact")
@Produces("application/json")
public class ContactCardResource {

    @Context
    @NotNull
    private ResourceContext resourceContext;

    @POST
    @Consumes("application/json")
    @NotNull(message = "{contact.already.exist}")
    @HasId
    public ContactCard addContact(
            @NotNull @AtLeastOneContact(message = "{contact.empty.means}") @Valid
            final ContactCard contact) {
        return StorageService.addContact(contact);
    }

    @GET
    @NotNull
    @HasId
    public List<ContactCard> getContacts() {
        return StorageService.findByName("");
    }

    @GET
    @Path("{id}")
    @NotNull(message = "{contact.does.not.exist}")
    @HasId
    public ContactCard getContact(
            @DecimalMin(value = "0", message = "{contact.wrong.id}")
            @PathParam("id") final Long id) {
        return StorageService.get(id);
    }

    @DELETE
    @NotNull
    @HasId
    public List<ContactCard> deleteContacts() {
        return StorageService.clear();
    }

    @DELETE
    @Path("{id}")
    @NotNull(message = "{contact.does.not.exist}")
    @HasId
    public ContactCard deleteContact(
            @DecimalMin(value = "0", message = "{contact.wrong.id}")
            @PathParam("id") final Long id) {
        return StorageService.remove(id);
    }

    @Path("search/{searchType}")
    public SearchResource search() {
        return resourceContext.getResource(SearchResource.class);
    }
}
