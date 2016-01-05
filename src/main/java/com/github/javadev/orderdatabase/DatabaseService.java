package com.github.javadev.orderdatabase;

import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseService {
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost/orderdb";

    static final String USER = "root";
    static final String PASS = "";

    public List<Map<String, Object>> readAll() {
        List<Map<String, Object>> result = Collections.emptyList();
        Connection conn = null;
        Statement stmt = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            stmt = conn.createStatement();

            String sql = "SELECT id, first, last, age FROM orderdata";
            try (ResultSet resultSet = stmt.executeQuery(sql)) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    int age = resultSet.getInt("age");
                    String first = resultSet.getString("first");
                    String last = resultSet.getString("last");
                    
                    //Display values
                    System.out.print("ID: " + id);
                    System.out.print(", Age: " + age);
                    System.out.print(", First: " + first);
                    System.out.println(", Last: " + last);
                }
            }
        } catch (SQLException | ClassNotFoundException  se) {
            if (se instanceof MySQLSyntaxErrorException) {
                String detailMessage = ((MySQLSyntaxErrorException) se).getMessage();
                if (detailMessage.contains("orderdata' doesn't exist")) {
//                    createTable();
                    Logger.getLogger(DatabaseService.class.getName()).log(Level.SEVERE, "I am here");
                }
            }
            Logger.getLogger(DatabaseService.class.getName()).log(Level.SEVERE, null, se);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException se) {
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException se) {
            }
        }
        return result;
    }
}
