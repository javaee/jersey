<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.

    The contents of this file are subject to the terms of either the GNU
    General Public License Version 2 only ("GPL") or the Common Development
    and Distribution License("CDDL") (collectively, the "License").  You
    may not use this file except in compliance with the License.  You can
    obtain a copy of the License at
    http://glassfish.java.net/public/CDDL+GPL_1_1.html
    or packager/legal/LICENSE.txt.  See the License for the specific
    language governing permissions and limitations under the License.

    When distributing the software, include this License Header Notice in each
    file and include the License file at packager/legal/LICENSE.txt.

    GPL Classpath Exception:
    Oracle designates this particular file as subject to the "Classpath"
    exception as provided by Oracle in the GPL Version 2 section of the License
    file that accompanied this code.

    Modifications:
    If applicable, add the following below the License Header, with the fields
    enclosed by brackets [] replaced by your own identifying information:
    "Portions Copyright [year] [name of copyright owner]"

    Contributor(s):
    If you wish your version of this file to be governed by only the CDDL or
    only the GPL Version 2, indicate your decision by adding "[Contributor]
    elects to include this software in this distribution under the [CDDL or GPL
    Version 2] license."  If you don't indicate a single choice of license, a
    recipient has the option to distribute your version of this file under
    either the CDDL, the GPL Version 2 or to extend the choice of license to
    its licensees as provided above.  However, if you add GPL Version 2 code
    and therefore, elected the GPL Version 2 license, then the option applies
    only if the new code is made subject to such option by the copyright
    holder.

-->
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <title>Feed Combiner</title>

    <!-- Bootstrap core CSS -->
    <link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css" rel="stylesheet">

    <!-- Bootstrap theme -->
    <link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap-theme.min.css" rel="stylesheet">

    <style type="text/css">
        body {
            padding-top: 70px;
            padding-bottom: 30px;
        }

        .theme-dropdown .dropdown-menu {
            position: static;
            display: block;
            margin-bottom: 20px;
        }

        .theme-showcase > p > .btn {
            margin: 5px 0;
        }

        .theme-showcase .navbar .container {
            width: auto;
        }
    </style>
</head>

<body role="document">

<!-- Fixed navbar -->
<nav class="navbar navbar-inverse navbar-fixed-top">
    <div class="container">
        <div class="navbar-header">
            <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar"
                    aria-expanded="false" aria-controls="navbar">
                <span class="sr-only">Toggle navigation</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="#">Feed Combiner</a>
        </div>
    </div>
</nav>

<div class="container theme-showcase" role="main">
    <div class="jumbotron" style="padding: 20px;">
        <h3>Add a new combined feed</h3>

        <p>

        <form name="feedCreator" action="/combiner" method="post">
            <div class="form-group">
                <label for="title">* Title</label>
                <input type="text" class="form-control" name="title" placeholder="Combined Feed Title">
            </div>
            <div class="form-group">
                <label for="description">* Description</label>
                <input type="text" class="form-control" name="description" placeholder="Combined Feed Description">
            </div>
            <div class="form-group">
                <label for="urls">* URLs (comma separated)</label>
                <textarea class="form-control" rows="5" name="urls" placeholder="Feed URL"></textarea>
            </div>
        <div class="form-group">
                <label for="urls">Refresh Period (in seconds)</label>
                <input type="text" class="form-control" name="refreshPeriod" placeholder="Feed Refresh Period">
            </div>
            <button type="submit" class="btn btn-primary">Submit</button>
        </form>
        </p>
    </div>

    <div class="page-header">
        <h1>Combined Feeds</h1>
    </div>
    <div>
        <div>
        <#if feeds?has_content>
            <table class="table table-striped">
                <thead>
                <tr>
                    <th>ID</th>
                    <th>Title</th>
                    <th>Description</th>
                    <th>URLs</th>
                    <th>Refresh</th>
                    <th></th>
                </tr>
                </thead>
                <tbody>
                    <#list feeds as feed>
                    <tr>
                        <th>${feed.id}</th>
                        <td><a href="http://localhost:8080/combiner/${feed.id}">${feed.title}</td>
                        <td>${feed.description}</td>
                        <td>
                            <#list feed.urls as url>
                            ${url} <br/>
                            </#list>
                        </td>
                        <td>${feed.refreshPeriod?c}</td>
                        <td>
                            <form action="/combiner/delete/${feed.id}?_method=DELETE" method="post">
                                <button type="submit">DELETE</button>
                            </form>
                        </td>
                    </tr>
                    </#list>
                </tbody>
            </table>
        <#else>
            <div class="alert alert-warning" role="alert">
                <strong>Warning!</strong> There is no Combined Feed!
            </div>
        </#if>
        </div>
    </div>
</div>
<!-- /container -->

<script src="//code.jquery.com/jquery-1.11.3.min.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js"></script>

</body>
</html>