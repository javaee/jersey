/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.samples.linking.representation;

import org.glassfish.jersey.samples.linking.resources.ItemResource;
import org.glassfish.jersey.linking.Binding;
import org.glassfish.jersey.linking.InjectLinks;
import org.glassfish.jersey.linking.InjectLink;
import org.glassfish.jersey.linking.InjectLink.Style;
import java.net.URI;
import java.util.List;
import javax.ws.rs.core.Link;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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
            condition = "${resource.next}",
            bindings = @Binding(name = "id", value = "${resource.nextId}"),
            rel = "next"
    ),
    @InjectLink(
            resource = ItemResource.class,
            style = Style.ABSOLUTE,
            condition = "${resource.prev}",
            bindings = @Binding(name = "id", value = "${resource.prevId}"),
            rel = "prev"
    )
})
public class ItemRepresentation {

    @XmlElement
    private String name;

    @InjectLink(
            resource = ItemResource.class,
            style = Style.ABSOLUTE,
            bindings = @Binding(name = "id", value = "${resource.id}"),
            rel = "self"
    )
    @XmlJavaTypeAdapter(Link.JaxbAdapter.class)
    @XmlElement(name="link")
    Link self;

    @InjectLinks({
        @InjectLink(
                resource = ItemResource.class,
                style = Style.ABSOLUTE,
                condition = "${resource.next}",
                bindings = @Binding(name = "id", value = "${resource.nextId}"),
                rel = "next"
        ),
        @InjectLink(
                resource = ItemResource.class,
                style = Style.ABSOLUTE,
                condition = "${resource.prev}",
                bindings = @Binding(name = "id", value = "${resource.prevId}"),
                rel = "prev"
        )})
    @XmlElement(name="link")
    @XmlElementWrapper(name = "links")
    @XmlJavaTypeAdapter(Link.JaxbAdapter.class)
    List<Link> links;

    public ItemRepresentation() {
        this.name = "";
    }

    public ItemRepresentation(String name) {
        this.name = name;
    }

}
