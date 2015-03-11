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

package org.glassfish.jersey.linking;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.glassfish.jersey.Beta;

/**
 * Specifies the binding between a URI template parameter and a bean property.
 * @see org.glassfish.jersey.linking.InjectLink#bindings()
 *
 * @author Mark Hadley
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Beta
public @interface Binding {

    /**
     * Specifies the name of the URI template parameter, defaults to
     * "value" for convenience.
     */
    String name() default "value";

    /**
     * Specifies the value of a URI template parameter. The value is an EL
     * expression using immediate evaluation syntax. E.g.:
     * <pre>${instance.widgetId}</pre>
     * In the above example the value is taken from the <code>widgetId</code>
     * property of the implicit <code>instance</code> bean.
     * <p>Three implicit beans are supported:</p>
     * <dl>
     * <dt><code>instance</code></dt><dd>The object whose class contains the
     * {@link org.glassfish.jersey.linking.InjectLink} annotation.</dd>
     * <dt><code>entity</code></dt><dd>The entity returned by the resource
     * class method. This is either the resource method return value
     * or the entity property for a resource method that returns Response.</dd>
     * <dt><code>resource</code></dt><dd>The resource class instance that
     * returned the object that contains the {@code InjectLink} annotation.</dd>
     * </dd>
     * </dl>
     */
    String value();
}
