/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Injects a {@link javax.ws.rs.client.WebTarget resource target} pointing at
 * a resource identified by the resolved URI into a method parameter,
 * class field or a bean property.
 * <p/>
 * Injected variable must be of type {@link javax.ws.rs.client.WebTarget}.
 *
 * @author Marek Potociar
 * @see javax.ws.rs.client.WebTarget
 * @since 2.0
 */
@java.lang.annotation.Target({PARAMETER, FIELD, METHOD})
@Retention(RUNTIME)
@Documented
public @interface Uri {

    /**
     * Specifies the URI of the injected {@link javax.ws.rs.client.WebTarget
     * resource target}.
     *
     * The value must be in the form of absolute URI if not used from inside of
     * a JAX-RS component class. For example:
     * <pre>
     * public class AuditingFilter implements RequestFilter {
     *    &#64;Uri("users/{name}/orders")
     *    WebTarget userOrders;
     *
     *    // An external resource target
     *    &#64;Uri("http://mail.acme.com/accounts/{name}")
     *    WebTarget userEmailAccount;
     *
     *    // An external, template-based resource target
     *    &#64;Uri("http://{audit-host}:{audit-port}/auditlogs/")
     *    WebTarget auditLogs;
     *    ...
     * }
     * </pre>
     *
     * If used from within a JAX-RS component class (e.g. resource, filter, provider&nbsp;&hellip;&nbsp;),
     * the value can take a form of absolute or relative URI.
     * A relative URI is resolved using the context path of the application as the base URI.
     * For example:
     * <pre>
     * public class AuditingFilter implements RequestFilter {
     *    &#64;Uri("audit/logs")
     *    WebTarget applicationLogs;
     *
     *    &#64;Uri("http://sales.acme.com/audit/logs")
     *    WebTarget domainLogs;
     *
     *    ...
     * }
     * </pre>
     *
     * In case the annotation is used from a JAX-RS resource class, an absolute
     * or relative URI template value may be provided. The template parameter (e.g. {@code {id}})
     * values are automatically resolved in the context of the enclosing resource class
     * {@link javax.ws.rs.Path path template} as well as the context of the processed request.
     * Other defined template parameters have to be resolved before invocation of managed web target.
     * For example:
     * <pre>
     * &#64;Path("users/{name}")
     * public class MyResource {
     *    &#64;Uri("users/{name}/orders")
     *    WebTarget userOrders;
     *
     *    &#64;Uri("http://mail.acme.com/accounts/{name}")
     *    WebTarget userEmailAccount;
     *
     *    ...
     * }
     * </pre>
     *
     * @see javax.ws.rs.client.WebTarget
     * @see javax.ws.rs.Path
     */
    String value();
}
