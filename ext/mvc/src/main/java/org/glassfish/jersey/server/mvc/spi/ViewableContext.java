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

package org.glassfish.jersey.server.mvc.spi;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.spi.Contract;

/**
 * The context for resolving an instance of {@link org.glassfish.jersey.server.mvc.Viewable} to an instance of {@link
 * ResolvedViewable}.
 * <p/>
 * Note:
 * {@link ViewableContext#resolveViewable(org.glassfish.jersey.server.mvc.Viewable, javax.ws.rs.core.MediaType, Class, TemplateProcessor)}
 * method may be called multiple times (combination of all the calculated possible media types of the response with all found
 * {@link TemplateProcessor template processors}).
 *
 * @author Paul Sandoz
 * @author Michal Gajdos
 */
@Contract
@ConstrainedTo(RuntimeType.SERVER)
public interface ViewableContext {

    /**
     * Resolve given {@link org.glassfish.jersey.server.mvc.Viewable viewable} using {@link javax.ws.rs.core.MediaType mediaType},
     * {@code resourceClass} and {@link TemplateProcessor templateProcessor}.
     * <p/>
     * If the template name of the viewable is not absolute then the given {@code resourceClass} may be utilized to resolve
     * the relative template name into an absolute template name.
     * <br/>
     * <ul>
     * {@code resourceClass} contains class of the matched resource.
     * </ul>
     *
     * @param viewable viewable to be resolved.
     * @param mediaType media type the viewable may be transformed into.
     * @param resourceClass resource class.
     * @param templateProcessor template processor to be used.
     * @return resolved viewable or {@code null} if the viewable cannot be resolved.
     */
    public ResolvedViewable resolveViewable(final Viewable viewable, final MediaType mediaType, final Class<?> resourceClass,
                                            final TemplateProcessor templateProcessor);
}
