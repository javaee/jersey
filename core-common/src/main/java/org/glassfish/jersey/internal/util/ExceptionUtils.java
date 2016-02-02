/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved.
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Exception Utils class provide utility method for exception handling.
 *
 * @author Stepan Vavra (stepan.vavra@oracle.com)
 */
public final class ExceptionUtils {

    private ExceptionUtils() {
    }

    /**
     * Gets the stack trace of the provided throwable as a string.
     *
     * @param t the exception to get the stack trace for.
     * @return the stack trace as a string.
     */
    public static String exceptionStackTraceAsString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * Based on the rethrow parameter, either rethrows the supplied exception or logs the provided message at the given level.
     *
     * @param e       the exception to rethrow if rethrow is {@code true}.
     * @param rethrow whether to rethrow an exception or just log the provided message.
     * @param logger  the logger to print the message with.
     * @param m       the message to log if rethrow is {@code false}.
     * @param level   the level of the logged message.
     * @param <T>     the type of the exception to be conditionally rethrown.
     * @throws T if rethrow is {@code true}.
     */
    public static <T extends Exception> void conditionallyReThrow(T e, boolean rethrow, Logger logger, String m, Level level)
            throws T {
        if (rethrow) {
            throw e;
        } else {
            // do not mask the other exception, just log this one
            logger.log(level, m, e);
        }
    }
}
