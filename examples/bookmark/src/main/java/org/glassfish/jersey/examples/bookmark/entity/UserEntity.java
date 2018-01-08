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
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.examples.bookmark.entity;

import java.io.Serializable;
import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Entity class UserEntity.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@SuppressWarnings("UnusedDeclaration")
@Entity
@Table(name = "USERS")
@NamedQueries({
        @NamedQuery(name = "UserEntity.findByUserid", query = "SELECT u FROM UserEntity u WHERE u.userid = :userid"),
        @NamedQuery(name = "UserEntity.findByPassword", query = "SELECT u FROM UserEntity u WHERE u.password = :password"),
        @NamedQuery(name = "UserEntity.findByUsername", query = "SELECT u FROM UserEntity u WHERE u.username = :username"),
        @NamedQuery(name = "UserEntity.findByEmail", query = "SELECT u FROM UserEntity u WHERE u.email = :email")
})
public class UserEntity implements Serializable {

    @Id
    @Column(name = "USERID", nullable = false)
    private String userid;

    @Column(name = "PASSWORD", nullable = false)
    private String password;

    @Column(name = "USERNAME")
    private String username;

    @Column(name = "EMAIL")
    private String email;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "userEntity")
    private Collection<BookmarkEntity> bookmarkEntityCollection;

    /**
     * Creates a new instance of UserEntity
     */
    public UserEntity() {
    }

    /**
     * Creates a new instance of UserEntity with the specified values.
     *
     * @param userid the userid of the UserEntity
     */
    public UserEntity(String userid) {
        this.userid = userid;
    }

    /**
     * Creates a new instance of UserEntity with the specified values.
     *
     * @param userid the userid of the UserEntity
     * @param password the password of the UserEntity
     */
    public UserEntity(String userid, String password) {
        this.userid = userid;
        this.password = password;
    }

    /**
     * Gets the userid of this UserEntity.
     *
     * @return the userid
     */
    public String getUserid() {
        return this.userid;
    }

    /**
     * Sets the userid of this UserEntity to the specified value.
     *
     * @param userid the new userid
     */
    public void setUserid(String userid) {
        this.userid = userid;
    }

    /**
     * Gets the password of this UserEntity.
     *
     * @return the password
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Sets the password of this UserEntity to the specified value.
     *
     * @param password the new password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets the username of this UserEntity.
     *
     * @return the username
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Sets the username of this UserEntity to the specified value.
     *
     * @param username the new username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the email of this UserEntity.
     *
     * @return the email
     */
    public String getEmail() {
        return this.email;
    }

    /**
     * Sets the email of this UserEntity to the specified value.
     *
     * @param email the new email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the bookmarkEntityCollection of this UserEntity.
     *
     * @return the bookmarkEntityCollection
     */
    public Collection<BookmarkEntity> getBookmarkEntityCollection() {
        return this.bookmarkEntityCollection;
    }

    /**
     * Sets the bookmarkEntityCollection of this UserEntity to the specified value.
     *
     * @param bookmarkEntityCollection the new bookmarkEntityCollection
     */
    public void setBookmarkEntityCollection(Collection<BookmarkEntity> bookmarkEntityCollection) {
        this.bookmarkEntityCollection = bookmarkEntityCollection;
    }

    /**
     * Returns a hash code value for the object.  This implementation computes
     * a hash code value based on the id fields in this object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.userid != null ? this.userid.hashCode() : 0);
        return hash;
    }

    /**
     * Determines whether another object is equal to this UserEntity.  The result is
     * <code>true</code> if and only if the argument is not null and is a UserEntity object that
     * has the same id field values as this object.
     *
     * @param object the reference object with which to compare
     * @return <code>true</code> if this object is the same as the argument;
     *         <code>false</code> otherwise.
     */
    @SuppressWarnings("StringEquality")
    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof UserEntity)) {
            return false;
        }

        UserEntity other = (UserEntity) object;

        return !(this.userid != other.userid && (this.userid == null || !this.userid.equals(other.userid)));
    }

    /**
     * Returns a string representation of the object.  This implementation constructs
     * that representation based on the id fields.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return "UserEntity{"
               + "userid='" + userid + '\''
               + '}';
    }
}
