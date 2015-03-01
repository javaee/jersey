/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.api;

import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;

import org.glassfish.jersey.message.internal.HttpHeaderReader;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Marc Hadley
 */
public class CookieImplTest {

    @Test
    public void testCookieToString() {
        Cookie cookie = new Cookie("fred", "flintstone");
        String expResult = "$Version=1;fred=flintstone";
        assertEquals(expResult, cookie.toString());

        cookie = new Cookie("fred", "flintstone", "/path", null);
        expResult = "$Version=1;fred=flintstone;$Path=/path";
        assertEquals(expResult, cookie.toString());

        cookie = new Cookie("fred", "flintstone", "/path", ".sun.com");
        expResult = "$Version=1;fred=flintstone;$Domain=.sun.com;$Path=/path";
        assertEquals(expResult, cookie.toString());

        cookie = new Cookie("fred", "flintstone", "/path", ".sun.com", 2);
        expResult = "$Version=2;fred=flintstone;$Domain=.sun.com;$Path=/path";
        assertEquals(expResult, cookie.toString());
    }

    @Test
    public void testCookieValueOf() {
        Cookie cookie = Cookie.valueOf("$Version=2;fred=flintstone");
        assertEquals("fred", cookie.getName());
        assertEquals("flintstone", cookie.getValue());
        assertEquals(2, cookie.getVersion());

        cookie = Cookie.valueOf("$Version=1;fred=flintstone;$Path=/path");
        assertEquals("fred", cookie.getName());
        assertEquals("flintstone", cookie.getValue());
        assertEquals(1, cookie.getVersion());
        assertEquals("/path", cookie.getPath());

        cookie = Cookie.valueOf("$Version=1;fred=flintstone;$Domain=.sun.com;$Path=/path");
        assertEquals("fred", cookie.getName());
        assertEquals("flintstone", cookie.getValue());
        assertEquals(1, cookie.getVersion());
        assertEquals(".sun.com", cookie.getDomain());
        assertEquals("/path", cookie.getPath());
    }

    @Test
    public void testCreateCookies() {
        String cookieHeader = "fred=flintstone";
        Map<String, Cookie> cookies = HttpHeaderReader.readCookies(cookieHeader);
        assertEquals(cookies.size(), 1);
        Cookie c = cookies.get("fred");
        assertEquals(c.getVersion(), 0);
        assertTrue("fred".equals(c.getName()));
        assertTrue("flintstone".equals(c.getValue()));

        cookieHeader = "fred=flintstone,barney=rubble";
        cookies = HttpHeaderReader.readCookies(cookieHeader);
        assertEquals(cookies.size(), 2);
        c = cookies.get("fred");
        assertEquals(c.getVersion(), 0);
        assertTrue("fred".equals(c.getName()));
        assertTrue("flintstone".equals(c.getValue()));
        c = cookies.get("barney");
        assertEquals(c.getVersion(), 0);
        assertTrue("barney".equals(c.getName()));
        assertTrue("rubble".equals(c.getValue()));

        cookieHeader = "fred=flintstone;barney=rubble";
        cookies = HttpHeaderReader.readCookies(cookieHeader);
        assertEquals(cookies.size(), 2);
        c = cookies.get("fred");
        assertEquals(c.getVersion(), 0);
        assertTrue("fred".equals(c.getName()));
        assertTrue("flintstone".equals(c.getValue()));
        c = cookies.get("barney");
        assertEquals(c.getVersion(), 0);
        assertTrue("barney".equals(c.getName()));
        assertTrue("rubble".equals(c.getValue()));

        cookieHeader = "$Version=1;fred=flintstone;$Path=/path;barney=rubble";
        cookies = HttpHeaderReader.readCookies(cookieHeader);
        assertEquals(cookies.size(), 2);
        c = cookies.get("fred");
        assertEquals(c.getVersion(), 1);
        assertTrue("fred".equals(c.getName()));
        assertTrue("flintstone".equals(c.getValue()));
        assertTrue("/path".equals(c.getPath()));
        c = cookies.get("barney");
        assertEquals(c.getVersion(), 1);
        assertTrue("barney".equals(c.getName()));
        assertTrue("rubble".equals(c.getValue()));

        cookieHeader = "$Version=1;fred=flintstone;$Path=/path,barney=rubble;$Domain=.sun.com";
        cookies = HttpHeaderReader.readCookies(cookieHeader);
        assertEquals(cookies.size(), 2);
        c = cookies.get("fred");
        assertEquals(c.getVersion(), 1);
        assertTrue("fred".equals(c.getName()));
        assertTrue("flintstone".equals(c.getValue()));
        assertTrue("/path".equals(c.getPath()));
        c = cookies.get("barney");
        assertEquals(c.getVersion(), 1);
        assertTrue("barney".equals(c.getName()));
        assertTrue("rubble".equals(c.getValue()));
        assertTrue(".sun.com".equals(c.getDomain()));

        cookieHeader = "$Version=1; fred = flintstone ; $Path=/path, barney=rubble ;$Domain=.sun.com";
        cookies = HttpHeaderReader.readCookies(cookieHeader);
        assertEquals(cookies.size(), 2);
        c = cookies.get("fred");
        assertEquals(c.getVersion(), 1);
        assertTrue("fred".equals(c.getName()));
        assertTrue("flintstone".equals(c.getValue()));
        assertTrue("/path".equals(c.getPath()));
        c = cookies.get("barney");
        assertEquals(c.getVersion(), 1);
        assertTrue("barney".equals(c.getName()));
        assertTrue("rubble".equals(c.getValue()));
        assertTrue(".sun.com".equals(c.getDomain()));
    }

    @Test
    public void testNewCookieToString() {
        NewCookie cookie = new NewCookie("fred", "flintstone");
        String expResult = "fred=flintstone;Version=1";
        assertEquals(expResult, cookie.toString());

        cookie = new NewCookie("fred", "flintstone", null, null, null, 60, false);
        expResult = "fred=flintstone;Version=1;Max-Age=60";
        assertEquals(expResult, cookie.toString());

        cookie = new NewCookie("fred", "flintstone", null, null, "a modern stonage family", 60, false);
        expResult = "fred=flintstone;Version=1;Comment=\"a modern stonage family\";Max-Age=60";
        assertEquals(expResult, cookie.toString());
    }

    @Test
    public void testNewCookieValueOf() {
        NewCookie cookie = NewCookie.valueOf("fred=flintstone;Version=2");
        assertEquals("fred", cookie.getName());
        assertEquals("flintstone", cookie.getValue());
        assertEquals(2, cookie.getVersion());

        cookie = NewCookie.valueOf("fred=flintstone;Version=1;Max-Age=60");
        assertEquals("fred", cookie.getName());
        assertEquals("flintstone", cookie.getValue());
        assertEquals(1, cookie.getVersion());
        assertEquals(60, cookie.getMaxAge());

        cookie = NewCookie.valueOf("fred=flintstone;Version=1;Comment=\"a modern stonage family\";Max-Age=60;Secure");
        assertEquals("fred", cookie.getName());
        assertEquals("flintstone", cookie.getValue());
        assertEquals("a modern stonage family", cookie.getComment());
        assertEquals(1, cookie.getVersion());
        assertEquals(60, cookie.getMaxAge());
        assertTrue(cookie.isSecure());
    }

}
