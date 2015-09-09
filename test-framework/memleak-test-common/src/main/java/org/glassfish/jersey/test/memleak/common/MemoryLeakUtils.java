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
package org.glassfish.jersey.test.memleak.common;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.management.MBeanServer;

import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

/**
 * Utility class for memory leak test infrastructure.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
public class MemoryLeakUtils {

    /**
     * The log file where the output (stdout/stderr) of container is located.
     * <p/>
     * For instance, this file is used for detection of {@link OutOfMemoryError} exception records.
     */
    public static final String JERSEY_CONFIG_TEST_CONTAINER_LOGFILE = "jersey.config.test.container.logfile";

    /**
     * The memory leak timeout denotes successful end of the memory leak test. That is, if the memory leak didn't occur during the
     * specified timeout, the test successfully finishes.
     */
    public static final String JERSEY_CONFIG_TEST_MEMLEAK_TIMEOUT = "jersey.config.test.memleak.timeout";

    /**
     * The context root where the deployed application will be accessible.
     */
    public static final String JERSEY_CONFIG_TEST_CONTAINER_CONTEXT_ROOT = "jersey.config.test.container.contextRoot";

    /**
     * The path where to create heap dump files.
     */
    public static final String JERSEY_CONFIG_TEST_MEMLEAK_HEAP_DUMP_PATH = "jersey.config.test.memleak.heapDumpPath";

    private MemoryLeakUtils() {
    }

    private static final Pattern PATTERN = Pattern.compile(".*java\\.lang\\.OutOfMemoryError.*");

    /**
     * Scans the file denoted by {@link #JERSEY_CONFIG_TEST_CONTAINER_LOGFILE} for {@link OutOfMemoryError} records.
     *
     * @throws IOException           In case of I/O error.
     * @throws IllegalStateException In case the {@link OutOfMemoryError} record was found.
     */
    public static void verifyNoOutOfMemoryOccurred() throws IOException {

        final String logFileName = System.getProperty(JERSEY_CONFIG_TEST_CONTAINER_LOGFILE);
        System.out.println("Verifying whether OutOfMemoryError occurred in log file: " + logFileName);

        if (logFileName == null) {
            return;
        }
        final File logFile = new File(logFileName);
        if (!logFile.exists()) {
            return;
        }

        final List<String> lines = Files.readLines(logFile, Charset.defaultCharset(), new LineProcessor<List<String>>() {
            private List<String> matchedLines = new LinkedList<>();

            @Override
            public boolean processLine(final String line) throws IOException {
                if (PATTERN.matcher(line).matches()) {
                    matchedLines.add(line);
                }
                return true;
            }

            @Override
            public List<String> getResult() {
                return matchedLines;
            }
        });

        if (lines.size() > 0) {
            throw new IllegalStateException(
                    "OutOfMemoryError detected in '" + logFileName + "': " + Arrays.toString(lines.toArray()));
        }
    }

    /**
     * The name of the HotSpot Diagnostic MXBean
     */
    private static final String HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";

    /**
     * The class name of HotSpot Diagnostic MXBean
     */
    private static final String HOT_SPOT_DIAGNOSTIC_MXBEAN_CLASSNAME = "com.sun.management.HotSpotDiagnosticMXBean";

    /**
     * Hotspot diagnostic MBean singleton
     */
    private static volatile Object hotSpotDiagnosticMBean;

    private static volatile Method dumpHeapMethod;

    /**
     * Create a heap dump into a given file.
     *
     * @param fileName name of the heap dump file
     * @param live     whether to dump only the live objects
     */
    static void dumpHeap(String fileName, boolean live)
            throws InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException, IOException {
        conditionallyInitHotSpotDiagnosticMXBean();
        try {
            java.nio.file.Files.deleteIfExists(Paths.get(fileName));
        } catch (IOException e) {
            // do nothing and try to go further
        }
        dumpHeapMethod.invoke(hotSpotDiagnosticMBean, fileName, live);
    }

    /**
     * Initialize the HotSpot diagnostic MBean
     */
    private static void conditionallyInitHotSpotDiagnosticMXBean()
            throws IOException, ClassNotFoundException, NoSuchMethodException {
        if (hotSpotDiagnosticMBean == null) {
            synchronized (MemoryLeakUtils.class) {
                if (hotSpotDiagnosticMBean == null) {
                    Class clazz = Class.forName(HOT_SPOT_DIAGNOSTIC_MXBEAN_CLASSNAME);
                    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                    hotSpotDiagnosticMBean = ManagementFactory.newPlatformMXBeanProxy(server, HOTSPOT_BEAN_NAME, clazz);
                    dumpHeapMethod = clazz.getMethod("dumpHeap", String.class, boolean.class);
                }
            }
        }
    }
}
