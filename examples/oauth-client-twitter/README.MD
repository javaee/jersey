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

OAuth 1 Client Sample - Twitter Client
======================================

This example demonstrates how to develop a simple web app client
utilizing JAX-RS client API and Jersey OAuth support.

Contents
--------

This example consists of the main class (`App`) and two JAXB beans used
for unmarshaling the responses from Twitter (`Status` and `User`).

Running the Example
-------------------

### Registering with Twitter

Before running the example, you have to register your own application
with Twitter (to obtain consumer key and consumer secret you have to use
to configure this client). You can do it as follows:

1.  Go to [Twitter Developers Page](http://dev.twitter.com/). You need
    to sign in to the Twitter (if you are new to Twitter you need to
    sign up). Then in the right upper corner you will see your profile
    photo and drop down list in which you select "My Application". Click
    on **Create a new application** button.

2.  Fill out the "Create an application" form - you have to pick a
    unique application name (e.g. app12345678), put in some description,
    put in something for app website (e.g. http://jersey.java.net),
    Accept terms by selecting **Yes, I agree** in "Twitter Content"
    checkbox, enter captcha.

3.  Click **Create your twitter application** button.

4.  You will be presented with consumer key, consumer secret and other
    details for your registered application.

### Running for the First Time

Enter the following on the command-line:

>    mvn clean compile exec:java -DconsumerKey=<consumer key assigned to your app> -DconsumerSecret=<consumer secret assigned to your app>

### Subsequent Runs

After you run the application for the first time, it stores the consumer
key, secret and token and token secret in a property file for future
use. So, for subsequent runs you don't have to specify these as system
properties. The following command is good enough for running the
application after it was built, run for the first time and the property
file was created:

>    mvn exec:java

Troubleshooting
---------------

### HTTP Proxy

Behind a HTTP proxy, you will need to setup the following system
properties to communicate with the Twitter server:

-   http.proxyHost - the name of the proxy host.
-   http.proxyPort - the port of the proxy host.
-   http.proxyAuth - the user name and password to use when connecting
    to the proxy; this string should be the user name and password
    separated by a colon (e.g., rickhall:mypassword).

These system properties can be set directly on the command line when
starting the JVM using the standard "-D&lt;prop&gt;=&lt;value&gt;"
syntax.
