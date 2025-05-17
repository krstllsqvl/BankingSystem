package EDPProject;
import EDPProject.DatabaseUtil;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.swing.*;
import javax.swing.border.TitledBorder;

@SuppressWarnings("unused")
public class Main  {
    private static final List<BankUser> bankUsers = new ArrayList<>();
    private static final List<CustomerAccount> customerAccounts = new ArrayList<>();
    
    public static void main(String[] args) {
        // Test database connection first
        try {
            Connection conn = DatabaseUtil.getConnection();
            System.out.println("Connection to database successful!");
            conn.close();
            
            // Only load accounts if connection is successful
            List<CustomerAccount> loadedAccounts = DatabaseUtil.loadCustomerAccounts();
            customerAccounts.addAll(loadedAccounts);
            System.out.println("Loaded " + customerAccounts.size() + " customer accounts from database");
            
        } catch (SQLException e) {
            System.err.println("Connection error: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, 
                "Database connection failed: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
        }
        
        // Suppress warning by using the instance in a way that cannot be optimized away
        new loginPage(bankUsers, customerAccounts).setVisible(true);
    }

    // Utility method for age calculation (static, so it can be used anywhere)
    public static int calculateAge(Date birthDate) {
        Calendar birthCal = Calendar.getInstance();
        birthCal.setTime(birthDate);

        Calendar todayCal = Calendar.getInstance();

        int age = todayCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);

        if (todayCal.get(Calendar.MONTH) < birthCal.get(Calendar.MONTH) ||
           (todayCal.get(Calendar.MONTH) == birthCal.get(Calendar.MONTH) &&
            todayCal.get(Calendar.DAY_OF_MONTH) < birthCal.get(Calendar.DAY_OF_MONTH))) {
            age--;
        }
        return age;
    }
}
class Transaction {
    String transactionID;
    String type;
    double amount;
    double balanceAfter;
    Date date;

    // Constructor for new transactions with auto-generated ID
    public Transaction(String type, double amount, double balanceAfter) {
        this.transactionID = generateTransactionID();
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.date = new Date();
    }
    
    // Constructor with specified date
    public Transaction(String type, double amount, double balanceAfter, Date date) {
        this.transactionID = generateTransactionID();
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.date = date;
    }
    
    // Constructor for loading from database (with existing transaction ID)
    public Transaction(String transactionID, String type, double amount, double balanceAfter, Date date) {
        this.transactionID = transactionID;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.date = date;
    }
    
    private String generateTransactionID() {
        // Generate a UUID-based transaction ID
        return "TRN" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}
class CustomerAccount {
    boolean is_active;
    int feeCycleCounter;
    String customerID;
    String accountID;
    
    String firstName;
    String middleName;
    String lastName;
    Date birthDate;
    int age;
    String street;
    String barangay;
    String municipality;
    String provinceCity;
    String zip;
    String phone;
    String email;
    String gender;
    String accountType;
    double balance;
    int transactionCount;
    Date lastTransactionDate;
    Date lastInterestFeeApplicationDate;
    ArrayList<Transaction> transactionHistory;

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
        this.gender = gender;
        this.accountType = accountType;
        this.balance = initialDeposit;
        this.transactionCount = 0;
        this.lastTransactionDate = null;
        this.lastInterestFeeApplicationDate = new Date();
        this.transactionHistory = new ArrayList<>();
        this.is_active = is_active;
        transactionHistory.add(new Transaction("Deposit", initialDeposit, balance));
    }

     private int calculateAge(Date birthDate) {
        Calendar birthCal = Calendar.getInstance();
        birthCal.setTime(birthDate);

        Calendar todayCal = Calendar.getInstance();

        int calculatedAge = todayCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);

        if (todayCal.get(Calendar.MONTH) < birthCal.get(Calendar.MONTH) ||
           (todayCal.get(Calendar.MONTH) == birthCal.get(Calendar.MONTH) &&
            todayCal.get(Calendar.DAY_OF_MONTH) < birthCal.get(Calendar.DAY_OF_MONTH))) {
            calculatedAge--;
        }
        return calculatedAge;
    }
     
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
            }
        }
    }
   public void applyMonthlyConditions() {
    if (accountType.equals("Savings Account")) {
        double interestRate = 0.002916; // Monthly interest rate (example)
        double interest = balance * interestRate;
        balance += interest;
        Transaction interestTransaction = new Transaction("Interest", interest, balance); 
        DatabaseUtil.saveTransaction(interestTransaction, accountID); 
        // Add a transaction record for the interest
        transactionHistory.add(new Transaction("Interest", interest, balance));
        DatabaseUtil.updateCustomerAccount(this); // Update in DB
        DatabaseUtil.saveTransaction(new Transaction("Interest", interest, balance), this.accountID);

    } else if (accountType.equals("Checkings Account")) {
    double monthlyFee = -10.0;
    balance += monthlyFee; // Note: fee is negative
    // Create a Transaction object
    Transaction feeTransaction = new Transaction("Monthly Fee", monthlyFee, balance);
    DatabaseUtil.saveTransaction(feeTransaction, accountID);
    // ... update account balance in database ...
}
}
   
  // CustomerAccount.java - Corrected methods
public void deposit(double amount) {
    balance += amount;
    Transaction transaction = new Transaction("Deposit", amount, balance);
    transactionHistory.add(transaction);
    DatabaseUtil.saveTransaction(transaction, this.accountID);
    DatabaseUtil.updateCustomerAccount(this); // Updates balance in DB
}

