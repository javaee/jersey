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
package org.glassfish.jersey.server;

/**
 * Jersey server-side configuration properties.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class ServerProperties {

    /**
     * Application configuration initialization property whose value is a fully
     * qualified class name of a class that implements {@link javax.ws.rs.core.Application}.
     * <p />
     * A default value is not set.
     * <p />
     * The name of the configuration property is <code>{@value}</code>.
     */
    // TODO implement generic support
    public static final String JAXRS_APPLICATION_CLASS = "javax.ws.rs.Application";

    /**
     * Defines one or more packages that contain application-specific resources and
     * providers. If the property is set, the specified packages will be scanned for
     * JAX-RS root resources (annotated with {@link javax.ws.rs.Path @Path}) and
     * providers (annotated with {@link javax.ws.rs.ext.Provider @Provider}).
     * <p />
     * The property value MUST be an instance of {@link String} or {@code String[]}
     * array. Each {@code String} instance represents one or more package names
     * that MUST be separated only by characters declared in {@link #COMMON_DELIMITERS}.
     * <p />
     * A default value is not set.
     * <p />
     * The name of the configuration property is <code>{@value}</code>.
     */
    // TODO add support for ':' and any (number of consecutive) whitespace(s).
    // TODO implement generic support
    public static final String PROVIDER_PACKAGES = "jersey.config.server.provider.packages";

    /**
     * Defines class-path that contains application-specific resources and
     * providers. If the property is set, the specified class-path will be scanned
     * for JAX-RS root resources (annotated with {@link javax.ws.rs.Path @Path})
     * and providers (annotated with {@link javax.ws.rs.ext.Provider @Provider}).
     * Each path element MUST be an absolute or relative directory, or a Jar file.
     * The contents of a directory, including Java class files, jars files
     * and sub-directories are scanned (recursively).
     * <p />
     * The property value MUST be an instance of {@link String} or {@code String[]}
     * array. Each {@code String} instance represents one or more paths
     * that MUST be separated only by characters declared in {@link #COMMON_DELIMITERS}.
     * <p />
     * A default value is not set.
     * <p />
     * The name of the configuration property is <code>{@value}</code>.
     */
    // TODO add support for ':' and any (number of consecutive) whitespace(s).
    // TODO implement generic support
    public static final String PROVIDER_CLASSPATH = "jersey.config.server.provider.classpath";

    /**
     * Defines one or more class names that implement application-specific resources
     * and providers. If the property is set, the specified classes will be instantiated
     * and registered as either application JAX-RS root resources (annotated with
     * {@link javax.ws.rs.Path @Path}) or providers (annotated with
     * {@link javax.ws.rs.ext.Provider @Provider}).
     * <p />
     * The property value MUST be an instance of {@link String} or {@code String[]}
     * array. Each {@code String} instance represents one or more class names
     * that MUST be separated only by characters declared in {@link #COMMON_DELIMITERS}.
     * <p />
     * A default value is not set.
     * <p />
     * The name of the configuration property is <code>{@value}</code>.
     */
    // TODO implement generic support
    public static final String PROVIDER_CLASSNAMES = "jersey.config.server.provider.classnames";

    /**
     * Common delimiters used by various properties.
     */
    public static final String COMMON_DELIMITERS = " ,;\n";

    private ServerProperties() {
        // prevents instantiation
    }
}
