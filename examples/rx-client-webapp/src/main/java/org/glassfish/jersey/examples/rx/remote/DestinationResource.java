/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.rx.remote;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import javax.inject.Singleton;

import org.glassfish.jersey.examples.rx.Helper;
import org.glassfish.jersey.examples.rx.domain.Destination;
import org.glassfish.jersey.internal.util.collection.Views;
import org.glassfish.jersey.server.ManagedAsync;

/**
 * Obtain a list of visited / recommended places for a given user.
 *
 * @author Michal Gajdos
 */
@Singleton
@Path("remote/destination")
@Produces("application/json")
public class DestinationResource {

    private static final Map<String, List<String>> VISITED = new HashMap<>();

    static {
        VISITED.put("Sync", Helper.getCountries(5));
        VISITED.put("Async", Helper.getCountries(5));
        VISITED.put("Guava", Helper.getCountries(5));
        VISITED.put("RxJava", Helper.getCountries(5));
        VISITED.put("RxJava2", Helper.getCountries(5));
        VISITED.put("CompletionStage", Helper.getCountries(5));
    }

    @GET
    @ManagedAsync
    @Path("visited")
    public List<Destination> visited(@HeaderParam("Rx-User") @DefaultValue("KO") final String user) {
        // Simulate long-running operation.
        Helper.sleep();

        if (!VISITED.containsKey(user)) {
            VISITED.put(user, Helper.getCountries(5));
        }

        return Views.listView(VISITED.get(user), Destination::new);
    }

    @GET
    @ManagedAsync
    @Path("recommended")
    public List<Destination> recommended(@HeaderParam("Rx-User") @DefaultValue("KO") final String user,
                                         @QueryParam("limit") @DefaultValue("5") final int limit) {
        // Simulate long-running operation.
        Helper.sleep();

        if (!VISITED.containsKey(user)) {
            VISITED.put(user, Helper.getCountries(5));
        }

        return Views.listView(Helper.getCountries(limit, VISITED.get(user)), Destination::new);
    }
}