public void withdraw(double amount) {
    if (amount > balance) {
        JOptionPane.showMessageDialog(null, "Insufficient funds!", "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }
    balance -= amount;
    Transaction transaction = new Transaction("Withdrawal", amount, balance);
    transactionHistory.add(transaction);
    DatabaseUtil.saveTransaction(transaction, this.accountID);
    DatabaseUtil.updateCustomerAccount(this); // Updates balance in DB
}

    public double getBalance() {
        return balance;
    }
    
       public List<Transaction> getTransactionHistory() {
        return transactionHistory;
    }
}



class BackgroundPanel extends JPanel {
    private Image backgroundImage;

    public BackgroundPanel(String imagePath) {
        try {
            backgroundImage = Toolkit.getDefaultToolkit().createImage(imagePath);
        } catch (Exception e) {
            backgroundImage = null;
        }
        setLayout(null); // Maintain null layout for absolute positioning
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}




//=========================START OF LOGIN ====================================================================================================START OF LOGIN ===================================================================

class loginPage extends JFrame {
    loginPage(List<BankUser> bankUsers, List<CustomerAccount> customerAccounts) {
        setSize(920, 640);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);
        
       
        
    

        BackgroundPanel bigPanel = new BackgroundPanel("assets\\2.png");
        bigPanel.setBounds(0, 0, 920, 640);
        bigPanel.setLayout(null);
         


        ImageIcon imageIcon = new ImageIcon("assets\\origlogo.png");
        Image image = imageIcon.getImage().getScaledInstance(320, 320, Image.SCALE_SMOOTH);
        ImageIcon resizedIcon = new ImageIcon(image);
        JLabel label = new JLabel(resizedIcon);
        label.setBounds(500, 150, 320, 320);

        

        JLabel labelLogin = new JLabel("LOGIN");
        labelLogin.setFont(new Font("SquanSerif", Font.PLAIN, 40));
        labelLogin.setBounds(170, 70, 200, 100);
        labelLogin.setForeground(new Color(0xffffff));

        JTextField usernameLogIn = new JTextField();
        usernameLogIn.setBounds(50, 200, 350, 50);
        usernameLogIn.setForeground(new Color(0xffffff)); // Text color
        usernameLogIn.setCaretColor(new Color(0xffffff)); // Cursor color
        usernameLogIn.setBackground(new Color(255, 255, 255, 0)); // Transparent
        usernameLogIn.setOpaque(false);
        usernameLogIn.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0xffffff)),
                "Username", TitledBorder.DEFAULT_POSITION, TitledBorder.DEFAULT_POSITION,
                new Font("SansSerif", Font.PLAIN, 15), new Color(0xffffff)));

        JPasswordField passLogIn = new JPasswordField();
        passLogIn.setBounds(50, 300, 350, 50);
        passLogIn.setForeground(new Color(0xffffff)); // Text color
        passLogIn.setCaretColor(new Color(0xffffff)); // Cursor color
        passLogIn.setBackground(new Color(255, 255, 255, 0)); // Transparent
        passLogIn.setOpaque(false);
        passLogIn.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0xffffff)),
                "Password", TitledBorder.DEFAULT_POSITION, TitledBorder.DEFAULT_POSITION,
                new Font("SansSerif", Font.PLAIN, 15), new Color(0xffffff)));

        JButton logInBtn = new JButton("LOGIN");
        logInBtn.setBounds(120, 400, 200, 40);
        logInBtn.setBackground(new Color(0x1d3557));
        logInBtn.setBorder(BorderFactory.createLineBorder(new Color(0x1d3557), 1));
        logInBtn.setFont(new Font("SquanSerif", Font.BOLD, 15));
        logInBtn.setForeground(Color.white);

        JButton exitBtn = new JButton("Exit");
        exitBtn.setBounds(170, 480, 100, 30);
        exitBtn.setBackground(new Color(0xFFCC00));
        exitBtn.setBorder(BorderFactory.createLineBorder(new Color(0x1d3557), 0));
        exitBtn.setFont(new Font("SquanSerif", Font.BOLD, 15));
        exitBtn.setForeground(new Color(0x1d3557));

        JProgressBar progressBar = new JProgressBar();
        progressBar.setBounds(100, 480, 250, 20);
        progressBar.setBackground(new Color(0x1d3557));
        progressBar.setVisible(false);
        progressBar.setStringPainted(true);
        
      


        logInBtn.addActionListener(e -> {
                String inputUsername = usernameLogIn.getText().trim();
                char[] passwordChars = passLogIn.getPassword();
                String inputPassword = new String(passwordChars);

                if (inputUsername.isEmpty() || inputPassword.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Username and password cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                List<BankUser> matchedUsers = DatabaseUtil.loadBankUsers(inputUsername, inputPassword);

                if (matchedUsers.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Invalid username or password!", "Login Failed", JOptionPane.ERROR_MESSAGE);
                } else {
                    BankUser loggedInUser = matchedUsers.get(0);
                    if (!loggedInUser.isActive) {
                        JOptionPane.showMessageDialog(null, "Account is inactive. Contact administrator.", "Login Blocked", JOptionPane.WARNING_MESSAGE);
                    } else {
                        dispose(); // Close login window
                        // Fix switch statement to use if-else for role selection
if ("Manager".equals(loggedInUser.role)) {
    new ManagerDashboard(loggedInUser, bankUsers, customerAccounts).setVisible(true);
} else if ("Teller1".equals(loggedInUser.role) || "Teller2".equals(loggedInUser.role)) {
    new navigation(loggedInUser, bankUsers, customerAccounts).setVisible(true);
} else {
    JOptionPane.showMessageDialog(null, "Unauthorized role!", "Error", JOptionPane.ERROR_MESSAGE);
}
                    }
                }
});
 exitBtn.addActionListener(e -> dispose());

        
        bigPanel.add(label);
     
      
       
      
        bigPanel.add(labelLogin);
        bigPanel.add(usernameLogIn);
        bigPanel.add(passLogIn);
       
        bigPanel.add(logInBtn);
        bigPanel.add(exitBtn);

        add(bigPanel);

        setVisible(true);
    }
}

//===============START OF MANAGER CLASS ================================================================================================== START OF MANAGER CLASS ================================================================

class ManagerDashboard extends JFrame {
    private final BankUser currentUser;
    private final List<BankUser> bankUsers;
    private final List<CustomerAccount> customerAccounts;
    private CustomerAccount currentAccount = null;

    ManagerDashboard(BankUser user, List<BankUser> bankUsers, List<CustomerAccount> customerAccounts) {
        this.currentUser = user;
        this.bankUsers = bankUsers;
        this.customerAccounts = customerAccounts;
        setSize(920, 740);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);

