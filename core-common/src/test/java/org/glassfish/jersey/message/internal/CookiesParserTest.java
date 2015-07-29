/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.message.internal;

import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.ws.rs.core.NewCookie;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Michal Gajdos
 */
public class CookiesParserTest {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

    @Test
    public void testCaseInsensitiveNewCookieParams() throws Exception {
        _testCaseInsensitiveNewCookieParams("expires", "max-age", "path", "domain", "comment", "version", "secure", "httponly");
        _testCaseInsensitiveNewCookieParams("Expires", "Max-Age", "Path", "Domain", "Comment", "Version", "Secure", "HttpOnly");
        _testCaseInsensitiveNewCookieParams("exPires", "max-aGe", "patH", "doMAin", "Comment", "vErsion", "secuRe", "httPonly");
    }

    private void _testCaseInsensitiveNewCookieParams(final String expires, final String maxAge, final String path,
                                                     final String domain, final String comment, final String version,
                                                     final String secure, final String httpOnly) throws Exception {

        final String header = "foo=bar;"
                + expires + "=Tue, 15 Jan 2013 21:47:38 GMT;"
                + maxAge + "=42;"
                + path + "=/;"
                + domain + "=.example.com;"
                + comment + "=Testing;"
                + version + "=1;"
                + secure + ";"
                + httpOnly;

        final NewCookie cookie = CookiesParser.parseNewCookie(header);

        assertThat(cookie.getName(), equalTo("foo"));
        assertThat(cookie.getValue(), equalTo("bar"));

        assertThat(cookie.getExpiry(), equalTo(dateFormat.parse("Tue, 15 Jan 2013 21:47:38 GMT")));
        assertThat(cookie.getMaxAge(), equalTo(42));
        assertThat(cookie.getPath(), equalTo("/"));
        assertThat(cookie.getDomain(), equalTo(".example.com"));
        assertThat(cookie.getComment(), equalTo("Testing"));
        assertThat(cookie.getVersion(), equalTo(1));
        assertThat(cookie.isSecure(), is(true));
        assertThat(cookie.isHttpOnly(), is(true));
    }
}
