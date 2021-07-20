/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.internal.inject;

import java.math.BigDecimal;
import java.util.SortedSet;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Paul Sandoz
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class QueryParamSortedSetStringConstructorTest extends AbstractTest {

    @Path("/")
    public static class ResourceStringSortedSet {
        @GET
        public String doGetString(@QueryParam("args") SortedSet<BigDecimal> args) {
            assertTrue(args.contains(new BigDecimal("3.145")));
            assertTrue(args.contains(new BigDecimal("2.718")));
            assertTrue(args.contains(new BigDecimal("1.618")));
            return "content";
        }
    }

    @Path("/")
    public static class ResourceStringSortedSetEmptyDefault {
        @GET
        public String doGetString(@QueryParam("args") SortedSet<BigDecimal> args) {
            assertEquals(0, args.size());
            return "content";
        }
    }

    @Path("/")
    public static class ResourceStringSortedSetDefault {
        @GET
        public String doGetString(
                @QueryParam("args") @DefaultValue("3.145") SortedSet<BigDecimal> args) {
            assertTrue(args.contains(new BigDecimal("3.145")));
            return "content";
        }
    }

    @Path("/")
    public static class ResourceStringSortedSetDefaultOverride {
        @GET
        public String doGetString(
                @QueryParam("args") @DefaultValue("3.145") SortedSet<BigDecimal> args) {
            assertTrue(args.contains(new BigDecimal("2.718")));
            return "content";
        }
    }

    @Test
    public void testStringConstructorSortedSetGet() throws ExecutionException, InterruptedException {
        initiateWebApplication(ResourceStringSortedSet.class);

        _test("/?args=3.145&args=2.718&args=1.618", "application/stringSortedSet");
    }

    @Test
    public void testStringConstructorSortedSetNullDefault() throws ExecutionException, InterruptedException {
        initiateWebApplication(ResourceStringSortedSetEmptyDefault.class);

        _test("/");
    }

    @Test
    public void testStringConstructorSortedSetDefault() throws ExecutionException, InterruptedException {
        initiateWebApplication(ResourceStringSortedSetDefault.class);

        _test("/");
    }

    @Test
    public void testStringConstructorSortedSetDefaultOverride() throws ExecutionException, InterruptedException {
        initiateWebApplication(ResourceStringSortedSetDefaultOverride.class);

        _test("/?args=2.718");
    }
}
