/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.media.sse;

import org.glassfish.jersey.server.Broadcaster;

/**
 * Used for broadcasting SSE to multiple {@link EventOutput} instances.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Martin Matula
 */
public class SseBroadcaster extends Broadcaster<OutboundEvent> {

    /**
     * Creates a new instance.
     * If this constructor is called by a subclass, it assumes the the reason for the subclass to exist is to implement
     * {@link #onClose(org.glassfish.jersey.server.ChunkedOutput)} and {@link #onException(org.glassfish.jersey.server.ChunkedOutput, Exception)} methods, so it adds
     * the newly created instance as the listener. To avoid this, subclasses may call {@link #SseBroadcaster(Class)}
     * passing their class as an argument.
     */
    public SseBroadcaster() {
        this(SseBroadcaster.class);
    }

    /**
     * Can be used by subclasses to override the default functionality of adding self to the set of
     * {@link org.glassfish.jersey.server.BroadcasterListener listeners}.
     * If creating a direct instance of a subclass passed in the parameter,
     * the broadcaster will not register itself as a listener.
     *
     * @param subclass subclass of SseBroadcaster that should not be registered as a listener - if creating a direct instance
     *                 of this subclass, this constructor will not register the new instance as a listener.
     * @see #SseBroadcaster()
     */
    protected SseBroadcaster(final Class<? extends SseBroadcaster> subclass) {
        super(subclass);
    }
}
