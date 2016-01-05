package com.github.javadev.orderdatabase;

import com.github.underscore.lodash.$;
import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseService {
    private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private final String hostName;
    private final String dbName;
    private final String user;
    private final String pass;

    public DatabaseService(String hostName, String dbName, String user, String pass) {
        this.hostName = hostName == null ? "localhost" : $.escape(hostName);
        this.dbName = dbName == null ? "orderdb" : $.escape(dbName);
        this.user = user == null ? "root" : user;
        this.pass = pass == null ? "" : pass;
    }

    private String getDbUrl() {
        return "jdbc:mysql://" + hostName + "/" + dbName + "?useUnicode=true&characterEncoding=utf-8";
    }

    public List<Map<String, Object>> readAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = createConnection();
            stmt = conn.createStatement();
            String sql = "SELECT _id, firstName, middleName, surname FROM orderdata";
            try (ResultSet resultSet = stmt.executeQuery(sql)) {
                while (resultSet.next()) {
                    Map<String, Object> data = new LinkedHashMap<String, Object>();
                    data.put("_id", resultSet.getString("_id"));
                    data.put("firstName", resultSet.getString("firstName"));
                    data.put("middleName", resultSet.getString("middleName"));
                    data.put("surname", resultSet.getString("surname"));
                    result.add(data);
                }
            }
        } catch (SQLException | ClassNotFoundException se) {
            checkExceptionAndCreateTable(se, stmt);
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
                   + "(_id VARCHAR(16) not NULL,"
                   + " firstName VARCHAR(255),"
                   + " middleName VARCHAR(255),"
                   + " surname VARCHAR(255),"
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
    
    public void insertData(List<Map<String, Object>> dataList) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = createConnection();
            String insertTableSQL = "INSERT INTO orderdata"
                            + "(_id, firstName, middleName, surname) VALUES"
                            + "(?,?,?,?)";
            stmt = conn.prepareStatement(insertTableSQL);
            stmt.getConnection().setAutoCommit(false);
            for (Map<String, Object> data : dataList) {
                stmt.setString(1, (String) data.get("_id"));
                stmt.setString(2, (String) data.get("firstName"));
                stmt.setString(3, (String) data.get("middleName"));
                stmt.setString(4, (String) data.get("surname"));
                stmt.executeUpdate();
            }
            stmt.getConnection().commit();
        } catch (SQLException | ClassNotFoundException se) {
            checkExceptionAndCreateTable(se, stmt);
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
    }

    private void checkExceptionAndCreateTable(final Exception se, Statement stmt) {
        if (se instanceof MySQLSyntaxErrorException) {
            String detailMessage = ((MySQLSyntaxErrorException) se).getMessage();
            if (detailMessage.contains("orderdata' doesn't exist")) {
                try {
                    createTable(stmt);
                    return;
                } catch (SQLException ex) {
                    Logger.getLogger(DatabaseService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        Logger.getLogger(DatabaseService.class.getName()).log(Level.SEVERE, null, se);                    
    }

    private Connection createConnection() throws ClassNotFoundException, SQLException {
        Connection conn;
        Class.forName("com.mysql.jdbc.Driver");
        conn = DriverManager.getConnection(getDbUrl(), user, pass);
        return conn;
    }
}
