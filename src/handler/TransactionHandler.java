package handler;

import models.Transaction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Optional;

import static config.constants.CREATE_DATE_COLUMN_NAME;
import static config.constants.DATABASE_URL;
import static config.constants.DELETE_TRANSACTION;
import static config.constants.GET_CREATED_TRANSACTION;
import static config.constants.GET_TRANSACTION;
import static config.constants.ID_COLUMN_NAME;
import static config.constants.INITIALIZE_TRANSACTION_TABLE;
import static config.constants.INSERT_TRANSACTION;
import static config.constants.INVENTORY_ID_COLUMN_NAME;
import static config.constants.JDBC_DRIVER_CLASS;
import static config.constants.PASSWORD;
import static config.constants.USERNAME;

public class TransactionHandler {
    private Connection conn;
    private Statement stmt;
    private static TransactionHandler instance;

    // TODO: Add logger, refactor code
    // FIXME: handle when adding duplicated records

    private TransactionHandler() {
        conn = null;
        stmt = null;
        initializeTransactionTable();
    }

    public static TransactionHandler getInstance() {
        if (instance == null) {
            instance = new TransactionHandler();
        }
        return instance;
    }

    /**
     * Create a transaction record in the database
     * @param inventoryId the inventory Id to insert
     * @return the transaction object containing all information
     */
    public Transaction createTransaction(String inventoryId) {
        try {
            setUpConnectionAndStatement();
            long timestamp = new Date().getTime() / 1000;
            System.out.println("timestamp:" + timestamp);
            stmt.execute(String.format(INSERT_TRANSACTION, inventoryId, timestamp));

            ResultSet rs = stmt.executeQuery(String.format(GET_CREATED_TRANSACTION, inventoryId, timestamp));
            if (!rs.next()) {
                throw new RuntimeException("No matching transaction record found");
            }
            Transaction res = toTransactionObject(rs);

            rs.close();
            return res;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot setup JDBC connection:", e);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot create new transaction entry:", e);
        } finally {
            closeConnectionAndStatement();
        }
    }

    /**
     * Return the queried Inventory object or empty result if not applicable.
     * @param id the inventory id for quering
     * @return an optional object containing queried result
     */
    public Optional<Transaction> getTransaction(String id) {
        try {
            setUpConnectionAndStatement();
            ResultSet rs = stmt.executeQuery(String.format(GET_TRANSACTION, id));
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
            closeConnectionAndStatement();
        }
    }

    /**
     * Delete the selected transaction entry from database
     * @param id the transaction id to delete
     */
    public void deleteTransaction(String id) {
        try {
            setUpConnectionAndStatement();
            stmt.execute(String.format(DELETE_TRANSACTION, id));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot setup JDBC connection:", e);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete transaction:", e);
        } finally {
            closeConnectionAndStatement();
        }
    }

    private Transaction toTransactionObject(ResultSet rs) throws SQLException {
        Transaction res = new Transaction();
        res.setId(String.valueOf(rs.getInt(ID_COLUMN_NAME)));
        res.setInventoryId(rs.getString(INVENTORY_ID_COLUMN_NAME));
        res.setCreateDate(new Date(rs.getDate(CREATE_DATE_COLUMN_NAME).getTime()));
        return res;
    }

    private void initializeTransactionTable() {
        try {
            setUpConnectionAndStatement();
            stmt.execute(INITIALIZE_TRANSACTION_TABLE);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot setup JDBC connection:", e);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot initialize transaction table:", e);
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
