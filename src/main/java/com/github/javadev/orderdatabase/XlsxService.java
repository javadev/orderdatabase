package com.github.javadev.orderdatabase;

import com.github.underscore.lodash.$;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class XlsxService {
    private static final Set<String> NAME_FIELDS = new HashSet<String>(Arrays.asList("firstName", "middleName", "surname"));
    private static final Map<String, String> COLUMN_NAME_TO_DB_NAME = new LinkedHashMap<String, String>() { {
        put("Заказа наряд №", "orderNumber");
        put("Заказчик", "customerName");
        put("Статус", "status");
        put("итоговая сумма заказа", "totalSum");
        put("скидка клиента", "discount");
        put("Телефон", "phoneNumber");
        put("Страна", "country");
        put("Город", "city");
        put("Улица", "street");
        put("Дом", "houseNumber");
        put("Корп.", "houseNumber2");
        put("Кв.", "appartmentNumber");
        put("e-mail", "email");
        put("Дата приема заказа", "created");
        put("Дата выполнения", "completed");
        put("Форма раслета", "paymentMethod");
        put("Форма доставки", "deliveryMethod");
        put("Комментарии", "comment");
    } };
    private final String xlsxPath;
    
    public XlsxService(String xlsxPath) {
        this.xlsxPath = xlsxPath == null || xlsxPath.isEmpty() ? "database.xlsx" : xlsxPath;
    }

    public List<Map<String, Object>> readAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!new File(xlsxPath).exists()) {
            return result;
        }
        Map<Integer, String> columnIndexToDbName = new LinkedHashMap<>();
        try {
            try (InputStream fileStream = new FileInputStream(xlsxPath)) {
                XSSFWorkbook book = new XSSFWorkbook(fileStream);
                XSSFSheet sheet = book.getSheetAt(0);
                Iterator<Row> rowIterator = sheet.iterator();
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    Iterator<Cell> cellIterator = row.cellIterator();
                    Map<String, Object> data = new LinkedHashMap<>();
                    while (cellIterator.hasNext()) {
                        Cell cell = cellIterator.next();
                        switch (cell.getCellType()) {
                            case Cell.CELL_TYPE_NUMERIC:
                                Double cellNumValue = cell.getNumericCellValue();
                                if (columnIndexToDbName.containsKey(cell.getColumnIndex())) {
                                    if ("created".equals(columnIndexToDbName.get(cell.getColumnIndex()))) {
                                        data.put(columnIndexToDbName.get(cell.getColumnIndex()), cellNumValue.longValue());
                                    } else {
                                        data.put(columnIndexToDbName.get(cell.getColumnIndex()), "" + cellNumValue.longValue());
                                    }
                                }
                                break;
                            case Cell.CELL_TYPE_STRING:
                                String cellValue = cell.getStringCellValue().trim();
                                if (row.getRowNum() == 0) {
                                    if ($.contains(COLUMN_NAME_TO_DB_NAME.keySet(), cellValue)) {
                                        columnIndexToDbName.put(cell.getColumnIndex(), COLUMN_NAME_TO_DB_NAME.get(cellValue));
                                    }
                                } else {
                                    if ("created".equals(columnIndexToDbName.get(cell.getColumnIndex()))) {
                                        data.put(columnIndexToDbName.get(cell.getColumnIndex()), Long.parseLong(cellValue));
                                    } else {
                                        if ("customerName".equals(columnIndexToDbName.get(cell.getColumnIndex()))) {
                                            String[] names = cellValue.split("\\s+");
                                            if (names.length == 1) {
                                                data.put("firstName", names[0]);
                                            } else if (names.length == 2) {
                                                data.put("surname", names[0]);
                                                data.put("firstName", names[1]);
                                            } else if (names.length > 2) {
                                                data.put("surname", names[0]);
                                                data.put("firstName", names[1]);
                                                data.put("middleName", names[2]);
                                            }
                                        } else {
                                            data.put(columnIndexToDbName.get(cell.getColumnIndex()), cellValue);
                                        }
                                    }
                                }
                                break;
                                
                        }
                    }
                    if (row.getRowNum() > 0) {
                        if (data.get("created") == null) {
                            data.put("created", new Date().getTime());
                        }
                        result.add(data);
                    }
                }
            }
