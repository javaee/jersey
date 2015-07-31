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

Feed Combiner WebApp Example
============================

Feed Combiner is an application that allows us to aggregate entries from
individual feeds and to provide a final combined result in several ways.
In order to download a given aggregated combined feed, we can use REST
API and consume the entry feeds in format, such as HTML, JSON and ATOM.
If it's easier for us to use a browser and sufficient to display the
settings of combined feeds, there is also a simple HTML page in which is
possible to manage our combined feeds.

The application provides two ways to manage it: REST API and WEB
Application

Contents
--------

The example consists of four web resources implemented by the following:

`org.glassfish.jersey.examples.feedcombiner.resource.CombinedFeedResource`
:   The combined feed resource that saves a new combined feed which
    automatically starts a scheduler for downloading new entries for
    provided feeds. Then it is also able to delete a particular feed and
    provide list of entries of the given feed in JSON or ATOM
    XML format.

`org.glassfish.jersey.examples.feedcombiner.resource.CombinedFeedController`
:   The combined feed controller provides GUI Manager using
    FreeMarker templates. It is possible to save, delete, and show a
    particular feed using HTML forms generated from templates.

The mapping of the URI path space is presented in the following table:

  URI path                                         | Resource class           | HTTP methods   | Allowed values
 ------------------------------------------------- | ------------------------ | -------------- | -------------------------------------
 **_/combiner/feeds_**                             | CombinerFeedResource     | POST           | returns JSON
 **_/combiner/feeds/{feed_id}_**                   | CombinerFeedResource     | DELETE         | N/A
 **_/combiner/feeds/{feed_id}/entries_**           | CombinerFeedResource     | GET            | returns JSON or ATOM\_XML
 **_/combiner_**                                   | CombinerFeedController   | GET            | FreeMarker templates/index.ftl
 **_/combiner_**                                   | CombinerFeedController   | POST           | FreeMarker templates/index.ftl
 **_/combiner/{feed_id}_**                         | CombinerFeedController   | GET            | FreeMarker templates/feed-entries.ftl
 **_/combiner/delete/{feed_id}?_method=DELETE_**   | CombinerFeedController   | POST           | FreeMarker templates/index.ftl

Sample Response
---------------

### Create a Combined Feed

-   URL: `http://localhost:8080/combiner/feeds`
-   Http-Method: `POST`
-   Content-Type: `application/json`

```javascript
{
    "title": "title",
    "description": "description",
    "refreshPeriod": 5,
    "urls": [
        "http://www.buzzfeed.com/index.xml",
        "http://www.reddit.com/r/java/.rss"
    ]
}
```

-   *refreshPeriod* is optional value, if it's `null`, the default value
    from `application.properties` will be used
-   At least one URL must be valid otherwise an error message will be
    returned

```javascript
{
    "messages":[
        "At least one valid URL must be in the request."
    ]
}
```

#### Response

-   Status Code: `201 Created`
-   Location: `http://localhost:8080/combiner/feeds/1`
-   Content-Type: `application/json`

```javascript
{
  "id":"1",
  "urls":[
    "http://www.buzzfeed.com/index.xml","http://www.reddit.com/r/java/.rss"
  ],
  "refreshPeriod":5,
  "feedEntries":[],
  "title":"title",
  "description":"description"
}
```

### Delete a Combined Feed

#### Request

-   URL: `http://localhost:8080/combiner/feeds/{feed_id}`
-   Http-Method: `DELETE`

#### Response

-   Status Code: `204 No Content`

### Get Combined Feed entries

#### Request

-   URL: `http://localhost:8080/combiner/feeds/{feed_id}/entries`
-   Http-Method: `GET`
-   Accept: `application/json` or `application/atom+xml`

#### Response

-   Status Code: `200 OK`
-   Body: Structured data depending on the `Accept` header in the
    request

Running the Example
-------------------

Run the example as follows:

>     mvn clean compile exec:java

This deploys the example using [Grizzly](http://grizzly.java.net/)

>     mvn clean package jetty:run

This deploys current example using Jetty. You can access the application
at

-   REST API: <http://localhost:8080/combiner/feeds>
-   GUI Manager: <http://localhost:8080/combiner>

Resources
---------

### This examples is using the following (3-rd party) libraries:

**ROME** by RomeTools

ROME includes a set of parsers and generators for the various flavors of
syndication feeds, as well as converters to convert from one format to
another. The parsers can give you back Java objects that are either
specific for the format you want to work with, or a generic normalized
SyndFeed class that lets you work on with the data without bothering
about the incoming or outgoing feed type.

-   [GitHub Pages](http://rometools.github.io/rome/)
-   [GitHub Sources](https://github.com/rometools/rome)
