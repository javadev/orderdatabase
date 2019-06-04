package com.github.javadev.orderdatabase;

import com.github.underscore.BiFunction;
import com.github.underscore.Function;
import com.github.underscore.Predicate;
import com.github.underscore.Supplier;
import com.github.underscore.lodash.U;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.table.AbstractTableModel;

public class NewJDialog9 extends javax.swing.JDialog {
    private final Supplier<List<Map<String, Object>>> databaseDataFunc;
    private boolean isApproved;

    public NewJDialog9(java.awt.Frame parent, boolean modal,
                       Supplier<List<Map<String, Object>>> databaseDataFunc) {
        super(parent, modal);
        initComponents();
        this.databaseDataFunc = databaseDataFunc;
        jTable1.setModel(new MyModel(getDatabaseMetrics()));
    }

    private static class MyModel extends AbstractTableModel {

        private static final String[] columnNames = {"Дата создания", "Число созданных заявок",
            "Число оплаченных заявок", "Подробности"};
        private final List<Map<String, Object>> list;

        private MyModel(List<Map<String, Object>> list) {
            this.list = list;
        }
        
        Class[] types = new Class [] {
            java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
        };
        boolean[] canEdit = new boolean [] {
            false, false, false, false
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
                    return list.get(rowIndex).get("summaryCreated");
                case 1:
                    return list.get(rowIndex).get("countCreated");
                case 2:
                    return list.get(rowIndex).get("countPayed");
                case 3:
                    return list.get(rowIndex).get("summary");
            }
            return null;
        }
    }
    
    public boolean isApproved() {
        return isApproved;
    }
    
    public List<Map<String, Object>> getDatabaseMetrics() {
        Map<String, List<Map<String, Object>>> groupedByOrderNumber = U.chain(databaseDataFunc.get())
            .sortBy(new Function<Map<String, Object>, Long>() {
                public Long apply(Map<String, Object> arg) {
                    return (Long) arg.get("created");
                }
            })
            .groupBy(new Function<Map<String, Object>, String>() {
                @Override
                public String apply(Map<String, Object> arg) {
                    return (String) arg.get("orderNumber");
                }
            })
            .item();
        Map<String, List<Map.Entry<String, List<Map<String, Object>>>>> groupedByDate =
            U.groupBy(groupedByOrderNumber.entrySet(),
                new Function<Map.Entry<String, List<Map<String, Object>>>, String>() {
                @Override
                public String apply(Map.Entry<String, List<Map<String, Object>>> arg) {
                    return new SimpleDateFormat("dd.MM.yyyy").format(
                            new Date((Long) arg.getValue().get(0).get("created")));
                }
            });
        final List<Map<String, Object>> createdOrders =
        U.chain(groupedByDate.entrySet())
                .map(new Function<Map.Entry<String, List<Map.Entry<String, List<Map<String, Object>>>>>,
                Map<String, Object>>() {
            public Map<String, Object> apply(final Map.Entry<String, List<Map.Entry<String, List<Map<String, Object>>>>> arg) {
                List<Map.Entry<String, List<Map<String, Object>>>> payedOrders =
                    U.filter(arg.getValue(), new Predicate<Map.Entry<String, List<Map<String, Object>>>>() {
                        public boolean test(Map.Entry<String, List<Map<String, Object>>> arg) {
                            String status = (String) U.last(arg.getValue()).get("status");
                            return status != null && status.equals("оплачено");
                        }
                    });
                Map<String, Integer> userCreated = groupByUsers(arg.getValue());
                final Map<String, Integer> userPayed = groupByUsers(payedOrders);
                Set<String> userSummary = U.map(userCreated.entrySet(), new Function<Map.Entry<String, Integer>, String>() {
                    @Override
                    public String apply(final Map.Entry<String, Integer> user) {
                        String summary = user.getKey() + " (" + user.getValue() + ", ";
                        if (userPayed.get(user.getKey()) != null) {
                            summary += userPayed.get(user.getKey());
                        } else {
                            summary += "0";
                        }
                        return summary + ")";
                    }
                });
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("summaryCreated", arg.getKey());
                map.put("countCreated", arg.getValue().size());
                map.put("countPayed", payedOrders.size());
                map.put("summary", userSummary.toString().replace("[", "").replace("]", ""));
                return map;
            }
        }).value();
        return createdOrders;
    }
    
    private Map<String, Integer> groupByUsers(List<Map.Entry<String, List<Map<String, Object>>>> orders) {
        return U.reduce(orders, new BiFunction<Map<String, Integer>, Map.Entry<String, List<Map<String, Object>>>, Map<String, Integer>>() {
            public Map<String, Integer> apply(Map<String, Integer> accum, Map.Entry<String, List<Map<String, Object>>> arg) {
                String user = (String) U.first(arg.getValue()).get("user");
                if (user != null && !user.isEmpty()) {
                    if (!accum.containsKey(user)) {
                        accum.put(user, 1);
                    } else {
                        accum.put(user, accum.get(user) + 1);
                    }
                }
                return accum;
            }
        }, new LinkedHashMap<String, Integer>());
    }
    
    private List<Long> getColumnWidth(javax.swing.JTable jTable) {
        int columnCount = jTable.getColumnModel().getColumnCount();
        List<Long> result = new ArrayList<>();
        for (int index = 0; index < columnCount; index += 1) {
            result.add(Long.valueOf(jTable.getColumnModel().getColumn(index).getPreferredWidth()));
        }
        return result;
    }

    private void setColumnWidth(javax.swing.JTable jTable, List<Long> columnWidth) {
        int columnCount = jTable.getColumnModel().getColumnCount();
        for (int index = 0; index < columnCount; index += 1) {
            if (index < columnWidth.size()) {
                jTable.getColumnModel().getColumn(index).setPreferredWidth(columnWidth.get(index).intValue());
            }
        }
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
        jButton5 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jButton1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Статистика использования");

        jLabel1.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jLabel1.setText("Статистика использования");

        jButton5.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jButton5.setText("ОК");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
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
                "Дата создания", "Число созданных заявок", "Число оплаченных заявок", "Подробности"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
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

        jButton1.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jButton1.setText("Обновить");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 387, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jButton1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jButton5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(42, 42, 42)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel1)
                    .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE)
                    .add(jButton1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jButton5)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        setVisible(false);
        isApproved = true;
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        List<Long> columns = getColumnWidth(jTable1);
        jTable1.setModel(new MyModel(getDatabaseMetrics()));
        setColumnWidth(jTable1, columns);
    }//GEN-LAST:event_jButton1ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton5;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
