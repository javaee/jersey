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

Jersey HTTP PATCH
=================

### *Jersey HTTP PATCH Support Example*

This example demonstrates how to implement generic support for
[HTTP PATCH Method (RFC-5789)](https://tools.ietf.org/html/rfc5789)
via JAX-RS reader interceptor. The example has been created based on
[this Gerard Davison's blogentry] (http://kingsfleet.blogspot.co.uk/2014/02/transparent-patch-support-in-jax-rs-20.html).
The patch format supported by this example is [JSON Patch (RFC-6902)](http://tools.ietf.org/html/rfc6902).

Contents
--------

The mapping of the URI path space is presented in the following table:

URI path                 | Resource class      | HTTP methods
------------------------ | ------------------- | --------------
**_/patchable-state_**   | PatchableResource   | GET, PATCH

As you can see in the table, there is only a single resource deployed in
this application, that supports `GET` and `PATCH` methods. The resource
represents a *patchable* state, which consists of 3 properties:

-   a `title` text property,
-   a `message` text property, and
-   a `list` property that represents a list/array of text strings.

This state can be patch via HTTP GET method by sending the desired
patch/diff in the JSON Patch format as defined in RFC-6902, for example:

```javascript
[
  {
    "op" : "replace",
    "path" : "/message",
    "value" : "patchedMessage"
  }, {
    "op" : "add",
    "path" : "/list/-",
    "value" : "one"
  }
]
```

This patch will instruct the resource to replace it's `message` property
content with a new `"patchedMessage"` text and also to append a new
`"one"` string value to the list of valued contained in the `list`
property.

(See `HttpPatchTest` for more details.)

Sample Response
---------------

You can apply a patch using curl:

>     curl -v -X PATCH http://localhost:8080/http-patch/patchable-state -H "Content-Type:application/json-patch+json" -d '[{
>        "op": "replace",
>        "path": "/message",
>        "value": "patchedMessage"
>     }, {
>        "op": "add",
>        "path": "/list/-",
>        "value": "one"
>     }]'

The application will answer with a patched object:

```javascript
{
   "list" : [ "one" ],
   "message" : "patchedMessage",
   "title" : ""
}
```

Running the Example
-------------------

Run the example as follows:

>     mvn clean compile exec:java

This deploys the example using [Grizzly](http://grizzly.java.net/) container.

A [WADL description](http://wadl.java.net/#spec) may be accessed at the URL:

-   <http://localhost:8080/http-patch/application.wadl>

The resource is available at

-   <http://localhost:8080/http-patch/patchable-state>
