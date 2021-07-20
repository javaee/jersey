/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.tests.integration.jersey2812;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * This servlet filter class provides a means to detect whether Jersey (in servlet filter based setup) properly freed the
 * server-side thread processing the http request to an async RESTful resource where {@link javax.ws.rs.container.AsyncResponse}
 * wasn't resumed.
 * <p/>
 * Reported as JERSEY-2812.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
public class TestFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(TestFilter.class.getName());
    public static final String CDL_FINISHED = "CDL-finished";

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(final ServletRequest servletRequest,
                         final ServletResponse servletResponse,
                         final FilterChain filterChain)
            throws IOException, ServletException {
        LOGGER.finest(new Date() + " Thread " + Thread.currentThread().getName() + " is being acquired...");

        final CountDownLatch cdlFinished = new CountDownLatch(1);
        servletRequest.setAttribute(CDL_FINISHED, cdlFinished);

        filterChain.doFilter(servletRequest, servletResponse);

        // the thread did return from Jersey
        cdlFinished.countDown();

        LOGGER.finest(new Date() + " Thread " + Thread.currentThread().getName() + " is being released.");
    }

    @Override
    public void destroy() {

    }
}
