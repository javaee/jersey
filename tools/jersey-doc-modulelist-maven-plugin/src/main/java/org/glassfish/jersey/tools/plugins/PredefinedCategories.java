/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tools.plugins;

/**
 * Jersey module categories definition.
 * The categories are based on maven groupId, this enum assigns to each "known" groupId a human-readable caption
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public enum PredefinedCategories {
    CORE("org.glassfish.jersey.core", "Jersey Core"),
    CONTAINERS("org.glassfish.jersey.containers", "Jersey Containers"),
    CONNECTORS("org.glassfish.jersey.connectors", "Jersey Connectors"),
    MEDIA("org.glassfish.jersey.media", "Jersey Media"),
    EXTENSIONS("org.glassfish.jersey.ext", "Jersey Extensions"),
    TEST_FRAMEWORK("org.glassfish.jersey.test-framework", "Jersey Test Framework"),
    TEST_FRAMEWORK_PROVIDERS("org.glassfish.jersey.test-framework.providers", "Jersey Test Framework Providers"),
    GLASSFISH_BUNDLES("org.glassfish.jersey.containers.glassfish", "Jersey Glassfish Bundles"),
    SECURITY("org.glassfish.jersey.security", "Security"),
    EXAMPLES("org.glassfish.jersey.examples", "Jersey Examples");

    private String groupId;
    private String caption;

    private PredefinedCategories(String groupId, String caption) {
        this.groupId = groupId;
        this.caption = caption;
    }

    public String getCaption() {
        return this.caption;
    }

    public String getGroupId() {
        return this.groupId;
    }
}
