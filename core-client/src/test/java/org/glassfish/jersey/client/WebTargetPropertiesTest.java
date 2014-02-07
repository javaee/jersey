/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.client;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author pavel.bucek@oracle.com
 */
public class WebTargetPropertiesTest {

    @Test
    public void testPropagation() {
        Client c = ClientBuilder.newBuilder().newClient();

        c.property("a", "val");

        WebTarget w1 = c.target("http://a");
        w1.property("b", "val");

        WebTarget w2 = w1.path("c");
        w2.property("c", "val");

        assertTrue(c.getConfiguration().getProperties().containsKey("a"));
        assertTrue(w1.getConfiguration().getProperties().containsKey("a"));
        assertTrue(w2.getConfiguration().getProperties().containsKey("a"));

        assertFalse(c.getConfiguration().getProperties().containsKey("b"));
        assertTrue(w1.getConfiguration().getProperties().containsKey("b"));
        assertTrue(w2.getConfiguration().getProperties().containsKey("b"));

        assertFalse(c.getConfiguration().getProperties().containsKey("c"));
        assertFalse(w1.getConfiguration().getProperties().containsKey("c"));
        assertTrue(w2.getConfiguration().getProperties().containsKey("c"));

        w2.property("a", null);

        assertTrue(c.getConfiguration().getProperties().containsKey("a"));
        assertTrue(w1.getConfiguration().getProperties().containsKey("a"));
        assertFalse(w2.getConfiguration().getProperties().containsKey("a"));
    }
}
