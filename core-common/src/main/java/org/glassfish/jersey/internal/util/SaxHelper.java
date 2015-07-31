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
package org.glassfish.jersey.internal.util;

import java.security.AccessController;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;

/**
 * Common helper methods for SAX parsers.
 *
 * @author Michal Gajdos
 */
public final class SaxHelper {

    private SaxHelper() {
    }

    /**
     * Determines whether the given SAX {@code parserFactory} is from XDK package or not.
     *
     * @param parserFactory SAX parser factory to evaluate.
     * @return {@code true} if the given SAX parser factory is an XDK factory, {@code false} otherwise.
     */
    public static boolean isXdkParserFactory(final SAXParserFactory parserFactory) {
        return isXdkFactory(parserFactory, "oracle.xml.jaxp.JXSAXParserFactory");
    }

    /**
     * Determines whether the given {@code builderFactory} is from XDK package or not.
     *
     * @param builderFactory document builder factory to evaluate.
     * @return {@code true} if the given document builder factory is an XDK factory, {@code false} otherwise.
     */
    public static boolean isXdkDocumentBuilderFactory(final DocumentBuilderFactory builderFactory) {
        return isXdkFactory(builderFactory, "oracle.xml.jaxp.JXDocumentBuilderFactory");
    }

    private static boolean isXdkFactory(final Object factory, final String className) {
        final Class<?> xdkFactoryClass = AccessController.doPrivileged(ReflectionHelper.classForNamePA(className, null));
        if (xdkFactoryClass == null) {
            return false;
        }
        return xdkFactoryClass.isAssignableFrom(factory.getClass());
    }
}
