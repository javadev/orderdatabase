package com.github.javadev.orderdatabase;

import com.github.underscore.U;
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
import java.util.function.Function;

public class DatabaseService {
    private static final List<String> FIELD_NAMES = Arrays.asList(
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
        "country",
        "city",
        "street",
        "houseNumber",
        "houseNumber2",
        "appartmentNumber",
        "status",
        "totalSum",
        "discount",
        "user",
        "comment"
    );
    private static final List<String> PRODUCT_FIELD_NAMES = Arrays.asList(
        "_id",
        "orderId",
        "vendorCode",
        "name",
        "price",
        "quantity",
        "totalPrice"
    );
    private static final String CREATE_ORDERDATA_SQL = "CREATE TABLE orderdata "
                   + "(_id VARCHAR(16) not NULL,"
                   + "created BIGINT,"
                   + U.join(U.without(FIELD_NAMES, "_id", "created"), " VARCHAR(255),")
                   + " TEXT, PRIMARY KEY ( _id ))";
    private static final String CREATE_PRODUCTDATA_SQL = "CREATE TABLE productdata "
                   + "(_id VARCHAR(16) not NULL,"
                   + U.join(U.without(PRODUCT_FIELD_NAMES, "_id"), " VARCHAR(255),")
                   + " VARCHAR(255), PRIMARY KEY ( _id ))";
    private static final String SELECT_ORDERDATA_SQL = "SELECT " + U.join(FIELD_NAMES, ", ") + " FROM orderdata";
    private static final String SELECT_PRODUCTDATA_SQL = "SELECT " + U.join(PRODUCT_FIELD_NAMES, ", ") + " FROM productdata";
    private static final String INSERT_ORDERDATA_SQL = "INSERT INTO orderdata"
                            + "(" + U.join(FIELD_NAMES, ", ") + ") VALUES"
                            + "(" + U.repeat("?,", FIELD_NAMES.size() - 1) + "?)";
    private static final String INSERT_PRODUCTDATA_SQL = "INSERT INTO productdata"
                            + "(" + U.join(PRODUCT_FIELD_NAMES, ", ") + ") VALUES"
                            + "(" + U.repeat("?,", PRODUCT_FIELD_NAMES.size() - 1) + "?)";
    private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private final String hostName;
    private final String dbName;
    private final String user;
    private final String pass;

    public DatabaseService(String hostName, String dbName, String user, String pass) {
        this.hostName = !U.isString(hostName) || hostName.trim().isEmpty() ? "localhost" : U.escape(hostName);
        this.dbName = !U.isString(dbName) || dbName.trim().isEmpty() ? "orderdb" : U.escape(dbName);
        this.user = !U.isString(user) || user.trim().isEmpty() ? "root" : user;
        this.pass = !U.isString(pass) || pass.trim().isEmpty() ? "" : pass;
    }

    private String getDbUrl() {
        return "jdbc:mysql://" + hostName + "/" + dbName + "?useUnicode=true&characterEncoding=utf-8";
    }

    public List<Map<String, Object>> readAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        Connection conn = null;
        Statement stmt1 = null;
        Statement stmt2 = null;
        try {
            conn = createConnection();
            stmt1 = conn.createStatement();
            try (ResultSet resultSet = stmt1.executeQuery(SELECT_ORDERDATA_SQL)) {
                while (resultSet.next()) {
                    Map<String, Object> data = new LinkedHashMap<>();
                    for (String field : FIELD_NAMES) {
                        data.put(field, "created".equals(field) ? resultSet.getLong(field) : resultSet.getString(field));
                    }
                    result.add(data);
                }
            }
            Map<String, List<Map<String, Object>>> resultById = U.groupBy(result,
                new Function<Map<String, Object>, String>() {
                    @Override
                    public String apply(Map<String, Object> item) {
                        return (String) item.get("_id");
                    }
                });
            stmt2 = conn.createStatement();
            try (ResultSet resultSet = stmt2.executeQuery(SELECT_PRODUCTDATA_SQL)) {
                while (resultSet.next()) {
                    Map<String, Object> data = new LinkedHashMap<>();
                    for (String field : PRODUCT_FIELD_NAMES) {
                        data.put(field, "created".equals(field) ? resultSet.getLong(field) : resultSet.getString(field));
                    }
                    if (data.get("orderId") != null) {
                        Map<String, Object> order = resultById.get((String) data.get("orderId")).get(0);
                        if (order.get("products") == null) {
                            order.put("products", new ArrayList<Map<String, Object>>());
                        }
                        ((List<Map<String, Object>>) order.get("products")).add(data);
                    }
                }
            }
        } catch (SQLException | ClassNotFoundException se) {
            checkExceptionAndCreateTable(se, conn);
        } finally {
            closeStatement(stmt1);
            closeStatement(stmt2);
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
    
    private void closeStatement(Statement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException se) {
        }
    }

    private void alterTable(Connection conn) throws SQLException {
        Statement stmt = null;
        try {
        stmt = conn.createStatement();
        stmt.getConnection().setAutoCommit(false);
        String sqlAlter1 = "ALTER TABLE orderdata ADD status VARCHAR(255);";
        stmt.executeUpdate(sqlAlter1);
        String sqlAlter2 = "ALTER TABLE orderdata ADD user VARCHAR(255);";
        stmt.executeUpdate(sqlAlter2);
        stmt.getConnection().commit();
        } finally {
            closeStatement(stmt);
        }
    }
    
