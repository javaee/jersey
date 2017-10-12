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

HTTPS Server Example - GlassFish
================================

This example demonstrates how to develop secure RESTful "Hello world"
web service with a Servlet 2.5 container.

Contents
--------

The example consists of just one Java class:

`org.glassfish.jersey.examples.https.glassfish.resources.HelloWorldResource`

A resource class that produces a textual response to an HTTP GET request.

The mapping of the URI path space is presented in the following table:

URI path            | Resource class       | HTTP methods
------------------- | -------------------- | --------------
**_/helloworld_**   | HelloWorldResource   | GET

Running the Example
-------------------

Run the example as follows:

Create war package

>     mvn clean package

Setup GlassFish 4.0 (installed at `AS_HOME=<install_dir>/glassfish4`):

Add new security realm called "`myRealm`"
-   Start GlassFish using asadmin `$AS_HOME/bin/asadmin start-domain`
-   Access Admin console, assuming the use of default admin port, at
    <http://localhost:4848>
-   Navigate in Admin console to:
    `Configurations -> server-config -> Security -> Realms`, click on
    "`New...`"
-   Create user and set password for user `myUser` (click on "`myRealm`"
    -&gt; "`Manage Users`" -&gt; "`New...`");
    -   **Ensure** "`User ID:`" set to "`myUser`" **and**
        "`Group List:`" set to "`Users`"
    -   Enter password **`myPass`** in "`New Password:`" and
        "`Confirm New Password:`" text fields
-   Add one other user name `joeUser` (click on "`myRealm`" -&gt;
    "`Manage Users`" -&gt; "`New...`");
    -   **Ensure** "`User ID:`" set to "`joeUser`" **and**
        "`Group List:`" set to "`Users`"
    -   Enter password **`joePass`** in "`New Password:`" and
        "`Confirm New Password:`" text fields

Check `web.xml` and `sun-web.xml` to understand how role mappings are
configured. [This
blog](http://blogs.oracle.com/bobby/entry/simplified_security_role_mapping)
is also helpful in helping to clarify role mappings.
Deploy `https-server-glassfish.war` as a Web Application
-   Admin console: select `Applications`, click on "`Deploy...`". Select
    file `./target/https-server-glassfish.war`
-   or you can use command line:
    `$AS_HOME/bin/asadmin deploy ./target/https-server-glassfish.war`

From a web browser, visit:

-   <https://localhost:8181/https-server-glassfish-webapp/helloworld>
-   Enter either Username `myUser` and Password `myPass` or, Username  `joeUser` and Password `joePass`
-   If entered Username `myUser` and Password `myPass`, verify that
    **expected output**: `Sending "Hello World" to user "myUser"` was displayed
-   If entered Username `joeUser` and Password `joePass`, verify that
    **expected output**: `Sending "Hello World" to user "joeUser"` was displayed

Running the Negative Example
----------------------------

Run the example as follows:

1.  Create war package

    >     mvn clean package

2.  setup GlassFish 4.0 (installed at
    `AS_HOME=<install_dir>/glassfish4`):

    -   Follow the steps described in the "**setup GlassFish 3.1**"
        section from the previous chapter "**Running the Example**" but
        instead of adding the user "`joeUser`" to the "`Group List:`" of
        "`Users`" set "`Group List:`" for this user to "`OtherUsers`"

3.  From a web browser, visit:

    -   <https://localhost:8181/https-server-glassfish-webapp/helloworld>
    -   Enter Username `joeUser` and Password `joePass`, verify that **expected output**
        looks like: `HTTP Status 403 - Access to the requested resource has been denied`
