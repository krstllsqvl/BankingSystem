package EDP;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Calendar;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

/**
 * CustomerAccount class representing a bank customer's account
 * Compatible with the ITRUSTBankingSystem database schema and MySQL 8.0+
 */
public class CustomerAccount {
    // Account status fields
    public boolean is_active;
    public int feeCycleCounter;
    
    // Customer identification fields
    public String customerID;
    public String accountID;
    
    // Personal information fields
    public String firstName;
    public String middleName;
    public String lastName;
    public Date birthDate;
    public int age;
    
    // Address fields
    public String street;
    public String barangay;
    public String municipality;
    public String provinceCity;
    public String zip;
    
    // Contact information
    public String phone;
    public String email;
    public String sex; // Renamed from gender to sex to match database schema
    
    // Account details
    public String accountType;
    public double balance;
    public int transactionCount;
    public Date lastTransactionDate;
    public Date lastInterestFeeApplicationDate;
    
    // Transaction history
    public List<Transaction> transactionHistory;

    /**
     * Constructor for creating a new customer account
     */
    public CustomerAccount(String customerID, String accountID, String firstName,
                      String middleName, String lastName, Date birthDate,
                      String street, String barangay, String municipality,
                      String provinceCity, String zip, String phone,
                      String email, String gender, String accountType, double initialDeposit, boolean is_active) {
        this.feeCycleCounter = 0;
        this.customerID = customerID;
        this.accountID = accountID;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.age = calculateAge(birthDate);
        this.street = street;
        this.barangay = barangay;
        this.municipality = municipality;
        this.provinceCity = provinceCity;
        this.zip = zip;
        this.phone = phone;
        this.email = email;
        this.sex = gender; // Note: We store gender in the sex field to match DB schema
        this.accountType = accountType;
        this.balance = initialDeposit;
        this.transactionCount = 0;
        this.lastTransactionDate = new Date(); // Initialize with current date
        this.lastInterestFeeApplicationDate = new Date();
        this.transactionHistory = new ArrayList<>();
        this.is_active = is_active;
        
        // Create initial deposit transaction
        Transaction initialDeposit = new Transaction("Deposit", initialDeposit, balance);
        transactionHistory.add(initialDeposit);
    }
    
    /**
     * Calculate age based on birth date using Java Time API (more accurate than Calendar)
     */
    private int calculateAge(Date birthDate) {
        if (birthDate == null) {
            return 0;
        }
        
        // Convert java.util.Date to LocalDate
        LocalDate birthLocalDate = birthDate.toInstant()
                                           .atZone(ZoneId.systemDefault())
                                           .toLocalDate();
        LocalDate currentDate = LocalDate.now();
        
        // Calculate the period between the two dates
        Period period = Period.between(birthLocalDate, currentDate);
        
        return period.getYears();
    }
    
    /**
     * Check if it's time to apply monthly interest or fees
     */
    public void checkAndApplyInterestAndFees() {
        Date now = new Date();
        long diffInMillies = now.getTime() - lastInterestFeeApplicationDate.getTime();
        long daysPassed = diffInMillies / (1000 * 60 * 60 * 24);
        
        if (daysPassed >= 7) { // 1 week
            feeCycleCounter++;
            if (feeCycleCounter >= 4) { // 4 weeks = 1 month
                applyMonthlyConditions(); // Apply interest/fees
                lastInterestFeeApplicationDate = now;
                feeCycleCounter = 0;
                // Update the database
                updateInDatabase();
            }
        }
    }
    
    /**
     * Apply monthly conditions (interest for savings, fees for checking)
     */
    public void applyMonthlyConditions() {
        if (accountType.equals("Savings Account")) {
            // Get interest rate from configuration or database
            double interestRate = 0.002916; // Monthly interest rate (3.5% annual)
            double interest = balance * interestRate;
            balance += interest;
            
            // Create transaction record
            Transaction interestTransaction = new Transaction("Interest", interest, balance);
            transactionHistory.add(interestTransaction);
            
            // Save to database
            saveTransactionToDatabase(interestTransaction);
            
        } else if (accountType.equals("Checking Account")) {
            // Get monthly fee from configuration or database
            double monthlyFee = 10.0;
            balance -= monthlyFee; // Subtract fee
            
            // Create transaction record
            Transaction feeTransaction = new Transaction("Monthly Fee", monthlyFee, balance);
            transactionHistory.add(feeTransaction);
            
            // Save to database
            saveTransactionToDatabase(feeTransaction);
        }
    }
    
