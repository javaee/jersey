/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.validation;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Default validation error entity to be included in {@code Response}.
 *
 * @author Michal Gajdos
 */
@XmlRootElement
@SuppressWarnings("UnusedDeclaration")
public final class ValidationError {

    private String message;

    private String messageTemplate;

    private String path;

    private String invalidValue;

    /**
     * Create a {@code ValidationError} instance. Constructor for JAXB providers.
     */
    public ValidationError() {
    }

    /**
     * Create a {@code ValidationError} instance.
     *
     * @param message interpolated error message.
     * @param messageTemplate non-interpolated error message.
     * @param path property path.
     * @param invalidValue value that failed to pass constraints.
     */
    public ValidationError(final String message, final String messageTemplate, final String path, final String invalidValue) {
        this.message = message;
        this.messageTemplate = messageTemplate;
        this.path = path;
        this.invalidValue = invalidValue;
    }

    /**
     * Return the interpolated error message for this validation error.
     *
     * @return the interpolated error message for this validation error.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Return the interpolated error message for this validation error.
     *
     * @param message the interpolated error message for this validation error.
     */
    public void setMessage(final String message) {
        this.message = message;
    }

    /**
     * Return the string representation of the property path to the value.
     *
     * @return the string representation of the property path to the value.
     */
    public String getPath() {
        return path;
    }

    /**
     * Set the string representation of the property path to the value.
     *
     * @param path the string representation of the property path to the value.
     */
    public void setPath(final String path) {
        this.path = path;
    }

    /**
     * Returns the string representation of the value failing to pass the constraint.
     *
     * @return the value failing to pass the constraint.
     */
    public String getInvalidValue() {
        return invalidValue;
    }

    /**
     * Set the value failing to pass the constraint.
     *
     * @param invalidValue the value failing to pass the constraint.
     */
    public void setInvalidValue(final String invalidValue) {
        this.invalidValue = invalidValue;
    }

    /**
     * Return the non-interpolated error message for this validation error.
     *
     * @return the non-interpolated error message for this validation error.
     */
    public String getMessageTemplate() {
        return messageTemplate;
    }

    /**
     * Set the non-interpolated error message for this validation error.
     *
     * @param messageTemplate the non-interpolated error message for this validation error.
     */
    public void setMessageTemplate(final String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }
}
