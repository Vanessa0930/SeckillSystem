package com.seckillservice.handler;

import com.seckillservice.common.models.Inventory;
import com.seckillservice.utils.constants;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.seckillservice.utils.constants.CONNECTION;
import static com.seckillservice.utils.constants.GET_ALL_INVENTORY_IDS;
import static com.seckillservice.utils.constants.GET_INVENTORY;
import static com.seckillservice.utils.constants.GET_INVENTORY_PER_VERSION;
import static com.seckillservice.utils.constants.ID_COLUMN_NAME;
import static com.seckillservice.utils.constants.INITIALIZE_INVENTORY_TABLE;
import static com.seckillservice.utils.constants.STATEMENT;

@Component
public class InventoryHandler {
    // TODO: Add logger, refactor code
    // FIXME: handle when adding duplicated records

    public InventoryHandler() {
        initializeInventoryTable();
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
            stmt.executeUpdate(String.format(constants.INSERT_INVENTORY, name, count),
                    Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = stmt.getGeneratedKeys();
            if (!rs.next()) {
                throw new RuntimeException("No matching record id returned");
            }

            int id = rs.getInt(1);
            Inventory res = new Inventory(String.valueOf(id), name, count, 0, 1);

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
                Inventory res = new Inventory(
                        String.valueOf(rs.getInt(constants.ID_COLUMN_NAME)),
                        rs.getString(constants.NAME_COLUMN_NAME),
                        rs.getInt(constants.COUNT_COLUMN_NAME),
                        rs.getInt(constants.SALES_COLUMN_NAME),
                        rs.getInt(constants.VERSION_COLUMN_NAME));
                result = Optional.of(res);
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
    public void updateInventoryOptimistically(Inventory inventory) throws SQLException {
        Connection conn = null;
        Statement stmt = null;

        // control transaction commit and rollback if encountered failure
        try {
            Map<String, Object> map = setUpConnectionAndStatement();
            conn = (Connection) map.get(CONNECTION);
            stmt = (Statement) map.get(STATEMENT);
            conn.setAutoCommit(false);
            ResultSet rs = stmt.executeQuery(String.format(GET_INVENTORY_PER_VERSION,
                    inventory.getId(), inventory.getVersion()));
            if (!rs.next()) {
                throw new RuntimeException(String.format(
                        "Failed to update database for inventory %s version %d: no such record",
                        inventory.getId(), inventory.getVersion()));
            }

            stmt.execute(String.format(constants.UPDATE_INVENTORY, inventory.getId(), inventory.getVersion()));
            conn.commit();

            inventory.setCount(inventory.getCount() - 1);
            inventory.setSales(inventory.getSales() + 1);
            inventory.setVersion(inventory.getVersion() + 1);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot setup JDBC connection:", e);
        } catch (SQLException e) {
            conn.rollback();
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

    public List<String> listAllInventoryIds() {
        Connection conn = null;
        Statement stmt = null;
        List<String> result = new ArrayList<>();
        try {
            Map<String, Object> map = setUpConnectionAndStatement();
            conn = (Connection) map.get(CONNECTION);
            stmt = (Statement) map.get(STATEMENT);

            ResultSet rs = stmt.executeQuery(GET_ALL_INVENTORY_IDS);
            while (rs.next()) {
                result.add(rs.getString(ID_COLUMN_NAME));
            }
            return result;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot setup JDBC connection:", e);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete inventory:", e);
        } finally {
            closeConnectionAndStatement(conn, stmt);
        }
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