    /**
     * Deposit money into the account
     */
    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        
        balance += amount;
        lastTransactionDate = new Date();
        transactionCount++;
        
        // Create transaction record
        Transaction transaction = new Transaction("Deposit", amount, balance);
        transactionHistory.add(transaction);
        
        // Save to database
        saveTransactionToDatabase(transaction);
        updateInDatabase();
    }
    
    /**
     * Withdraw money from the account
     */
    public boolean withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        
        if (amount > balance) {
            return false; // Insufficient funds
        }
        
        balance -= amount;
        lastTransactionDate = new Date();
        transactionCount++;
        
        // Create transaction record
        Transaction transaction = new Transaction("Withdrawal", amount, balance);
        transactionHistory.add(transaction);
        
        // Save to database
        saveTransactionToDatabase(transaction);
        updateInDatabase();
        
        return true;
    }
    
    /**
     * Get the current balance
     */
    public double getBalance() {
        return balance;
    }
    
    /**
     * Get the transaction history
     */
    public List<Transaction> getTransactionHistory() {
        if (transactionHistory == null || transactionHistory.isEmpty()) {
            // Load from database if empty
            loadTransactionsFromDatabase();
        }
        return transactionHistory;
    }
    
    /**
     * Get the full name of the customer
     */
    public String getFullName() {
        if (middleName != null && !middleName.isEmpty()) {
            return firstName + " " + middleName + " " + lastName;
        } else {
            return firstName + " " + lastName;
        }
    }
    
    /**
     * Get the full address of the customer
     */
    public String getFullAddress() {
        StringBuilder address = new StringBuilder();
        
        if (street != null && !street.isEmpty()) {
            address.append(street);
        }
        
        if (barangay != null && !barangay.isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(barangay);
        }
        
        if (municipality != null && !municipality.isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(municipality);
        }
        
        if (provinceCity != null && !provinceCity.isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(provinceCity);
        }
        
        if (zip != null && !zip.isEmpty()) {
            if (address.length() > 0) address.append(" ");
            address.append(zip);
        }
        
        return address.toString();
    }
    
    /**
     * Convert to SQL date for database operations
     */
    public java.sql.Date getSqlBirthDate() {
        if (birthDate == null) return null;
        return new java.sql.Date(birthDate.getTime());
    }
    
    /**
     * Save the account to the database
     */
    public boolean saveToDatabase() {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO ITRUSTCustomerAcc (customer_id, account_id, first_name, middle_name, " +
                 "last_name, birth_date, age, street, barangay, municipality, province_city, " +
                 "zip, phone, email, sex, account_type, balance, is_active, " +
                 "transaction_count, last_transaction_date, last_interest_fee_application_date, " +
                 "fee_cycle_counter) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                 "ON DUPLICATE KEY UPDATE " +
                 "first_name = VALUES(first_name), middle_name = VALUES(middle_name), " +
                 "last_name = VALUES(last_name), birth_date = VALUES(birth_date), " +
                 "age = VALUES(age), street = VALUES(street), barangay = VALUES(barangay), " +
                 "municipality = VALUES(municipality), province_city = VALUES(province_city), " +
                 "zip = VALUES(zip), phone = VALUES(phone), email = VALUES(email), " +
                 "sex = VALUES(sex), account_type = VALUES(account_type), balance = VALUES(balance), " +
                 "is_active = VALUES(is_active), transaction_count = VALUES(transaction_count), " +
                 "last_transaction_date = VALUES(last_transaction_date), " +
                 "last_interest_fee_application_date = VALUES(last_interest_fee_application_date), " +
                 "fee_cycle_counter = VALUES(fee_cycle_counter)")) {
            
            // Set all parameters
            stmt.setString(1, customerID);
            stmt.setString(2, accountID);
            stmt.setString(3, firstName);
            stmt.setString(4, middleName);
            stmt.setString(5, lastName);
            stmt.setDate(6, getSqlBirthDate());
            stmt.setInt(7, age);
            stmt.setString(8, street);
            stmt.setString(9, barangay);
            stmt.setString(10, municipality);
            stmt.setString(11, provinceCity);
            stmt.setString(12, zip);
            stmt.setString(13, phone);
            stmt.setString(14, email);
            stmt.setString(15, sex);
            stmt.setString(16, accountType);
            stmt.setDouble(17, balance);
            stmt.setBoolean(18, is_active);
            stmt.setInt(19, transactionCount);
            
            if (lastTransactionDate != null) {
                stmt.setTimestamp(20, new Timestamp(lastTransactionDate.getTime()));
            } else {
                stmt.setNull(20, Types.TIMESTAMP);
            }
            
            if (lastInterestFeeApplicationDate != null) {
                stmt.setTimestamp(21, new Timestamp(lastInterestFeeApplicationDate.getTime()));
            } else {
                stmt.setTimestamp(21, new Timestamp(new Date().getTime()));
            }
            
            stmt.setInt(22, feeCycleCounter);
            
            int result = stmt.executeUpdate();
            
            // Save initial transaction if it exists and this is a new account
            if (result > 0 && !transactionHistory.isEmpty()) {
                Transaction initialTransaction = transactionHistory.get(0);
                saveTransactionToDatabase(initialTransaction);
            }
            
            return result > 0;
        } catch (SQLException e) {
            System.err.println("Error saving customer account: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Update account details in the database
     */
    public boolean updateInDatabase() {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE ITRUSTCustomerAcc SET first_name = ?, middle_name = ?, last_name = ?, " +
                 "street = ?, barangay = ?, municipality = ?, province_city = ?, zip = ?, " +
                 "phone = ?, email = ?, sex = ?, balance = ?, is_active = ?, " +
                 "fee_cycle_counter = ?, transaction_count = ?, last_transaction_date = ?, " +
                 "last_interest_fee_application_date = ? " +
                 "WHERE account_id = ?")) {
            
            // Set all parameters
            stmt.setString(1, firstName);
            stmt.setString(2, middleName);
            stmt.setString(3, lastName);
            stmt.setString(4, street);
            stmt.setString(5, barangay);
            stmt.setString(6, municipality);
            stmt.setString(7, provinceCity);
            stmt.setString(8, zip);
            stmt.setString(9, phone);
            stmt.setString(10, email);
            stmt.setString(11, sex);
            stmt.setDouble(12, balance);
            stmt.setBoolean(13, is_active);
            stmt.setInt(14, feeCycleCounter);
            stmt.setInt(15, transactionCount);
            
            if (lastTransactionDate != null) {
                stmt.setTimestamp(16, new Timestamp(lastTransactionDate.getTime()));
            } else {
                stmt.setNull(16, Types.TIMESTAMP);
            }
            
            if (lastInterestFeeApplicationDate != null) {
                stmt.setTimestamp(17, new Timestamp(lastInterestFeeApplicationDate.getTime()));
            } else {
                stmt.setTimestamp(17, new Timestamp(new Date().getTime()));
            }
            
            stmt.setString(18, accountID);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating customer account: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Save a transaction to the database
     */
    private boolean saveTransactionToDatabase(Transaction transaction) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO transaction (transaction_id, account_id, type, amount, " +
                 "balance_after, transaction_date) VALUES (?, ?, ?, ?, ?, ?)")) {
            
            stmt.setString(1, transaction.transactionID);
            stmt.setString(2, accountID);
            stmt.setString(3, transaction.type);
            stmt.setDouble(4, transaction.amount);
            stmt.setDouble(5, transaction.balanceAfter);
            stmt.setTimestamp(6, new Timestamp(transaction.date.getTime()));
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error saving transaction: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Load transactions from the database
     */
    private void loadTransactionsFromDatabase() {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM transaction WHERE account_id = ? ORDER BY transaction_date DESC")) {
            
            stmt.setString(1, accountID);
            
            try (ResultSet rs = stmt.executeQuery()) {
                transactionHistory = new ArrayList<>();
                
                while (rs.next()) {
                    Transaction transaction = new Transaction(
                        rs.getString("transaction_id"),
                        rs.getString("type"),
                        rs.getDouble("amount"),
                        rs.getDouble("balance_after"),
                        new Date(rs.getTimestamp("transaction_date").getTime())
                    );
                    transactionHistory.add(transaction);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading transactions: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Load an account from the database by ID
     */
    public static CustomerAccount loadFromDatabase(String identifier) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM ITRUSTCustomerAcc WHERE customer_id = ? OR account_id = ? OR email = ?")) {
            
            stmt.setString(1, identifier);
            stmt.setString(2, identifier);
            stmt.setString(3, identifier);
            
            try (ResultSet rs = stmt.executeQuery()) {
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
                        rs.getString("sex"),
                        rs.getString("account_type"),
                        rs.getDouble("balance"),
                        rs.getBoolean("is_active")
                    );
                    
                    // Set additional fields
                    account.feeCycleCounter = rs.getInt("fee_cycle_counter");
                    account.transactionCount = rs.getInt("transaction_count");
                    
                    Timestamp lastTrans = rs.getTimestamp("last_transaction_date");
                    if (lastTrans != null) {
                        account.lastTransactionDate = new Date(lastTrans.getTime());
                    }
                    
                    Timestamp lastInterest = rs.getTimestamp("last_interest_fee_application_date");
                    if (lastInterest != null) {
                        account.lastInterestFeeApplicationDate = new Date(lastInterest.getTime());
                    }
                    
                    // Load transaction history
                    account.loadTransactionsFromDatabase();
                    
                    return account;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading customer account: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Load all accounts from the database
     */
    public static List<CustomerAccount> loadAllFromDatabase() {
        List<CustomerAccount> accounts = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM ITRUSTCustomerAcc")) {
            
            while (rs.next()) {
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
                    rs.getString("sex"),
                    rs.getString("account_type"),
                    rs.getDouble("balance"),
                    rs.getBoolean("is_active")
                );
                
                // Set additional fields
                account.feeCycleCounter = rs.getInt("fee_cycle_counter");
                account.transactionCount = rs.getInt("transaction_count");
                
                Timestamp lastTrans = rs.getTimestamp("last_transaction_date");
                if (lastTrans != null) {
                    account.lastTransactionDate = new Date(lastTrans.getTime());
                }
                
                Timestamp lastInterest = rs.getTimestamp("last_interest_fee_application_date");
                if (lastInterest != null) {
                    account.lastInterestFeeApplicationDate = new Date(lastInterest.getTime());
                }
                
                accounts.add(account);
            }
        } catch (SQLException e) {
            System.err.println("Error loading customer accounts: " + e.getMessage());
            e.printStackTrace();
        }
        
        return accounts;
    }
    
    /**
     * Generate a new customer ID
     */
    public static String generateCustomerID() {
        return "CUST" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }
    
    /**
     * Generate a new account ID
     */
    public static String generateAccountID() {
        return "ACC" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }
    
    /**
     * Update customer details and save to database
     */
    public boolean updateDetails(String firstName, String middleName, String lastName,
                               String street, String barangay, String municipality,
                               String provinceCity, String zip, String phone,
                               String email, String sex) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.street = street;
        this.barangay = barangay;
        this.municipality = municipality;
        this.provinceCity = provinceCity;
        this.zip = zip;
        this.phone = phone;
        this.email = email;
        this.sex = sex;
        
        return updateInDatabase();
    }
    
    /**
     * Deactivate the account
     */
    public boolean deactivate() {
        this.is_active = false;
        return updateInDatabase();
    }
    
    /**
     * Activate the account
     */
    public boolean activate() {
        this.is_active = true;
        return updateInDatabase();
    }
    
    @Override
    public String toString() {
        return accountID + ": " + getFullName() + " (" + accountType + ") - Balance: ₱" + String.format("%,.2f", balance);
    }
}