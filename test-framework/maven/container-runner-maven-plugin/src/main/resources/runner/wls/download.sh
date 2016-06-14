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
DIST_URL=$DIST_URL
DIST_TGT_LOCATION=$DIST_TGT_LOCATION
DIST_SUBDIR=$DIST_SUBDIR
DIST_DIR=$DIST_DIR

SKIP_START_STOP=$SKIP_START_STOP
DOWNLOAD_IF_EXISTS=$DOWNLOAD_IF_EXISTS
OVERWRITE=$OVERWRITE
all_proxy=$all_proxy

if [ "$SKIP_START_STOP" = "true" ]; then
    echo Download skipped
    exit 0
fi

if [ "$DIST_DIR" = "" -o "$DIST_TGT_LOCATION" = "" -o "$DIST_URL" = "" ]; then
    echo ARGUMENTS NOT OK
    exit 1
fi

if [ ! -f "$DIST_TGT_LOCATION" -o "$DOWNLOAD_IF_EXISTS" = "true" ]; then
    mkdir -p "$(dirname "$DIST_TGT_LOCATION")"
    curl -sS -o "$DIST_TGT_LOCATION" "$DIST_URL"
fi

if [ "$OVERWRITE" = "true" -o ! -d "$DIST_DIR"/"$DIST_SUBDIR" ]; then
    rm -rf "$DIST_DIR"/"$DIST_SUBDIR"

    cd "$DIST_DIR"
    java -jar "$DIST_TGT_LOCATION"
fi
