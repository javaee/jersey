#!/bin/bash
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
#
# The contents of this file are subject to the terms of either the GNU
# General Public License Version 2 only ("GPL") or the Common Development
# and Distribution License("CDDL") (collectively, the "License").  You
# may not use this file except in compliance with the License.  You can
# obtain a copy of the License at
# http://glassfish.java.net/public/CDDL+GPL_1_1.html
# or packager/legal/LICENSE.txt.  See the License for the specific
# language governing permissions and limitations under the License.
#
# When distributing the software, include this License Header Notice in each
# file and include the License file at packager/legal/LICENSE.txt.
#
# GPL Classpath Exception:
# Oracle designates this particular file as subject to the "Classpath"
# exception as provided by Oracle in the GPL Version 2 section of the License
# file that accompanied this code.
#
# Modifications:
# If applicable, add the following below the License Header, with the fields
# enclosed by brackets [] replaced by your own identifying information:
# "Portions Copyright [year] [name of copyright owner]"
#
# Contributor(s):
# If you wish your version of this file to be governed by only the CDDL or
# only the GPL Version 2, indicate your decision by adding "[Contributor]
# elects to include this software in this distribution under the [CDDL or GPL
# Version 2] license."  If you don't indicate a single choice of license, a
# recipient has the option to distribute your version of this file under
# either the CDDL, the GPL Version 2 or to extend the choice of license to
# its licensees as provided above.  However, if you add GPL Version 2 code
# and therefore, elected the GPL Version 2 license, then the option applies
# only if the new code is made subject to such option by the copyright
# holder.
#

LIBS=$(for l in `ls lib`; do echo -n lib/$l":";done)
LIBS=`echo $LIBS | sed -es'/:$//'`

APP=$(for l in `ls app`; do echo -n app/$l":";done)
APP=`echo $APP | sed -es'/:$//'`

nohup java -server -Xms512m -Xmx1024m -XX:PermSize=256m -XX:MaxPermSize=512m \
      -XX:+UseParallelGC -XX:+AggressiveOpts -XX:+UseFastAccessorMethods \
      -cp $APP:$LIBS \
      -Djava.net.preferIPv4Stack=true \
      -Dcom.sun.management.jmxremote \
      -Dcom.sun.management.jmxremote.port=11112 \
      -Dcom.sun.management.jmxremote.authenticate=false \
      -Dcom.sun.management.jmxremote.ssl=false \
      -Dcom.sun.management.jmxremote.local.only=false \
      $JAVA_OPTIONS \
      org.glassfish.jersey.tests.performance.runners.jersey2grizzly.Jersey2GrizzlyRunner $* &
echo $! > grizzly.pid
wait
