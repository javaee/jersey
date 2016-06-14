#!/bin/sh
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2015-2016 Oracle and/or its affiliates. All rights reserved.
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

set -e
[ "$DEBUG" = "true" ] && set -x

# redeclaration of env variables so that editors do not think every variable is a typo
WAR_PATH=$WAR_PATH
MAX_HEAP=$MAX_HEAP
TRIES_COUNT=$TRIES_COUNT
CATALINA_HOME=$CATALINA_HOME
PORT=$PORT
SKIP_DEPLOY=$SKIP_DEPLOY
APPLICATION_NAME=$APPLICATION_NAME
CONTEXT_ROOT=$CONTEXT_ROOT
MEMORY_LEAK_PREVENTION=$MEMORY_LEAK_PREVENTION
DIST_DIR=$DIST_DIR
SKIP_START_STOP=$SKIP_START_STOP
JVM_ARGS=$JVM_ARGS
SKIP_CHECK=$SKIP_CHECK

if [ "$CATALINA_HOME" = "" -o "$WAR_PATH" = "" -o "$MAX_HEAP" = "" -o "$TRIES_COUNT" = "" ]; then
    echo ARGUMENTS NOT OK
    exit 1
fi

start_tomcat() {
    if [ "$SKIP_CHECK" != "true" ] && jps -v | grep 'jersey.config.test.memleak.tomcat.magicRunnerIdentifier'; then
        echo ERROR There is already running instance of Tomcat
        exit 2
    fi

    if nc -z localhost $PORT; then
        echo ERROR port $PORT is not free!
        exit 3
    fi

    [ -d "$CATALINA_HOME"/webapps/"$CONTEXT_ROOT" ] && rm -rf "$CATALINA_HOME"/webapps/"$CONTEXT_ROOT"
    [ -f "$CATALINA_HOME"/webapps/"$CONTEXT_ROOT".* ] && rm -f "$CATALINA_HOME"/webapps/"$CONTEXT_ROOT".*

    sed -i -e 's@\(Connector port="\)[0-9]*\(" protocol="HTTP/1.1"\)@\1'$PORT'\2@' "$CATALINA_HOME"/conf/server.xml

    if [ "$MEMORY_LEAK_PREVENTION" != "true" ]; then
        sed -i -e 's@\(^[^<].*org.apache.catalina.core.ThreadLocalLeakPreventionListener.*$\)@<!--\1-->@' "$CATALINA_HOME"/conf/server.xml
        sed -i -e 's@\(^[^<].*org.apache.catalina.core.JreMemoryLeakPreventionListener.*$\)@<!--\1-->@' "$CATALINA_HOME"/conf/server.xml
        sed -i -e 's@\(^[^<].*org.apache.catalina.mbeans.GlobalResourcesLifecycleListener.*$\)@<!--\1-->@' "$CATALINA_HOME"/conf/server.xml
    fi

    if ! grep '<role rolename="manager-gui"/>' "$CATALINA_HOME"/conf/tomcat-users.xml; then
        sed -i -e 's@</tomcat-users>@<role rolename="manager-gui"/>\
    </tomcat-users>@g' "$CATALINA_HOME"/conf/tomcat-users.xml
    fi
    if ! grep '<role rolename="manager-script"/>' "$CATALINA_HOME"/conf/tomcat-users.xml; then
        sed -i -e 's@</tomcat-users>@<role rolename="manager-script"/>\
    </tomcat-users>@g' "$CATALINA_HOME"/conf/tomcat-users.xml
    fi
    if ! grep '<user username="tomcat" password="tomcat" roles="tomcat,manager-gui,manager-script"/>' "$CATALINA_HOME"/conf/tomcat-users.xml; then
        sed -i -e 's@</tomcat-users>@<user username="tomcat" password="tomcat" roles="tomcat,manager-gui,manager-script"/>\
    </tomcat-users>@g' "$CATALINA_HOME"/conf/tomcat-users.xml
    fi

    export CATALINA_OPTS="-Xmx$MAX_HEAP -Djersey.config.test.memleak.tomcat.magicRunnerIdentifier -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$DIST_DIR -XX:GCTimeLimit=20 -XX:GCHeapFreeLimit=30 $JVM_ARGS"

    chmod +x "$CATALINA_HOME"/bin/startup.sh

    "$CATALINA_HOME"/bin/startup.sh

    for A in `seq $TRIES_COUNT`; do
        set +e
        nc -z localhost $PORT && break
        set -e
        sleep 5
    done
}

deploy_tomcat() {
    all_proxy="" http_proxy="" curl -sS --upload-file "$WAR_PATH" "http://tomcat:tomcat@localhost:$PORT/manager/text/deploy?path=/$CONTEXT_ROOT"
}

if [ "$SKIP_START_STOP" = "true" ]; then
    echo Start skipped
else
    start_tomcat
fi

if [ "$SKIP_DEPLOY" = "true" ]; then
    echo Deployment skipped
else
    deploy_tomcat
fi

