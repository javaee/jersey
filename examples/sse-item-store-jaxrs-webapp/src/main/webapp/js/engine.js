/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

function addItem() {
    var itemInput = document.getElementById("name");

    var req = new XMLHttpRequest();
    req.open("POST", "resources/items", true);
    req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    req.onreadystatechange = function () {
        if (req.readyState == 4 && req.status == 204) {
            //Call a function when the state changes.
            itemInput.value = "";
            getItems();
        }
    };
    req.send("name=" + itemInput.value);
}

function getItems() {
    var req = new XMLHttpRequest();
    req.open("GET", "resources/items", true);
    req.setRequestHeader("Accept", "text/plain");
    req.onreadystatechange = function () {
        //Call a function when the state changes.
        if (req.readyState == 4 && req.status == 200) {
            document.getElementById("items").innerHTML = req.responseText;
        }
    };
    req.send();
}

function display(data, rgb) {
    var msgSpan = document.createElement("span");
    msgSpan.style.color = rgb;
    msgSpan.innerHTML = data;
    var msgDiv = document.createElement("div");
    msgDiv.className = "message";
    msgDiv.appendChild(msgSpan);

    var messages = document.getElementById("messages");
    messages.insertBefore(msgDiv, messages.firstChild);
}

function receiveMessages() {
    if (typeof(EventSource) !== "undefined") {
        // Yes! Server-sent events support!
        var source = new EventSource("resources/items/events");
        source.onmessage = function (event) {
            console.log('Received unnamed event: ' + event.data);
            display("Added new item: " + event.data, "#444444");
        };

        source.addEventListener("size", function(e) {
            console.log('Received event ' + event.name + ': ' + event.data);
            display("New items size: " + event.data, "#0000FF");
        }, false);

        source.onopen = function (event) {
            console.log("event source opened");
        };

        source.onerror = function (event) {
            console.log('Received error event: ' + event.data);
            display(event.data, "#FF0000");
        };
    } else {
        // Sorry! No server-sent events support..
        display('SSE not supported by browser.', "#FF0000");
    }
}

window.onload = receiveMessages;
