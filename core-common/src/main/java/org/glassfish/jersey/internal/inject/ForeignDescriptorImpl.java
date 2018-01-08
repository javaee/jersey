/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.internal.inject;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * The descriptor holder for an externally provided DI providers. Using this interface DI provider is able to provider his own
 * descriptor which can be used and returned to the DI provider in further processing.
 * <p>
 * This is useful in the case of caching where an algorithm is able to store and subsequently provide for an injection the already
 * resolved descriptor of the same value.
 */
public class ForeignDescriptorImpl implements ForeignDescriptor {

    private static final Consumer<Object> NOOP_DISPOSE_INSTANCE = instance -> {};

    private final Object foreignDescriptor;
    private final Consumer<Object> disposeInstance;

    /**
     * Constructor accepts a descriptor of the DI provider and to be able to provide it in further processing.
     *
     * @param foreignDescriptor DI provider's descriptor.
     */
    public ForeignDescriptorImpl(Object foreignDescriptor) {
        this(foreignDescriptor, NOOP_DISPOSE_INSTANCE);
    }

    /**
     * Constructor accepts a descriptor of the DI provider and to be able to provide it in further processing along with
     * dispose mechanism to destroy the objects corresponding the given {@code foreign key}.
     *
     * @param foreignDescriptor DI provider's descriptor.
     */
    public ForeignDescriptorImpl(Object foreignDescriptor, Consumer<Object> disposeInstance) {
        this.foreignDescriptor = foreignDescriptor;
        this.disposeInstance = disposeInstance;
    }

    @Override
    public Object get() {
        return foreignDescriptor;
    }

    @Override
    public void dispose(Object instance) {
        disposeInstance.accept(instance);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ForeignDescriptorImpl)) {
            return false;
        }
        final ForeignDescriptorImpl that = (ForeignDescriptorImpl) o;
        return foreignDescriptor.equals(that.foreignDescriptor);
    }

    @Override
    public int hashCode() {
        return foreignDescriptor.hashCode();
    }
}
