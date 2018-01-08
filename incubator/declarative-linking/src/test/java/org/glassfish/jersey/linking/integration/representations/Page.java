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

package org.glassfish.jersey.linking.integration.representations;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Link;

import org.glassfish.jersey.linking.InjectLinks;

public class Page {

    /**
     * The number of the current page. Is always non-negative and less that {@code Page#getTotalPages()}.
     */
    private int number;

    /**
     * The size of the page.
     */
    private int size;

    /**
     * The number of total pages.
     */
    private int totalPages;

    /**
     * The number of elements currently on this page.
     */
    private int numberOfElements;

    /**
     * The total amount of elements.
     */
    private long totalElements;

    /**
     * If there is a previous page.
     */
    private boolean isPreviousPageAvailable;

    /**
     * Whether the current page is the first one.
     */
    private boolean isFirstPage;

    /**
     * If there is a next page.
     */
    private boolean isNextPageAvailable;

    /**
     * Whether the current page is the last one.
     */
    private boolean isLastPage;

    @InjectLinks
    private List<Link> links = new ArrayList<>();

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getNumberOfElements() {
        return numberOfElements;
    }

    public void setNumberOfElements(int numberOfElements) {
        this.numberOfElements = numberOfElements;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public boolean isPreviousPageAvailable() {
        return isPreviousPageAvailable;
    }

    public void setPreviousPageAvailable(boolean previousPageAvailable) {
        isPreviousPageAvailable = previousPageAvailable;
    }

    public boolean isFirstPage() {
        return isFirstPage;
    }

    public void setFirstPage(boolean firstPage) {
        isFirstPage = firstPage;
    }

    public boolean isNextPageAvailable() {
        return isNextPageAvailable;
    }

    public void setNextPageAvailable(boolean nextPageAvailable) {
        isNextPageAvailable = nextPageAvailable;
    }

    public boolean isLastPage() {
        return isLastPage;
    }

    public void setLastPage(boolean lastPage) {
        isLastPage = lastPage;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }
}
