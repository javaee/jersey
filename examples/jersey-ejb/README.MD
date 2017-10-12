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

Jersey EJB Example
==================

This example demonstrates how to develop RESTful web service with a Servlet 3.0 and EJB 3.1 container.

Contents
--------

The mapping of the URI path space is presented in the following table:

URI path                                | Resource class             | HTTP methods   | Description
--------------------------------------- | -------------------------- | -------------- | ------------------------------------------------------------------------------------
**_/jersey-ejb_**                       | ---                        | GET            | Simple html/ajax client
**_/jersey-ejb/app/messages_**          | MessageBoardResourceBean   | GET            | Returns first ten messsages
**_/jersey-ejb/app/messages/{id}_**     | MessageBoardResourceBean   | GET            | Returns message with uniqueId = {id} or returns HTTP 404 if message does not exist
**_/jersey-ejb/app/messages/{id}_**     | MessageBoardResourceBean   | DELETE         | Deletes message with uniqueId = {id} or returns HTTP 404 if message does not exist
**_/jersey-ejb/app/messages/_**         | MessageBoardResourceBean   | POST           | Creates new message and allocates unique id for it (with message as request entity)

Running the Example
-------------------

This sample utilizes Java EE features in the GlassFish container

Build the application WAR file by executing maven 2 from the project directory:

>     mvn clean package

To deploy on a running GlassFish container, you would then typically need to launch:

>     $AS_HOME/bin/asadmin deploy target/jersey-ejb.war

Another option is to deploy using [GlassFish administrator console GUI](http://localhost:4848/)

You can access the application at:

-   <http://localhost:8080/jersey-ejb>
