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
package org.glassfish.jersey.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Properties delegate backed by a {@code Map}.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class MapPropertiesDelegate implements PropertiesDelegate {

    private final Map<String, Object> store;

    /**
     * Create new map-based properties delegate.
     */
    public MapPropertiesDelegate() {
        this.store = new HashMap<String, Object>();
    }

    /**
     * Create new map-based properties delegate.
     *
     * @param store backing property store.
     */
    public MapPropertiesDelegate(Map<String, Object> store) {
        this.store = store;
    }

    /**
     * Initialize new map-based properties delegate from another
     * delegate.
     *
     * @param that original properties delegate.
     */
    public MapPropertiesDelegate(PropertiesDelegate that) {
        if (that instanceof MapPropertiesDelegate) {
            this.store = new HashMap<String, Object>(((MapPropertiesDelegate) that).store);
        } else {
            this.store = new HashMap<String, Object>();
            for (String name : that.getPropertyNames()) {
                this.store.put(name, that.getProperty(name));
            }
        }
    }

    @Override
    public Object getProperty(String name) {
        return store.get(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return Collections.unmodifiableCollection(store.keySet());
    }

    @Override
    public void setProperty(String name, Object value) {
        store.put(name, value);
    }

    @Override
    public void removeProperty(String name) {
        store.remove(name);
    }
}
