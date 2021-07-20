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

Resource Configuration Reload Example
=====================================

This example demonstrates how to create a simple Jersey application with
resource reloading capability using
[Grizzly HTTP Server container](http://grizzly.java.net).

Contents
--------

The mapping of the URI path space is presented in the following table:

URI path                    | Resource class                                            | Content                                          | HTTP methods
--------------------------- | --------------------------------------------------------- | ------------------------------------------------ | --------------
**_/flights/arrivals_**     | org.glassfish.jersey.examples.reload.ArrivalsResource     | Dummy Arrivals Information                       | GET
**_/flights/departures_**   | org.glassfish.jersey.examples.reload.DeparturesResource   | Dummy Departures Information                     | GET
**_/flights/stats_**        | org.glassfish.jersey.examples.reload.StatsResource        | Statistics on application resource utilization   | GET

The application gets configured via a plain text file named `resources`.
The file contains a list of resource classes to be published by the
application. After the application gets started, it watches the file system
and reloads the application as file updates are being detected.
Changes in JAX-RS resource source files and updates of the above mentioned resources file
are being detected. Java code re-compilation is done as required.

Running the Example
-------------------

Run the example as follows:

>     mvn clean compile exec:java

This deploys current example using[Grizzly
container](http://grizzly.java.net/). StatsResource should be available
at <http://localhost:8080/flights/stats>

You may want to check the other resources as well:

-   <http://localhost:8080/flights/departures>
-   <http://localhost:8080/flights/arrivals>

Updated information on number of hits should be available at
<http://localhost:8080/flights/stats>.
Now you can try to edit the `resources` file and comment the stats
resource out by using the `#` prefix. The point is you leave the
application running while editing the file. The changes wil get picked
up automatically. No need to manually restart the application. The
`resource` file content should now look like follows:

    org.glassfish.jersey.examples.reload.DeparturesResource
    org.glassfish.jersey.examples.reload.ArrivalsResource
    #org.glassfish.jersey.examples.reload.StatsResource

Once you save the change, the stats resource should not be available,
but the other, departures/arrivals, resources should remain functional.

Now you can revert the change back and check the stats resource is back
providing actual statistics information.

You can also try to change the JAX-RS resources source code. These changes
will be also picked up automatically.
