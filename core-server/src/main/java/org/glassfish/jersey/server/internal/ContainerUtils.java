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
package org.glassfish.jersey.server.internal;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

/**
 * Utility methods used by container implementations.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class ContainerUtils {

    /**
     * Encodes (predefined subset of) unsafe/unwise URI characters with the percent-encoding.
     * <p/>
     * <p>Replaces the predefined set of unsafe URI characters in the query string with its percent-encoded counterparts. The
     * reserved characters (as defined by the RFC) are automatically encoded by browsers, but some characters are in the "gray
     * zone" - are not explicitly forbidden, but not recommended and known to cause issues.</p>
     * <p/>
     * <p>Currently, the method only encodes the curly brackets ({@code \{} and {@code \}}),
     * if any, to allow URI request parameters to contain JSON.</p>
     *
     * @param originalQueryString URI query string (the part behind the question mark character).
     * @return the same string with unsafe characters (currently only curly brackets) eventually percent encoded.
     */
    public static String encodeUnsafeCharacters(String originalQueryString) {
        if (originalQueryString == null) {
            return null;
        }
        if (originalQueryString.contains("{") || originalQueryString.contains("}")) {
            return originalQueryString.replace("{", "%7B").replace("}", "%7D");
        }
        return originalQueryString;
    }
}
