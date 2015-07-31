/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.message.filtering.spi;

import java.util.Map;
import java.util.Set;

/**
 * Read-only graph containing representations of an entity class that should be processed in entity-filtering. The
 * representations are twofolds: simple (primitive/non-filterable) fields and further-filterable fields (represented by
 * subgraphs).
 * <p/>
 * Object graph instances are created for entity-filtering scopes that are obtained from entity annotations, configuration or
 * scopes defined on resource methods / classes (on server).
 *
 * @author Michal Gajdos
 * @see ObjectGraphTransformer
 * @see ScopeResolver
 */
public interface ObjectGraph {

    /**
     * Get entity domain class of this graph.
     *
     * @return entity domain class.
     */
    public Class<?> getEntityClass();

    /**
     * Get a set of all simple (non-filterable) fields of entity class. Value of each of these fields is either primitive or
     * the entity-filtering feature cannot be applied to this field. Values of these fields can be directly processed.
     *
     * @return non-filterable fields.
     */
    public Set<String> getFields();

    /**
     * Get fields with the given parent path. The parent path, which may exist in the requested filtering scopes, is
     * used for context to match against the field at the subgraph level.
     *
     * @param parent name of parent field.
     * @return non-filterable fields.
     */
    public Set<String> getFields(String parent);

    /**
     * Get a map of all further-filterable fields of entity class. Mappings are represented as:
     * <pre>
     * &lt;field&gt; -&gt; &lt;object-graph&gt;</pre>
     * It is supposed that object graphs contained in this map would be processed further.
     *
     * @return further-filterable map of fields.
     */
    public Map<String, ObjectGraph> getSubgraphs();

    /**
     * Get subgraphs with the given parent path. The parent path, which may exist in the requested filtering scopes, is
     * used for context to match against the subgraph level.
     *
     * @param parent name of parent field.
     * @return further-filterable map of fields.
     *
     */
    public Map<String, ObjectGraph> getSubgraphs(String parent);
}
