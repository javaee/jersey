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

package org.glassfish.jersey.examples.aggregator;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.ActionMap;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

/**
 * Main data aggregator client application UI window.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class MainWindow extends javax.swing.JFrame {

    private final DefaultListModel keywordListModel;
    private final AtomicBoolean receiveMessages;
    private final List<DataAggregator> dataAggregators;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JLabel connectionStatusLabel;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JList keywordList;
    private javax.swing.JTextArea messagesArea;
    private javax.swing.JTextField newKeywordField;
    private javax.swing.JButton startStopButton;
    private javax.swing.JCheckBox testCheckbox;
    private javax.swing.JLabel testColorLabel;
    private javax.swing.JCheckBox twitterCheckbox;
    private javax.swing.JLabel twitterColorLabel;
    // End of variables declaration//GEN-END:variables

    /**
     * Creates new form MessageAggregator
     */
    public MainWindow() {
        keywordListModel = new DefaultListModel();
        receiveMessages = new AtomicBoolean(false);

        initComponents();

        // Bind "remove selected list items" action to DELETE key pressed in keyword list.
        ActionMap actionMap = keywordList.getActionMap();
        InputMap inputMap = keywordList.getInputMap(JComponent.WHEN_FOCUSED);

        final String actionKey = "RemoveSelectedListItems";
        actionMap.put(actionKey, new RemoveSelectedListItemsAction(keywordList, keywordListModel));
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), actionKey);

        dataAggregators = new ArrayList<DataAggregator>(2);
    }

    private void initAggregators() {
        if (twitterCheckbox.isSelected()) {
            dataAggregators.add(new TwitterAggregator(colorToString(twitterColorLabel.getBackground())));
        }
        if (testCheckbox.isSelected()) {
            dataAggregators.add(new TestAggregatorJersey(colorToString(testColorLabel.getBackground())));
            dataAggregators.add(new TestAggregatorJaxRs(colorToString(testColorLabel.getBackground())));
        }
    }

    private String colorToString(Color color) {
        StringBuilder colorBuilder = new StringBuilder();

        colorBuilder
                .append(String.format("%02X", color.getRed()))
                .append(String.format("%02X", color.getGreen()))
                .append(String.format("%02X", color.getBlue()));

        return colorBuilder.toString();
    }

    private void onKeywordEntered(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onKeywordEntered
        final String keyword = newKeywordField.getText();
        if (keyword == null || keyword.isEmpty() || keywordListModel.contains(keyword)) {
            return;
        }
        addButton.doClick();
    }//GEN-LAST:event_onKeywordEntered

    private void onAdd(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onAdd
        final String keyword = newKeywordField.getText();
        if (keyword == null || keyword.isEmpty() || keywordListModel.contains(keyword)) {
            return;
        }

        keywordListModel.addElement(keyword);
        newKeywordField.setText("");
    }//GEN-LAST:event_onAdd

    private void onStartOrStop(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onStartOrStop
        if (receiveMessages.get()) {
            // stop
            receiveMessages.set(false);
            startStopButton.setText("Start");
            newKeywordField.setEnabled(true);
            addButton.setEnabled(true);
            stop();
        } else {
            // start
            receiveMessages.set(true);
            startStopButton.setText("Stop");
            connectionStatusLabel.setText("Connecting...");
            newKeywordField.setEnabled(false);
            addButton.setEnabled(false);
            messagesArea.setText("");

            StringBuilder keywordsBuilder = new StringBuilder();
            final Enumeration<?> elements = keywordListModel.elements();
            while (elements.hasMoreElements()) {
                keywordsBuilder.append(elements.nextElement());
                if (elements.hasMoreElements()) {
                    keywordsBuilder.append(",");
                }
            }

            start(keywordsBuilder.toString(), new DataListener() {
                @Override
                public void onStart() {
                    EventQueue.invokeLater(() -> connectionStatusLabel.setText("Connected."));
                }

                @Override
                public void onMessage(final Message message) {
                    EventQueue.invokeLater(() -> messagesArea.append(message.getText() + "\n\n"));
                }

                @Override
                public void onError() {
                    EventQueue.invokeLater(() -> {
                        connectionStatusLabel.setText("Connection Error!");
                        receiveMessages.set(false);
                        startStopButton.setText("Start");
                        newKeywordField.setEnabled(true);
                        addButton.setEnabled(true);
                    });
                }

                @Override
                public void onComplete() {
                    EventQueue.invokeLater(() -> connectionStatusLabel.setText("Disconnected."));
                }
            });
        }

    }//GEN-LAST:event_onStartOrStop

    private void stop() {
        for (DataAggregator dataAggregator : dataAggregators) {
            dataAggregator.stop();
        }
        dataAggregators.clear();
    }

    private void start(String keywords, DataListener listener) {
        initAggregators();

        for (DataAggregator dataAggregator : dataAggregators) {
            dataAggregator.start(keywords, listener);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Create and display the form */
        EventQueue.invokeLater(() -> {
            final MainWindow messageAggregator = new MainWindow();
            messageAggregator.setLocationRelativeTo(null);
            messageAggregator.setVisible(true);
        });
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JPanel jPanel3 = new javax.swing.JPanel();
        newKeywordField = new javax.swing.JTextField();
        addButton = new javax.swing.JButton();
        javax.swing.JPanel jPanel1 = new javax.swing.JPanel();
        connectionStatusLabel = new javax.swing.JLabel();
        startStopButton = new javax.swing.JButton();
        javax.swing.JScrollPane jScrollPane2 = new javax.swing.JScrollPane();
        messagesArea = new javax.swing.JTextArea();
        javax.swing.JPanel jPanel2 = new javax.swing.JPanel();
        javax.swing.JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
        keywordList = new javax.swing.JList();
        jPanel4 = new javax.swing.JPanel();
        twitterCheckbox = new javax.swing.JCheckBox();
        twitterColorLabel = new javax.swing.JLabel();
        testColorLabel = new javax.swing.JLabel();
        testCheckbox = new javax.swing.JCheckBox();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Message aggregator");
        setMinimumSize(new java.awt.Dimension(304, 300));

        jPanel3.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        newKeywordField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onKeywordEntered(evt);
            }
        });

        addButton.setText("Add");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onAdd(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
                jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(newKeywordField)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(addButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 72,
                                        org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
                jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(newKeywordField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
                                                org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
                                                org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(addButton))
                                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        startStopButton.setText("Start");
        startStopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onStartOrStop(evt);
            }
        });

        messagesArea.setColumns(20);
        messagesArea.setLineWrap(true);
        messagesArea.setRows(5);
        messagesArea.setWrapStyleWord(true);
        messagesArea.setFocusable(false);
        jScrollPane2.setViewportView(messagesArea);

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(jPanel1Layout.createSequentialGroup()
                                                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 247,
                                                        Short.MAX_VALUE)
                                                .addContainerGap())
                                        .add(jPanel1Layout.createSequentialGroup()
                                                .add(connectionStatusLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
                                                        org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(startStopButton))))
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(startStopButton)
                                        .add(connectionStatusLabel))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jScrollPane2)
                                .addContainerGap())
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        keywordList.setModel(keywordListModel);
        jScrollPane1.setViewportView(keywordList);

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 97, Short.MAX_VALUE)
                                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 332, Short.MAX_VALUE)
                                .addContainerGap())
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        twitterCheckbox.setSelected(true);
        twitterCheckbox.setText("Twitter aggregator");

        twitterColorLabel.setBackground(new java.awt.Color(85, 170, 0));
        twitterColorLabel.setText("    ");
        twitterColorLabel.setOpaque(true);

        testColorLabel.setBackground(new java.awt.Color(0, 85, 170));
        testColorLabel.setText("    ");
        testColorLabel.setOpaque(true);

        testCheckbox.setText("Test Aggregator");

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
                jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel4Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup()
                                                .add(twitterColorLabel)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(twitterCheckbox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
                                                        org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup()
                                                .add(testColorLabel)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(testCheckbox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
                                                        org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
                jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel4Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(twitterCheckbox)
                                        .add(twitterColorLabel))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(testCheckbox)
                                        .add(testColorLabel))
                                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(layout.createSequentialGroup()
                                .addContainerGap()
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
                                                org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .add(jPanel4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
                                                org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .add(jPanel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
                                                org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
                                        org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(layout.createSequentialGroup()
                                .addContainerGap()
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
                                                org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .add(layout.createSequentialGroup()
                                                .add(jPanel4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
                                                        org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
                                                        org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
                                                        org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
                                                        org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
                                                        org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
}