        BackgroundPanel bigPanel = new BackgroundPanel("assets\\2.png");
        bigPanel.setBounds(0, 0, 250 ,740);
        bigPanel.setBackground(new Color(0x1d3557));

        ImageIcon imageIconm = new ImageIcon("assets\\origlogo.png");
        Image imagem = imageIconm.getImage().getScaledInstance(180, 180, Image.SCALE_SMOOTH);
        ImageIcon resizedIconm = new ImageIcon(imagem);
        JLabel labelm = new JLabel(resizedIconm);
        labelm.setBounds(35, 20, 180, 180);

        JPanel profilePanel = new JPanel(new FlowLayout());
        profilePanel.setBounds(0, 220, 250, 95);
        profilePanel.setForeground(Color.white);
        profilePanel.setOpaque(false);
        profilePanel.setBackground(new Color(0x1d3557));

        JLabel nameLabel = new JLabel("Name : " + currentUser.name);
        nameLabel.setForeground(Color.white);
        JLabel usernameLabel = new JLabel("Username : " + currentUser.username);
        usernameLabel.setForeground(Color.white);
        JLabel contactLabel = new JLabel("Contact : " + "althea@gmail.com");
        contactLabel.setForeground(Color.white);
        JLabel branchLabel = new JLabel("Branch : BuISU Main Campus");
        branchLabel.setForeground(Color.white);

        profilePanel.add(nameLabel);
        profilePanel.add(usernameLabel);
        profilePanel.add(contactLabel);
        profilePanel.add(branchLabel);

        JPanel tellerListPanel = new JPanel();
        tellerListPanel.setLayout(new BoxLayout(tellerListPanel, BoxLayout.Y_AXIS));
        tellerListPanel.setBounds(0, 320, 250, 130);
        tellerListPanel.setBackground(new Color(0x1d3557));
        tellerListPanel.setOpaque(false);
        tellerListPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.WHITE),
            "Teller Status", TitledBorder.CENTER, TitledBorder.TOP,
            new Font("SquanSerif", Font.BOLD, 14), Color.WHITE));

        // Use the updateTellerListPanel method here
        updateTellerListPanel(tellerListPanel);

        // ... rest of your panels and logic ...
        bigPanel.add(labelm);
        bigPanel.add(profilePanel);
        bigPanel.add(tellerListPanel);
        // Continue adding other panels...

        add(bigPanel);
        setVisible(true);
    }

    // FIXED: Added updateTellerListPanel here, inside ManagerDashboard
    private void updateTellerListPanel(JPanel tellerListPanel) {
        tellerListPanel.removeAll();
        for (BankUser user : this.bankUsers) {
            if (user.role != null && user.role.toLowerCase().contains("teller")) {
                JPanel tellerEntryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                tellerEntryPanel.setBackground(new Color(0x1d3557));

                JLabel tellerNameLabel = new JLabel(user.name + " (" + user.id + ")");
                tellerNameLabel.setForeground(Color.WHITE);
                tellerNameLabel.setFont(new Font("SquanSerif", Font.PLAIN, 12));

                JLabel statusLabel = new JLabel(user.isActive ? "[Active]" : "[Inactive]");
                statusLabel.setForeground(user.isActive ? Color.GREEN : Color.RED);
                statusLabel.setFont(new Font("SquanSerif", Font.PLAIN, 12));

                JButton editStatusButton = new JButton("Edit Status");
                editStatusButton.setFont(new Font("SquanSerif", Font.PLAIN, 10));
                editStatusButton.setBackground(new Color(0xFFCC00));
                editStatusButton.setForeground(new Color(0x1d3557));
                editStatusButton.setBorder(BorderFactory.createLineBorder(new Color(0x1d3557), 1));
                editStatusButton.setFocusPainted(false);

                editStatusButton.addActionListener(e -> {
                    user.setActive(!user.isActive);
                    updateTellerListPanel(tellerListPanel);
                });

                tellerEntryPanel.add(tellerNameLabel);
                tellerEntryPanel.add(statusLabel);
                tellerEntryPanel.add(editStatusButton);

                tellerListPanel.add(tellerEntryPanel);
            }
        }
        tellerListPanel.revalidate();
        tellerListPanel.repaint();
    }
}




                              
//---------------------------------------dito maglalagaw ng mga icon dito yung sa may daxh board ---------------------------------------------------------------------------------------------------------------------------   

                                ImageIcon tcustomersicon = new ImageIcon("C:/Users/Jed/DocumentsNetBeansProjects/EventDrivenProgramming_Project/src/eventdrivenprogramming_project/customer-royalty.png");
                                Image  tcustomersimage =  tcustomersicon.getImage().getScaledInstance(180, 180, Image.SCALE_SMOOTH);
                                ImageIcon tcustomersresizedIcon = new ImageIcon( tcustomersimage);
                                JLabel  tcustomerslabel = new JLabel( tcustomersresizedIcon);
                                tcustomerslabel.setBounds(0, 0, 180, 180);
                            JPanel mp1 =new JPanel();
                            mp1.setBorder(BorderFactory.createLineBorder(new Color(0x1d3557), 3, false));
                            mp1.setBackground(Color.white); mp1.add(tcustomerslabel);
                            
                            JLabel totalcustomers = new JLabel("Total Customers : "+String.valueOf(totalCustomers));
                            
                               
                            mp1.setBackground(Color.white);
                           
                            mp1.add(tcustomerslabel);  
                            mp1.add( totalcustomers); 
                            
                            
                                
                            
                                 ImageIcon tdepositsicon = new ImageIcon("");
                                Image  tdepositsimage =  tdepositsicon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                                ImageIcon tdepositsresizedIcon = new ImageIcon( tdepositsimage);
                                JLabel tdepositslabel = new JLabel( tdepositsresizedIcon);
                            JPanel mp2 =new JPanel();
                            mp2.setBorder(BorderFactory.createLineBorder(new Color(0x1d3557), 3, false));
                            mp2.setBackground(Color.white);
                              JLabel totaldeposits = new JLabel("Total Deposits : "+ "₱" + String.format("%,.2f", totalDeposits));
                              
                            mp2.add(totaldeposits); mp2.add(tdepositslabel);
                            
                            
                                 ImageIcon twicon = new ImageIcon("");
                                Image twimage = twicon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                                ImageIcon twresizedIcon = new ImageIcon(twimage);
                                JLabel twlabel = new JLabel(twresizedIcon);
                                twlabel.setBounds(100, 10, 200, 200);
                            JPanel mp3 =new JPanel();
                            mp3.setBorder(BorderFactory.createLineBorder(new Color(0x1d3557), 3, false));
                            mp3.setBackground(Color.white);
                           
                            mp3.setBackground(Color.white);
                            
                             JLabel totalwitdrawals = new JLabel("Total Withdrawals : " + "₱" + String.format("%,.2f", totalWithdrawals));
                             
                            mp3.add(twlabel);
                            mp3.add(totalwitdrawals);
                                
                              JLabel accountscreated = new JLabel("Accounts Created : " + String.valueOf(totalCustomers));
                              ImageIcon acicon = new ImageIcon("");
                              Image acimage = acicon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                              ImageIcon acresizedIcon = new ImageIcon(acimage);
                              JLabel aclabel = new JLabel(acresizedIcon);
                              aclabel.setBounds(100, 10, 200, 200);
                            JPanel mp4 =new JPanel();
                            mp4.setBackground(Color.white);
                            mp4.setBorder(BorderFactory.createLineBorder(new Color(0x1d3557), 3, false));
                             
                            mp4.setBackground(Color.white);
                            mp4.add(aclabel);
                            mp4.add(accountscreated);
                                
                                JLabel statementscreated = new JLabel("Statements Created :");
                             ImageIcon scicon = new ImageIcon("");
                                Image scimage = scicon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                                ImageIcon scresizedIcon = new ImageIcon(acimage);
                                JLabel sclabel = new JLabel(scresizedIcon);
                                sclabel.setBounds(100, 10, 200, 200);
                            JPanel mp5 =new JPanel();
                            mp5.setBackground(Color.white);
                            mp5.setBorder(BorderFactory.createLineBorder(new Color(0x1d3557), 3, false));
                             
                            mp5.setBackground(Color.white);
                            
                              mp5.add(sclabel);
                            mp5.add(statementscreated);
                    //-----------------------      
                                 ImageIcon tcmresizedIcon = new ImageIcon(acimage);
                                JLabel tcmlabel = new JLabel(tcmresizedIcon);
                                tcmlabel.setBounds(100, 10, 200, 200);
                            JPanel mp6 =new JPanel();
                            mp6.setBackground(Color.white);
                            mp6.setBorder(BorderFactory.createLineBorder(new Color(0x1d3557), 3, false));
                             JLabel transactioncompleted = new JLabel("Transaction Completed : "+ String.valueOf(totalTransactions));
                            
                            mp6.setBackground(Color.white);
                            
                              mp6.add(tcmlabel);
                            
                            mp6.add(transactioncompleted);

                            con.add(mp1);
                             con.add(mp2);
                              con.add(mp3);
                               con.add(mp4);
                                con.add(mp5);
                                 con.add(mp6);

