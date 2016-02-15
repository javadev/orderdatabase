package com.github.javadev.orderdatabase;

import com.github.underscore.Function;
import com.github.underscore.Function1;
import com.github.underscore.FunctionAccum;
import com.github.underscore.Optional;
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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.JTextComponent;

public class Form1 extends javax.swing.JFrame {
    private final Map<String, Object> database = new LinkedHashMap<>();
    private final List<Map<String, Object>> foundOrders = new ArrayList<>();
    private final List<Map<String, Object>> users = new ArrayList<>();
    private final Map<String, Object> activeUser = new LinkedHashMap<>();
    private final List<String> createdFiles = new ArrayList<>();
    private final JFileChooser chooser1 = new JFileChooser();
    private final NewJDialog1 dossieDialog;
    private final Locale localeRu = new Locale("ru", "RU");
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> taskHandle;
    private ScheduledFuture<?> passHandle;
    private boolean useMySql;
    private String hostName;
    private String dbName;
    private String user;
    private String pass;
    private boolean useXlsx;
    private String xlsxPath;
    private String adminPass;
    private boolean showDbNumber;

    public Form1() {
        initComponents();
        setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage(getClass().getResource("/orderdatabase.png")));
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
                        && !$.omit(data, "_id", "created", "status", "user", "country", "products").toString().equals(
                        $.omit((Map<String, Object>) database.get("currentOrder"),
                            "_id", "created", "status", "user", "country", "products").toString())) {
                    saveData(data);
                } else {
                    saveData(null);
                }
                for (String fileName : $.uniq(createdFiles)) {
                    try {
                        Files.delete(Paths.get(fileName));
                    } catch (IOException ex) {
                        Logger.getLogger(Form1.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        fillComboBoxModel("paymentMethodData", jComboBox1);
        fillComboBoxModel("deliveryMethodData", jComboBox2);
        fillComboBoxModel("statusData", jComboBox3);
        fillComboBoxModel("searchData", jComboBox4);
        fillComboBoxModel("countryData", jComboBox7, Optional.of("Россия"));
        fillComboBoxModel("cityData", jComboBox8, Optional.of("Москва"));
        useMySql = database.get("useMySql") == null ? true : (Boolean) database.get("useMySql");
        hostName = (String) database.get("hostName");
        dbName = (String) database.get("dbName");
        user = (String) database.get("user");
        pass = database.get("pass") == null ? null
                : decrypt(((String) database.get("pass")));
        useXlsx = database.get("useXlsx") == null ? true : (Boolean) database.get("useXlsx");
        xlsxPath = (String) database.get("xlsxPath");
        ((JTextComponent) jComboBox4.getEditor().getEditorComponent()).setText((String) database.get("searchDataText"));
        showDbNumber = database.get("showDbNumber") == null ? false : (Boolean) database.get("showDbNumber");
        if ($.isNumber(database.get("periodIndex"))) {
            jComboBox5.setSelectedIndex(((Long) database.get("periodIndex")).intValue());
        }
        if ($.isNumber(database.get("autoLoadIndex"))) {
            jComboBox6.setSelectedIndex(((Long) database.get("autoLoadIndex")).intValue());
        }
        if ($.isBoolean(database.get("searchPanelEnabled"))) {
            jCheckBoxMenuItem1.setSelected((Boolean) database.get("searchPanelEnabled"));
        } else {
            jCheckBoxMenuItem1.setSelected(false);
        }
        adminPass = database.get("adminPass") == null ? null
                : decrypt(((String) database.get("adminPass")));
        final java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        if (database.get("locationX") instanceof Long && database.get("locationY") instanceof Long) {
            setLocation(Math.min(screenSize.width - 50, ((Long) database.get("locationX")).intValue()),
                    Math.min(screenSize.height - 50, ((Long) database.get("locationY")).intValue()));
        }
        dossieDialog = new NewJDialog1(this, "Досье покупателя", false, database);
        dossieDialog.setLocationRelativeTo(this);
        List<Map<String, Object>> filteredOrders = getFilteredOrders(getDatabaseData());
        jTable2.setModel(new MyProductModel(new ArrayList<Map<String, Object>>()));
        if (database.get("productColumnWidth") instanceof List) {
            setColumnWidth(jTable2, (List<Long>) database.get("productColumnWidth"));
        } else {
            setColumnWidth(jTable2, java.util.Arrays.asList(60L, 175L, 34L, 54L, 52L));
        }
        addJTable2Listener(jTable2);
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
        jTable2.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_BACK_SPACE
                        || event.getKeyCode() == KeyEvent.VK_DELETE) {
                    javax.swing.JTable jTable = (javax.swing.JTable) event.getSource();
                    int selectedIndex = jTable.getSelectionModel().getAnchorSelectionIndex();
                    List<Map<String, Object>> list = ((MyProductModel) jTable.getModel()).getData();
                    list.remove(selectedIndex);
                    long totalSum = calcTotalSum(list);
                    jTextField15.setText(formatSum(totalSum));
                    long previousSum = calcPreviousSum(getFilteredOrders(getDatabaseData()));
                    jTextField16.setText(formatSum(previousSum + totalSum));
                    jTextField18.setText(calcDiscount(previousSum + totalSum) + "%");
                    List<Long> columnWidth = getColumnWidth(jTable);
                    jTable.setModel(new MyProductModel(list));
                    setColumnWidth(jTable, columnWidth);
                    addJTable2Listener(jTable);
                    if (list.size() > 0) {
                        int newSelectedIndex = Math.min(selectedIndex, list.size() - 1);
                        jTable.setRowSelectionInterval(newSelectedIndex, newSelectedIndex);
                    }
                }
            }
        });
        if (!showDbNumber) {
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    jLabel24.setVisible(false);
                    jLabel25.setVisible(false);
                }
            });
        }
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
        if (database.get("userData") instanceof String) {
            users.addAll((List<Map<String, Object>>) $.fromJson(decrypt((String) database.get("userData"))));
            if (!getActiveUsers().isEmpty()) {
                disableButtons();
            }
        }
    }

    private List<Map<String, Object>> getActiveUsers() {
        return $.filter(users, new Predicate<Map<String, Object>>() {
                public Boolean apply(Map<String, Object> arg) {
                    return $.isBoolean(arg.get("active")) && (Boolean) arg.get("active"); 
                }
        });
    }

    private long calcTotalSum(List<Map<String, Object>> products) {
        long totalSum = 0;
        if (products != null) {
            for (Map<String, Object> product : products) {
                if (String.valueOf(product.get("totalPrice")).matches("\\d+")) {
                    totalSum += Long.parseLong(String.valueOf(product.get("totalPrice")));
                }
            }
        }
        return totalSum;
    }
    
    private long calcPreviousSum(List<Map<String, Object>> databaseData) {
        if (database.get("currentOrder") == null) {
            return 0L;
        }
        final String firstName = (String) ((Map<String, Object>) database.get("currentOrder")).get("firstName");
        final String middleName = (String) ((Map<String, Object>) database.get("currentOrder")).get("middleName");
        final String surname = (String) ((Map<String, Object>) database.get("currentOrder")).get("surname");
        final String phoneNumber = (String) ((Map<String, Object>) database.get("currentOrder")).get("phoneNumber");
        long previousSum = $.chain(databaseData)
            .filter(new Predicate<Map<String, Object>>() {
                public Boolean apply(Map<String, Object> map) {
                    boolean firstNameFound = checkMap(map, firstName, "firstName");
                    boolean middleNameFound = checkMap(map, middleName, "middleName");
                    boolean surnameFound = checkMap(map, surname, "surname");
                    boolean phoneNumberFound = checkNumbersMap(map, phoneNumber, "phoneNumber");
                    boolean statusFound = checkStrictMap(map, "оплачено", "status");
                    return firstNameFound && middleNameFound && surnameFound
                            && phoneNumberFound && statusFound;
                }
            })
            .reduce(new FunctionAccum<Long, Map<String, Object>>() {
                @Override
                public Long apply(Long accum, Map<String, Object> order) {
                    return accum + calcTotalSum((List<Map<String, Object>>) order.get("products"));
                }
            }, 0L).item();
        return previousSum;
    }
    
    private long calcDiscount(long totalSum) {
        long result = 0;
        if (totalSum > 5000) {
            result = 5;
        }
        if (totalSum > 10000) {
            result = 7;
        }
        if (totalSum > 15000) {
            result = 10;
        }
        if (totalSum > 20000) {
            result = 12;
        }
        if (totalSum > 100000) {
            result = 15;
        }
        if (totalSum > 150000) {
            result = 20;
        }
        return result;
    }
    
    private String formatSum(Long value) {
        DecimalFormat format = (DecimalFormat) NumberFormat.getInstance(localeRu);
        format.setMaximumFractionDigits(2);
        return format.format(value.doubleValue());
    }

    private void fillComboBoxModel(String key, JComboBox jComboBox) {
        fillComboBoxModel(key, jComboBox, Optional.<String>absent());
    }

    private void fillComboBoxModel(String key, JComboBox jComboBox, Optional<String> defaultValue) {
        final List<String> databaseData;
        if (database.get(key) == null || !(database.get(key) instanceof List)) {
            databaseData = new ArrayList<String>();
            if (defaultValue.isPresent()) {
                databaseData.add(defaultValue.get());
            }
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
        fillComboBoxSelectedItem(jComboBox7, (String) order.get("country"), "countryData", Optional.of("Россия"));
        fillComboBoxSelectedItem(jComboBox8, (String) order.get("city"), "cityData", Optional.of("Москва"));
        jTextField10.setText((String) order.get("street"));
        jTextField11.setText((String) order.get("houseNumber"));
        jTextField12.setText((String) order.get("houseNumber2"));
        jTextField13.setText((String) order.get("appartmentNumber"));
        jTextArea1.setText((String) order.get("comment"));
        jLabel25.setText((String) order.get("_id"));
        List<Long> columnWidth = getColumnWidth(jTable2);
        if (order.get("products") == null) {
            jTable2.setModel(new MyProductModel(new ArrayList<Map<String, Object>>()));
        } else {
            jTable2.setModel(new MyProductModel((List<Map<String, Object>>) order.get("products")));
        }
        setColumnWidth(jTable2, columnWidth);
        addJTable2Listener(jTable2);
        long totalSum = calcTotalSum((List<Map<String, Object>>) order.get("products"));
        jTextField15.setText(formatSum(totalSum));
        long previousSum = calcPreviousSum(getFilteredOrders(getDatabaseData()));
        jTextField16.setText(formatSum(previousSum + totalSum));
        jTextField18.setText(calcDiscount(previousSum + totalSum) + "%");
        Long created = (Long) order.get("created");
        if (created == null) {
            jTextField19.setText("");
        } else {
            jTextField19.setText(new SimpleDateFormat("dd.MM.yy").format(new Date(created)));
        }
    }
    
    private Map<String, Object> getCleanOrderData() {
                Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("created", null);
        data.put("_id", " ");
        data.put("orderNumber", "");
        data.put("firstName", "");
        data.put("middleName", "");
        data.put("surname", "");
        data.put("phoneNumber", "");
        data.put("email", "");
        data.put("paymentMethod",  null);
        data.put("deliveryMethod", null);
        data.put("status", null);
        data.put("country", $.isEmpty((List<String>) database.get("countryData")) ? "Россия"
            : $.first((List<String>) database.get("countryData")));
        data.put("city", $.isEmpty((List<String>) database.get("cityData")) ? "Москва"
            : $.first((List<String>) database.get("cityData")));
        data.put("street", "");
        data.put("houseNumber", "");
        data.put("houseNumber2", "");
        data.put("appartmentNumber", "");
        data.put("comment", "");
        data.put("user", "");
        data.put("products", new ArrayList<Map<String, Object>>());
        data.put("totalSum", "0");
        data.put("discount", "0");
        return data;
    }

    private void fillComboBoxSelectedItem(JComboBox jComboBox, String data,
            String dictKey) {
        fillComboBoxSelectedItem(jComboBox, data, dictKey, Optional.<String>absent());
    }

    private void fillComboBoxSelectedItem(JComboBox jComboBox, String data,
            String dictKey, Optional<String> defaultValue) {
        if (jComboBox.getModel() == null) {
            return;
        }
        if (data == null || data.isEmpty()) {
            jComboBox.getModel().setSelectedItem(null);
            return;
        }
        String modelElement = null;
        fillComboBoxModel(dictKey, jComboBox, defaultValue);
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

    private void fillOrderNumber() {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                jTextField1.setText(calcOrderNumber(jTextField4.getText(), jTextField2.getText(), jTextField3.getText(),
                        String.valueOf(jComboBox8.getSelectedItem()).trim()));
            }
        });
    }
    
    private String calcOrderNumber(String surname, String firstName, String middleName, String cityName) {
        String name = $.chain(
                surname, firstName, middleName)
            .compact()
            .map(
            new Function1<String, String>() {
                public String apply(String f) {
                    return f.trim().isEmpty() ? "" : f.trim().substring(0, 1);
                }
            })
            .join("")
            .item();
        String city = $.join($.map($.words(cityName),
                    new Function1<String, String>() {
                public String apply(String f) {
                    return f.trim().isEmpty() ? "" : f.trim().substring(0, 1).toUpperCase(localeRu);
                }
        }), "");
        return $.join($.chain(name, city.isEmpty() ? "М" : city,
                "" + getFilteredOrders(getDatabaseData()).size()).value(), "-");        
    }

    private List<Map<String, Object>> getDatabaseData() {
        if (database.get("data") == null) {
            database.put("data", new ArrayList<Map<String, Object>>());
        }
        Set<String> ids = new HashSet<String>();
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) database.get("data");
        if (useMySql) {
            for (Map<String, Object> data : dataList) {
                ids.add((String) data.get("_id"));
            }
            List<Map<String, Object>> dbDataList = new DatabaseService(hostName, dbName,
                activeUser.isEmpty() ? user : (String) activeUser.get("login"), pass).readAll();
            for (Map<String, Object> data : dbDataList) {
                if (!ids.contains((String) data.get("_id"))) {
                    dataList.add(data);
                }
            }
        }
        if (useXlsx) {
            XlsxService xlsxService = new XlsxService(xlsxPath);
            Map<String, Map<String, Object>> orderNumbers = new LinkedHashMap<>();
            List<Map<String, Object>> xlsxDataList = xlsxService.readAll();
            for (Map<String, Object> data : dataList) {
                orderNumbers.put((String) data.get("orderNumber"), data);
            }
            for (Map<String, Object> data : xlsxDataList) {
                if (orderNumbers.get((String) data.get("orderNumber")) == null) {
                    data.put("_id", uniqueId());
                    orderNumbers.put((String) data.get("orderNumber"), data);
                    dataList.add(data);
                }
            }
        }
        return dataList;
    }

    private void saveDatabaseData() {
        if (useMySql) {
            DatabaseService databaseService = new DatabaseService(hostName, dbName,
                activeUser.isEmpty() ? user : (String) activeUser.get("login"), pass);
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
        if (useXlsx) {
            XlsxService xlsxService = new XlsxService(xlsxPath);
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) database.get("data");
            List<Map<String, Object>> filteredOrders = getFilteredOrders(dataList);
            xlsxService.updateData($.sortBy(filteredOrders, new Function1<Map<String, Object>, Long>() {
                public Long apply(Map<String, Object> item) {
                    return item.get("created") == null ? Long.valueOf(0) : (Long) item.get("created");
                }
            }));
        }
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
                   return item.get("created") == null ? Long.valueOf(0) : (Long) item.get("created");
               } 
            });
            if (!sorted.isEmpty()) {
                filteredOrders.add($.last(sorted));
            }
        }
        $.sortBy(filteredOrders, new Function1<Map<String, Object>, Long>() {
            public Long apply(Map<String, Object> item) {
                return item.get("created") == null ? Long.valueOf(0) : (Long) item.get("created");
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
        data.put("country", jComboBox7.getSelectedItem() == null ? null
                : String.valueOf(jComboBox7.getSelectedItem()).trim());
        data.put("city", jComboBox8.getSelectedItem() == null ? null
                : String.valueOf(jComboBox8.getSelectedItem()).trim());
        data.put("street", jTextField10.getText().trim());
        data.put("houseNumber", jTextField11.getText().trim());
        data.put("houseNumber2", jTextField12.getText().trim());
        data.put("appartmentNumber", jTextField13.getText().trim());
        data.put("comment", jTextArea1.getText().trim());
        data.put("user", user);
        List<Map<String, Object>> products = ((MyProductModel) jTable2.getModel()).getData();
        for (Map<String, Object> product : products) {
            product.put("_id", uniqueId());
            product.put("orderId", data.get("_id"));            
        }
        data.put("products", products);
        long totalSum = calcTotalSum(products);
        long previousSum = calcPreviousSum(getFilteredOrders(getDatabaseData()));
        data.put("totalSum", "" + totalSum);
        data.put("discount", "" + calcDiscount(previousSum + totalSum));
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
            jTextField19.setText(new SimpleDateFormat("dd.MM.yy").format(new Date((Long) data.get("created"))));
            database.put("currentOrder", data);
        }
        dossieDialog.reload();
        database.put("searchData", new ArrayList<String>());
        for (int index = 0; index < jComboBox4.getModel().getSize(); index += 1) {
            ((List<String>) database.get("searchData")).add(
                String.valueOf(jComboBox4.getModel().getElementAt(index)).trim());
        }
        database.put("searchDataText", ((JTextComponent) jComboBox4.getEditor().getEditorComponent()).getText().trim());
        database.put("periodIndex", jComboBox5.getSelectedIndex());
        database.put("autoLoadIndex", jComboBox6.getSelectedIndex());
        database.put("productColumnWidth", getColumnWidth(jTable2));
        database.put("searchPanelEnabled", jCheckBoxMenuItem1.getState());
        database.put("locationX", getLocation().x);
        database.put("locationY", getLocation().y);
        database.put("useMySql", useMySql);
        database.put("hostName", hostName);
        database.put("dbName", dbName);
        database.put("user", user);
        database.put("pass", pass == null ? null : encrypt(pass));
        database.put("adminPass", adminPass == null ? null : encrypt(adminPass));
        database.put("useXlsx", useXlsx);
        database.put("xlsxPath", xlsxPath);
        database.put("showDbNumber", showDbNumber);
        
        try {
            Files.write(Paths.get("./database.json"), $.toJson(database).getBytes("UTF-8"));
        } catch (IOException ex) {
            Logger.getLogger(Form1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static String encrypt(String value) {
         try {
             javax.crypto.spec.IvParameterSpec iv = new javax.crypto.spec.IvParameterSpec("PWJB6205kuou(!@-".getBytes("UTF-8"));
             javax.crypto.spec.SecretKeySpec skeySpec = new javax.crypto.spec.SecretKeySpec("KYMT5802hccx$#(+".getBytes("UTF-8"), "AES");

             javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5PADDING");
             cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, skeySpec, iv);

             byte[] encrypted = cipher.doFinal(value.getBytes("UTF-8"));
             return javax.xml.bind.DatatypeConverter.printBase64Binary(encrypted);
         } catch (Exception ex) {
             return "";
         }
     }
 
     public static String decrypt(String encrypted) {
         try {
             javax.crypto.spec.IvParameterSpec iv = new javax.crypto.spec.IvParameterSpec("PWJB6205kuou(!@-".getBytes("UTF-8"));
             javax.crypto.spec.SecretKeySpec skeySpec = new javax.crypto.spec.SecretKeySpec("KYMT5802hccx$#(+".getBytes("UTF-8"), "AES");

             javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5PADDING");
             cipher.init(javax.crypto.Cipher.DECRYPT_MODE, skeySpec, iv);

             byte[] original = cipher.doFinal(javax.xml.bind.DatatypeConverter.parseBase64Binary(encrypted));

             final String decrypted = new String(original, "UTF-8");
             if (!encrypted.isEmpty() && decrypted.isEmpty()) {
                 return new String(xor(encrypted.getBytes("UTF-8"), "UTF-8".getBytes("UTF-8")), "UTF-8");
             }
             return decrypted;
         } catch (Exception ex) {
             return "";
         }
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

    private void addJTable2Listener(javax.swing.JTable jTable) {
        jTable.getModel().addTableModelListener(new TableModelListener() {
             @Override
            public void tableChanged(TableModelEvent event) {
                 if (event.getType() == TableModelEvent.UPDATE
                         || event.getType() == TableModelEvent.INSERT) {
                     long totalSum = calcTotalSum(((MyProductModel) event.getSource()).getData());
                     jTextField15.setText(formatSum(totalSum));
                     long previousSum = calcPreviousSum(getFilteredOrders(getDatabaseData()));
                     jTextField16.setText(formatSum(previousSum + totalSum));
                     jTextField18.setText(calcDiscount(previousSum + totalSum) + "%");
                 }
            }
        });
    }

    private String getPriceWithDiscount(String amount, String discount) {
        if (!amount.matches("\\d+") || !discount.matches("\\d+")) {
            return amount;
        }
        Long amountValue = Long.parseLong(amount);
        Long discountValue = Long.parseLong(discount);
        if (discountValue == 0L) {
            return amount;
        }
        Double newPrice = amountValue.longValue() * (1 - discountValue.doubleValue() / 100D);
        Long intPart = newPrice.longValue();
        Long fractPart = Math.round((newPrice - intPart) * 100);
        return newPrice.floatValue() == newPrice.longValue() ?
                "" + newPrice.longValue() : intPart + "," + (fractPart < 10 ? "0" + fractPart : fractPart);
    }

    private void setupSearchPanelVisible(boolean selected) {
        jPanel1.setVisible(selected);
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                Form1.this.pack();
            }
        });
    }

    private void enableButtons() {
        try {
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    final java.awt.Color colorBlack = new java.awt.Color(0, 0, 0);
                    jButton1.setForeground(colorBlack);
                    jButton8.setForeground(colorBlack);
                    jButton9.setForeground(colorBlack);
                    jButton12.setForeground(colorBlack);
                    jButton2.setForeground(colorBlack);
                    jButton3.setForeground(colorBlack);
                    jButton4.setForeground(colorBlack);
                }
            });
        } catch(Exception ex) {
            Logger.getLogger(Form1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void disableButtons() {
        try {
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    final java.awt.Color colorGrey = new java.awt.Color(102, 102, 102);
                    jButton1.setForeground(colorGrey);
                    jButton8.setForeground(colorGrey);
                    jButton9.setForeground(colorGrey);
                    jButton12.setForeground(colorGrey);
                    jButton2.setForeground(colorGrey);
                    jButton3.setForeground(colorGrey);
                    jButton4.setForeground(colorGrey);
                    setTitle("Программа обработки заявок покупателей");
                }
            });
        } catch(Exception ex) {
            Logger.getLogger(Form1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private Runnable logout() {
        activeUser.clear();
        if (passHandle != null) {
            passHandle.cancel(true);
        }
        return new Runnable() {
            public void run() {
                disableButtons();
            }
        };
    }

    private boolean checkLogin() {
        if (getActiveUsers().isEmpty() || !activeUser.isEmpty()) {
            return true;
        }
        if (passHandle != null) {
            passHandle.cancel(true);
        }
        passHandle = scheduler.scheduleAtFixedRate(logout(), 0, 2, TimeUnit.HOURS);
        javax.swing.JLabel jLabelName = new javax.swing.JLabel("Введите имя пользователя");
        final javax.swing.JTextField jTextField = new javax.swing.JTextField();
        javax.swing.JLabel jLabel = new javax.swing.JLabel("Введите пароль пользователя");
        final javax.swing.JTextField jPassword = new javax.swing.JPasswordField();
        Object[] objects = {jLabelName, jTextField, jLabel, jPassword};
        String[] options = {"ОК", "Отмена"};
        while (true) {
            int result = javax.swing.JOptionPane.showOptionDialog(this, objects,
                    "Имя пользователя и пароль", javax.swing.JOptionPane.OK_CANCEL_OPTION,
                    javax.swing.JOptionPane.QUESTION_MESSAGE, null, options, null);
            if (result == javax.swing.JOptionPane.OK_OPTION) {
                Optional<Map<String, Object>> user = $.find(users, new Predicate<Map<String, Object>>() {
                    public Boolean apply(Map<String, Object> arg) {
                        return $.isBoolean(arg.get("active")) && (Boolean) arg.get("active")
                                && jTextField.getText().trim().equals(arg.get("login"))
                                && jPassword.getText().equals(arg.get("pass"));
                    }
                });
                if (!user.isPresent()) {
                    final javax.swing.JOptionPane optionPane = new javax.swing.JOptionPane("Неверный пароль",
                            javax.swing.JOptionPane.INFORMATION_MESSAGE,
                            javax.swing.JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
                    final javax.swing.JDialog dialog = new javax.swing.JDialog(this);
                    dialog.setLocationRelativeTo(this);
                    dialog.setTitle("Неверный пароль");
                    dialog.setModal(true);
                    dialog.setContentPane(optionPane);
                    dialog.setDefaultCloseOperation(javax.swing.JDialog.DO_NOTHING_ON_CLOSE);
                    dialog.pack();
                    $.delay(new Function<Void>() {
                        public Void apply() {
                            dialog.dispose();
                            return null;
                        }
                    }, 1500);
                    dialog.setVisible(true);
                    continue;
                }
                activeUser.putAll(user.get());
                enableButtons();
                setTitle("Программа обработки заявок покупателей, оператор: " + activeUser.get("login"));
                return true;
            } else {
                break;
            }
        }
        return false;
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
    
    private static class MyProductModel extends AbstractTableModel {

        private static final String[] columnNames = {"Артикул", "Наименование", "Кол-во", "Цена", "Стоимость"};
        private final List<Map<String, Object>> list;

        private MyProductModel(List<Map<String, Object>> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> item : list) {
                result.add((Map<String, Object>) $.clone(item));
            }
            this.list = result;
        }

        public void addModel(Map<String, Object> model) {
            list.add(model);
            fireTableDataChanged();
        }

        public List<Map<String, Object>> getData() {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> item : list) {
                result.add((Map<String, Object>) $.clone(item));
            }
            return result;
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
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 2;
        } 

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return list.get(rowIndex).get("vendorCode");
                case 1:
                    return list.get(rowIndex).get("name");
                case 2:
                    return list.get(rowIndex).get("quantity");
                case 3:
                    return list.get(rowIndex).get("price");
                case 4:
                    return list.get(rowIndex).get("totalPrice");
            }
            return null;
        }
        
        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 2) {
                Map<String, Object> newRow = (Map<String, Object>) $.clone((Map<String, Object>) list.get(row));
                newRow.put("quantity", value);
                if (String.valueOf(value).matches("\\d+") && String.valueOf(newRow.get("price")).matches("\\d+")) {
                    newRow.put("totalPrice",
                            "" + (Long.parseLong(String.valueOf(value)) * Long.parseLong(String.valueOf(newRow.get("price")))));
                } else {
                    newRow.put("totalPrice", "0");
                    newRow.put("quantity", "0");
                }
                list.set(row, newRow);
                fireTableCellUpdated(row, 2);
                fireTableCellUpdated(row, 4);
            }
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
                        boolean email = checkMap(map, word, "email");
                        boolean paymentMethod = checkMap(map, word, "paymentMethod");
                        boolean deliveryMethod = checkMap(map, word, "deliveryMethod");
                        boolean country = checkMap(map, word, "country");
                        boolean city = checkMap(map, word, "city");
                        boolean street = checkMap(map, word, "street");
                        boolean houseNumber = checkMap(map, word, "houseNumber");
                        boolean houseNumber2 = checkMap(map, word, "houseNumber2");
                        boolean appartmentNumber = checkMap(map, word, "appartmentNumber");
                        boolean status = checkMap(map, word, "status");
                        boolean comment = checkMap(map, word, "comment");
                        if (!orderNumber && !firstName && !middleName && !surname
                                && !phoneNumber && !status && !email && !paymentMethod
                                && !deliveryMethod && !country && !city && !street
                                && !houseNumber && !houseNumber2 && !appartmentNumber
                                && !comment) {
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
                        default:
                            break;
                    }
                    return result;
                }
            })
            .sortBy(new Function1<Map<String, Object>, Long>() {
                public Long apply(Map<String, Object> item) {
                    return item.get("created") == null ? Long.valueOf(0) : (Long) item.get("created");
                }
            })    
            .value();
        foundOrders.addAll(selectedOrders);
        foundOrders.addAll(selectedOrders2);
        jTable1.setModel(new MyModel(foundOrders));
    }
    
    private boolean checkMap(Map<String, Object> map, String text, String key) {
        return text == null || text.trim().isEmpty()
            || (map.get(key) != null && $.isString(text) && (((String) map.get(key)).toLowerCase(localeRu)).contains(
                text.trim().toLowerCase(localeRu)));
    }
    
    private boolean checkNumbersMap(Map<String, Object> map, String text, String key) {
        if (text == null || text.trim().isEmpty()) {
            return true;
        }
        if (map.get(key) != null) {
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
            && (map.get(key) != null && ((String) map.get(key)).equals(text.trim()));
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
        jTextField1 = new javax.swing.JTextField();
        jTextField2 = new javax.swing.JTextField();
        jTextField3 = new javax.swing.JTextField();
        jTextField4 = new javax.swing.JTextField();
        jTextField5 = new javax.swing.JTextField();
        jTextField6 = new javax.swing.JTextField();
        jTextField10 = new javax.swing.JTextField();
        jTextField11 = new javax.swing.JTextField();
        jTextField12 = new javax.swing.JTextField();
        jTextField13 = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        jComboBox1 = new javax.swing.JComboBox();
        jComboBox2 = new javax.swing.JComboBox();
        jPanel2 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jButton9 = new javax.swing.JButton();
        jButton10 = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        jTextField14 = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        jTextField15 = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jTextField16 = new javax.swing.JTextField();
        jLabel27 = new javax.swing.JLabel();
        jTextField18 = new javax.swing.JTextField();
        jLabel23 = new javax.swing.JLabel();
        jComboBox3 = new javax.swing.JComboBox();
        jLabel28 = new javax.swing.JLabel();
        jTextField19 = new javax.swing.JTextField();
        jLabel29 = new javax.swing.JLabel();
        jButton12 = new javax.swing.JButton();
        jLabel24 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        jLabel17 = new javax.swing.JLabel();
        jComboBox4 = new HistoryComboBox();
        jLabel18 = new javax.swing.JLabel();
        jComboBox5 = new javax.swing.JComboBox();
        jLabel19 = new javax.swing.JLabel();
        jComboBox6 = new javax.swing.JComboBox();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        jLabel21 = new javax.swing.JLabel();
        jComboBox7 = new javax.swing.JComboBox();
        jComboBox8 = new javax.swing.JComboBox();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem3 = new javax.swing.JMenuItem();
        jCheckBoxMenuItem1 = new javax.swing.JCheckBoxMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Программа обработки заявок покупателей");

        jLabel2.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel2.setText("Заказ наряд №");

        jLabel3.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel3.setText("Имя покупателя");

        jLabel4.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel4.setText("Отчество покупателя");

        jLabel5.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel5.setText("Фамилия покупателя");

        jLabel6.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel6.setText("Телефон для связи");

        jLabel7.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel7.setText("E-mail");

        jLabel8.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel8.setText("Способ оплаты");

        jLabel9.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel9.setText("Способ доставки");

        jLabel10.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel10.setText("Город");

        jLabel11.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel11.setText("Улица");

        jLabel12.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel12.setText("Дом");

        jLabel13.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel13.setText("Корпус");

        jLabel14.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel14.setText("Номер квартиры");

        jLabel15.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel15.setText("Комментарии");

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
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextField2KeyReleased(evt);
            }
        });

        jTextField3.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTextField3.setNextFocusableComponent(jTextField4);
        jTextField3.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField3KeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextField3KeyReleased(evt);
            }
        });

        jTextField4.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTextField4.setNextFocusableComponent(jTextField5);
        jTextField4.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField4KeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextField4KeyReleased(evt);
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
        jTextField13.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField13KeyPressed(evt);
            }
        });

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        jComboBox1.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jComboBox1.setNextFocusableComponent(jComboBox2);
        jComboBox1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jComboBox1KeyPressed(evt);
            }
        });

        jComboBox2.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jComboBox2.setNextFocusableComponent(jComboBox7);
        jComboBox2.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jComboBox2KeyPressed(evt);
            }
        });

        jPanel2.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N

        jButton1.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jButton1.setText("Создать заказ");
        jButton1.setNextFocusableComponent(jButton12);
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

        jButton8.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jButton8.setText("Добавить");
        jButton8.setNextFocusableComponent(jButton1);
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });
        jButton8.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jButton8KeyPressed(evt);
            }
        });

        jButton9.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jButton9.setText("Печать");
        jButton9.setNextFocusableComponent(jButton10);
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });
        jButton9.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jButton9KeyPressed(evt);
            }
        });

        jButton10.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jButton10.setText("Выход");
        jButton10.setNextFocusableComponent(jTextField1);
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });
        jButton10.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jButton10KeyPressed(evt);
            }
        });

        jScrollPane3.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N

        jTable2.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Артикул", "Наименование", "Кол-во", "Цена", "Стоимость"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, true, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable2.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane3.setViewportView(jTable2);

        jLabel1.setFont(new java.awt.Font("Times New Roman", 2, 24)); // NOI18N
        jLabel1.setText("Формирование нового заказа");

        jTextField14.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTextField14.setNextFocusableComponent(jButton8);
        jTextField14.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField14KeyPressed(evt);
            }
        });

        jLabel16.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel16.setText("Итоговая сумма");

        jTextField15.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTextField15.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField15KeyPressed(evt);
            }
        });

        jLabel20.setText("Ввести артикул");

        jLabel22.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel22.setLabelFor(jTextField16);
        jLabel22.setText("Общая сумма клиента");
        jLabel22.setToolTipText("Общая сумма клиента");

        jTextField16.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTextField16.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField16KeyPressed(evt);
            }
        });

        jLabel27.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel27.setText("Скидка клиента");

        jTextField18.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTextField18.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField18KeyPressed(evt);
            }
        });

        jLabel23.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel23.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel23.setText("Статус заявки");
        jLabel23.setToolTipText("Статус заявки");

        jComboBox3.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jComboBox3.setNextFocusableComponent(jTextField14);
        jComboBox3.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jComboBox3KeyPressed(evt);
            }
        });

        jLabel28.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel28.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel28.setText("Дата приёма заказа");

        jTextField19.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTextField19.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField19KeyPressed(evt);
            }
        });

        jLabel29.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel29.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel29.setText("<html><font color=\"green\"><u>Быстрый переход на сайт</u> </font><font color=\"blue\"><u>www.sveta-shop.ru</u></font></html>");
        jLabel29.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jLabel29.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel29MouseClicked(evt);
            }
        });

        jButton12.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jButton12.setText("Сохранить");
        jButton12.setNextFocusableComponent(jButton9);
        jButton12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton12ActionPerformed(evt);
            }
        });
        jButton12.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jButton12KeyPressed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel20, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 490, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jTextField14, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel16))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton12, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton9, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jButton10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jTextField15, javax.swing.GroupLayout.DEFAULT_SIZE, 111, Short.MAX_VALUE))))
                .addGap(37, 37, 37))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jLabel1)
                        .addGap(163, 163, 163))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel28, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextField19, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel23, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel27, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel22, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField18, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField16, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(35, 35, 35))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel29, javax.swing.GroupLayout.PREFERRED_SIZE, 487, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel29, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel27)
                    .addComponent(jTextField18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel28)
                    .addComponent(jTextField19, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel22)
                        .addComponent(jTextField16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel23))
                    .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 89, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton8, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16)
                    .addComponent(jTextField15, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel20)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton9, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton10, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton12, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        jLabel24.setText("Номер для записи в базе");

        jLabel25.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel25.setText(" ");

        jLabel26.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel26.setText("Страна");

        jTextArea1.setColumns(15);
        jTextArea1.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(3);
        jTextArea1.setNextFocusableComponent(jComboBox3);
        jTextArea1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextArea1KeyPressed(evt);
            }
        });
        jScrollPane4.setViewportView(jTextArea1);

        jLabel17.setFont(new java.awt.Font("Times New Roman", 2, 12)); // NOI18N
        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel17.setLabelFor(jComboBox4);
        jLabel17.setText("<html>Номер в базе, номер заказа,<br/>имя покупателя, телефон, e-mail, способ оплаты, способ доставки, статус заявки, адрес, комментарии</html>");

        jComboBox4.setEditable(true);
        jComboBox4.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jComboBox4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox4ActionPerformed(evt);
            }
        });

        jLabel18.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jLabel18.setText("По дате создания");

        jComboBox5.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jComboBox5.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "за всё время", "за последний час", "за сегодня", "за эту неделю", "за этот месяц", "за этот год" }));
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
        jLabel19.setText("Автообновление");

        jComboBox6.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jComboBox6.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "не обновлять", "каждые 5 секунд", "каждые 60 секунд", "каждые 10 минут" }));
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

        jButton2.setFont(new java.awt.Font("Times New Roman", 2, 18)); // NOI18N
        jButton2.setText("Досье покупателя");
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

        jLabel21.setFont(new java.awt.Font("Times New Roman", 0, 12)); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel21, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(116, Short.MAX_VALUE)
                .addComponent(jLabel21)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 188, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel18, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel19, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jComboBox6, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 213, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jComboBox5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 213, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jComboBox4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 213, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(3, 3, 3)
                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(11, 11, 11)
                                .addComponent(jComboBox4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel18)
                            .addComponent(jComboBox5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel19)
                            .addComponent(jComboBox6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(11, 11, 11)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 97, Short.MAX_VALUE))
        );

        jComboBox7.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jComboBox7.setNextFocusableComponent(jComboBox8);
        jComboBox7.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jComboBox7KeyPressed(evt);
            }
        });

        jComboBox8.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jComboBox8.setNextFocusableComponent(jTextField10);
        jComboBox8.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jComboBox8KeyPressed(evt);
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

        jCheckBoxMenuItem1.setSelected(true);
        jCheckBoxMenuItem1.setText("Показывать панель поиска");
        jCheckBoxMenuItem1.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCheckBoxMenuItem1ItemStateChanged(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItem1);

        jMenuItem4.setText("Настроить справочники...");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem4);

        jMenuItem5.setText("Выйти из системы");
        jMenu3.add(jMenuItem5);

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
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(44, 44, 44)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel11, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel14, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel15, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel12, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jLabel10, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel26, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addGap(18, 18, 18))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField1)
                            .addComponent(jTextField2)
                            .addComponent(jTextField3)
                            .addComponent(jTextField4)
                            .addComponent(jTextField5)
                            .addComponent(jTextField6)
                            .addComponent(jComboBox1, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jComboBox2, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jTextField10)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jTextField11, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel13)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextField12, javax.swing.GroupLayout.DEFAULT_SIZE, 128, Short.MAX_VALUE))
                            .addComponent(jTextField13)
                            .addComponent(jScrollPane4)
                            .addComponent(jComboBox7, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jComboBox8, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel25, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel24, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addGap(28, 28, 28)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 502, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel24)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel25)
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
                            .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel9)
                            .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel26)
                            .addComponent(jComboBox7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel10)
                            .addComponent(jComboBox8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel11)
                            .addComponent(jTextField10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel12)
                            .addComponent(jTextField11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel13)
                            .addComponent(jTextField12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel14)
                            .addComponent(jTextField13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel15)
                            .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(9, 9, 9))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(8, 8, 8)
                                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(8, 8, 8))
                            .addComponent(jSeparator1))
                        .addContainerGap())))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        if (!checkLogin()) {
            return;
        }
        Map<String, Object> data = createOrderData();
        if (database.get("currentOrder") != null
            && !$.omit(data, "_id", "created", "status", "user", "country", "products").toString().equals(
            $.omit((Map<String, Object>) database.get("currentOrder"),
                "_id", "created", "status", "user", "country", "products").toString())) {
            saveData(data);
        }
        fillOrderForm(getCleanOrderData());
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

    private void jTextField10KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField10KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jTextField10KeyPressed

    private void jTextField11KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField11KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jTextField11KeyPressed

    private void jButton1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jButton1KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jButton1KeyPressed

    private void jComboBox3KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jComboBox3KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jComboBox3KeyPressed

    private void jTextField12KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField12KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jTextField12KeyPressed

    private void jTextField13KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField13KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jTextField13KeyPressed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        About dialog = new About(this, true);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        NewJDialog2 dialog = new NewJDialog2Builder()
                .setParent(this)
                .setModal(true)
                .setUseMysql(useMySql)
                .setHostName(hostName)
                .setDbName(dbName)
                .setUser(user)
                .setPass(pass)
                .setUseXlsx(useXlsx)
                .setXlsxPath(xlsxPath)
                .setShowDbNumber(showDbNumber)
                .createNewJDialog2();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        if (dialog.isApproved()) {
            dialog.getDbName();
            useMySql = dialog.getUseMySql();
            hostName = dialog.getHostName();
            dbName = dialog.getDbName();
            user = dialog.getUser();
            pass = dialog.getPass();
            useXlsx = dialog.getUseXlsx();
            xlsxPath = dialog.getXlsxPath();
            showDbNumber = dialog.getShowDbNumber();
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    jLabel24.setVisible(showDbNumber);
                    jLabel25.setVisible(showDbNumber);
                }
            });
        }
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        if (!checkLogin()) {
            return;
        }
        Optional<Map<String, Object>> product = database.get("productData") == null
                ? Optional.<Map<String, Object>>absent()
                : $.find((List<Map<String, Object>>) database.get("productData"),
                new Predicate<Map<String, Object>>() {
            public Boolean apply(Map<String, Object> f) {
                return jTextField14.getText().trim().equals(f.get("vendorCode"));
            }
        });
        if (product.isPresent()) {
            Map<String, Object> newProduct = (Map<String, Object>) $.clone(product.get());
            newProduct.put("quantity", "1");
            newProduct.put("totalPrice", newProduct.get("price"));
            ((MyProductModel) jTable2.getModel()).addModel(newProduct);
            jTextField14.setText("");
        }
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jButton8KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jButton8KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jButton8KeyPressed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        if (!checkLogin()) {
            return;
        }
        Map<String, Object> data = createOrderData();
        if (database.get("currentOrder") != null
            && !$.omit(data, "_id", "created", "status", "user", "country", "products").toString().equals(
            $.omit((Map<String, Object>) database.get("currentOrder"),
                "_id", "created", "status", "user", "country", "products").toString())) {
            saveData(data);
        }
        Map<String, Object> currentOrder = (Map<String, Object>) database.get("currentOrder");
        if (currentOrder != null) {
            if (currentOrder.get("created") == null) {
                return;
            }
            if (currentOrder.get("totalSum") == null) {
                List<Map<String, Object>> products = (List<Map<String, Object>>) currentOrder.get("products");
                long totalSum = calcTotalSum(products);
                long previousSum = calcPreviousSum(getFilteredOrders(getDatabaseData()));
                currentOrder.put("totalSum", "" + totalSum);
                currentOrder.put("discount", "" + calcDiscount(previousSum + totalSum));
            }
            Map<String, Object> clonedCurrentOrder = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : currentOrder.entrySet()) {
                if ("totalSum".equals(entry.getKey())) {
                    clonedCurrentOrder.put(entry.getKey(), entry.getValue());
                    clonedCurrentOrder.put("totalSumWithDiscount", getPriceWithDiscount(
                            (String) entry.getValue(), (String) currentOrder.get("discount")));
                } else if ("products".equals(entry.getKey())) {
                    List<Map<String, Object>> newProducts = new ArrayList<>();
                    for (Map<String, Object> product : (List<Map<String, Object>>) entry.getValue()) {
                        Map<String, Object> newProduct = new LinkedHashMap<>();
                        for (Map.Entry<String, Object> entryProduct : product.entrySet()) {
                            newProduct.put(entryProduct.getKey(), (String) entryProduct.getValue());
                        }
                        newProducts.add(newProduct);
                    }
                    clonedCurrentOrder.put(entry.getKey(), newProducts);
                } else {
                    clonedCurrentOrder.put(entry.getKey(), entry.getValue());
                }
            }
            String fileName = $.chain("order-", (String) currentOrder.get("_id"), ".xlsx").join("").item();
            new XlsxService(fileName).fillBlank(clonedCurrentOrder);
            createdFiles.add(fileName);
            try {
                java.awt.Desktop.getDesktop().browse(new File(fileName).toURI());
            } catch (IOException ex) {
                Logger.getLogger(Form1.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jButton9KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jButton9KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jButton9KeyPressed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }//GEN-LAST:event_jButton10ActionPerformed

    private void jButton10KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jButton10KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jButton10KeyPressed

    private void jTextField14KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField14KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jTextField14KeyPressed

    private void jTextField15KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField15KeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField15KeyPressed

    private void jTextField16KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField16KeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField16KeyPressed

    private void jTextField2KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField2KeyReleased
        fillOrderNumber();
    }//GEN-LAST:event_jTextField2KeyReleased

    private void jTextField3KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField3KeyReleased
        fillOrderNumber();
    }//GEN-LAST:event_jTextField3KeyReleased

    private void jTextField4KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField4KeyReleased
        fillOrderNumber();
    }//GEN-LAST:event_jTextField4KeyReleased

    private void jTextField18KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField18KeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField18KeyPressed

    private void jTextField19KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField19KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jTextField19KeyPressed

    private void jTextArea1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextArea1KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jTextArea1KeyPressed

    private void jLabel29MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel29MouseClicked
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI("http://www.sveta-shop.ru"));
        } catch (URISyntaxException ex) {
            Logger.getLogger(About.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(About.class.getName()).log(Level.SEVERE, null, ex);
        }

    }//GEN-LAST:event_jLabel29MouseClicked

    private void jButton12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton12ActionPerformed
        if (!checkLogin()) {
            return;
        }
        saveData(createOrderData());
    }//GEN-LAST:event_jButton12ActionPerformed

    private void jButton12KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jButton12KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jButton12KeyPressed

    private void jCheckBoxMenuItem1ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItem1ItemStateChanged
        setupSearchPanelVisible(((javax.swing.JCheckBoxMenuItem) evt.getItem()).getState());
    }//GEN-LAST:event_jCheckBoxMenuItem1ItemStateChanged

    private void jComboBox4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox4ActionPerformed
                searchOrders();
    }//GEN-LAST:event_jComboBox4ActionPerformed

    private void jComboBox5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox5ActionPerformed
                searchOrders();
    }//GEN-LAST:event_jComboBox5ActionPerformed

    private void jComboBox5KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jComboBox5KeyPressed
                focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jComboBox5KeyPressed

    private void jComboBox6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox6ActionPerformed
                initTimer();
    }//GEN-LAST:event_jComboBox6ActionPerformed

    private void jComboBox6KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jComboBox6KeyPressed
                focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jComboBox6KeyPressed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        if (!checkLogin()) {
            return;
        }
        dossieDialog.reload();
        dossieDialog.setVisible(true);
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton2KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jButton2KeyPressed
                focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jButton2KeyPressed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        if (!checkLogin()) {
            return;
        }
        searchOrders();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton3KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jButton3KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jButton3KeyPressed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        if (!checkLogin()) {
            return;
        }
        int result = chooser1.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            writeDataFile(chooser1.getSelectedFile().getAbsolutePath());
        }
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton4KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jButton4KeyPressed
        focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jButton4KeyPressed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        if (adminPass != null) {
            javax.swing.JLabel jLabel = new javax.swing.JLabel("Введите пароль администратора");
            final javax.swing.JTextField jPassword = new javax.swing.JPasswordField();
            Object[] objects = {jLabel, jPassword};
            String[] options = {"ОК", "Отмена"};
            int result = javax.swing.JOptionPane.showOptionDialog(this, objects,
                    "Пароль администратора", javax.swing.JOptionPane.OK_CANCEL_OPTION,
                    javax.swing.JOptionPane.QUESTION_MESSAGE, null, options, null);
            if (result == javax.swing.JOptionPane.OK_OPTION) {
                String passwordValue = jPassword.getText();
                if (!passwordValue.equals(adminPass)) {
                    final javax.swing.JOptionPane optionPane = new javax.swing.JOptionPane("Неверный пароль",
                            javax.swing.JOptionPane.INFORMATION_MESSAGE,
                            javax.swing.JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
                    final javax.swing.JDialog dialog = new javax.swing.JDialog();
                    dialog.setLocationRelativeTo(this);
                    dialog.setTitle("Неверный пароль");
                    dialog.setModal(true);
                    dialog.setContentPane(optionPane);
                    dialog.setDefaultCloseOperation(javax.swing.JDialog.DO_NOTHING_ON_CLOSE);
                    dialog.pack();
                    $.delay(new Function<Void>() {
                        public Void apply() {
                            dialog.dispose();
                            return null;
                        }
                    }, 1500);
                    dialog.setVisible(true);
                    return;
                }
            } else {
                return;
            }
        }
        NewJDialog5 dialog = new NewJDialog5(this, useXlsx, jComboBox1.getModel(),
                jComboBox2.getModel(), jComboBox3.getModel(), jComboBox7.getModel(),
                jComboBox8.getModel(),
                (List<Map<String, Object>>) database.get("productData"), useXlsx, xlsxPath, adminPass,
                users);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        if (dialog.isApproved()) {
            jComboBox1.setModel(dialog.getModel1());
            jComboBox2.setModel(dialog.getModel2());
            jComboBox3.setModel(dialog.getModel3());
            jComboBox7.setModel(dialog.getModel4());
            jComboBox8.setModel(dialog.getModel5());
            database.put("productData", dialog.getProductData());
            if (!users.equals(dialog.getUserData())) {
                users.clear();
                users.addAll(dialog.getUserData());
                database.put("userData", encrypt($.toJson(dialog.getUserData())));
                if (getActiveUsers().isEmpty()) {
                    enableButtons();
                } else {
                    logout().run();
                }
            }
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
            database.put("countryData", new ArrayList<String>());
            for (int index = 0; index < jComboBox7.getModel().getSize(); index += 1) {
                ((List<String>) database.get("countryData")).add(
                    String.valueOf(jComboBox7.getModel().getElementAt(index)).trim());
            }
            database.put("cityData", new ArrayList<String>());
            for (int index = 0; index < jComboBox8.getModel().getSize(); index += 1) {
                ((List<String>) database.get("cityData")).add(
                    String.valueOf(jComboBox8.getModel().getElementAt(index)).trim());
            }
            adminPass = dialog.getNewAdminPass();
        }
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jComboBox7KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jComboBox7KeyPressed
                focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jComboBox7KeyPressed

    private void jComboBox8KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jComboBox8KeyPressed
                focusNextElementOnPressEnter(evt);
    }//GEN-LAST:event_jComboBox8KeyPressed

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
            default:
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
                            searchOrders();
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
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItem1;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JComboBox jComboBox3;
    private javax.swing.JComboBox jComboBox4;
    private javax.swing.JComboBox jComboBox5;
    private javax.swing.JComboBox jComboBox6;
    private javax.swing.JComboBox jComboBox7;
    private javax.swing.JComboBox jComboBox8;
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
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
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
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField10;
    private javax.swing.JTextField jTextField11;
    private javax.swing.JTextField jTextField12;
    private javax.swing.JTextField jTextField13;
    private javax.swing.JTextField jTextField14;
    private javax.swing.JTextField jTextField15;
    private javax.swing.JTextField jTextField16;
    private javax.swing.JTextField jTextField18;
    private javax.swing.JTextField jTextField19;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    // End of variables declaration//GEN-END:variables
}
