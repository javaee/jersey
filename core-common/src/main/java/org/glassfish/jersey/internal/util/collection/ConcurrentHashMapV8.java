/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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
/**
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package org.glassfish.jersey.internal.util.collection;

import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A hash table supporting full concurrency of retrievals and
 * high expected concurrency for updates. This class obeys the
 * same functional specification as {@link java.util.Hashtable}, and
 * includes versions of methods corresponding to each method of
 * {@code Hashtable}. However, even though all operations are
 * thread-safe, retrieval operations do <em>not</em> entail locking,
 * and there is <em>not</em> any support for locking the entire table
 * in a way that prevents all access.  This class is fully
 * interoperable with {@code Hashtable} in programs that rely on its
 * thread safety but not on its synchronization details.
 *
 * <p>Retrieval operations (including {@code get}) generally do not
 * block, so may overlap with update operations (including {@code put}
 * and {@code remove}). Retrievals reflect the results of the most
 * recently <em>completed</em> update operations holding upon their
 * onset. (More formally, an update operation for a given key bears a
 * <em>happens-before</em> relation with any (non-null) retrieval for
 * that key reporting the updated value.)  For aggregate operations
 * such as {@code putAll} and {@code clear}, concurrent retrievals may
 * reflect insertion or removal of only some entries.  Similarly,
 * Iterators and Enumerations return elements reflecting the state of
 * the hash table at some point at or since the creation of the
 * iterator/enumeration.  They do <em>not</em> throw {@link
 * ConcurrentModificationException}.  However, iterators are designed
 * to be used by only one thread at a time.  Bear in mind that the
 * results of aggregate status methods including {@code size}, {@code
 * isEmpty}, and {@code containsValue} are typically useful only when
 * a map is not undergoing concurrent updates in other threads.
 * Otherwise the results of these methods reflect transient states
 * that may be adequate for monitoring or estimation purposes, but not
 * for program control.
 *
 * <p>The table is dynamically expanded when there are too many
 * collisions (i.e., keys that have distinct hash codes but fall into
 * the same slot modulo the table size), with the expected average
 * effect of maintaining roughly two bins per mapping (corresponding
 * to a 0.75 load factor threshold for resizing). There may be much
 * variance around this average as mappings are added and removed, but
 * overall, this maintains a commonly accepted time/space tradeoff for
 * hash tables.  However, resizing this or any other kind of hash
 * table may be a relatively slow operation. When possible, it is a
 * good idea to provide a size estimate as an optional {@code
 * initialCapacity} constructor argument. An additional optional
 * {@code loadFactor} constructor argument provides a further means of
 * customizing initial table capacity by specifying the table density
 * to be used in calculating the amount of space to allocate for the
 * given number of elements.  Also, for compatibility with previous
 * versions of this class, constructors may optionally specify an
 * expected {@code concurrencyLevel} as an additional hint for
 * internal sizing.  Note that using many keys with exactly the same
 * {@code hashCode()} is a sure way to slow down performance of any
 * hash table. To ameliorate impact, when keys are {@link Comparable},
 * this class may use comparison order among keys to help break ties.
 *
 * <p>A {@link Set} projection of a ConcurrentHashMapV8 may be created
 * (using {@link #newKeySet()} or {@link #newKeySet(int)}), or viewed
 * (using {@link #keySet(Object)} when only keys are of interest, and the
 * mapped values are (perhaps transiently) not used or all take the
 * same mapping value.
 *
 * <p>This class and its views and iterators implement all of the
 * <em>optional</em> methods of the {@link Map} and {@link Iterator}
 * interfaces.
 *
 * <p>Like {@link java.util.Hashtable} but unlike {@link HashMap}, this class
 * does <em>not</em> allow {@code null} to be used as a key or value.
 *
 * <p>ConcurrentHashMapV8s support a set of sequential and parallel bulk
 * operations that are designed
 * to be safely, and often sensibly, applied even with maps that are
 * being concurrently updated by other threads; for example, when
 * computing a snapshot summary of the values in a shared registry.
 * There are three kinds of operation, each with four forms, accepting
 * functions with Keys, Values, Entries, and (Key, Value) arguments
 * and/or return values. Because the elements of a ConcurrentHashMapV8
 * are not ordered in any particular way, and may be processed in
 * different orders in different parallel executions, the correctness
 * of supplied functions should not depend on any ordering, or on any
 * other objects or values that may transiently change while
 * computation is in progress; and except for forEach actions, should
 * ideally be side-effect-free. Bulk operations on {@link java.util.Map.Entry}
 * objects do not support method {@code setValue}.
 *
 * <ul>
 * <li> forEach: Perform a given action on each element.
 * A variant form applies a given transformation on each element
 * before performing the action.</li>
 *
 * <li> search: Return the first available non-null result of
 * applying a given function on each element; skipping further
 * search when a result is found.</li>
 *
 * <li> reduce: Accumulate each element.  The supplied reduction
 * function cannot rely on ordering (more formally, it should be
 * both associative and commutative).  There are five variants:
 *
 * <ul>
 *
 * <li> Plain reductions. (There is not a form of this method for
 * (key, value) function arguments since there is no corresponding
 * return type.)</li>
 *
 * <li> Mapped reductions that accumulate the results of a given
 * function applied to each element.</li>
 *
 * <li> Reductions to scalar doubles, longs, and ints, using a
 * given basis value.</li>
 *
 * </ul>
 * </li>
 * </ul>
 *
 * <p>These bulk operations accept a {@code parallelismThreshold}
 * argument. Methods proceed sequentially if the current map size is
 * estimated to be less than the given threshold. Using a value of
 * {@code Long.MAX_VALUE} suppresses all parallelism.  Using a value
 * of {@code 1} results in maximal parallelism by partitioning into
 * enough subtasks to fully utilize the {@code
 * ForkJoinPool#commonPool()} that is used for all parallel
 * computations. Normally, you would initially choose one of these
 * extreme values, and then measure performance of using in-between
 * values that trade off overhead versus throughput.
 *
 * <p>The concurrency properties of bulk operations follow
 * from those of ConcurrentHashMapV8: Any non-null result returned
 * from {@code get(key)} and related access methods bears a
 * happens-before relation with the associated insertion or
 * update.  The result of any bulk operation reflects the
 * composition of these per-element relations (but is not
 * necessarily atomic with respect to the map as a whole unless it
 * is somehow known to be quiescent).  Conversely, because keys
 * and values in the map are never null, null serves as a reliable
 * atomic indicator of the current lack of any result.  To
 * maintain this property, null serves as an implicit basis for
 * all non-scalar reduction operations. For the double, long, and
 * int versions, the basis should be one that, when combined with
 * any other value, returns that other value (more formally, it
 * should be the identity element for the reduction). Most common
 * reductions have these properties; for example, computing a sum
 * with basis 0 or a minimum with basis MAX_VALUE.
 *
 * <p>Search and transformation functions provided as arguments
 * should similarly return null to indicate the lack of any result
 * (in which case it is not used). In the case of mapped
 * reductions, this also enables transformations to serve as
 * filters, returning null (or, in the case of primitive
 * specializations, the identity basis) if the element should not
 * be combined. You can create compound transformations and
 * filterings by composing them yourself under this "null means
 * there is nothing there now" rule before using them in search or
 * reduce operations.
 *
 * <p>Methods accepting and/or returning Entry arguments maintain
 * key-value associations. They may be useful for example when
 * finding the key for the greatest value. Note that "plain" Entry
 * arguments can be supplied using {@code new
 * AbstractMap.SimpleEntry(k,v)}.
 *
 * <p>Bulk operations may complete abruptly, throwing an
 * exception encountered in the application of a supplied
 * function. Bear in mind when handling such exceptions that other
 * concurrently executing functions could also have thrown
 * exceptions, or would have done so if the first exception had
 * not occurred.
 *
 * <p>Speedups for parallel compared to sequential forms are common
 * but not guaranteed.  Parallel operations involving brief functions
 * on small maps may execute more slowly than sequential forms if the
 * underlying work to parallelize the computation is more expensive
 * than the computation itself.  Similarly, parallelization may not
 * lead to much actual parallelism if all processors are busy
 * performing unrelated tasks.
 *
 * <p>All arguments to all task methods must be non-null.
 *
 * <p><em>jsr166e note: During transition, this class
 * uses nested functional interfaces with different names but the
 * same forms as those expected for JDK8.</em>
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @author Doug Lea
 */
