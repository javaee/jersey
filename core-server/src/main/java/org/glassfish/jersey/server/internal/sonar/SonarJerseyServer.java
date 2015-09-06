/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal.sonar;

/**
 * The purpose of this class is to verify the reported test coverage shows correct results in various modes of test executions.
 * For further details, see javadoc bellow.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
public class SonarJerseyServer {

    /**
     * This method is invoked indirectly from the tests.
     */
    private String server() {
        return "server";
    }

    /**
     * A method that is executed from a unit test by maven surefire plugin within the same Maven module.
     */
    public String unitTest() {
        return server() + " unit test";
    }

    /**
     * This method is executed from a unit test by maven surefire plugin from a dependant module.
     */
    public String e2e() {
        return server() + " e2e";
    }

    /**
     * A method that is executed in a JVM of maven failsafe plugin from a dependant maven module. The call is executed directly.
     */
    public String integrationTestJvm() {
        return server() + " test jvm";
    }

    /**
     * This method is executed from a server (Jetty for instance) during the integration test phase. This server is called by a
     * JUnit test that is executed by maven failsafe plugin.
     */
    public String integrationServerJvm() {
        return server() + " server jvm";
    }
}
