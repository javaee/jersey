/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server.validation.validateonexecution;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import javax.validation.ValidationException;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ValidateOnExecution;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * Testing whether an {@link javax.validation.ValidationException} is raised when {@link ValidateOnExecution} is present on
 * overriding/implementing method as well.
 *
 * @author Michal Gajdos
 */
public class ValidateOnExecutionOverrideTest extends JerseyTest {

    public static interface Validation {

        @NotNull
        @ValidateOnExecution
        public String interfaceMessage();
    }

    public abstract static class ValidationBase {

        @NotNull
        @ValidateOnExecution
        public abstract String classMessage();
    }

    @Path("/")
    public static class ValidationResource extends ValidationBase implements Validation {

        @GET
        @Path("interface")
        @ValidateOnExecution(type = ExecutableType.GETTER_METHODS)
        public String interfaceMessage() {
            return "ko";
        }

        @GET
        @Path("class")
        @ValidateOnExecution(type = ExecutableType.GETTER_METHODS)
        public String classMessage() {
            return "ko";
        }
    }

    @Override
    protected Application configure() {
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.LOG_TRAFFIC);

        set(TestProperties.RECORD_LOG_LEVEL, Level.WARNING.intValue());

        return new ResourceConfig(ValidationResource.class);
    }

    @Test
    public void testOverridingCheckOnInterface() throws Exception {
        _test("interface");
    }

    @Test
    public void testOverridingCheckOnClass() throws Exception {
        _test("class");
    }

    private void _test(final String path) throws Exception {
        assertThat(target(path).request().get().getStatus(), equalTo(500));

        final List<LogRecord> loggedRecords = getLoggedRecords();
        assertThat(loggedRecords.size(), equalTo(1));
        assertThat(loggedRecords.get(0).getThrown(), instanceOf(ValidationException.class));
    }
}
