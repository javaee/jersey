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

/* Controllers */

'use strict';

function ContactController($scope, $resource, Contact) {

    // Common
    $scope.httpStatus = '';
    $scope.errors = [];

    $scope.processErrors = function(response) {
        $scope.clearErrors();

        $scope.httpStatus = response.status;
        response.data.forEach(function(error) {
            $scope.errors.push({path:error.path, message:error.message});
        });
    };

    $scope.clearErrors = function() {
        $scope.httpStatus = '';
        $scope.errors = [];
    };

    $scope.reset = function() {
        $scope.contact = {};
        $scope.searchValue = '';

        $scope.clearErrors();
    };

    $scope.refresh = function() {
        $scope.reset();
        $scope.contacts = Contact.query();
    };

    $scope.remove = function(array, id) {
        var i = 0;
        array.forEach(function(object) {
            if (object.id == id) {
                array.splice(i, 1);
            }
            i++;
        });
    };

    // Contact
    $scope.contacts = [];
    $scope.contact = {};

    $scope.addContact = function() {
        Contact.save(
            $scope.contact,
            function(data) {
                $scope.contacts.push(data);
                $scope.reset();
            },
            function(response) {
                $scope.processErrors(response);
            }
        );
    };

    $scope.removeContact = function(id) {
        Contact.remove(
            {contactId:id},
            function(data) {
                $scope.remove($scope.contacts, data.id);
            },
            function(response) {
                $scope.processErrors(response);
            }
        );
    };

    // Search

    $scope.searchType = 'name';
    $scope.searchValue = '';

    $scope.searchIcon = 'user';
    $scope.searchCollapse = true;

    $scope.changeSearchType = function(type) {
        $scope.searchType = type;

        if (type == 'phone') {
            $scope.searchIcon = 'home';
        } else if (type == 'email') {
            $scope.searchIcon = 'envelope';
        } else if (type == 'unknown') {
            $scope.searchIcon = 'question-sign';
        }  else {
            $scope.searchIcon = 'user';
        }

        $scope.searchCollapse = true;
    };

    $scope.search = function() {
        $resource('api/contact/search/:searchType?q=:searchValue').
            query(
                {searchType:$scope.searchType, searchValue:$scope.searchValue},
                function(data) {
                    $scope.contacts = data;
                    $scope.reset();
                },
                function(response) {
                    $scope.processErrors(response);
                });
    };

    // Refresh
    $scope.refresh();
}