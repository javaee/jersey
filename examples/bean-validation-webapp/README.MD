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

Bean Validation Bean Validation Webapp Example
==============================================

This example demonstrates a simple Bean Validation (JSR-349) example
with Jersey on the backend and AngularJs on the frontend.

Note: There is no validation on the frontend side, every error displayed
is recognized on the backend.

Contents
--------

The mapping of the URI path space is presented in the following table:

URI path                                                 |  Resource class        | HTTP methods        | Allowed values
-------------------------------------------------------- |  --------------------- | ------------------- | ----------------------------------
**_/api/contact_**                                       |  ContactCardResource   | GET, POST, DELETE   |
**_/api/contact/{id}_**                                  |  ContactCardResource   | GET, DELETE         | id - positive number
**_/api/contact/search/{searchType}?q={searchValue}_**   |  SearchResource        | GET                 | searchType - (name|phone|email) \searchValue - non empty string


Application is configured by using web.xml, which registers
[javax.ws.rs.core.Application](https://jax-rs-spec.java.net/nonav/2.0/apidocs/javax/ws/rs/core/Application.html)
descendant to get classes and singletons from it (see class
MyApplication). Bean Validation annotations are present on resource
classes (see ContactCardResource, SearchResource) as well as on the
domain class (see ContactCard).

When `curl` (see examples below) is used for sending requests to the
server, one can see how to affect the media type of returned validation
errors (if any).\
 Allowed media types for validation errors entities are:
"text/plain"/"text/html" or "application/json"/"application/xml"
(appropriate provider has to be registered to transform errors into
JSON/XML)\
 Note:
`jersey.config.beanValidation.enableOutputValidationErrorEntity.server`
init parameter (`web.xml`) has to be enabled to let Jersey know that it
should send validation errors as a Response entities (this behavior if
disabled by default).

Sample Response
---------------

You can access resources of this application also using curl:

>     curl -v -H "Accept: application/json" http://localhost:8080/bean-validation-webapp/api/contact/424242

In this example you should see JSON array returned with one error -
"Contact with given ID does not exist.".

```javascript
{
    message: "Contact with given ID does not exist."
    messageTemplate: "{contact.does.not.exist}"
    path: "ContactCardResource.getContact.<return value>"
}
```
>     curl -v http://localhost:8080/bean-validation-webapp/api/contact/424242

In this example you should see the same error but as a plain text.

    Contact with given ID does not exist. (path = ContactCardResource.getContact.<return value>, invalidValue = null)

Running the Example
-------------------

Run the example as follows:

>     mvn clean package jetty:run

This deploys current example using Jetty. You can access the application
at:

-   <http://localhost:8080/bean-validation-webapp>

Resources
---------

This examples is using following (3-rd party) front-end frameworks:

**AngularJS** by Google - front-end MVC JS framework
-   [Homepage](http://angularjs.org)
-   [Tutorial](http://docs.angularjs.org/tutorial)
-   [Reference guide](http://docs.angularjs.org/api)
-   [Developer guide](http://docs.angularjs.org/guide)

**Bootstrap** by Twitter - front-end HTML/CSS/JS framework
-   [Homepage](https://getbootstrap.com/)
-   [Getting started](https://getbootstrap.com/docs/4.0/getting-started/introduction/)
