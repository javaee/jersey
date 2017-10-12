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

Entity Data Filtering Dynamic and Configurable Query Parameters
===============================================================

This example demonstrates how to use entity filtering feature
dynamically with defining what should be sent to the other side based on
query parameters and how to apply the rules on domain classes as well as
on JAX-RS resource classes or JAX-RS resource methods.

The full description how Entity Data Filtering can be found in Jersey
User Guide, chapter [Entity Data
Filtering](https://jersey.java.net/documentation/latest/entity-filtering.html).
Sections relevant to this example (describing this exact example) are:

-   [Enabling and configuring Entity Filtering in your
    application](https://jersey.java.net/documentation/latest/entity-filtering.html#d0e13911)
-   [Entity Filtering based on dynamic and configurable query
    parameters](https://jersey.java.net/documentation/latest/entity-filtering.html#ef.selectable.annotations)

Contents
--------

The mapping of the URI path space is presented in the following table:

URI path                                     | Resource class   | HTTP methods   | Allowed values                                                               | Notes
------------------------------------------   | ---------------- | -------------- | ---------------------------------------------------------------------------- | ----------------------------------------------
**_/people/{id}?select={select}_**           |  PersonResource  |  GET           |  id = any value, select = field names from Person/Address/PhoneNumber class  |  Returns fields of Person/Address/PhoneNumber
**_/people/{id}/addresses?select={select}_** |  PersonResource  |  GET           |  id = any value, select = field names from Address/PhoneNumber class         |  Returns fields of Address/PhoneNumber

Application is based on Grizzly container (see `App`). Everything needed
(resources/providers) is registered in
`SelectableEntityFilteringApplication`.

Sample Response
---------------

Even though the same instance of, e.g. Person class, is used to create
response the given values of `select` query parameter are used to select
the fields that would be transferred over the wire. For
`people/1234?select=familyName,givenName` it looks like:

```javascript
{
   "familyName": "Dowd",
   "givenName": "Andrew"
}
```

And for `people/1234?select=familyName,givenName,addresses.phoneNumber.number` it looks like:

```javascript
{
   "addresses":[
      {
         "phoneNumber":{
            "number": "867-5309"
         }
      }
   ],
   "familyName": "Dowd",
   "givenName": "Andrew"
}
```

Running the Example
-------------------

Run the example as follows:

>     mvn clean package exec:java

This deploys current example using Grizzly. You can access the
application at:

-   <http://localhost:8080/people/1234>
-   <http://localhost:8080/people/1234?select=familyName,givenName>
-   <http://localhost:8080/people/1234?select=region,addresses.region>
-   <http://localhost:8080/people/1234?select=familyName,givenName,addresses.phoneNumber.number>

Using Jackson instead of MOXy
-----------------------------

This examples uses by default Entity Data Filtering feature together
with MOXy. To switch MOXy JSON provider to Jackson (2.x) JSON provider
simply

-   comment registration of MOXy ContextResolver, and\
     `register(new MoxyJsonConfig().setFormattedOutput(true).resolver())`
-   uncomment registration of JacksonFeature\
     `register(JacksonFeature.class)`

in `SelectableEntityFilteringApplication` class.
