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

#
# Environment setup:
#
# GIT_FETCH_COMMAND - specify GIT command to get sources to test, e.g. git checkout master && git pull
#
# STATUS_DIR=$HOME/.hudson-jersey2-performance-test
# mkdir -p $STATUS_DIR
# source jersey2-performance-test-common.sh
# test machine sets: cfg#, group, server, clients
# createMachineFiles 1 1 server1 client1a client1b
# createMachineFiles 1 2 server2 client2a client2b
# MEASUREMENT_DATA=~/MEASUREMENT_DATA
#

SERVER_PORT=8080

function singleTest() {
  echo "================================================================================="
  echo "===== SINGLE TEST RUN, SERVER=$SERVER_MACHINE, loader=$ab_cmdline, app=$app, JMX_URI=$JMX_URI, group_id=$group_id ====="
  echo "================================================================================="

  echo "########### going to start the test app $app"
  ssh -n jerseyrobot@${SERVER_MACHINE} '(cd workspace/jersey/tests/performance/runners/jersey2-grizzly-runner; rm -rf app; mkdir -p app; cp $HOME/workspace/jersey/tests/performance/test-cases/'$app'/target/runner/lib/* app/; JAVA_OPTIONS="-javaagent:$HOME/jersey-perftest-agent.jar" ./start.sh '$app_class' http://0.0.0.0:8080/'$app'/ 4 8)' &

  waitForGroupStatus $actual_runner $group_id "open"

  for client_machine in ${CLIENT_LIST[*]}; do
    echo "########### going to start load generator at $client_machine"
    (sleep $WAIT_FOR_APP_STARTUP_SEC; ssh -n jerseyrobot@$client_machine "nohup $ab_cmdline" & ) &
  done

  echo "########### waiting $WARM_UP_SECONDS sec to warm up server"
  sleep $WARM_UP_SECONDS

  echo "########### warm up finished, terminating ab clients..."
  for client_machine in ${CLIENT_LIST[*]}; do
    echo -n "########### warm up finished, going to stop load generator at $client_machine..."
    ssh -n jerseyrobot@$client_machine 'if ! test -e `ps h o pid -Cwrk`; then kill -s INT `ps h o pid -Cwrk` ; fi'
    echo " done."
  done

  waitForGroupStatus $actual_runner $group_id "lock"

  for client_machine in ${CLIENT_LIST[*]}; do
    echo "########### going to start load generator at $client_machine again"
    (ssh -n jerseyrobot@$client_machine "nohup $ab_cmdline" & ) &
  done

  echo "########### waiting before start capturing jmx data"
  sleep $WAIT_FOR_APP_RUNNING_SEC

  echo "########### starting jmx client to capture data"
  if ! java -cp jmxclient.jar org.glassfish.jersey.tests.performance.jmxclient.Main $JMX_URI "$mbean" OneMinuteRate $SAMPLES $filename; then
    echo "########### ERROR WHEN PROCESSING LINE#${LINE_NUMBER}, test-case: ${app}, mbean: ${mbean}, filename: ${filename}!"
  fi

  echo "########### jmx client finished, terminating ab clients..."
  for client_machine in ${CLIENT_LIST[*]}; do
    echo -n "########### going to stop load generator at $client_machine..."
    ssh -n jerseyrobot@$client_machine 'if ! test -e `ps h o pid -Cwrk`; then kill -s INT `ps h o pid -Cwrk` ; fi'
    echo " done."
  done

  echo "########### terminating test app..."
  ssh jerseyrobot@${SERVER_MACHINE} '(cd workspace/jersey/tests/performance/runners/jersey2-grizzly-runner && ./stop.sh)'

  cleanupServer $SERVER_MACHINE

  releaseRunnerAndGroup $actual_runner $group_id
}

#
# test process start
#

removeOldCapturedData

retrieveJmxClient

prepareClients

buildTestAppOnServers

echo "########### Package (lib dir) all test case applications"
for SERVER_MACHINE in ${SERVER_LIST[@]}; do
  for app in ${APP_LIST[*]}; do
    ssh jerseyrobot@${SERVER_MACHINE} '(cd workspace/jersey/tests/performance/test-cases/'$app'; mkdir -p target/runner/lib; cd target/runner/lib; unzip ../../*.zip)'
  done
done

echo "########### Build and package (lib dir) test runner"
for SERVER_MACHINE in ${SERVER_LIST[@]}; do
  ssh -n jerseyrobot@${SERVER_MACHINE} '(cd workspace/jersey; mvn -pl tests/performance/runners/jersey2-grizzly-runner clean install; cd tests/performance/runners/jersey2-grizzly-runner; mkdir -p lib; rm -f lib/*.jar; cd lib; unzip ../target/*.zip)' &
done

wait

cleanupServers

testLoop

waitForTerminator
