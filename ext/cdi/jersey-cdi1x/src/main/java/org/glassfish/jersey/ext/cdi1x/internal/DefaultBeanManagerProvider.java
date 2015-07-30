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

package org.glassfish.jersey.ext.cdi1x.internal;

import java.util.logging.Logger;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.glassfish.jersey.ext.cdi1x.internal.spi.BeanManagerProvider;

/**
 * Default implementation of {@link org.glassfish.jersey.ext.cdi1x.internal.spi.BeanManagerProvider} that works on most environments.
 * At first the implementation tries to lookup the bean manager in JNDI, then via CDI 1.1 API. If not found {@code null} is
 * returned.
 *
 * @author Michal Gajdos
 * @since 2.17
 */
final class DefaultBeanManagerProvider implements BeanManagerProvider {

    private static final Logger LOGGER = Logger.getLogger(DefaultBeanManagerProvider.class.getName());

    @Override
    public BeanManager getBeanManager() {
        InitialContext initialContext = null;
        try {
            initialContext = new InitialContext();
            return (BeanManager) initialContext.lookup("java:comp/BeanManager");
        } catch (final Exception ex) {
            try {
                return CDI.current().getBeanManager();
            } catch (final Exception e) {
                LOGGER.config(LocalizationMessages.CDI_BEAN_MANAGER_JNDI_LOOKUP_FAILED());
                return null;
            }
        } finally {
            if (initialContext != null) {
                try {
                    initialContext.close();
                } catch (final NamingException ignored) {
                    // no-op
                }
            }
        }
    }
}