//------------------------------------------------------------maglalagay ng icon ---------------------------------------------------------------------------maglalagay ng icon ---------------------------------------------------

                        homePanel.add(con1);
                        homePanel.add(con);


                  cardPanel.add(deleteAccPanel, "Delete");

                        JLabel deleteAccountTitle = new JLabel("Delete Account");
                        deleteAccountTitle.setBounds(50, 50, 150, 25);
                        deleteAccPanel.add(deleteAccountTitle);


                           cardPanel.add(updateCustomerPanel, "Update");

                 cardPanel.add(transactionsPanel, "View");

                     JLabel viewAccountTypestitle = new JLabel("View Transactions");
                           viewAccountTypestitle.setBounds(50, 50, 100, 25);

                           transactionsPanel.add(viewAccountTypestitle);

                           






//-------------------------------START! of Update Customer Info ----------------------------------------------------------------------------------START! of Update Customer Info-----------------------------------------------------
 
      
                           cardPanel.add(updateCustomerPanel, "Update");


                          
                            JLabel searchLabel = new JLabel("Search by Email, Name, Customer ID, or Account ID:");
                            searchLabel.setBounds(50, 60, 400, 25);
                            updateCustomerPanel.add(searchLabel);

                            JTextField searchField = new JTextField();
                            searchField.setBounds(50, 90, 400, 30);
                            updateCustomerPanel.add(searchField);

                            JButton searchButton = new JButton("Search");
                            searchButton.setBounds(470, 90, 100, 30);

                            // Non-editable Customer ID and Account ID
                            JLabel customerIDLabel = new JLabel("");
                            customerIDLabel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0x1d3557)),
                                "Customer ID :", TitledBorder.DEFAULT_POSITION, TitledBorder.DEFAULT_POSITION,
                                new Font("SansSerif", Font.PLAIN, 15), new Color(0x1d3557))); 
                            customerIDLabel.setBounds(50, 130, 250, 45);
                            updateCustomerPanel.add(customerIDLabel);

                            JLabel accountIDLabel = new JLabel("");
                            accountIDLabel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0x1d3557)),
                                "Account ID :", TitledBorder.DEFAULT_POSITION, TitledBorder.DEFAULT_POSITION,
                                new Font("SansSerif", Font.PLAIN, 15), new Color(0x1d3557))); 
                            accountIDLabel.setBounds(320, 130, 250, 45);
                            updateCustomerPanel.add(accountIDLabel);
                            

                            JList<String> suggestionList = new JList<>();
                            suggestionList.setBounds(50, 130, 400, 100);
                            suggestionList.setVisible(false);
                            updateCustomerPanel.add(suggestionList);
                            
                            JPanel panel1 = new JPanel(new GridLayout(0,1,4,4));
                            panel1.setBounds(50, 185, 520, 250);

                            // Editable Fields for First Name, Middle Name, and Last Name
                            JTextField updateFirstName = new JTextField();
                            updateFirstName.setBounds(50, 185, 250, 45);
                            updateFirstName.setBorder(BorderFactory.createTitledBorder("First Name"));
                            panel1.add(updateFirstName);

                            JTextField updateMiddleName = new JTextField();
                            updateMiddleName.setBounds(320, 185, 250, 45);
                            updateMiddleName.setBorder(BorderFactory.createTitledBorder("Middle Name"));
                            panel1.add(updateMiddleName);

                            JTextField updateLastName = new JTextField();
                            updateLastName.setBounds(50, 240, 520, 45);
                            updateLastName.setBorder(BorderFactory.createTitledBorder("Last Name"));
                            panel1.add(updateLastName);
                            
                           // Email (Editable)
                           JTextField updateEmail = new JTextField();
                           updateEmail.setBounds(50, 295, 250, 45);                       
                           updateEmail.setBorder(BorderFactory.createTitledBorder("Email Address"));
                           panel1.add(updateEmail);
                           
                            // Phone Number (Editable)
                           JTextField updatePhone = new JTextField();
                           updatePhone.setBounds(320, 295, 250, 45);
                           updatePhone.setBorder(BorderFactory.createTitledBorder("Phone Number"));
                           panel1.add(updatePhone);
                           
                           updateCustomerPanel.add(panel1);
                           
                           JPanel panel2 = new JPanel(new GridLayout(3,3,4,4));
                            panel2.setBounds(50, 440, 520, 185);
                            panel2.setBackground(Color.white);
                           
                           
                           
                            // Complete Address
                           JTextField updateStreet = new JTextField();
                           updateStreet.setBounds(50, 350, 520, 45);                        
                           updateStreet.setBorder(BorderFactory.createTitledBorder("Street"));
                            panel2.add(updateStreet);

                           JTextField updateBarangay = new JTextField();
                           updateBarangay.setBounds(50, 405, 250, 45);                          
                           updateBarangay.setBorder(BorderFactory.createTitledBorder("Barangay"));
                            panel2.add(updateBarangay);

                           JTextField updateMunicipality = new JTextField();
                           updateMunicipality.setBounds(320, 405, 250, 45);
                           updateMunicipality.setBorder(BorderFactory.createTitledBorder("Municipality"));
                            panel2.add(updateMunicipality);

                           JTextField updateProvinceCity = new JTextField();
                           updateProvinceCity.setBounds(50, 460, 250, 45);
                           updateProvinceCity.setBorder(BorderFactory.createTitledBorder("Province/City"));
                            panel2.add(updateProvinceCity);

                           JTextField updateZip = new JTextField();
                           updateZip.setBounds(320, 460, 250, 45);
                           updateZip.setBorder(BorderFactory.createTitledBorder("Zip Code"));
                            panel2.add(updateZip);
                           
                            // Gender (Disabled Radio Buttons)
                            JPanel genderPanel = new JPanel();
                            genderPanel.setBounds(50, 515, 250, 50);
                            genderPanel.setBackground(Color.white);
                            genderPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0x1d3557)),
                                    "Gender :", TitledBorder.DEFAULT_POSITION, TitledBorder.DEFAULT_POSITION,
                                    new Font("SansSerif", Font.PLAIN, 15), new Color(0x1d3557)));

                            JRadioButton maleButton = new JRadioButton("Male");
                            JRadioButton femaleButton = new JRadioButton("Female");

                            maleButton.setFont(new Font("SansSerif", Font.PLAIN, 15));
                            femaleButton.setFont(new Font("SansSerif", Font.PLAIN, 15));
                            femaleButton.setBackground(Color.white);
                            maleButton.setBackground(Color.white);

                            ButtonGroup genderGroup = new ButtonGroup();
                            genderGroup.add(maleButton);
                            genderGroup.add(femaleButton);

                            genderPanel.add(maleButton);
                            genderPanel.add(femaleButton);
                             panel2.add(genderPanel);
                             updateCustomerPanel.add(panel2);
                                                        
                             // Age (Non-editable, populated based on Birthday)
                            JTextField updateAge = new JTextField();
                            updateAge.setBounds(50, 580, 250, 50);
                            updateAge.setBorder(BorderFactory.createTitledBorder("Age"));
                            updateAge.setEditable(false);
                           
      
                            // Type of Account (Non-editable ComboBox)
                           String[] accountTypes = { "Savings Account", "Checking Account" };
                           JComboBox<String> updateAccountType = new JComboBox<>(accountTypes);
                           updateAccountType.setBounds(320, 580, 250, 50);
                           updateAccountType.setBorder(BorderFactory.createTitledBorder("Type of Account"));
                           updateAccountType.setEnabled(false);
                           panel2.add(updateAccountType);
                            
                            JButton updateBtn = new JButton("Update");
                            updateBtn.setBounds(70, 650, 130, 30);
                            updateBtn.setBackground(new Color(0x1d3557));
                            updateBtn.setForeground(Color.white);
                            updateCustomerPanel.add(updateBtn);

                            JButton clearBtn = new JButton("Clear");
                            clearBtn.setBounds(250, 650, 130, 30);
                            clearBtn.setBackground(new Color(0x1d3557));
                            clearBtn.setForeground(Color.white);
                            updateCustomerPanel.add(clearBtn);
                           
                            JButton deleteBtn = new JButton("Delete Account");
                            deleteBtn.setBounds(430, 650, 130, 30);
                            deleteBtn.setBackground(new Color(0xFF0000)); // Red color for delete
                            deleteBtn.setForeground(Color.white);
                            updateCustomerPanel.add(deleteBtn);


                            
         
                            updateCustomerPanel.add(searchButton);     
                            
                            
                            
                           
                         
