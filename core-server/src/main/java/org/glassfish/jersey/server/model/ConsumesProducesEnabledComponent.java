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

import java.util.List;

import javax.ws.rs.core.MediaType;

import javax.naming.OperationNotSupportedException;

/**
 * Jersey model component that may contain {@link javax.ws.rs.Consumes consumes}
 * and {@link javax.ws.rs.Produces produces} information.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public interface ConsumesProducesEnabledComponent {

    /**
     * Provides information on whether consumed media types were explicitly
     * configured for this component.
     *
     * @return true if consumed media types were configured explicitly
     */
    public boolean areInputTypesDeclared();

    /**
     * Provides information on whether produced media types were explicitly
     * configured for this component.
     *
     * @return true if produced media types were configured explicitly
     */
    public boolean areOutputTypesDeclared();

    /**
     * Getter for consumed media types.
     * Returned list could be mutable if given component supports updating.
     *
     * @see #areInputTypesDeclared()
     *
     * @return list of consumed media types
     */
    public List<MediaType> getSupportedInputTypes();

    /**
     * Getter for produced media types.
     * Returned list could be mutable if given component supports updating.
     *
     * @see #areOutputTypesDeclared()
     *
     * @return list of produced media types
     */
    public List<MediaType> getSupportedOutputTypes();

    /**
     * Setter for information on how the consumed media types were configured.
     *
     * @param declared should be set to true if consumed media types were configured
     *     explicitly.
     * @throws OperationNotSupportedException if the component is immutable.
     */
    public void setAreInputTypesDeclared(boolean declared) throws OperationNotSupportedException;

    /**
     * Setter for information on how the produced media types were configured.
     *
     * @param declared should be set to true if produced media types were configured
     *     explicitly.
     * @throws OperationNotSupportedException if the component is immutable.
     */
    public void setAreOutputTypesDeclared(boolean declared) throws OperationNotSupportedException;
}
