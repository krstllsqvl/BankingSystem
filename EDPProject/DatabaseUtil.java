package EDP;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class DatabaseUtil  {
    private static final String URL = "jdbc:mysql://localhost:3306/bank_system?zeroDateTimeBehavior=CONVERT_TO_NULL";
    private static final String USER = "user";
    private static final String PASSWORD = "password";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Load BankUsers from database
 public static List<BankUser> loadBankUsers(String inputUsername, String inputPassword) {
    List<BankUser> users = new ArrayList<>();
    String sql = "SELECT * FROM bank_users WHERE BINARY username = ? AND BINARY password = ?"; 
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        // Set parameters for username and password
        pstmt.setString(1, inputUsername);
        pstmt.setString(2, inputPassword);

        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                BankUser user = new BankUser(
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("role"),
                    rs.getString("name"),
                    rs.getString("EmpID"),
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


    

 

    // Save CustomerAccount to database
 public static void saveCustomerAccount(CustomerAccount account) {
    String tableName = account.accountType.equalsIgnoreCase("Savings Account") 
                        ? "savings_account" 
                        : "checkings_account"; // Fixed typo

    String sql = "INSERT INTO " + tableName + " (customerID, accountID, firstName, middleName, lastName, " +
            "birthDate, age, street, barangay, municipality, provinceCity, zip, phone, email, gender, balance, is_active) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"; // Removed accountType from INSERT

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
  
 public static CustomerAccount getCustomerAccount(String accountId) {
    String sql = "(SELECT *, 'Savings Account' AS accountType FROM savings_account WHERE BINARY accountID = ?) " +
                 "UNION ALL " +
                 "(SELECT *, 'Checking Account' AS accountType FROM checkings_account WHERE BINARY accountID = ?) " +
                 "UNION ALL " +
                 "(SELECT *, 'Savings Account' AS accountType FROM savings_account WHERE BINARY customerID = ?) " +
                 "UNION ALL " +
                 "(SELECT *, 'Checking Account' AS accountType FROM checkings_account WHERE BINARY customerID = ?) " +
                 "LIMIT 1";
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        
        pstmt.setString(1, accountId);
        pstmt.setString(2, accountId);
        pstmt.setString(3, accountId);
         pstmt.setString(4, accountId);

        try (ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return new CustomerAccount(
                    rs.getString("customerID"),
                    rs.getString("accountID"),
                    rs.getString("firstName"),
                    rs.getString("middleName"),
                    rs.getString("lastName"),
                    rs.getDate("birthDate"),
                    rs.getString("street"),
                    rs.getString("barangay"),
                    rs.getString("municipality"),
                    rs.getString("provinceCity"),
                    rs.getString("zip"),
                    rs.getString("phone"),
                    rs.getString("email"),
                    rs.getString("gender"),
                    rs.getString("accountType"),
                    rs.getDouble("balance"),
                    rs.getBoolean("is_active")
                );
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
  
 static boolean disableAccount(String accountID) {
        String sqlSavings = "UPDATE savings_account SET is_active = false WHERE accountID = ?";
        String sqlCheckings = "UPDATE checkings_account SET is_active = false WHERE accountID = ?";
        int affectedRows = 0;
        try (Connection conn = getConnection();
             PreparedStatement pstmt1 = conn.prepareStatement(sqlSavings);
             PreparedStatement pstmt2 = conn.prepareStatement(sqlCheckings)) {
            pstmt1.setString(1, accountID);
            affectedRows += pstmt1.executeUpdate();
            pstmt2.setString(1, accountID);
            affectedRows += pstmt2.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Database Error: " + e.getMessage(), 
                "Disable Account Failed", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
  
  
  public static List<CustomerAccount> loadCustomerAccounts() {
    List<CustomerAccount> accounts = new ArrayList<>();
    String sql = "SELECT s.*, 'Savings Account' AS accountType FROM savings_account s " +
                 "UNION ALL " +
                 "SELECT c.*, 'Checking Account' AS accountType FROM checkings_account c";
    try (Connection conn = getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        while (rs.next()) {
            CustomerAccount acc = new CustomerAccount(
                rs.getString("customerID"),
                rs.getString("accountID"),
                rs.getString("firstName"),
                rs.getString("middleName"),
                rs.getString("lastName"),
                rs.getDate("birthDate"),
                rs.getString("street"),
                rs.getString("barangay"),
                rs.getString("municipality"),
                rs.getString("provinceCity"),
                rs.getString("zip"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("gender"),
                rs.getString("accountType"),
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
  
  
  
public static List<CustomerAccount> searchCustomerAccounts(String query) {
    List<CustomerAccount> results = new ArrayList<>();
    String sql = "SELECT * FROM (" +
            "SELECT s.*, 'Savings Account' AS accountType FROM savings_account s " +
            "WHERE (LOWER(s.customerID) LIKE LOWER(?) OR LOWER(s.accountID) LIKE LOWER(?) OR " +
            "LOWER(CONCAT(s.firstName, ' ', s.lastName)) LIKE LOWER(?) OR LOWER(s.email) LIKE LOWER(?)) " +
            "UNION ALL " +
            "SELECT c.*, 'Checking Account' AS accountType FROM checkings_account c " +
            "WHERE (LOWER(c.customerID) LIKE LOWER(?) OR LOWER(c.accountID) LIKE LOWER(?) OR " +
            "LOWER(CONCAT(c.firstName, ' ', c.lastName)) LIKE LOWER(?) OR LOWER(c.email) LIKE LOWER(?))" +
            ") AS combined";

    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        String likeQuery = "%" + query.toLowerCase() + "%";
        for (int i = 1; i <= 8; i++) {
            pstmt.setString(i, likeQuery);
        }

        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                CustomerAccount acc = new CustomerAccount(
                        rs.getString("customerID"),
                        rs.getString("accountID"),
                        rs.getString("firstName"),
                        rs.getString("middleName"),
                        rs.getString("lastName"),
                        rs.getDate("birthDate"),
                        rs.getString("street"),
                        rs.getString("barangay"),
                        rs.getString("municipality"),
                        rs.getString("provinceCity"),
                        rs.getString("zip"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("gender"),
                        rs.getString("accountType"),
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



public static List<Transaction> getTransactions(String accountId) {
    List<Transaction> transactions = new ArrayList<>();
    String sql = "SELECT * FROM transactions WHERE accountID = ? ORDER BY date DESC";
    
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        
        pstmt.setString(1, accountId);
        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Transaction t = new Transaction(
                    rs.getString("type"),
                    rs.getDouble("amount"),
                    rs.getDouble("balanceAfter"),
                    rs.getTimestamp("date")
                );
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
  
    
public static boolean deleteCustomerAccount(String accountId) {
    // 1. Reuse the search logic to validate the account exists
    CustomerAccount account = getCustomerAccount(accountId);
    if (account == null) {
        JOptionPane.showMessageDialog(null, "Account not found.", "Error", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    // 2. Check for transactions (if needed)
    if (hasTransactions(accountId)) {
        JOptionPane.showMessageDialog(null, 
            "Account has transactions. Cannot delete!", 
            "Error", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    // 3. Delete from the correct table
    String tableName = account.accountType.equalsIgnoreCase("Savings Account") 
                       ? "savings_account" 
                       : "checkings_account";

    String sql = "DELETE FROM " + tableName + " WHERE accountID = ?";
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, accountId);
        int affectedRows = pstmt.executeUpdate();
        return affectedRows > 0;
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, 
            "Database Error: " + e.getMessage(), 
            "Error", JOptionPane.ERROR_MESSAGE);
        return false;
    }
}

public static int getTotalCustomers() {
      
    String sql = "SELECT (SELECT COUNT(*) FROM savings_account) + " +
                "(SELECT COUNT(*) FROM checkings_account) AS total";
  
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
public static double getTotalDeposits() {
    String sql = 
        "SELECT SUM(t.amount) FROM transactions t " +
        "JOIN (" +
        "  SELECT accountID FROM savings_account WHERE is_active = 1" +
        "  UNION " +
        "  SELECT accountID FROM checkings_account WHERE is_active = 1" +
        ") active_accounts ON t.accountID = active_accounts.accountID " +
        "WHERE t.type = 'Deposit'";
    
    try (Connection conn = getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        return rs.next() ? rs.getDouble(1) : 0.0;
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, 
            "Database Error: " + e.getMessage(), 
            "Error", 
            JOptionPane.ERROR_MESSAGE);
        return 0.0;
    }
}
public static double getTotalWithdrawals() {
    String sql = 
        "SELECT SUM(t.amount) FROM transactions t " +
        "JOIN (" +
        "  SELECT accountID FROM savings_account WHERE is_active = true " +
        "  UNION " +
        "  SELECT accountID FROM checkings_account WHERE is_active = true" +
        ") active_accounts ON t.accountID = active_accounts.accountID " +
        "WHERE t.type = 'Withdrawal'";

    try (Connection conn = getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        return rs.next() ? rs.getDouble(1) : 0.0;
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, 
            "Database Error: " + e.getMessage(), 
            "Error", 
            JOptionPane.ERROR_MESSAGE);
        return 0.0;
    }
}


public static int getTotalTransactions() {
    String sql = "SELECT COUNT(*) FROM transactions";
    try (Connection conn = getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        return rs.next() ? rs.getInt(1) : 0;
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, 
            "Database Error: " + e.getMessage(), 
            "Error", 
            JOptionPane.ERROR_MESSAGE);
        return 0;
    }
}


    // Update BankUser status
  public static boolean updateCustomerAccount(CustomerAccount account) {
    String tableName = account.accountType.equalsIgnoreCase("Savings Account") 
                        ? "savings_account" 
                        : "checkings_account";

    String sql = "UPDATE " + tableName + " SET firstName=?, middleName=?, lastName=?, street=?, barangay=?, " +
                 "municipality=?, provinceCity=?, zip=?, phone=?, email=?, gender=?, balance=? WHERE accountID=?";

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
        pstmt.setString(11, account.gender);
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
   
    public static void saveTransaction(Transaction transaction, String accountID) {
    String sql = "INSERT INTO transactions (transactionID, accountID, type, amount, balanceAfter, date) VALUES (?, ?, ?, ?, ?, ?)";
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

    static boolean hasTransactions(String accountID) {
    String sql = "SELECT COUNT(*) FROM transactions WHERE accountID = ?";
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, accountID);
        ResultSet rs = pstmt.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, 
            "Database Error: " + e.getMessage(), 
            "Error", 
            JOptionPane.ERROR_MESSAGE);
        return false;
    }
}
}

