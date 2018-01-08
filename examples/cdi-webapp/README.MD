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

Jersey CDI Integration CDI Beans WebApp Example
===============================================

This example demonstrates how to develop RESTful web service with CDI
managed beans and a Servlet 3.0 Web container.

Contents
--------

The mapping of the URI path space is presented in the following table:

URI path                              | Description                                                                                 | Expected Results
------------------------------------- | ------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------
**_/cdi-webapp/helloworld_**          | A managed bean with no use of injection whatsoever                                          | Hello World
**_/cdi-webapp/singleton_**           | Shows injection of context objects into the fields of a managed bean.                       | OK GET /cdi-webapp/singleton
**_/cdi-webapp/singleton/counter_**   | Shows injection of context objects into the fields of a managed bean.                       | standalone Grizzly (no resource injection support) - 0 initial increment value, that gets incremented with each other request
                                      |                                                                                             | Java EE compliant AS - 42 initial increment value
**_/cdi-webapp/other/C/D_**           | Shows injection of context objects and path parameters into the fields of a managed bean.   | INTERCEPTED: OK GET /cdi-webapp/other, c=C, d=D
**_/cdi-webapp/echofield/b?a=a_**     | Shows injection of path and query parameters into the fields of a managed bean.             | ECHO a b
**_/cdi-webapp/echo/a_**              | A managed bean that uses (but does not inject) a path parameter.                            | ECHO a

Running the Example
-------------------

This example should work on any Java EE 7 compliant application server.
It has been tested on a standalone GlassFish 4 instance. The easiest way
to get the application running there is to build it and deploy as
follows:

    mvn clean package
    $AS_HOME/asadmin deploy target/cdi-webapp.war

Another option, introduced in Jersey 2.15, is to run this example in
Grizzly HTTP server. To get the application running there you just
invoke the following command:

    mvn clean compile exec:java

Since Weld Servlet support is provided also for Apache Tomcat server,
there is yet another way how to deploy the application. Use the
`tomcat-packaging` maven profile to get the WAR archive packaged in a
way that makes it ready for Tomcat 7+ deployment:

    mvn -Ptomcat-packaging clean package
    cp target/cdi-webapp.war $CATALINA_HOME/webapps

After you successfully deploy the application, visit the following URLs:

-   <http://localhost:8080/cdi-webapp/helloworld>
-   <http://localhost:8080/cdi-webapp/singleton>
-   <http://localhost:8080/cdi-webapp/singleton/counter>
-   <http://localhost:8080/cdi-webapp/other/C/D>
-   <http://localhost:8080/cdi-webapp/echofield/b?a=a>
-   <http://localhost:8080/cdi-webapp/echo/a>