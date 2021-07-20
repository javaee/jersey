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

OAuth 2 client Google sample in WebApp deployment
=================================================

This example demonstrates how to build a simple web JAX-RS application
that can utilize JAX-RS client to query data from Google API using
OAuth2 for authentication and authorization. This sample works only with
browser as user interaction is needed to authorize the application for
accessing user's open Google Tasks.

Contents
--------

The mapping of the URI path space is presented in the following table:

URI path                    | Resource class         | HTTP method        | Description
--------------------------- | ---------------------- | ------------------ | -----------------------------------------------------------------
**_/index.html_**           |                        | GET                | contains for on which the user setups Client ID (application id).
**_/api/setup_**            | SetupResource          | GET                | Resource that initializes Client ID sent form from index.html
**_/api/tasks_**            | TaskResource           | GET                | Resource with Google tasks
**_/api/oauth2/authorize_** | AuthorizationResource  | GET                | Resource that will receive Authorization response from Google

How to setup the application
----------------------------

You need to have Google account and register into the Google API console
<https://code.google.com/apis/console>. Register a new project.

Under 'Services' enable 'Tasks API'. Then go to the 'API Access' and
create a new application. Choose Web Application as a type and define
the redirect\_uri. This you can define under 'Authorized Redirects
URIs'. Put there the URI to your deployment. As you will probably run
the app on the localhost, the URI may look for example like the
following:
"http://localhost:8080/oauth2-client-google-webapp/api/oauth2/authorize".
The URI must point to the AuthorizationResource.authorize resource (so,
it ends with /api/oauth2/authorize). If the URI is incorrectly entered
into the API console, the authorization request will fail and the user
will get an appropriate error message from Google (then go and fix the
URI in the console).

Your new application will get and 'Client ID' and 'Client Secret'. Do
not distribute these keys as they can be misused. You will need these
keys to setup the application on index.html.

Running the Example
-------------------

Run the example as follows:

>     mvn clean package jetty:run

This deploys current example using Jetty. Then go to
[http://localhost:8080/oauth2-client-google-webapp/](http://localhost:8080/oauth2-client-google-webapp/index.html)
and enter Client ID and Client Secret of your application. Then continue
to /api/tasks (following links), you will be redirected to the Google
Authorization URI. Grant an access to your application. You will then be
redirected back to "/api/oauth2/authorize" and authorization process
will be finished. Immediately, you will be redirected back to the
"/api/tasks". This time, the tasks will be get on the background from
Google API and returned as HTML to your page.
