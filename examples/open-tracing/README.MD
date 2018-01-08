<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.

    The contents of this file are subject to the terms of either the GNU
    General Public License Version 2 only ("GPL") or the Common Development
    and Distribution License("CDDL") (collectively, the "License").  You
    may not use this file except in compliance with the License.  You can
    obtain a copy of the License at
    http://glassfish.java.net/public/CDDL+GPL_1_1.html
    or packager/legal/LICENSE.txt.  See the License for the specific
    language governing permissions and limitations under the License.

    When distributing the software, include this License Header Notice in each
    file and include the License file at packager/legal/LICENSE.txt.

    GPL Classpath Exception:
    Oracle designates this particular file as subject to the "Classpath"
    exception as provided by Oracle in the GPL Version 2 section of the License
    file that accompanied this code.

    Modifications:
    If applicable, add the following below the License Header, with the fields
    enclosed by brackets [] replaced by your own identifying information:
    "Portions Copyright [year] [name of copyright owner]"

    Contributor(s):
    If you wish your version of this file to be governed by only the CDDL or
    only the GPL Version 2, indicate your decision by adding "[Contributor]
    elects to include this software in this distribution under the [CDDL or GPL
    Version 2] license."  If you don't indicate a single choice of license, a
    recipient has the option to distribute your version of this file under
    either the CDDL, the GPL Version 2 or to extend the choice of license to
    its licensees as provided above.  However, if you add GPL Version 2 code
    and therefore, elected the GPL Version 2 license, then the option applies
    and therefore, elected the GPL Version 2 license, then the option applies
    only if the new code is made subject to such option by the copyright
    holder.

-->

Jersey OpenTracing Example
==========================

This example demonstrates Jersey OpenTracing integration. The JAX-RS resource contains several resource method showing various
possibilities.

This particular example is configured with Jaeger as the GlobalTracer, but can be easily adjusted to use any other tracer
implementation.

Running the Example
-------------------
Example can be launched as is, but does not perform any interesting action out of the box.
To be able to visualise the traces, it is recommended to start Jaeger (and th UI) locally, e.g. in Docker:

>     docker run -d -p 5775:5775/udp -p 16686:16686 jaegertracing/all-in-one:travis-1278

Check the UI on [localhost:16686](http://localhost:16686), there should be no traces visible.

Run the example as follows:

>     mvn clean compile exec:java

This deploys the example using [Grizzly](http://grizzly.java.net/) container.

Try to access resources using the URIs listed bellow and look into Jager UI what traces are produced.
The first example should be visible in the Jaeger UI right after the example application is started. Others can be created by
doing rest calls to the exposed resource.

1. On start, the example application sends one request demonstrating more complex tracing
- jersey client (w/ registered OpenTracingFeature) creates the "jersey-client-GET" span
- the span is propagated to server (via http headers), jersey server (w/ registered OpenTracinfFeature) automatically creates a
 child span of the client span
- in the resource method, child span of the server span is manually created and propagated into managed client calls
- the managed clients are also tracing-enabled, so each one creates its own child span, that is propagated to the server, and
so on

2. No explicit (user-defined) spans.
- one automatically created "root" span, that measures the processing on the Jersey side and one "resource-level" span to be
used for logging events on the application level
- the "root" contains several tags with request and response metadata, such as request headers, status code, etc
- also, several technical Jersey-level events have been logged
- to see this simple case, call
>     curl localhost:8080/opentracing/resource/defaultTrace

3. Explicit logging into resource-level span
- same as above, but the "resource-level" span was resolved within the application logic, an event was logged and a tag was added
>     curl localhost:8080/opentracing/resource/appLevelLogging

- similar call with POST:
>     curl -X POST -d "Jersey Rocks" localhost:8080/opentracing/resource/appLevelPost


4. Explicit child span creation
- same as above, but instead of "resource-level" span, a child span was created and used for logging and adding tags
- note, that such a span needs to be finished manually, otherwise it won't propagate (the tracing info will be lost)
>     curl localhost:8080/opentracing/resource/childSpan


4. Client calls from the resource
- more complex case, the resource method invokes two client calls using managed client
- one request via managed client is triggered within the "provided", resource-level span
- child span is created and another request via manged client is triggered within the child span
- the span context needs to be propagated manually using the OpenTracingFeature.SPAN_CONTEXT_PROPERTY
- in both cases, the span context is propagated using http headers to the server and back to the client
- the child span (created in the resource) needs to be explicitly finished in the resource (or elsewhere, but needs to be finished)
>     curl localhost:8080/opentracing/resource/traceWithManagedClient

5. Asynchronous processing
- either basic case, but using asynchronous request processing
- there should be no practical difference visible in the tracing info
>     curl localhost:8080/opentracing/resource/async

6. Failing resource
- demonstrates exception thrown in the resource method
- Jaeger shows failed spans with the exclamation mark symbol and the exception is logged in the request span
>     curl localhost:8080/opentracing/resource/error