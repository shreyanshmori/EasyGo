package common.utilities;

import common.database.UserDAO;
import common.exceptions.*;
import common.payment.PaymentService;
import common.validation.Validator;
import datastructures.linkedlist.CustomLinkedList;

/**
 * AuthService — handles all authentication and account management.
 *
 * Features:
 *   - User signup with full validation
 *   - User / Admin login with SHA-256 password verification
 *   - Session management via AuthManager
 *   - Login history recording
 *   - Profile view / update
 *   - Password change
 *   - Default admin seed (first run)
 */
public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SIGNUP
    // ══════════════════════════════════════════════════════════════════════════

    public int signup() throws ReservationException {
        ConsoleUtils.printHeader("CREATE ACCOUNT");

        // Collect details
        String username = ConsoleUtils.readStringNonEmpty("Username");
        Validator.validUsername(username);

        if (userDAO.usernameExists(username))
            throw new ValidationException("username", "'" + username + "' is already taken.");

        String fullName = ConsoleUtils.readStringNonEmpty("Full Name");
        Validator.validName(fullName, "full_name");

        String email = ConsoleUtils.readStringNonEmpty("Email Address");
        Validator.validEmail(email);

        String phone = ConsoleUtils.readStringNonEmpty("Phone Number (10 digits)");
        Validator.validPhone(phone);

        String password = ConsoleUtils.readPassword("Password");
        Validator.validPassword(password);

        String confirm = ConsoleUtils.readPassword("Confirm Password");
        if (!password.equals(confirm))
            throw new ValidationException("password", "Passwords do not match.");

        // Register
        int userId = userDAO.registerUser(username, password, fullName, email, phone);
        if (userId == -1)
            throw new ReservationException("SIGNUP_FAILED", "Account creation failed.");

        ConsoleUtils.printSuccess("Account created! Welcome, " + fullName + ".");
        ConsoleUtils.printInfo("You can now log in with username: " + username);
        return userId;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  USER LOGIN
    // ══════════════════════════════════════════════════════════════════════════

    public boolean loginUser() throws ReservationException {
        ConsoleUtils.printHeader("USER LOGIN");

        String username = ConsoleUtils.readStringNonEmpty("Username");
        String password = ConsoleUtils.readPassword("Password");

        int[] result = userDAO.loginUser(username, password);

        if (result == null) {
            throw new AuthenticationException("Invalid username or password.");
        }
        if (result[0] == -2) {
            throw new AuthenticationException("Your account has been deactivated. Contact support.");
        }

        int userId = result[0];
        AuthManager.setSession(userId, username, false);
        userDAO.recordLogin(userId, false);

        // Greet user
        String[] profile = userDAO.getUserProfile(userId);
        String name = (profile != null) ? profile[1] : username;
        ConsoleUtils.printSuccess("Welcome back, " + name + "!");
        return true;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ADMIN LOGIN
    // ══════════════════════════════════════════════════════════════════════════

    public boolean loginAdmin() throws ReservationException {
        ConsoleUtils.printHeader("ADMIN LOGIN");

        String username = ConsoleUtils.readStringNonEmpty("Admin Username");
        String password = ConsoleUtils.readPassword("Admin Password");

        int[] result = userDAO.loginAdmin(username, password);
        if (result == null)
            throw new AuthenticationException("Invalid admin credentials.");

        int adminId = result[0];
        AuthManager.setSession(adminId, username, true);
        userDAO.recordLogin(adminId, true);

        String[] profile = userDAO.getAdminProfile(adminId);
        String name = (profile != null) ? profile[1] : username;
        ConsoleUtils.printSuccess("Admin access granted. Welcome, " + name + "!");
        return true;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LOGOUT
    // ══════════════════════════════════════════════════════════════════════════

    public void logout() {
        String name = AuthManager.getUsername();
        AuthManager.clearSession();
        PaymentService.clearSession();
        ConsoleUtils.printInfo("Logged out. Goodbye, " + (name != null ? name : "user") + "!");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PROFILE
    // ══════════════════════════════════════════════════════════════════════════

    public void showProfile() throws DatabaseException {
        if (!AuthManager.isLoggedIn()) {
            ConsoleUtils.printError("Not logged in.");
            return;
        }
        if (AuthManager.isAdmin()) {
            String[] p = userDAO.getAdminProfile(AuthManager.getUserId());
            if (p == null) { ConsoleUtils.printError("Profile not found."); return; }
            ConsoleUtils.printHeader("ADMIN PROFILE");
            ConsoleUtils.printInfo("Username  : " + p[0]);
            ConsoleUtils.printInfo("Full Name : " + p[1]);
            ConsoleUtils.printInfo("Role      : " + p[2]);
            ConsoleUtils.printInfo("Joined    : " + p[3]);
        } else {
            String[] p = userDAO.getUserProfile(AuthManager.getUserId());
            if (p == null) { ConsoleUtils.printError("Profile not found."); return; }
            ConsoleUtils.printHeader("MY PROFILE");
            ConsoleUtils.printInfo("Username  : " + p[0]);
            ConsoleUtils.printInfo("Full Name : " + p[1]);
            ConsoleUtils.printInfo("Email     : " + p[2]);
            ConsoleUtils.printInfo("Phone     : " + p[3]);
            ConsoleUtils.printInfo("Joined    : " + p[4]);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UPDATE PROFILE
    // ══════════════════════════════════════════════════════════════════════════

    public void updateProfile() throws ReservationException {
        if (!AuthManager.isLoggedIn() || AuthManager.isAdmin()) {
            ConsoleUtils.printError("Feature not available."); return;
        }
        ConsoleUtils.printHeader("UPDATE PROFILE");
        String fullName = ConsoleUtils.readStringNonEmpty("New Full Name");
        Validator.validName(fullName, "full_name");
        String email = ConsoleUtils.readStringNonEmpty("New Email");
        Validator.validEmail(email);
        String phone = ConsoleUtils.readStringNonEmpty("New Phone (10 digits)");
        Validator.validPhone(phone);

        userDAO.updateProfile(AuthManager.getUserId(), fullName, email, phone);
        ConsoleUtils.printSuccess("Profile updated successfully.");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CHANGE PASSWORD
    // ══════════════════════════════════════════════════════════════════════════

    public void changePassword() throws ReservationException {
        if (!AuthManager.isLoggedIn() || AuthManager.isAdmin()) {
            ConsoleUtils.printError("Feature not available."); return;
        }
        ConsoleUtils.printHeader("CHANGE PASSWORD");

        String current = ConsoleUtils.readPassword("Current Password");
        int[]  check   = userDAO.loginUser(AuthManager.getUsername(), current);
        if (check == null)
            throw new AuthenticationException("Current password is incorrect.");

        String newPw  = ConsoleUtils.readPassword("New Password");
        Validator.validPassword(newPw);
        String confirm = ConsoleUtils.readPassword("Confirm New Password");
        if (!newPw.equals(confirm))
            throw new ValidationException("password", "Passwords do not match.");

        userDAO.updatePassword(AuthManager.getUserId(), newPw);
        ConsoleUtils.printSuccess("Password changed successfully.");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ADMIN: MANAGE USERS
    // ══════════════════════════════════════════════════════════════════════════

    public void adminListUsers() throws DatabaseException {
        CustomLinkedList<String[]> users = userDAO.getAllUsers();
        ConsoleUtils.printHeader("ALL USERS");
        if (users.isEmpty()) { ConsoleUtils.printInfo("No users registered."); return; }

        String[] h = {"ID","Username","Full Name","Email","Phone","Joined","Status"};
        int[]    w = {5, 14, 20, 24, 12, 20, 8};
        ConsoleUtils.printTableHeader(h, w);
        for (int i = 0; i < users.size(); i++) {
            ConsoleUtils.printTableRow(users.get(i), w);
        }
        ConsoleUtils.printTableSeparator(w);
        ConsoleUtils.printInfo("Total users: " + users.size());
    }

    public void adminDeactivateUser() throws DatabaseException {
        ConsoleUtils.printHeader("DEACTIVATE USER");
        adminListUsers();
        int userId = ConsoleUtils.readInt("Enter User ID to deactivate");
        if (ConsoleUtils.readYesNo("Confirm deactivate User ID " + userId)) {
            userDAO.deactivateUser(userId);
            ConsoleUtils.printSuccess("User " + userId + " deactivated.");
        }
    }

    public void adminCreateAdmin() throws ReservationException {
        ConsoleUtils.printHeader("CREATE NEW ADMIN");
        String username = ConsoleUtils.readStringNonEmpty("Admin Username");
        Validator.validUsername(username);
        String fullName = ConsoleUtils.readStringNonEmpty("Full Name");
        String password = ConsoleUtils.readPassword("Password");
        Validator.validPassword(password);
        String[] roles  = {"ADMIN","SUPER_ADMIN","SUPPORT"};
        int rc = ConsoleUtils.showMenu("Admin Role", roles);
        if (rc == 0) return;
        int adminId = userDAO.registerAdmin(username, password, fullName, roles[rc-1]);
        ConsoleUtils.printSuccess("Admin '" + username + "' created with ID: " + adminId);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LOGIN HISTORY
    // ══════════════════════════════════════════════════════════════════════════

    public void showLoginHistory() throws DatabaseException {
        int limit = ConsoleUtils.readIntInRange("How many recent entries", 10, 100);
        CustomLinkedList<String[]> history = userDAO.getLoginHistory(limit);
        ConsoleUtils.printHeader("LOGIN HISTORY (last " + limit + ")");

        String[] h = {"ID","Type","Username","Login Time"};
        int[]    w = {6, 7, 16, 22};
        ConsoleUtils.printTableHeader(h, w);
        for (int i = 0; i < history.size(); i++) {
            ConsoleUtils.printTableRow(history.get(i), w);
        }
        ConsoleUtils.printTableSeparator(w);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FIRST-RUN DEFAULT ADMIN SEED
    // ══════════════════════════════════════════════════════════════════════════

    public void seedDefaultAdmin() {
        try {
            if (!userDAO.adminExists()) {
                userDAO.registerAdmin("admin", "Admin@123", "System Administrator", "SUPER_ADMIN");
                System.out.println("[SYSTEM] Default admin created — username: admin | password: Admin@123");
                System.out.println("[SYSTEM] Please change the default password after first login.");
            }
        } catch (DatabaseException e) {
            System.err.println("[SYSTEM] Could not seed default admin: " + e.getMessage());
        }
    }
}