searchButton.addActionListener(e -> {
    String query = searchField.getText().trim();
    
    if (query.isEmpty()) {
        JOptionPane.showMessageDialog(null, "Please enter a search keyword (Account ID, Customer ID, Name, or Email).", "Empty Field", JOptionPane.WARNING_MESSAGE);
        return;
    }

    // Assuming your DatabaseUtil can get a customer account by exact account or customer ID.
    // If you want to search by name or email too, you might need to adjust this accordingly.
    CustomerAccount acc = DatabaseUtil.getCustomerAccount(query);
    currentAccount = DatabaseUtil.getCustomerAccount(query);
if (currentAccount != null) {
    // Display account details in UI
    updateFirstName.setText(currentAccount.firstName);
    accountIDLabel.setText(currentAccount.accountID);}

    if (acc == null) {
        JOptionPane.showMessageDialog(null, "Account not found.", "Error", JOptionPane.ERROR_MESSAGE);
        // Optionally clear all fields
     
    updateFirstName.setText("");
    updateMiddleName.setText("");
    updateLastName.setText("");
    updateStreet.setText("");
    updateBarangay.setText("");
    updateMunicipality.setText("");
    updateProvinceCity.setText("");
    updateZip.setText("");
    updatePhone.setText("");
    updateEmail.setText("");
    customerIDLabel.setText(" ");
    accountIDLabel.setText(" ");
    updateAccountType.setSelectedIndex(-1);
    genderGroup.clearSelection();


        return;
    }
    if (!acc.is_active) {
        JOptionPane.showMessageDialog(null, "This account is inactive.", "Warning", JOptionPane.WARNING_MESSAGE);
       updateFirstName.setText("");
    updateMiddleName.setText("");
    updateLastName.setText("");
    updateStreet.setText("");
    updateBarangay.setText("");
    updateMunicipality.setText("");
    updateProvinceCity.setText("");
    updateZip.setText("");
    updatePhone.setText("");
    updateEmail.setText("");
    customerIDLabel.setText(" ");
    accountIDLabel.setText(" ");
    updateAccountType.setSelectedIndex(-1);
    genderGroup.clearSelection();
        return;
    }

    // Populate update fields here
    updateFirstName.setText(acc.firstName);
    updateMiddleName.setText(acc.middleName);
    updateLastName.setText(acc.lastName);
    updateStreet.setText(acc.street);
    updateBarangay.setText(acc.barangay);
    updateMunicipality.setText(acc.municipality);
    updateProvinceCity.setText(acc.provinceCity);
    updateZip.setText(acc.zip);
    updatePhone.setText(acc.phone);
    updateEmail.setText(acc.email);

    customerIDLabel.setText("" + acc.customerID);
    accountIDLabel.setText("" + acc.accountID + " (" + acc.accountType + ")");
    updateAccountType.setSelectedItem(acc.accountType);

    if ("Male".equalsIgnoreCase(acc.gender)) {
        maleButton.setSelected(true);
    } else if ("Female".equalsIgnoreCase(acc.gender)) {
        femaleButton.setSelected(true);
    } else {
        genderGroup.clearSelection();
    }

    // Store original info for updates
    updateCustomerPanel.putClientProperty("originalCustomerID", acc.customerID);
    updateCustomerPanel.putClientProperty("originalAccountType", acc.accountType);
});

