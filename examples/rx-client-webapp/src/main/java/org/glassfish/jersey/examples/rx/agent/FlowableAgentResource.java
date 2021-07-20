/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.GenericType;

import javax.inject.Singleton;

import org.glassfish.jersey.client.rx.rxjava2.RxFlowableInvoker;
import org.glassfish.jersey.client.rx.rxjava2.RxFlowableInvokerProvider;
import org.glassfish.jersey.examples.rx.domain.AgentResponse;
import org.glassfish.jersey.examples.rx.domain.Calculation;
import org.glassfish.jersey.examples.rx.domain.Destination;
import org.glassfish.jersey.examples.rx.domain.Forecast;
import org.glassfish.jersey.examples.rx.domain.Recommendation;
import org.glassfish.jersey.server.Uri;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

/**
 * Obtain information about visited (destination) and recommended (destination, forecast, price) places for "RxJava2"
 * user. Uses RxJava2 Flowable and Jersey Client to obtain the data.
 *
 * @author Michal Gajdos
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@Singleton
@Path("agent/flowable")
@Produces("application/json")
public class FlowableAgentResource {

    @Uri("remote/destination")
    private WebTarget destination;

    @Uri("remote/calculation/from/{from}/to/{to}")
    private WebTarget calculation;

    @Uri("remote/forecast/{destination}")
    private WebTarget forecast;

    @GET
    public void flowable(@Suspended final AsyncResponse async) {
        final long time = System.nanoTime();
        final Queue<String> errors = new ConcurrentLinkedQueue<>();

        Flowable.just(new AgentResponse())
                // Obtain visited destinations.
                .zipWith(visited(errors), (agentResponse, visited) -> {
                    agentResponse.setVisited(visited);
                    return agentResponse;
                })
                // Obtain recommended destinations. (does not depend on visited ones)
                .zipWith(
                        recommended(errors),
                        (agentResponse, recommendations) -> {
                            agentResponse.setRecommended(recommendations);
                            return agentResponse;
                        })
                // Observe on another thread than the one processing visited or recommended destinations.
                .observeOn(Schedulers.io())
                // Subscribe.
                .subscribe(agentResponse -> {
                    agentResponse.setProcessingTime((System.nanoTime() - time) / 1000000);
                    async.resume(agentResponse);

                }, async::resume);
    }

    private Flowable<List<Destination>> visited(final Queue<String> errors) {
        destination.register(RxFlowableInvokerProvider.class);

        return destination.path("visited").request()
                          // Identify the user.
                          .header("Rx-User", "RxJava2")
                          // Reactive invoker.
                          .rx(RxFlowableInvoker.class)
                          // Return a list of destinations.
                          .get(new GenericType<List<Destination>>() {
                          })
                          // Handle Errors.
                          .onErrorReturn(throwable -> {
                              errors.offer("Visited: " + throwable.getMessage());
                              return Collections.emptyList();
                          });
    }

    private Flowable<List<Recommendation>> recommended(final Queue<String> errors) {
        destination.register(RxFlowableInvokerProvider.class);

        // Recommended places.
        final Flowable<Destination> recommended = destination.path("recommended").request()
                                                             // Identify the user.
                                                             .header("Rx-User", "RxJava2")
                                                             // Reactive invoker.
                                                             .rx(RxFlowableInvoker.class)
                                                             // Return a list of destinations.
                                                             .get(new GenericType<List<Destination>>() {
                                                             })
                                                             // Handle Errors.
                                                             .onErrorReturn(throwable -> {
                                                                 errors.offer("Recommended: " + throwable
                                                                         .getMessage());
                                                                 return Collections.emptyList();
                                                             })
                                                             // Emit destinations one-by-one.
                                                             .flatMap(Flowable::fromIterable)
                                                             // Remember emitted items for dependant requests.
                                                             .cache();

        forecast.register(RxFlowableInvokerProvider.class);

        // Forecasts. (depend on recommended destinations)
        final Flowable<Forecast> forecasts = recommended.flatMap(destination ->
                forecast
                        .resolveTemplate("destination", destination.getDestination())
                        .request().rx(RxFlowableInvoker.class).get(Forecast.class)
                        .onErrorReturn(throwable -> {
                            errors.offer("Forecast: " + throwable.getMessage());
                            return new Forecast(destination.getDestination(), "N/A");
                        }));

        calculation.register(RxFlowableInvokerProvider.class);

        // Calculations. (depend on recommended destinations)
        final Flowable<Calculation> calculations = recommended.flatMap(destination -> {
            return calculation.resolveTemplate("from", "Moon").resolveTemplate("to", destination.getDestination())
                              .request().rx(RxFlowableInvoker.class).get(Calculation.class)
                              .onErrorReturn(throwable -> {
                                  errors.offer("Calculation: " + throwable.getMessage());
                                  return new Calculation("Moon", destination.getDestination(), -1);
                              });
        });

        return Flowable.zip(recommended, forecasts, calculations, Recommendation::new).buffer(Integer.MAX_VALUE);
    }
}
