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

Hello World JMH Example
=======================

### *How to write JAX-RS Micro-Benchmarks*

This example demonstrates how to write simple micro-benchmarks, based on
JMH, for JAX-RS application. This particular example shows how to obtain
throughput of a simple 'Hello World' application.

The application consists of one resource, `HelloWorldResource` with
three resource methods (one method per HTTP GET, POST and PUT methods).
To make it more interesting the resource also acts like sub-resource
locator which allows us to measure the difference between processing of
normal resource methods and sub-resource locators.

This application is solely focused on showing how to write benchmarks
and it doesn't start any server that would allow one to send request to
the `HelloWorldResource`.

Main class that illustrates writing benchmarks for JAX-RS applications
is `HelloWorldBenchmark`. Measurements are not based on any container
such as Servlet or Grizzly container. Requests are directly invoked on
`ApplicationHandler` class from Jersey Server module to eliminate any
network influences. Still, all JAX-RS features, like filters and
interceptors, are taken into consideration when the benchmark is
executed.

It's also possible to run the benchmark on older versions of Jersey,
e.g. 2.15, which had known performance issues with sub-resource
locators. The recommended approach is to run the benchmarks without any
changes at first and then modify the `pom.xml` to switch version of
Jersey to 2.15 and run the benchmarks again to see the (big) difference.
Look for `jersey-bom` artifact in `pom.xml` to see how the switch should
be done.

Sample Results
--------------

The execution takes around 2 minutes and you can see partial results
(for each combination of parameters) of every iteration. At the end JMH
gives you whole summary. For example, Jersey 2.16 gives the following
results:

    # Run complete. Total time: 00:01:41

    Benchmark                    (method)              (path)   Mode  Cnt      Score      Error  Units
    HelloWorldBenchmark.measure       GET          helloworld  thrpt    8  74343.864 ± 4814.979  ops/s
    HelloWorldBenchmark.measure       GET  helloworld/locator  thrpt    8  54137.102 ± 8996.766  ops/s
    HelloWorldBenchmark.measure      POST          helloworld  thrpt    8  45173.853 ± 5349.363  ops/s
    HelloWorldBenchmark.measure      POST  helloworld/locator  thrpt    8  37144.797 ± 4699.782  ops/s
    HelloWorldBenchmark.measure       PUT          helloworld  thrpt    8  45945.974 ± 4116.752  ops/s
    HelloWorldBenchmark.measure       PUT  helloworld/locator  thrpt    8  36345.667 ± 5929.480  ops/s

Running the Example
-------------------

There are two ways how to run the micro-benchmark. Either via Maven
`exec:exec`

>     mvn clean install exec:exec

or you can build the benchmark JAR at first

>     mvn clean install

and then invoking it using `java` command

>     java -jar target/benchmark.jar

Resources
---------

JMH has excellent overview of it's features presented as list of
different samples. You can find them on [JMH
Samples](http://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/).
The general overview of JMH is available at their [home
page](http://openjdk.java.net/projects/code-tools/jmh/).
