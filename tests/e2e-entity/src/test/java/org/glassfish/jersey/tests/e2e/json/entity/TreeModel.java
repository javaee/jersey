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
package org.glassfish.jersey.tests.e2e.json.entity;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.tests.e2e.json.JsonTestHelper;

/**
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@SuppressWarnings({"UnusedDeclaration", "RedundantIfStatement"})
@XmlRootElement
public class TreeModel {

    @SuppressWarnings({"UnusedDeclaration", "StringEquality", "RedundantIfStatement"})
    public static class Node {

        @XmlElement
        public String label;
        @XmlElement
        public boolean expanded;
        @XmlElement
        public List<Node> children;

        public Node() {
            this("dummy node", null);
        }

        public Node(String label) {
            this(label, null);
        }

        public Node(String label, Collection<Node> children) {
            this.label = label;
            if (!JsonTestHelper.isCollectionEmpty(children)) {
                this.children = new LinkedList<Node>();
                this.children.addAll(children);
                expanded = true;
            }
        }

        @Override
        public int hashCode() {
            int result = 13;
            result += 17 * label.hashCode();
            if (!JsonTestHelper.isCollectionEmpty(children)) {
                for (Node n : children) {
                    result = 5 + 17 * n.hashCode();
                }
            }
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Node)) {
                return false;
            }
            final Node other = (Node) obj;
            if (this.label != other.label && (this.label == null || !this.label.equals(other.label))) {
                return false;
            }
            if ((this.children != other.children
                    && JsonTestHelper.isCollectionEmpty(this.children) != JsonTestHelper.isCollectionEmpty(other.children))
                    && (this.children == null || !this.children.equals(other.children))) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            String result = "(" + label + ":";
            if (!JsonTestHelper.isCollectionEmpty(children)) {
                for (Node n : children) {
                    result += n.toString();
                }
                return result + ")";
            } else {
                return result + "0 children)";
            }
        }
    }

    @XmlElement
    public Node root;

    public TreeModel() {
    }

    public TreeModel(Node root) {
        this.root = root;
    }

    public static Object createTestInstance() {
        TreeModel instance = new TreeModel();
        instance.root = new Node();
        return instance;
    }

    @Override
    public int hashCode() {
        if (null != root) {
            return 7 + root.hashCode();
        } else {
            return 7;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TreeModel)) {
            return false;
        }
        final TreeModel other = (TreeModel) obj;
        if (this.root != other.root && (this.root == null || !this.root.equals(other.root))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return (null != root) ? root.toString() : "(NULL_ROOT)";
    }
}
