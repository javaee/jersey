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

APP_LIST=(mbw-text-plain \
          mbw-json-moxy mbw-json-jackson \
          mbw-xml-moxy mbw-xml-jaxb \
          mbw-custom-provider \
          mbw-kryo \
          param-srl \
          filter-global filter-name filter-dynamic \
          interceptor-global interceptor-name interceptor-dynamic \
          proxy-injection)

WARM_UP_SECONDS=300
WAIT_FOR_APP_STARTUP_SEC=5
WAIT_FOR_APP_RUNNING_SEC=60
CHECK_RUNNER_INTERVAL=5
CHECK_TERM_INTERVAL=10

JMX_URI_TEMPLATE="service:jmx:rmi:///jndi/rmi://SERVER_MACHINE:11112/jmxrmi"
SAMPLES=30

#uncomment for debug purposes:
#WARM_UP_SECONDS=3
#WAIT_FOR_APP_STARTUP_SEC=2
#WAIT_FOR_APP_RUNNING_SEC=1
#CHECK_RUNNER_INTERVAL=1
#CHECK_TERM_INTERVAL=2
#SAMPLES=3
#/debug

MODULES_TO_BUILD=""
for app in ${APP_LIST[*]}; do
  MODULES_TO_BUILD="$MODULES_TO_BUILD,tests/performance/test-cases/$app"
done
MODULES_TO_BUILD=`echo $MODULES_TO_BUILD|sed -e's/,//'`

SERVER_LIST=()
CLIENT_LIST_ALL=()


function waitForGroupStatus() {
  echo "########### Waiting for group status: $*"
  RUNNER_ID=$1
  shift
  GROUP_ID=$1
  shift
  STATUS=$1

  echo "#`date`" > $STATUS_DIR/.runner.$RUNNER_ID.waiting.$GROUP_ID.$STATUS
  FILE="$STATUS_DIR/.group.$GROUP_ID.running.$RUNNER_ID.$STATUS"

  rm -f $STATUS_DIR/.group.$GROUP_ID.running.$RUNNER_ID.*

  available=false
  while [ "$available" != true ]; do
    if [ -e "$FILE" ]; then
      available=true
    else
      sleep 1
    fi
  done
}

function releaseRunnerAndGroup() {
  echo "########### Release Runner and Group: $*"
  RUNNER_ID=$1
  shift
  GROUP_ID=$1

  echo "#`date`" > $STATUS_DIR/.runner.$RUNNER_ID.available
  rm -f $STATUS_DIR/.group.$GROUP_ID.running.$RUNNER_ID.* $STATUS_DIR/.runner.$RUNNER_ID.running
}

function checkWaitingRunners() {
  echo "########### Check Waiting Runners"
  for waiting_file in `ls $STATUS_DIR/.runner.*.waiting.*.lock 2> /dev/null`; do
    filename_array=(`basename $waiting_file | tr "." " "`)

    runner_id=${filename_array[1]}
    group_id=${filename_array[3]}
    status=${filename_array[4]}

    _files=($STATUS_DIR/.group.$group_id.running.*)
    if [ ! -f "${_files}" ]; then
      echo "#`date`" > "$STATUS_DIR/.group.$group_id.running.$runner_id.lock"
      rm $waiting_file
    fi
  done

  for waiting_file in `ls $STATUS_DIR/.runner.*.waiting.*.open 2> /dev/null`; do
    filename_array=(`basename $waiting_file | tr "." " "`)

    runner_id=${filename_array[1]}
    group_id=${filename_array[3]}
    status=${filename_array[4]}

    _files=($STATUS_DIR/.group.$group_id.running.*.lock)
    if [ ! -f "${_files}" ]; then
      echo "#`date`" > "$STATUS_DIR/.group.$group_id.running.$runner_id.open"
      rm $waiting_file
    fi
  done
}

function createMachineFiles {
  echo "########### Creating machine files in $STATUS_DIR for: $*"
  RUNNER_ID=$1
  shift
  GROUP_ID=$1
  shift
  SERVER_MACHINE=$1
  shift
  CLIENT_LIST=($@)

  echo ${GROUP_ID} > $STATUS_DIR/.runner.$RUNNER_ID.group
  echo "#`date`" > $STATUS_DIR/.runner.$RUNNER_ID.available
  echo ${SERVER_MACHINE} > $STATUS_DIR/.runner.$RUNNER_ID.server
  echo ${CLIENT_LIST[@]} > $STATUS_DIR/.runner.$RUNNER_ID.clients

  SERVER_LIST=("${SERVER_LIST[@]}" "$SERVER_MACHINE")
  CLIENT_LIST_ALL=("${CLIENT_LIST_ALL[@]}" "${CLIENT_LIST[@]}")
}

function waitForTerminator {
  echo "########### Waiting for finish"
  # wait for the last round to finish
  terminated=false
  while [ "$terminated" != true ]; do
    checkWaitingRunners
    _files=($STATUS_DIR/.runner.*.running)
    if [ ! -f "${_files}" ]; then
      terminated=true
    fi
    if [ "$terminated" != true ]; then
      echo "########### Terminated tests: $terminated, waiting $CHECK_TERM_INTERVAL sec..."
      sleep $CHECK_TERM_INTERVAL
    fi
  done

  echo "DONE!"

  wait
  sleep 4
  wait
}

