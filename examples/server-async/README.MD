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

Server Asynchronous Example
===========================

The full description how to create an asynchronous resource can be found in Jersey User Guide, chapter 
[Asynchronous Services and Clients](https://jersey.java.net/documentation/latest/async.html).

This example demonstrates JAX-RS 2.0 server-side non-blocking API in comparison to blocking and long-running
asynchronous operations.

The  goal of the sample is to demonstrate that with limited I/O processing threads
on the server the synchronous execution of a long-running task may lead to resource
starvation caused by I/O processing threads being blocked by the long-running
operation, unable to serve new incoming requests.

OTOH, when the same long-running operation is executed asynchronously, the I/O
threads do not need to block while waiting for the long-running operation to finish
and the overall throughput of the system is greatly increased.

Contents
--------

The mapping of the URI path space is presented in the following table:

URI path                             | Resource class            | HTTP methods
------------------------------------ | --------------------------| --------------
**_async/messaging/fireAndForget_**  | FireAndForgetChatResource | POST1
**_async/messaging/fireAndForget_**  | FireAndForgetChatResource | GET
**_async/messaging/blocking_**       | BlockingPostChatResource  | POST
**_async/messaging/blocking_**       | BlockingPostChatResource  | GET
**_async/longrunning_**              | SimpleLongRunningResource | GET

Sample Response
---------------

This section shortly describes a main difference between blocking and non-blocking approach implemented
in the example.

#### Blocking Approach

First you have to sent POST request which is shown below and repeat this call 5-times.

>     curl -v -X POST http://localhost:8080/base/async/messaging/blocking -H "Content-Type: text/plain" -d "My message"

All previous requests were processed using I/O Container threads, there was no problem with
a returning response and a response's delay until now. But if you run the 6-th call, the server will
be blocked. At this time I/O thread is blocked (because of ArrayBlockingQueue is full) and waits for
the call which should read and remove one of the saved messages. Once the a below mentioned
request is called, the Blocking has place for another message and waiting thread is processed.

>     curl -v -X GET http://localhost:8080/base/async/messaging/blocking

#### Non-blocking Approach

We can start with the same procedure as in previous approach. Sent the POST request shown below 5-times

>     curl -v -X POST http://localhost:8080/base/async/messaging/fireAndForget -H "Content-Type: text/plain" -d "My message"

At this time blocking queue in resource should be full, but if you send another request, the client application won't
be blocked and will receive the given response immediately. Since the server application doesn't block the I/O thread
but blocks a thread which is dedicated for processing the request. After the GET call, the blocked dedicated thread
is processed and returned to a cached thread pool.

>     curl -v -X GET http://localhost:8080/base/async/messaging/fireAndForget

Running the Example
-------------------

Run the example as follows:

>     mvn clean compile exec:java

This deploys the example using [Grizzly](http://grizzly.java.net/) container.
