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

Declarative Hyperlinking Example
================================

This example demonstrates how to use the declarative hyperlink
annotations.

Contents
--------

The example consists of one resource class and one representation class
along with a couple of model classes:

`org.glassfish.jersey.examples.linking.resources.ItemResource`

A resource class that produces a XML response to an HTTP GET.

`com.sun.jersey.samples.linking.representation.ItemRepresentation`

A JAXB representation class used to produce XML. This class also
contains @Link and @Ref annotations to produce hyperlinks in the
XML document.

The mapping of the URI path space is presented in the following table:

URI path               | Resource class   | HTTP methods
---------------------- | ---------------- | --------------
**_/items/{index}_**   | ItemResource     | GET

Sample Response
---------------

You can access resources of this application also using curl:

>     curl -v http://localhost:8080/items/1

In this example you should see an XML snippet with a next and a previous
link.

```xml
<item>
    <name>Item 1</name>
    <link href="http://localhost:8080/items/1" rel="self"/>
    <links>
        <link href="http://localhost:8080/items/2" rel="next"/>
        <link href="http://localhost:8080/items/0" rel="prev"/>
    </links>
</item>
```

Running the Example
-------------------

Run the example as follows:

>     mvn clean compile exec:java

This deploys current example using Grizzly. You can access the
application at:

-   <http://localhost:8080/items/0>\
-   <http://localhost:8080/items/1>\
-   <http://localhost:8080/items/2>\
-   <http://localhost:8080/items/3>\
-   <http://localhost:8080/items/4>\

Notice how the first has a self link, a next link but no previous link,
the second, third and fourth have all three and the last one only has a
self link and prev link.

Observe the HTTP headers included in the responses (e.g. via `curl -v`).
Notice that there is a HTTP Link header for next and previous when
appropriate.