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

package org.glassfish.jersey.media.multipart;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.server.model.ParamQualifier;

/**
 * Binds the named body part(s) of a "multipart/form-data" request
 * entity body to a resource method parameter.
 * <p/>
 * The {@link FormParam} annotation in conjunction with the media type
 * "application/x-www-form-urlencoded" is inefficient for sending and
 * consuming large quantities of binary data or text containing non-ASCII
 * characters.
 * <p/>
 * This annotation in conjunction with the media type "multipart/form-data"
 * should be used for submitting and consuming forms that contain files,
 * non-ASCII data, and binary data.
 * <p/>
 * The type {@code T} of the annotated parameter must be one of the
 * following:
 * <ol>
 * <li>{@link FormDataBodyPart}. The value of the parameter will be the
 *     first named body part, otherwise null if such a named body part is not
 *     present.
 * <li>A {@code List} or {@code Collection} of {@link FormDataBodyPart}.
 *     The value of the
 *     parameter will one or more named body parts with the same name, otherwise
 *     null if such a named body part is not present.
 * <li>{@link FormDataContentDisposition}. The value of the parameter will be
 *     the content disposition of the first named body part, otherwise null if
 *     such a named body part is not present.
 * <li>A {@code List} or {@code Collection} of {@link FormDataContentDisposition}.
 *     The value of
 *     the parameter will one or more content dispositions of the named body parts
 *     with the same name, otherwise null if such a named body part is not
 *     present.
 * <li>A type for which a message body reader is available given the media type
 *     of the first named body part. The value of the parameter will be the
 *     result of reading using the message body reader given the type {@code T},
 *     the media type of the named part, and the bytes of the named body part as
 *     input.
 *     <p>
 *     If there is no named part present and there is a default value present as
 *     declared by {@link DefaultValue} then the media type will be set to
 *     "text/plain". The value of the parameter will be the result of reading
 *     using the message body reader given the type {@code T}, the media type
 *     "text/plain", and the UTF-8 encoded bytes of the default value as input.
 *     <p>
 *     If there is no message body reader available and the type {@code T} conforms
 *     to a type specified by {@link FormParam} then processing is performed
 *     as specified by {@link FormParam}, where the values of the form parameter
 *     are {@code String} instances produced by reading the bytes of the named body
 *     parts utilizing a message body reader for the {@code String} type and the
 *     media type "text/plain".
 *     <p>
 *     If there is no named part present then processing is performed as
 *     specified by {@link FormParam}.</li>
 * </ol>
 * <p/>
 * For example, the use of this annotation allows one to support the
 * following:
 * <blockquote><pre>
 *     &#064;POST
 *     &#064;Consumes(MediaType.MULTIPART_FORM_DATA_TYPE)
 *     public String postForm(
 *             &#064;DefaultValue("true") &#064;FormDataParam("enabled") boolean enabled,
 *             &#064;FormDataParam("data") FileData bean,
 *             &#064;FormDataParam("file") InputStream file,
 *             &#064;FormDataParam("file") FormDataContentDisposition fileDisposition) {
 *         ...
 *     }
 * </pre></blockquote>
 * Where the server consumes a "multipart/form-data" request entity body that
 * contains one optional named body part "enabled" and two required named
 * body parts "data" and "file".
 * <p/>
 * The optional part "enabled" is processed
 * as a {@code boolean} value, if the part is absent then the
 * value will be {@code true}.
 * <p/>
 * The part "data" is processed as a JAXB bean and contains some meta-data
 * about the following part.
 * <p/>
 * The part "file" is a file that is uploaded, this is processed as an
 * {@code InputStream}. Additional information about the file from the
 * "Content-Disposition" header can be accessed by the parameter
 * {@code fileDisposition}.
 * <p/>
 * Note that, whilst the annotation target permits use on fields and methods,
 * this annotation is only required to be supported on resource method
 * parameters.
 *
 * @see FormDataMultiPart
 * @see FormDataBodyPart
 * @see FormDataContentDisposition
 * @see javax.ws.rs.DefaultValue
 * @see javax.ws.rs.FormParam
 *
 * @author Craig McClanahan
 * @author Paul Sandoz
 * @author Michal Gajdos
 */
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@ParamQualifier
public @interface FormDataParam {

    /**
     * Defines the control name of a "multipart/form-data" body part whose
     * content will be used to initialize the value of the annotated method
     * argument.
     *
     * @return the control name of a "multipart/form-data" body part.
     */
    String value();

}
