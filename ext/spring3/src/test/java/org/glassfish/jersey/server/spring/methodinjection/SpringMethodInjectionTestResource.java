/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.spring.methodinjection;

import org.glassfish.jersey.server.spring.NoComponent;
import org.glassfish.jersey.server.spring.TestComponent1;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class SpringMethodInjectionTestResource {

    private TestComponent1 testComponent1;
    private List<org.glassfish.jersey.server.spring.TestComponent2> testComponent2List;
    private Set<org.glassfish.jersey.server.spring.TestComponent2> testComponent2Set;
    private NoComponent noComponent;

    @Autowired
    public void setTestComponent1(TestComponent1 testComponent1) {
        this.testComponent1 = testComponent1;
    }

    @Autowired
    public void setTestComponent2List(List<org.glassfish.jersey.server.spring.TestComponent2> testComponent2List) {
        this.testComponent2List = testComponent2List;
    }

    @Autowired
    public void setTestComponent2Set(Set<org.glassfish.jersey.server.spring.TestComponent2> testComponent2Set) {
        this.testComponent2Set = testComponent2Set;
    }

    @Autowired(required = false)
    public void setNoComponent(NoComponent noComponent) {
        this.noComponent = noComponent;
    }

    @Path("test1")
    @GET
    public String test1() {
        return testComponent1.result();
    }

    @Path("test2")
    @GET
    public String test2() {
        return (testComponent2List.size() == 2 && "test ok".equals(testComponent2List.get(0).result())
                && "test ok".equals(testComponent2List.get(1).result())) ? "test ok" : "test failed";
    }

    @Path("test3")
    @GET
    public String test3() {
        java.util.Iterator<org.glassfish.jersey.server.spring.TestComponent2> iterator = testComponent2Set.iterator();
        return (testComponent2Set.size() == 2 && "test ok".equals(iterator.next().result())
                && "test ok".equals(iterator.next().result())) ? "test ok" : "test failed";
    }

    @Path("JERSEY-2643")
    @GET
    public String JERSEY_2643() {
        return noComponent == null ? "test ok" : "test failed";
    }

}
