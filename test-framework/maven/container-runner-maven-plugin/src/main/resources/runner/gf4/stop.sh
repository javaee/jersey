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
AS_HOME=$AS_HOME
PORT=$PORT
ADMINPORT=$ADMINPORT
DOMAIN=$DOMAIN
SKIP_START_STOP=$SKIP_START_STOP
SKIP_STOP=$SKIP_STOP

PID_SUBDIR=domains/$DOMAIN/config/pid
STOP_TIME=20
KILL_TIME=5

if [ "$SKIP_START_STOP" = "true" -o "$SKIP_STOP" = "true" ]; then
    echo Stop skipped
    exit 0
fi

if [ "$AS_HOME" = "" -o "$DOMAIN" = "" ]; then
    echo ARGUMENTS NOT OK
    exit 1
fi

chmod +x "$AS_HOME"/bin/asadmin

"$AS_HOME"/bin/asadmin stop-domain --port $ADMINPORT --force=true $DOMAIN &
STOP_PID=$!
# give Glassfish $STOP_TIME seconds to stop .. this may hang forever since JVM might have thrown OutOfMemoryError
for A in `seq $STOP_TIME`; do
    kill -0 $STOP_PID || break;
    sleep 1
done

set +e
kill -9 $STOP_PID

[ -f "$AS_HOME"/$PID_SUBDIR ] && kill -9 `cat "$AS_HOME"/$PID_SUBDIR`

# Wait for
for A in `seq $KILL_TIME`; do
    nc -z localhost $ADMINPORT || nc -z localhost $PORT || exit 0
    sleep 1
done

echo ERROR Glassfish server seems to not be shutdown!
exit 2