/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.oauth1.signature;

import org.glassfish.jersey.spi.Contract;

/**
 * An interface representing the OAuth signature method.
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 * @author Paul C. Bryan <pbryan@sun.com>
 */
@Contract
public interface OAuth1SignatureMethod {

    /**
     * Returns the name of this signature method, as negotiated through the
     * OAuth protocol.
     *
     * @return Signature method name.
     */
     public String name();

    /**
     * Signs the data using the supplied secret(s).
     *
     * @param baseString a {@link String} that contains the request baseString to be signed.
     * @param secrets the secret(s) to use to sign the data.
     * @return a {@link String} that contains the signature.
     * @throws InvalidSecretException if a supplied secret is not valid.
     */
    public String sign(String baseString, OAuth1Secrets secrets) throws InvalidSecretException;

    /**
     * Verifies the signature for the data using the supplied secret(s).
     *
     * @param elements a {@link String} that contains the request elements to be verified.
     * @param secrets the secret(s) to use to verify the signature.
     * @param signature a {@link String} that contains the signature to be verified.
     * @return true if the signature matches the secrets and data.
     * @throws InvalidSecretException if a supplied secret is not valid.
     */
    public boolean verify(String elements, OAuth1Secrets secrets, String signature) throws InvalidSecretException;
}
