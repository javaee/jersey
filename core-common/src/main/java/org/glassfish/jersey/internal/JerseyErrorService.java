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
package org.glassfish.jersey.internal;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.inject.Singleton;

import org.glassfish.hk2.api.ErrorInformation;
import org.glassfish.hk2.api.ErrorService;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Jersey implementation of HK2 Error Service to provide improved reporting
 * of HK2 issues, that may be otherwise hidden (ignored).
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class JerseyErrorService implements ErrorService {

    /**
     * HK2 Binder for the Jersey implementation of HK2 {@link ErrorService} contract.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(JerseyErrorService.class).to(ErrorService.class).in(Singleton.class);
        }
    }

    @Override
    public void onFailure(final ErrorInformation error) throws MultiException {
        final String msg;

        switch (error.getErrorType()) {
            case FAILURE_TO_REIFY:
                msg = LocalizationMessages.HK_2_REIFICATION_ERROR(
                        error.getDescriptor().getImplementation(), printStackTrace(error.getAssociatedException()));
                break;
            default:
                msg = LocalizationMessages.HK_2_UNKNOWN_ERROR(printStackTrace(error.getAssociatedException()));
                break;
        }

        try {
            Errors.warning(error.getInjectee(), msg);
        } catch (IllegalStateException ex) {
            Errors.process(new Runnable() {
                @Override
                public void run() {
                    Errors.warning(this, LocalizationMessages.HK_2_FAILURE_OUTSIDE_ERROR_SCOPE());
                    Errors.warning(error.getInjectee(), msg);
                }
            });
        }
    }

    private String printStackTrace(Throwable t) {
        final StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
