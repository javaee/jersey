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

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.GenericType;

import org.glassfish.jersey.examples.rx.domain.AgentResponse;
import org.glassfish.jersey.examples.rx.domain.Calculation;
import org.glassfish.jersey.examples.rx.domain.Destination;
import org.glassfish.jersey.examples.rx.domain.Forecast;
import org.glassfish.jersey.examples.rx.domain.Recommendation;
import org.glassfish.jersey.internal.guava.ThreadFactoryBuilder;
import org.glassfish.jersey.server.Uri;

/**
 * Obtain information about visited (destination) and recommended (destination, forecast, price) places for
 * "CompletionStage" user. Uses Java 8 CompletionStage and Jersey Client to obtain the data.
 *
 * @author Michal Gajdos
 */
@Path("agent/completion")
@Produces("application/json")
public class CompletionStageAgentResource {

    @Uri("remote/destination")
    private WebTarget destinationTarget;

    @Uri("remote/calculation/from/{from}/to/{to}")
    private WebTarget calculationTarget;

    @Uri("remote/forecast/{destination}")
    private WebTarget forecastTarget;

    private final ExecutorService executor;

    public CompletionStageAgentResource() {
        executor = new ScheduledThreadPoolExecutor(20,
                new ThreadFactoryBuilder().setNameFormat("jersey-rx-client-completion-%d").build());
    }

    @GET
    public void completion(@Suspended final AsyncResponse async) {
        final long time = System.nanoTime();

        final Queue<String> errors = new ConcurrentLinkedQueue<>();

        CompletableFuture.completedFuture(new AgentResponse())
                .thenCombine(visited(destinationTarget, executor, errors), AgentResponse::visited)
                .thenCombine(recommended(destinationTarget, executor, errors), AgentResponse::recommended)
                .whenCompleteAsync((response, throwable) -> {
                    // Do something with errors.

                    response.setProcessingTime((System.nanoTime() - time) / 1000000);
                    async.resume(throwable == null ? response : throwable);
                });
    }

    private CompletionStage<List<Destination>> visited(final WebTarget destinationTarget,
                                                       final ExecutorService executor,
                                                       final Queue<String> errors) {
        return destinationTarget.path("visited").request()
                // Identify the user.
                .header("Rx-User", "CompletionStage")
                // Reactive invoker.
                .rx()
                // Return a list of destinations.
                .get(new GenericType<List<Destination>>() {})
                .exceptionally(throwable -> {
                    errors.offer("Visited: " + throwable.getMessage());
                    return Collections.emptyList();
                });
    }

    private CompletionStage<List<Recommendation>> recommended(final WebTarget destinationTarget,
                                                              final ExecutorService executor,
                                                              final Queue<String> errors) {
        // Recommended places.
        final CompletionStage<List<Destination>> recommended = destinationTarget.path("recommended")
                .request()
                // Identify the user.
                .header("Rx-User", "CompletionStage")
                // Reactive invoker.
                .rx()
                // Return a list of destinations.
                .get(new GenericType<List<Destination>>() {})
                .exceptionally(throwable -> {
                    errors.offer("Recommended: " + throwable.getMessage());
                    return Collections.emptyList();
                });

        return recommended.thenCompose(destinations -> {
            final WebTarget finalForecast = forecastTarget;
            final WebTarget finalCalculation = calculationTarget;

            List<CompletionStage<Recommendation>> recommendations = destinations.stream().map(destination -> {
                // For each destination, obtain a weather forecast ...
                final CompletionStage<Forecast> forecast =
                        finalForecast.resolveTemplate("destination", destination.getDestination())
                                     .request().rx().get(Forecast.class)
                                     .exceptionally(throwable -> {
                                         errors.offer("Forecast: " + throwable.getMessage());
                                         return new Forecast(destination.getDestination(), "N/A");
                                     });
                // ... and a price calculation
                final CompletionStage<Calculation> calculation = finalCalculation.resolveTemplate("from", "Moon")
                        .resolveTemplate("to", destination.getDestination())
                        .request().rx().get(Calculation.class)
                        .exceptionally(throwable -> {
                            errors.offer("Calculation: " + throwable.getMessage());
                            return new Calculation("Moon", destination.getDestination(), -1);
                        });

                //noinspection unchecked
                return CompletableFuture.completedFuture(new Recommendation(destination))
                        // Set forecast for recommended destination.
                        .thenCombine(forecast, Recommendation::forecast)
                        // Set calculation for recommended destination.
                        .thenCombine(calculation, Recommendation::calculation);
            }).collect(Collectors.toList());

            // Transform List<CompletionStage<Recommendation>> to CompletionStage<List<Recommendation>>
            return sequence(recommendations);
        });
    }

    private <T> CompletionStage<List<T>> sequence(final List<CompletionStage<T>> stages) {
        //noinspection SuspiciousToArrayCall
        final CompletableFuture<Void> done = CompletableFuture.allOf(stages.toArray(new CompletableFuture[stages.size()]));

        return done.thenApply(v -> stages.stream()
                        .map(CompletionStage::toCompletableFuture)
                        .map(CompletableFuture::join)
                        .collect(Collectors.<T>toList())
        );
    }

}
