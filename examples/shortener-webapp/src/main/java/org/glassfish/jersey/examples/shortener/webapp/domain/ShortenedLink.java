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

package org.glassfish.jersey.examples.shortener.webapp.domain;

import java.net.URI;

import javax.validation.constraints.NotNull;

import org.glassfish.jersey.examples.shortener.webapp.validation.ValidShortenedLink;

/**
 * Representation of shortened link.
 *
 * @author Michal Gajdos
 */
@ValidShortenedLink
public class ShortenedLink {

    @NotNull
    private URI original;
    @NotNull
    private URI shortened;
    @NotNull
    private URI permanent;

    public ShortenedLink() {
    }

    public ShortenedLink(final URI original, final URI permanent, final URI shortened) {
        this.original = original;
        this.shortened = shortened;
        this.permanent = permanent;
    }

    public URI getOriginal() {
        return original;
    }

    public void setOriginal(final URI original) {
        this.original = original;
    }

    public URI getShortened() {
        return shortened;
    }

    public void setShortened(final URI shortened) {
        this.shortened = shortened;
    }

    public URI getPermanent() {
        return permanent;
    }

    public void setPermanent(final URI permanent) {
        this.permanent = permanent;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ShortenedLink that = (ShortenedLink) o;

        if (original != null ? !original.equals(that.original) : that.original != null) {
            return false;
        }
        if (permanent != null ? !permanent.equals(that.permanent) : that.permanent != null) {
            return false;
        }
        if (shortened != null ? !shortened.equals(that.shortened) : that.shortened != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = original != null ? original.hashCode() : 0;
        result = 31 * result + (shortened != null ? shortened.hashCode() : 0);
        result = 31 * result + (permanent != null ? permanent.hashCode() : 0);
        return result;
    }
}
