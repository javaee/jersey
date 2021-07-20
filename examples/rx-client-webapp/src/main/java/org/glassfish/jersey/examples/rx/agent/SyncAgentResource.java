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

package org.glassfish.jersey.examples.rx.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

import org.glassfish.jersey.examples.rx.domain.AgentResponse;
import org.glassfish.jersey.examples.rx.domain.Calculation;
import org.glassfish.jersey.examples.rx.domain.Destination;
import org.glassfish.jersey.examples.rx.domain.Forecast;
import org.glassfish.jersey.examples.rx.domain.Recommendation;
import org.glassfish.jersey.server.Uri;

/**
 * Obtain information about visited (destination) and recommended (destination, forecast, price) places for "Sync" user. Uses
 * standard JAX-RS sync approach to obtain the data.
 *
 * @author Michal Gajdos
 */
@Path("agent/sync")
@Produces("application/json")
public class SyncAgentResource {

    @Uri("remote/destination")
    private WebTarget destination;

    @Uri("remote/calculation/from/{from}/to/{to}")
    private WebTarget calculation;

    @Uri("remote/forecast/{destination}")
    private WebTarget forecast;

    @GET
    public AgentResponse sync() {
        final long time = System.nanoTime();

        final AgentResponse response = new AgentResponse();
        final Queue<String> errors = new ConcurrentLinkedQueue<>();

        // Obtain visited destinations.
        try {
            response.setVisited(destination.path("visited").request()
                    // Identify the user.
                    .header("Rx-User", "Sync")
                    // Return a list of destinations
                    .get(new GenericType<List<Destination>>() {}));
        } catch (final Throwable throwable) {
            errors.offer("Visited: " + throwable.getMessage());
        }

        // Obtain recommended destinations. (does not depend on visited ones)
        List<Destination> recommended = Collections.emptyList();
        try {
            recommended = destination.path("recommended").request()
                    // Identify the user.
                    .header("Rx-User", "Sync")
                    // Return a list of destinations.
                    .get(new GenericType<List<Destination>>() {});
        } catch (final Throwable throwable) {
            errors.offer("Recommended: " + throwable.getMessage());
        }

        // Forecasts. (depend on recommended destinations)
        final Map<String, Forecast> forecasts = new HashMap<>();
        for (final Destination dest : recommended) {
            try {
                forecasts.put(dest.getDestination(),
                        forecast.resolveTemplate("destination", dest.getDestination()).request().get(Forecast.class));
            } catch (final Throwable throwable) {
                errors.offer("Forecast: " + throwable.getMessage());
            }
        }

        // Calculations. (depend on recommended destinations)
        final Map<String, Calculation> calculations = new HashMap<>();
        recommended.stream().forEach(destination -> {
            try {
                calculations.put(destination.getDestination(), calculation.resolveTemplate("from", "Moon")
                        .resolveTemplate("to", destination.getDestination())
                        .request().get(Calculation.class));
            } catch (final Throwable throwable) {
                errors.offer("Calculation: " + throwable.getMessage());
            }
        });

        // Recommendations.
        final List<Recommendation> recommendations = new ArrayList<>(recommended.size());
        for (final Destination dest : recommended) {
            final Forecast fore = forecasts.get(dest.getDestination());
            final Calculation calc = calculations.get(dest.getDestination());

            recommendations.add(new Recommendation(dest.getDestination(),
                    fore != null ? fore.getForecast() : "N/A", calc != null ? calc.getPrice() : -1));
        }

        // Do something with errors.
        // ...

        response.setRecommended(recommendations);
        response.setProcessingTime((System.nanoTime() - time) / 1000000);
        return response;
    }
}
