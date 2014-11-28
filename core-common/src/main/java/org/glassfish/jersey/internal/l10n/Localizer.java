/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.internal.l10n;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.glassfish.jersey.internal.OsgiRegistry;
import org.glassfish.jersey.internal.util.ReflectionHelper;

import org.glassfish.hk2.osgiresourcelocator.ResourceFinder;

/**
 * Localizes the {@link Localizable} into a message
 * by using a configured {@link Locale}.
 *
 * @author WS Development Team
 */
public class Localizer {

    private final Locale _locale;
    private final HashMap<String, ResourceBundle> _resourceBundles;

    public Localizer() {
        this(Locale.getDefault());
    }

    public Localizer(Locale l) {
        _locale = l;
        _resourceBundles = new HashMap<String, ResourceBundle>();
    }

    public Locale getLocale() {
        return _locale;
    }

    public String localize(Localizable l) {
        String key = l.getKey();
        if (Localizable.NOT_LOCALIZABLE.equals(key)) {
            // this message is not localizable
            return (String) l.getArguments()[0];
        }
        String bundlename = l.getResourceBundleName();

        try {
            ResourceBundle bundle = _resourceBundles.get(bundlename);

            if (bundle == null) {
                try {
                    bundle = ResourceBundle.getBundle(bundlename, _locale);
                } catch (MissingResourceException e) {
                    // work around a bug in the com.sun.enterprise.deployment.WebBundleArchivist:
                    //   all files with an extension different from .class (hence all the .properties files)
                    //   get copied to the top level directory instead of being in the package where they
                    //   are defined
                    // so, since we can't find the bundle under its proper name, we look for it under
                    //   the top-level package

                    int i = bundlename.lastIndexOf('.');
                    if (i != -1) {
                        String alternateBundleName =
                                bundlename.substring(i + 1);
                        try {
                            bundle =
                                    ResourceBundle.getBundle(
                                    alternateBundleName,
                                    _locale);
                        } catch (MissingResourceException e2) {
                            // try OSGi
                                OsgiRegistry osgiRegistry = ReflectionHelper.getOsgiRegistryInstance();
                                if (osgiRegistry != null) {
                                    bundle = osgiRegistry.getResourceBundle(bundlename);
                                } else {
                                    final String path = bundlename.replace('.', '/') + ".properties";
                                    final URL bundleUrl = ResourceFinder.findEntry(path);
                                    if (bundleUrl != null) {
                                        try {
                                            bundle = new PropertyResourceBundle(bundleUrl.openStream());
                                        } catch (IOException ex) {
                                            // ignore
                                        }
                                    }
                                }
                        }
                    }
                }

                if (bundle == null) {
                    return getDefaultMessage(l);
                } else {
                    _resourceBundles.put(bundlename, bundle);
                }
            }

            if (key == null) {
                key = "undefined";
            }

            String msg;
            try {
                msg = bundle.getString(key);
            } catch (MissingResourceException e) {
                // notice that this may throw a MissingResourceException of its own (caught below)
                msg = bundle.getString("undefined");
            }

            // localize all arguments to the given localizable object
            Object[] args = l.getArguments();
            for (int i = 0; i < args.length; ++i) {
                if (args[i] instanceof Localizable) {
                    args[i] = localize((Localizable) args[i]);
                }
            }

            String message = MessageFormat.format(msg, args);
            return message;

        } catch (MissingResourceException e) {
            return getDefaultMessage(l);
        }

    }

    private String getDefaultMessage(Localizable l) {
        String key = l.getKey();
        Object[] args = l.getArguments();
        StringBuilder sb = new StringBuilder();
        sb.append("[failed to localize] ");
        sb.append(key);
        if (args != null) {
            sb.append('(');
            for (int i = 0; i < args.length; ++i) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(String.valueOf(args[i]));
            }
            sb.append(')');
        }
        return sb.toString();
    }
}
