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

Link Shortener MVC + Bean Validation Example
============================================

This example demonstrates a simple Link Shortener webapp demonstrating
Jersey MVC and Bean Validation capabilities. There is no validation on
the frontend side, every error displayed is recognized on the backend.

Contents
--------

The mapping of the URI path space is presented in the following table:

URI path          | Resource class          | HTTP methods   | Allowed values
----------------- | ----------------------- | -------------- | ----------------------------
**_/_**           | ShortenerResource       | GET, POST      |
**_/r/{link}_**   | ResolverResource        | GET            | link - shortened link part
**_/i/{link}_**   | PermanentLinkResource   | GET            | link - shortened link part

Application is configured by using web.xml, which registers
[javax.ws.rs.core.Application](https://jax-rs-spec.java.net/nonav/2.0/apidocs/javax/ws/rs/core/Application.html)
extension to get classes and singletons from it (see class `ShortenerApplication`).
MVC and Bean Validation annotations are present on resource classes (see `ShortenerResource`, `ResolverResource`,
`PermanentLinkResource`) as well as on the domain class (see `ShortenedLink`).

Running the Example
-------------------

Run the example as follows:

>     mvn clean package jetty:run

This deploys current example using Jetty. You can access the application at <http://localhost:8080/>

Resources
---------

This examples is using following (3-rd party) front-end frameworks:

**Mustache.java** by spullara - templating engine
-   [Homepage](http://mustache.github.io/)
-   [Homepage.Java](https://github.com/spullara/mustache.java)

**Bootstrap** by Twitter - front-end HTML/CSS/JS framework
-   [Homepage](http://twitter.github.com/bootstrap)
-   [Getting started](http://twitter.github.com/bootstrap/getting-started.html)
