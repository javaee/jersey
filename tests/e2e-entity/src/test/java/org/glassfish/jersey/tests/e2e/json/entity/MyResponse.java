/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import javax.json.bind.annotation.JsonbVisibility;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for anonymous complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://test.jaxb.com}myMessage" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://test.jaxb.com}myError"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@SuppressWarnings("RedundantIfStatement")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "myMessage", "myError"
})
@XmlRootElement(name = "myResponse")
@JsonbVisibility(CustomJsonbVisibilityStrategy.class)
public class MyResponse {

    @XmlElement(namespace = "http://test.jaxb.com")
    protected List<MyMessage> myMessage;
    @XmlElement(namespace = "http://test.jaxb.com", required = true)
    protected MyError myError;

    /**
     * Gets the value of the myMessage property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the myMessage property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMyMessage().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link MyMessage }
     */
    public List<MyMessage> getMyMessage() {
        if (myMessage == null) {
            myMessage = new ArrayList<>();
        }
        return this.myMessage;
    }

    /**
     * Gets the value of the myError property.
     *
     * @return possible object is
     *         {@link MyError }
     */
    public MyError getMyError() {
        return myError;
    }

    /**
     * Sets the value of the myError property.
     *
     * @param value allowed object is
     * {@link MyError }
     */
    public void setMyError(MyError value) {
        this.myError = value;
    }

    public static Object createTestInstance() {

        MyResponse myResponse = new MyResponse();

        MyMessage msg = new MyMessage();
        msg.setId("0");
        msg.setText("ok");
        MyMessage msg2 = new MyMessage();
        msg2.setId("1");
        msg2.setText("ok");
        myResponse.getMyMessage().add(msg);
        myResponse.getMyMessage().add(msg2);

        MyError err = new MyError();
        err.setId("-1");
        err.setDesc("error");
        myResponse.setMyError(err);
        return myResponse;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MyResponse other = (MyResponse) obj;
        if (this.myMessage != other.myMessage && (this.myMessage == null || !this.myMessage.equals(other.myMessage))) {
            return false;
        }
        if (this.myError != other.myError && (this.myError == null || !this.myError.equals(other.myError))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + (this.myMessage != null ? this.myMessage.hashCode() : 0);
        hash = 97 * hash + (this.myError != null ? this.myError.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(getMyMessage());
        sb.append(",");
        sb.append(getMyError());
        sb.append("}");
        return sb.toString();
    }
}
