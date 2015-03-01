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
package org.glassfish.jersey.tests.integration.jersey2137;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

/**
 * Request scoped transactional CDI bean registered as JAX-RS resource class.
 * Part of JERSEY-2137 reproducer. {@link javax.ws.rs.WebApplicationException}
 * thrown in the resource method below should drive the response as specified
 * in the JAX-RS spec.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@RequestScoped
@Transactional
@Path("cdi-transactional")
public class CdiTransactionalResource {

    @PersistenceContext(unitName = "Jersey2137PU")
    private EntityManager entityManager;

    @Path("{a}")
    @GET
    public String getBalance(@PathParam("a") long a) {
        final Account account = entityManager.find(Account.class, a);
        if (account == null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).entity(String.format("Account %d not found", a)).build());
        } else {
            return String.format("%d", account.getBalance());
        }
    }

    @Path("{a}")
    @PUT
    public void putBalance(@PathParam("a") long a, String balance) {
        final Account account = entityManager.find(Account.class, a);
        if (account == null) {
            Account newAccount = new Account();
            newAccount.setId(a);
            newAccount.setBalance(Long.decode(balance));
            entityManager.persist(newAccount);
        } else {
            account.setBalance(Long.decode(balance));
            entityManager.merge(account);
        }
    }

    @POST
    public String transferMoney(@QueryParam("from") long from, @QueryParam("to") long to, String amount) {

        final Account toAccount = entityManager.find(Account.class, to);

        if (toAccount != null) {
            try {
                toAccount.setBalance(toAccount.getBalance() + Long.decode(amount));
                entityManager.merge(toAccount);
                final Account fromAccount = entityManager.find(Account.class, from);
                fromAccount.setBalance(fromAccount.getBalance() - Long.decode(amount));
                if (fromAccount.getBalance() < 0) {
                    throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                            .entity("Transaction failed. Not enough money on the funding account.").build());
                }
                entityManager.merge(fromAccount);
                return "Transaction sucessful.";
            } catch (Exception e) {
                if (e instanceof WebApplicationException) {
                    throw (WebApplicationException) e;
                } else {
                    throw new WebApplicationException(
                            Response.status(Response.Status.BAD_REQUEST).entity("Something bad happened.").build());
                }
            }
        } else {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).entity("Target account not found.").build());
        }
    }
}
