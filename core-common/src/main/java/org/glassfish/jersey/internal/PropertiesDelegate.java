/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.internal;

import java.util.Collection;

/**
 * TODO: javadoc.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public interface PropertiesDelegate {
    /**
     * Returns the property with the given name registered in the current request/response
     * exchange context, or {@code null} if there is no property by that name.
     * <p>
     * A property allows a JAX-RS filters and interceptors to exchange
     * additional custom information not already provided by this interface.
     * </p>
     * <p>
     * A list of supported properties can be retrieved using {@link #getPropertyNames()}.
     * Custom property names should follow the same convention as package names.
     * </p>
     *
     * @param name a {@code String} specifying the name of the property.
     * @return an {@code Object} containing the value of the property, or
     *         {@code null} if no property exists matching the given name.
     * @see #getPropertyNames()
     */
    public Object getProperty(String name);


    /**
     * Returns an immutable {@link java.util.Collection collection} containing the property
     * names available within the context of the current request/response exchange context.
     * <p>
     * Use the {@link #getProperty} method with a property name to get the value of
     * a property.
     * </p>
     *
     * @return an immutable {@link java.util.Collection collection} of property names.
     * @see #getProperty
     */
    public Collection<String> getPropertyNames();


    /**
     * Binds an object to a given property name in the current request/response
     * exchange context. If the name specified is already used for a property,
     * this method will replace the value of the property with the new value.
     * <p>
     * A property allows a JAX-RS filters and interceptors to exchange
     * additional custom information not already provided by this interface.
     * </p>
     * <p>
     * A list of supported properties can be retrieved using {@link #getPropertyNames()}.
     * Custom property names should follow the same convention as package names.
     * </p>
     * <p>
     * If a {@code null} value is passed, the effect is the same as calling the
     * {@link #removeProperty(String)} method.
     * </p>
     *
     * @param name   a {@code String} specifying the name of the property.
     * @param object an {@code Object} representing the property to be bound.
     */
    public void setProperty(String name, Object object);

    /**
     * Removes a property with the given name from the current request/response
     * exchange context. After removal, subsequent calls to {@link #getProperty}
     * to retrieve the property value will return {@code null}.
     *
     * @param name a {@code String} specifying the name of the property to be removed.
     */
    public void removeProperty(String name);
}
