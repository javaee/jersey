/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.jdk.connector.internal;

import org.glassfish.jersey.client.ClientProperties;

/**
 * This Exception is used only if {@link ClientProperties#FOLLOW_REDIRECTS} is set to {@code true}.
 * <p/>
 * This exception is thrown when any of the Redirect HTTP response status codes (301, 302, 303, 307, 308) is received and:
 * <ul>
 * <li>
 * the chained redirection count exceeds the value of
 * {@link org.glassfish.jersey.client.JdkConnectorProvider#MAX_REDIRECTS}
 * </li>
 * <li>
 * or an infinite redirection loop is detected
 * </li>
 * <li>
 * or Location response header is missing, empty or does not contain a valid {@link java.net.URI}.
 * </li>
 * </ul>
 *
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 * @see RedirectHandler
 */
public class RedirectException extends Exception {

    private static final long serialVersionUID = 4357724300486801294L;

    /**
     * Constructor.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public RedirectException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public RedirectException(String message, Throwable t) {
        super(message, t);
    }
}
