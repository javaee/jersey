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

DTMF Tone Generator Example
===========================

Dual Tone Multi Frequency signals are common way how to transfer simple
data over analogue voice line. The detailed description can be found in
the Wikipedia article
[here](http://en.wikipedia.org/wiki/Dual-tone_multi-frequency_signaling).

Contents
--------

The example consists of one web resource:

`org.glassfish.jersey.examples.tonegen.WavResource`

This Java class returns the wave audio containing the sequence of
DTMF tones generated based on a given set of query parameters.

The example parses query parameters, generates sinus waves and composes
them to create a valid DTMF tone sequence.

The mapping of the URI path space is presented in the following table:

URI path       | Resource class   | HTTP methods   | Description
-------------- | ---------------- | -------------- | -----------------------------------------------
**_wav_**      | WavResource      | GET            | Create a wav audio containing the DTMF tones.

The following query parameters can be declared:

Query parameter         | Default Value    | Description
----------------------- | ---------------- | -------------------------------------------------------------
**_toneSequence_**      | 1234567890\#\*   | A String containing valid DTMF characters (0-9, \# and \*).
**_toneDuration_**      | 300              | Duration of one generated tone in milliseconds.
**_silenceDuration_**   | 100              | Duration of the silence between two generated tones.

Running the Example
-------------------

Run the example as follows:

>     mvn clean compile exec:java

This deploys the example using [Grizzly](http://grizzly.java.net/) container. You can access the application at:

-   <http://localhost:9998/tonegen>

Tweak the parameters and click *Play* to hear the result or compose your
own URI based on the description above and have the wav audio returned
from the resource.
