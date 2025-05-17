import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class DatabaseUtil  {

    // Load MySQL JDBC Driver (important for some Java versions/environments)
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null, 
                "MySQL JDBC Driver not found. Please add the connector JAR to your classpath.", 
                "Driver Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private static final String URL = "jdbc:mysql://localhost:3306/bank_system?zeroDateTimeBehavior=CONVERT_TO_NULL";
    private static final String USER = "user"; // CHANGE to your DB username
    private static final String PASSWORD = "password"; // CHANGE to your DB password

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Load BankUsers from database
    public static List<BankUser> loadBankUsers(String inputUsername, String inputPassword) {
        List<BankUser> users = new ArrayList<>();
        String sql = "SELECT * FROM bank_users WHERE BINARY username = ? AND BINARY password = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, inputUsername);
            pstmt.setString(2, inputPassword);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BankUser user = new BankUser(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getString("name"),
                        rs.getString("id"), // Make sure this matches your table column name
                        rs.getBoolean("is_active")
                    );
                    users.add(user);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                "Database Error: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
        return users;
    }

    /**
     * Check if a customer ID exists in the database
     */
    private static boolean isCustomerIdExists(String customerId) {
        String sql = "SELECT COUNT(*) FROM ("
                   + "SELECT customerID FROM savings_account "
                   + "UNION ALL "
                   + "SELECT customerID FROM checkings_account"
                   + ") AS combined WHERE customerID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, customerId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                "Database Error: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }


    private static boolean isAccountIdExists(String accountId) {
        String sql = "SELECT COUNT(*) FROM ("
                   + "SELECT accountID FROM savings_account "
                   + "UNION ALL "
                   + "SELECT accountID FROM checkings_account"
                   + ") AS combined WHERE accountID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                "Database Error: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    /**
     * Save a customer account to the database
     */
      public static void saveCustomerAccount(CustomerAccount account) {
        String tableName = account.accountType.equalsIgnoreCase("Savings Account")
                            ? "savings_account"
                            : "checkings_account";
        String sql = "INSERT INTO " + tableName + " (customerID, accountID, firstName, middleName, lastName, "
                   + "birthDate, age, street, barangay, municipality, provinceCity, zip, phone, email, gender, balance, is_active) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, account.customerID);
            pstmt.setString(2, account.accountID);
            pstmt.setString(3, account.firstName);
            pstmt.setString(4, account.middleName);
            pstmt.setString(5, account.lastName);
            pstmt.setDate(6, new java.sql.Date(account.birthDate.getTime()));
            pstmt.setInt(7, account.age);
            pstmt.setString(8, account.street);
            pstmt.setString(9, account.barangay);
            pstmt.setString(10, account.municipality);
            pstmt.setString(11, account.provinceCity);
            pstmt.setString(12, account.zip);
            pstmt.setString(13, account.phone);
            pstmt.setString(14, account.email);
            pstmt.setString(15, account.gender);
            pstmt.setDouble(16, account.balance);
            pstmt.setBoolean(17, account.is_active);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Database Error: " + e.getMessage(),
                    "Save Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }


    /**
     * Get a customer account from the database by ID
     */
    public static CustomerAccount getCustomerAccount(String identifier) {
        // Updated to use ITRUSTCustomerAcc table
        String sql = "SELECT * FROM ITRUSTCustomerAcc WHERE customer_id = ? OR account_id = ? OR email = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, identifier);
            pstmt.setString(2, identifier);
            pstmt.setString(3, identifier);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    CustomerAccount account = new CustomerAccount(
                        rs.getString("customer_id"),
                        rs.getString("account_id"),
                        rs.getString("first_name"),
                        rs.getString("middle_name"),
                        rs.getString("last_name"),
                        rs.getDate("birth_date"),
                        rs.getString("street"),
                        rs.getString("barangay"),
                        rs.getString("municipality"),
                        rs.getString("province_city"),
                        rs.getString("zip"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("sex"),  // Note: This is 'sex' in DB, 'gender' in Java
                        rs.getString("account_type"),
                        rs.getDouble("balance"),
                        rs.getBoolean("is_active")
                    );
                    
                    // Load transactions for this account
                    account.transactionHistory = getTransactions(account.accountID);
                    return account;
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Database Error: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    /**
     * Disable a customer account
     */
    public static boolean disableAccount(String accountID) {
        // Updated to use ITRUSTCustomerAcc table
        String sql = "UPDATE ITRUSTCustomerAcc SET is_active = false WHERE account_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, accountID);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Database Error: " + e.getMessage(), 
                "Disable Account Failed", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Load all customer accounts from the database
     */
    public static List<CustomerAccount> loadCustomerAccounts() {
        List<CustomerAccount> accounts = new ArrayList<>();
        // Updated to use ITRUSTCustomerAcc table
        String sql = "SELECT * FROM ITRUSTCustomerAcc";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                CustomerAccount acc = new CustomerAccount(
                    rs.getString("customer_id"),
                    rs.getString("account_id"),
                    rs.getString("first_name"),
                    rs.getString("middle_name"),
                    rs.getString("last_name"),
                    rs.getDate("birth_date"),
                    rs.getString("street"),
                    rs.getString("barangay"),
                    rs.getString("municipality"),
                    rs.getString("province_city"),
                    rs.getString("zip"),
                    rs.getString("phone"),
                    rs.getString("email"),
                    rs.getString("sex"),  // Note: This is 'sex' in DB, 'gender' in Java
                    rs.getString("account_type"),
                    rs.getDouble("balance"),
                    rs.getBoolean("is_active")
                );
                accounts.add(acc);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Database Error: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
        return accounts;
    }

    /**
     * Search for customer accounts
     */
    public static List<CustomerAccount> searchCustomerAccounts(String query) {
        List<CustomerAccount> results = new ArrayList<>();
        // Updated to use ITRUSTCustomerAcc table
        String sql = "SELECT * FROM ITRUSTCustomerAcc WHERE " +
                     "LOWER(customer_id) LIKE LOWER(?) OR " +
                     "LOWER(account_id) LIKE LOWER(?) OR " +
                     "LOWER(CONCAT(first_name, ' ', last_name)) LIKE LOWER(?) OR " +
                     "LOWER(email) LIKE LOWER(?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String likeQuery = "%" + query.toLowerCase() + "%";
            pstmt.setString(1, likeQuery);
            pstmt.setString(2, likeQuery);
            pstmt.setString(3, likeQuery);
            pstmt.setString(4, likeQuery);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    CustomerAccount acc = new CustomerAccount(
                        rs.getString("customer_id"),
                        rs.getString("account_id"),
                        rs.getString("first_name"),
                        rs.getString("middle_name"),
                        rs.getString("last_name"),
                        rs.getDate("birth_date"),
                        rs.getString("street"),
                        rs.getString("barangay"),
                        rs.getString("municipality"),
                        rs.getString("province_city"),
                        rs.getString("zip"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("sex"),  // Note: This is 'sex' in DB, 'gender' in Java
                        rs.getString("account_type"),
                        rs.getDouble("balance"),
                        rs.getBoolean("is_active")
                    );
                    results.add(acc);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Database Error: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
        return results;
    }

    /**
     * Get transactions for an account
     */
    public static List<Transaction> getTransactions(String accountId) {
        List<Transaction> transactions = new ArrayList<>();
        // Updated to use 'transaction' table
        String sql = "SELECT * FROM transaction WHERE account_id = ? ORDER BY transaction_date DESC";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, accountId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Transaction t = new Transaction(
                        rs.getString("type"),
                        rs.getDouble("amount"),
                        rs.getDouble("balance_after"),
                        rs.getTimestamp("transaction_date")
                    );
                    // Set the transaction ID from the database
                    t.transactionID = rs.getString("transaction_id");
                    transactions.add(t);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Database Error: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
        return transactions;
    }

    /**
     * Delete a customer account
     */
    public static boolean deleteCustomerAccount(String accountId) {
        // Updated to use ITRUSTCustomerAcc table
        CustomerAccount account = getCustomerAccount(accountId);
        if (account == null) {
            JOptionPane.showMessageDialog(null, "Account not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Check for transactions
        if (hasTransactions(accountId)) {
            // Instead of preventing deletion, we'll mark as inactive
            return disableAccount(accountId);
        }

        // Delete the account
        String sql = "DELETE FROM ITRUSTCustomerAcc WHERE account_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, accountId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Database Error: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Get total number of customers
     */
    public static int getTotalCustomers() {
        // Updated to use ITRUSTCustomerAcc table
        String sql = "SELECT COUNT(*) AS total FROM ITRUSTCustomerAcc";
      
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            return rs.next() ? rs.getInt("total") : 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Database Error: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return 0;
        }
    }

    /**
     * Get total deposits
     */
    public static double getTotalDeposits() {
        // Updated to use 'transaction' table
        String sql = "SELECT SUM(amount) AS total FROM transaction " +
                     "WHERE type = 'Deposit'";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            return rs.next() ? rs.getDouble("total") : 0.0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Database Error: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return 0.0;
        }
    }

    /**
     * Get total withdrawals
     */
    public static double getTotalWithdrawals() {
        // Updated to use 'transaction' table
        String sql = "SELECT SUM(amount) AS total FROM transaction " +
                     "WHERE type = 'Withdrawal'";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            return rs.next() ? rs.getDouble("total") : 0.0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Database Error: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return 0.0;
        }
    }

    /**
     * Get total transactions
     */
    public static int getTotalTransactions() {
        // Updated to use 'transaction' table
        String sql = "SELECT COUNT(*) AS total FROM transaction";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            return rs.next() ? rs.getInt("total") : 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Database Error: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return 0;
        }
    }

    /**
     * Update a customer account
     */
    public static boolean updateCustomerAccount(CustomerAccount account) {
        // Updated to use ITRUSTCustomerAcc table
        String sql = "UPDATE ITRUSTCustomerAcc SET " +
                     "first_name=?, middle_name=?, last_name=?, street=?, barangay=?, " +
                     "municipality=?, province_city=?, zip=?, phone=?, email=?, sex=?, balance=? " +
                     "WHERE account_id=?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, account.firstName);
            pstmt.setString(2, account.middleName);
            pstmt.setString(3, account.lastName);
            pstmt.setString(4, account.street);
            pstmt.setString(5, account.barangay);
            pstmt.setString(6, account.municipality);
            pstmt.setString(7, account.provinceCity);
            pstmt.setString(8, account.zip);
            pstmt.setString(9, account.phone);
            pstmt.setString(10, account.email);
            pstmt.setString(11, account.gender);  // Maps to 'sex' in database
            pstmt.setDouble(12, account.balance);
            pstmt.setString(13, account.accountID);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Database Error: " + e.getMessage(), 
                "Update Failed", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
   
    /**
     * Save a transaction to the database
     */
    public static void saveTransaction(Transaction transaction, String accountID) {
        // Updated to use 'transaction' table
        String sql = "INSERT INTO transaction (transaction_id, account_id, type, amount, " +
                     "balance_after, transaction_date) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, transaction.transactionID);
            pstmt.setString(2, accountID);
            pstmt.setString(3, transaction.type);
            pstmt.setDouble(4, transaction.amount);
            pstmt.setDouble(5, transaction.balanceAfter);
            pstmt.setTimestamp(6, new java.sql.Timestamp(transaction.date.getTime()));

            pstmt.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Database Error: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Check if an account has transactions
     */
    public static boolean hasTransactions(String accountID) {
        // Updated to use 'transaction' table
        String sql = "SELECT COUNT(*) AS count FROM transaction WHERE account_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, accountID);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt("count") > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Database Error: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    /**
     * Test database connection
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            JOptionPane.showMessageDialog(null, 
                "Successfully connected to the database!", 
                "Connection Test", 
                JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Failed to connect to the database: " + e.getMessage(), 
                "Connection Test Failed", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}