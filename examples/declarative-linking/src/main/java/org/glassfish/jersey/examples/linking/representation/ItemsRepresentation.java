/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.linking.representation;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Link;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.glassfish.jersey.examples.linking.model.ItemsModel;
import org.glassfish.jersey.examples.linking.resources.ItemsResource;
import org.glassfish.jersey.linking.Binding;
import org.glassfish.jersey.linking.InjectLink;
import org.glassfish.jersey.linking.InjectLink.Style;
import org.glassfish.jersey.linking.InjectLinks;

/**
 * JAXB representation of a sublist of items
 *
 * @author Mark Hadley
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "items")
@InjectLinks({
        @InjectLink(
                resource = ItemsResource.class,
                style = Style.ABSOLUTE,
                method = "query",
                condition = "${instance.offset + instance.limit < instance.modelLimit}",
                bindings = {
                        @Binding(name = "offset", value = "${instance.offset + instance.limit}"),
                        @Binding(name = "limit", value = "${instance.limit}")
                },
                rel = "next"
        ),
        @InjectLink(
                resource = ItemsResource.class,
                style = Style.ABSOLUTE,
                method = "query",
                condition = "${instance.offset - instance.limit >= 0}",
                bindings = {
                        @Binding(name = "offset", value = "${instance.offset - instance.limit}"),
                        @Binding(name = "limit", value = "${instance.limit}")
                },
                rel = "prev"
        )})

public class ItemsRepresentation {

    @XmlElement(name = "items")
    private List<ItemRepresentation> items;

    @XmlTransient
    private int offset, limit;

    @XmlTransient
    private ItemsModel itemsModel;

    @InjectLink(
            resource = ItemsResource.class,
            method = "query",
            style = Style.ABSOLUTE,
            bindings = {@Binding(name = "offset", value = "${instance.offset}"),
                    @Binding(name = "limit", value = "${instance.limit}")
            },
            rel = "self"
    )
    @XmlJavaTypeAdapter(Link.JaxbAdapter.class)
    @XmlElement(name = "link")
    Link self;

    @InjectLinks({
            @InjectLink(
                    resource = ItemsResource.class,
                    style = Style.ABSOLUTE,
                    method = "query",
                    condition = "${instance.offset + instance.limit < instance.modelLimit}",
                    bindings = {
                            @Binding(name = "offset", value = "${instance.offset + instance.limit}"),
                            @Binding(name = "limit", value = "${instance.limit}")
                    },
                    rel = "next"
            ),
            @InjectLink(
                    resource = ItemsResource.class,
                    style = Style.ABSOLUTE,
                    method = "query",
                    condition = "${instance.offset - instance.limit >= 0}",
                    bindings = {
                            @Binding(name = "offset", value = "${instance.offset - instance.limit}"),
                            @Binding(name = "limit", value = "${instance.limit}")
                    },
                    rel = "prev"
            )})
    @XmlElement(name = "link")
    @XmlElementWrapper(name = "links")
    @XmlJavaTypeAdapter(Link.JaxbAdapter.class)
    List<Link> links;

    public ItemsRepresentation() {
        offset = 0;
        limit = 10;
    }

    public ItemsRepresentation(ItemsModel itemsModel, int offset, int limit) {

        this.offset = offset;
        this.limit = limit;
        this.itemsModel = itemsModel;

        items = new ArrayList<>();
        for (int i = offset; i < (offset + limit) && i < itemsModel.getSize(); i++) {
            items.add(new ItemRepresentation(
                    itemsModel,
                    Integer.toString(i),
                    itemsModel.getItem(Integer.toString(i)).getName()));
        }

    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    public int getModelLimit() {
        return itemsModel.getSize();
    }
}
