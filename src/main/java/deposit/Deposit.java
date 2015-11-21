package deposit;

/**
 * Created by ehsan on 11/18/15.
 */
public class Deposit {

    private String ownersName;
    private String id;
    private Long initialBalance;
    private Long upperBound;

    public Deposit(String ownersName, String id, Long initialBalance, Long upperBound) {
        this.ownersName = ownersName;
        this.id = id;
        this.initialBalance = initialBalance;
        this.upperBound = upperBound;
    }

    public String getID() {
        return this.id;
    }
    public String getOwnersName() {
        return ownersName;
    }
    public Long getInitialBalance() {
        return initialBalance;
    }
    public Long getUpperBound() {
        return upperBound;
    }

    public boolean deposit(Long depositValue) {
        if (initialBalance + depositValue < upperBound) {
            synchronized (initialBalance) {
                initialBalance += depositValue;
            }
            return true;
        }
        return false;
    }

    public boolean withdraw(Long withdrawValue) {
        if (withdrawValue <= initialBalance) {
            synchronized (initialBalance) {
                initialBalance -= withdrawValue;
            }
            return true;
        }
        return false;
    }

    public String toString() {
        String me;
        me = "Owners name: " + ownersName;
        me += "\nID: " + id;
        me += "\nInitialBalance: " + initialBalance;
        me += "\nUpperBound: " + upperBound;
        return me;
    }

}
