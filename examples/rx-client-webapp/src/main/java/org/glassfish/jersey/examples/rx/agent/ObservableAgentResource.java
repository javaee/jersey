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

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.GenericType;

import javax.inject.Singleton;

import org.glassfish.jersey.client.rx.RxWebTarget;
import org.glassfish.jersey.client.rx.rxjava.RxObservable;
import org.glassfish.jersey.client.rx.rxjava.RxObservableInvoker;
import org.glassfish.jersey.examples.rx.domain.AgentResponse;
import org.glassfish.jersey.examples.rx.domain.Calculation;
import org.glassfish.jersey.examples.rx.domain.Destination;
import org.glassfish.jersey.examples.rx.domain.Forecast;
import org.glassfish.jersey.examples.rx.domain.Recommendation;
import org.glassfish.jersey.server.Uri;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.schedulers.Schedulers;

/**
 * Obtain information about visited (destination) and recommended (destination, forecast, price) places for "RxJava" user. Uses
 * RxJava Observable and Jersey Client to obtain the data.
 *
 * @author Michal Gajdos
 */
@Singleton
@Path("agent/observable")
@Produces("application/json")
public class ObservableAgentResource {

    @Uri("remote/destination")
    private WebTarget destination;

    @Uri("remote/calculation/from/{from}/to/{to}")
    private WebTarget calculation;

    @Uri("remote/forecast/{destination}")
    private WebTarget forecast;

    @GET
    public void observable(@Suspended final AsyncResponse async) {
        final long time = System.nanoTime();

        Observable.just(new AgentResponse())
                // Obtain visited destinations.
                .zipWith(visited(), new Func2<AgentResponse, List<Destination>,  AgentResponse>() {
                    @Override
                    public AgentResponse call(final AgentResponse agentResponse, final List<Destination> destinations) {
                        agentResponse.setVisited(destinations);
                        return agentResponse;
                    }
                })
                // Obtain recommended destinations. (does not depend on visited ones)
                .zipWith(recommended(), new Func2<AgentResponse, List<Recommendation>, AgentResponse>() {
                    @Override
                    public AgentResponse call(final AgentResponse agentResponse, final List<Recommendation> recommendations) {
                        agentResponse.setRecommended(recommendations);
                        return agentResponse;
                    }
                })
                // Observe on another thread than the one processing visited or recommended destinations.
                .observeOn(Schedulers.io())
                // Subscribe.
                .subscribe(new Action1<AgentResponse>() {
                    @Override
                    public void call(final AgentResponse agentResponse) {
                        agentResponse.setProcessingTime((System.nanoTime() - time) / 1000000);
                        async.resume(agentResponse);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable throwable) {
                        async.resume(throwable);
                    }
                });
    }

    private Observable<List<Destination>> visited() {
        return RxObservable.from(destination).path("visited").request()
                // Identify the user.
                .header("Rx-User", "RxJava")
                // Reactive invoker.
                .rx()
                // Return a list of destinations.
                .get(new GenericType<List<Destination>>() {});
    }

    private Observable<List<Recommendation>> recommended() {
        // Recommended places.
        final Observable<Destination> recommended = RxObservable.from(destination).path("recommended").request()
                // Identify the user.
                .header("Rx-User", "RxJava")
                // Reactive invoker.
                .rx()
                // Return a list of destinations.
                .get(new GenericType<List<Destination>>() {})
                // Emit destinations one-by-one.
                .flatMap(new Func1<List<Destination>, Observable<Destination>>() {
                    @Override
                    public Observable<Destination> call(final List<Destination> destinations) {
                        return Observable.from(destinations);
                    }
                })
                // Remember emitted items for dependant requests.
                .cache();

        // Forecasts. (depend on recommended destinations)
        final RxWebTarget<RxObservableInvoker> rxForecast = RxObservable.from(forecast);
        final Observable<Forecast> forecasts = recommended.flatMap(new Func1<Destination, Observable<Forecast>>() {
            @Override
            public Observable<Forecast> call(final Destination destination) {
                return rxForecast.resolveTemplate("destination", destination.getDestination())
                        .request().rx().get(Forecast.class);
            }
        });

        // Calculations. (depend on recommended destinations)
        final RxWebTarget<RxObservableInvoker> rxCalculation = RxObservable.from(calculation);
        final Observable<Calculation> calculations = recommended.flatMap(new Func1<Destination, Observable<Calculation>>() {
            @Override
            public Observable<Calculation> call(final Destination destination) {
                return rxCalculation.resolveTemplate("from", "Moon").resolveTemplate("to", destination.getDestination())
                        .request().rx().get(Calculation.class);
            }
        });

        return Observable.zip(recommended, forecasts, calculations,
                new Func3<Destination, Forecast, Calculation, Recommendation>() {
                    @Override
                    public Recommendation call(final Destination destination,
                                               final Forecast forecast,
                                               final Calculation calculation) {
                        return new Recommendation(destination.getDestination(), forecast.getForecast(), calculation.getPrice());
                    }
                }).toList();
    }
}
