<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.

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
    only if the new code is made subject to such option by the copyright
    holder.

-->

Managed Beans Example
=====================

This example demonstrates managed bean support in Jersey. JAX-RS root
resource classes are annotated with @ManagedBean, which means EE-related
resources may be injected into instances of those classes.

Content
-------

The example application includes two root resource classes that are
treated as Java EE managed beans. One root resource class is managed in
the default JAX-RS life-cycle (one instance per request) and the other
is managed in the singleton life-cycle (one instance per web
application).

Two Java EE artifacts are injected into the singleton root resource. The
first is a resource constant defined in the web.xml. The second is an
entity manager factory to allow integration with JPA layer.

Running the Example
-------------------

This sample utilizes Java EE features in GlassFish application server.

The easiest way to get the application running is to build it and deploy
as follows:

    mvn clean package
    $AS_HOME/asadmin deploy target/managed-beans-webapp.war

From a web browser, visit:

-   <http://localhost:8080/managed-beans-webapp>

Managed Client Example
======================

This example demonstrates a simple usage of Jersey Managed Client
feature.

Contents
--------

The mapping of the URI path space is presented in the following table:

URI path            | Resource class     | HTTP methods
------------------- | ------------------ | --------------
**_/public/a_**     | PublicResource     | GET
**_/public/b_**     | PublicResource     | GET
**_/internal/a_**   | InternalResource   | GET
**_/internal/b_**   | InternalResource   | GET

In the example, the requests to a *public resource* deployed on
`/public/` path are forwarded to an *internal resource* (deployed on `/internal/` path) using injected
[javax.ws.rs.client.WebTarget](https://jax-rs-spec.java.net/nonav/2.0/apidocs/javax/ws/rs/client/WebTarget.html)
instances produced using 2 separate managed clients each of the managed
clients using it's own custom configuration.

An access to internal resource methods is guarded by a container request
filter (`CustomHeaderFilter`) which rejects any request that does not
contain expected custom header set to an expected value. In the example,
the 2 managed clients (used by a public resource to access the methods
on the internal resource) are configured with custom configurations,
each containing a registration of a custom client request filter that is
instructed to append a required custom header and value to every
outgoing request. Only with managed client support working properly, the
public resource is able to successfully retrieve data from the internal
resource.

Running the Example
-------------------

Run the example as follows:

>     mvn clean compile exec:java

This deploys current example on the local host. You can then access WADL
description of the deployed application at
<http://localhost:8080/managedclient/application.wadl>.

You can access public resource of this application using curl:

>     curl -v -H "Accept: text/plain" http://localhost:8080/managedclient/public/a

>     curl -v -H "Accept: text/plain" http://localhost:8080/managedclient/public/b

In this example you should see the returned response message body
contains "a" or "b" respectively upon successful invocation.

You may also verify that access to internal resource is not possible
without including a proper header in the request. First try to access
the internal resource without any custom header:

>     curl -v -H "Accept: text/plain" http://localhost:8080/managedclient/internal/a

>     curl -v -H "Accept: text/plain" http://localhost:8080/managedclient/internal/b

In both cases a `HTTP 403 Forbidden.` response is returned. Now lets try
to access the resource once again, but this time we'll include also the
expected custom headers:

>     curl -v -H "Accept: text/plain" -H "custom-header:a" http://localhost:8080/managedclient/internal/a

>     curl -v -H "Accept: text/plain" -H "custom-header:b" http://localhost:8080/managedclient/internal/b

Finally, you should see the invocation succeeded and the returned response message body contains "a" or "b" respectively.