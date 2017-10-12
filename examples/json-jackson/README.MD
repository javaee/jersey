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

Jackson JAX-RS JSON Provider Example
====================================

This example demonstrates how to produce/consume JSON representations
from Java objects. This applies not only to JAXB beans, as shown in the
`json-from-jaxb` example but also to ordinary, un-annotated, POJOs.

This example hosts three simple read-only resources: One provides an
example of using a Jackson JSON provider (registered by the feature
`JacksonFeature` in the `MyApplication` class) instead of using JAXB
(Object-&gt;JAXB-&gt;JSON) which has some limitations (e.g. empty arrays
in JAXB beans). For this resource the JSON representation is produced by
the Jackson JAX-RS provider, while the XML representation is generated
by JAXB as usual. The second web resource is based on a simple
un-annotated POJO and provides only JSON-based representations: JSON and
JSON with padding (JSONP). The third resource depicts how Jackson and
JAXB annotations could be mixed together within a single POJO.

Contents
--------

The "empty array" web resource is implemented by the `org.glassfish.jersey.examples.jackson.EmptyArrayResource` class.

The "non JAXB" web resource is implemented by the `org.glassfish.jersey.examples.jackson.NonJaxbBeanResource` class.

Both resources use the default Jackson mapper configuration to serialize JSON
data out and depicts the corner cases, where the Jersey internal, StAX
based, JSON processing can not be utilized.

The `org.glassfish.jersey.examples.jackson.CombinedAnnotationResource`
class is used to show how JAXB and Jackson annotations could be combined
together in one Java bean for JSON serialization. See the

`org.glassfish.jersey.examples.jackson.MyObjectMapperProvider` class
where Jackson specific options are used to set up the desired JSON
serialization configuration.

The mapping of the URI path space is presented in the following table:

URI path                     | Resource class               | HTTP method
---------------------------- | ---------------------------- | -------------
**_/emptyArrayResource_**    | EmptyArrayResource           | GET
**_/nonJaxbResource_**       | NonJaxbBeanResource          | GET
**_/combinedAnnotations_**   | CombinedAnnotationResource   | GET

To use Jackson specific configuration options, one can implement a
`ContextResolver<ObjectMapper>` provider. For an example of such an
implementation, see the `MyObjectMapperProvider` class.

Running the Example
-------------------

Run the example as follows:

>     mvn clean compile exec:java

This deploys the example using[Grizzly](http://grizzly.java.net/)

A [WADL description](http://wadl.java.net/#spec) may be accessed at the URL:

-   <http://localhost:8080/jackson/application.wadl>

The three resources are available at

-   <http://localhost:8080/jackson/emptyArrayResource>
-   <http://localhost:8080/jackson/nonJaxbResource>
-   <http://localhost:8080/jackson/combinedAnnotations>

To easily obtain the different output types available,
[cURL](http://curl.haxx.se/) can be used as follows:

Obtain the JSON output of EmptyArrayResource (use
`-HAccept:application/xml` for XML output):

>     curl -HAccept:application/json http://localhost:8080/jackson/emptyArrayResource

Obtain the JSON output of NonJaxbBeanResource (use `-HAccept:application/javascript` for JSONP output):

>     curl -HAccept:application/json http://localhost:8080/jackson/nonJaxbResource

Obtain the JSON output of CombinedAnnotationResource:

>     curl -HAccept:application/json http://localhost:8080/jackson/combinedAnnotations
