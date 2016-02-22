package com.github.javadev.orderdatabase;

import com.github.underscore.lodash.$;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

public class NewJDialog7 extends javax.swing.JDialog {
    private final List<Map<String, Object>> userData;
    private boolean isApproved;

    public NewJDialog7(java.awt.Frame parent, boolean modal,
            List<Map<String, Object>> userData) {
        super(parent, modal);
        initComponents();
        this.userData = (List<Map<String, Object>>) (userData == null ?
                new ArrayList<>() : (List<Map<String, Object>>) $.clone(userData));
        jTable1.setModel(new MyModel(this.userData));
        jTable1.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent evt) {
                if (!evt.getValueIsAdjusting()) {
                    int index = ((DefaultListSelectionModel) evt.getSource()).getAnchorSelectionIndex();
                    if (index >= 0) {
                        jButton2.setEnabled(true);
                        jButton3.setEnabled(true);
                        if (NewJDialog7.this.userData.size() <= 1) {
                            jButton3.setEnabled(false);
                            jButton4.setEnabled(false);
                        } else {
                            jButton4.setEnabled(true);
                            jButton5.setEnabled(true);
                        }
                    }
                }
            }
        });
    }

    private static class MyModel extends AbstractTableModel {

        private static final String[] columnNames = {"Дата создания", "Дата изменения", "Имя пользователя", "Разрешён"};
        private final List<Map<String, Object>> list;

        private MyModel(List<Map<String, Object>> list) {
            this.list = list;
        }
        
        Class[] types = new Class [] {
            java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class
        };
        boolean[] canEdit = new boolean [] {
            false, false, false, true
        };

        @Override
        public Class getColumnClass(int columnIndex) {
            return types [columnIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return canEdit[columnIndex];
        }


        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 3) {
                Map<String, Object> newRow = (Map<String, Object>) $.clone((Map<String, Object>) list.get(row));
                newRow.put("active", value);
                list.set(row, newRow);
                fireTableCellUpdated(row, 2);
            }
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int index) {
            return columnNames[index];
        }

        @Override
        public int getRowCount() {
            return list.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch(columnIndex) {
                case 0:
                    if (list.get(rowIndex).get("created") != null) {
                        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(
                            new Date((Long) list.get(rowIndex).get("created")));
                    }
                    return "";
                case 1:
                    if (list.get(rowIndex).get("passChanged") != null) {
                        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(
                            new Date((Long) list.get(rowIndex).get("passChanged")));
                    }
                    return "";
                case 2:
                    return list.get(rowIndex).get("login");
                case 3:
                    return $.isBoolean(list.get(rowIndex).get("active"))
                            ? (Boolean) list.get(rowIndex).get("active") : Boolean.TRUE;
            }
            return null;
        }
    }
    
    private void swapElements(int from, int to) {
        Object aObject = userData.get(from);
        Object bObject = userData.get(to);
        userData.remove(from);
        userData.add(from, (Map<String, Object>) bObject);
        userData.remove(to);
        userData.add(to, (Map<String, Object>) aObject);
    }
    
    public boolean isApproved() {
        return isApproved;
    }
    
    public List<Map<String, Object>> getNewModel() {
        return userData;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Список пользователей");
        setResizable(false);

        jLabel1.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jLabel1.setText("Список пользователей");

        jButton1.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jButton1.setText("Добавить ...");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jButton2.setText("Удалить");
        jButton2.setEnabled(false);
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jButton3.setText("Переместить вверх");
        jButton3.setEnabled(false);
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton4.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jButton4.setText("Переместить вниз");
        jButton4.setEnabled(false);
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jButton5.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jButton5.setText("ОК");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jButton6.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jButton6.setText("Отмена");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        jTable1.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Дата создания", "Дата изменения", "Имя пользователя", "Разрешён"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(jTable1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jButton5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 136, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jButton6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 136, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 598, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jButton1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 187, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jButton2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 187, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jButton3)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jButton4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 187, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(12, 12, 12))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(42, 42, 42)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED))
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                        .add(layout.createSequentialGroup()
                            .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED))
                        .add(layout.createSequentialGroup()
                            .add(jButton1)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                            .add(jButton2)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                            .add(jButton3)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                            .add(jButton4)
                            .add(101, 101, 101))))
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButton5)
                    .add(jButton6))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        layout.linkSize(new java.awt.Component[] {jButton1, jButton2, jButton3, jButton4}, org.jdesktop.layout.GroupLayout.VERTICAL);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        setVisible(false);
        isApproved = true;
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        setVisible(false);
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        int selectedIndex = jTable1.getSelectionModel().getAnchorSelectionIndex();
        this.userData.remove(selectedIndex);
        jTable1.setModel(new MyModel(this.userData));
        if (userData.size() > 0) {
            int newSelectedIndex = Math.min(selectedIndex, userData.size() - 1);
            jTable1.setRowSelectionInterval(newSelectedIndex, newSelectedIndex);
        } else {
            jButton2.setEnabled(false);
        }
        if (userData.size() <= 1) {
            jButton3.setEnabled(false);
            jButton4.setEnabled(false);
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        int moveMe = jTable1.getSelectionModel().getAnchorSelectionIndex();
        if (moveMe != 0) {
            swapElements(moveMe, moveMe - 1);
            jTable1.setRowSelectionInterval(moveMe - 1, moveMe - 1);
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        int moveMe = jTable1.getSelectionModel().getAnchorSelectionIndex();
        if (moveMe != userData.size() - 1) {
            swapElements(moveMe, moveMe + 1);
            jTable1.setRowSelectionInterval(moveMe + 1, moveMe + 1);
        }
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        final NewJDialog8 dialog = new NewJDialog8((java.awt.Frame) getOwner(), true);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        if (dialog.isApproved()) {
            userData.add(new LinkedHashMap<String, Object>() { {
                put("created", new Date().getTime());
                put("login", dialog.getLogin());
                put("pass", dialog.getPass());
                put("active", true);
            } });
            int row = jTable1.getSelectedRow();
            jTable1.setModel(new MyModel(this.userData));
            if (row >= 0 && row < this.userData.size()) {
                jTable1.setRowSelectionInterval(row, row);
            }
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
