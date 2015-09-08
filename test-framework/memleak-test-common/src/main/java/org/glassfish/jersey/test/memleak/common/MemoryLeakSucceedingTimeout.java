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
package org.glassfish.jersey.test.memleak.common;

import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
public class MemoryLeakSucceedingTimeout extends Timeout {

    private static final int DEFAULT_TIMEOUT_MILLIS = 300_000;
    private int millis;

    public MemoryLeakSucceedingTimeout() {
        this(DEFAULT_TIMEOUT_MILLIS);
    }

    public MemoryLeakSucceedingTimeout(final int defaultMillisTimeout) {
        super(defaultMillisTimeout);

        this.millis = Integer.getInteger(MemoryLeakUtils.JERSEY_CONFIG_TEST_MEMLEAK_TIMEOUT, defaultMillisTimeout);
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new FailOnTimeout(base, millis) {
            @Override
            public void evaluate() throws Throwable {
                try {
                    super.evaluate();
                } catch (Throwable throwable) {
                    if (throwable.getMessage().startsWith("test timed out after")) {
                        MemoryLeakUtils.verifyNoOutOfMemoryOccurred();
                        System.out.println("Test timed out after " + millis + " ms. Successfully ending.");
                    } else {
                        throw throwable;
                    }
                }
            }
        };
    }
}
