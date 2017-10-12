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

HelloWorld OSGi Example
=======================

This example demonstrates how to develop a simple OSGi WAR bundle
containing a RESTful hello world web service

Contents
--------

The example WAR (see the `war-bundle` module) consists of two Jersey
resources:

`org.glassfish.jersey.examples.osgi.helloworld.resource.HelloWorldResource`

that produces a textual response to an HTTP GET

`org.glassfish.jersey.examples.osgi.helloworld.resource.AnotherResource`

that produces a different textual response to an HTTP GET. The
purpose of this resource is to show how to define multiple web
resources within a web application.

The mapping of the URI path space is presented in the following table:

URI path           | Resource class       | HTTP method
------------------ | -------------------- | -------------
**_/helloworld_**  | HelloWorldResource   | GET
**_/another_**     | AnotherResource      | GET

Running the Example
-------------------

To run the example, you would need to build the WAR file and install it
to an OSGi runtime (e.g. Apache Felix) together with other OSGi modules.
Look at the attached `functional-test` module for details on the
programatical runtime configuration. To build the war archive and run
the tests, you can just launch

>     mvn clean install
