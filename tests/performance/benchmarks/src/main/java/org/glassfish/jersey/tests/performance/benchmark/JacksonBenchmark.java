/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.performance.benchmark;

import java.util.concurrent.TimeUnit;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.test.util.server.ContainerRequestBuilder;
import org.glassfish.jersey.tests.performance.benchmark.entity.json.JacksonApplication;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Jackson Entity Filtering {@link org.glassfish.jersey.server.ApplicationHandler} benchmark.
 *
 * @author Michal Gajdos
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 16, time = 2500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 16, time = 2500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class JacksonBenchmark {

    @Param(value = {"projects/basic", "projects/detailed"})
    private String path;

    @Param(value = {"false", "true"})
    private String filtering;

    private volatile ApplicationHandler handler;
    private volatile ContainerRequest request;

    @Setup
    public void start() throws Exception {
        handler = new ApplicationHandler(new JacksonApplication(Boolean.valueOf(filtering)));
    }

    @Setup(Level.Iteration)
    public void request() {
        request = ContainerRequestBuilder
                .from(path, "GET")
                .build();
    }

    @TearDown
    public void shutdown() {
    }

    @Benchmark
    public ContainerResponse measureResource() throws Exception {
        return handler.apply(request).get();
    }

    public static void main(final String[] args) throws Exception {
        final Options opt = new OptionsBuilder()
                // Register our benchmarks.
                .include(JacksonBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
