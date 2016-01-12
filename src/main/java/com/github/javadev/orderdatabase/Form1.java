package com.github.javadev.orderdatabase;

import com.github.underscore.Function1;
import com.github.underscore.Predicate;
import com.github.underscore.lodash.$;
import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.awt.KeyboardFocusManager;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.JTextComponent;

public class Form1 extends javax.swing.JFrame {
    private final Map<String, Object> database = new LinkedHashMap<String, Object>();
    private final List<Map<String, Object>> foundOrders = new ArrayList<Map<String, Object>>();
    private final JFileChooser chooser1 = new JFileChooser();
    private final NewJDialog1 dossieDialog;
    private final Locale localeRu = new Locale("ru", "RU");
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> taskHandle;
    private String hostName;
    private String dbName;
    private String user;
    private String pass;
    
    public Form1() {
        initComponents();
        Path path = Paths.get("./database.json");
        if (Files.exists(path)) {
            try {
                database.putAll((Map<String, Object>) $.fromJson(
                    new String(Files.readAllBytes(path), "UTF-8")));
            } catch (IOException ex) {
                Logger.getLogger(Form1.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent winEvt) {
                Map<String, Object> data = createOrderData();
                if (database.get("currentOrder") != null
                        && !$.omit(data, "_id", "created", "status", "user").toString().equals(
                        $.omit((Map<String, Object>) database.get("currentOrder"), "_id", "created", "status", "user").toString())) {
                    saveData(data);
                } else {
                    saveData(null);
                }
            }
        });
        fillComboBoxModel("paymentMethodData", jComboBox1);
        fillComboBoxModel("deliveryMethodData", jComboBox2);
        fillComboBoxModel("statusData", jComboBox3);
        fillComboBoxModel("searchData", jComboBox4);
        ((JTextComponent) jComboBox4.getEditor().getEditorComponent()).setText((String) database.get("searchDataText"));
        if ($.isNumber(database.get("periodIndex"))) {
            jComboBox5.setSelectedIndex(((Long) database.get("periodIndex")).intValue());
        }
        if ($.isNumber(database.get("autoLoadIndex"))) {
            jComboBox6.setSelectedIndex(((Long) database.get("autoLoadIndex")).intValue());
        }
        final java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        if (database.get("locationX") instanceof Long && database.get("locationY") instanceof Long) {
            setLocation(Math.min(screenSize.width - 50, ((Long) database.get("locationX")).intValue()),
                    Math.min(screenSize.height - 50, ((Long) database.get("locationY")).intValue()));
        }
        hostName = (String) database.get("hostName");
        dbName = (String) database.get("dbName");
        user = (String) database.get("user");
        pass = database.get("pass") == null ? null
                : new String(xor(((String) database.get("pass")).getBytes(), "UTF-8".getBytes()));

        dossieDialog = new NewJDialog1(this, "Досье покупателя", false, database);
        dossieDialog.setLocationRelativeTo(this);
        List<Map<String, Object>> filteredOrders = getFilteredOrders(getDatabaseData());
        if (!filteredOrders.isEmpty()) {
            fillOrderForm($.last(filteredOrders));
        }
        jTable1.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent evt) {
                if (!evt.getValueIsAdjusting()) {
                    int index = ((DefaultListSelectionModel) evt.getSource()).getAnchorSelectionIndex();
                    if (index >= 0) {
                        fillOrderForm(foundOrders.get(index));
                    }
                }
            }
        });
        chooser1.setAcceptAllFileFilterUsed(false);
        chooser1.addChoosableFileFilter(new FileNameExtensionFilter("Xml file", "xml"));
        chooser1.addChoosableFileFilter(new FileNameExtensionFilter("Json file", ".json")); 
        chooser1.setSelectedFile(new File("search-result.xml"));
        jTextField1.requestFocusInWindow();
        JTextComponent editor = (JTextComponent) jComboBox4.getEditor().getEditorComponent();
        editor.setNextFocusableComponent(jComboBox4.getNextFocusableComponent());
        editor.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
               focusNextElementOnPressEnter(evt); 
            }
            public void keyTyped(KeyEvent evt) {
                java.awt.EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        searchOrders();
                    }
                });
            }
        });
    }

    private void fillComboBoxModel(String key, JComboBox jComboBox) {
        final List<String> databaseData;
        if (database.get(key) == null || !(database.get(key) instanceof List)) {
            databaseData = new ArrayList<String>();
        } else {
            databaseData = (List<String>) database.get(key);
        }
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for (String data : databaseData) {
            model.addElement(data);
        }
        jComboBox.setModel(model);
    }

    private void fillOrderForm(Map<String, Object> order) {
        database.put("currentOrder", order);
        dossieDialog.reload();
        jTextField1.setText((String) order.get("orderNumber"));
        jTextField2.setText((String) order.get("firstName"));
        jTextField3.setText((String) order.get("middleName"));
        jTextField4.setText((String) order.get("surname"));
        jTextField5.setText((String) order.get("phoneNumber"));
        jTextField6.setText((String) order.get("email"));
        fillComboBoxSelectedItem(jComboBox1, (String) order.get("paymentMethod"), "paymentMethodData");
        fillComboBoxSelectedItem(jComboBox2, (String) order.get("deliveryMethod"), "deliveryMethodData");
        fillComboBoxSelectedItem(jComboBox3, (String) order.get("status"), "statusData");
        jTextField9.setText((String) order.get("city"));
        jTextField10.setText((String) order.get("street"));
        jTextField11.setText((String) order.get("houseNumber"));
        jTextField12.setText((String) order.get("houseNumber2"));
        jTextField13.setText((String) order.get("appartmentNumber"));
        jTextPane1.setText((String) order.get("comment"));
        jLabel25.setText((String) order.get("_id"));
    }
    
    private void fillComboBoxSelectedItem(JComboBox jComboBox, String data, String dictKey) {
        if (jComboBox.getModel() == null) {
            return;
        }
        if (data == null) {
            jComboBox.getModel().setSelectedItem(null);
            return;
        }
        String modelElement = null;
        fillComboBoxModel(dictKey, jComboBox);
        for (int index = 0; index < jComboBox.getModel().getSize(); index += 1) {
            if (jComboBox.getModel().getElementAt(index).equals(data)) {
                modelElement = (String) jComboBox.getModel().getElementAt(index);
                break;
            }
        }
        if (modelElement == null) {
            ((DefaultComboBoxModel) jComboBox.getModel()).addElement(data);
            jComboBox.getModel().setSelectedItem(data);
        } else {
            jComboBox.getModel().setSelectedItem(modelElement);
        }
    }

    private List<Map<String, Object>> getDatabaseData() {
        if (database.get("data") == null) {
            database.put("data", new ArrayList<Map<String, Object>>());
        }
        Set<String> ids = new HashSet<String>();
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) database.get("data");
        for (Map<String, Object> data : dataList) {
            ids.add((String) data.get("_id"));
        }
        List<Map<String, Object>> dbDataList = new DatabaseService(hostName, dbName, user, pass).readAll();
        for (Map<String, Object> data : dbDataList) {
            if (!ids.contains((String) data.get("_id"))) {
                dataList.add(data);
            }
        }
        return dataList;
    }

    private void saveDatabaseData() {
        DatabaseService databaseService = new DatabaseService(hostName, dbName, user, pass);
        Set<String> ids = new HashSet<String>();
        List<Map<String, Object>> dbDataList = databaseService.readAll();
        for (Map<String, Object> data : dbDataList) {
            ids.add((String) data.get("_id"));
        }
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) database.get("data");
        List<Map<String, Object>> dataToSaveList = new ArrayList<>();
        for (Map<String, Object> data : dataList) {
            if (!ids.contains((String) data.get("_id"))) {
                dataToSaveList.add(data);
            }
        }
        databaseService.insertData(dataToSaveList);
    }

    private List<Map<String, Object>> getFilteredOrders(List<Map<String, Object>> databaseData) {
        List<Map<String, Object>> filteredOrders = new ArrayList<Map<String, Object>>();
        Map<String, List<Map<String, Object>>> orders =
                $.groupBy(databaseData, 
                new Function1<Map<String, Object>, String>() {
                    public String apply(Map<String, Object> item) {
                        return (String) item.get("orderNumber");
                    }
                });
        for (Map.Entry<String, List<Map<String, Object>>> entry : orders.entrySet()) {
            List<Map<String, Object>> sorted = $.sortBy(entry.getValue(),
               new Function1<Map<String, Object>, Long>() {
               public Long apply(final Map<String, Object> item) {
                   return item.get("created") == null ? 0L : (Long) item.get("created");
               } 
            });
            if (!sorted.isEmpty()) {
                filteredOrders.add($.last(sorted));
            }
        }
        $.sortBy(filteredOrders, new Function1<Map<String, Object>, Long>() {
            public Long apply(Map<String, Object> item) {
                return item.get("created") == null ? 0L : (Long) item.get("created");
            }
        });
        return filteredOrders;
    }
    
    private Map<String, Object> createOrderData() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("created", new Date().getTime());
        data.put("_id", uniqueId());
        data.put("orderNumber", jTextField1.getText().trim());
        data.put("firstName", jTextField2.getText().trim());
        data.put("middleName", jTextField3.getText().trim());
        data.put("surname", jTextField4.getText().trim());
        data.put("phoneNumber", jTextField5.getText().trim());
        data.put("email", jTextField6.getText().trim());
        data.put("paymentMethod", jComboBox1.getSelectedItem() == null ? null
                : String.valueOf(jComboBox1.getSelectedItem()).trim());
        data.put("deliveryMethod", jComboBox2.getSelectedItem() == null ? null
                : String.valueOf(jComboBox2.getSelectedItem()).trim());
        data.put("status", jComboBox3.getSelectedItem() == null ? null
                : String.valueOf(jComboBox3.getSelectedItem()).trim());
        data.put("city", jTextField9.getText().trim());
        data.put("street", jTextField10.getText().trim());
        data.put("houseNumber", jTextField11.getText().trim());
        data.put("houseNumber2", jTextField12.getText().trim());
        data.put("appartmentNumber", jTextField13.getText().trim());
        data.put("comment", jTextPane1.getText().trim());
        data.put("user", user);
        return data;
    }
    
    private void saveData(Map<String, Object> data) {
        if (database.get("data") == null) {
            database.put("data", new ArrayList<Map<String, Object>>());
        }
        if (data != null) {
            ((List<Map<String, Object>>) database.get("data")).add(data);
            saveDatabaseData();
            jLabel25.setText((String) data.get("_id"));
            database.put("currentOrder", data);
        }
        dossieDialog.reload();
        database.put("paymentMethodData", new ArrayList<String>());
        for (int index = 0; index < jComboBox1.getModel().getSize(); index += 1) {
            ((List<String>) database.get("paymentMethodData")).add(
                String.valueOf(jComboBox1.getModel().getElementAt(index)).trim());
        }
        database.put("deliveryMethodData", new ArrayList<String>());
        for (int index = 0; index < jComboBox2.getModel().getSize(); index += 1) {
            ((List<String>) database.get("deliveryMethodData")).add(
                String.valueOf(jComboBox2.getModel().getElementAt(index)).trim());
        }
        database.put("statusData", new ArrayList<String>());
        for (int index = 0; index < jComboBox3.getModel().getSize(); index += 1) {
            ((List<String>) database.get("statusData")).add(
                String.valueOf(jComboBox3.getModel().getElementAt(index)).trim());
        }
        database.put("searchData", new ArrayList<String>());
        for (int index = 0; index < jComboBox4.getModel().getSize(); index += 1) {
            ((List<String>) database.get("searchData")).add(
                String.valueOf(jComboBox4.getModel().getElementAt(index)).trim());
        }
        database.put("searchDataText", ((JTextComponent) jComboBox4.getEditor().getEditorComponent()).getText().trim());
        database.put("periodIndex", jComboBox5.getSelectedIndex());
        database.put("autoLoadIndex", jComboBox6.getSelectedIndex());
        database.put("locationX", getLocation().x);
        database.put("locationY", getLocation().y);
        database.put("hostName", hostName);
        database.put("dbName", dbName);
        database.put("user", user);
        database.put("pass", pass == null ? null : new String(xor(pass.getBytes(), "UTF-8".getBytes())));
        try {
            Files.write(Paths.get("./database.json"), $.toJson(database).getBytes("UTF-8"));
        } catch (IOException ex) {
            Logger.getLogger(Form1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static byte[] xor(final byte[] input, final byte[] secret) {
        final byte[] output = new byte[input.length];
        if (secret.length == 0) {
            throw new IllegalArgumentException("empty security key");
        }
        int spos = 0;
        for (int pos = 0; pos < input.length; ++pos) {
            output[pos] = (byte) (input[pos] ^ secret[spos]);
            ++spos;
            if (spos >= secret.length) {
                spos = 0;
            }
        }
        return output;
    }
    
    public String uniqueId() {
        final String[] passwords = new String[] {
            "ALKJVBPIQYTUIWEBVPQALZVKQRWORTUYOYISHFLKAJMZNXBVMNFGAHKJSDFALAPOQIERIUYTGSFGKMZNXBVJAHGFAKX",
            "1234567890",
            "qpowiealksdjzmxnvbfghsdjtreiuowiruksfhksajmzxncbvlaksjdhgqwetytopskjhfgvbcnmzxalksjdfhgbvzm"
        };
        final StringBuilder result = new StringBuilder();
        final long passwordLength = 12;
        for (int index = 0; index < passwordLength; index += 1) {
            final int passIndex = (int) (passwords.length * index / passwordLength);
            final int charIndex = (int) Math.abs(
                UUID.randomUUID().getLeastSignificantBits() % passwords[passIndex].length());
            result.append(passwords[passIndex].charAt(charIndex));
        }
        return result.toString();
    }

    
    private static class MyModel extends AbstractTableModel {

        private static final String[] columnNames = {"Номер", "Фамилия", "Имя", "Отчество"};
        private final List<Map<String, Object>> list;

        private MyModel(List<Map<String, Object>> list) {
            this.list = list;
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
                    return list.get(rowIndex).get("orderNumber");
                case 1:
                    return list.get(rowIndex).get("surname");
                case 2:
                    return list.get(rowIndex).get("firstName");
                case 3:
                    return list.get(rowIndex).get("middleName");
            }
            return null;
        }
    }

    private void searchOrders() {
        foundOrders.clear();
        final List<Map<String, Object>> databaseData = getDatabaseData();
        final String searchText = ((JTextComponent) jComboBox4.getEditor().getEditorComponent()).getText().trim();
        final List<String> wordsForSearch = $.words(searchText);
        List<Map<String, Object>> selectedOrders = $.chain(
            databaseData)
            .filter(new Predicate<Map<String, Object>>() {
                @Override
                public Boolean apply(Map<String, Object> map) {
                    return checkStrictMap(map, searchText, "_id");
                }
            })
            .value();
        List<Map<String, Object>> selectedOrders2 = $.chain(
            getFilteredOrders(databaseData))
            .filter(new Predicate<Map<String, Object>>() {
                @Override
                public Boolean apply(Map<String, Object> map) {
                    boolean idNumber = true;
                    for (String word : wordsForSearch) {
                        boolean orderNumber = checkMap(map, word, "orderNumber");
                        boolean firstName = checkMap(map, word, "firstName");
                        boolean middleName = checkMap(map, word, "middleName");
                        boolean surname = checkMap(map, word, "surname");
                        boolean phoneNumber = checkNumbersMap(map, word, "phoneNumber");
                        boolean status = checkMap(map, word, "status");
                        if (!orderNumber && !firstName && !middleName && !surname
                                && !phoneNumber && !status) {
                            idNumber = false;
                            break;
                        };
                    }
                   return idNumber;
                }
            })
            .filter(new Predicate<Map<String, Object>>() {
                public Boolean apply(Map<String, Object> map) {
                    int index = jComboBox5.getSelectedIndex();
                    if (map.get("created") == null) {
                        return true;
                    }
                    long difference = new Date().getTime() - (Long) map.get("created");
                    boolean result = true;
                    switch (index) {
                        case 0:
                            break;
                        case 1:
                            result = difference <= 60 * 60 * 1000;
                            break;
                        case 2:
                            result = difference <= 86400000;
                            break;
                        case 3:
                            result = difference <= 86400000 * 7;
                            break;
                        case 4:
                            result = difference <= 86400000 * 30.59;
                            break;
                        case 5:
                            result = difference <= 86400000 * 365.25;
                            break;                            
                    }
                    return result;
                }
            })
            .value();
        foundOrders.addAll(selectedOrders);
        foundOrders.addAll(selectedOrders2);
        jTable1.setModel(new MyModel(foundOrders));
    }
    
    private boolean checkMap(Map<String, Object> map, String text, String key) {
        return text.trim().isEmpty()
            || (map.get(key) != null && $.isString(text) && (((String) map.get(key)).toLowerCase(localeRu)).contains(
                text.trim().toLowerCase(localeRu)));
    }
    
    private boolean checkNumbersMap(Map<String, Object> map, String text, String key) {
        if (text.trim().isEmpty()) {
            return true;
        }
        if (map.containsKey(key)) {
            StringBuilder fieldBuilder = new StringBuilder();
            for (char character : text.toCharArray()) {
                if (character >= '0' && character <= '9') {
                    fieldBuilder.append(character);
                }
            }
            StringBuilder valueBuilder = new StringBuilder();
            for (char character : ((String) map.get(key)).toCharArray()) {
                if (character >= '0' && character <= '9') {
                    valueBuilder.append(character);
                }
            }
            return !fieldBuilder.toString().isEmpty() && valueBuilder.toString().contains(fieldBuilder.toString());
        }
        return false;
    }
    
    private boolean checkStrictMap(Map<String, Object> map, String text, String key) {
        return !text.trim().isEmpty()
            && (map.containsKey(key) && ((String) map.get(key)).equals(text.trim()));
    }

    private void focusNextElementOnPressEnter(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER || evt.getKeyCode() == KeyEvent.VK_TAB) {
            KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            int modifiers = evt.getModifiers();
            if ((modifiers & (InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK)) != 0) {
                Component component = manager.getFocusOwner();
                Container root = component.getFocusCycleRootAncestor();
                FocusTraversalPolicy policy = root.getFocusTraversalPolicy();
                Component prevFocus = policy.getComponentBefore(root, component);
                if (prevFocus != null) {
                    prevFocus.requestFocusInWindow();
                }
            } else {
                manager.getFocusOwner().transferFocus();
            }
            evt.consume();
        }
    }
    
    public void writeDataFile(String fileName) {
        try {
            if (fileName.endsWith(".xml")) {
                Files.write(Paths.get(fileName), $.toXml(foundOrders).getBytes("UTF-8"));
            } else if (fileName.endsWith(".json")) {
                Files.write(Paths.get(fileName), $.toJson(foundOrders).getBytes("UTF-8"));
            }
        } catch (IOException ex) {
            Logger.getLogger(Form1.class.getName()).log(Level.SEVERE, null, ex);
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
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        jTextField2 = new javax.swing.JTextField();
        jTextField3 = new javax.swing.JTextField();
        jTextField4 = new javax.swing.JTextField();
        jTextField5 = new javax.swing.JTextField();
        jTextField6 = new javax.swing.JTextField();
        jTextField9 = new javax.swing.JTextField();
        jTextField10 = new javax.swing.JTextField();
        jTextField11 = new javax.swing.JTextField();
        jTextField12 = new javax.swing.JTextField();
        jTextField13 = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        jSeparator1 = new javax.swing.JSeparator();
        jComboBox1 = new javax.swing.JComboBox();
        jComboBox2 = new javax.swing.JComboBox();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jComboBox3 = new javax.swing.JComboBox();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel21 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jLabel24 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        jComboBox4 = new HistoryComboBox();
        jLabel18 = new javax.swing.JLabel();
        jComboBox5 = new javax.swing.JComboBox();
        jLabel19 = new javax.swing.JLabel();
        jComboBox6 = new javax.swing.JComboBox();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setFont(new java.awt.Font("Times New Roman", 2, 24)); // NOI18N
        jLabel1.setText("<html><u>Оформление заказа</u></html>");
        jLabel1.setToolTipText("");
        jLabel1.setFocusable(false);

        jLabel2.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel2.setText("Номер заказа:");

        jLabel3.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel3.setText("Имя покупателя:");

        jLabel4.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel4.setText("Отчество покупателя:");

        jLabel5.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel5.setText("Фамилия покупателя:");

        jLabel6.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel6.setText("Телефон для связи:");

        jLabel7.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel7.setText("e-mail:");

        jLabel8.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel8.setText("Способ оплаты:");

        jLabel9.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel9.setText("Способ доставки:");

        jLabel10.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel10.setText("Город:");

        jLabel11.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel11.setText("Улица:");

        jLabel12.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel12.setText("Номер дома:");

        jLabel13.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel13.setText("Корпус / строение:");

        jLabel14.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel14.setText("Номер квартиры:");

        jLabel15.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel15.setText("Комментарии:");

        jButton1.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jButton1.setText("Записать");
        jButton1.setNextFocusableComponent(jComboBox4);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jButton1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jButton1KeyPressed(evt);
            }
        });

        jTextField1.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTextField1.setNextFocusableComponent(jTextField2);
        jTextField1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField1KeyPressed(evt);
            }
        });

        jTextField2.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTextField2.setNextFocusableComponent(jTextField3);
        jTextField2.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField2KeyPressed(evt);
            }
        });

        jTextField3.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTextField3.setNextFocusableComponent(jTextField4);
        jTextField3.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField3KeyPressed(evt);
            }
        });

        jTextField4.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTextField4.setNextFocusableComponent(jTextField5);
        jTextField4.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField4KeyPressed(evt);
            }
        });

        jTextField5.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTextField5.setNextFocusableComponent(jTextField6);
        jTextField5.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField5KeyPressed(evt);
            }
        });

        jTextField6.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTextField6.setNextFocusableComponent(jComboBox1);
        jTextField6.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField6KeyPressed(evt);
            }
        });

        jTextField9.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTextField9.setNextFocusableComponent(jTextField10);
        jTextField9.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField9KeyPressed(evt);
            }
        });

        jTextField10.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTextField10.setNextFocusableComponent(jTextField11);
        jTextField10.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField10KeyPressed(evt);
            }
        });

        jTextField11.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTextField11.setNextFocusableComponent(jTextField12);
        jTextField11.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField11KeyPressed(evt);
            }
        });

        jTextField12.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTextField12.setNextFocusableComponent(jTextField13);
        jTextField12.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField12KeyPressed(evt);
            }
        });

        jTextField13.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTextField13.setNextFocusableComponent(jTextPane1);
        jTextField13.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField13KeyPressed(evt);
            }
        });

        jTextPane1.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTextPane1.setNextFocusableComponent(jButton1);
        jTextPane1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextPane1KeyPressed(evt);
            }
        });
        jScrollPane1.setViewportView(jTextPane1);

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        jComboBox1.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jComboBox1.setNextFocusableComponent(jComboBox2);
        jComboBox1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jComboBox1KeyPressed(evt);
            }
        });

        jComboBox2.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jComboBox2.setNextFocusableComponent(jComboBox3);
        jComboBox2.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jComboBox2KeyPressed(evt);
            }
        });

        jLabel16.setFont(new java.awt.Font("Times New Roman", 2, 24)); // NOI18N
        jLabel16.setText("<html><u>Поиск</u></html>");
        jLabel16.setToolTipText("");
        jLabel16.setFocusable(false);

        jLabel17.setFont(new java.awt.Font("Times New Roman", 2, 12)); // NOI18N
        jLabel17.setText("<html>Номер в базе, номер заказа,<br/>имя покупателя, телефон, статус заявки:<html>");

        jLabel23.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel23.setText("<html><u>Статус заявки:</u><html>");

        jComboBox3.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jComboBox3.setNextFocusableComponent(jTextField9);
        jComboBox3.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jComboBox3KeyPressed(evt);
            }
        });

        jButton2.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jButton2.setText("Досье покупателя");
        jButton2.setNextFocusableComponent(jButton3);
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jButton2.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jButton2KeyPressed(evt);
            }
        });

        jButton3.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jButton3.setText("Поиск");
        jButton3.setNextFocusableComponent(jButton4);
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jButton3.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jButton3KeyPressed(evt);
            }
        });

        jButton4.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jButton4.setText("Сохранить");
        jButton4.setNextFocusableComponent(jTextField1);
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        jButton4.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jButton4KeyPressed(evt);
            }
        });

        jLabel21.setFont(new java.awt.Font("Times New Roman", 0, 12)); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel21, javax.swing.GroupLayout.DEFAULT_SIZE, 69, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel21)
                .addContainerGap())
        );

        jPanel2.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N

        jScrollPane2.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N

        jTable1.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Номер", "Фамилия", "Имя", "Отчество"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(jTable1);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 478, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
        );

        jButton5.setText("...");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jButton6.setText("...");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        jButton7.setText("...");
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        jLabel24.setText("Номер для записи в базе");

        jLabel25.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel25.setText(" ");

        jComboBox4.setEditable(true);
        jComboBox4.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jComboBox4.setNextFocusableComponent(jComboBox5);
        jComboBox4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox4ActionPerformed(evt);
            }
        });

        jLabel18.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel18.setText("По дате создания:");

        jComboBox5.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jComboBox5.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "за всё время", "за последний час", "за сегодня", "за эту неделю", "за этот месяц", "за этот год" }));
        jComboBox5.setNextFocusableComponent(jComboBox6);
        jComboBox5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox5ActionPerformed(evt);
            }
        });
        jComboBox5.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jComboBox5KeyPressed(evt);
            }
        });

        jLabel19.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel19.setText("Автообновление:");

        jComboBox6.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jComboBox6.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "не обновлять", "каждые 5 секунд", "каждые 60 секунд", "каждые 10 минут" }));
        jComboBox6.setNextFocusableComponent(jButton2);
        jComboBox6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox6ActionPerformed(evt);
            }
        });
        jComboBox6.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jComboBox6KeyPressed(evt);
            }
        });

        jMenu1.setMnemonic('\u0430');
        jMenu1.setText("Файл");

        jMenuItem1.setMnemonic('\u044b');
        jMenuItem1.setText("Выход");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuBar1.add(jMenu1);

        jMenu3.setMnemonic('\u0435');
        jMenu3.setText("Сервис");

        jMenuItem3.setMnemonic('\u043f');
        jMenuItem3.setText("Параметры...");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem3);

        jMenuBar1.add(jMenu3);

        jMenu2.setMnemonic('\u0441');
        jMenu2.setText("Справка");

        jMenuItem2.setMnemonic('\u043e');
        jMenuItem2.setText("О программе");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem2);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(60, 60, 60)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 55, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel24, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel25, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(38, 38, 38)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel13)
                            .addComponent(jLabel12)
                            .addComponent(jLabel14)
                            .addComponent(jLabel15)
                            .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 167, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jLabel10, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel11, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jLabel4)
                                .addComponent(jLabel6)
                                .addComponent(jLabel7)
                                .addComponent(jLabel5)
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel8)
                                        .addComponent(jLabel9)
                                        .addComponent(jLabel23, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jButton6, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
                                        .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jButton7, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)))))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1)
                            .addComponent(jTextField13)
                            .addComponent(jTextField12)
                            .addComponent(jTextField11)
                            .addComponent(jTextField10)
                            .addComponent(jTextField9)
                            .addComponent(jComboBox3, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jComboBox2, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jComboBox1, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jTextField6)
                            .addComponent(jTextField5)
                            .addComponent(jTextField4)
                            .addComponent(jTextField3)
                            .addComponent(jTextField2)
                            .addComponent(jTextField1))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(198, 198, 198)
                                        .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                .addComponent(jLabel18, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(jLabel19, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                            .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 188, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jComboBox6, javax.swing.GroupLayout.PREFERRED_SIZE, 213, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                .addComponent(jComboBox5, 0, 213, Short.MAX_VALUE)
                                                .addComponent(jComboBox4, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                                .addGap(10, 10, 10)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(7, 7, 7))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(15, 15, 15)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jComboBox4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel18)
                                    .addComponent(jComboBox5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel19)
                                    .addComponent(jComboBox6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel24)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel25)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel8)
                            .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton5))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel9)
                            .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel23, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton7)
                            .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel10)
                            .addComponent(jTextField9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel11)
                            .addComponent(jTextField10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel12)
                            .addComponent(jTextField11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel13)
                            .addComponent(jTextField12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel14)
                            .addComponent(jTextField13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel15)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        saveData(createOrderData());
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jTextField1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField1KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jTextField1KeyPressed

    private void jTextField2KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField2KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jTextField2KeyPressed

    private void jTextField3KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField3KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jTextField3KeyPressed

    private void jTextField4KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField4KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jTextField4KeyPressed

    private void jTextField5KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField5KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jTextField5KeyPressed

    private void jTextField6KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField6KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jTextField6KeyPressed

    private void jComboBox1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jComboBox1KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jComboBox1KeyPressed

    private void jComboBox2KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jComboBox2KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jComboBox2KeyPressed

    private void jTextField9KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField9KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jTextField9KeyPressed

    private void jTextField10KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField10KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jTextField10KeyPressed

    private void jTextField11KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField11KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jTextField11KeyPressed

    private void jTextPane1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextPane1KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jTextPane1KeyPressed

    private void jButton1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jButton1KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jButton1KeyPressed

    private void jComboBox3KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jComboBox3KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jComboBox3KeyPressed

    private void jButton2KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jButton2KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jButton2KeyPressed

    private void jButton3KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jButton3KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jButton3KeyPressed

    private void jButton4KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jButton4KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jButton4KeyPressed

    private void jTextField12KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField12KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jTextField12KeyPressed

    private void jTextField13KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField13KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jTextField13KeyPressed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        searchOrders();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        NewJDialog dialog = new NewJDialog(this, "Настройка справочника", true, jComboBox1.getModel());
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        if (dialog.isApproved()) {
            jComboBox1.setModel(dialog.getNewModel());
        }
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        int result = chooser1.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            writeDataFile(chooser1.getSelectedFile().getAbsolutePath());
        }
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        NewJDialog dialog = new NewJDialog(this, "Настройка справочника", true, jComboBox2.getModel());
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        if (dialog.isApproved()) {
            jComboBox2.setModel(dialog.getNewModel());
        }
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        NewJDialog dialog = new NewJDialog(this, "Настройка справочника", true, jComboBox3.getModel());
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        if (dialog.isApproved()) {
            jComboBox3.setModel(dialog.getNewModel());
        }
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        dossieDialog.reload();
        dossieDialog.setVisible(true);
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        About dialog = new About(this, true);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        NewJDialog2 dialog = new NewJDialog2(this, true, hostName, dbName, user, pass);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        if (dialog.isApproved()) {
            dialog.getDbName();
            hostName = dialog.getHostName();
            dbName = dialog.getDbName();
            user = dialog.getUser();
            pass = dialog.getPass();
        }
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jComboBox5KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jComboBox5KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jComboBox5KeyPressed

    private void jComboBox6KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jComboBox6KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jComboBox6KeyPressed

    private void jComboBox4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox4ActionPerformed
        searchOrders();
    }//GEN-LAST:event_jComboBox4ActionPerformed

    private void jComboBox6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox6ActionPerformed
        initTimer();
    }//GEN-LAST:event_jComboBox6ActionPerformed

    private void jComboBox5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox5ActionPerformed
        searchOrders();
    }//GEN-LAST:event_jComboBox5ActionPerformed

    private void initTimer() {
        int index = jComboBox6.getSelectedIndex();
        switch (index) {
            case 0:
                if (taskHandle != null) {
                    taskHandle.cancel(true);
                }
                break;
            case 1:
                taskHandle = scheduler.scheduleAtFixedRate(reloadSearchOrders(), 0, 5, TimeUnit.SECONDS);
                break;
            case 2:
                taskHandle = scheduler.scheduleAtFixedRate(reloadSearchOrders(), 0, 60, TimeUnit.SECONDS);
                break;
            case 3:
                taskHandle = scheduler.scheduleAtFixedRate(reloadSearchOrders(), 0, 10, TimeUnit.MINUTES);
                break;
        }
    }

    private Runnable reloadSearchOrders() {
        if (taskHandle != null) {
            taskHandle.cancel(true);
        }
        return new Runnable() {
            public void run() {
                try {
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            jLabel21.setText(new SimpleDateFormat("HH:mm:ss").format(new Date()));
                            int rowIndex = jTable1.getSelectedRow();
                            searchOrders();
                            jTable1.getSelectionModel().setSelectionInterval(rowIndex, rowIndex);
                        }
                    });
                } catch(Exception ex) {
                    Logger.getLogger(Form1.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Form1.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Form1.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Form1.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Form1.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Form1().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JComboBox jComboBox3;
    private javax.swing.JComboBox jComboBox4;
    private javax.swing.JComboBox jComboBox5;
    private javax.swing.JComboBox jComboBox6;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField10;
    private javax.swing.JTextField jTextField11;
    private javax.swing.JTextField jTextField12;
    private javax.swing.JTextField jTextField13;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField9;
    private javax.swing.JTextPane jTextPane1;
    // End of variables declaration//GEN-END:variables
}
