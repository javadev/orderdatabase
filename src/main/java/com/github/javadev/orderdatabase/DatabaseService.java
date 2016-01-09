package com.github.javadev.orderdatabase;

import com.github.underscore.lodash.$;
import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseService {
    private final static List<String> FIELD_NAMES = Arrays.asList(
        "created",
        "_id",
        "orderNumber",
        "firstName",
        "middleName",
        "surname",
        "phoneNumber",
        "email",
        "paymentMethod",
        "deliveryMethod",
        "city",
        "street",
        "houseNumber",
        "houseNumber2",
        "appartmentNumber",
        "status",
        "user",
        "comment"
    );
    private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private final String hostName;
    private final String dbName;
    private final String user;
    private final String pass;

    public DatabaseService(String hostName, String dbName, String user, String pass) {
        this.hostName = !$.isString(hostName) || hostName.trim().isEmpty() ? "localhost" : $.escape(hostName);
        this.dbName = !$.isString(dbName) || dbName.trim().isEmpty() ? "orderdb" : $.escape(dbName);
        this.user = !$.isString(user) || user.trim().isEmpty() ? "root" : user;
        this.pass = !$.isString(pass) || pass.trim().isEmpty() ? "" : pass;
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
            String sql = "SELECT " + $.join(FIELD_NAMES, ", ") + " FROM orderdata";
            try (ResultSet resultSet = stmt.executeQuery(sql)) {
                while (resultSet.next()) {
                    Map<String, Object> data = new LinkedHashMap<String, Object>();
                    for (String field : FIELD_NAMES) {
                        data.put(field, "created".equals(field) ? resultSet.getLong(field) : resultSet.getString(field));
                    }
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
    
    private void alterTable(Statement stmt) throws SQLException {
        stmt.getConnection().setAutoCommit(false);
        String sqlAlter1 = "ALTER TABLE orderdata ADD status VARCHAR(255);";
        stmt.executeUpdate(sqlAlter1);
        String sqlAlter2 = "ALTER TABLE orderdata ADD user VARCHAR(255);";
        stmt.executeUpdate(sqlAlter2);
        stmt.getConnection().commit();    
    }
    
    private void createTable(Statement stmt) throws SQLException {
        stmt.getConnection().setAutoCommit(false);
        String sql = "CREATE TABLE orderdata "
                   + "(_id VARCHAR(16) not NULL,"
                   + "created BIGINT,"
                   + $.join($.without(FIELD_NAMES, "_id", "created"), " VARCHAR(255),")
                   + " TEXT, PRIMARY KEY ( _id ))";
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
                            + "(" + $.join(FIELD_NAMES, ", ") + ") VALUES"
                            + "(" + $.repeat("?,", FIELD_NAMES.size() - 1) + "?)";
            stmt = conn.prepareStatement(insertTableSQL);
            stmt.getConnection().setAutoCommit(false);
            for (Map<String, Object> data : dataList) {
                int index = 1;
                for (String field : FIELD_NAMES) {
                    if ("created".equals(field)) {
                        stmt.setLong(index, (Long) data.get(field));
                    } else {
                        stmt.setString(index, (String) data.get(field));
                    }
                    index += 1;
                }
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
            } else if (detailMessage.contains("Unknown column 'status'")) {
                try {
                    alterTable(stmt);
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
