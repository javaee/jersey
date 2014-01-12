/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
"use strict";

var stage = new Kinetic.Stage({
    container: 'container',
    width: 800,
    height: 350
});

var layer = new Kinetic.Layer();
stage.add(layer);

var flights = {};

function receiveMessages() {
    if (typeof(EventSource) !== "undefined") {
        // Yes! Server-sent events support!
        var source = new EventSource('/api/simulation/events');
        source.onmessage = function (event) {
            var data = JSON.parse(event.data);
            console.log(data);

            var flight = flights[data.flightId];
            if (flight == null) {
                var marker = new Kinetic.Circle({
                    x: 0,
                    y: 0,
                    radius: 5,
                    fillRGB: {
                        r: data.location.x % 256,
                        g: data.location.y % 256,
                        b: data.location.x + data.location.y % 256
                    },
                    stroke: 'black',
                    strokeWidth: 1
                });
                var label = new Kinetic.Text({
                    x: 10,
                    y: 0,
                    text: data.flightId,
                    fontSize: 12,
                    fontFamily: 'Calibri',
                    fill: 'black'
                });
                flight = new Kinetic.Group({
                    x: data.location.x,
                    y: data.location.y
                });
                flight.add(marker);
                flight.add(label);
                flights[data.flightId] = flight;
                layer.add(flight);
            } else {
                var tween = new Kinetic.Tween({
                    node: flight,
                    duration: 0.5,
                    x: data.location.x,
                    y: data.location.y
                });
                tween.play();
            }
        };

        source.onopen = function (event) {
            // Connection was opened.
            console.log('opened')
        };

        source.onclose = function (event) {
            // Connection was closed.
            console.log('connection closed')
        };
    } else {
        // Sorry! No server-sent events support..
        console.log('SSE not supported by browser.')
    }
}

function startStop() {
    var button = document.getElementById("startStop");
    var xmlhttp = new XMLHttpRequest();
    if (button.innerHTML == "Start") {
        button.innerHTML = "Stop";
        xmlhttp.open("POST", "api/simulation", true);
        xmlhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
        xmlhttp.send("command=START");
    } else {
        button.innerHTML = "Start";
        xmlhttp.open("POST", "api/simulation", true);
        xmlhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
        xmlhttp.send("command=STOP");
    }
}

receiveMessages();
