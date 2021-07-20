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

Sparklines Example
==================

This example is inspired by Joe Gregorio's [Sparkline](http://bitworking.org/projects/sparklines/) Generator Web
Application. For more about the concept of sparklines read [here](http://en.wikipedia.org/wiki/Sparkline).

Contents
--------

The example consists of one web resource implemented by thefollowing:

`org.glassfish.jersey.examples.sparklines.SparklinesResource`

This Java class returns the a PNG image of a sparkline calculated
for a given set of query parameters.

The example makes use of classes to parse query parameters, a message
body writer to write an image in the PNG format, and makes use of entity
tags and performs precondition checking to support not modified
responses.

The mapping of the URI path space is presented in the following table:

URI path          | Resource class       | Content   | Description
----------------- | -------------------- | --------- | -----------------------------------------------------------------
_**/discrete_**   | SparklinesResource   | GET       | Plot a sparkline as discrete vertical bars, one per data value.
_**/smooth_**     | SparklinesResource   | GET       | Plot a sparkline as continuous line.

For either URI path the following query parameters can be declared:

Query parameter       | Default Value   | Description
--------------------- | --------------- | -------------------------------------------------------------------------------------------------
**_d_**               |                 | A comma separated list of integer values that represents the input data to plot as a sparkline.
**_limits_**          | 0,100           | The valid range of the input data in "d".

The "smooth" URI path can have following query parameters:

Query parameter       | Default Value   | Description
--------------------- | --------------- | --------------------------------------------------------------------------
**_min-m_**           | true            | If true then a marker is placed at the smallest value in the input data.
**_max-m_**           | true            | If true then a marker is placed at the largest value in the input data.
**_last-m_**          | true            | If true then a marker is placed at the last value in the input data.
**_min-color_**       | blue            | The colour of the marker placed at the smallest value in the input data
**_max-color_**       | green           | The colour of the marker placed at the largest value in the input data.
**_last-color_**      | red             | The colour of the marker placed at the last value in the input data.
**_step_**            | 2               | The horizontal distance, in pixels, between each plot of a data value.

The "discrete" URI path can have following query parameters:

Query parameter       | Default Value   | Description
--------------------- | --------------- | -----------------------------------------------------------------------------------------------------------------
**_width_**           | 2               | The width, in pixels, of each plot of a virtual bar for each data value.
**_upper_**           | 50              | Data values &gt;= upper will be coloured in "upper-color" otherwse data values will be colured in "lower-color"
**_upper-color_**     | red             | The colour for data values &gt;= "upper"
**_lower-color_**     | gray            | The colour for data values &lt; upper

Running the Example
-------------------

Run the example as follows:

>     mvn clean compile exec:java

This deploys the example using [Grizzly](http://grizzly.java.net/) container. You can access the application at:

-   <http://localhost:9998/sparklines/discrete?d=88,84,82,92,82,86,66,82,44,64,66,88,96,80,24,26,14,0,0,26,8,6,6,24,52,66,36,6,10,14,30>

and manipulate the URI manually to adjust the result image.

You can also use the example's web frontend at:

-   <http://localhost:9998/sparklines>

to see the basic functionality.
