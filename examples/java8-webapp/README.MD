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
    and therefore, elected the GPL Version 2 license, then the option applies
    only if the new code is made subject to such option by the copyright
    holder.

-->

Jersey and Java 8 Examples Features and Types
=============================================

This example demonstrates the usage of Java 8's new constructs and types.

Currently demonstrated are:

-   Default methods in interfaces - acting as JAX-RS Resource Methods

Contents
--------

The mapping of the URI path space is presented in the following table:

URI path                         | Resource class           | HTTP method   | description
-------------------------------- | ------------------------ | ------------- | ------------------------------------------
**_/j8/default-method_**         | DefaultMethodInterface   | GET           | returns `interface-root` in `text/plain`
**_/j8/default-method/path_**    | DefaultMethodInterface   | GET           | returns `interface-path` in `text/plain`
**_/j8/default-method/class_**   | DefaultMethodResource    | GET           | returns `class` in `text/plain`
**_/j8/lambdas/{p}_**            | LambdaResource           | GET           | returns `{p}-lambdaized` in `text/plain`

Application is Servlet 3 based, web.xml-less. Everything needed
(resources/providers) is registered in the `Java8Application` class.

Running the Example
-------------------

Run the example as follows:

>     mvn clean package jetty:run

This deploys current example using Jetty. You can access the application at:

-   <http://localhost:8080/j8/default-method>
-   <http://localhost:8080/j8/default-method/path>
-   <http://localhost:8080/j8/default-method/class>
-   <http://localhost:8080/j8/lambdas/test>
