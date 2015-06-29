#!/bin/bash
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

TARGET=$PWD/target
MW_HOME=`cat $TARGET/mw_home.txt`
TEST_DOMAIN=$MW_HOME/hudson_test_domain
DOMAIN_NAME=HudsonTestDomain
SERVER_NAME=HudsonTestServer
PID_FILE=$TARGET/wls.pid

echo $TEST_DOMAIN > $TARGET/test_domain.txt

cd $MW_HOME
. $MW_HOME/wlserver/server/bin/setWLSEnv.sh

rm -rf $TEST_DOMAIN
mkdir -p $TEST_DOMAIN
cd $TEST_DOMAIN

rm -f $TARGET/autodeploy
ln -s $TEST_DOMAIN/autodeploy $TARGET/autodeploy

rm -f $TARGET/server.log
rm -f $TARGET/domain.log
ln -s $TEST_DOMAIN/servers/$SERVER_NAME/logs/$SERVER_NAME.log $TARGET/server.log
ln -s $TEST_DOMAIN/servers/$SERVER_NAME/logs/$DOMAIN_NAME.log $TARGET/domain.log

JAVA_OPTIONS="-javaagent:$HOME/jersey-perftest-agent.jar"

yes | nohup java -server \
      -Xms1024m \
      -Xmx1024m \
      -XX:MaxPermSize=256m \
      -Dweblogic.Domain=$DOMAIN_NAME \
      -Dweblogic.Name=$SERVER_NAME \
      -Dweblogic.management.username=weblogic \
      -Dweblogic.management.password=weblogic1 \
      -Dweblogic.ListenPort=7001 \
      -Djava.security.egd=file:/dev/./urandom \
      -Djava.net.preferIPv4Stack=true \
      -Dcom.sun.management.jmxremote \
      -Dcom.sun.management.jmxremote.port=11112 \
      -Dcom.sun.management.jmxremote.authenticate=false \
      -Dcom.sun.management.jmxremote.ssl=false \
      -Dcom.sun.management.jmxremote.local.only=false \
      $JAVA_OPTIONS \
      weblogic.Server &

echo $! > $PID_FILE

# wait for server to start
echo "******** WAITING FOR SERVER TO START"
while [ ! `wget -q --server-response --no-proxy http://localhost:7001 2>&1 | awk '/^  HTTP/{print $2}'` ]; do
  sleep 5
  echo "*"
done
echo "******** SERVER IS READY"
