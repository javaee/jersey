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

Server-Sent Events (SSE) Example
===================================

This example demonstrates JAX-RS 2.0 server-sent events support sometimes also called one-way publish-subscribe model.

The full description how to create a support for server-sent events can be found in Jersey User Guide, chapter
[Server-Sent Events (SSE) Support](https://jersey.java.net/documentation/latest/sse.html).

It is highly recommended to look at details of SSE API Specification on
<http://www.w3.org/TR/2009/WD-eventsource-20091029/>

Contents
--------

The mapping of the URI path space is presented in the following table:

URI path                              | Resource class             | HTTP methods   | Description
------------------------------------- | -------------------------- | -------------- | ------------
**_server-sent-events_**              | ServerSentEventsResource   | GET            | Get entire EventOutput with all messages
**_server-sent-events_**              | ServerSentEventsResource   | POST           | Insert a new message in EventOutput
**_server-sent-events_**              | ServerSentEventsResource   | DELETE         | Reset EventOutput
**_server-sent-events/domains/{id}_** | BlockingPostChatResource   | POST           | Generate several messages with a delay in EventOutput and return it

Sample Response
---------------

A great example of Server-Sent Events is `server-sent-events/domains/{id}` which sends several messages with a delay 200ms
between each other.

>     curl -v -X POST http://localhost:8080/server-sent-events-jersey/domains/1 -H "Content-Type: text/event-stream"

Look at a console how events are handled one after another in the right order.

```
event: domain-progress
data: starting domain 1 ...

event: domain-progress
data: 50%

event: domain-progress
data: 60%

event: domain-progress
data: 70%

event: domain-progress
data: 99%

event: domain-progress
data: done
```

Running the Example
-------------------

Look at Jersey Documentation to learn how to consume Server-Sent Events using Jersey Client 
<https://jersey.java.net/documentation/latest/sse.html#d0e11786>

Run the example as follows:

>     mvn clean compile exec:java

This deploys the example using [Grizzly](http://grizzly.java.net/) container.
