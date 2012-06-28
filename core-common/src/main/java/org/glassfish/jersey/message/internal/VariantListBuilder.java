/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.message.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;

/**
 * An implementation of {@link VariantListBuilder}.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class VariantListBuilder extends Variant.VariantListBuilder {

    private List<Variant> variants;
    private final List<MediaType> mediaTypes = new ArrayList<MediaType>();
    private final List<Locale> languages = new ArrayList<Locale>();
    private final List<String> encodings = new ArrayList<String>();

    @Override
    public List<Variant> build() {
        if (!mediaTypes.isEmpty() || !languages.isEmpty() || !encodings.isEmpty()) {
            // if current state is not empty, add combinations to the variant list
            add();
        }
        if (variants == null) {
            variants = new ArrayList<Variant>();
        }

        return variants;
    }

    @Override
    public VariantListBuilder add() {
        if (variants == null) {
            variants = new ArrayList<Variant>();
        }

        addMediaTypes();

        languages.clear();
        encodings.clear();
        mediaTypes.clear();

        return this;
    }

    private void addMediaTypes() {
        if (mediaTypes.isEmpty()) {
            addLanguages(null);
        } else {
            for (MediaType mediaType : mediaTypes) {
                addLanguages(mediaType);
            }
        }
    }

    private void addLanguages(MediaType mediaType) {
        if (languages.isEmpty()) {
            addEncodings(mediaType, null);
        } else {
            for (Locale language : languages) {
                addEncodings(mediaType, language);
            }
        }
    }

    private void addEncodings(MediaType mediaType, Locale language) {
        if (encodings.isEmpty()) {
            addVariant(mediaType, language, null);
        } else {
            for (String encoding : encodings) {
                addVariant(mediaType, language, encoding);
            }
        }
    }

    private void addVariant(MediaType mediaType, Locale language, String encoding) {
        variants.add(new Variant(mediaType, language, encoding));
    }

    @Override
    public VariantListBuilder languages(Locale... languages) {
        this.languages.addAll(Arrays.asList(languages));
        return this;
    }

    @Override
    public VariantListBuilder encodings(String... encodings) {
        this.encodings.addAll(Arrays.asList(encodings));
        return this;
    }

    @Override
    public VariantListBuilder mediaTypes(MediaType... mediaTypes) {
        this.mediaTypes.addAll(Arrays.asList(mediaTypes));
        return this;
    }
}
