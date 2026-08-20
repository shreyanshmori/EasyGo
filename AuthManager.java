package common.utilities;

public class AuthManager {

    private static int     loggedInUserId   = -1;
    private static String  loggedInUsername = null;
    private static boolean isAdmin          = false;

    // ── Session management ────────────────────────────────────────────────────

    public static void setSession(int userId, String username, boolean admin) {
        loggedInUserId   = userId;
        loggedInUsername = username;
        isAdmin          = admin;
    }

    public static void clearSession() {
        loggedInUserId   = -1;
        loggedInUsername = null;
        isAdmin          = false;
    }

    public static boolean isLoggedIn()   { return loggedInUserId != -1; }
    public static boolean isAdmin()      { return isAdmin; }
    public static int     getUserId()    { return loggedInUserId; }
    public static String  getUsername()  { return loggedInUsername; }

    // ── Generate unique booking reference ─────────────────────────────────────

    public static String generateBookingRef(String prefix) {
        long ts     = System.currentTimeMillis();
        int  random = (int)(Math.random() * 9000) + 1000;
        return prefix + ts % 1000000L + random;
    }

    // ── Generate PNR (Railway) ────────────────────────────────────────────────

    public static String generatePNR() {
        long ts = System.currentTimeMillis();
        int  r  = (int)(Math.random() * 900000) + 100000;
        return "PNR" + (ts % 10000000L) + r;
    }

    // ── Generate transaction ID ───────────────────────────────────────────────

    public static String generateTxnId() {
        long ts = System.currentTimeMillis();
        int  r  = (int)(Math.random() * 900000) + 100000;
        return "TXN" + ts + r;
    }
}