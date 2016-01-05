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
                    String id = resultSet.getString("_id");
                    String firstName = resultSet.getString("firstName");
                    String middleName = resultSet.getString("middleName");
                    String surname = resultSet.getString("surname");
                    
                    System.out.print("ID: " + id);
                    System.out.print(", First: " + firstName);
                    System.out.print(", Middle: " + middleName);
                    System.out.println(", surname: " + surname);
                }
            }
        } catch (SQLException | ClassNotFoundException se) {
            if (se instanceof MySQLSyntaxErrorException) {
                String detailMessage = ((MySQLSyntaxErrorException) se).getMessage();
                if (detailMessage.contains("orderdata' doesn't exist")) {
                    try {
                        createTable(stmt);
                    } catch (SQLException ex) {
                        Logger.getLogger(DatabaseService.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    Logger.getLogger(DatabaseService.class.getName()).log(Level.SEVERE, null, se);                    
                }
            }
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException se) {
            }
            try {
                if (conn != null) {
                    conn.rollback();
                    conn.close();
                }
            } catch (SQLException se) {
            }
        }
        return result;
    }
    
    private void createTable(Statement stmt) throws SQLException {
        stmt.getConnection().setAutoCommit(false);
        String sql = "CREATE TABLE orderdata "
                   + "(_id VARCHAR(16) not NULL, "
                   + " firstName VARCHAR(255), "
                   + " middleName VARCHAR(255), "
                   + " surname VARCHAR(255), "
                   + " PRIMARY KEY ( _id ))"; 
        stmt.executeUpdate(sql);
        String restrictionUpdate =
            "CREATE TRIGGER orderdata_upd BEFORE UPDATE ON orderdata FOR EACH ROW\n"
            + "  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot update record';\n"
            + ";";
        stmt.executeUpdate(restrictionUpdate);
        String restrictionDelete =
            "CREATE TRIGGER orderdata_del BEFORE DELETE ON orderdata FOR EACH ROW\n"
            + "  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot delete record';\n"
            + ";";
        stmt.executeUpdate(restrictionDelete);
        stmt.getConnection().commit();
    }
}
