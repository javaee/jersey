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

Flight Management Web Application Example
=========================================

This example demonstrates various Jersey features, namely:

-   Jersey Test Framework
-   Monitoring and Tracing support
-   Bean Validation integration
-   Implementing custom security context provider & use of standard security annotations
-   Entity Filtering support
-   MVC Templates
-   SSE events; including a simple HTML5 animation based on data received from SSE events

Flight Management Web Application is a simple demo application for
managing (creating, updating, deleting, reserving) flights and aircrafts
and their subsequent animated flight simulation on HTML5 canvas using
streamed SSE data.

Contents
--------

The example consists of 3 main resource classes:

`org.glassfish.jersey.examples.flight.resources.AircraftsResource`

Resource exposing aircraft data (collections or individual
aircraft instances) as well as methods for manipulating the
aircraft data.

`org.glassfish.jersey.examples.flight.resources.FlightsResource`

Resource exposing flight data (collections or individual
flight instances) as well as methods for manipulating the
flight data.

`org.glassfish.jersey.examples.flight.resources.FlightSimResource`

Resource controlling the flight simulation lifecycle and exposing
SSE stream of simulation events (changes in flight locations).

All the application JAX-RS resources are deployed under the same `api`
root path. The mapping of the URI path space for the individual
resources is presented in the following table:

URI path                           | Resource class      | HTTP methods | Description
---------------------------------- | ------------------- | ------------ | ---------------------------------------------------------------------------------------------------------------------------
**_/flights_**                     | FlightsResource     | GET          | Retrieve list of flights in various formats - XML, JSON, TXT, HTML, CSV, YAML
**_/flights_**                     | FlightsResource     | POST         | Create a new flight (input: `application/x-www-form-urlencoded`; output: XML, JSON); Requires "admin" role.
**_/flights/open_**                | FlightsResource     | GET          | Retrieve list of open flights (not closed for simulation) - XML, JSON
**_/flights/{id}_**                | FlightsResource     | GET          | Retrieve detailed information about a single flight - XML, JSON, HTML
**_/flights/{id}_**                | FlightsResource     | DELETE       | Delete a single flight (output: deleted flight identifier - TXT); Requires "admin" role.
**_/flights/{id}/new-booking_**    | FlightsResource     | POST         | Reserve a seat in a flight. (output: reservation identifier - TXT, HTML)
**_/flights/{id}/status_**         | FlightsResource     | POST         | Change flight status. (input: `application/x-www-form-urlencoded`; output: new status name - TXT); Requires "admin" role.
**_/aircrafts_**                   | AircraftsResource   | GET          | Retrieve list of aircrafts in various formats - XML, JSON, HTML
**_/aircrafts_**                   | AircraftsResource   | POST         | Create a new aircraft (input: `application/x-www-form-urlencoded`; output: XML, JSON); Requires "admin" role.
**_/aircrafts/available_**         | AircraftsResource   | GET          | Retrieve list of available aircrafts (not assigned to a flight) - XML, JSON
**_/aircrafts/{id}_**              | AircraftsResource   | GET          | Retrieve detailed information about a single aircraft - XML, JSON, HTML
**_/aircrafts/{id}_**              | AircraftsResource   | DELETE       | Delete a single aircraft (output: deleted aircraft identifier - TXT); Requires "admin" role.
**_/simulation_**                  | FlightSimResource   | POST         | START/STOP simulation (input: `application/x-www-form-urlencoded`; output: TXT)
**_/simulation/events_**           | FlightSimResource   | GET          | Receive simulation events as SSE stream.

Running the Example
-------------------

Flight Management Web Application Example is a web application (WAR)
that can be deployed to any Servlet 3.x container. By default, the
examples runs on Jetty using the Jetty Maven plugin. Before the example
is run, it must be built first:

>    mvn clean package

This command builds the example WAR and runs all the unit tests against
the example application.

Once the example is built, it can be run as follows:

>    mvn jetty:run

Links
-----

When the example is run using the Jetty Maven plugin as described above,
the example Web application gets deployed to the local host under port
`8080`. As mentioned earlier, the resources can be accessed directly
under common `api` root path. Here are a few links to directly access
some of the `HTTP GET` operations on the application resources:

-   <http://localhost:8080/api/flights>
-   <http://localhost:8080/api/flights/open>
-   <http://localhost:8080/api/aircrafts>
-   <http://localhost:8080/api/aircrafts/available>

The HTML application root to be accessed via browser is represented by
the application `index.html` page:

-   <http://localhost:8080/index.html>