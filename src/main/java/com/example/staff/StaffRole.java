package com.example.staff;

import com.example.bank.model.Employee;
import com.example.bank.model.LoanStatus;
import com.example.bank.service.AuthService;
import com.example.bank.service.LoanService;
import javafx.util.Callback;

import java.util.Optional;
import java.util.Set;

/**
 * What differs between the employee app and the manager app. The screens
 * are shared; everything role-specific is asked of the role.
 */
public interface StaffRole {

    String name();

    /** The FXML file for a page, as a resource path. */
    String resource(Page page);

    Optional<Employee> signIn(AuthService auth, String username, String password);

    void changePassword(AuthService auth, String username, String current, String next);

    /** Loans this role decides on. */
    Set<LoanStatus> loansToDecide();

    /** Loans counted as still in progress on this role's loan screen. */
    Set<LoanStatus> loansInProgress();

    /** Loans shown under "Loan Status". */
    Set<LoanStatus> loanHistory();

    void decideLoan(LoanService loans, long loanId, boolean approve);

    /** The label of the reviewed-cheques view. */
    String reviewedChequesLabel();

    /** Creates controllers for this role's FXML: pages get the role, cards are created plainly. */
    default Callback<Class<?>, Object> controllers() {
        StaffRole role = this;
        return type -> {
            try {
                for (var c : type.getConstructors()) {
                    if (c.getParameterCount() == 1 && c.getParameterTypes()[0] == StaffRole.class) return c.newInstance(role);
                }
                return type.getConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot create " + type.getName(), e);
            }
        };
    }
}
