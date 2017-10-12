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

JAX-RS Types Injection Example
==============================

This example demonstrates injection of various runtime information into Jersey/JAX-RS resources.

A simple application consists of two simple resources: One is a typical
JAX-RS `@Path` annotated resource. The second resource is created
programmatically using the new Jeresy 2 programmatic resource binding
API. Both resources inject the same information and produce the report
that is returned in the response back to the client.

Contents
--------

The JAX-RS `@Path` annotated web resource is implemented by the

`org.glassfish.jersey.examples.jaxrstypeinjection.JaxrsInjectionReportingResource`

class. The programmatic resource is implemented by the

`org.glassfish.jersey.examples.jaxrstypeinjection.JaxrsInjectionReportingInflector` class.

The mapping of the URI path space is presented in the following table:

URI path                                                                                 | Resource class                     | HTTP methods
---------------------------------------------------------------------------------------- | ---------------------------------- | --------------
**_/annotated/instance/{p1}/{p2}?q1=<int_value>&q2=<string_value>&q2=<string_value>_**   | JaxrsInjectionReportingResource    | GET
**_/annotated/method/{p1}/{p2}?q1=<int_value>&q2=<string_value>&q2=<string_value>_**     | JaxrsInjectionReportingResource    | GET
**_/programmatic/{p1}/{p2}?q1=<int_value>&q2=<string_value>&q2=<string_value>_**         | JaxrsInjectionReportingInflector   | GET

Please note that the value of the `q1` URI query parameter is expected

Running the Example
-------------------

Run the example as follows:

>     mvn clean compile exec:java

This deploys the JAX-RS Types Injection Example using [Grizzly](http://grizzly.java.net/)

A [WADL description](http://wadl.java.net/#spec) may be accessed at the URL:

-   <http://localhost:8080/jaxrs-type-injection/application.wadl>

The resources could be accessed via

-   <http://localhost:8080/jaxrs-type-injection/annotated/instance/p1/p2?q1=123&q2=abc&q2=def>
-   <http://localhost:8080/jaxrs-type-injection/annotated/method/p1/p2?q1=123&q2=abc&q2=def>
-   <http://localhost:8080/jaxrs-type-injection/programmatic/p1/p2?q1=123&q2=abc&q2=def>
