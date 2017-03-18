/*
 * Copyright (C) 2008 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glassfish.jersey.internal.guava;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import static org.glassfish.jersey.internal.guava.Preconditions.checkNotNull;

/**
 * An object which joins pieces of text (specified as an array, {@link Iterable}, varargs or even a
 * {@link Map}) with a separator. It either appends the results to an {@link Appendable} or returns
 * them as a {@link String}. Example: <pre>   {@code
 * <p>
 *   Joiner joiner = Joiner.on("; ").skipNulls();
 *    . . .
 *   return joiner.join("Harry", null, "Ron", "Hermione");}</pre>
 * <p>
 * <p>This returns the string {@code "Harry; Ron; Hermione"}. Note that all input elements are
 * converted to strings using {@link Object#toString()} before being appended.
 * <p>
 * <p>If neither {@link #skipNulls()} nor {@link #useForNull(String)} is specified, the joining
 * methods will throw {@link NullPointerException} if any given element is null.
 * <p>
 * <p><b>Warning: joiner instances are always immutable</b>; a configuration method such as {@code
 * useForNull} has no effect on the instance it is invoked on! You must store and use the new joiner
 * instance returned by the method. This makes joiners thread-safe, and safe to store as {@code
 * static final} constants. <pre>   {@code
 * <p>
 *   // Bad! Do not do this!
 *   Joiner joiner = Joiner.on(',');
 *   joiner.skipNulls(); // does nothing!
 *   return joiner.join("wrong", null, "wrong");}</pre>
 * <p>
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/StringsExplained#Joiner">{@code Joiner}</a>.
 *
 * @author Kevin Bourrillion
 * @since 2.0 (imported from Google Collections Library)
 */
public class Joiner {
    private final String separator;

    private Joiner(String separator) {
        this.separator = Preconditions.checkNotNull(separator);
    }

    /**
     * Returns a joiner which automatically places {@code separator} between consecutive elements.
     */
    public static Joiner on() {
        return new Joiner(", ");
    }

    /**
     * Appends the string representation of each of {@code parts}, using the previously configured
     * separator between each, to {@code appendable}.
     *
     * @since 11.0
     */
    private <A extends Appendable> A appendTo(A appendable, Iterator<?> parts) throws IOException {
        Preconditions.checkNotNull(appendable);
        if (parts.hasNext()) {
            appendable.append(toString(parts.next()));
            while (parts.hasNext()) {
                appendable.append(separator);
                appendable.append(toString(parts.next()));
            }
        }
        return appendable;
    }

    /**
     * Appends the string representation of each of {@code parts}, using the previously configured
     * separator between each, to {@code builder}. Identical to {@link #appendTo(Appendable,
     * Iterable)}, except that it does not throw {@link IOException}.
     *
     * @since 11.0
     */
    private StringBuilder appendTo(StringBuilder builder, Iterator<?> parts) {
        try {
            appendTo((Appendable) builder, parts);
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
        return builder;
    }

    /**
     * Returns a {@code MapJoiner} using the given key-value separator, and the same configuration as
     * this {@code Joiner} otherwise.
     */
    public MapJoiner withKeyValueSeparator() {
        return new MapJoiner(this, "=");
    }

    private CharSequence toString(Object part) {
        Preconditions.checkNotNull(part);  // checkNotNull for GWT (do not optimize).
        return (part instanceof CharSequence) ? (CharSequence) part : part.toString();
    }

    /**
     * An object that joins map entries in the same manner as {@code Joiner} joins iterables and
     * arrays. Like {@code Joiner}, it is thread-safe and immutable.
     * <p>
     * <p>In addition to operating on {@code Map} instances, {@code MapJoiner} can operate on {@code
     * Multimap} entries in two distinct modes:
     * <p>
     * <ul>
     * <li>To output a separate entry for each key-value pair, pass {@code multimap.entries()} to a
     * {@code MapJoiner} method that accepts entries as input, and receive output of the form
     * {@code key1=A&key1=B&key2=C}.
     * <li>To output a single entry for each key, pass {@code multimap.asMap()} to a {@code MapJoiner}
     * method that accepts a map as input, and receive output of the form {@code
     * key1=[A, B]&key2=C}.
     * </ul>
     *
     * @since 2.0 (imported from Google Collections Library)
     */
    public static final class MapJoiner {
        private final Joiner joiner;
        private final String keyValueSeparator;

        private MapJoiner(Joiner joiner, String keyValueSeparator) {
            this.joiner = joiner; // only "this" is ever passed, so don't checkNotNull
            this.keyValueSeparator = Preconditions.checkNotNull(keyValueSeparator);
        }

        /**
         * Appends the string representation of each entry of {@code map}, using the previously
         * configured separator and key-value separator, to {@code builder}. Identical to {@link
         * #appendTo(Appendable, Map)}, except that it does not throw {@link IOException}.
         */
        public StringBuilder appendTo(StringBuilder builder, Map<?, ?> map) {
            return appendTo(builder, map.entrySet());
        }

        /**
         * Appends the string representation of each entry in {@code entries}, using the previously
         * configured separator and key-value separator, to {@code appendable}.
         *
         * @since 11.0
         */
        public <A extends Appendable> A appendTo(A appendable, Iterator<? extends Entry<?, ?>> parts)
                throws IOException {
            Preconditions.checkNotNull(appendable);
            if (parts.hasNext()) {
                Entry<?, ?> entry = parts.next();
                appendable.append(joiner.toString(entry.getKey()));
                appendable.append(keyValueSeparator);
                appendable.append(joiner.toString(entry.getValue()));
                while (parts.hasNext()) {
                    appendable.append(joiner.separator);
                    Entry<?, ?> e = parts.next();
                    appendable.append(joiner.toString(e.getKey()));
                    appendable.append(keyValueSeparator);
                    appendable.append(joiner.toString(e.getValue()));
                }
            }
            return appendable;
        }

        /**
         * Appends the string representation of each entry in {@code entries}, using the previously
         * configured separator and key-value separator, to {@code builder}. Identical to {@link
         * #appendTo(Appendable, Iterable)}, except that it does not throw {@link IOException}.
         *
         * @since 10.0
         */
        public StringBuilder appendTo(StringBuilder builder, Iterable<? extends Entry<?, ?>> entries) {
            return appendTo(builder, entries.iterator());
        }

        /**
         * Appends the string representation of each entry in {@code entries}, using the previously
         * configured separator and key-value separator, to {@code builder}. Identical to {@link
         * #appendTo(Appendable, Iterable)}, except that it does not throw {@link IOException}.
         *
         * @since 11.0
         */
        public StringBuilder appendTo(StringBuilder builder, Iterator<? extends Entry<?, ?>> entries) {
            try {
                appendTo((Appendable) builder, entries);
            } catch (IOException impossible) {
                throw new AssertionError(impossible);
            }
            return builder;
        }

    }

}
