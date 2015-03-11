/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.client.proxy;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

public class MyResource implements MyResourceIfc {

    @Context HttpHeaders headers;

    @Override
    public String getIt() {
        return "Got it!";
    }

    @Override
    public List<MyBean> postIt(List<MyBean> entity) {
        return entity;
    }

    @Override
    public MyBean postValid(@Valid MyBean entity) {
        return entity;
    }

    @Override
    public String getId(String id) {
        return id;
    }

    @Override
    public String getByName(String name) {
        return name;
    }

    @Override
    public String getByNameCookie(String name) {
        return name;
    }

    @Override
    public String getByNameHeader(String name) {
        return name;
    }

    @Override
    public String getByNameMatrix(String name) {
        return name;
    }

    @Override
    public String postByNameFormParam(String name) {
        return name;
    }

    @Override
    public String getByNameList(List<String> name) {
        return name.size() + ":" + name;
    }

    @Override
    public String getByNameSet(Set<String> name) {
        return name.size() + ":" + name;
    }

    @Override
    public String getByNameSortedSet(SortedSet<String> name) {
        return name.size() + ":" + name;
    }

    @Override
    public String getByNameCookieList(List<String> name) {
        return name.size() + ":" + name;
    }

    @Override
    public String getByNameCookieSet(Set<String> name) {
        return name.size() + ":" + name;
    }

    @Override
    public String getByNameCookieSortedSet(SortedSet<String> name) {
        return name.size() + ":" + name;
    }

    @Override
    public String getByNameHeaderList(List<String> name) {
        return name.size() + ":" + name;
    }

    @Override
    public String getByNameHeaderSet(Set<String> name) {
        return name.size() + ":" + name;
    }

    @Override
    public String getByNameHeaderSortedSet(SortedSet<String> name) {
        return name.size() + ":" + name;
    }

    @Override
    public String getByNameMatrixList(List<String> name) {
        return name.size() + ":" + name;
    }

    @Override
    public String getByNameMatrixSet(Set<String> name) {
        return name.size() + ":" + name;
    }

    @Override
    public String getByNameMatrixSortedSet(SortedSet<String> name) {
        return name.size() + ":" + name;
    }

    @Override
    public String postByNameFormList(List<String> name) {
        return name.size() + ":" + name;
    }

    @Override
    public String postByNameFormSet(Set<String> name) {
        return name.size() + ":" + name;
    }

    @Override
    public String postByNameFormSortedSet(SortedSet<String> name) {
        return name.size() + ":" + name;
    }

    @Override
    public MySubResourceIfc getSubResource() {
        return new MySubResource();
    }

    @Override
    public boolean isAcceptHeaderValid(HttpHeaders headers) {
        List<MediaType> accepts = headers.getAcceptableMediaTypes();
        return accepts.contains(MediaType.TEXT_PLAIN_TYPE) && accepts.contains(MediaType.TEXT_XML_TYPE);
    }

    @Override
    public String putIt(MyBean dummyBean) {
        return headers.getHeaderString(HttpHeaders.CONTENT_TYPE);
    }
}
