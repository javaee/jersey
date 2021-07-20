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

Simple Console Example
======================

This example demonstrates how to develop RESTful web services with the
embedded Grizzly server.

Contents
--------

The example consists of three Java classes and one HTML file:

`org.glassfish.jersey.examples.console.App`

Contains the main function which starts and stops the HTTP server.

`org.glassfish.jersey.examples.console.resources.Colours`

A resource class that can produce two different representations of a
list of colors. The list can be filtered by a client-supplied
query parameter. The resource references the Form resource using the
Path annotation declared on the Colours.getColours method.

`org.glassfish.jersey.examples.console.FormResource`

A resource class that produces a form in response to an HTTP GET,
processes the content of the form when submitted, and produces a
table of the submitted values in respose to the form being posted.

`form.html`

A static HTML file that is returned by the Form resource.

The mapping of the URI path space is presented in the following table:

URI path              | Resource class   | HTTP methods
--------------------- | ---------------- | --------------
**_/form_**           | Form             | GET, POST
**_/form/colours_**   | Colours          | GET

Running the Example
-------------------

Run the example as follows:

>     mvn clean compile exec:java

This deploys the example using [Grizzly](http://grizzly.java.net/) container. You can access the application at:

-   <http://127.0.0.1:9998/resources/form>

Fill in the form and submit it. Note that the pull-down list of colors
is populated dynamically. Enter part of a color name in the hint box to
see a filtered list of colors containing the hint.

You can see the complete list of colors at:

-   <http://127.0.0.1:9998/resources/form/colours>

Or see only those colors containing the letter 'r':

-   <http://127.0.0.1:9998/resources/form/colours?match=r>

A [WADL description](http://wadl.java.net/#spec) may be accessed at the URL:

-   <http://127.0.0.1:9998/resources/application.wadl>