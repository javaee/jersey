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

package org.glassfish.jersey.examples.beanvalidation.webapp.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.glassfish.jersey.examples.beanvalidation.webapp.domain.ContactCard;

/**
 * Simple storage of contacts.
 *
 * @author Michal Gajdos
 */
public class StorageService {

    private static final AtomicLong contactCounter = new AtomicLong(0);
    private static final Map<Long, ContactCard> contacts = new HashMap<Long, ContactCard>();

    /**
     * Adds a contact into the storage. If a contact with given data already exist {@code null} value is returned.
     *
     * @param contact contact to be added.
     * @return contact with pre-filled {@code id} field, {@code null} if the contact already exist in the storage.
     */
    public static ContactCard addContact(final ContactCard contact) {
        if (contacts.containsValue(contact)) {
            return null;
        }

        contact.setId(contactCounter.incrementAndGet());
        contacts.put(contact.getId(), contact);

        return contact;
    }

    /**
     * Removes all contacts from the storage.
     *
     * @return list of all removed contacts.
     */
    public static List<ContactCard> clear() {
        final Collection<ContactCard> values = contacts.values();
        contacts.clear();
        return new ArrayList<ContactCard>(values);
    }

    /**
     * Removes contact with given {@code id}.
     *
     * @param id id of the contact to be removed.
     * @return removed contact or {@code null} if the contact is not present in the storage.
     */
    public static ContactCard remove(final Long id) {
        return contacts.remove(id);
    }

    /**
     * Retrieves contact with given {@code id}.
     *
     * @param id id of the contact to be retrieved.
     * @return contact or {@code null} if the contact is not present in the storage.
     */
    public static ContactCard get(final Long id) {
        return contacts.get(id);
    }

    /**
     * Finds contacts whose email contains {@code emailPart} as a substring.
     *
     * @param emailPart search phrase.
     * @return list of matched contacts or an empty list.
     */
    public static List<ContactCard> findByEmail(final String emailPart) {
        final List<ContactCard> results = new ArrayList<ContactCard>();

        for (final ContactCard contactCard : contacts.values()) {
            final String email = contactCard.getEmail();
            if (email != null && email.contains(emailPart)) {
                results.add(contactCard);
            }
        }

        return results;
    }

    /**
     * Finds contacts whose name contains {@code namePart} as a substring.
     *
     * @param namePart search phrase.
     * @return list of matched contacts or an empty list.
     */
    public static List<ContactCard> findByName(final String namePart) {
        final List<ContactCard> results = new ArrayList<ContactCard>();

        for (final ContactCard contactCard : contacts.values()) {
            if (contactCard.getFullName().contains(namePart)) {
                results.add(contactCard);
            }
        }

        return results;
    }

    /**
     * Finds contacts whose phone contains {@code phonePart} as a substring.
     *
     * @param phonePart search phrase.
     * @return list of matched contacts or an empty list.
     */
    public static List<ContactCard> findByPhone(final String phonePart) {
        final List<ContactCard> results = new ArrayList<ContactCard>();

        for (final ContactCard contactCard : contacts.values()) {
            final String phone = contactCard.getPhone();
            if (phone != null && phone.contains(phonePart)) {
                results.add(contactCard);
            }
        }

        return results;
    }
}
