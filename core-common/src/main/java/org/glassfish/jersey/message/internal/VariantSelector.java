/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;

import org.glassfish.jersey.internal.util.collection.Ref;

import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.collect.Lists;

/**
 * Utility for selecting variant that best matches request from a list of variants.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class VariantSelector {

    private VariantSelector() {
    }

    /**
     * Interface to get a dimension value from a variant and check if an
     * acceptable dimension value is compatible with a dimension value.
     */
    private static interface DimensionChecker<T, U> {

        /**
         * Get the dimension value from the variant.
         *
         * @param v the variant.
         * @return the dimension value.
         */
        U getDimension(VariantHolder v);

        /**
         * Get the quality source of the dimension.
         *
         * @return quality source.
         */
        int getQualitySource(VariantHolder v, U u);

        /**
         * Ascertain if the acceptable dimension value is compatible with
         * the dimension value.
         *
         * @param t the acceptable dimension value.
         * @param u the dimension value.
         * @return {@code true} if the acceptable dimension value is compatible with
         *         the dimension value.
         */
        boolean isCompatible(T t, U u);

        /**
         * Get the value of the Vary header.
         *
         * @return the value of the Vary header.
         */
        String getVaryHeaderValue();
    }

    private static final DimensionChecker<AcceptableMediaType, MediaType> MEDIA_TYPE_DC =
            new DimensionChecker<AcceptableMediaType, MediaType>() {

                @Override
                public MediaType getDimension(final VariantHolder v) {
                    return v.v.getMediaType();
                }

                @Override
                public boolean isCompatible(final AcceptableMediaType t, final MediaType u) {
                    return t.isCompatible(u);
                }

                @Override
                public int getQualitySource(final VariantHolder v, final MediaType u) {
                    return v.mediaTypeQs;
                }

                @Override
                public String getVaryHeaderValue() {
                    return HttpHeaders.ACCEPT;
                }
            };
    private static final DimensionChecker<AcceptableLanguageTag, Locale> LANGUAGE_TAG_DC =
            new DimensionChecker<AcceptableLanguageTag, Locale>() {

                @Override
                public Locale getDimension(final VariantHolder v) {
                    return v.v.getLanguage();
                }

                @Override
                public boolean isCompatible(final AcceptableLanguageTag t, final Locale u) {
                    return t.isCompatible(u);
                }

                @Override
                public int getQualitySource(final VariantHolder qsv, final Locale u) {
                    return Quality.MINIMUM;
                }

                @Override
                public String getVaryHeaderValue() {
                    return HttpHeaders.ACCEPT_LANGUAGE;
                }
            };
    private static final DimensionChecker<AcceptableToken, String> CHARSET_DC =
            new DimensionChecker<AcceptableToken, String>() {

                @Override
                public String getDimension(final VariantHolder v) {
                    final MediaType m = v.v.getMediaType();
                    return (m != null) ? m.getParameters().get("charset") : null;
                }

                @Override
                public boolean isCompatible(final AcceptableToken t, final String u) {
                    return t.isCompatible(u);
                }

                @Override
                public int getQualitySource(final VariantHolder qsv, final String u) {
                    return Quality.MINIMUM;
                }

                @Override
                public String getVaryHeaderValue() {
                    return HttpHeaders.ACCEPT_CHARSET;
                }
            };
    private static final DimensionChecker<AcceptableToken, String> ENCODING_DC =
            new DimensionChecker<AcceptableToken, String>() {

                @Override
                public String getDimension(final VariantHolder v) {
                    return v.v.getEncoding();
                }

                @Override
                public boolean isCompatible(final AcceptableToken t, final String u) {
                    return t.isCompatible(u);
                }

                @Override
                public int getQualitySource(final VariantHolder qsv, final String u) {
                    return Quality.MINIMUM;
                }

                @Override
                public String getVaryHeaderValue() {
                    return HttpHeaders.ACCEPT_ENCODING;
                }
            };

    /**
     * Select variants for a given dimension.
     *
     * @param variantHolders   collection of variants.
     * @param acceptableValues the list of acceptable dimension values, ordered by the quality
     *                         parameter, with the highest quality dimension value occurring
     *                         first.
     * @param dimensionChecker the dimension checker
     * @param vary             output list of generated vary headers.
     */
    private static <T extends Qualified, U> LinkedList<VariantHolder> selectVariants(
            final List<VariantHolder> variantHolders,
            final List<T> acceptableValues,
            final DimensionChecker<T, U> dimensionChecker,
            final Set<String> vary) {
        int cq = Quality.MINIMUM;
        int cqs = Quality.MINIMUM;

        final LinkedList<VariantHolder> selected = new LinkedList<VariantHolder>();

        // Iterate over the acceptable entries
        // This assumes the entries are ordered by the quality
        for (final T a : acceptableValues) {
            final int q = a.getQuality();

            final Iterator<VariantHolder> iv = variantHolders.iterator();
            while (iv.hasNext()) {
                final VariantHolder v = iv.next();

                // Get the dimension  value of the variant to check
                final U d = dimensionChecker.getDimension(v);

                if (d != null) {
                    vary.add(dimensionChecker.getVaryHeaderValue());
                    // Check if the acceptable entry is compatable with
                    // the dimension value
                    final int qs = dimensionChecker.getQualitySource(v, d);
                    if (qs >= cqs && dimensionChecker.isCompatible(a, d)) {
                        if (qs > cqs) {
                            cqs = qs;
                            cq = q;
                            // Remove all entries that were added for qs < cqs
                            selected.clear();
                            selected.add(v);
                        } else if (q > cq) {
                            cq = q;
                            // Add variant with higher accept quality at the front
                            selected.addFirst(v);
                        } else if (q == cq) {
                            // Ensure selection is stable with order of variants
                            // with same quality of source and accept quality
                            selected.add(v);
                        }
                        iv.remove();
                    }
                }
            }
        }

        // Add all variants that are not compatible with this dimension
        // to the end
        for (final VariantHolder v : variantHolders) {
            if (dimensionChecker.getDimension(v) == null) {
                selected.add(v);
            }
        }
        return selected;
    }

    private static class VariantHolder {

        private final Variant v;
        private final int mediaTypeQs;

        public VariantHolder(final Variant v) {
            this(v, Quality.DEFAULT);
        }

        public VariantHolder(final Variant v, final int mediaTypeQs) {
            this.v = v;
            this.mediaTypeQs = mediaTypeQs;
        }
    }

    private static LinkedList<VariantHolder> getVariantHolderList(final List<Variant> variants) {
        final LinkedList<VariantHolder> l = new LinkedList<VariantHolder>();
        for (final Variant v : variants) {
            final MediaType mt = v.getMediaType();
            if (mt != null) {
                if (mt instanceof QualitySourceMediaType || mt.getParameters()
                        .containsKey(Quality.QUALITY_SOURCE_PARAMETER_NAME)) {
                    final int qs = QualitySourceMediaType.getQualitySource(mt);
                    l.add(new VariantHolder(v, qs));
                } else {
                    l.add(new VariantHolder(v));
                }
            } else {
                l.add(new VariantHolder(v));
            }
        }

        return l;
    }

    /**
     * Select the representation variant that best matches the request. More explicit
     * variants are chosen ahead of less explicit ones.
     *
     * @param context          inbound message context.
     * @param variants         list of possible variants.
     * @param varyHeaderValue an output reference of vary header value that should be put
     *                         into the response Vary header.
     * @return selected variant.
     */
    public static Variant selectVariant(final InboundMessageContext context,
                                        final List<Variant> variants,
                                        final Ref<String> varyHeaderValue) {
        final List<Variant> selectedVariants = selectVariants(context, variants, varyHeaderValue);
        return selectedVariants.isEmpty() ? null : selectedVariants.get(0);
    }

    /**
     * Select possible representation variants in order in which they best matches the request.
     *
     * @param context inbound message context.
     * @param variants list of possible variants.
     * @param varyHeaderValue an output reference of vary header value that should be put into the response Vary header.
     * @return possible variants.
     */
    public static List<Variant> selectVariants(final InboundMessageContext context,
                                               final List<Variant> variants,
                                               final Ref<String> varyHeaderValue) {
        LinkedList<VariantHolder> vhs = getVariantHolderList(variants);

        final Set<String> vary = new HashSet<String>();
        vhs = selectVariants(vhs, context.getQualifiedAcceptableMediaTypes(), MEDIA_TYPE_DC, vary);
        vhs = selectVariants(vhs, context.getQualifiedAcceptableLanguages(), LANGUAGE_TAG_DC, vary);
        vhs = selectVariants(vhs, context.getQualifiedAcceptCharset(), CHARSET_DC, vary);
        vhs = selectVariants(vhs, context.getQualifiedAcceptEncoding(), ENCODING_DC, vary);

        if (vhs.isEmpty()) {
            return Collections.emptyList();
        } else {
            final StringBuilder varyHeader = new StringBuilder();
            for (final String v : vary) {
                if (varyHeader.length() > 0) {
                    varyHeader.append(',');
                }
                varyHeader.append(v);
            }
            final String varyValue = varyHeader.toString();
            if (!varyValue.isEmpty()) {
                varyHeaderValue.set(varyValue);
            }
            return Lists.transform(vhs, new Function<VariantHolder, Variant>() {
                @Override
                public Variant apply(final VariantHolder holder) {
                    return holder.v;
                }
            });
        }
    }
}
