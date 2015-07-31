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

package org.glassfish.jersey.examples.feedcombiner.resources;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.inject.Inject;
import javax.validation.Valid;

import org.glassfish.jersey.examples.feedcombiner.model.CombinedFeed;
import org.glassfish.jersey.examples.feedcombiner.model.FeedEntry;
import org.glassfish.jersey.examples.feedcombiner.model.FeedRequestBean;
import org.glassfish.jersey.examples.feedcombiner.service.CrudService;
import org.glassfish.jersey.server.mvc.ErrorTemplate;
import org.glassfish.jersey.server.mvc.Template;
import org.glassfish.jersey.server.mvc.Viewable;

/**
 * Expose operations for a web access to feed entries
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
@Path("/")
@ErrorTemplate(name = "/error.ftl")
public class CombinedFeedController {

    @Inject
    private CrudService<CombinedFeed> feedService;

    @POST
    @Template(name = "/index.ftl")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Viewable create(@Valid @BeanParam FeedRequestBean request) {
        String[] urlArray = createUrls(request.getUrls()).stream().toArray(String[]::new);
        CombinedFeed feed = new CombinedFeed.CombinedFeedBuilder(null, urlArray)
                .title(request.getTitle())
                .description(request.getDescription())
                .refreshPeriod(request.getRefreshPeriod()).build();

        feedService.save(feed);
        return new Viewable("/index.ftl", getModel());
    }

    @POST
    @Template(name = "/index.ftl")
    @Path("/delete/{feedId}")
    public Viewable delete(@PathParam("feedId") String feedId) {
        Serializable deleted = feedService.delete(feedId);

        if (deleted != null) {
            return new Viewable("/index.ftl", getModel());
        } else {
            throw new NotFoundException("No Combined Feed was found with ID: " + feedId);
        }
    }

    @GET
    @Template(name = "/index.ftl")
    public Viewable getAll() {
        return new Viewable("/index.ftl", getModel());
    }

    @GET
    @Template(name = "/feed-entries.ftl")
    @Path("/{id}")
    public CombinedFeed get(@PathParam("id") String feedId) {
        CombinedFeed combinedFeed = feedService.get(feedId);

        if (combinedFeed != null) {
            return combinedFeed;
        } else {
            throw new NotFoundException("No Combined Feed was found with ID: " + feedId);
        }
    }

    private Map<String, Object> getModel() {
        Map<String, Object> model = new HashMap<>();
        model.put("feeds", feedService.getAll());
        return model;
    }

    private List<String> createUrls(String urlsWithCommas) {
        StringTokenizer tokenizer = new StringTokenizer(urlsWithCommas, ",");
        List<String> urls = new ArrayList<>();

        while (tokenizer.hasMoreElements()) {
            urls.add(tokenizer.nextToken());
        }
        return urls;
    }
}
