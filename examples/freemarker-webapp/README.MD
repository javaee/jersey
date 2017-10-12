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

Freemaker example
=================

This example demonstrates how to use Freemarker templating engine to
produce web pages via Jersey viewable support.

Contents
--------

The mapping of the URI path space is presented in the following table:

URI path                    | Resource class       | HTTP methods
--------------------------- | -------------------- | --------------
**_/hello_**                | FreemarkerResource   | GET
**_/autoTemplate_**         | FreemarkerResource   | GET
**_/helloWithoutSuffix_**   | FreemarkerResource   | GET

See FreemarkerResource for details on how to use jersey-freemaker-webapp
module for generating web pages.

If you want to use freemarker in your project, don't forget to set
**"jersey.config.freemarker.TemplateBasePath"** init param, which will
be pointing to base directory where your templates are stored
(FreemarkerTest class does that).

Running the Example
-------------------

Run the tests of the example as follows:

>     mvn clean test

This deploys this example using [Grizzly](http://grizzly.java.net/) and
executes the Jersey Test Framework using the unit test framework.

You can see test classes at `src/main/test` subdirectory for detailed
information how to consume the service using Client API.

Run the example as follows:

>     mvn clean package jetty:run

This deploys current example using Jetty. You can access the application
at <http://localhost:8080/freemarker-webapp/hello>