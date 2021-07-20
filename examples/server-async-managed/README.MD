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

Server Asynchronous Managed Example
===================================

This example demonstrates JAX-RS 2.0 server-side non-blocking API using **ManagedAsync** annotation.

The full description how to create an asynchronous resource can be found in Jersey User Guide, chapter 
[Asynchronous Services and Clients](https://jersey.java.net/documentation/latest/async.html).

Indicates that the resource method to which the annotation has been applied
 * should be executed on a separate thread managed by an internal Jersey

The  goal of the sample is to demonstrate that with limited I/O processing threads
on the server the synchronous execution of a long-running task may lead to resource
starvation caused by I/O processing threads being blocked by the long-running
operation, unable to serve new incoming requests.

OTOH, when the same long-running operation is executed asynchronously, the I/O
threads do not need to block while waiting for the long-running operation to finish
and the overall throughput of the system is greatly increased.

- Look at the another example how to create a non-blocking API without **ManagedAsync**
annotation <https://github.com/jersey/jersey/tree/master/examples/server-async>
which also shown the way how to provide an immediate response for a client

Contents
--------

The mapping of the URI path space is presented in the following table:

URI path                        | Resource class            | HTTP methods
--------------------------------| --------------------------| --------------
**_chat_**                      | ChatResource              | POST
**_chat_**                      | ChatResource              | GET
**_managedasync/longrunning_**  | BlockingPostChatResource  | POST

Sample Response
---------------

This section shortly describes what really happens during the calls in this chat example.

We can start with POST request:
```
curl -v -X POST http://localhost:8080/base/chat -H "Content-Type: application/json" -d '{
    "author": "Anonymous Author",
    "message": "Secret Message"
}'
```

We can notice that we haven't got any response yet. The called method is annotated **@ManagedAsync**
which means that the I/O thread delegates a method processing to another Thread-Pool and is ready
to accept another incoming request. But the processing is still stop. Why? **AsyncResponse** was added
to the blocking queue and now it's waiting for GET call which will take the response and will finish the processing
of the preceding call.

```
curl -v -X GET http://localhost:8080/base/chat
```

Running the Example
-------------------

Run the example as follows:

>     mvn clean compile exec:java

This deploys the example using [Grizzly](http://grizzly.java.net/) container.