function testLoop {
  # Following is the main measurement loop
  # MEASUREMENT_DATA is the input data in the following format:
  # application directory name|command line to generate load on client machines|JMX URI for the application|MBean name|output filename

  echo "########### Let's test it, reading from $MEASUREMENT_DATA file"

  TOTAL_COUNT=`cat $MEASUREMENT_DATA | grep -v "^#" | grep "^." | wc -l | sed -e 's/ *//g'`
  INDEX=0
  cat $MEASUREMENT_DATA | grep -v "^#" | grep "^." | while IFS="\|" read app ab_cmdline app_class agent_param mbean filename
  do
    INDEX=`expr $INDEX + 1`
    echo "========================================= DATA [$INDEX/$TOTAL_COUNT] =============================================="
    echo "app       = $app"
    echo "app_class = $app_class"
    echo "ab_cmdline= $ab_cmdline"

    spawned=false
    while [ "$spawned" != true ]; do
      for runner_file in `ls $STATUS_DIR/.runner.*.available 2> /dev/null`; do
        if [ "$spawned" != true ]; then
          filename_array=(`basename $runner_file | tr "." " "`)
          actual_runner=${filename_array[1]}
          echo "#`date`" > $STATUS_DIR/.runner.$actual_runner.running
          rm $runner_file

          SERVER_MACHINE=`cat $STATUS_DIR/.runner.$actual_runner.server`
          APP_CONTEXT=$app
          CLIENT_LIST=(`cat $STATUS_DIR/.runner.$actual_runner.clients`)
          ab_cmdline=`echo $ab_cmdline | sed -e"s/SERVER_MACHINE/$SERVER_MACHINE/" | sed -e"s/SERVER_PORT/$SERVER_PORT/" | sed -e"s/APP_CONTEXT/$APP_CONTEXT/"`
          JMX_URI=`echo $JMX_URI_TEMPLATE | sed -e"s/SERVER_MACHINE/$SERVER_MACHINE/"`
          group_id=`cat $STATUS_DIR/.runner.$actual_runner.group`

          echo "#[$INDEX/$TOTAL_COUNT]" >> $STATUS_DIR/.runner.$actual_runner.running
          echo "$app" >> $STATUS_DIR/.runner.$actual_runner.running
          echo "$app_class" >> $STATUS_DIR/.runner.$actual_runner.running
          echo "$ab_cmdline" >> $STATUS_DIR/.runner.$actual_runner.running
          echo "$filename" >> $STATUS_DIR/.runner.$actual_runner.running
          echo "$mbean" >> $STATUS_DIR/.runner.$actual_runner.running
          echo "$agent_param" >> $STATUS_DIR/.runner.$actual_runner.running

          spawned=true
          singleTest &
        fi
      done
      checkWaitingRunners
      if [ "$spawned" != true ]; then
        sleep $CHECK_RUNNER_INTERVAL
      fi
    done
  done
}

function removeOldCapturedData {
  rm -f $WORKSPACE/*.properties
}

function retrieveJmxClient {
  echo "########### Retrieving JMX client"
  scp jerseyrobot@${SERVER_LIST[0]}:jmxclient.jar .
}

function prepareClients {
  echo "########### Copy client files to each client"
  for CLIENT_MACHINE in ${CLIENT_LIST_ALL[@]}; do
    echo "... copy client files to ${CLIENT_MACHINE}"
    scp $WORKSPACE/jersey2/tests/performance/etc/client/* jerseyrobot@${CLIENT_MACHINE}:. &
  done

  wait
}

function buildTestAppOnServers {
  echo "########### Building test applications on each server"
  # git fetch jersey on the server machine and build all apps there:
  for SERVER_MACHINE in ${SERVER_LIST[@]}; do
    ssh -n jerseyrobot@${SERVER_MACHINE} '(cd $HOME/workspace/jersey && '$GIT_FETCH_COMMAND' && mvn -pl '$MODULES_TO_BUILD' -am -Dskip.tests=true clean install)' &
  done

  wait
}

function cleanupHudsonSlave {
  rm -f $STATUS_DIR/.runner.* $STATUS_DIR/.group.*
}

function cleanupServer {
  echo "########### Kill java processes on server $1"
  ssh -n jerseyrobot@$1 'if ! test -e `ps h o pid -Cjava`; then kill -s TERM `ps h o pid -Cjava` ; fi'
}

function cleanupServers {
  echo "########### Kill all java processes on each server"
  for SERVER_MACHINE in ${SERVER_LIST[@]}; do
    cleanupServer ${SERVER_MACHINE}
  done
}


trap "cleanupHudsonSlave; cleanupServers" EXIT SIGTERM SIGINT

#uncomment for debug purposes:
#trap 'echo "[$BASH_SOURCE:$LINENO] $BASH_COMMAND" >> .debug; tail -10 .debug > .debug.swap; mv .debug.swap .debug' DEBUG
