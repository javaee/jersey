/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.examples.shortener.webapp.service;

import java.net.URI;
import java.util.concurrent.ConcurrentMap;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.examples.shortener.webapp.domain.ShortenedLink;
import org.glassfish.jersey.internal.util.collection.DataStructures;

/**
 * Service responsible for shortening links and storing shortened links in memory.
 *
 * @author Michal Gajdos
 */
public final class ShortenerService {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
    private static final int BASE = ALPHABET.length();

    private static final ConcurrentMap<String, ShortenedLink> store = DataStructures.createConcurrentMap();

    public static ShortenedLink shortenLink(final URI baseUri, final String link) {
        // Remove this condition if you want to create different shortened links for the same URI.
        if (store.containsKey(encode(link))) {
            return store.get(encode(link));
        }

        String path = link;
        ShortenedLink shortenedLink;

        do {
            path = encode(path);
            shortenedLink = new ShortenedLink(URI.create(link),
                    UriBuilder.fromUri(baseUri).path("i").path(path).build(),
                    UriBuilder.fromUri(baseUri).path("r").path(path).build());
        } while (store.putIfAbsent(path, shortenedLink) != null);

        return store.get(path);
    }

    public static ShortenedLink getLink(final String link) {
        return store.get(link);
    }

    public static boolean containsLink(final ShortenedLink link) {
        return store.containsValue(link);
    }

    /**
     * Implementation details described at http://stackoverflow.com/a/742047/290799.
     */
    private static String encode(final String link) {
        final StringBuilder builder = new StringBuilder();

        // NOTE: Math.abs(Integer.MIN_VALUE) = Integer.MIN_VALUE (negative value)
        int hash = Math.abs(link.hashCode());
        while (hash > 0 || hash == Integer.MIN_VALUE) {
            builder.append(ALPHABET.charAt(Math.abs(hash % BASE)));
            hash = Math.abs(hash / BASE);
        }

        return builder.reverse().toString();
    }
}
