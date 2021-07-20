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

MOXy Example
============

What is MOXy
------------

This example demonstrates that you can utilize MOXy extensions when
dealing with XML representation when developing a Jersey based RESTful
Web application

MOXy is EclipseLink's Object to XML Mapping services. MOXy allows for a
POJO object model to be mapped to an XML schema. The Java Architecture
for XML Binding (JAXB) provides a Java standard for object XML mapping
(OXM). MOXy supports JAXB, as well as providing its' own native API and
integration with Web Services.

MOXy XML Path Mapping Extension
-------------------------------

The MOXy extension shown in this example is described on the Eclipse
Wiki site at
<http://wiki.eclipse.org/EclipseLink/Examples/MOXy/GettingStarted/MOXyExtensions>.
It allows you to specify path based mapping via @XmlPath annotation.

If the MOXy extension was not used, the XML represenation of the
customer data would look like follows:

```xml
<customer>
  <name>Jane Doe</name>
  <address>
    <city>My Town</city>
    <street>123 Any Street</street>
  </address>
  <phoneNumbers type="work">613-555-1111</phoneNumbers>
  <phoneNumbers type="cell">613-555-2222</phoneNumbers>
</customer>
```

By adding `@org.eclipse.persistence.oxm.annotations.XmlPath` annotation
to the bean definition classes, you will get the following XML
representation instead:

```xml
<customer>
  <personal-info>
    <name>Jane Doe</name>
  </personal-info>
  <contact-info>
    <address>
      <city>My Town</city>
      <street>123 Any Street</street>
    </address>
    <phone-number type="work">613-555-1111</phone-number>
    <phone-number type="cell">613-555-2222</phone-number>
  </contact-info>
</customer>
```

XML Path expressions used are:

-   personal-info/name/text()
-   contact-info/address
-   contact-info/phone-number

Please check out the source code and the wiki page linked above for the
detailed information on the XML Path mapping feature.

Replacing Implicit JAXB Runtime With MOXy
-----------------------------------------

Since MOXy is a JAXB implementation, the example still utilizes the
standard Jersey JAXB message body reader/writer providers. To make
Jersey use MOXy runtime, you just need to put a `jaxb.properties` file
into the Java package containing your JAXB beans. The file should have
the following content:

    javax.xml.bind.context.factory=org.eclipse.persistence.jaxb.JAXBContextFactory

Concrete file used in this example is placed under

    src/main/resources/com/sun/jersey/samples/moxy/beans/jaxb.properties

Contents
--------

The example consists of a single REST Resource represented by the
following Java class:

    org.glassfish.jersey.examples.xmlmoxy.CustomerResource

A resource class that maintains a single customer data.

The mapping of the URI path space is presented in the following table:

URI path         | Resource class     | HTTP methods
---------------- | ------------------ | --------------
**_/customer_**  | CustomerResource   | GET, PUT

Running the Example
-------------------

Run the example as follows:

>     mvn clean compile exec:java

This deploys the example using [Grizzly](http://grizzly.java.net/) container. You can access the application at:

-   [http://localhost:8080/moxy/customer](http://localhost:8080/xml-moxy/customer)
