package EDP;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ITRUSTCustomerAcc {
    public boolean is_active;
    public int feeCycleCounter;
    public String customerID;
    public String accountID;
    public String firstName;
    public String middleName;
    public String lastName;
    public Date birthDate;
    public int age;
    public String street;
    public String barangay;
    public String municipality;
    public String provinceCity;
    public String zip;
    public String phone;
    public String email;
    public String sex;
    public String accountType;
    public double balance;
    public int transactionCount;
    public Date lastTransactionDate;
    public Date lastInterestFeeApplicationDate;
    public List<Transaction> transactionHistory;

    public ITRUSTCustomerAcc(String customerID, String accountID, String firstName,
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
        this.sex = gender;
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
        java.util.Calendar birthCal = java.util.Calendar.getInstance();
        birthCal.setTime(birthDate);
        java.util.Calendar todayCal = java.util.Calendar.getInstance();
        int age = todayCal.get(java.util.Calendar.YEAR) - birthCal.get(java.util.Calendar.YEAR);
        if (todayCal.get(java.util.Calendar.MONTH) < birthCal.get(java.util.Calendar.MONTH) ||
           (todayCal.get(java.util.Calendar.MONTH) == birthCal.get(java.util.Calendar.MONTH) &&
            todayCal.get(java.util.Calendar.DAY_OF_MONTH) < birthCal.get(java.util.Calendar.DAY_OF_MONTH))) {
            age--;
        } else {
            age = 0;
        }
        return age;
    }
}
