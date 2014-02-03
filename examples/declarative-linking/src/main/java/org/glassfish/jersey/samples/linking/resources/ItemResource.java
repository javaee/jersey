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

package org.glassfish.jersey.samples.linking.resources;

import org.glassfish.jersey.samples.linking.model.ItemModel;
import org.glassfish.jersey.samples.linking.model.ItemsModel;
import org.glassfish.jersey.samples.linking.representation.ItemRepresentation;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Resource that provides access to one item from a set of items managed
 * by ItemsModel
 * @author mh124079
 */
@Path("{id}")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class ItemResource {

    private ItemsModel itemsModel;
    private ItemModel itemModel;
    private String id;

    public ItemResource(@PathParam("id") String id) {
        this.id = id;
        itemsModel = ItemsModel.getInstance();
        try {
            itemModel = itemsModel.getItem(id);
        } catch (IndexOutOfBoundsException ex) {
            throw new NotFoundException();
        }
    }

    @GET
    public ItemRepresentation get() {
        return new ItemRepresentation(itemModel.getName());
    }

    /**
     * Determines whether there is a next item.
     * @return
     */
    public boolean isNext() {
        return itemsModel.hasNext(id);
    }

    /**
     * Determines whether there is a previous item
     * @return
     */
    public boolean isPrev() {
        return itemsModel.hasPrev(id);
    }

    public String getNextId() {
        return itemsModel.getNextId(id);
    }

    public String getPrevId() {
        return itemsModel.getPrevId(id);
    }

    public String getId() {
        return id;
    }
}
