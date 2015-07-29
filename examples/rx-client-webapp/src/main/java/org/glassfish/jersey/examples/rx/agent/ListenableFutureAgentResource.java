/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.rx.agent;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.GenericType;

import org.glassfish.jersey.client.rx.RxWebTarget;
import org.glassfish.jersey.client.rx.guava.RxListenableFuture;
import org.glassfish.jersey.client.rx.guava.RxListenableFutureInvoker;
import org.glassfish.jersey.examples.rx.domain.AgentResponse;
import org.glassfish.jersey.examples.rx.domain.Calculation;
import org.glassfish.jersey.examples.rx.domain.Destination;
import org.glassfish.jersey.examples.rx.domain.Forecast;
import org.glassfish.jersey.examples.rx.domain.Recommendation;
import org.glassfish.jersey.server.ManagedAsync;
import org.glassfish.jersey.server.Uri;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Obtain information about visited (destination) and recommended (destination, forecast, price) places for "RxJava" user. Uses
 * RxJava Observable and Jersey Client to obtain the data.
 *
 * @author Michal Gajdos
 */
@Path("agent/listenable")
@Produces("application/json")
public class ListenableFutureAgentResource {

    @Uri("remote/destination")
    private WebTarget destination;

    @Uri("remote/calculation/from/{from}/to/{to}")
    private WebTarget calculation;

    @Uri("remote/forecast/{destination}")
    private WebTarget forecast;

    @GET
    @ManagedAsync
    public void listenable(@Suspended final AsyncResponse async) {
        final long time = System.nanoTime();
        final AgentResponse response = new AgentResponse();

        // Fallback.
        Futures.addCallback(Futures.successfulAsList(Arrays.asList(visited(response), recommended(response))),
                new FutureCallback<List<AgentResponse>>() {
                    @Override
                    public void onSuccess(final List<AgentResponse> result) {
                        response.setProcessingTime((System.nanoTime() - time) / 1000000);
                        async.resume(response);
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        async.resume(t);
                    }
                });
    }

    private ListenableFuture<AgentResponse> visited(final AgentResponse response) {
        final ListenableFuture<List<Destination>> visited = RxListenableFuture.from(destination).path("visited").request()
                // Identify the user.
                .header("Rx-User", "Guava")
                        // Reactive invoker.
                .rx()
                        // Return a list of destinations.
                .get(new GenericType<List<Destination>>() {});
        return Futures.transform(visited, new AsyncFunction<List<Destination>, AgentResponse>() {
            @Override
            public ListenableFuture<AgentResponse> apply(final List<Destination> input) throws Exception {
                final SettableFuture<AgentResponse> future = SettableFuture.create();

                response.setVisited(input);
                future.set(response);

                return future;
            }
        });
    }

    private ListenableFuture<AgentResponse> recommended(final AgentResponse response) {
        // Destinations.
        final ListenableFuture<List<Destination>> recommended = RxListenableFuture.from(destination).path("recommended").request()
                // Identify the user.
                .header("Rx-User", "Guava")
                        // Reactive invoker.
                .rx()
                        // Return a list of destinations.
                .get(new GenericType<List<Destination>>() {});
        final ListenableFuture<List<Recommendation>> recommendations = Futures.transform(recommended,
                new AsyncFunction<List<Destination>, List<Recommendation>>() {
                    @Override
                    public ListenableFuture<List<Recommendation>> apply(final List<Destination> input) throws Exception {
                        final List<Recommendation> recommendations = Lists.newArrayList(Lists.transform(input,
                                new Function<Destination, Recommendation>() {
                                    @Override
                                    public Recommendation apply(final Destination input) {
                                        return new Recommendation(input.getDestination(), null, 0);
                                    }
                                }));
                        return Futures.immediateFuture(recommendations);
                    }
                });

        final ListenableFuture<List<List<Recommendation>>> syncedFuture = Futures.successfulAsList(Arrays.asList(
                // Add Forecasts to Recommendations.
                forecasts(recommendations),
                // Add Forecasts to Recommendations.
                calculations(recommendations)));

        return Futures.transform(syncedFuture, new AsyncFunction<List<List<Recommendation>>, AgentResponse>() {
            @Override
            public ListenableFuture<AgentResponse> apply(final List<List<Recommendation>> input) throws Exception {
                response.setRecommended(input.get(0));
                return Futures.immediateFuture(response);
            }
        });
    }

    private ListenableFuture<List<Recommendation>> forecasts(final ListenableFuture<List<Recommendation>> recommendations) {
        return Futures.transform(recommendations,
                new AsyncFunction<List<Recommendation>, List<Recommendation>>() {
                    @Override
                    public ListenableFuture<List<Recommendation>> apply(final List<Recommendation> input) throws Exception {
                        final RxWebTarget<RxListenableFutureInvoker> rxForecast = RxListenableFuture.from(forecast);
                        return Futures.successfulAsList(Lists.transform(input,
                                new Function<Recommendation, ListenableFuture<Recommendation>>() {
                                    @Override
                                    public ListenableFuture<Recommendation> apply(final Recommendation r) {
                                        return Futures.transform(
                                                rxForecast.resolveTemplate("destination", r.getDestination()).request().rx()
                                                        .get(Forecast.class),
                                                new AsyncFunction<Forecast, Recommendation>() {
                                                    @Override
                                                    public ListenableFuture<Recommendation> apply(final Forecast f)
                                                            throws Exception {
                                                        r.setForecast(f.getForecast());
                                                        return Futures.immediateFuture(r);
                                                    }
                                                });
                                    }
                                }));
                    }
                });
    }

    private ListenableFuture<List<Recommendation>> calculations(final ListenableFuture<List<Recommendation>> recommendations) {
        return Futures.transform(recommendations,
                new AsyncFunction<List<Recommendation>, List<Recommendation>>() {
                    @Override
                    public ListenableFuture<List<Recommendation>> apply(final List<Recommendation> input) throws Exception {
                        final RxWebTarget<RxListenableFutureInvoker> rxCalculations = RxListenableFuture.from(calculation);
                        return Futures.successfulAsList(Lists.transform(input,
                                new Function<Recommendation, ListenableFuture<Recommendation>>() {
                                    @Override
                                    public ListenableFuture<Recommendation> apply(final Recommendation r) {
                                        return Futures.transform(
                                                rxCalculations.resolveTemplate("from", "Moon")
                                                        .resolveTemplate("to", r.getDestination()).request().rx()
                                                        .get(Calculation.class),
                                                new AsyncFunction<Calculation, Recommendation>() {
                                                    @Override
                                                    public ListenableFuture<Recommendation> apply(final Calculation c)
                                                            throws Exception {
                                                        r.setPrice(c.getPrice());
                                                        return Futures.immediateFuture(r);
                                                    }
                                                });
                                    }
                                }));
                    }
                });
    }
}
