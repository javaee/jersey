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

package org.glassfish.jersey.server.oauth1;

import java.security.Principal;

/**
 * Class representing a registered consumer.
 *
 * @author Martin Matula
 */
public interface OAuth1Consumer {
    /** Returns consumer key.
     *
     * @return consumer key
     */
    String getKey();

    /** Returns consumer secret.
     *
     * @return consumer secret
     */
    String getSecret();

    /** Returns a {@link java.security.Principal} object representing this consumer.
     * When the oauth filter verifies the request
     * and no access token is provided, this is the principal that will get set to the security context.
     * This can be used for 2-legged oauth. If the server does not allow consumers acting
     * on their own (with no access token), this method should return null.
     *
     * @return Principal corresponding to this consumer, or null if 2-legged oauth not supported (i.e. consumers can't act on their own)
     */
    Principal getPrincipal();

    /** Returns a boolean indicating whether this consumer is authorized for the
     * specified logical "role". When the oauth filter verifies the request
     * and no access token is provided (2-legged oauth), it sets the consumer object to the security context
     * which then delegates {@link javax.ws.rs.core.SecurityContext#isUserInRole(String)} to this
     * method.
     *
     * @param role a {@code String} specifying the name of the role
     *
     * @return a {@code boolean} indicating whether this token is authorized for
     * a given role
     */
    boolean isInRole(String role);
}
