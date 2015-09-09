#!/bin/sh
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

set -e
[ "$DEBUG" = "true" ] && set -x

# redeclaration of env variables so that editors do not think every variable is a typo
WAR_PATH=$WAR_PATH
MAX_HEAP=$MAX_HEAP
TRIES_COUNT=$TRIES_COUNT
AS_HOME=$AS_HOME
PORT=$PORT
ADMINPORT=$ADMINPORT
SKIP_DEPLOY=$SKIP_DEPLOY
DOMAIN=$DOMAIN
APPLICATION_NAME=$APPLICATION_NAME
CONTEXT_ROOT=$CONTEXT_ROOT
JVM_ARGS=$JVM_ARGS
SKIP_START_STOP=$SKIP_START_STOP
SKIP_CHECK=$SKIP_CHECK

if [ "$AS_HOME" = "" -o "$WAR_PATH" = "" -o "$MAX_HEAP" = "" ]; then
    echo ARGUMENTS NOT OK
    exit 1
fi

chmod +x "$AS_HOME"/bin/asadmin

# Start Glassfish
if [ "$SKIP_START_STOP" = "true" ]; then
    echo Start skipped
else
    if [ "$SKIP_CHECK" != "true" ] && jps -v | grep 'jersey.config.test.memleak.gf4.magicRunnerIdentifier'; then
        echo ERROR There is already running instance of Glassfish
        exit 2
    fi

    if nc -z localhost $PORT; then
        echo ERROR port $PORT is not free!
        exit 3
    fi

    [ -d "$AS_HOME"/domains/$DOMAIN ] && rm -rf "$AS_HOME"/domains/$DOMAIN

    "$AS_HOME"/bin/asadmin create-domain --adminport $ADMINPORT --instanceport $PORT --nopassword $DOMAIN
    "$AS_HOME"/bin/asadmin start-domain --port $ADMINPORT $DOMAIN

    set +e
    "$AS_HOME"/bin/asadmin delete-jvm-options --port $ADMINPORT --target default-config -Xmx512m
    "$AS_HOME"/bin/asadmin delete-jvm-options --port $ADMINPORT --target default-config -Xmx$MAX_HEAP

    "$AS_HOME"/bin/asadmin delete-jvm-options --port $ADMINPORT --target server-config -Xmx512m
    "$AS_HOME"/bin/asadmin delete-jvm-options --port $ADMINPORT --target server-config -Xmx$MAX_HEAP

    set -e
    "$AS_HOME"/bin/asadmin create-jvm-options --port $ADMINPORT --target default-config -Xmx$MAX_HEAP
    "$AS_HOME"/bin/asadmin create-jvm-options --port $ADMINPORT --target server-config -Xmx$MAX_HEAP

    # add magic runner identifier so that we can identify other processes
    "$AS_HOME"/bin/asadmin create-jvm-options --port $ADMINPORT --target default-config -Djersey.config.test.memleak.gf4.magicRunnerIdentifier
    "$AS_HOME"/bin/asadmin create-jvm-options --port $ADMINPORT --target server-config -Djersey.config.test.memleak.gf4.magicRunnerIdentifier

    # if JVM_ARGS doesn't contain following vm options, set them (increases the probability of OOME GC Overhead exceeded)
    echo "$JVM_ARGS" | grep GCTimeLimit > /dev/null || "$AS_HOME"/bin/asadmin create-jvm-options --port $ADMINPORT --target server-config "-XX\:GCTimeLimit=20"
    echo "$JVM_ARGS" | grep GCHeapFreeLimit > /dev/null || "$AS_HOME"/bin/asadmin create-jvm-options --port $ADMINPORT --target server-config "-XX\:GCHeapFreeLimit=30"

    if [ "$JVM_ARGS" != "" ]; then
        for JVM_ARG in `echo $JVM_ARGS`; do
            "$AS_HOME"/bin/asadmin create-jvm-options --port $ADMINPORT --target default-config "$JVM_ARG"
            "$AS_HOME"/bin/asadmin create-jvm-options --port $ADMINPORT --target server-config "$JVM_ARG"
        done
    fi

    "$AS_HOME"/bin/asadmin stop-domain --port $ADMINPORT --force=true $DOMAIN
    "$AS_HOME"/bin/asadmin start-domain --port $ADMINPORT $DOMAIN
fi

# Deploy to Glassfish
if [ "$SKIP_DEPLOY" = "true" ]; then
    echo Deployment skipped
else
    "$AS_HOME"/bin/asadmin deploy --port $ADMINPORT --contextroot $CONTEXT_ROOT --name $APPLICATION_NAME "$WAR_PATH"
fi