// A helper method to clear update fields if needed








updateBtn.addActionListener(e -> {
    System.out.println("\n=== UPDATE BUTTON CLICKED ===");
    if (updateFirstName.getText().trim().isEmpty() || updateLastName.getText().trim().isEmpty()) {
        JOptionPane.showMessageDialog(null, 
            "First Name and Last Name are required", 
            "Validation Error", 
            JOptionPane.WARNING_MESSAGE);
        return;
    }
    String newEmail = updateEmail.getText().trim();
    if (!newEmail.isEmpty() && !newEmail.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
        JOptionPane.showMessageDialog(null, 
            "Invalid email format", 
            "Error", 
            JOptionPane.ERROR_MESSAGE);
        return;
    }
    String newPhone = updatePhone.getText().trim();
    if (!newPhone.matches("^09\\d{9}$")) {
        JOptionPane.showMessageDialog(null, 
            "Invalid phone number (09XXXXXXXXX)", 
            "Error", 
            JOptionPane.ERROR_MESSAGE);
        return;
    }
    String customerID = (String) updateCustomerPanel.getClientProperty("originalCustomerID");
    if (customerID == null) {
        JOptionPane.showMessageDialog(null, 
            "Search for a customer first!", 
            "Error", 
            JOptionPane.ERROR_MESSAGE);
        return;
    }
    CustomerAccount foundAcc = null;
    for (CustomerAccount a : customerAccounts) {
        if (a.customerID.equals(customerID)) {
            foundAcc = a;
            break;
        }
    }
    if (foundAcc == null) {
        JOptionPane.showMessageDialog(null, "Customer not found in memory!", "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }
    // Update fields
    foundAcc.firstName = updateFirstName.getText().trim();
    foundAcc.middleName = updateMiddleName.getText().trim();
    foundAcc.lastName = updateLastName.getText().trim();
    foundAcc.street = updateStreet.getText().trim();
    foundAcc.barangay = updateBarangay.getText().trim();
    foundAcc.municipality = updateMunicipality.getText().trim();
    foundAcc.provinceCity = updateProvinceCity.getText().trim();
    foundAcc.zip = updateZip.getText().trim();
    foundAcc.phone = newPhone;
    foundAcc.email = newEmail;
    // Update gender
    if (maleButton.isSelected()) {
        foundAcc.gender = "Male";
    } else if (femaleButton.isSelected()) {
        foundAcc.gender = "Female";
    } else {
        foundAcc.gender = null;
    }
    // Update account type
    foundAcc.accountType = (String) updateAccountType.getSelectedItem();
    // Update database
    boolean dbUpdated = DatabaseUtil.updateCustomerAccount(foundAcc);
    // Print confirmation
    System.out.println("\n=== UPDATED CUSTOMER DETAILS ===");
    System.out.println("Customer ID: " + foundAcc.customerID);
    System.out.println("Account ID: " + foundAcc.accountID);
    System.out.println("Name: " + foundAcc.firstName + " " + foundAcc.lastName);
    System.out.println("Email: " + foundAcc.email);
    System.out.println("Phone: " + foundAcc.phone);
    System.out.println("Gender: " + foundAcc.gender);
    System.out.println("Account Type: " + foundAcc.accountType);
    System.out.println("Address: " + foundAcc.street + ", " + foundAcc.barangay);
    System.out.println("=====================================\n");
    // Show feedback
    if (dbUpdated) {
        JOptionPane.showMessageDialog(null, 
            "Update successful!", 
            "Success", 
            JOptionPane.INFORMATION_MESSAGE);
    } else {
        JOptionPane.showMessageDialog(null, 
            "Update failed in database!", 
            "Error", 
            JOptionPane.ERROR_MESSAGE);
    }
});


                                   
clearBtn.addActionListener(e -> {
    searchField.setText("");
    updateFirstName.setText("");
    updateMiddleName.setText("");
    updateLastName.setText("");
    updateStreet.setText("");
    updateBarangay.setText("");
    updateMunicipality.setText("");
    updateProvinceCity.setText("");
    updateZip.setText("");
    updatePhone.setText("");
    updateEmail.setText("");
    genderGroup.clearSelection();
    updateAccountType.setSelectedIndex(-1);
    customerIDLabel.setText(" ");
    accountIDLabel.setText(" ");
    updateCustomerPanel.putClientProperty("originalCustomerID", null);
    updateEmail.putClientProperty("originalEmail", null);
    suggestionList.setVisible(false);
});

deleteBtn.addActionListener(e -> {
    if (currentAccount == null) {
        JOptionPane.showMessageDialog(null, "No account selected!", "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    int confirm = JOptionPane.showConfirmDialog(
        null, 
        "Delete account " + currentAccount.accountID + "?", 
        "Confirm Deletion", 
        JOptionPane.YES_NO_OPTION
    );

    if (confirm == JOptionPane.YES_OPTION) {
        boolean success = DatabaseUtil.deleteCustomerAccount(currentAccount.accountID);
        if (success) {
            // Remove the account from the in-memory list
            customerAccounts.removeIf(acc -> acc.accountID.equals(currentAccount.accountID));
            JOptionPane.showMessageDialog(null, "Account deleted!", "Success", JOptionPane.INFORMATION_MESSAGE);
            
            // Clear UI fields and references
            currentAccount = null;
            searchField.setText("");
            updateFirstName.setText("");
            updateMiddleName.setText("");
            updateLastName.setText("");
            updateStreet.setText("");
            updateBarangay.setText("");
            updateMunicipality.setText("");
            updateProvinceCity.setText("");
            updateZip.setText("");
            updatePhone.setText("");
            updateEmail.setText("");
            customerIDLabel.setText(" ");
            accountIDLabel.setText(" ");
            genderGroup.clearSelection();
            updateAccountType.setSelectedIndex(-1);
        } else {
            JOptionPane.showMessageDialog(null, "Deletion failed!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
});


                         
//----------------------------- END of Update Customer Info --------------------------------------------------------------------------------------END of Update Customer Info----------------------------------------------------
                
                   homeBtn.addActionListener(e -> cardLayout.show(cardPanel, "Home"));
                   

                     updateCustomerInformationBtn.addActionListener(e -> cardLayout.show(cardPanel, "Update"));


                     transactionsBtn.addActionListener(e -> cardLayout.show(cardPanel, "View"));
logOutBtn.addActionListener(e -> {
    int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to log out?", "Confirm Logout", JOptionPane.YES_NO_OPTION);
    if (confirm == JOptionPane.YES_OPTION) {
        dispose();
        new loginPage(bankUsers, customerAccounts).setVisible(true);
    }
});



        add(bigPanel);
        add(cardPanel);

     setVisible(true);

    }
} // <-- This closes the navigation class
//=========================START OF NAVIGATION (TELLER DASHBOARD)============================================

class navigation extends JFrame {
    DecimalFormat decimalFormat = new DecimalFormat("₱#,##0.00");
    ArrayList<CustomerAccount> customerAccounts;
    private BankUser currentUser;
    private List<BankUser> bankUsers;

    navigation(BankUser user, List<BankUser> bankUsers, List<CustomerAccount> customerAccounts) {
        this.currentUser = user;
        this.bankUsers = bankUsers;
        this.customerAccounts = new ArrayList<>(customerAccounts);
        setTitle("Teller Dashboard");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        JLabel welcomeLabel = new JLabel("Welcome, " + currentUser.name + " (" + currentUser.role + ")", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(welcomeLabel, BorderLayout.NORTH);
        JPanel mainPanel = new JPanel();
        mainPanel.add(new JLabel("Teller dashboard content goes here."));
        add(mainPanel, BorderLayout.CENTER);
        setVisible(true);
    }
}

// Sample BankUser class for completeness of this file.
// Make sure yours matches these fields/methods:
class BankUser {
    public String id;
    public String name;
    public String username;
    public String role;
    public boolean isActive;

    public void setActive(boolean active) { this.isActive = active; }
}
class navigation extends JFrame{
    DecimalFormat decimalFormat = new DecimalFormat("₱#,##0.00");
    ArrayList<eventdrivenprogramming_project.CustomerAccount> customerAccounts;
    private eventdrivenprogramming_project.BankUser currentUser;
    private ArrayList<eventdrivenprogramming_project.BankUser> bankUsers;


    navigation(eventdrivenprogramming_project.BankUser user, ArrayList<eventdrivenprogramming_project.BankUser> bankUsers, ArrayList<eventdrivenprogramming_project.CustomerAccount> customerAccounts) {
        this.currentUser = user;
        this.bankUsers = bankUsers;
        this.customerAccounts = customerAccounts;
     setSize(920, 740);
     setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
     setLocationRelativeTo(null);
     setLayout(null);

           eventdrivenprogramming_project.BackgroundPanel bigPanel = new eventdrivenprogramming_project.BackgroundPanel("C:/Users/Jed/Documents/NetBeansProjects/EventDrivenProgramming_Project/src/eventdrivenprogramming_project/2.png");
                bigPanel.setBounds(0, 0, 250 ,740);
                bigPanel.setBackground(new Color(0x1d3557));

                     ImageIcon imageIconm = new ImageIcon("C:/Users/Jed/Documents/NetBeansProjects/EventDrivenProgramming_Project/src/eventdrivenprogramming_project/origlogo.png");
                        Image imagem = imageIconm.getImage().getScaledInstance(180, 180, Image.SCALE_SMOOTH);
                        ImageIcon resizedIconm = new ImageIcon(imagem);
                    JLabel labelm = new JLabel(resizedIconm);
                        labelm.setBounds(35, 20, 180, 180);

                    

                       JPanel profilePanel = new JPanel(null);
                        profilePanel.setBounds(0, 210, 250, 80);
                        profilePanel.setOpaque(false);
                        profilePanel.setBackground(new Color(0x1d3557));
                        profilePanel.setOpaque(false);

                        JLabel roleIcon = new JLabel();
                        ImageIcon profilePic = new ImageIcon("");
                        Image scaledPic = profilePic.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                        roleIcon.setIcon(new ImageIcon(scaledPic));
                        roleIcon.setBounds(20, 10, 40, 40);

                        JLabel roleName = new JLabel(currentUser.role + ": " + currentUser.name);
                        roleName.setFont(new Font("SquanSerif", Font.PLAIN, 12));
                        roleName.setForeground(Color.WHITE);
                        roleName.setBounds(50, 10, 150, 20);

                        JLabel role = new JLabel(currentUser.role);
                        role.setFont(new Font("SquanSerif", Font.ITALIC, 11));
                        role.setForeground(Color.WHITE);
                        role.setBounds(98, 30, 150, 20);
                        
                        JLabel statusLabel = new JLabel("Status: Active");
                        statusLabel.setFont(new Font("SquanSerif", Font.ITALIC, 11));
                        statusLabel.setForeground(Color.GREEN); 
                        statusLabel.setBounds(85, 50, 150, 20);
                        
                        profilePanel.add(roleIcon);
                        profilePanel.add(roleName);
                        profilePanel.add(role);
                        profilePanel.add(statusLabel);

            JPanel panelForBtns = new JPanel(new GridLayout(0,1,5,5));
               panelForBtns.setBounds(0, 400, 250, 300);
               panelForBtns.setOpaque(false);
               panelForBtns.setBackground(new Color(0x1d3557));
               
                        ImageIcon homenavicon = new ImageIcon("");
                                Image  homenavimage =  homenavicon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                                ImageIcon homenavresizedIcon = new ImageIcon( homenavimage);
                                JLabel  homenavlabel = new JLabel( homenavresizedIcon);
                JButton homeBtn = new JButton("Home");
                    homeBtn.setBackground(new Color(0x1d3557));
                    homeBtn.setForeground(new Color(0xFFFFFF));
                     homeBtn.setOpaque(false);
                    homeBtn.setFont(new Font("SquanSerif",Font.PLAIN,12));
                    homeBtn.add(homenavlabel);
                    
                         ImageIcon createaccicon = new ImageIcon("");
                                Image  createaccimage =  createaccicon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                                ImageIcon createaccresizedIcon = new ImageIcon( createaccimage);
                                JLabel  createacclabel = new JLabel( createaccresizedIcon);
                JButton createAccountBtn=new JButton("Create Account");
                    createAccountBtn.setBackground(new Color(0x1d3557));
                    createAccountBtn.setForeground(new Color(0xFFFFFF));
                      createAccountBtn.setOpaque(false);
                     createAccountBtn.setFont(new Font("SquanSerif",Font.PLAIN,12));
                     createAccountBtn.add(createacclabel);
                     
                     
                        ImageIcon createtransicon = new ImageIcon("");
                                Image  createtransimage =  createtransicon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                                ImageIcon createtransresizedIcon = new ImageIcon( createtransimage);
                                JLabel  createtranslabel = new JLabel( createtransresizedIcon);
                JButton createTransactionBtn=new JButton("Create Transaction");
                     createTransactionBtn.setBackground(new Color(0x1d3557));
                     createTransactionBtn.setForeground(new Color(0xFFFFFF));
                      createTransactionBtn.setOpaque(false);
                      createTransactionBtn.setFont(new Font("SquanSerif",Font.PLAIN,12));
                      createTransactionBtn.add(createtranslabel);


                         ImageIcon generateaccicon = new ImageIcon("");
                                Image  generateaccimage = generateaccicon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                                ImageIcon generateaccresizedIcon = new ImageIcon( generateaccimage);
                                JLabel  generateacclabel = new JLabel( generateaccresizedIcon);
                JButton generateAccStatementBtn=new JButton("Generate Account Statements");
                     generateAccStatementBtn.setBackground(new Color(0x1d3557));
                     generateAccStatementBtn.setForeground(new Color(0xFFFFFF));
                       generateAccStatementBtn.setOpaque(false);
                      generateAccStatementBtn.setFont(new Font("SquanSerif",Font.PLAIN,12));


                JButton logOutBtn=new JButton("Log out");
                    logOutBtn.setBackground(new Color(0x1d3557));
                    logOutBtn.setForeground(new Color(0xFFFFFF));
                    logOutBtn.setOpaque(false);
                     logOutBtn.setFont(new Font("SquanSerif",Font.PLAIN,12));

             panelForBtns.add(homeBtn);
             panelForBtns.add(createAccountBtn);
             panelForBtns.add(createTransactionBtn);
             panelForBtns.add(generateAccStatementBtn);
             panelForBtns.add(logOutBtn);



       
        bigPanel.add(labelm);
        bigPanel.add(profilePanel);
        bigPanel.add(panelForBtns);

        CardLayout cardLayout = new CardLayout();
          JPanel cardPanel = new JPanel(cardLayout);
          cardPanel.setBounds(250, 0, 650, 700);


           JPanel homePanel = new JPanel(null);
          homePanel.setBackground(Color.white);

          JPanel createAccPanel = new JPanel(null);
          createAccPanel.setBackground(new Color(0xffffff));
          createAccPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0x1d3557)),
                                "CREATE ACCOUNT", TitledBorder.CENTER, TitledBorder.CENTER,
                                new Font("SansSerif", Font.BOLD, 40), new Color(0x1d3557)));

          JPanel createTransactionPanel = new JPanel(null);
          createTransactionPanel.setBackground(Color.white);

          JPanel generateStatementPanel = new JPanel(null);
          generateStatementPanel.setBackground(Color.white);
          generateStatementPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0x1d3557)),
                                "GENERATE STATEMENT", TitledBorder.CENTER, TitledBorder.CENTER,
                                new Font("SansSerif", Font.BOLD, 40), new Color(0x1d3557)));

    }
}