package config;

public class constants {
    // JDBC Driver constants
    // TODO: Hide username password
    public static final String JDBC_DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
    public static final String DATABASE_URL = "jdbc:mysql://localhost/seckill_instance";
    public static final String USERNAME = "root";
    public static final String PASSWORD = "Vanessa920930";

    // SQL statement templates for Inventory table
    // FIXME: handle when inserting duplicated records, ID+NAME should be unique
    public static final String INITIALIZE_INVENTORY_TABLE = "CREATE TABLE IF NOT EXISTS inventory(" +
            "\tid INT AUTO_INCREMENT PRIMARY KEY," +
            "\tname VARCHAR(255) NOT NULL," +
            "\tcount INT DEFAULT 0," +
            "\tsales INT DEFAULT 0," +
            "\tversion INT DEFAULT 1" +
            ") ENGINE=INNODB;";
    public static final String INSERT_INVENTORY = "INSERT INTO inventory (name, count) VALUES (\"%s\", %d);";
    public static final String DELETE_INVENTORY = "DELETE FROM inventory WHERE id=%s;";
    public static final String GET_INVENTORY = "SELECT * FROM inventory WHERE id=%s;";
    public static final String UPDATE_INVENTORY = "UPDATE inventory SET count = count - 1, sales = sales + 1," +
            "version = version + 1 WHERE id=%s AND version=%d;";
    public static final String GET_CREATED_INVENTORY = "SELECT * FROM inventory WHERE name=\"%s\" AND count=%d;";

    public static final String ID_COLUMN_NAME = "id";
    public static final String SALES_COLUMN_NAME = "sales";
    public static final String NAME_COLUMN_NAME = "name";
    public static final String COUNT_COLUMN_NAME = "count";
    public static final String VERSION_COLUMN_NAME = "version";

    // SQL statements for Transaction table
    public static final String INITIALIZE_TRANSACTION_TABLE = "CREATE TABLE IF NOT EXISTS transaction(" +
            "\tid INT AUTO_INCREMENT PRIMARY KEY," +
            "\tinventoryId INT NOT NULL," +
            "\tcreateDate DATETIME NOT NULL," +
            "\tFOREIGN KEY (inventoryId) REFERENCES inventory(id)" +
            ") ENGINE=INNODB;";
    public static final String INSERT_TRANSACTION = "INSERT INTO transaction (inventoryId, createDate) VALUES " +
            "(%s, FROM_UNIXTIME(%d));";
    public static final String GET_CREATED_TRANSACTION = "SELECT * FROM transaction " +
            "WHERE inventoryId=%s AND createDate=FROM_UNIXTIME(%d);";
    public static final String DELETE_TRANSACTION = "DELETE FROM transaction WHERE id=%s;";
    public static final String GET_TRANSACTION= "SELECT * FROM transaction WHERE id=%s;";

    public static final String INVENTORY_ID_COLUMN_NAME = "inventoryId";
    public static final String CREATE_DATE_COLUMN_NAME = "createDate";
}
