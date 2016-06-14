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
APPLICATION_NAME=$APPLICATION_NAME
CONTEXT_ROOT=$CONTEXT_ROOT
REQUEST_PATH_QUERY=$REQUEST_PATH_QUERY
SKIP_REDEPLOY=$SKIP_REDEPLOY

if [ "$CONTEXT_ROOT" = "" -o "$WAR_PATH" = "" ]; then
    echo ARGUMENTS NOT OK
    exit 1
fi

ab -n50 -c5 "http://localhost:$PORT/$REQUEST_PATH_QUERY"

all_proxy="" http_proxy="" curl -sS "http://tomcat:tomcat@localhost:$PORT/manager/text/undeploy?path=/$CONTEXT_ROOT"

if [ "$SKIP_REDEPLOY" = "true" ]; then
    echo Skipping redeploy.
    exit
fi

all_proxy="" http_proxy="" curl -sS --upload-file "$WAR_PATH" "http://tomcat:tomcat@localhost:$PORT/manager/text/deploy?path=/$CONTEXT_ROOT&tag=$APPLICATION_NAME"

EXIT_CODE=$?
echo Redeployment finished with $EXIT_CODE

exit $EXIT_CODE
