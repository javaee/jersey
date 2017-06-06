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

package org.glassfish.jersey.jdk.connector;

import java.net.CookieManager;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
class HttpConnectionPool {

    // TODO better solution, containers won't like this
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    private final ConnectorConfiguration connectorConfiguration;
    private final CookieManager cookieManager;
    private final Map<DestinationConnectionPool.DestinationKey, DestinationConnectionPool> destinationPools = new
            ConcurrentHashMap<>();

    HttpConnectionPool(ConnectorConfiguration connectorConfiguration, CookieManager cookieManager) {
        this.connectorConfiguration = connectorConfiguration;
        this.cookieManager = cookieManager;
    }

    void send(HttpRequest httpRequest, CompletionHandler<HttpResponse> completionHandler) {
        final DestinationConnectionPool.DestinationKey destinationKey = new DestinationConnectionPool.DestinationKey(
                httpRequest.getUri());
        DestinationConnectionPool destinationConnectionPool = destinationPools.get(destinationKey);

        if (destinationConnectionPool == null) {
            synchronized (this) {
                // check again while holding the lock
                destinationConnectionPool = destinationPools.get(destinationKey);

                if (destinationConnectionPool == null) {
                    final DestinationConnectionPool pool = new DestinationConnectionPool(connectorConfiguration, cookieManager,
                            scheduler);
                    pool.setConnectionCloseListener(() -> {
                        /* There is a potential race when there is a request just about to be submitted to the pool
                        we are just removing. Such request will be executed on the removed pool without any problems.
                        The only issue is that this listener will be called for the second time in such a case, so we
                        have to make sure we don't remove a new pool that might have been created in the meantime. */
                        destinationPools.remove(destinationKey, pool);
                    });

                    destinationConnectionPool = pool;
                    destinationPools.put(destinationKey, destinationConnectionPool);
                }
            }
        }

        destinationConnectionPool.send(httpRequest, completionHandler);
    }

    synchronized void close() {
        destinationPools.values().forEach(DestinationConnectionPool::close);
    }
}
