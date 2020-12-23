package handler;

import models.Inventory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import static config.constants.COUNT_COLUMN_NAME;
import static config.constants.DATABASE_URL;
import static config.constants.DELETE_INVENTORY;
import static config.constants.GET_CREATED_INVENTORY;
import static config.constants.GET_INVENTORY;
import static config.constants.ID_COLUMN_NAME;
import static config.constants.INITIALIZE_INVENTORY_TABLE;
import static config.constants.INSERT_INVENTORY;
import static config.constants.JDBC_DRIVER_CLASS;
import static config.constants.NAME_COLUMN_NAME;
import static config.constants.PASSWORD;
import static config.constants.SALES_COLUMN_NAME;
import static config.constants.UPDATE_INVENTORY;
import static config.constants.USERNAME;
import static config.constants.VERSION_COLUMN_NAME;

public class InventoryHandler {
    private Connection conn;
    private Statement stmt;
    private static InventoryHandler instance;

    // TODO: Add logger, refactor code
    // FIXME: handle when adding duplicated records

    private InventoryHandler() {
        conn = null;
        stmt = null;
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
        try {
            setUpConnectionAndStatement();
            stmt.execute(String.format(INSERT_INVENTORY, name, count));
            ResultSet rs = stmt.executeQuery(String.format(GET_CREATED_INVENTORY, name, count));
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
            closeConnectionAndStatement();
        }
    }

    /**
     * Return the queried Inventory object or empty result if not applicable.
     * @param id the inventory id for quering
     * @return an optional object containing queried result
     */
    public Optional<Inventory> getInventory(String id) {
        try {
            setUpConnectionAndStatement();
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
            closeConnectionAndStatement();
        }
    }

    /**
     * Replace and update the inventory record
     * @param inventory the new object to update for
     */
    public void updateInventory(Inventory inventory) {
        try {
            setUpConnectionAndStatement();
            stmt.execute(String.format(UPDATE_INVENTORY, inventory.getId(), inventory.getVersion()));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot setup JDBC connection:", e);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot update inventory:", e);
        } finally {
            closeConnectionAndStatement();
        }
    }

    /**
     * Delete the selected inventory from database
     * @param id the inventory id to delete
     */
    public void deleteInventory(String id) {
        try {
            setUpConnectionAndStatement();
            stmt.execute(String.format(DELETE_INVENTORY, id));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot setup JDBC connection:", e);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete inventory:", e);
        } finally {
            closeConnectionAndStatement();
        }
    }

    private Inventory toInventoryObject(ResultSet rs) throws SQLException {
        Inventory res = new Inventory();
        res.setId(String.valueOf(rs.getInt(ID_COLUMN_NAME)));
        res.setName(rs.getString(NAME_COLUMN_NAME));
        res.setSales(rs.getInt(SALES_COLUMN_NAME));
        res.setCount(rs.getInt(COUNT_COLUMN_NAME));
        res.setVersion(rs.getInt(VERSION_COLUMN_NAME));
        return res;
    }

    private void initializeInventoryTable() {
        try {
            setUpConnectionAndStatement();
            stmt.execute(INITIALIZE_INVENTORY_TABLE);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot setup JDBC connection:", e);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot initialize inventory table:", e);
        } finally {
            closeConnectionAndStatement();
        }
    }

    private void setUpConnectionAndStatement() throws ClassNotFoundException, SQLException {
        Class.forName(JDBC_DRIVER_CLASS);
        conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
        stmt = conn.createStatement();
    }

    private void closeConnectionAndStatement() {
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
