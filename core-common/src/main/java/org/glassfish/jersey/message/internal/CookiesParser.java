/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.text.ParseException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;

import org.glassfish.jersey.internal.LocalizationMessages;

/**
 * Cookies parser.
 *
 * @author Marc Hadley
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
/* package */ class CookiesParser {

    private static final Logger LOGGER = Logger.getLogger(CookiesParser.class.getName());

    private static class MutableCookie {

        String name;
        String value;
        int version = Cookie.DEFAULT_VERSION;
        String path = null;
        String domain = null;

        public MutableCookie(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public Cookie getImmutableCookie() {
            return new Cookie(name, value, path, domain, version);
        }
    }

    public static Map<String, Cookie> parseCookies(String header) {
        String bites[] = header.split("[;,]");
        Map<String, Cookie> cookies = new LinkedHashMap<String, Cookie>();
        int version = 0;
        MutableCookie cookie = null;
        for (String bite : bites) {
            String crumbs[] = bite.split("=", 2);
            String name = crumbs.length > 0 ? crumbs[0].trim() : "";
            String value = crumbs.length > 1 ? crumbs[1].trim() : "";
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
                value = value.substring(1, value.length() - 1);
            }
            if (!name.startsWith("$")) {
                if (cookie != null) {
                    cookies.put(cookie.name, cookie.getImmutableCookie());
                }

                cookie = new MutableCookie(name, value);
                cookie.version = version;
            } else if (name.startsWith("$Version")) {
                version = Integer.parseInt(value);
            } else if (name.startsWith("$Path") && cookie != null) {
                cookie.path = value;
            } else if (name.startsWith("$Domain") && cookie != null) {
                cookie.domain = value;
            }
        }
        if (cookie != null) {
            cookies.put(cookie.name, cookie.getImmutableCookie());
        }
        return cookies;
    }

    public static Cookie parseCookie(String header) {
        Map<String, Cookie> cookies = parseCookies(header);
        return cookies.entrySet().iterator().next().getValue();
    }

    private static class MutableNewCookie {

        String name = null;
        String value = null;
        String path = null;
        String domain = null;
        int version = Cookie.DEFAULT_VERSION;
        String comment = null;
        int maxAge = NewCookie.DEFAULT_MAX_AGE;
        boolean secure = false;
        boolean httpOnly = false;
        Date expiry = null;

        public MutableNewCookie(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public NewCookie getImmutableNewCookie() {
            return new NewCookie(name, value, path, domain, version, comment, maxAge, expiry, secure, httpOnly);
        }
    }

    public static NewCookie parseNewCookie(String header) {
        String bites[] = header.split("[;,]");

        MutableNewCookie cookie = null;
        for (int i = 0; i < bites.length; i++) {
            String crumbs[] = bites[i].split("=", 2);
            String name = crumbs.length > 0 ? crumbs[0].trim() : "";
            String value = crumbs.length > 1 ? crumbs[1].trim() : "";

            if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
                value = value.substring(1, value.length() - 1);
            }

            if (cookie == null) {
                cookie = new MutableNewCookie(name, value);
            } else {
                final String param = name.toLowerCase();

                if (param.startsWith("comment")) {
                    cookie.comment = value;
                } else if (param.startsWith("domain")) {
                    cookie.domain = value;
                } else if (param.startsWith("max-age")) {
                    cookie.maxAge = Integer.parseInt(value);
                } else if (param.startsWith("path")) {
                    cookie.path = value;
                } else if (param.startsWith("secure")) {
                    cookie.secure = true;
                } else if (param.startsWith("version")) {
                    cookie.version = Integer.parseInt(value);
                } else if (param.startsWith("domain")) {
                    cookie.domain = value;
                } else if (param.startsWith("httponly")) {
                    cookie.httpOnly = true;
                }  else if (param.startsWith("expires")) {
                    try {
                        cookie.expiry = HttpDateFormat.readDate(value + ", " + bites[++i]);
                    } catch (ParseException e) {
                        LOGGER.log(Level.FINE, LocalizationMessages.ERROR_NEWCOOKIE_EXPIRES(value), e);
                    }
                }
            }
        }

        return cookie.getImmutableNewCookie();
    }

    /**
     * Prevents instantiation.
     */
    private CookiesParser() {
    }
}