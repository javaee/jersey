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

Jersey Weld Integration Jersey Weld on Grizzly Example
======================================================

This example demonstrates how to develop RESTful web service with CDI
managed beans and Grizzly HTTP container.

We suggest you inspect the example source code to get better
understanding on various CDI features utilized there. Following table
contains some of the published resources that you might want to check.

Contents
--------

The mapping of the URI path space is presented in the following table:

URL                                            | Description                                                          | Expected Results
---------------------------------------------- | -------------------------------------------------------------------- | ------------------------------------------------------
**_/helloworld?name=Bill_**                    | A hello-world managed bean                                           | Hello Bill
**_/app/count_**                               | Application scoped resource                                          | request count (incremented with every other request)
**_/req/app/counter_**                         | Request scoped resource                                              | links to above request counter
**_/req/parameterized-async?q=echo%20this_**   | Asynchronous echo resource method that is being JAX-RS intercepted   | echo this

Running the Example
-------------------

Run the example as follows:

>     mvn clean compile exec:java

This deploys the example using [Grizzly](http://grizzly.java.net/) container. You can access the application at:

- <http://localhost:8080/weld>