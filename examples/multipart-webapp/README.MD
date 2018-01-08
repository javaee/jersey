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

Multipart Web app Example
=========================

This example demonstrates how to develop RESTful web service with
demonstrating JAX-RS Integration with MIME MultiPart Message Formats and
an EE 6 compliant Web container.

Contents
--------

The mapping of the URI path space is presented in the following table:

URI path                     | Description                                    | Sample request using curl
---------------------------- | ---------------------------------------------- | -----------------------------------------------------------------------------------------------
**_/form/part_**             | POST message returning entire string           | `curl -X POST -F "part=part1"  http://localhost:8080/multipart-webapp/form/part`
**_/form/part-file-name_**   | POST message returning part filename string.   | Be sure to execute this curl from project directory where pom.xml resides
                             |                                                | `curl -X POST -F "part=@pom.xml"  http://localhost:8080/multipart-webapp/form/part-file-name`
**_/form/xml-jaxb-part_**    | POST message returning xml jaxb part string.   | No curl sample available, please check test sources.

Running the Example
-------------------

You can run the example using Jetty as follows:

> `mvn clean package jetty:run`

Following steps are using [cURL](http://curl.haxx.se/) command line tool: