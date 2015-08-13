/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.spi.internal;

import org.glassfish.jersey.server.model.Parameter;

import org.glassfish.hk2.api.Factory;

/**
 * Extends {@link Factory} interface with
 * {@link org.glassfish.jersey.server.model.Parameter.Source} information.
 *
 * @param <T> This must be the type of entity for which this is a factory.
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public final class ParamValueFactoryWithSource<T> implements Factory<T> {

    private final Factory<T> factory;
    private final Parameter.Source parameterSource;

    /**
     * Wrap provided param factory.
     *
     * @param factory         param factory to be wrapped.
     * @param parameterSource param source.
     */
    public ParamValueFactoryWithSource(Factory<T> factory, Parameter.Source parameterSource) {
        this.factory = factory;
        this.parameterSource = parameterSource;
    }

    @Override
    public T provide() {
        return factory.provide();
    }

    @Override
    public void dispose(T t) {
        factory.dispose(t);
    }

    /**
     * Returns {@link org.glassfish.jersey.server.model.Parameter.Source}
     * which closely determines a function of the given factory.
     *
     * @return Source which a given parameter belongs to.
     **/
    public Parameter.Source getSource() {
        return parameterSource;
    }

}
