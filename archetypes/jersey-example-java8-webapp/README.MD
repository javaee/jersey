<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.

    The contents of this file are subject to the terms of either the GNU
    General Public License Version 2 only ("GPL") or the Common Development
    and Distribution License("CDDL") (collectively, the "License").  You
    may not use this file except in compliance with the License.  You can
    obtain a copy of the License at
    http://glassfish.java.net/public/CDDL+GPL_1_1.html
    or packager/legal/LICENSE.txt.  See the License for the specific
    language governing permissions and limitations under the License.

    When distributing the software, include this License Header Notice in each
    file and include the License file at packager/legal/LICENSE.txt.

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
    and therefore, elected the GPL Version 2 license, then the option applies
    only if the new code is made subject to such option by the copyright
    holder.

-->

Jersey (Java8) WebApp Example Archetype.
==========================================================

This module defines skeleton maven project (archetype) for developing new Java SE-based and Servlet-based examples for Jersey.

Instructions
------------

- install this archetype into your local repository, `maven-archetype-plugin` updates your local `archetype-catalog.xml` 
which is located in `.m2` directory.

>     mvn clean install

- run generation your new project using:

>     mvn archetype:generate -DarchetypeCatalog=local

- maven provides you a set of your local installed maven archetypes, choose this one
`org.glassfish.jersey.archetypes:jersey-example-java8-webapp`

- fill in all input fields properly, e.g.

```
groupId: org.glassfish.jersey.examples (default value)
artifactId: my-example
version: Just use a release version - e.g. "2.20"
package: Use org.glassfish.jersey.examples.my-example
projectAuthor: Use "Name Surname (name.surname at mycompany.com)"
projectDescription: "My New Example using Jersey."
projectName: "My Jersey Example"
```

- confirm the project generation and feel free to start coding!
