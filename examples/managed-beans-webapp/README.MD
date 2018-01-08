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

Managed Beans Example
=====================

This example demonstrates managed bean support in Jersey. JAX-RS root
resource classes are annotated with @ManagedBean, which means EE-related
resources may be injected into instances of those classes.

Content
-------

The example application includes two root resource classes that are
treated as Java EE managed beans. One root resource class is managed in
the default JAX-RS life-cycle (one instance per request) and the other
is managed in the singleton life-cycle (one instance per web
application).

Two Java EE artifacts are injected into the singleton root resource. The
first is a resource constant defined in the web.xml. The second is an
entity manager factory to allow integration with JPA layer.

Running the Example
-------------------

This sample utilizes Java EE features in GlassFish application server.

The easiest way to get the application running is to build it and deploy
as follows:

    mvn clean package
    $AS_HOME/asadmin deploy target/managed-beans-webapp.war

From a web browser, visit:

-   <http://localhost:8080/managed-beans-webapp>
