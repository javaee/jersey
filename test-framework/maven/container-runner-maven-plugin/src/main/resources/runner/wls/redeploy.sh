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
MW_HOME=$MW_HOME
DOMAIN=$DOMAIN
PORT=$PORT
APPLICATION_NAME=$APPLICATION_NAME
SKIP_REDEPLOY=$SKIP_REDEPLOY

DEPLOY_TIMEOUT=30000
CONNECT_TIMEOUT=30000
RENAMED_WAR_PATH="$MW_HOME"/$CONTEXT_ROOT.${WAR_PATH##*.}

if [ "$WAR_PATH" = "" -o "$DOMAIN" = "" -o "$MW_HOME" = "" ]; then
    echo ARGUMENTS NOT OK
    exit 1
fi

export JAVA_OPTIONS=-Djava.endorsed.dirs="$JAVA_HOME/jre/lib/endorsed":"$MW_HOME/oracle_common/modules/endorsed"

set +x
. "$MW_HOME"/wlserver/server/bin/setWLSEnv.sh
[ "$DEBUG" = "true" ] && set -x

cd "$MW_HOME/$DOMAIN"

java weblogic.WLST << EOF
from java.util import *
from javax.management import *
import javax.management.Attribute
print "WLST:  Connecting..."
connect("weblogic", "weblogic1", "localhost:$PORT")
undeploy("$APPLICATION_NAME")
print "WLST: Application undeployed, exiting."
exit()
EOF

if [ "$SKIP_REDEPLOY" = "true" ]; then
    echo Skipping redeploy.
    exit
fi

# It is impossible to easilly set the context root in Weblogic besides changing the war name
if [ "${WAR_PATH##*/}" != "${RENAMED_WAR_PATH##*/}" ]; then
    [ -f "$RENAMED_WAR_PATH" ] && rm "$RENAMED_WAR_PATH"
    ln -s "$WAR_PATH" "$RENAMED_WAR_PATH"
else
    RENAMED_WAR_PATH="$WAR_PATH"
fi

set +e

java weblogic.WLST << EOF
from java.util import *
from javax.management import *
import javax.management.Attribute
print "WLST:  Connecting..."
try:
    connect("weblogic", "weblogic1", "localhost:$PORT", timeout=$CONNECT_TIMEOUT)
    progress = deploy("$APPLICATION_NAME", "$RENAMED_WAR_PATH", timeout=$DEPLOY_TIMEOUT)
    if not progress.isCompleted():
        print "WLST: Deployment wasn't completed successfully. Failure: " + progress.getState()
        exit(exitcode=3)
except Exception, e:
    print "Exception occurred! " + str(e)
    exit(exitcode=4)

print "WLST: Deployment finished, exiting."
exit()
EOF

EXIT_CODE=$?
echo Deployment finished with $EXIT_CODE

exit $EXIT_CODE