/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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
 * This package defines a high-level (proxy-based) client API.
 * The API enables utilization of the server-side JAX-RS annotations
 * to describe the server-side resources and dynamically generate client-side
 * proxy objects for them.
 * <p>
 * Consider a server which exposes a resource at http://localhost:8080. The resource
 * can be described by the following interface:
 * </p>
 *
 * <pre>
 * &#064;Path("myresource")
 * public interface MyResourceIfc {
 *     &#064;GET
 *     &#064;Produces("text/plain")
 *     String get();
 *
 *     &#064;POST
 *     &#064;Consumes("application/xml")
 *     &#064;Produces("application/xml")
 *     MyBean postEcho(MyBean bean);
 *
 *     &#064;Path("{id}")
 *     &#064;GET
 *     &#064;Produces("text/plain")
 *     String getById(&#064;PathParam("id") String id);
 * }
 * </pre>
 *
 * <p>
 * You can use <a href="WebResourceFactory.html">WebResourceFactory</a> class defined
 * in this package to access the server-side resource using this interface.
 * Here is an example:
 * </p>
 *
 * <pre>
 * Client client = ClientBuilder.newClient();
 * WebTarget target = client.target("http://localhost:8080/");
 * MyResourceIfc resource = WebResourceFactory.newResource(MyResourceIfc.class, target);
 *
 * String responseFromGet = resource.get();
 * MyBean responseFromPost = resource.postEcho(myBeanInstance);
 * String responseFromGetById = resource.getById("abc");
 * </pre>
 */
package org.glassfish.jersey.client.proxy;
