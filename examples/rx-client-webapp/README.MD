<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.

    The contents of this file are subject to the terms of either the GNU
    General Public License Version 2 only ("GPL") or the Common Development
    and Distribution License("CDDL") (collectively, the "License").  You
    may not use this file except in compliance with the License.  You can
    obtain a copy of the License at
    https://oss.oracle.com/licenses/CDDL+GPL-1.1
    or LICENSE.txt.  See the License for the specific
    language governing permissions and limitations under the License.

    When distributing the software, include this License Header Notice in each
    file and include the License file at LICENSE.txt.

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
    only if the new code is made subject to such option by the copyright
    holder.

-->

Reactive Clients Jersey Reactive Client Extensions Example
==========================================================

This example demonstrates how data from multiple resources and in
various formats (json, xml) can be combined into a form suitable to ones
requirements. The example show and compares usage of standard JAX-RS
sync client, JAX-RS async client, Jersey's Observable (RxJava) client
extension, Jersey's CompletionStage (Java 8) client extension and
Jersey's ListenableFuture (Guava) client extension.

The application consists of two parts:

-   **"Remote"**, that can be considered to be deployed on a
    remote machine. In fact each resource from
    `org.glassfish.jersey.examples.rx.remote` package can be deployed on
    a separate remote machine as every resource represents a service of
    it's own.

-   **Note:** `DestinationResource` returns response entities as `JSON`
    while `CalculationResource` and `ForecastResource` as `XML`.

-   **"Agent"**, which is a synchronization/orchestration layer. This
    layer, also deployed as a server application, fetches data from all
    needed resources (using JAX-RS Client API) and combine
    them together. The combined result is then sent to the client that
    invoked the original request on the orchestration layer.

Every "agent" resource (package `org.glassfish.jersey.examples.rx.agent`)
invokes the following requests to the "remote" resource:

-   Obtain **visited** destinations for a user. (User identification is
    propagated via request header and basically everything about a user
    maximally simplified since it's out of scope of this example)

-   Obtain **recommended** destinations for a user. Requests obtaining
    **visited** and **recommended** destinations are independent and can
    be run in parallel.

-   Obtain weather **forecasts** for recommended destinations. New
    client request is invoked for every recommended destination.
    Obtaining weather **forecasts** depend on actual
    **recommended** destinations.

-   Obtain trip **calculations** (prices) for recommended destinations.
    New client request is invoked for every recommended destination.
    Obtaining trip **calculations** depend on actual
    **recommended** destinations. **forecast** and **calculation**
    requests are independent on each other and can be invoked
    in parallel.

Contents
--------

> Please note that the example is not fully implemented right now.
>
> Available "agent" resources are _/rx/agent/sync_, _/rx/agent/async_ and _/rx/agent/completion_.

The mapping of the URI path space is presented in the following table:

URI path                                           | Resource class                  | HTTP methods   | Allowed values
-------------------------------------------------- | ------------------------------- | -------------- | ------------------------------------------------------------------------------
**_/rx/agent/sync_**                               | SyncAgentResource               | GET            | returns JSON
**_/rx/agent/async_**                              | AsyncAgentResource              | GET            | returns JSON
**_/rx/agent/observable_** **(temporarily n/a)**   | ObservableAgentResource         | GET            | returns JSON
**_/rx/agent/completion_**                         | CompletionStageAgentResource    | GET            | returns JSON
**_/rx/agent/listenable_** **(temporarily n/a)**   | ListenableFutureAgentResource   | GET            | returns JSON
**_/rx/remote/destination/visited_**               | DestinationResource             | GET            | returns JSON
**_/rx/remote/destination/recommended_**           | DestinationResource             | GET            | returns JSON
**_/rx/remote/forecast/{destination}_**            | ForecastResource                | GET            | destination - name of a country; returns XML (random value)
**_/rx/remote/calculation/from/{from}/to/{to}_**   | CalculationResource             | GET            | from - name of a country, to - name of a country; returns XML (random value)

Application is Servlet 3 based, web.xml-less. Everything needed (resources/providers) is registered in `RxApplication`.

Sample Response
---------------

Agent responses look similar to the following one:

```javascript
{
  "visited" : [ {
    "destination" : "Antigua & Barbuda"
  }, {
    "destination" : "Guinea"
  }, {
    "destination" : "Malta"
  }, {
    "destination" : "Denmark"
  }, {
    "destination" : "Tajikistan"
  } ],
  "recommended" : [ {
    "destination" : "Bolivia",
    "forecast" : "Showers",
    "price" : 1359
  }, {
    "destination" : "Yemen",
    "forecast" : "Haze",
    "price" : 8032
  }, {
    "destination" : "Dominican Republic",
    "forecast" : "Cloudy",
    "price" : 1141
  }, {
    "destination" : "Korea South",
    "forecast" : "Mostly Sunny",
    "price" : 9853
  }, {
    "destination" : "Saudi Arabia",
    "forecast" : "Fog",
    "price" : 9063
  } ],
  "processingTime" : 877
}
```

As can be seen the response entity contains 3 main elements: **visited**
(list of visited destinations), **recommended** (list of recommended
destinations + weather forecast and price calculation) and
**processingTime** (describing how long it took to obtain previous two
elements).

Running the Example
-------------------

Run the example as follows:

>     mvn clean package jetty:run

This deploys current example using Jetty. You can access the application at:

-   <http://localhost:8080/rx/agent/sync>
-   <http://localhost:8080/rx/agent/async>
-   <http://localhost:8080/rx/agent/listenable> **(temporarily n/a)**
-   <http://localhost:8080/rx/agent/observable> **(temporarily n/a)**
-   <http://localhost:8080/rx/agent/completion>

Resources
---------

This examples is using the following (3-rd party) libraries:

**RxJava** by Netflix
-   [GitHub](https://github.com/ReactiveX/RxJava)
-   [GitHub.Wiki](https://github.com/ReactiveX/RxJava/wiki)
-   [JavaDoc](http://reactivex.io/RxJava/javadoc/)

**Guava** by Google
-   [Homepage](https://code.google.com/p/guava-libraries/)
-   [ListenableFuture
    Explained](https://code.google.com/p/guava-libraries/wiki/ListenableFutureExplained)
-   [JavaDoc](http://docs.guava-libraries.googlecode.com/git/javadoc/index.html?overview-summary.html)
