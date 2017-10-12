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

JSONP Example
=============

This example demonstrates how to use JSONP wrapper in order to serve not
only JSON but also a JavaScript expression representing a function call
with the original JSON data as a parameter. The technique is well
described e.g. at <http://en.wikipedia.org/wiki/JSON#JSONP>.

A simple web application is presented with just one resource: a change
record list. A client can obtain the list using XML or JSON or
JavaScript representation. The last change record is also available in
all three formats as a subresource of the previously mentioned list
resource.

Contents
--------

The change record list web resource is implemented by
`org.glassfish.jersey.examples.jsonp.ChangeListResource` class.

The mapping of the URI path space is presented in the following table:

URI path                | Resource class   | HTTP methods
----------------------- | ---------------- | --------------
**_/changes_**          | ChangeList       | GET
**_/changes/latest_**   | ChangeList       | GET

Running the Example
-------------------

Run the example as follows:

>     mvn clean compile exec:java

This deploys the JSONP example using [Grizzly](http://grizzly.java.net/)

A [WADL description](http://wadl.java.net/#spec) may be then accessed at the URL:

- <http://localhost:8080/jsonp/application.wadl>

Following steps are using [cURL](http://curl.haxx.se/) command line tool:

Get the list of changes in JSON format:

>     curl -HAccept:application/json http://localhost:8080/jsonp/changes

Get the list of changes in XML format:

>     curl -HAccept:application/xml http://localhost:8080/jsonp/changes

Get the list of changes as a JavaScript callback:

>     curl -HAccept:application/x-javascript http://localhost:8080/jsonp/changes

You can also see test classes at `src/main/test` subdirectory for detail
information how to consume the service using Jersey client API

### JavaScript Client

After starting the service and refreshing this page, a list of changes
should be displayed below:

`You need to run the service first!`