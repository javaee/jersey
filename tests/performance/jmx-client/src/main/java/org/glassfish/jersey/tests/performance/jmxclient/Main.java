/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.performance.jmxclient;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * JMX Client entry point.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class Main {

    public static void main(String[] args) throws Exception {

//      e.g. "service:jmx:rmi:///jndi/rmi://sysifos.cz.oracle.com:11112/jmxrmi"
        final String jmxUrl = args[0];
//      e.g. "org.glassfish.jersey.test.performance.interceptor.dynamic:type=DynamicallyBoundInterceptorResource,name=gets"
        final String mBeanName = args[1];
//      e.g. "OneMinuteRate"
        final String mBeanAttrName = args[2];
//      e.g. 50
        final int sampleCount = Integer.parseInt(args[3]);
//      e.g. "phishing.properties"
        final String propertiesFile = args[4];

        System.out.printf("JMX URL = %s\nMBean = %s\nattribute = %s\nsamples = %d\nfilename = %s\n"
                + "Going to connect...\n",
                jmxUrl, mBeanName, mBeanAttrName, sampleCount, propertiesFile);

        final JMXServiceURL url = new JMXServiceURL(jmxUrl);
        final JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
        final MBeanServerConnection mBeanServer = jmxc.getMBeanServerConnection();
        final ObjectName mBeanObjectName = new ObjectName(mBeanName);

        System.out.println("Connected...");

        double totalSum = 0;
        int samplesTaken = 0;

        for (int i = 0; i < sampleCount; i++) {
            Thread.sleep(5000);

            Double sample = (Double) mBeanServer.getAttribute(mBeanObjectName, mBeanAttrName);

            System.out.printf("OMR[%d]=%f\n", i, sample);

            totalSum += sample;
            samplesTaken++;
        }

        jmxc.close();

        final double result = totalSum / samplesTaken;
        writeResult(result, propertiesFile);

        System.out.printf("\nAverage=%f\n", result);
    }

    private static void writeResult(double resultValue, String propertiesFile) throws IOException {
        Properties resultProps = new Properties();
        resultProps.put("YVALUE", Double.toString(resultValue));
        resultProps.store(new FileOutputStream(propertiesFile), null);
    }
}
