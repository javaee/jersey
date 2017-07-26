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
import org.glassfish.jersey.examples.linking.resources.ItemResource;
import org.glassfish.jersey.linking.Binding;
import org.glassfish.jersey.linking.InjectLink;
import org.glassfish.jersey.linking.InjectLink.Style;
import org.glassfish.jersey.linking.InjectLinks;

/**
 * JAXB representation of an item
 *
 *
 * @author Mark Hadley
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "item")
@InjectLinks({
        @InjectLink(
                resource = ItemResource.class,
                style = Style.ABSOLUTE,
                condition = "${instance.next}",
                bindings = @Binding(name = "id", value = "${instance.nextId}"),
                rel = "next"
        ),
        @InjectLink(
                resource = ItemResource.class,
                style = Style.ABSOLUTE,
                condition = "${instance.prev}",
                bindings = @Binding(name = "id", value = "${instance.prevId}"),
                rel = "prev"
        )
})
public class ItemRepresentation {

    @XmlElement
    private String name;

    @XmlTransient
    private String id;
    @XmlTransient
    private ItemsModel itemsModel;

    @InjectLink(
            resource = ItemResource.class,
            style = Style.ABSOLUTE,
            bindings = @Binding(name = "id", value = "${instance.id}"),
            rel = "self"
    )
    @XmlJavaTypeAdapter(Link.JaxbAdapter.class)
    @XmlElement(name = "link")
    Link self;

    @InjectLinks({
            @InjectLink(
                    resource = ItemResource.class,
                    style = Style.ABSOLUTE,
                    condition = "${instance.next}",
                    bindings = @Binding(name = "id", value = "${instance.nextId}"),
                    rel = "next"
            ),
            @InjectLink(
                    resource = ItemResource.class,
                    style = Style.ABSOLUTE,
                    condition = "${instance.prev}",
                    bindings = @Binding(name = "id", value = "${instance.prevId}"),
                    rel = "prev"
            )})
    @XmlElement(name = "link")
    @XmlElementWrapper(name = "links")
    @XmlJavaTypeAdapter(Link.JaxbAdapter.class)
    List<Link> links;

    public ItemRepresentation() {

    }

    public ItemRepresentation(ItemsModel itemsModel, String id, String name) {
        this.itemsModel = itemsModel;
        this.name = name;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public boolean isNext() {
        return itemsModel.hasNext(id);
    }

    public boolean isPrev() {
        return itemsModel.hasPrev(id);
    }

    public String getNextId() {
        return itemsModel.getNextId(id);
    }

    public String getPrevId() {
        return itemsModel.getPrevId(id);
    }

}
