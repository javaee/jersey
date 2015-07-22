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
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <title>Feed Entries</title>

    <!-- Bootstrap core CSS -->
    <link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css" rel="stylesheet">

    <!-- Bootstrap theme -->
    <link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap-theme.min.css" rel="stylesheet">
</head>
<body>

    <header class="page-header" style="padding-left: 15px">
        <div style="text-align: right; padding-right: 15px;">
            <a href="/combiner" class="btn btn-info" role="button"><< BACK</a>
        </div>
        <h1 style="">${model.title}</h1>
        <blockquote>
            <p>${model.description}</p>
        </blockquote>
    </header>
    <div>
        <div>
        <#if model.feedEntries?has_content>
            <table class="table table-striped">
                <thead>
                <tr>
                    <th>Title</th>
                    <th>Publish Date</th>
                    <th>Link</th>
                </tr>
                </thead>
                <tbody>
                    <#list model.feedEntries as i>
                    <tr>
                        <td>${i.publishDate?datetime}</td>
                        <td>${i.title}</td>
                        <td><a href="${i.link}">${i.link}</a></td>
                    </tr>
                    </#list>
                </tbody>
            </table>
        <#else>
            <div class="alert alert-warning" role="alert">
                <strong>Warning!</strong> There are no entries!
            </div>
        </#if>
        </div>
    </div>
</body>
</html>