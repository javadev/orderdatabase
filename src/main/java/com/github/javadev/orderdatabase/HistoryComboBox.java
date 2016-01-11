package com.github.javadev.orderdatabase;

public class HistoryComboBox extends javax.swing.JComboBox {
    public HistoryComboBox() {
        setEditable(true);
        addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent actionevent) {
                Object obj = getSelectedItem();
                if (obj != null && actionevent.getActionCommand().equals("comboBoxEdited")) {
                    addToList(obj.toString());
                }
            }
        });
    }

    public void addToList(String string) {
        if ("".equals(string)) {
            removeItem(string);
            return;
        }
        int index;
        for (index = 0; index < getItemCount(); index += 1) {
            if (string.equals(getItemAt(index))) {
                if (index > 0) {
                    javax.swing.DefaultComboBoxModel model = (javax.swing.DefaultComboBoxModel) getModel();
                    model.insertElementAt(model.getElementAt(index), 0);
                    model.removeElementAt(index + 1);
                    model.setSelectedItem(model.getElementAt(0));
                  }
                  return;
            }
        }
        if (index > 20) {
           removeItemAt(index - 1);
        }
        insertItemAt(string, 0);
        getEditor().setItem(string);
        setSelectedItem(string);
    }
}
