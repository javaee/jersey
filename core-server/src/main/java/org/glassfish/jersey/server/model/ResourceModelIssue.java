/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.model;

/**
 * Resource model validity issue.
 *
 * Covers various model issues, such as duplicate URI templates, duplicate
 * HTTP method annotations, etc.
 * <p />
 * The model issues can be either fatal or non-fatal (see {@link #isFatal()}).
 * While the non-fatal issues are merely reported as warnings in the log, the
 * fatal issues prevent the successful application deployment.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class ResourceModelIssue {

    private final Object source;
    private final String message;
    private final boolean fatal;

    /**
     * Create a new {@link #isFatal() non-fatal} resource model issue.
     *
     * @param source issue source.
     * @param message human-readable issue description.
     */
    public ResourceModelIssue(Object source, String message) {
        this(source, message, false);
    }

    /**
     * Create a new resource model issue.
     *
     * @param source issue source.
     * @param message human-readable issue description.
     * @param isFatal {@code true} if the issue is {@link #isFatal() fatal},
     *     {@code false} otherwise.
     */
    public ResourceModelIssue(Object source, String message, boolean isFatal) {
        this.source = source;
        this.message = message;
        this.fatal = isFatal;
    }

    /**
     * Human-readable description of the issue.
     *
     * @return message describing the issue.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Check if the issue is fatal.
     *
     * Fatal issues typically prevent the deployment of the application to succeed.
     *
     * @return {@code true} if the issue is fatal, {@code false} otherwise.
     */
    public boolean isFatal() {
        return fatal;
    }

    /**
     * The issue source.
     *
     * Identifies the object where the issue was found.
     *
     * @return source of the issue.
     */
    public Object getSource() {
        return source;
    }

    @Override
    public String toString() {
        return (fatal) ? "[FATAL] " : "[NON-FATAL] " +
                message +
                "; source=" + source + '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ResourceModelIssue other = (ResourceModelIssue) obj;
        if (this.source != other.source && (this.source == null || !this.source.equals(other.source))) {
            return false;
        }
        if ((this.message == null) ? (other.message != null) : !this.message.equals(other.message)) {
            return false;
        }
        return this.fatal == other.fatal;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + (this.source != null ? this.source.hashCode() : 0);
        hash = 79 * hash + (this.message != null ? this.message.hashCode() : 0);
        hash = 79 * hash + (this.fatal ? 1 : 0);
        return hash;
    }
}
