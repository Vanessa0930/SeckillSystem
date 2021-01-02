package main.java.com.seckillservice.handler;

import main.java.com.seckillservice.common.models.Inventory;
import main.java.com.seckillservice.utils.constants;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Optional;

import static main.java.com.seckillservice.utils.constants.CONNECTION;
import static main.java.com.seckillservice.utils.constants.GET_INVENTORY;
import static main.java.com.seckillservice.utils.constants.INITIALIZE_INVENTORY_TABLE;
import static main.java.com.seckillservice.utils.constants.STATEMENT;

public class InventoryHandler {
    private static InventoryHandler instance;

    // TODO: Add logger, refactor code
    // FIXME: handle when adding duplicated records

    private InventoryHandler() {
        initializeInventoryTable();
    }

    public static InventoryHandler getInstance() {
        if (instance == null) {
            instance = new InventoryHandler();
        }
        return instance;
    }

    /**
     * Create an inventory record in the database
     * @param name inventory name
     * @param count inital count of on stock items
     * @return the inventory object containing all information
     */
    public Inventory createInventory(String name, int count) {
        Connection conn = null;
        Statement stmt = null;
        try {
            Map<String, Object> result = setUpConnectionAndStatement();
            conn = (Connection) result.get(CONNECTION);
            stmt = (Statement) result.get(STATEMENT);
            stmt.execute(String.format(constants.INSERT_INVENTORY, name, count));
            ResultSet rs = stmt.executeQuery(String.format(constants.GET_CREATED_INVENTORY, name, count));
            if (!rs.next()) {
                throw new RuntimeException("No matching record found");
            }
            Inventory res = toInventoryObject(rs);

            rs.close();
            return res;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot setup JDBC connection:", e);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot create new inventory entry:", e);
        } finally {
            closeConnectionAndStatement(conn, stmt);
        }
    }

    /**
     * Return the queried Inventory object or empty result if not applicable.
     * @param id the inventory id for quering
     * @return an optional object containing queried result
     */
    public Optional<Inventory> getInventory(String id) {
        Connection conn = null;
        Statement stmt = null;
        try {
            Map<String, Object> map = setUpConnectionAndStatement();
            conn = (Connection) map.get(CONNECTION);
            stmt = (Statement) map.get(STATEMENT);
            ResultSet rs = stmt.executeQuery(String.format(GET_INVENTORY, id));
            Optional<Inventory> result = Optional.empty();
            if (!rs.next()) {
                return result;
            } else {
                result = Optional.of(toInventoryObject(rs));
            }

            return result;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot setup JDBC connection:", e);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot get inventory entry:", e);
        } finally {
            closeConnectionAndStatement(conn, stmt);
        }
    }

    /**
     * Replace and update the inventory record following optimistic lock strategy
     * @param inventory the new object to update for
     */
    public void updateInventoryOptimistically(Inventory inventory) {
        Connection conn = null;
        Statement stmt = null;
        try {
            Map<String, Object> map = setUpConnectionAndStatement();
            conn = (Connection) map.get(CONNECTION);
            stmt = (Statement) map.get(STATEMENT);
            stmt.execute(String.format(constants.UPDATE_INVENTORY, inventory.getId(), inventory.getVersion()));

            inventory.setCount(inventory.getCount() - 1);
            inventory.setSales(inventory.getSales() + 1);
            inventory.setVersion(inventory.getVersion() + 1);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot setup JDBC connection:", e);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot update inventory:", e);
        } finally {
            closeConnectionAndStatement(conn, stmt);
        }
    }

    /**
     * Delete the selected inventory from database
     * @param id the inventory id to delete
     */
    public void deleteInventory(String id) {
        Connection conn = null;
        Statement stmt = null;
        try {
            Map<String, Object> map = setUpConnectionAndStatement();
            conn = (Connection) map.get(CONNECTION);
            stmt = (Statement) map.get(STATEMENT);
            stmt.execute(String.format(constants.DELETE_INVENTORY, id));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot setup JDBC connection:", e);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete inventory:", e);
        } finally {
            closeConnectionAndStatement(conn, stmt);
        }
    }

    private Inventory toInventoryObject(ResultSet rs) throws SQLException {
        Inventory res = new Inventory();
        res.setId(String.valueOf(rs.getInt(constants.ID_COLUMN_NAME)));
        res.setName(rs.getString(constants.NAME_COLUMN_NAME));
        res.setSales(rs.getInt(constants.SALES_COLUMN_NAME));
        res.setCount(rs.getInt(constants.COUNT_COLUMN_NAME));
        res.setVersion(rs.getInt(constants.VERSION_COLUMN_NAME));
        return res;
    }

    private void initializeInventoryTable() {
        Connection conn = null;
        Statement stmt = null;
        try {
            Map<String, Object> map = setUpConnectionAndStatement();
            conn = (Connection) map.get(CONNECTION);
            stmt = (Statement) map.get(STATEMENT);
            stmt.execute(INITIALIZE_INVENTORY_TABLE);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot setup JDBC connection:", e);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot initialize inventory table:", e);
        } finally {
            closeConnectionAndStatement(conn, stmt);
        }
    }

    private Map<String, Object> setUpConnectionAndStatement() throws ClassNotFoundException, SQLException {
        Class.forName(constants.JDBC_DRIVER_CLASS);
        Connection conn = DriverManager.getConnection(
                constants.DATABASE_URL, constants.USERNAME, constants.PASSWORD);
        Statement stmt = conn.createStatement();
        return Map.of(CONNECTION, conn, STATEMENT, stmt);
    }

    private void closeConnectionAndStatement(Connection conn, Statement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            // ignore exceptions
        }
    }
}
