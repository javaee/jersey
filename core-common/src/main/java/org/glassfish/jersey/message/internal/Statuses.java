/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

/**
 * Factory for producing custom JAX-RS {@link StatusType response status type}
 * instances.
 *
 * @author Paul Sandoz
 */
public final class Statuses {

    private static final class StatusImpl implements StatusType {

        private final int code;
        private final String reason;
        private final Family family;

        private StatusImpl(int code, String reason) {
            this.code = code;
            this.reason = reason;
            this.family = Family.familyOf(code);
        }

        @Override
        public int getStatusCode() {
            return code;
        }

        @Override
        public String getReasonPhrase() {
            return reason;
        }

        @Override
        public String toString() {
            return reason;
        }

        @Override
        public Family getFamily() {
            return family;
        }

        @Override
        @SuppressWarnings("RedundantIfStatement")
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof StatusType)) {
                return false;
            }

            final StatusType status = (StatusType) o;

            if (code != status.getStatusCode()) {
                return false;
            }
            if (family != status.getFamily()) {
                return false;
            }
            if (reason != null ? !reason.equals(status.getReasonPhrase()) : status.getReasonPhrase() != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = code;
            result = 31 * result + (reason != null ? reason.hashCode() : 0);
            result = 31 * result + family.hashCode();
            return result;
        }
    }

    /**
     * Create a new status type instance.
     * <p>
     * For standard status codes listed in {@link javax.ws.rs.core.Response.Status} enum, the default reason phrase
     * is used. For any other status code an empty string is used as a reason phrase.
     * </p>
     *
     * @param code response status code.
     * @return new status type instance representing a given response status code.
     */
    public static StatusType from(int code) {
        StatusType result = Response.Status.fromStatusCode(code);
        return (result != null) ? result : new StatusImpl(code, "");
    }

    /**
     * Create a new status type instance with a custom reason phrase.
     *
     * @param code   response status code.
     * @param reason custom response status reason phrase.
     * @return new status type instance representing a given response status code and custom reason phrase.
     */
    public static StatusType from(int code, String reason) {
        return new StatusImpl(code, reason);
    }

    /**
     * Create a new status type instance with a custom reason phrase.
     *
     * @param status response status type.
     * @param reason custom response status reason phrase.
     * @return new status type instance representing a given response status code and custom reason phrase.
     */
    public static StatusType from(StatusType status, String reason) {
        return new StatusImpl(status.getStatusCode(), reason);
    }

    /**
     * Prevents instantiation.
     */
    private Statuses() {
        throw new AssertionError("Instantiation not allowed.");
    }
}
