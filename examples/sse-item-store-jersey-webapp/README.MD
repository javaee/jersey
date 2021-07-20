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

SSE Item Store Jersey Example
=============================

An example demonstrating how Server Sent Events (SSE) Jersey support can
be used to notify clients about changes in server-side managed data
(collection of items). The example also outlines how SSE events can be
consumed using javascript browser-based clients.

Contents
--------

The example consists of a web-based client application and a server-side
application deployed on a [Jetty servlet container](http://www.eclipse.org/jetty/documentation/current/).

The server side part of the application consists of a JAX-RS resource
managing collection of string items and a simple HTML page that includes
a browser SSE client written in Javascript as well as a basic CSS
stylesheet. The SSE Javascript client connects to the JAX-RS resource
and transforms the streamed messages into HTML code that is rendered by
the browser. The javascript client also demonstrates how named and
unnamed SSE events are handled by HTML5 `EventSource` component. The
mapping of the URI path space of the server-side part of the application
is presented in the following table:

URI path                        |  Resource class      | HTTP methods
------------------------------- |  ------------------- | --------------
**_/resources/items_**          |  ItemStoreResource   | GET, POST
**_/resources/items/events_**   |  ItemStoreResource   | GET (SSE)

Application is configured to run using Jetty maven plugin under base
path `sse-item-store-webapp`.

Running the Example
-------------------

>     mvn clean compile jetty:run

The command above deploys the current example. After successful
deployment, you should be able to access the browser SSE client page at
<http://localhost:8080/sse-item-store-jersey-webapp/index.html>. To see the raw
SSE event stream, you may also point your browser directly at the
[`ItemStoreResource`](http://localhost:8080/sse-item-store-jersey-webapp/resources/items/events).

Deploying the example to another servlet container.
---------------------------------------------------

>     mvn clean package

The command above creates a Servlet 3.x compliant WAR located in the
target directory. The WAR can be then deployed to your Servlet 3.x
compliant container.

Running Test Client
-------------------

After deploying the application into a Servlet 3.x compliant container,
you can run the attached test by executing:

>     mvn test -Djersey.config.test.container.factory=org.glassfish.jersey.test.external.ExternalTestContainerFactory -Djersey.config.test.container.port=<port>
