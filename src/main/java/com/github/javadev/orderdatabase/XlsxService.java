package com.github.javadev.orderdatabase;

import com.github.underscore.lodash.$;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class XlsxService {
    private static final Map<String, String> COLUMN_NAME_TO_DB_NAME = new LinkedHashMap<String, String>() { {
        put("Заказа наряд №", "orderNumber");
        put("Имя", "firstName");
        put("Отчество", "middleName");
        put("Фамилия", "surname");
        put("Статус", "status");
//        put("итоговая сумма заказа", "totalSum");
//        put("скидка клиента", "discount");
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
                                        data.put(columnIndexToDbName.get(cell.getColumnIndex()), cellValue);
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
        if (!new File(xlsxPath).exists()) {
            XSSFWorkbook book = new XSSFWorkbook();
            XSSFSheet sheet = book.createSheet("база клиентов") ;
            XSSFRow row = sheet.createRow(0);
            int cellIndex = 0;
            for (final String callName : COLUMN_NAME_TO_DB_NAME.keySet()) {
                XSSFCell cell = row.createCell(cellIndex);
                cell.setCellValue(callName);
                cellIndex += 1;
            }
            try (FileOutputStream outputStream = new FileOutputStream(new File(xlsxPath))) {
                book.write(outputStream);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(XlsxService.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(XlsxService.class.getName()).log(Level.SEVERE, null, ex);
            }
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
            for (Map<String, Object> data : newDataList) {
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

}
