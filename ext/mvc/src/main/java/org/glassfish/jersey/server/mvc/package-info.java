/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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

/**
 * Provides support for Model, View and Controller (MVC).
 * <p/>
 * Given the MVC pattern the Controller corresponds to a resource class, the View to a template referenced by a template name,
 * and the Model to a Java object (or a Java bean).
 * <p/>
 * A resource method of a resource class may return an instance of {@link org.glassfish.jersey.server.mvc.Viewable} that
 * encapsulates the template name and the model. In this respect the instance of{@link org.glassfish.jersey.server.mvc.Viewable}
 * is the response entity. Such a viewable response entity may be set in contexts other than a resource method but for the
 * purposes of this section the focus is on resource methods.
 * <p/>
 * The {@link org.glassfish.jersey.server.mvc.Viewable}, returned by a resource method,
 * will be processed such that the template name is resolved to a template reference that identifies a template capable of
 * being processed by an appropriate view processor.
 * <br/>
 * The view processor then processes template given the model to produce a response entity that is returned to the client.
 * <p/>
 * For example, the template name could reference a Java Server Page (JSP) and the model will be accessible to that JSP. The
 * JSP view processor will process the JSP resulting in an HTML document that is returned as the response entity. (See later
 * for more details.)
 * <p/>
 * Two forms of returning {@link org.glassfish.jersey.server.mvc.Viewable} instances are supported: explicit; and implicit.
 */
package org.glassfish.jersey.server.mvc;
