/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

/**
 * The documentation type for params: method params, path params on a class etc.<br>
 * Created on: Jun 12, 2008<br>
 *
 * @author Martin Grotzke (martin.grotzke at freiheit.com)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "paramDoc", propOrder = {

})
public class ParamDocType {

    private String paramName;
    private String commentText;

    public ParamDocType() {
    }

    public ParamDocType(String paramName, String commentText) {
        this.paramName = paramName;
        this.commentText = commentText;
    }

    @XmlElementWrapper(name = "annotationDocs")
    protected List<AnnotationDocType> annotationDoc;

    public List<AnnotationDocType> getAnnotationDocs() {
        if (annotationDoc == null) {
            annotationDoc = new ArrayList<>();
        }
        return this.annotationDoc;
    }

    @XmlAnyElement(lax = true)
    private List<Object> any;

    public List<Object> getAny() {
        if (any == null) {
            any = new ArrayList<>();
        }
        return this.any;
    }

    /**
     * @return the commentText
     */
    public String getCommentText() {
        return commentText;
    }

    /**
     * @param commentText the commentText to set
     */
    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    /**
     * @return the className
     */
    public String getParamName() {
        return paramName;
    }

    /**
     * @param paramName the className to set
     */
    public void setParamName(String paramName) {
        this.paramName = paramName;
    }
}
