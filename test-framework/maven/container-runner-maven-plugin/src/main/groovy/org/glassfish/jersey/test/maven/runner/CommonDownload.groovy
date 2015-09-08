/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.test.maven.runner

import org.apache.maven.plugins.annotations.Parameter

/**
 * Common functionality of download Mojos.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
trait CommonDownload implements RunnerMojo {

    /**
     * From where to download the distribution archive file.
     */
    @Parameter(required = true, name = "distUrl", property = "jersey.runner.distUrl")
    String distUrl

    /**
     * Where to download (or look for) the distribution archive file.
     */
    @Parameter(defaultValue = "\${project.build.directory}/distArchive.jar", name = "distTargetLocation")
    String distTargetLocation

    /**
     * Whether to download the distribution archive if it already exists in {@link #distTargetLocation} path.
     */
    @Parameter(defaultValue = "false", property = "jersey.runner.downloadIfExists", name = "downloadIfExists")
    boolean downloadIfExists

    /**
     * Whether to overwrite unpacked distribution in {@link #distDir}; i.e., the {@code <distDir>/<distSubdir>} is removed and the
     * distribution is unpacked.
     */
    @Parameter(defaultValue = "true", property = "jersey.runner.overwrite", name = "overwrite")
    boolean overwrite

    /**
     * The http/https proxy to use
     */
    @Parameter(property = "jersey.runner.proxy", name = "proxy")
    String proxy

    Map commonEnvironment() {
        return [
                "DIST_URL"          : distUrl,
                "DIST_TGT_LOCATION" : distTargetLocation,

                "DOWNLOAD_IF_EXISTS": downloadIfExists as String,
                "OVERWRITE"         : overwrite as String,

                "all_proxy"         : proxy ?: "",
        ]
    }

    void setDistUrl(final String distUrl) {
        this.distUrl = distUrl
    }

    void setDistTargetLocation(final String distTargetLocation) {
        this.distTargetLocation = distTargetLocation
    }

    void setDownloadIfExists(final boolean downloadIfExists) {
        this.downloadIfExists = downloadIfExists
    }

    void setOverwrite(final boolean overwrite) {
        this.overwrite = overwrite
    }

    void setProxy(final String proxy) {
        this.proxy = proxy
    }
}
