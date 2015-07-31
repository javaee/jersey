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

package org.glassfish.jersey.examples.feedcombiner.model;

import java.util.Objects;

import javax.ws.rs.FormParam;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public final class FeedRequestBean {

    @NotEmpty
    @FormParam("urls")
    private String urls;

    @FormParam("refreshPeriod")
    private long refreshPeriod;

    @NotEmpty
    @FormParam("title")
    private String title;

    @NotEmpty
    @FormParam("description")
    private String description;

    public String getUrls() {
        return urls;
    }

    public void setUrls(final String urls) {
        this.urls = urls;
    }

    public long getRefreshPeriod() {
        return refreshPeriod;
    }

    public void setRefreshPeriod(final long refreshPeriod) {
        this.refreshPeriod = refreshPeriod;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FeedRequestBean)) {
            return false;
        }
        final FeedRequestBean that = (FeedRequestBean) o;
        return Objects.equals(refreshPeriod, that.refreshPeriod)
                && Objects.equals(urls, that.urls)
                && Objects.equals(title, that.title)
                && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(urls, refreshPeriod, title, description);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FeedRequestBean{");
        sb.append("urls='").append(urls).append('\'');
        sb.append(", refreshPeriod=").append(refreshPeriod);
        sb.append(", title='").append(title).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
