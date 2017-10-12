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

HTTPS Client/Server Example - Grizzly
=====================================

This example demonstrates how to develop RESTful HTTPS Server using
Grizzly and how to implement HTTPS Client using Jersey with server
authentication.

Contents
--------

### Server side

This example consists of just one resource - RootResource, which is
basically copy of HelloWorld Resource from corresponding sample with
little improvement.

Other classes are used to start Grizzly embedded server and set up its
authentication and authorization mechanism and keystore and truststore.

### Client side

Client side is implemented as a test case, see class
**org.glassfish.jersey.examples.httpsclientservergrizzly.MainTest**, and
its method `testSSLWithAuth` (others are just tests for invalid
authorization). First thing you have to do if you want to communicate
with service via https is set up **SSLContext** which is basically
providing keystore and truststore. Keystore is used for storing own keys
and truststore is used for storing certificates to which you have
decided to trust. For more informations see [\[1\]](#JSSERefGuide).

To set **SSLContext** on Jersey client you have to set it as a property
to the client instance:

    Client client = ClientFactory.newClient();
    client.configuration().setProperty(ClientProperties.SSL_CONTEXT, context);

### Certificates setup

**These steps are not required to run this example. Pre-generated
keystore and truststore files are already present.**

We needed set up few things to get this example working:

-   generate client and server keys
-   generate client and server certificates
-   import certificates to corresponding truststores

Client certificate is needed too because we're going to use server-side
certificate authentication as well (yes, after this Http Basic
authentication seems to be kind of redundant but there are some usecases
where you might want to use them both).
Generate client key and store it into keystore:\

    keytool -genkey -keystore ./keystore_client -alias clientKey -dname "CN=Client, OU=Jersey, O=Oracle Corporation, L=Prague, ST=Czech Republic, C=CZ"

Generate client certificate (this will generate self-signed certificate;
if you have certification authority and want generate certificate
request, use keytool -certreq):

    keytool -export -alias clientKey -rfc -keystore ./keystore_client > ./client.cert

Import client certificate to servers truststore:

    keytool -import -alias clientCert -file ./client.cert -keystore ./truststore_server

These steps are similar for server side:

    keytool -genkey -keystore ./keystore_server -alias serverKey -dname "CN=localhost, OU=Jersey, O=Oracle Corporation, L=Prague, ST=Czech Republic, C=CZ"
    keytool -export -alias serverKey -rfc -keystore ./keystore_server > ./server.cert
    keytool -import -alias serverCert -file ./server.cert -keystore ./truststore_client

Running the Example
-------------------

Run the example as follows:

test

>     mvn clean test

run

>     mvn compile exec:java

From a web browser, visit:\
 **This won't work! \***

> `https://localhost:8463`

**\[\*\]** Your web browser needs have and use generated client keys. Or
you have to disable server side client authentication - set NeedClientAuth to false:
**new SSLEngineConfigurator(sslContext).setClientMode(false).setNeedClientAuth(false)** in *Server.java*.

Then ignore any security warning (self-signed certificates aren't trusted in general)
and login with username "user" and password "password". Text "JERSEY
HTTPS EXAMPLE" should appear.

Mozila Firefox and Internet Explorer don't allow users to display any
content provided on behalf of any self-signed certificate so you have to
use some other browser which allows this (for example Safari or Opera).

References
----------

\[1\]
<http://download.oracle.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html>
