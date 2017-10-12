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

Bookmark EM Example
===================

This example demonstrates how to use JPA in the backend. The example is
based on bookmark example from the [RESTful Web
Services](http://www.oreilly.com/catalog/9780596529260/) book, which was
inspired by the [http://del.icio.us/](http://del.icio.us/v1) web
service.

A bookmark web application is presented that is capable of maintaining
users and their bookmarks.

Contents
--------

The mapping of the URI path space is presented in the following table:

URI path                                 | Resource class      | HTTP methods                                          | Notes
---------------------------------------- | ------------------- | ----------------------------------------------------- | --------------------------------------------------------
**_/users_**                             | UsersResource       | GET                                                   | Returns a list of users.
**_/users/{userid}_**                    | UserResource        | GET, PUT (used also for creating new users), DELETE   | Returns user details
**_/users/{userid}/bookmarks_**          | BookmarksResource   | GET, POST                                             | Returns a list of bookmarks for actual user resource.
**_/users/{userid}/bookmarks/{bmid}_**   | BookmarkResource    | GET, PUT, DELETE                                      | Returns bookmark uri and a long and short description.

Running the Example
-------------------

Bookmark example runs on Glassfish 3.1 application server
([https://javaee.github.io/glassfish/](https://javaee.github.io/glassfish/)) and needs a running JavaDB
(<http://www.oracle.com/technetwork/java/javadb/overview/index.html>)
instance for underlying data (it comes along with GlassFish 3.1).

Presuming, you have installed Glassfish 3.1. `AS_HOME` variable should
point to your glassfish installation directory.

#### Building And Starting The Bookmark Service

You then build and run the example by

    mvn package
    $AS_HOME/bin/asadmin start-domain
    $AS_HOME/bin/asadmin start-database
    $AS_HOME/bin/asadmin deploy target/bookmark-em.war

#### Test Client Running

    mvn test -Djersey.config.test.container.factory=org.glassfish.jersey.test.external.ExternalTestContainerFactory -Djersey.config.test.logging.enable=true -Djersey.config.test.logging.dumpEntity=true -Djersey.config.test.container.port=8080 -Ptest

Links
-----

<http://java.net/projects/bpcatalog/sources/svn/content/trunk/ee5/docs/persistence/webonlyapp.html?raw=true>