    private void alterTableCountry(Connection conn) throws SQLException {
        Statement stmt = null;
        try {
        stmt = conn.createStatement();
        stmt.getConnection().setAutoCommit(false);
        String sqlAlter1 = "ALTER TABLE orderdata ADD country VARCHAR(255);";
        stmt.executeUpdate(sqlAlter1);
        stmt.getConnection().commit();    
        } finally {
            closeStatement(stmt);
        }
    }
    
    private void alterTableTotalSum(Connection conn) throws SQLException {
        Statement stmt = null;
        try {
        stmt = conn.createStatement();
        stmt.getConnection().setAutoCommit(false);
        String sqlAlter1 = "ALTER TABLE orderdata ADD totalSum VARCHAR(255);";
        stmt.executeUpdate(sqlAlter1);
        String sqlAlter2 = "ALTER TABLE orderdata ADD discount VARCHAR(255);";
        stmt.executeUpdate(sqlAlter2);
        stmt.getConnection().commit();    
        } finally {
            closeStatement(stmt);
        }
    }

    private void createTable(Connection conn) throws SQLException {
        Statement stmt = null;
        try {
        stmt = conn.createStatement();
        stmt.getConnection().setAutoCommit(false);
        stmt.executeUpdate(CREATE_ORDERDATA_SQL);
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
        } finally {
            closeStatement(stmt);
        }
    }

    private void createProductTable(Connection conn) throws SQLException {
        Statement stmt = null;
        try {
        stmt = conn.createStatement();
        stmt.getConnection().setAutoCommit(false);
        stmt.executeUpdate(CREATE_PRODUCTDATA_SQL);
        String restrictionUpdate =
            "CREATE TRIGGER productdata_upd BEFORE UPDATE ON productdata FOR EACH ROW\n"
            + "  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot update record';\n"
            + ";";
        stmt.executeUpdate(restrictionUpdate);
        String restrictionDelete =
            "CREATE TRIGGER productdata_del BEFORE DELETE ON productdata FOR EACH ROW\n"
            + "  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot delete record';\n"
            + ";";
        stmt.executeUpdate(restrictionDelete);
        stmt.getConnection().commit();
        } finally {
            closeStatement(stmt);
        }
    }
        
    public void insertData(List<Map<String, Object>> dataList) {
        Connection conn = null;
        PreparedStatement stmt1 = null;
        PreparedStatement stmt2 = null;
        try {
            conn = createConnection();
            stmt1 = conn.prepareStatement(INSERT_ORDERDATA_SQL);
            stmt1.getConnection().setAutoCommit(false);
            for (Map<String, Object> data : dataList) {
                int index = 1;
                for (String field : FIELD_NAMES) {
                    if ("created".equals(field)) {
                        stmt1.setLong(index, (Long) data.get(field));
                    } else {
                        stmt1.setString(index, (String) data.get(field));
                    }
                    index += 1;
                }
                stmt1.executeUpdate();
            }
            stmt2 = conn.prepareStatement(INSERT_PRODUCTDATA_SQL);
            stmt2.getConnection().setAutoCommit(false);
            for (Map<String, Object> data : dataList) {
                if (data.get("products") == null || ((List<Map<String, Object>>) data.get("products")).isEmpty()) {
                    continue;
                }
                for (Map<String, Object> product : (List<Map<String, Object>>) data.get("products")) {
                    int index = 1;
                    for (String field : PRODUCT_FIELD_NAMES) {
                        stmt2.setString(index, (String) product.get(field));
                        index += 1;
                    }
                }
                stmt2.executeUpdate();
            }
            conn.commit();
        } catch (SQLException | ClassNotFoundException se) {
            checkExceptionAndCreateTable(se, conn);
        } finally {
            closeStatement(stmt1);
            closeStatement(stmt2);
            try {
                if (conn != null) {
                    conn.rollback();
                    conn.close();
                }
            } catch (SQLException se) {
            }
        }
    }

    private void checkExceptionAndCreateTable(final Exception se, Connection conn) {
        if (se instanceof SQLException) {
            String detailMessage = ((SQLException) se).getMessage();
            if (detailMessage.contains("orderdata' doesn't exist")) {
                try {
                    createTable(conn);
                    createProductTable(conn);
                    return;
                } catch (SQLException ex) {
                    Log.error(ex, null);
                }
            } else if (detailMessage.contains("Unknown column 'status'")) {
                try {
                    alterTable(conn);
                    alterTableCountry(conn);
                    createProductTable(conn);
                    alterTableTotalSum(conn);
                    return;
                } catch (SQLException ex) {
                    Log.error(ex, null);
                }
            } else if (detailMessage.contains("Unknown column 'country'")) {
                try {
                    alterTableCountry(conn);
                    createProductTable(conn);
                    alterTableTotalSum(conn);
                    return;
                } catch (SQLException ex) {
                    Log.error(ex, null);
                }
            } else if (detailMessage.contains("productdata' doesn't exist")) {
                try {
                    createProductTable(conn);
                    alterTableTotalSum(conn);
                    return;
                } catch (SQLException ex) {
                    Log.error(ex, null);
                }
            } else if (detailMessage.contains("Unknown column 'totalSum'")) {
                try {
                    alterTableTotalSum(conn);
                    return;
                } catch (SQLException ex) {
                    Log.error(ex, null);
                }
            }
        }
        Log.error(se, null);
    }

    private Connection createConnection() throws ClassNotFoundException, SQLException {
        Connection conn;
        Class.forName(JDBC_DRIVER);
        conn = DriverManager.getConnection(getDbUrl(), user, pass);
        return conn;
    }
}
