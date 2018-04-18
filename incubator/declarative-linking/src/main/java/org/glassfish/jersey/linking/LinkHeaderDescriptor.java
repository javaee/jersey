/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.linking;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.glassfish.jersey.linking.InjectLink.LinkQueryParam;
import org.glassfish.jersey.linking.InjectLink.Style;
import org.glassfish.jersey.linking.mapping.ResourceMappingContext;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Utility class for working with {@link org.glassfish.jersey.linking.InjectLink} annotations.
 *
 * @author Mark Hadley
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
class LinkHeaderDescriptor implements InjectLinkDescriptor {

    private InjectLink linkHeader;
    private Map<String, String> bindings;
    private MultivaluedMap<String, String> queryParams;
    private boolean copyFromRequestQueryParams = false;
    private Set<String> excludeFromRequestQueryParams = null;


    LinkHeaderDescriptor(InjectLink linkHeader) {
        this.linkHeader = linkHeader;
        bindings = new HashMap<>();
        for (Binding binding : linkHeader.bindings()) {
            bindings.put(binding.name(), binding.value());
        }
        queryParams = new MultivaluedHashMap<>();
        for (LinkQueryParam param : linkHeader.queryParams()) {
          queryParams.add(param.name(), param.value());
        }
        copyFromRequestQueryParams = linkHeader.copyRequestQueryParams();
        if (copyFromRequestQueryParams) {
          excludeFromRequestQueryParams =
              Collections.unmodifiableSet(
                  new HashSet<>(Arrays.asList(linkHeader.excludeFromRequestQueryParams())));
        }
    }

    InjectLink getLinkHeader() {
        return linkHeader;
    }

    public String getLinkTemplate(ResourceMappingContext rmc) {
        return InjectLinkFieldDescriptor.getLinkTemplate(rmc, linkHeader);
    }

    public Style getLinkStyle() {
        return linkHeader.style();
    }

    public String getBinding(String name) {
        return bindings.get(name);
    }

    public String getCondition() {
        return linkHeader.condition();
    }

    @Override
    public MultivaluedMap<String, String> getQueryParams() {
      return queryParams;
    }

    @Override
    public boolean copyFromRequestParams() {
      return copyFromRequestQueryParams;
    }

    @Override
    public Set<String> excludeFromRequestParams() {
      if (!copyFromRequestQueryParams) {
        return null;
      }
      return excludeFromRequestQueryParams;
    }

}
