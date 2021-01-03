package main.java.com.seckillservice.handler;

import main.java.com.seckillservice.common.models.Transaction;
import main.java.com.seckillservice.utils.constants;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static main.java.com.seckillservice.utils.constants.CONNECTION;
import static main.java.com.seckillservice.utils.constants.STATEMENT;

public class TransactionHandler {
    // TODO: Add logger, refactor code
    // FIXME: handle when adding duplicated records

    public TransactionHandler() {
        initializeTransactionTable();
    }

    /**
     * Create a transaction record in the database
     * @param inventoryId the inventory Id to insert
     * @return the transaction object containing all information
     */
    public Transaction createTransaction(String inventoryId) {
        Connection conn = null;
        Statement stmt = null;
        try {
            Map<String, Object> map = setUpConnectionAndStatement();
            conn = (Connection) map.get(CONNECTION);
            stmt = (Statement) map.get(STATEMENT);
            long timestamp = new Date().getTime() / 1000;
            stmt.executeUpdate(String.format(constants.INSERT_TRANSACTION, inventoryId, timestamp),
                    Statement.RETURN_GENERATED_KEYS);

            // FIXME: BUG: Get transaction id directly, not work when timestamp is the same
            ResultSet rs = stmt.getGeneratedKeys();
            if (!rs.next()) {
                throw new RuntimeException("No matching transaction id returned");
            }
            int id = rs.getInt(1);
            Transaction res = new Transaction();
            res.setId(String.valueOf(id));
            res.setInventoryId(inventoryId);
            res.setCreateDate(new Date(timestamp));

            rs.close();
            return res;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot setup JDBC connection:", e);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot create new transaction entry:", e);
        } finally {
            closeConnectionAndStatement(conn, stmt);
        }
    }

    /**
     * Return the queried Inventory object or empty result if not applicable.
     * @param id the inventory id for quering
     * @return an optional object containing queried result
     */
    public Optional<Transaction> getTransaction(String id) {
        Connection conn = null;
        Statement stmt = null;
        try {
            Map<String, Object> map = setUpConnectionAndStatement();
            conn = (Connection) map.get(CONNECTION);
            stmt = (Statement) map.get(STATEMENT);
            ResultSet rs = stmt.executeQuery(String.format(constants.GET_TRANSACTION, id));
            Optional<Transaction> result = Optional.empty();
            if (!rs.next()) {
                return result;
            } else {
                result = Optional.of(toTransactionObject(rs));
            }

            return result;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot setup JDBC connection:", e);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot get transaction entry:", e);
        } finally {
            closeConnectionAndStatement(conn, stmt);
        }
    }

    /**
     * Delete the selected transaction entry from database
     * @param id the transaction id to delete
     */
    public void deleteTransaction(String id) {
        Connection conn = null;
        Statement stmt = null;
        try {
            Map<String, Object> map = setUpConnectionAndStatement();
            conn = (Connection) map.get(CONNECTION);
            stmt = (Statement) map.get(STATEMENT);
            stmt.execute(String.format(constants.DELETE_TRANSACTION, id));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot setup JDBC connection:", e);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete transaction:", e);
        } finally {
            closeConnectionAndStatement(conn, stmt);
        }
    }

    private Transaction toTransactionObject(ResultSet rs) throws SQLException {
        Transaction res = new Transaction();
        res.setId(String.valueOf(rs.getInt(constants.ID_COLUMN_NAME)));
        res.setInventoryId(rs.getString(constants.INVENTORY_ID_COLUMN_NAME));
        res.setCreateDate(new Date(rs.getDate(constants.CREATE_DATE_COLUMN_NAME).getTime()));
        return res;
    }

    private void initializeTransactionTable() {
        Connection conn = null;
        Statement stmt = null;
        try {
            Map<String, Object> map = setUpConnectionAndStatement();
            conn = (Connection) map.get(CONNECTION);
            stmt = (Statement) map.get(STATEMENT);
            stmt.execute(constants.INITIALIZE_TRANSACTION_TABLE);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot setup JDBC connection:", e);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot initialize transaction table:", e);
        } finally {
            closeConnectionAndStatement(conn, stmt);
        }
    }

    private Map<String, Object> setUpConnectionAndStatement() throws ClassNotFoundException, SQLException {
        Class.forName(constants.JDBC_DRIVER_CLASS);
        Connection conn = DriverManager.getConnection(constants.DATABASE_URL, constants.USERNAME, constants.PASSWORD);
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