class ConcurrentHashMapV8<K, V> extends AbstractMap<K, V>
        implements ConcurrentMap<K, V>, Serializable {
    private static final long serialVersionUID = 7249069246763182397L;

    /*
     * Overview:
     *
     * The primary design goal of this hash table is to maintain
     * concurrent readability (typically method get(), but also
     * iterators and related methods) while minimizing update
     * contention. Secondary goals are to keep space consumption about
     * the same or better than java.util.HashMap, and to support high
     * initial insertion rates on an empty table by many threads.
     *
     * This map usually acts as a binned (bucketed) hash table.  Each
     * key-value mapping is held in a Node.  Most nodes are instances
     * of the basic Node class with hash, key, value, and next
     * fields. However, various subclasses exist: TreeNodes are
     * arranged in balanced trees, not lists.  TreeBins hold the roots
     * of sets of TreeNodes. ForwardingNodes are placed at the heads
     * of bins during resizing. ReservationNodes are used as
     * placeholders while establishing values in computeIfAbsent and
     * related methods.  The types TreeBin, ForwardingNode, and
     * ReservationNode do not hold normal user keys, values, or
     * hashes, and are readily distinguishable during search etc
     * because they have negative hash fields and null key and value
     * fields. (These special nodes are either uncommon or transient,
     * so the impact of carrying around some unused fields is
     * insignificant.)
     *
     * The table is lazily initialized to a power-of-two size upon the
     * first insertion.  Each bin in the table normally contains a
     * list of Nodes (most often, the list has only zero or one Node).
     * Table accesses require volatile/atomic reads, writes, and
     * CASes.  Because there is no other way to arrange this without
     * adding further indirections, we use intrinsics
     * (sun.misc.Unsafe) operations.
     *
     * We use the top (sign) bit of Node hash fields for control
     * purposes -- it is available anyway because of addressing
     * constraints.  Nodes with negative hash fields are specially
     * handled or ignored in map methods.
     *
     * Insertion (via put or its variants) of the first node in an
     * empty bin is performed by just CASing it to the bin.  This is
     * by far the most common case for put operations under most
     * key/hash distributions.  Other update operations (insert,
     * delete, and replace) require locks.  We do not want to waste
     * the space required to associate a distinct lock object with
     * each bin, so instead use the first node of a bin list itself as
     * a lock. Locking support for these locks relies on builtin
     * "synchronized" monitors.
     *
     * Using the first node of a list as a lock does not by itself
     * suffice though: When a node is locked, any update must first
     * validate that it is still the first node after locking it, and
     * retry if not. Because new nodes are always appended to lists,
     * once a node is first in a bin, it remains first until deleted
     * or the bin becomes invalidated (upon resizing).
     *
     * The main disadvantage of per-bin locks is that other update
     * operations on other nodes in a bin list protected by the same
     * lock can stall, for example when user equals() or mapping
     * functions take a long time.  However, statistically, under
     * random hash codes, this is not a common problem.  Ideally, the
     * frequency of nodes in bins follows a Poisson distribution
     * (http://en.wikipedia.org/wiki/Poisson_distribution) with a
     * parameter of about 0.5 on average, given the resizing threshold
     * of 0.75, although with a large variance because of resizing
     * granularity. Ignoring variance, the expected occurrences of
     * list size k are (exp(-0.5) * pow(0.5, k) / factorial(k)). The
     * first values are:
     *
     * 0:    0.60653066
     * 1:    0.30326533
     * 2:    0.07581633
     * 3:    0.01263606
     * 4:    0.00157952
     * 5:    0.00015795
     * 6:    0.00001316
     * 7:    0.00000094
     * 8:    0.00000006
     * more: less than 1 in ten million
     *
     * Lock contention probability for two threads accessing distinct
     * elements is roughly 1 / (8 * #elements) under random hashes.
     *
     * Actual hash code distributions encountered in practice
     * sometimes deviate significantly from uniform randomness.  This
     * includes the case when N > (1<<30), so some keys MUST collide.
     * Similarly for dumb or hostile usages in which multiple keys are
     * designed to have identical hash codes or ones that differs only
     * in masked-out high bits. So we use a secondary strategy that
     * applies when the number of nodes in a bin exceeds a
     * threshold. These TreeBins use a balanced tree to hold nodes (a
     * specialized form of red-black trees), bounding search time to
     * O(log N).  Each search step in a TreeBin is at least twice as
     * slow as in a regular list, but given that N cannot exceed
     * (1<<64) (before running out of addresses) this bounds search
     * steps, lock hold times, etc, to reasonable constants (roughly
     * 100 nodes inspected per operation worst case) so long as keys
     * are Comparable (which is very common -- String, Long, etc).
     * TreeBin nodes (TreeNodes) also maintain the same "next"
     * traversal pointers as regular nodes, so can be traversed in
     * iterators in the same way.
     *
     * The table is resized when occupancy exceeds a percentage
     * threshold (nominally, 0.75, but see below).  Any thread
     * noticing an overfull bin may assist in resizing after the
     * initiating thread allocates and sets up the replacement
     * array. However, rather than stalling, these other threads may
     * proceed with insertions etc.  The use of TreeBins shields us
     * from the worst case effects of overfilling while resizes are in
     * progress.  Resizing proceeds by transferring bins, one by one,
     * from the table to the next table. To enable concurrency, the
     * next table must be (incrementally) prefilled with place-holders
     * serving as reverse forwarders to the old table.  Because we are
     * using power-of-two expansion, the elements from each bin must
     * either stay at same index, or move with a power of two
     * offset. We eliminate unnecessary node creation by catching
     * cases where old nodes can be reused because their next fields
     * won't change.  On average, only about one-sixth of them need
     * cloning when a table doubles. The nodes they replace will be
     * garbage collectable as soon as they are no longer referenced by
     * any reader thread that may be in the midst of concurrently
     * traversing table.  Upon transfer, the old table bin contains
     * only a special forwarding node (with hash field "MOVED") that
     * contains the next table as its key. On encountering a
     * forwarding node, access and update operations restart, using
     * the new table.
     *
     * Each bin transfer requires its bin lock, which can stall
     * waiting for locks while resizing. However, because other
     * threads can join in and help resize rather than contend for
     * locks, average aggregate waits become shorter as resizing
     * progresses.  The transfer operation must also ensure that all
     * accessible bins in both the old and new table are usable by any
     * traversal.  This is arranged by proceeding from the last bin
     * (table.length - 1) up towards the first.  Upon seeing a
     * forwarding node, traversals (see class Traverser) arrange to
     * move to the new table without revisiting nodes.  However, to
     * ensure that no intervening nodes are skipped, bin splitting can
     * only begin after the associated reverse-forwarders are in
     * place.
     *
     * The traversal scheme also applies to partial traversals of
     * ranges of bins (via an alternate Traverser constructor)
     * to support partitioned aggregate operations.  Also, read-only
     * operations give up if ever forwarded to a null table, which
     * provides support for shutdown-style clearing, which is also not
     * currently implemented.
     *
     * Lazy table initialization minimizes footprint until first use,
     * and also avoids resizings when the first operation is from a
     * putAll, constructor with map argument, or deserialization.
     * These cases attempt to override the initial capacity settings,
     * but harmlessly fail to take effect in cases of races.
     *
     * The element count is maintained using a specialization of
     * LongAdder. We need to incorporate a specialization rather than
     * just use a LongAdder in order to access implicit
     * contention-sensing that leads to creation of multiple
     * CounterCells.  The counter mechanics avoid contention on
     * updates but can encounter cache thrashing if read too
     * frequently during concurrent access. To avoid reading so often,
     * resizing under contention is attempted only upon adding to a
     * bin already holding two or more nodes. Under uniform hash
     * distributions, the probability of this occurring at threshold
     * is around 13%, meaning that only about 1 in 8 puts check
     * threshold (and after resizing, many fewer do so).
     *
     * TreeBins use a special form of comparison for search and
     * related operations (which is the main reason we cannot use
     * existing collections such as TreeMaps). TreeBins contain
     * Comparable elements, but may contain others, as well as
     * elements that are Comparable but not necessarily Comparable for
     * the same T, so we cannot invoke compareTo among them. To handle
     * this, the tree is ordered primarily by hash value, then by
     * Comparable.compareTo order if applicable.  On lookup at a node,
     * if elements are not comparable or compare as 0 then both left
     * and right children may need to be searched in the case of tied
     * hash values. (This corresponds to the full list search that
     * would be necessary if all elements were non-Comparable and had
     * tied hashes.) On insertion, to keep a total ordering (or as
     * close as is required here) across rebalancings, we compare
     * classes and identityHashCodes as tie-breakers. The red-black
     * balancing code is updated from pre-jdk-collections
     * (http://gee.cs.oswego.edu/dl/classes/collections/RBCell.java)
     * based in turn on Cormen, Leiserson, and Rivest "Introduction to
     * Algorithms" (CLR).
     *
     * TreeBins also require an additional locking mechanism.  While
     * list traversal is always possible by readers even during
     * updates, tree traversal is not, mainly because of tree-rotations
     * that may change the root node and/or its linkages.  TreeBins
     * include a simple read-write lock mechanism parasitic on the
     * main bin-synchronization strategy: Structural adjustments
     * associated with an insertion or removal are already bin-locked
     * (and so cannot conflict with other writers) but must wait for
     * ongoing readers to finish. Since there can be only one such
     * waiter, we use a simple scheme using a single "waiter" field to
     * block writers.  However, readers need never block.  If the root
     * lock is held, they proceed along the slow traversal path (via
     * next-pointers) until the lock becomes available or the list is
     * exhausted, whichever comes first. These cases are not fast, but
     * maximize aggregate expected throughput.
     *
     * Maintaining API and serialization compatibility with previous
     * versions of this class introduces several oddities. Mainly: We
     * leave untouched but unused constructor arguments refering to
     * concurrencyLevel. We accept a loadFactor constructor argument,
     * but apply it only to initial table capacity (which is the only
     * time that we can guarantee to honor it.) We also declare an
     * unused "Segment" class that is instantiated in minimal form
     * only when serializing.
     *
     * Also, solely for compatibility with previous versions of this
     * class, it extends AbstractMap, even though all of its methods
     * are overridden, so it is just useless baggage.
     *
     * This file is organized to make things a little easier to follow
     * while reading than they might otherwise: First the main static
     * declarations and utilities, then fields, then main public
     * methods (with a few factorings of multiple public methods into
     * internal ones), then sizing methods, trees, traversers, and
     * bulk operations.
     */

    /* ---------------- Constants -------------- */

    /**
     * The largest possible table capacity.  This value must be
     * exactly 1<<30 to stay within Java array allocation and indexing
     * bounds for power of two table sizes, and is further required
     * because the top two bits of 32bit hash fields are used for
     * control purposes.
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The default initial table capacity.  Must be a power of 2
     * (i.e., at least 1) and at most MAXIMUM_CAPACITY.
     */
    private static final int DEFAULT_CAPACITY = 16;

    /**
     * The largest possible (non-power of two) array size.
     * Needed by toArray and related methods.
     */
    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * The default concurrency level for this table. Unused but
     * defined for compatibility with previous versions of this class.
     */
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    /**
     * The load factor for this table. Overrides of this value in
     * constructors affect only the initial table capacity.  The
     * actual floating point value isn't normally used -- it is
     * simpler to use expressions such as {@code n - (n >>> 2)} for
     * the associated resizing threshold.
     */
    private static final float LOAD_FACTOR = 0.75f;

    /**
     * The bin count threshold for using a tree rather than list for a
     * bin.  Bins are converted to trees when adding an element to a
     * bin with at least this many nodes. The value must be greater
     * than 2, and should be at least 8 to mesh with assumptions in
     * tree removal about conversion back to plain bins upon
     * shrinkage.
     */
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * The bin count threshold for untreeifying a (split) bin during a
     * resize operation. Should be less than TREEIFY_THRESHOLD, and at
     * most 6 to mesh with shrinkage detection under removal.
     */
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * The smallest table capacity for which bins may be treeified.
     * (Otherwise the table is resized if too many nodes in a bin.)
     * The value should be at least 4 * TREEIFY_THRESHOLD to avoid
     * conflicts between resizing and treeification thresholds.
     */
    static final int MIN_TREEIFY_CAPACITY = 64;

    /**
     * Minimum number of rebinnings per transfer step. Ranges are
     * subdivided to allow multiple resizer threads.  This value
     * serves as a lower bound to avoid resizers encountering
     * excessive memory contention.  The value should be at least
     * DEFAULT_CAPACITY.
     */
    private static final int MIN_TRANSFER_STRIDE = 16;

    /*
     * Encodings for Node hash fields. See above for explanation.
     */
    static final int MOVED = -1; // hash for forwarding nodes
    static final int TREEBIN = -2; // hash for roots of trees
    static final int RESERVED = -3; // hash for transient reservations
    static final int HASH_BITS = 0x7fffffff; // usable bits of normal node hash

    /**
     * Number of CPUS, to place bounds on some sizings
     */
    static final int NCPU = Runtime.getRuntime().availableProcessors();

    /**
     * For serialization compatibility.
     */
    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("segments", Segment[].class),
            new ObjectStreamField("segmentMask", Integer.TYPE),
            new ObjectStreamField("segmentShift", Integer.TYPE)
    };

    /* ---------------- Nodes -------------- */

    /**
     * Key-value entry.  This class is never exported out as a
     * user-mutable Map.Entry (i.e., one supporting setValue; see
     * MapEntry below), but can be used for read-only traversals used
     * in bulk tasks.  Subclasses of Node with a negative hash field
     * are special, and contain null keys and values (but are never
     * exported).  Otherwise, keys and vals are never null.
     */
    static class Node<K, V> implements Map.Entry<K, V> {
        final int hash;
        final K key;
        volatile V val;
        volatile Node<K, V> next;

        Node(final int hash, final K key, final V val, final Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.val = val;
            this.next = next;
        }

        public final K getKey() {
            return key;
        }

        public final V getValue() {
            return val;
        }

        public final int hashCode() {
            return key.hashCode() ^ val.hashCode();
        }

        public final String toString() {
            return key + "=" + val;
        }

        public final V setValue(final V value) {
            throw new UnsupportedOperationException();
        }

        public final boolean equals(final Object o) {
            final Object k;
            final Object v;
            final Object u;
            final Map.Entry<?, ?> e;
            return ((o instanceof Map.Entry) &&
                    (k = (e = (Map.Entry<?, ?>) o).getKey()) != null &&
                    (v = e.getValue()) != null &&
                    (k == key || k.equals(key)) &&
                    (v == (u = val) || v.equals(u)));
        }

        /**
         * Virtualized support for map.get(); overridden in subclasses.
         */
        Node<K, V> find(final int h, final Object k) {
            Node<K, V> e = this;
            if (k != null) {
                do {
                    final K ek;
                    if (e.hash == h &&
                            ((ek = e.key) == k || (ek != null && k.equals(ek))))
                        return e;
                } while ((e = e.next) != null);
            }
            return null;
        }
    }

    /* ---------------- Static utilities -------------- */

    /**
     * Spreads (XORs) higher bits of hash to lower and also forces top
     * bit to 0. Because the table uses power-of-two masking, sets of
     * hashes that vary only in bits above the current mask will
     * always collide. (Among known examples are sets of Float keys
     * holding consecutive whole numbers in small tables.)  So we
     * apply a transform that spreads the impact of higher bits
     * downward. There is a tradeoff between speed, utility, and
     * quality of bit-spreading. Because many common sets of hashes
     * are already reasonably distributed (so don't benefit from
     * spreading), and because we use trees to handle large sets of
     * collisions in bins, we just XOR some shifted bits in the
     * cheapest possible way to reduce systematic lossage, as well as
     * to incorporate impact of the highest bits that would otherwise
     * never be used in index calculations because of table bounds.
     */
    static final int spread(final int h) {
        return (h ^ (h >>> 16)) & HASH_BITS;
    }

    /**
     * Returns a power of two table size for the given desired capacity.
     * See Hackers Delight, sec 3.2
     */
    private static final int tableSizeFor(final int c) {
        int n = c - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    /**
     * Returns x's Class if it is of the form "class C implements
     * Comparable<C>", else null.
     */
    static Class<?> comparableClassFor(final Object x) {
        if (x instanceof Comparable) {
            final Class<?> c;
            final Type[] ts;
            Type[] as;
            Type t;
            ParameterizedType p;
            if ((c = x.getClass()) == String.class) // bypass checks
                return c;
            if ((ts = c.getGenericInterfaces()) != null) {
                for (int i = 0; i < ts.length; ++i) {
                    if (((t = ts[i]) instanceof ParameterizedType) &&
                            ((p = (ParameterizedType) t).getRawType() ==
                                    Comparable.class) &&
                            (as = p.getActualTypeArguments()) != null &&
                            as.length == 1 && as[0] == c) // type arg is c
                        return c;
                }
            }
        }
        return null;
    }

    /**
     * Returns k.compareTo(x) if x matches kc (k's screened comparable
     * class), else 0.
     */
    @SuppressWarnings({"rawtypes", "unchecked"}) // for cast to Comparable
    static int compareComparables(final Class<?> kc, final Object k, final Object x) {
        return (x == null || x.getClass() != kc ? 0 :
                ((Comparable) k).compareTo(x));
    }

    /* ---------------- Table element access -------------- */

    /*
     * Volatile access methods are used for table elements as well as
     * elements of in-progress next table while resizing.  All uses of
     * the tab arguments must be null checked by callers.  All callers
     * also paranoically precheck that tab's length is not zero (or an
     * equivalent check), thus ensuring that any index argument taking
     * the form of a hash value anded with (length - 1) is a valid
     * index.  Note that, to be correct wrt arbitrary concurrency
     * errors by users, these checks must operate on local variables,
     * which accounts for some odd-looking inline assignments below.
     * Note that calls to setTabAt always occur within locked regions,
     * and so in principle require only release ordering, not need
     * full volatile semantics, but are currently coded as volatile
     * writes to be conservative.
     */

    @SuppressWarnings("unchecked")
    static final <K, V> Node<K, V> tabAt(final Node<K, V>[] tab, final int i) {
        return (Node<K, V>) U.getObjectVolatile(tab, ((long) i << ASHIFT) + ABASE);
    }

    static final <K, V> boolean casTabAt(final Node<K, V>[] tab, final int i,
                                         final Node<K, V> c, final Node<K, V> v) {
        return U.compareAndSwapObject(tab, ((long) i << ASHIFT) + ABASE, c, v);
    }

    static final <K, V> void setTabAt(final Node<K, V>[] tab, final int i, final Node<K, V> v) {
        U.putObjectVolatile(tab, ((long) i << ASHIFT) + ABASE, v);
    }

    /* ---------------- Fields -------------- */

    /**
     * The array of bins. Lazily initialized upon first insertion.
     * Size is always a power of two. Accessed directly by iterators.
     */
    transient volatile Node<K, V>[] table;

    /**
     * The next table to use; non-null only while resizing.
     */
    private transient volatile Node<K, V>[] nextTable;

    /**
     * Base counter value, used mainly when there is no contention,
     * but also as a fallback during table initialization
     * races. Updated via CAS.
     */
    private transient volatile long baseCount;

    /**
     * Table initialization and resizing control.  When negative, the
     * table is being initialized or resized: -1 for initialization,
     * else -(1 + the number of active resizing threads).  Otherwise,
     * when table is null, holds the initial table size to use upon
     * creation, or 0 for default. After initialization, holds the
     * next element count value upon which to resize the table.
     */
    private transient volatile int sizeCtl;

    /**
     * The next table index (plus one) to split while resizing.
     */
    private transient volatile int transferIndex;

    /**
     * The least available table index to split while resizing.
     */
    private transient volatile int transferOrigin;

    /**
     * Spinlock (locked via CAS) used when resizing and/or creating CounterCells.
     */
    private transient volatile int cellsBusy;

    /**
     * Table of counter cells. When non-null, size is a power of 2.
     */
    private transient volatile CounterCell[] counterCells;

    // views
    private transient KeySetView<K, V> keySet;
    private transient ValuesView<K, V> values;
    private transient EntrySetView<K, V> entrySet;


    /* ---------------- Public operations -------------- */

    /**
     * Creates a new, empty map with the default initial table size (16).
     */
    ConcurrentHashMapV8() {
    }

    /**
     * Creates a new, empty map with an initial table size
     * accommodating the specified number of elements without the need
     * to dynamically resize.
     *
     * @param initialCapacity The implementation performs internal
     *                        sizing to accommodate this many elements.
     * @throws IllegalArgumentException if the initial capacity of
     *                                  elements is negative
     */
    ConcurrentHashMapV8(final int initialCapacity) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException();
        final int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ?
                MAXIMUM_CAPACITY :
                tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
        this.sizeCtl = cap;
    }

    /**
     * Creates a new map with the same mappings as the given map.
     *
     * @param m the map
     */
    ConcurrentHashMapV8(final Map<? extends K, ? extends V> m) {
        this.sizeCtl = DEFAULT_CAPACITY;
        putAll(m);
    }

    /**
     * Creates a new, empty map with an initial table size based on
     * the given number of elements ({@code initialCapacity}) and
     * initial table density ({@code loadFactor}).
     *
     * @param initialCapacity the initial capacity. The implementation
     *                        performs internal sizing to accommodate this many elements,
     *                        given the specified load factor.
     * @param loadFactor      the load factor (table density) for
     *                        establishing the initial table size
     * @throws IllegalArgumentException if the initial capacity of
     *                                  elements is negative or the load factor is nonpositive
     * @since 1.6
     */
    ConcurrentHashMapV8(final int initialCapacity, final float loadFactor) {
        this(initialCapacity, loadFactor, 1);
    }

    /**
     * Creates a new, empty map with an initial table size based on
     * the given number of elements ({@code initialCapacity}), table
     * density ({@code loadFactor}), and number of concurrently
     * updating threads ({@code concurrencyLevel}).
     *
     * @param initialCapacity  the initial capacity. The implementation
     *                         performs internal sizing to accommodate this many elements,
     *                         given the specified load factor.
     * @param loadFactor       the load factor (table density) for
     *                         establishing the initial table size
     * @param concurrencyLevel the estimated number of concurrently
     *                         updating threads. The implementation may use this value as
     *                         a sizing hint.
     * @throws IllegalArgumentException if the initial capacity is
     *                                  negative or the load factor or concurrencyLevel are
     *                                  nonpositive
     */
    ConcurrentHashMapV8(int initialCapacity,
                        final float loadFactor, final int concurrencyLevel) {
        if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0)
            throw new IllegalArgumentException();
        if (initialCapacity < concurrencyLevel)   // Use at least as many bins
            initialCapacity = concurrencyLevel;   // as estimated threads
        final long size = (long) (1.0 + (long) initialCapacity / loadFactor);
        final int cap = (size >= (long) MAXIMUM_CAPACITY) ?
                MAXIMUM_CAPACITY : tableSizeFor((int) size);
        this.sizeCtl = cap;
    }

    // Original (since JDK1.2) Map methods

    /**
     * {@inheritDoc}
     */
    public int size() {
        final long n = sumCount();
        return ((n < 0L) ? 0 :
                (n > (long) Integer.MAX_VALUE) ? Integer.MAX_VALUE :
                        (int) n);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return sumCount() <= 0L; // ignore transient negative values
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code key.equals(k)},
     * then this method returns {@code v}; otherwise it returns
     * {@code null}.  (There can be at most one such mapping.)
     *
     * @throws NullPointerException if the specified key is null
     */
    public V get(final Object key) {
        final Node<K, V>[] tab;
        Node<K, V> e;
        final Node<K, V> p;
        final int n;
        final int eh;
        K ek;
        final int h = spread(key.hashCode());
        if ((tab = table) != null && (n = tab.length) > 0 &&
                (e = tabAt(tab, (n - 1) & h)) != null) {
            if ((eh = e.hash) == h) {
                if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                    return e.val;
            } else if (eh < 0)
                return (p = e.find(h, key)) != null ? p.val : null;
            while ((e = e.next) != null) {
                if (e.hash == h &&
                        ((ek = e.key) == key || (ek != null && key.equals(ek))))
                    return e.val;
            }
        }
        return null;
    }

    /**
     * Tests if the specified object is a key in this table.
     *
     * @param key possible key
     * @return {@code true} if and only if the specified object
     *         is a key in this table, as determined by the
     *         {@code equals} method; {@code false} otherwise
     * @throws NullPointerException if the specified key is null
     */
    public boolean containsKey(final Object key) {
        return get(key) != null;
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the
     * specified value. Note: This method may require a full traversal
     * of the map, and is much slower than method {@code containsKey}.
     *
     * @param value value whose presence in this map is to be tested
     * @return {@code true} if this map maps one or more keys to the
     *         specified value
     * @throws NullPointerException if the specified value is null
     */
    public boolean containsValue(final Object value) {
        if (value == null)
            throw new NullPointerException();
        final Node<K, V>[] t;
        if ((t = table) != null) {
            final Traverser<K, V> it = new Traverser<K, V>(t, t.length, 0, t.length);
            for (Node<K, V> p; (p = it.advance()) != null; ) {
                final V v;
                if ((v = p.val) == value || (v != null && value.equals(v)))
                    return true;
            }
        }
        return false;
    }

    /**
     * Maps the specified key to the specified value in this table.
     * Neither the key nor the value can be null.
     *
     * <p>The value can be retrieved by calling the {@code get} method
     * with a key that is equal to the original key.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}
     * @throws NullPointerException if the specified key or value is null
     */
    public V put(final K key, final V value) {
        return putVal(key, value, false);
    }

    /**
     * Implementation for put and putIfAbsent
     */
    final V putVal(final K key, final V value, final boolean onlyIfAbsent) {
        if (key == null || value == null) throw new NullPointerException();
        final int hash = spread(key.hashCode());
        int binCount = 0;
        for (Node<K, V>[] tab = table; ; ) {
            final Node<K, V> f;
            final int n;
            final int i;
            final int fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
                if (casTabAt(tab, i, null,
                        new Node<K, V>(hash, key, value, null)))
                    break;                   // no lock when adding to empty bin
            } else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                V oldVal = null;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K, V> e = f; ; ++binCount) {
                                final K ek;
                                if (e.hash == hash &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    oldVal = e.val;
                                    if (!onlyIfAbsent)
                                        e.val = value;
                                    break;
                                }
                                final Node<K, V> pred = e;
                                if ((e = e.next) == null) {
                                    pred.next = new Node<K, V>(hash, key,
                                            value, null);
                                    break;
                                }
                            }
                        } else if (f instanceof TreeBin) {
                            final Node<K, V> p;
                            binCount = 2;
                            if ((p = ((TreeBin<K, V>) f).putTreeVal(hash, key,
                                    value)) != null) {
                                oldVal = p.val;
                                if (!onlyIfAbsent)
                                    p.val = value;
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    if (oldVal != null)
                        return oldVal;
                    break;
                }
            }
        }
        addCount(1L, binCount);
        return null;
    }

    /**
     * Copies all of the mappings from the specified map to this one.
     * These mappings replace any mappings that this map had for any of the
     * keys currently in the specified map.
     *
     * @param m mappings to be stored in this map
     */
    public void putAll(final Map<? extends K, ? extends V> m) {
        tryPresize(m.size());
        for (final Map.Entry<? extends K, ? extends V> e : m.entrySet())
            putVal(e.getKey(), e.getValue(), false);
    }

    /**
     * Removes the key (and its corresponding value) from this map.
     * This method does nothing if the key is not in the map.
     *
     * @param key the key that needs to be removed
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}
     * @throws NullPointerException if the specified key is null
     */
    public V remove(final Object key) {
        return replaceNode(key, null, null);
    }

    /**
     * Implementation for the four public remove/replace methods:
     * Replaces node value with v, conditional upon match of cv if
     * non-null.  If resulting value is null, delete.
     */
    final V replaceNode(final Object key, final V value, final Object cv) {
        final int hash = spread(key.hashCode());
        for (Node<K, V>[] tab = table; ; ) {
            final Node<K, V> f;
            final int n;
            final int i;
            final int fh;
            if (tab == null || (n = tab.length) == 0 ||
                    (f = tabAt(tab, i = (n - 1) & hash)) == null)
                break;
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                V oldVal = null;
                boolean validated = false;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            validated = true;
                            for (Node<K, V> e = f, pred = null; ; ) {
                                final K ek;
                                if (e.hash == hash &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    final V ev = e.val;
                                    if (cv == null || cv == ev ||
                                            (ev != null && cv.equals(ev))) {
                                        oldVal = ev;
                                        if (value != null)
                                            e.val = value;
                                        else if (pred != null)
                                            pred.next = e.next;
                                        else
                                            setTabAt(tab, i, e.next);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null)
                                    break;
                            }
                        } else if (f instanceof TreeBin) {
                            validated = true;
                            final TreeBin<K, V> t = (TreeBin<K, V>) f;
                            final TreeNode<K, V> r;
                            final TreeNode<K, V> p;
                            if ((r = t.root) != null &&
                                    (p = r.findTreeNode(hash, key, null)) != null) {
                                final V pv = p.val;
                                if (cv == null || cv == pv ||
                                        (pv != null && cv.equals(pv))) {
                                    oldVal = pv;
                                    if (value != null)
                                        p.val = value;
                                    else if (t.removeTreeNode(p))
                                        setTabAt(tab, i, untreeify(t.first));
                                }
                            }
                        }
                    }
                }
                if (validated) {
                    if (oldVal != null) {
                        if (value == null)
                            addCount(-1L, -1);
                        return oldVal;
                    }
                    break;
                }
            }
        }
        return null;
    }

    /**
     * Removes all of the mappings from this map.
     */
    public void clear() {
        long delta = 0L; // negative number of deletions
        int i = 0;
        Node<K, V>[] tab = table;
        while (tab != null && i < tab.length) {
            final int fh;
            final Node<K, V> f = tabAt(tab, i);
            if (f == null)
                ++i;
            else if ((fh = f.hash) == MOVED) {
                tab = helpTransfer(tab, f);
                i = 0; // restart
            } else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        Node<K, V> p = (fh >= 0 ? f :
                                (f instanceof TreeBin) ?
                                        ((TreeBin<K, V>) f).first : null);
                        while (p != null) {
                            --delta;
                            p = p.next;
                        }
                        setTabAt(tab, i++, null);
                    }
                }
            }
        }
        if (delta != 0L)
            addCount(delta, -1);
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa. The set supports element
     * removal, which removes the corresponding mapping from this map,
     * via the {@code Iterator.remove}, {@code Set.remove},
     * {@code removeAll}, {@code retainAll}, and {@code clear}
     * operations.  It does not support the {@code add} or
     * {@code addAll} operations.
     *
     * <p>The view's {@code iterator} is a "weakly consistent" iterator
     * that will never throw {@link ConcurrentModificationException},
     * and guarantees to traverse elements as they existed upon
     * construction of the iterator, and may (but is not guaranteed to)
     * reflect any modifications subsequent to construction.
     *
     * @return the set view
     */
    public KeySetView<K, V> keySet() {
        final KeySetView<K, V> ks;
        return (ks = keySet) != null ? ks : (keySet = new KeySetView<K, V>(this, null));
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  The collection
     * supports element removal, which removes the corresponding
     * mapping from this map, via the {@code Iterator.remove},
     * {@code Collection.remove}, {@code removeAll},
     * {@code retainAll}, and {@code clear} operations.  It does not
     * support the {@code add} or {@code addAll} operations.
     *
     * <p>The view's {@code iterator} is a "weakly consistent" iterator
     * that will never throw {@link ConcurrentModificationException},
     * and guarantees to traverse elements as they existed upon
     * construction of the iterator, and may (but is not guaranteed to)
     * reflect any modifications subsequent to construction.
     *
     * @return the collection view
     */
    public Collection<V> values() {
        final ValuesView<K, V> vs;
        return (vs = values) != null ? vs : (values = new ValuesView<K, V>(this));
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  The set supports element
     * removal, which removes the corresponding mapping from the map,
     * via the {@code Iterator.remove}, {@code Set.remove},
     * {@code removeAll}, {@code retainAll}, and {@code clear}
     * operations.
     *
     * <p>The view's {@code iterator} is a "weakly consistent" iterator
     * that will never throw {@link ConcurrentModificationException},
     * and guarantees to traverse elements as they existed upon
     * construction of the iterator, and may (but is not guaranteed to)
     * reflect any modifications subsequent to construction.
     *
     * @return the set view
     */
    public Set<Map.Entry<K, V>> entrySet() {
        final EntrySetView<K, V> es;
        return (es = entrySet) != null ? es : (entrySet = new EntrySetView<K, V>(this));
    }

    /**
     * Returns the hash code value for this {@link Map}, i.e.,
     * the sum of, for each key-value pair in the map,
     * {@code key.hashCode() ^ value.hashCode()}.
     *
     * @return the hash code value for this map
     */
    public int hashCode() {
        int h = 0;
        final Node<K, V>[] t;
        if ((t = table) != null) {
            final Traverser<K, V> it = new Traverser<K, V>(t, t.length, 0, t.length);
            for (Node<K, V> p; (p = it.advance()) != null; )
                h += p.key.hashCode() ^ p.val.hashCode();
        }
        return h;
    }

    /**
     * Returns a string representation of this map.  The string
     * representation consists of a list of key-value mappings (in no
     * particular order) enclosed in braces ("{@code {}}").  Adjacent
     * mappings are separated by the characters {@code ", "} (comma
     * and space).  Each key-value mapping is rendered as the key
     * followed by an equals sign ("{@code =}") followed by the
     * associated value.
     *
     * @return a string representation of this map
     */
    public String toString() {
        final Node<K, V>[] t;
        final int f = (t = table) == null ? 0 : t.length;
        final Traverser<K, V> it = new Traverser<K, V>(t, f, 0, f);
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        Node<K, V> p;
        if ((p = it.advance()) != null) {
            for (; ; ) {
                final K k = p.key;
                final V v = p.val;
                sb.append(k == this ? "(this Map)" : k);
                sb.append('=');
                sb.append(v == this ? "(this Map)" : v);
                if ((p = it.advance()) == null)
                    break;
                sb.append(',').append(' ');
            }
        }
        return sb.append('}').toString();
    }

    /**
     * Compares the specified object with this map for equality.
     * Returns {@code true} if the given object is a map with the same
     * mappings as this map.  This operation may return misleading
     * results if either map is concurrently modified during execution
     * of this method.
     *
     * @param o object to be compared for equality with this map
     * @return {@code true} if the specified object is equal to this map
     */
    public boolean equals(final Object o) {
        if (o != this) {
            if (!(o instanceof Map))
                return false;
            final Map<?, ?> m = (Map<?, ?>) o;
            final Node<K, V>[] t;
            final int f = (t = table) == null ? 0 : t.length;
            final Traverser<K, V> it = new Traverser<K, V>(t, f, 0, f);
            for (Node<K, V> p; (p = it.advance()) != null; ) {
                final V val = p.val;
                final Object v = m.get(p.key);
                if (v == null || (v != val && !v.equals(val)))
                    return false;
            }
            for (final Map.Entry<?, ?> e : m.entrySet()) {
                final Object mk;
                final Object mv;
                final Object v;
                if ((mk = e.getKey()) == null ||
                        (mv = e.getValue()) == null ||
                        (v = get(mk)) == null ||
                        (mv != v && !mv.equals(v)))
                    return false;
            }
        }
        return true;
    }

    /**
     * Stripped-down version of helper class used in previous version,
     * declared for the sake of serialization compatibility
     */
    static class Segment<K, V> extends ReentrantLock implements Serializable {
        private static final long serialVersionUID = 2249069246763182397L;
        final float loadFactor;

        Segment(final float lf) {
            this.loadFactor = lf;
        }
    }

    /**
     * Saves the state of the {@code ConcurrentHashMapV8} instance to a
     * stream (i.e., serializes it).
     *
     * @param s the stream
     * @throws java.io.IOException if an I/O error occurs
     * @serialData the key (Object) and value (Object)
     * for each key-value mapping, followed by a null pair.
     * The key-value mappings are emitted in no particular order.
     */
    private void writeObject(final java.io.ObjectOutputStream s)
            throws java.io.IOException {
        // For serialization compatibility
        // Emulate segment calculation from previous version of this class
        int sshift = 0;
        int ssize = 1;
        while (ssize < DEFAULT_CONCURRENCY_LEVEL) {
            ++sshift;
            ssize <<= 1;
        }
        final int segmentShift = 32 - sshift;
        final int segmentMask = ssize - 1;
        @SuppressWarnings("unchecked") Segment<K, V>[] segments = (Segment<K, V>[])
                new Segment<?, ?>[DEFAULT_CONCURRENCY_LEVEL];
        for (int i = 0; i < segments.length; ++i)
            segments[i] = new Segment<K, V>(LOAD_FACTOR);
        s.putFields().put("segments", segments);
        s.putFields().put("segmentShift", segmentShift);
        s.putFields().put("segmentMask", segmentMask);
        s.writeFields();

        final Node<K, V>[] t;
        if ((t = table) != null) {
            final Traverser<K, V> it = new Traverser<K, V>(t, t.length, 0, t.length);
            for (Node<K, V> p; (p = it.advance()) != null; ) {
                s.writeObject(p.key);
                s.writeObject(p.val);
            }
        }
        s.writeObject(null);
        s.writeObject(null);
        segments = null; // throw away
    }

    /**
     * Reconstitutes the instance from a stream (that is, deserializes it).
     *
     * @param s the stream
     * @throws ClassNotFoundException if the class of a serialized object
     *                                could not be found
     * @throws java.io.IOException    if an I/O error occurs
     */
    private void readObject(final java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        /*
         * To improve performance in typical cases, we create nodes
         * while reading, then place in table once size is known.
         * However, we must also validate uniqueness and deal with
         * overpopulated bins while doing so, which requires
         * specialized versions of putVal mechanics.
         */
        sizeCtl = -1; // force exclusion for table construction
        s.defaultReadObject();
        long size = 0L;
        Node<K, V> p = null;
        for (; ; ) {
            @SuppressWarnings("unchecked") final K k = (K) s.readObject();
            @SuppressWarnings("unchecked") final V v = (V) s.readObject();
            if (k != null && v != null) {
                p = new Node<K, V>(spread(k.hashCode()), k, v, p);
                ++size;
            } else
                break;
        }
        if (size == 0L)
            sizeCtl = 0;
        else {
            final int n;
            if (size >= (long) (MAXIMUM_CAPACITY >>> 1))
                n = MAXIMUM_CAPACITY;
            else {
                final int sz = (int) size;
                n = tableSizeFor(sz + (sz >>> 1) + 1);
            }
            @SuppressWarnings({"rawtypes", "unchecked"}) final
            Node<K, V>[] tab = (Node<K, V>[]) new Node[n];
            final int mask = n - 1;
            long added = 0L;
            while (p != null) {
                boolean insertAtFront;
                final Node<K, V> next = p.next;
                final Node<K, V> first;
                final int h = p.hash;
                final int j = h & mask;
                if ((first = tabAt(tab, j)) == null)
                    insertAtFront = true;
                else {
                    final K k = p.key;
                    if (first.hash < 0) {
                        final TreeBin<K, V> t = (TreeBin<K, V>) first;
                        if (t.putTreeVal(h, k, p.val) == null)
                            ++added;
                        insertAtFront = false;
                    } else {
                        int binCount = 0;
                        insertAtFront = true;
                        Node<K, V> q;
                        K qk;
                        for (q = first; q != null; q = q.next) {
                            if (q.hash == h &&
                                    ((qk = q.key) == k ||
                                            (qk != null && k.equals(qk)))) {
                                insertAtFront = false;
                                break;
                            }
                            ++binCount;
                        }
                        if (insertAtFront && binCount >= TREEIFY_THRESHOLD) {
                            insertAtFront = false;
                            ++added;
                            p.next = first;
                            TreeNode<K, V> hd = null, tl = null;
                            for (q = p; q != null; q = q.next) {
                                final TreeNode<K, V> t = new TreeNode<K, V>
                                        (q.hash, q.key, q.val, null, null);
                                if ((t.prev = tl) == null)
                                    hd = t;
                                else
                                    tl.next = t;
                                tl = t;
                            }
                            setTabAt(tab, j, new TreeBin<K, V>(hd));
                        }
                    }
                }
                if (insertAtFront) {
                    ++added;
                    p.next = first;
                    setTabAt(tab, j, p);
                }
                p = next;
            }
            table = tab;
            sizeCtl = n - (n >>> 2);
            baseCount = added;
        }
    }

    // ConcurrentMap methods

    /**
     * {@inheritDoc}
     *
     * @return the previous value associated with the specified key,
     *         or {@code null} if there was no mapping for the key
     * @throws NullPointerException if the specified key or value is null
     */
    public V putIfAbsent(final K key, final V value) {
        return putVal(key, value, true);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the specified key is null
     */
    public boolean remove(final Object key, final Object value) {
        if (key == null)
            throw new NullPointerException();
        return value != null && replaceNode(key, null, value) != null;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if any of the arguments are null
     */
    public boolean replace(final K key, final V oldValue, final V newValue) {
        if (key == null || oldValue == null || newValue == null)
            throw new NullPointerException();
        return replaceNode(key, newValue, oldValue) != null;
    }

    /**
     * {@inheritDoc}
     *
     * @return the previous value associated with the specified key,
     *         or {@code null} if there was no mapping for the key
     * @throws NullPointerException if the specified key or value is null
     */
    public V replace(final K key, final V value) {
        if (key == null || value == null)
            throw new NullPointerException();
        return replaceNode(key, value, null);
    }

    // Overrides of JDK8+ Map extension method defaults

    /**
     * Returns the value to which the specified key is mapped, or the
     * given default value if this map contains no mapping for the
     * key.
     *
     * @param key          the key whose associated value is to be returned
     * @param defaultValue the value to return if this map contains
     *                     no mapping for the given key
     * @return the mapping for the key, if present; else the default value
     * @throws NullPointerException if the specified key is null
     */
    public V getOrDefault(final Object key, final V defaultValue) {
        final V v;
        return (v = get(key)) == null ? defaultValue : v;
    }


    // Hashtable legacy methods

    /**
     * Legacy method testing if some key maps into the specified value
     * in this table.  This method is identical in functionality to
     * {@link #containsValue(Object)}, and exists solely to ensure
     * full compatibility with class {@link java.util.Hashtable},
     * which supported this method prior to introduction of the
     * Java Collections framework.
     *
     * @param value a value to search for
     * @return {@code true} if and only if some key maps to the
     *         {@code value} argument in this table as
     *         determined by the {@code equals} method;
     *         {@code false} otherwise
     * @throws NullPointerException if the specified value is null
     */
    @Deprecated
    public boolean contains(final Object value) {
        return containsValue(value);
    }

    /**
     * Returns an enumeration of the keys in this table.
     *
     * @return an enumeration of the keys in this table
     * @see #keySet()
     */
    public Enumeration<K> keys() {
        final Node<K, V>[] t;
        final int f = (t = table) == null ? 0 : t.length;
        return new KeyIterator<K, V>(t, f, 0, f, this);
    }

    /**
     * Returns an enumeration of the values in this table.
     *
     * @return an enumeration of the values in this table
     * @see #values()
     */
    public Enumeration<V> elements() {
        final Node<K, V>[] t;
        final int f = (t = table) == null ? 0 : t.length;
        return new ValueIterator<K, V>(t, f, 0, f, this);
    }

    // ConcurrentHashMapV8-only methods

    /**
     * Returns the number of mappings. This method should be used
     * instead of {@link #size} because a ConcurrentHashMapV8 may
     * contain more mappings than can be represented as an int. The
     * value returned is an estimate; the actual count may differ if
     * there are concurrent insertions or removals.
     *
     * @return the number of mappings
     * @since 1.8
     */
    public long mappingCount() {
        final long n = sumCount();
        return (n < 0L) ? 0L : n; // ignore transient negative values
    }

    /**
     * Creates a new {@link Set} backed by a ConcurrentHashMapV8
     * from the given type to {@code Boolean.TRUE}.
     *
     * @return the new set
     * @since 1.8
     */
    public static <K> KeySetView<K, Boolean> newKeySet() {
        return new KeySetView<K, Boolean>
                (new ConcurrentHashMapV8<K, Boolean>(), Boolean.TRUE);
    }

    /**
     * Creates a new {@link Set} backed by a ConcurrentHashMapV8
     * from the given type to {@code Boolean.TRUE}.
     *
     * @param initialCapacity The implementation performs internal
     *                        sizing to accommodate this many elements.
     * @return the new set
     * @throws IllegalArgumentException if the initial capacity of
     *                                  elements is negative
     * @since 1.8
     */
    public static <K> KeySetView<K, Boolean> newKeySet(final int initialCapacity) {
        return new KeySetView<K, Boolean>
                (new ConcurrentHashMapV8<K, Boolean>(initialCapacity), Boolean.TRUE);
    }

    /**
     * Returns a {@link Set} view of the keys in this map, using the
     * given common mapped value for any additions (i.e., {@link
     * Collection#add} and {@link Collection#addAll(Collection)}).
     * This is of course only appropriate if it is acceptable to use
     * the same value for all additions from this view.
     *
     * @param mappedValue the mapped value to use for any additions
     * @return the set view
     * @throws NullPointerException if the mappedValue is null
     */
    public KeySetView<K, V> keySet(final V mappedValue) {
        if (mappedValue == null)
            throw new NullPointerException();
        return new KeySetView<K, V>(this, mappedValue);
    }

    /* ---------------- Special Nodes -------------- */

    /**
     * A node inserted at head of bins during transfer operations.
     */
    static final class ForwardingNode<K, V> extends Node<K, V> {
        final Node<K, V>[] nextTable;

        ForwardingNode(final Node<K, V>[] tab) {
            super(MOVED, null, null, null);
            this.nextTable = tab;
        }

        Node<K, V> find(final int h, final Object k) {
            // loop to avoid arbitrarily deep recursion on forwarding nodes
            outer:
            for (Node<K, V>[] tab = nextTable; ; ) {
                Node<K, V> e;
                final int n;
                if (k == null || tab == null || (n = tab.length) == 0 ||
                        (e = tabAt(tab, (n - 1) & h)) == null)
                    return null;
                for (; ; ) {
                    final int eh;
                    final K ek;
                    if ((eh = e.hash) == h &&
                            ((ek = e.key) == k || (ek != null && k.equals(ek))))
                        return e;
                    if (eh < 0) {
                        if (e instanceof ForwardingNode) {
                            tab = ((ForwardingNode<K, V>) e).nextTable;
                            continue outer;
                        } else
                            return e.find(h, k);
                    }
                    if ((e = e.next) == null)
                        return null;
                }
            }
        }
    }

    /**
     * A place-holder node used in computeIfAbsent and compute
     */
    static final class ReservationNode<K, V> extends Node<K, V> {
        ReservationNode() {
            super(RESERVED, null, null, null);
        }

        Node<K, V> find(final int h, final Object k) {
            return null;
        }
    }

    /* ---------------- Table Initialization and Resizing -------------- */

    /**
     * Initializes table, using the size recorded in sizeCtl.
     */
    private final Node<K, V>[] initTable() {
        Node<K, V>[] tab;
        int sc;
        while ((tab = table) == null || tab.length == 0) {
            if ((sc = sizeCtl) < 0)
                Thread.yield(); // lost initialization race; just spin
            else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                try {
                    if ((tab = table) == null || tab.length == 0) {
                        final int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                        @SuppressWarnings({"rawtypes", "unchecked"}) final
                        Node<K, V>[] nt = (Node<K, V>[]) new Node[n];
                        table = tab = nt;
                        sc = n - (n >>> 2);
                    }
                } finally {
                    sizeCtl = sc;
                }
                break;
            }
        }
        return tab;
    }

    /**
     * Adds to count, and if table is too small and not already
     * resizing, initiates transfer. If already resizing, helps
     * perform transfer if work is available.  Rechecks occupancy
     * after a transfer to see if another resize is already needed
     * because resizings are lagging additions.
     *
     * @param x     the count to add
     * @param check if <0, don't check resize, if <= 1 only check if uncontended
     */
    private final void addCount(final long x, final int check) {
        final CounterCell[] as;
        final long b;
        long s;
        if ((as = counterCells) != null ||
                !U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) {
            final CounterHashCode hc;
            final CounterCell a;
            final long v;
            final int m;
            boolean uncontended = true;
            if ((hc = threadCounterHashCode.get()) == null ||
                    as == null || (m = as.length - 1) < 0 ||
                    (a = as[m & hc.code]) == null ||
                    !(uncontended =
                            U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
                fullAddCount(x, hc, uncontended);
                return;
            }
            if (check <= 1)
                return;
            s = sumCount();
        }
        if (check >= 0) {
            Node<K, V>[] tab, nt;
            int sc;
            while (s >= (long) (sc = sizeCtl) && (tab = table) != null &&
                    tab.length < MAXIMUM_CAPACITY) {
                if (sc < 0) {
                    if (sc == -1 || transferIndex <= transferOrigin ||
                            (nt = nextTable) == null)
                        break;
                    if (U.compareAndSwapInt(this, SIZECTL, sc, sc - 1))
                        transfer(tab, nt);
                } else if (U.compareAndSwapInt(this, SIZECTL, sc, -2))
                    transfer(tab, null);
                s = sumCount();
            }
        }
    }

    /**
     * Helps transfer if a resize is in progress.
     */
    final Node<K, V>[] helpTransfer(final Node<K, V>[] tab, final Node<K, V> f) {
        final Node<K, V>[] nextTab;
        final int sc;
        if ((f instanceof ForwardingNode) &&
                (nextTab = ((ForwardingNode<K, V>) f).nextTable) != null) {
            if (nextTab == nextTable && tab == table &&
                    transferIndex > transferOrigin && (sc = sizeCtl) < -1 &&
                    U.compareAndSwapInt(this, SIZECTL, sc, sc - 1))
                transfer(tab, nextTab);
            return nextTab;
        }
        return table;
    }

    /**
     * Tries to presize table to accommodate the given number of elements.
     *
     * @param size number of elements (doesn't need to be perfectly accurate)
     */
    private final void tryPresize(final int size) {
        final int c = (size >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY :
                tableSizeFor(size + (size >>> 1) + 1);
        int sc;
        while ((sc = sizeCtl) >= 0) {
            final Node<K, V>[] tab = table;
            int n;
            if (tab == null || (n = tab.length) == 0) {
                n = (sc > c) ? sc : c;
                if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                    try {
                        if (table == tab) {
                            @SuppressWarnings({"rawtypes", "unchecked"}) final
                            Node<K, V>[] nt = (Node<K, V>[]) new Node[n];
                            table = nt;
                            sc = n - (n >>> 2);
                        }
                    } finally {
                        sizeCtl = sc;
                    }
                }
            } else if (c <= sc || n >= MAXIMUM_CAPACITY)
                break;
            else if (tab == table &&
                    U.compareAndSwapInt(this, SIZECTL, sc, -2))
                transfer(tab, null);
        }
    }

    /**
     * Moves and/or copies the nodes in each bin to new table. See
     * above for explanation.
     */
    private final void transfer(final Node<K, V>[] tab, Node<K, V>[] nextTab) {
        final int n = tab.length;
        int stride;
        if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
            stride = MIN_TRANSFER_STRIDE; // subdivide range
        if (nextTab == null) {            // initiating
            try {
                @SuppressWarnings({"rawtypes", "unchecked"}) final
                Node<K, V>[] nt = (Node<K, V>[]) new Node[n << 1];
                nextTab = nt;
            } catch (final Throwable ex) {      // try to cope with OOME
                sizeCtl = Integer.MAX_VALUE;
                return;
            }
            nextTable = nextTab;
            transferOrigin = n;
            transferIndex = n;
            final ForwardingNode<K, V> rev = new ForwardingNode<K, V>(tab);
            for (int k = n; k > 0; ) {    // progressively reveal ready slots
                final int nextk = (k > stride) ? k - stride : 0;
                for (int m = nextk; m < k; ++m)
                    nextTab[m] = rev;
                for (int m = n + nextk; m < n + k; ++m)
                    nextTab[m] = rev;
                U.putOrderedInt(this, TRANSFERORIGIN, k = nextk);
            }
        }
        final int nextn = nextTab.length;
        final ForwardingNode<K, V> fwd = new ForwardingNode<K, V>(nextTab);
        boolean advance = true;
        boolean finishing = false; // to ensure sweep before committing nextTab
        for (int i = 0, bound = 0; ; ) {
            int nextIndex;
            int nextBound;
            final int fh;
            final Node<K, V> f;
            while (advance) {
                if (--i >= bound || finishing)
                    advance = false;
                else if ((nextIndex = transferIndex) <= transferOrigin) {
                    i = -1;
                    advance = false;
                } else if (U.compareAndSwapInt
                        (this, TRANSFERINDEX, nextIndex,
                                nextBound = (nextIndex > stride ?
                                        nextIndex - stride : 0))) {
                    bound = nextBound;
                    i = nextIndex - 1;
                    advance = false;
                }
            }
            if (i < 0 || i >= n || i + n >= nextn) {
                if (finishing) {
                    nextTable = null;
                    table = nextTab;
                    sizeCtl = (n << 1) - (n >>> 1);
                    return;
                }
                for (int sc; ; ) {
                    if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, ++sc)) {
                        if (sc != -1)
                            return;
                        finishing = advance = true;
                        i = n; // recheck before commit
                        break;
                    }
                }
            } else if ((f = tabAt(tab, i)) == null) {
                if (casTabAt(tab, i, null, fwd)) {
                    setTabAt(nextTab, i, null);
                    setTabAt(nextTab, i + n, null);
                    advance = true;
                }
            } else if ((fh = f.hash) == MOVED)
                advance = true; // already processed
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        Node<K, V> ln, hn;
                        if (fh >= 0) {
                            int runBit = fh & n;
                            Node<K, V> lastRun = f;
                            for (Node<K, V> p = f.next; p != null; p = p.next) {
                                final int b = p.hash & n;
                                if (b != runBit) {
                                    runBit = b;
                                    lastRun = p;
                                }
                            }
                            if (runBit == 0) {
                                ln = lastRun;
                                hn = null;
                            } else {
                                hn = lastRun;
                                ln = null;
                            }
                            for (Node<K, V> p = f; p != lastRun; p = p.next) {
                                final int ph = p.hash;
                                final K pk = p.key;
                                final V pv = p.val;
                                if ((ph & n) == 0)
                                    ln = new Node<K, V>(ph, pk, pv, ln);
                                else
                                    hn = new Node<K, V>(ph, pk, pv, hn);
                            }
                            setTabAt(nextTab, i, ln);
                            setTabAt(nextTab, i + n, hn);
                            setTabAt(tab, i, fwd);
                            advance = true;
                        } else if (f instanceof TreeBin) {
                            final TreeBin<K, V> t = (TreeBin<K, V>) f;
                            TreeNode<K, V> lo = null, loTail = null;
                            TreeNode<K, V> hi = null, hiTail = null;
                            int lc = 0, hc = 0;
                            for (Node<K, V> e = t.first; e != null; e = e.next) {
                                final int h = e.hash;
                                final TreeNode<K, V> p = new TreeNode<K, V>
                                        (h, e.key, e.val, null, null);
                                if ((h & n) == 0) {
                                    if ((p.prev = loTail) == null)
                                        lo = p;
                                    else
                                        loTail.next = p;
                                    loTail = p;
                                    ++lc;
                                } else {
                                    if ((p.prev = hiTail) == null)
                                        hi = p;
                                    else
                                        hiTail.next = p;
                                    hiTail = p;
                                    ++hc;
                                }
                            }
                            ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) :
                                    (hc != 0) ? new TreeBin<K, V>(lo) : t;
                            hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) :
                                    (lc != 0) ? new TreeBin<K, V>(hi) : t;
                            setTabAt(nextTab, i, ln);
                            setTabAt(nextTab, i + n, hn);
                            setTabAt(tab, i, fwd);
                            advance = true;
                        }
                    }
                }
            }
        }
    }

    /* ---------------- Conversion from/to TreeBins -------------- */

    /**
     * Replaces all linked nodes in bin at given index unless table is
     * too small, in which case resizes instead.
     */
    private final void treeifyBin(final Node<K, V>[] tab, final int index) {
        final Node<K, V> b;
        final int n;
        final int sc;
        if (tab != null) {
            if ((n = tab.length) < MIN_TREEIFY_CAPACITY) {
                if (tab == table && (sc = sizeCtl) >= 0 &&
                        U.compareAndSwapInt(this, SIZECTL, sc, -2))
                    transfer(tab, null);
            } else if ((b = tabAt(tab, index)) != null && b.hash >= 0) {
                synchronized (b) {
                    if (tabAt(tab, index) == b) {
                        TreeNode<K, V> hd = null, tl = null;
                        for (Node<K, V> e = b; e != null; e = e.next) {
                            final TreeNode<K, V> p =
                                    new TreeNode<K, V>(e.hash, e.key, e.val,
                                            null, null);
                            if ((p.prev = tl) == null)
                                hd = p;
                            else
                                tl.next = p;
                            tl = p;
                        }
                        setTabAt(tab, index, new TreeBin<K, V>(hd));
                    }
                }
            }
        }
    }

    /**
     * Returns a list on non-TreeNodes replacing those in given list.
     */
    static <K, V> Node<K, V> untreeify(final Node<K, V> b) {
        Node<K, V> hd = null, tl = null;
        for (Node<K, V> q = b; q != null; q = q.next) {
            final Node<K, V> p = new Node<K, V>(q.hash, q.key, q.val, null);
            if (tl == null)
                hd = p;
            else
                tl.next = p;
            tl = p;
        }
        return hd;
    }

    /* ---------------- TreeNodes -------------- */

    /**
     * Nodes for use in TreeBins
     */
    static final class TreeNode<K, V> extends Node<K, V> {
        TreeNode<K, V> parent;  // red-black tree links
        TreeNode<K, V> left;
        TreeNode<K, V> right;
        TreeNode<K, V> prev;    // needed to unlink next upon deletion
        boolean red;

        TreeNode(final int hash, final K key, final V val, final Node<K, V> next,
                 final TreeNode<K, V> parent) {
            super(hash, key, val, next);
            this.parent = parent;
        }

        Node<K, V> find(final int h, final Object k) {
            return findTreeNode(h, k, null);
        }

        /**
         * Returns the TreeNode (or null if not found) for the given key
         * starting at given root.
         */
        final TreeNode<K, V> findTreeNode(final int h, final Object k, Class<?> kc) {
            if (k != null) {
                TreeNode<K, V> p = this;
                do {
                    final int ph;
                    final int dir;
                    final K pk;
                    final TreeNode<K, V> q;
                    final TreeNode<K, V> pl = p.left;
                    final TreeNode<K, V> pr = p.right;
                    if ((ph = p.hash) > h)
                        p = pl;
                    else if (ph < h)
                        p = pr;
                    else if ((pk = p.key) == k || (pk != null && k.equals(pk)))
                        return p;
                    else if (pl == null)
                        p = pr;
                    else if (pr == null)
                        p = pl;
                    else if ((kc != null ||
                            (kc = comparableClassFor(k)) != null) &&
                            (dir = compareComparables(kc, k, pk)) != 0)
                        p = (dir < 0) ? pl : pr;
                    else if ((q = pr.findTreeNode(h, k, kc)) != null)
                        return q;
                    else
                        p = pl;
                } while (p != null);
            }
            return null;
        }
    }

    /* ---------------- TreeBins -------------- */

    /**
     * TreeNodes used at the heads of bins. TreeBins do not hold user
     * keys or values, but instead point to list of TreeNodes and
     * their root. They also maintain a parasitic read-write lock
     * forcing writers (who hold bin lock) to wait for readers (who do
     * not) to complete before tree restructuring operations.
     */
    static final class TreeBin<K, V> extends Node<K, V> {
        TreeNode<K, V> root;
        volatile TreeNode<K, V> first;
        volatile Thread waiter;
        volatile int lockState;
        // values for lockState
        static final int WRITER = 1; // set while holding write lock
        static final int WAITER = 2; // set when waiting for write lock
        static final int READER = 4; // increment value for setting read lock

        /**
         * Tie-breaking utility for ordering insertions when equal
         * hashCodes and non-comparable. We don't require a total
         * order, just a consistent insertion rule to maintain
         * equivalence across rebalancings. Tie-breaking further than
         * necessary simplifies testing a bit.
         */
        static int tieBreakOrder(final Object a, final Object b) {
            int d;
            if (a == null || b == null ||
                    (d = a.getClass().getName().
                            compareTo(b.getClass().getName())) == 0)
                d = (System.identityHashCode(a) <= System.identityHashCode(b) ?
                        -1 : 1);
            return d;
        }

        /**
         * Creates bin with initial set of nodes headed by b.
         */
        TreeBin(final TreeNode<K, V> b) {
            super(TREEBIN, null, null, null);
            this.first = b;
            TreeNode<K, V> r = null;
            for (TreeNode<K, V> x = b, next; x != null; x = next) {
                next = (TreeNode<K, V>) x.next;
                x.left = x.right = null;
                if (r == null) {
                    x.parent = null;
                    x.red = false;
                    r = x;
                } else {
                    final K k = x.key;
                    final int h = x.hash;
                    Class<?> kc = null;
                    for (TreeNode<K, V> p = r; ; ) {
                        int dir;
                        final int ph;
                        final K pk = p.key;
                        if ((ph = p.hash) > h)
                            dir = -1;
                        else if (ph < h)
                            dir = 1;
                        else if ((kc == null &&
                                (kc = comparableClassFor(k)) == null) ||
                                (dir = compareComparables(kc, k, pk)) == 0)
                            dir = tieBreakOrder(k, pk);
                        final TreeNode<K, V> xp = p;
                        if ((p = (dir <= 0) ? p.left : p.right) == null) {
                            x.parent = xp;
                            if (dir <= 0)
                                xp.left = x;
                            else
                                xp.right = x;
                            r = balanceInsertion(r, x);
                            break;
                        }
                    }
                }
            }
            this.root = r;
            assert checkInvariants(root);
        }

        /**
         * Acquires write lock for tree restructuring.
         */
        private final void lockRoot() {
            if (!U.compareAndSwapInt(this, LOCKSTATE, 0, WRITER))
                contendedLock(); // offload to separate method
        }

        /**
         * Releases write lock for tree restructuring.
         */
        private final void unlockRoot() {
            lockState = 0;
        }

        /**
         * Possibly blocks awaiting root lock.
         */
        private final void contendedLock() {
            boolean waiting = false;
            for (int s; ; ) {
                if (((s = lockState) & WRITER) == 0) {
                    if (U.compareAndSwapInt(this, LOCKSTATE, s, WRITER)) {
                        if (waiting)
                            waiter = null;
                        return;
                    }
                } else if ((s & WAITER) == 0) {
                    if (U.compareAndSwapInt(this, LOCKSTATE, s, s | WAITER)) {
                        waiting = true;
                        waiter = Thread.currentThread();
                    }
                } else if (waiting)
                    LockSupport.park(this);
            }
        }

        /**
         * Returns matching node or null if none. Tries to search
         * using tree comparisons from root, but continues linear
         * search when lock not available.
         */
        final Node<K, V> find(final int h, final Object k) {
            if (k != null) {
                for (Node<K, V> e = first; e != null; e = e.next) {
                    final int s;
                    final K ek;
                    if (((s = lockState) & (WAITER | WRITER)) != 0) {
                        if (e.hash == h &&
                                ((ek = e.key) == k || (ek != null && k.equals(ek))))
                            return e;
                    } else if (U.compareAndSwapInt(this, LOCKSTATE, s,
                            s + READER)) {
                        final TreeNode<K, V> r;
                        TreeNode<K, V> p;
                        try {
                            p = ((r = root) == null ? null :
                                    r.findTreeNode(h, k, null));
                        } finally {
                            final Thread w;
                            int ls;
                            do {
                            } while (!U.compareAndSwapInt
                                    (this, LOCKSTATE,
                                            ls = lockState, ls - READER));
                            if (ls == (READER | WAITER) && (w = waiter) != null)
                                LockSupport.unpark(w);
                        }
                        return p;
                    }
                }
            }
            return null;
        }

        /**
         * Finds or adds a node.
         *
         * @return null if added
         */
        final TreeNode<K, V> putTreeVal(final int h, final K k, final V v) {
            Class<?> kc = null;
            boolean searched = false;
            for (TreeNode<K, V> p = root; ; ) {
                int dir;
                final int ph;
                final K pk;
                if (p == null) {
                    first = root = new TreeNode<K, V>(h, k, v, null, null);
                    break;
                } else if ((ph = p.hash) > h)
                    dir = -1;
                else if (ph < h)
                    dir = 1;
                else if ((pk = p.key) == k || (pk != null && k.equals(pk)))
                    return p;
                else if ((kc == null &&
                        (kc = comparableClassFor(k)) == null) ||
                        (dir = compareComparables(kc, k, pk)) == 0) {
                    if (!searched) {
                        TreeNode<K, V> q, ch;
                        searched = true;
                        if (((ch = p.left) != null &&
                                (q = ch.findTreeNode(h, k, kc)) != null) ||
                                ((ch = p.right) != null &&
                                        (q = ch.findTreeNode(h, k, kc)) != null))
                            return q;
                    }
                    dir = tieBreakOrder(k, pk);
                }

                final TreeNode<K, V> xp = p;
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    final TreeNode<K, V> x;
                    final TreeNode<K, V> f = first;
                    first = x = new TreeNode<K, V>(h, k, v, f, xp);
                    if (f != null)
                        f.prev = x;
                    if (dir <= 0)
                        xp.left = x;
                    else
                        xp.right = x;
                    if (!xp.red)
                        x.red = true;
                    else {
                        lockRoot();
                        try {
                            root = balanceInsertion(root, x);
                        } finally {
                            unlockRoot();
                        }
                    }
                    break;
                }
            }
            assert checkInvariants(root);
            return null;
        }

        /**
         * Removes the given node, that must be present before this
         * call.  This is messier than typical red-black deletion code
         * because we cannot swap the contents of an interior node
         * with a leaf successor that is pinned by "next" pointers
         * that are accessible independently of lock. So instead we
         * swap the tree linkages.
         *
         * @return true if now too small, so should be untreeified
         */
        final boolean removeTreeNode(final TreeNode<K, V> p) {
            final TreeNode<K, V> next = (TreeNode<K, V>) p.next;
            final TreeNode<K, V> pred = p.prev;  // unlink traversal pointers
            TreeNode<K, V> r;
            final TreeNode<K, V> rl;
            if (pred == null)
                first = next;
            else
                pred.next = next;
            if (next != null)
                next.prev = pred;
            if (first == null) {
                root = null;
                return true;
            }
            if ((r = root) == null || r.right == null || // too small
                    (rl = r.left) == null || rl.left == null)
                return true;
            lockRoot();
            try {
                final TreeNode<K, V> replacement;
                final TreeNode<K, V> pl = p.left;
                final TreeNode<K, V> pr = p.right;
                if (pl != null && pr != null) {
                    TreeNode<K, V> s = pr, sl;
                    while ((sl = s.left) != null) // find successor
                        s = sl;
                    final boolean c = s.red;
                    s.red = p.red;
                    p.red = c; // swap colors
                    final TreeNode<K, V> sr = s.right;
                    final TreeNode<K, V> pp = p.parent;
                    if (s == pr) { // p was s's direct parent
                        p.parent = s;
                        s.right = p;
                    } else {
                        final TreeNode<K, V> sp = s.parent;
                        if ((p.parent = sp) != null) {
                            if (s == sp.left)
                                sp.left = p;
                            else
                                sp.right = p;
                        }
                        if ((s.right = pr) != null)
                            pr.parent = s;
                    }
                    p.left = null;
                    if ((p.right = sr) != null)
                        sr.parent = p;
                    if ((s.left = pl) != null)
                        pl.parent = s;
                    if ((s.parent = pp) == null)
                        r = s;
                    else if (p == pp.left)
                        pp.left = s;
                    else
                        pp.right = s;
                    if (sr != null)
                        replacement = sr;
                    else
                        replacement = p;
                } else if (pl != null)
                    replacement = pl;
                else if (pr != null)
                    replacement = pr;
                else
                    replacement = p;
                if (replacement != p) {
                    final TreeNode<K, V> pp = replacement.parent = p.parent;
                    if (pp == null)
                        r = replacement;
                    else if (p == pp.left)
                        pp.left = replacement;
                    else
                        pp.right = replacement;
                    p.left = p.right = p.parent = null;
                }

                root = (p.red) ? r : balanceDeletion(r, replacement);

                if (p == replacement) {  // detach pointers
                    final TreeNode<K, V> pp;
                    if ((pp = p.parent) != null) {
                        if (p == pp.left)
                            pp.left = null;
                        else if (p == pp.right)
                            pp.right = null;
                        p.parent = null;
                    }
                }
            } finally {
                unlockRoot();
            }
            assert checkInvariants(root);
            return false;
        }

        /* ------------------------------------------------------------ */
        // Red-black tree methods, all adapted from CLR

        static <K, V> TreeNode<K, V> rotateLeft(TreeNode<K, V> root,
                                                final TreeNode<K, V> p) {
            final TreeNode<K, V> r;
            final TreeNode<K, V> pp;
            final TreeNode<K, V> rl;
            if (p != null && (r = p.right) != null) {
                if ((rl = p.right = r.left) != null)
                    rl.parent = p;
                if ((pp = r.parent = p.parent) == null)
                    (root = r).red = false;
                else if (pp.left == p)
                    pp.left = r;
                else
                    pp.right = r;
                r.left = p;
                p.parent = r;
            }
            return root;
        }

        static <K, V> TreeNode<K, V> rotateRight(TreeNode<K, V> root,
                                                 final TreeNode<K, V> p) {
            final TreeNode<K, V> l;
            final TreeNode<K, V> pp;
            final TreeNode<K, V> lr;
            if (p != null && (l = p.left) != null) {
                if ((lr = p.left = l.right) != null)
                    lr.parent = p;
                if ((pp = l.parent = p.parent) == null)
                    (root = l).red = false;
                else if (pp.right == p)
                    pp.right = l;
                else
                    pp.left = l;
                l.right = p;
                p.parent = l;
            }
            return root;
        }

        static <K, V> TreeNode<K, V> balanceInsertion(TreeNode<K, V> root,
                                                      TreeNode<K, V> x) {
            x.red = true;
            for (TreeNode<K, V> xp, xpp, xppl, xppr; ; ) {
                if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                } else if (!xp.red || (xpp = xp.parent) == null)
                    return root;
                if (xp == (xppl = xpp.left)) {
                    if ((xppr = xpp.right) != null && xppr.red) {
                        xppr.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    } else {
                        if (x == xp.right) {
                            root = rotateLeft(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateRight(root, xpp);
                            }
                        }
                    }
                } else {
                    if (xppl != null && xppl.red) {
                        xppl.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    } else {
                        if (x == xp.left) {
                            root = rotateRight(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateLeft(root, xpp);
                            }
                        }
                    }
                }
            }
        }

        static <K, V> TreeNode<K, V> balanceDeletion(TreeNode<K, V> root,
                                                     TreeNode<K, V> x) {
            for (TreeNode<K, V> xp, xpl, xpr; ; ) {
                if (x == null || x == root)
                    return root;
                else if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                } else if (x.red) {
                    x.red = false;
                    return root;
                } else if ((xpl = xp.left) == x) {
                    if ((xpr = xp.right) != null && xpr.red) {
                        xpr.red = false;
                        xp.red = true;
                        root = rotateLeft(root, xp);
                        xpr = (xp = x.parent) == null ? null : xp.right;
                    }
                    if (xpr == null)
                        x = xp;
                    else {
                        final TreeNode<K, V> sl = xpr.left;
                        TreeNode<K, V> sr = xpr.right;
                        if ((sr == null || !sr.red) &&
                                (sl == null || !sl.red)) {
                            xpr.red = true;
                            x = xp;
                        } else {
                            if (sr == null || !sr.red) {
                                if (sl != null)
                                    sl.red = false;
                                xpr.red = true;
                                root = rotateRight(root, xpr);
                                xpr = (xp = x.parent) == null ?
                                        null : xp.right;
                            }
                            if (xpr != null) {
                                xpr.red = (xp == null) ? false : xp.red;
                                if ((sr = xpr.right) != null)
                                    sr.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateLeft(root, xp);
                            }
                            x = root;
                        }
                    }
                } else { // symmetric
                    if (xpl != null && xpl.red) {
                        xpl.red = false;
                        xp.red = true;
                        root = rotateRight(root, xp);
                        xpl = (xp = x.parent) == null ? null : xp.left;
                    }
                    if (xpl == null)
                        x = xp;
                    else {
                        TreeNode<K, V> sl = xpl.left;
                        final TreeNode<K, V> sr = xpl.right;
                        if ((sl == null || !sl.red) &&
                                (sr == null || !sr.red)) {
                            xpl.red = true;
                            x = xp;
                        } else {
                            if (sl == null || !sl.red) {
                                if (sr != null)
                                    sr.red = false;
                                xpl.red = true;
                                root = rotateLeft(root, xpl);
                                xpl = (xp = x.parent) == null ?
                                        null : xp.left;
                            }
                            if (xpl != null) {
                                xpl.red = (xp == null) ? false : xp.red;
                                if ((sl = xpl.left) != null)
                                    sl.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateRight(root, xp);
                            }
                            x = root;
                        }
                    }
                }
            }
        }

        /**
         * Recursive invariant check
         */
        static <K, V> boolean checkInvariants(final TreeNode<K, V> t) {
            final TreeNode<K, V> tp = t.parent;
            final TreeNode<K, V> tl = t.left;
            final TreeNode<K, V> tr = t.right;
            final TreeNode<K, V> tb = t.prev;
            final TreeNode<K, V> tn = (TreeNode<K, V>) t.next;
            if (tb != null && tb.next != t)
                return false;
            if (tn != null && tn.prev != t)
                return false;
            if (tp != null && t != tp.left && t != tp.right)
                return false;
            if (tl != null && (tl.parent != t || tl.hash > t.hash))
                return false;
            if (tr != null && (tr.parent != t || tr.hash < t.hash))
                return false;
            if (t.red && tl != null && tl.red && tr != null && tr.red)
                return false;
            if (tl != null && !checkInvariants(tl))
                return false;
            if (tr != null && !checkInvariants(tr))
                return false;
            return true;
        }

        private static final sun.misc.Unsafe U;
        private static final long LOCKSTATE;

        static {
            try {
                U = getUnsafe();
                final Class<?> k = TreeBin.class;
                LOCKSTATE = U.objectFieldOffset
                        (k.getDeclaredField("lockState"));
            } catch (final Exception e) {
                throw new Error(e);
            }
        }
    }

    /* ----------------Table Traversal -------------- */

    /**
     * Encapsulates traversal for methods such as containsValue; also
     * serves as a base class for other iterators and spliterators.
     *
     * Method advance visits once each still-valid node that was
     * reachable upon iterator construction. It might miss some that
     * were added to a bin after the bin was visited, which is OK wrt
     * consistency guarantees. Maintaining this property in the face
     * of possible ongoing resizes requires a fair amount of
     * bookkeeping state that is difficult to optimize away amidst
     * volatile accesses.  Even so, traversal maintains reasonable
     * throughput.
     *
     * Normally, iteration proceeds bin-by-bin traversing lists.
     * However, if the table has been resized, then all future steps
     * must traverse both the bin at the current index as well as at
     * (index + baseSize); and so on for further resizings. To
     * paranoically cope with potential sharing by users of iterators
     * across threads, iteration terminates if a bounds checks fails
     * for a table read.
     */
    static class Traverser<K, V> {
        Node<K, V>[] tab;        // current table; updated if resized
        Node<K, V> next;         // the next entry to use
        int index;              // index of bin to use next
        int baseIndex;          // current index of initial table
        int baseLimit;          // index bound for initial table
        final int baseSize;     // initial table size

        Traverser(final Node<K, V>[] tab, final int size, final int index, final int limit) {
            this.tab = tab;
            this.baseSize = size;
            this.baseIndex = this.index = index;
            this.baseLimit = limit;
            this.next = null;
        }

        /**
         * Advances if possible, returning next valid node, or null if none.
         */
        final Node<K, V> advance() {
            Node<K, V> e;
            if ((e = next) != null)
                e = e.next;
            for (; ; ) {
                final Node<K, V>[] t;
                final int i;
                final int n;
                K ek;  // must use locals in checks
                if (e != null)
                    return next = e;
                if (baseIndex >= baseLimit || (t = tab) == null ||
                        (n = t.length) <= (i = index) || i < 0)
                    return next = null;
                if ((e = tabAt(t, index)) != null && e.hash < 0) {
                    if (e instanceof ForwardingNode) {
                        tab = ((ForwardingNode<K, V>) e).nextTable;
                        e = null;
                        continue;
                    } else if (e instanceof TreeBin)
                        e = ((TreeBin<K, V>) e).first;
                    else
                        e = null;
                }
                if ((index += baseSize) >= n)
                    index = ++baseIndex;    // visit upper slots if present
            }
        }
    }

    /**
     * Base of key, value, and entry Iterators. Adds fields to
     * Traverser to support iterator.remove.
     */
    static class BaseIterator<K, V> extends Traverser<K, V> {
        final ConcurrentHashMapV8<K, V> map;
        Node<K, V> lastReturned;

        BaseIterator(final Node<K, V>[] tab, final int size, final int index, final int limit,
                     final ConcurrentHashMapV8<K, V> map) {
            super(tab, size, index, limit);
            this.map = map;
            advance();
        }

        public final boolean hasNext() {
            return next != null;
        }

        public final boolean hasMoreElements() {
            return next != null;
        }

        public final void remove() {
            final Node<K, V> p;
            if ((p = lastReturned) == null)
                throw new IllegalStateException();
            lastReturned = null;
            map.replaceNode(p.key, null, null);
        }
    }

    static final class KeyIterator<K, V> extends BaseIterator<K, V>
            implements Iterator<K>, Enumeration<K> {
        KeyIterator(final Node<K, V>[] tab, final int index, final int size, final int limit,
                    final ConcurrentHashMapV8<K, V> map) {
            super(tab, index, size, limit, map);
        }

        public final K next() {
            final Node<K, V> p;
            if ((p = next) == null)
                throw new NoSuchElementException();
            final K k = p.key;
            lastReturned = p;
            advance();
            return k;
        }

        public final K nextElement() {
            return next();
        }
    }

    static final class ValueIterator<K, V> extends BaseIterator<K, V>
            implements Iterator<V>, Enumeration<V> {
        ValueIterator(final Node<K, V>[] tab, final int index, final int size, final int limit,
                      final ConcurrentHashMapV8<K, V> map) {
            super(tab, index, size, limit, map);
        }

        public final V next() {
            final Node<K, V> p;
            if ((p = next) == null)
                throw new NoSuchElementException();
            final V v = p.val;
            lastReturned = p;
            advance();
            return v;
        }

        public final V nextElement() {
            return next();
        }
    }

    static final class EntryIterator<K, V> extends BaseIterator<K, V>
            implements Iterator<Map.Entry<K, V>> {
        EntryIterator(final Node<K, V>[] tab, final int index, final int size, final int limit,
                      final ConcurrentHashMapV8<K, V> map) {
            super(tab, index, size, limit, map);
        }

        public final Map.Entry<K, V> next() {
            final Node<K, V> p;
            if ((p = next) == null)
                throw new NoSuchElementException();
            final K k = p.key;
            final V v = p.val;
            lastReturned = p;
            advance();
            return new MapEntry<K, V>(k, v, map);
        }
    }

    /**
     * Exported Entry for EntryIterator
     */
    static final class MapEntry<K, V> implements Map.Entry<K, V> {
        final K key; // non-null
        V val;       // non-null
        final ConcurrentHashMapV8<K, V> map;

        MapEntry(final K key, final V val, final ConcurrentHashMapV8<K, V> map) {
            this.key = key;
            this.val = val;
            this.map = map;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return val;
        }

        public int hashCode() {
            return key.hashCode() ^ val.hashCode();
        }

        public String toString() {
            return key + "=" + val;
        }

        public boolean equals(final Object o) {
            final Object k;
            final Object v;
            final Map.Entry<?, ?> e;
            return ((o instanceof Map.Entry) &&
                    (k = (e = (Map.Entry<?, ?>) o).getKey()) != null &&
                    (v = e.getValue()) != null &&
                    (k == key || k.equals(key)) &&
                    (v == val || v.equals(val)));
        }

        /**
         * Sets our entry's value and writes through to the map. The
         * value to return is somewhat arbitrary here. Since we do not
         * necessarily track asynchronous changes, the most recent
         * "previous" value could be different from what we return (or
         * could even have been removed, in which case the put will
         * re-establish). We do not and cannot guarantee more.
         */
        public V setValue(final V value) {
            if (value == null) throw new NullPointerException();
            final V v = val;
            val = value;
            map.put(key, value);
            return v;
        }
    }

    /* ----------------Views -------------- */

    /**
     * Base class for views.
     */
    abstract static class CollectionView<K, V, E>
            implements Collection<E>, java.io.Serializable {
        private static final long serialVersionUID = 7249069246763182397L;
        final ConcurrentHashMapV8<K, V> map;

        CollectionView(final ConcurrentHashMapV8<K, V> map) {
            this.map = map;
        }

        /**
         * Returns the map backing this view.
         *
         * @return the map backing this view
         */
        public ConcurrentHashMapV8<K, V> getMap() {
            return map;
        }

        /**
         * Removes all of the elements from this view, by removing all
         * the mappings from the map backing this view.
         */
        public final void clear() {
            map.clear();
        }

        public final int size() {
            return map.size();
        }

        public final boolean isEmpty() {
            return map.isEmpty();
        }

        // implementations below rely on concrete classes supplying these
        // abstract methods

        /**
         * Returns a "weakly consistent" iterator that will never
         * throw {@link ConcurrentModificationException}, and
         * guarantees to traverse elements as they existed upon
         * construction of the iterator, and may (but is not
         * guaranteed to) reflect any modifications subsequent to
         * construction.
         */
        public abstract Iterator<E> iterator();

        public abstract boolean contains(Object o);

        public abstract boolean remove(Object o);

        private static final String oomeMsg = "Required array size too large";

        public final Object[] toArray() {
            final long sz = map.mappingCount();
            if (sz > MAX_ARRAY_SIZE)
                throw new OutOfMemoryError(oomeMsg);
            int n = (int) sz;
            Object[] r = new Object[n];
            int i = 0;
            for (final E e : this) {
                if (i == n) {
                    if (n >= MAX_ARRAY_SIZE)
                        throw new OutOfMemoryError(oomeMsg);
                    if (n >= MAX_ARRAY_SIZE - (MAX_ARRAY_SIZE >>> 1) - 1)
                        n = MAX_ARRAY_SIZE;
                    else
                        n += (n >>> 1) + 1;
                    r = Arrays.copyOf(r, n);
                }
                r[i++] = e;
            }
            return (i == n) ? r : Arrays.copyOf(r, i);
        }

        @SuppressWarnings("unchecked")
        public final <T> T[] toArray(final T[] a) {
            final long sz = map.mappingCount();
            if (sz > MAX_ARRAY_SIZE)
                throw new OutOfMemoryError(oomeMsg);
            final int m = (int) sz;
            T[] r = (a.length >= m) ? a :
                    (T[]) java.lang.reflect.Array
                            .newInstance(a.getClass().getComponentType(), m);
            int n = r.length;
            int i = 0;
            for (final E e : this) {
                if (i == n) {
                    if (n >= MAX_ARRAY_SIZE)
                        throw new OutOfMemoryError(oomeMsg);
                    if (n >= MAX_ARRAY_SIZE - (MAX_ARRAY_SIZE >>> 1) - 1)
                        n = MAX_ARRAY_SIZE;
                    else
                        n += (n >>> 1) + 1;
                    r = Arrays.copyOf(r, n);
                }
                r[i++] = (T) e;
            }
            if (a == r && i < n) {
                r[i] = null; // null-terminate
                return r;
            }
            return (i == n) ? r : Arrays.copyOf(r, i);
        }

        /**
         * Returns a string representation of this collection.
         * The string representation consists of the string representations
         * of the collection's elements in the order they are returned by
         * its iterator, enclosed in square brackets ({@code "[]"}).
         * Adjacent elements are separated by the characters {@code ", "}
         * (comma and space).  Elements are converted to strings as by
         * {@link String#valueOf(Object)}.
         *
         * @return a string representation of this collection
         */
        public final String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append('[');
            final Iterator<E> it = iterator();
            if (it.hasNext()) {
                for (; ; ) {
                    final Object e = it.next();
                    sb.append(e == this ? "(this Collection)" : e);
                    if (!it.hasNext())
                        break;
                    sb.append(',').append(' ');
                }
            }
            return sb.append(']').toString();
        }

        public final boolean containsAll(final Collection<?> c) {
            if (c != this) {
                for (final Object e : c) {
                    if (e == null || !contains(e))
                        return false;
                }
            }
            return true;
        }

        public final boolean removeAll(final Collection<?> c) {
            boolean modified = false;
            for (final Iterator<E> it = iterator(); it.hasNext(); ) {
                if (c.contains(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        }

        public final boolean retainAll(final Collection<?> c) {
            boolean modified = false;
            for (final Iterator<E> it = iterator(); it.hasNext(); ) {
                if (!c.contains(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        }

    }

    /**
     * A view of a ConcurrentHashMapV8 as a {@link Set} of keys, in
     * which additions may optionally be enabled by mapping to a
     * common value.  This class cannot be directly instantiated.
     * See {@link #keySet() keySet()},
     * {@link #keySet(Object) keySet(V)},
     * {@link #newKeySet() newKeySet()},
     * {@link #newKeySet(int) newKeySet(int)}.
     *
     * @since 1.8
     */
    public static class KeySetView<K, V> extends CollectionView<K, V, K>
            implements Set<K>, java.io.Serializable {
        private static final long serialVersionUID = 7249069246763182397L;
        private final V value;

        KeySetView(final ConcurrentHashMapV8<K, V> map, final V value) {  // non-public
            super(map);
            this.value = value;
        }

        /**
         * Returns the default mapped value for additions,
         * or {@code null} if additions are not supported.
         *
         * @return the default mapped value for additions, or {@code null}
         *         if not supported
         */
        public V getMappedValue() {
            return value;
        }

        /**
         * {@inheritDoc}
         *
         * @throws NullPointerException if the specified key is null
         */
        public boolean contains(final Object o) {
            return map.containsKey(o);
        }

        /**
         * Removes the key from this map view, by removing the key (and its
         * corresponding value) from the backing map.  This method does
         * nothing if the key is not in the map.
         *
         * @param o the key to be removed from the backing map
         * @return {@code true} if the backing map contained the specified key
         * @throws NullPointerException if the specified key is null
         */
        public boolean remove(final Object o) {
            return map.remove(o) != null;
        }

        /**
         * @return an iterator over the keys of the backing map
         */
        public Iterator<K> iterator() {
            final Node<K, V>[] t;
            final ConcurrentHashMapV8<K, V> m = map;
            final int f = (t = m.table) == null ? 0 : t.length;
            return new KeyIterator<K, V>(t, f, 0, f, m);
        }

        /**
         * Adds the specified key to this set view by mapping the key to
         * the default mapped value in the backing map, if defined.
         *
         * @param e key to be added
         * @return {@code true} if this set changed as a result of the call
         * @throws NullPointerException          if the specified key is null
         * @throws UnsupportedOperationException if no default mapped value
         *                                       for additions was provided
         */
        public boolean add(final K e) {
            final V v;
            if ((v = value) == null)
                throw new UnsupportedOperationException();
            return map.putVal(e, v, true) == null;
        }

        /**
         * Adds all of the elements in the specified collection to this set,
         * as if by calling {@link #add} on each one.
         *
         * @param c the elements to be inserted into this set
         * @return {@code true} if this set changed as a result of the call
         * @throws NullPointerException          if the collection or any of its
         *                                       elements are {@code null}
         * @throws UnsupportedOperationException if no default mapped value
         *                                       for additions was provided
         */
        public boolean addAll(final Collection<? extends K> c) {
            boolean added = false;
            final V v;
            if ((v = value) == null)
                throw new UnsupportedOperationException();
            for (final K e : c) {
                if (map.putVal(e, v, true) == null)
                    added = true;
            }
            return added;
        }

        public int hashCode() {
            int h = 0;
            for (final K e : this)
                h += e.hashCode();
            return h;
        }

        public boolean equals(final Object o) {
            final Set<?> c;
            return ((o instanceof Set) &&
                    ((c = (Set<?>) o) == this ||
                            (containsAll(c) && c.containsAll(this))));
        }
    }

    /**
     * A view of a ConcurrentHashMapV8 as a {@link Collection} of
     * values, in which additions are disabled. This class cannot be
     * directly instantiated. See {@link #values()}.
     */
    static final class ValuesView<K, V> extends CollectionView<K, V, V>
            implements Collection<V>, java.io.Serializable {
        private static final long serialVersionUID = 2249069246763182397L;

        ValuesView(final ConcurrentHashMapV8<K, V> map) {
            super(map);
        }

        public final boolean contains(final Object o) {
            return map.containsValue(o);
        }

        public final boolean remove(final Object o) {
            if (o != null) {
                for (final Iterator<V> it = iterator(); it.hasNext(); ) {
                    if (o.equals(it.next())) {
                        it.remove();
                        return true;
                    }
                }
            }
            return false;
        }

        public final Iterator<V> iterator() {
            final ConcurrentHashMapV8<K, V> m = map;
            final Node<K, V>[] t;
            final int f = (t = m.table) == null ? 0 : t.length;
            return new ValueIterator<K, V>(t, f, 0, f, m);
        }

        public final boolean add(final V e) {
            throw new UnsupportedOperationException();
        }

        public final boolean addAll(final Collection<? extends V> c) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * A view of a ConcurrentHashMapV8 as a {@link Set} of (key, value)
     * entries.  This class cannot be directly instantiated. See
     * {@link #entrySet()}.
     */
    static final class EntrySetView<K, V> extends CollectionView<K, V, Map.Entry<K, V>>
            implements Set<Map.Entry<K, V>>, java.io.Serializable {
        private static final long serialVersionUID = 2249069246763182397L;

        EntrySetView(final ConcurrentHashMapV8<K, V> map) {
            super(map);
        }

        public boolean contains(final Object o) {
            final Object k;
            final Object v;
            final Object r;
            final Map.Entry<?, ?> e;
            return ((o instanceof Map.Entry) &&
                    (k = (e = (Map.Entry<?, ?>) o).getKey()) != null &&
                    (r = map.get(k)) != null &&
                    (v = e.getValue()) != null &&
                    (v == r || v.equals(r)));
        }

        public boolean remove(final Object o) {
            final Object k;
            final Object v;
            final Map.Entry<?, ?> e;
            return ((o instanceof Map.Entry) &&
                    (k = (e = (Map.Entry<?, ?>) o).getKey()) != null &&
                    (v = e.getValue()) != null &&
                    map.remove(k, v));
        }

        /**
         * @return an iterator over the entries of the backing map
         */
        public Iterator<Map.Entry<K, V>> iterator() {
            final ConcurrentHashMapV8<K, V> m = map;
            final Node<K, V>[] t;
            final int f = (t = m.table) == null ? 0 : t.length;
            return new EntryIterator<K, V>(t, f, 0, f, m);
        }

        public boolean add(final Entry<K, V> e) {
            return map.putVal(e.getKey(), e.getValue(), false) == null;
        }

        public boolean addAll(final Collection<? extends Entry<K, V>> c) {
            boolean added = false;
            for (final Entry<K, V> e : c) {
                if (add(e))
                    added = true;
            }
            return added;
        }

        public final int hashCode() {
            int h = 0;
            final Node<K, V>[] t;
            if ((t = map.table) != null) {
                final Traverser<K, V> it = new Traverser<K, V>(t, t.length, 0, t.length);
                for (Node<K, V> p; (p = it.advance()) != null; ) {
                    h += p.hashCode();
                }
            }
            return h;
        }

        public final boolean equals(final Object o) {
            final Set<?> c;
            return ((o instanceof Set) &&
                    ((c = (Set<?>) o) == this ||
                            (containsAll(c) && c.containsAll(this))));
        }
    }

    // -------------------------------------------------------

    /* ---------------- Counters -------------- */

    // Adapted from LongAdder and Striped64.
    // See their internal docs for explanation.

    // A padded cell for distributing counts
    static final class CounterCell {
        volatile long p0, p1, p2, p3, p4, p5, p6;
        volatile long value;
        volatile long q0, q1, q2, q3, q4, q5, q6;

        CounterCell(final long x) {
            value = x;
        }
    }

    /**
     * Holder for the thread-local hash code determining which
     * CounterCell to use. The code is initialized via the
     * counterHashCodeGenerator, but may be moved upon collisions.
     */
    static final class CounterHashCode {
        int code;
    }

    /**
     * Generates initial value for per-thread CounterHashCodes.
     */
    static final AtomicInteger counterHashCodeGenerator = new AtomicInteger();

    /**
     * Increment for counterHashCodeGenerator. See class ThreadLocal
     * for explanation.
     */
    static final int SEED_INCREMENT = 0x61c88647;

    /**
     * Per-thread counter hash codes. Shared across all instances.
     */
    static final ThreadLocal<CounterHashCode> threadCounterHashCode =
            new ThreadLocal<CounterHashCode>();


    final long sumCount() {
        final CounterCell[] as = counterCells;
        CounterCell a;
        long sum = baseCount;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    sum += a.value;
            }
        }
        return sum;
    }

    // See LongAdder version for explanation
    private final void fullAddCount(final long x, CounterHashCode hc,
                                    boolean wasUncontended) {
        int h;
        if (hc == null) {
            hc = new CounterHashCode();
            final int s = counterHashCodeGenerator.addAndGet(SEED_INCREMENT);
            h = hc.code = (s == 0) ? 1 : s; // Avoid zero
            threadCounterHashCode.set(hc);
        } else
            h = hc.code;
        boolean collide = false;                // True if last slot nonempty
        for (; ; ) {
            final CounterCell[] as;
            final CounterCell a;
            final int n;
            final long v;
            if ((as = counterCells) != null && (n = as.length) > 0) {
                if ((a = as[(n - 1) & h]) == null) {
                    if (cellsBusy == 0) {            // Try to attach new Cell
                        final CounterCell r = new CounterCell(x); // Optimistic create
                        if (cellsBusy == 0 &&
                                U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                            boolean created = false;
                            try {               // Recheck under lock
                                final CounterCell[] rs;
                                final int m;
                                final int j;
                                if ((rs = counterCells) != null &&
                                        (m = rs.length) > 0 &&
                                        rs[j = (m - 1) & h] == null) {
                                    rs[j] = r;
                                    created = true;
                                }
                            } finally {
                                cellsBusy = 0;
                            }
                            if (created)
                                break;
                            continue;           // Slot is now non-empty
                        }
                    }
                    collide = false;
                } else if (!wasUncontended)       // CAS already known to fail
                    wasUncontended = true;      // Continue after rehash
                else if (U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))
                    break;
                else if (counterCells != as || n >= NCPU)
                    collide = false;            // At max size or stale
                else if (!collide)
                    collide = true;
                else if (cellsBusy == 0 &&
                        U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                    try {
                        if (counterCells == as) {// Expand table unless stale
                            final CounterCell[] rs = new CounterCell[n << 1];
                            System.arraycopy(as, 0, rs, 0, n);
                            counterCells = rs;
                        }
                    } finally {
                        cellsBusy = 0;
                    }
                    collide = false;
                    continue;                   // Retry with expanded table
                }
                h ^= h << 13;                   // Rehash
                h ^= h >>> 17;
                h ^= h << 5;
            } else if (cellsBusy == 0 && counterCells == as &&
                    U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                boolean init = false;
                try {                           // Initialize table
                    if (counterCells == as) {
                        final CounterCell[] rs = new CounterCell[2];
                        rs[h & 1] = new CounterCell(x);
                        counterCells = rs;
                        init = true;
                    }
                } finally {
                    cellsBusy = 0;
                }
                if (init)
                    break;
            } else if (U.compareAndSwapLong(this, BASECOUNT, v = baseCount, v + x))
                break;                          // Fall back on using base
        }
        hc.code = h;                            // Record index for next time
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe U;
    private static final long SIZECTL;
    private static final long TRANSFERINDEX;
    private static final long TRANSFERORIGIN;
    private static final long BASECOUNT;
    private static final long CELLSBUSY;
    private static final long CELLVALUE;
    private static final long ABASE;
    private static final int ASHIFT;

    static {
        try {
            U = getUnsafe();
            final Class<?> k = ConcurrentHashMapV8.class;
            SIZECTL = U.objectFieldOffset
                    (k.getDeclaredField("sizeCtl"));
            TRANSFERINDEX = U.objectFieldOffset
                    (k.getDeclaredField("transferIndex"));
            TRANSFERORIGIN = U.objectFieldOffset
                    (k.getDeclaredField("transferOrigin"));
            BASECOUNT = U.objectFieldOffset
                    (k.getDeclaredField("baseCount"));
            CELLSBUSY = U.objectFieldOffset
                    (k.getDeclaredField("cellsBusy"));
            final Class<?> ck = CounterCell.class;
            CELLVALUE = U.objectFieldOffset
                    (ck.getDeclaredField("value"));
            final Class<?> ak = Node[].class;
            ABASE = U.arrayBaseOffset(ak);
            final int scale = U.arrayIndexScale(ak);
            if ((scale & (scale - 1)) != 0)
                throw new Error("data type scale not a power of two");
            ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (final Exception e) {
            throw new Error(e);
        }
    }

    /**
     * Returns a sun.misc.Unsafe.  Suitable for use in a 3rd party package.
     * Replace with a simple call to Unsafe.getUnsafe when integrating
     * into a jdk.
     *
     * @return a sun.misc.Unsafe
     */
    private static sun.misc.Unsafe getUnsafe() {
        try {
            return sun.misc.Unsafe.getUnsafe();
        } catch (final SecurityException tryReflectionInstead) {
        }
        try {
            return java.security.AccessController.doPrivileged
                    (new java.security.PrivilegedExceptionAction<sun.misc.Unsafe>() {
                        public sun.misc.Unsafe run() throws Exception {
                            final Class<sun.misc.Unsafe> k = sun.misc.Unsafe.class;
                            for (final java.lang.reflect.Field f : k.getDeclaredFields()) {
                                f.setAccessible(true);
                                final Object x = f.get(null);
                                if (k.isInstance(x))
                                    return k.cast(x);
                            }
                            throw new NoSuchFieldError("the Unsafe");
                        }
                    });
        } catch (final java.security.PrivilegedActionException e) {
            throw new RuntimeException("Could not initialize intrinsics",
                    e.getCause());
        }
    }
}
