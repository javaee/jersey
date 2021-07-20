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

package org.glassfish.jersey.servlet.init;

import org.glassfish.jersey.servlet.spi.FilterUrlMappingsProvider;

import javax.servlet.FilterConfig;
import javax.servlet.FilterRegistration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Provide all configured context paths (url mappings) of the application deployed using filter.
 * <p>
 * The url patterns are returned without the eventual trailing asterisk.
 * <p>
 * The functionality is available in Servlet 3.x environment only, so this
 * implementation of {@link FilterUrlMappingsProvider} interface is Servlet 3 specific.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class FilterUrlMappingsProviderImpl implements FilterUrlMappingsProvider {
    @Override
    public List<String> getFilterUrlMappings(FilterConfig filterConfig) {
        FilterRegistration filterRegistration =
          filterConfig.getServletContext().getFilterRegistration(filterConfig.getFilterName());

        Collection<String> urlPatternMappings = filterRegistration.getUrlPatternMappings();
        List<String> result = new ArrayList<>();

        for (String pattern : urlPatternMappings) {
            result.add(pattern.endsWith("*") ? pattern.substring(0, pattern.length() - 1) : pattern);
        }

        return result;
    }
}
