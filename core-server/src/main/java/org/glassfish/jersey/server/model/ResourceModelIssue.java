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
package org.glassfish.jersey.server.model;

import org.glassfish.jersey.Severity;

/**
 * Resource model validity issue.
 * <p/>
 * Covers various model issues, such as duplicate URI templates, duplicate
 * HTTP method annotations, etc.
 * <p/>
 * The model issues can be either fatal warnings or hings (see {@link #getSeverity()}).
 * While the non-fatal issues are merely reported as warnings in the log, the
 * fatal issues prevent the successful application deployment.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class ResourceModelIssue {

    private final Object source;
    private final String message;
    private final Severity severity;

    /**
     * Create a new resource model warning.
     *
     * @param source  issue source.
     * @param message human-readable issue description.
     */
    public ResourceModelIssue(final Object source, final String message) {
        this(source, message, Severity.WARNING);
    }

    /**
     * Create a new resource model issue.
     *
     * @param source   issue source.
     * @param message  human-readable issue description.
     * @param severity indicates severity of added error.
     */
    public ResourceModelIssue(final Object source, final String message, final Severity severity) {
        this.source = source;
        this.message = message;
        this.severity = severity;
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
     * Get {@link org.glassfish.jersey.Severity}.
     *
     * @return severity of current {@link ResourceModelIssue}.
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * The issue source.
     * <p/>
     * Identifies the object where the issue was found.
     *
     * @return source of the issue.
     */
    public Object getSource() {
        return source;
    }

    @Override
    public String toString() {
        return "[" + severity + "] " + message + "; source='" + source + '\'';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ResourceModelIssue that = (ResourceModelIssue) o;

        if (message != null ? !message.equals(that.message) : that.message != null) {
            return false;
        }
        if (severity != that.severity) {
            return false;
        }
        if (source != null ? !source.equals(that.source) : that.source != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = source != null ? source.hashCode() : 0;
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (severity != null ? severity.hashCode() : 0);
        return result;
    }
}