//            System.out.println(columnIndexToDbName);
        } catch (IOException ex) {
            Logger.getLogger(XlsxService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }
    
    public void updateData(List<Map<String, Object>> newDataList) {
        List<Map<String, Object>> clonedNewDataList = new ArrayList<>();
        for (Map<String, Object> data : newDataList) {
            Map<String, Object> clonedData = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (!NAME_FIELDS.contains(entry.getKey())) {
                    clonedData.put(entry.getKey(), entry.getValue());
                }
            }
            clonedData.put("customerName", 
                    $.join($.compact(Arrays.asList((String) data.get("surname"),
                        (String) data.get("firstName"), (String) data.get("middleName")))));
            clonedNewDataList.add(clonedData);
        }
        if (!new File(xlsxPath).exists()) {
            copyResourceToFile();
        }
        FileInputStream stream;
        try {
            stream = new FileInputStream(new File(xlsxPath));
            XSSFWorkbook book = new XSSFWorkbook(stream);
            XSSFSheet sheet = book.getSheetAt(0);
            int cellIndex = 0;
            for (final String callName : COLUMN_NAME_TO_DB_NAME.keySet()) {
                XSSFCell cell = sheet.getRow(0).createCell(cellIndex);
                cell.setCellValue(callName);
                cellIndex += 1;
            }
            int rownum = 1;
            for (Map<String, Object> data : clonedNewDataList) {
                XSSFRow row = sheet.createRow(rownum++);
                int cellnum = 0; 
                for (final Map.Entry<String, String> columnEntry : COLUMN_NAME_TO_DB_NAME.entrySet()) {
                    XSSFCell cell = row.createCell(cellnum++);
                    if (data.get(columnEntry.getValue()) != null) {
                        cell.setCellValue((String) data.get(columnEntry.getValue()).toString());
                    }
                }
            }
            stream.close();
            try (FileOutputStream outputStream = new FileOutputStream(new File(xlsxPath))) {
                book.write(outputStream);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(XlsxService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(XlsxService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void copyResourceToFile() {
        InputStream stream = getClass().getResourceAsStream("/data.xlsx");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(xlsxPath);
            byte[] buf = new byte[2048];
            int r = stream.read(buf);
            while(r != -1) {
                fos.write(buf, 0, r);
                r = stream.read(buf);
            }
        } catch (IOException ex) {
            Logger.getLogger(XlsxService.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ex) {
                    Logger.getLogger(XlsxService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public void fillBlank(Map<String, Object> data) {
        if (!(data instanceof Map)) {
            return;
        }
        if (!new File(xlsxPath).exists()) {
            copyResourceToFile();
        }
        FileInputStream stream;
        try {
            stream = new FileInputStream(new File(xlsxPath));
            XSSFWorkbook book = new XSSFWorkbook(stream);
            XSSFSheet sheet = book.getSheetAt(1);
            fillCell(sheet, 7, 9, (String) data.get("orderNumber"));
            fillCell(sheet, 4, 11, $.join($.compact($.chain(
                    (String) data.get("surname"),
                    (String) data.get("firstName"),
                    (String) data.get("middleName")).value())));
            fillCell(sheet, 3, 12, (String) data.get("country"), "Россия");
            fillCell(sheet, 10, 12, (String) data.get("city"), "Москва");
            fillCell(sheet, 3, 13, (String) data.get("street"));
            fillCell(sheet, 3, 14, (String) data.get("houseNumber"));
            fillCell(sheet, 7, 14, (String) data.get("houseNumber2"));
            fillCell(sheet, 12, 14, (String) data.get("appartmentNumber"));
            fillCell(sheet, 4, 15, (String) data.get("phoneNumber"));
            fillCell(sheet, 3, 16, (String) data.get("email"));
            fillCell(sheet, 3, 16, (String) data.get("email"));
            fillCell(sheet, 5, 18, (String) data.get("comment"));
            fillCell(sheet, 24, 9, new SimpleDateFormat("dd/MM/yyyy").format(new Date((Long) data.get("created"))));
            fillCell(sheet, 29, 9, new SimpleDateFormat("HH:mm").format(new Date((Long) data.get("created"))));
            fillCell(sheet, 24, 11, (String) data.get("paymentMethod"));
            fillCell(sheet, 24, 12, (String) data.get("deliveryMethod"));
            fillCell(sheet, 28, 40, (String) data.get("totalSum"));
            if ((String) data.get("totalSum") != null && ((String) data.get("totalSum")).matches("\\d+")) {
            fillCell(sheet, 7, 42, new MoneyToStr(MoneyToStr.Currency.RUR, MoneyToStr.Language.RUS, MoneyToStr.Pennies.NUMBER)
                    .convert(Long.parseLong((String) data.get("totalSum")), 0L));
            } else {
                fillCell(sheet, 7, 42, "");
            }
            for (int index = 0; index < 15; index += 1) {
                fillCell(sheet, 2, 25 + index, "");
                fillCell(sheet, 8, 25 + index, "");
                fillCell(sheet, 20, 25 + index, "");
                fillCell(sheet, 23, 25 + index, "");
                fillCell(sheet, 28, 25 + index, "");
            }
            if (data.get("products") != null) {
                fillCell(sheet, 20, 40, String.valueOf(((List<Map<String, Object>>) data.get("products")).size()));
                int index = 0;
                for (Map<String, Object> product : (List<Map<String, Object>>) data.get("products")) {
                    fillCell(sheet, 2, 25 + index, (String) product.get("vendorCode"));
                    fillCell(sheet, 8, 25 + index, (String) product.get("name"));
                    fillCell(sheet, 20, 25 + index, (String) product.get("quantity"));
                    fillCell(sheet, 23, 25 + index, (String) product.get("price"));
                    fillCell(sheet, 28, 25 + index, (String) product.get("totalPrice"));
                    index += 1;
                    if (index > 15) {
                        break;
                    }
                }
            } else {
                fillCell(sheet, 20, 40, "0");
            }
            stream.close();
            try (FileOutputStream outputStream = new FileOutputStream(new File(xlsxPath))) {
                book.write(outputStream);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(XlsxService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(XlsxService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void fillCell(XSSFSheet sheet, int columnNumber, int rowNumber, String value) {
        XSSFRow row = sheet.getRow(rowNumber);
        XSSFCell cell = row.getCell(columnNumber);
        cell.setCellValue(value == null ? "" : value);
    }

    private void fillCell(XSSFSheet sheet, int columnNumber, int rowNumber, String value, String defaultValue) {
        fillCell(sheet, columnNumber, rowNumber, value == null ? defaultValue : value);
    }

    public List<Map<String, Object>> loadCatalog() {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!new File(xlsxPath).exists()) {
            return result;
        }
        try {
            try (InputStream fileStream = new FileInputStream(xlsxPath)) {
                XSSFWorkbook book = new XSSFWorkbook(fileStream);
                XSSFSheet sheet = book.getSheetAt(2);
                Iterator<Row> rowIterator = sheet.iterator();
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    Iterator<Cell> cellIterator = row.cellIterator();
                    Map<String, Object> data = new LinkedHashMap<>();
                    while (cellIterator.hasNext()) {
                        Cell cell = cellIterator.next();
                        switch (cell.getColumnIndex()) {
                            case 0:
                                data.put("vendorCode", cell.getStringCellValue().trim());
                                break;
                            case 1:
                                data.put("name", cell.getStringCellValue().trim());
                                break;
                            case 4:
                                if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                                    Double cellNumValue = cell.getNumericCellValue();
                                    data.put("price", "" + cellNumValue.longValue());
                                } else {
                                    data.put("price", cell.getStringCellValue().trim());
                                }
                                break;
                        }
                    }
                    if (row.getRowNum() > 0) {
                        result.add(data);
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(XlsxService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

}
