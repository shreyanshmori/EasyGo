package common.validation;

import common.exceptions.ValidationException;

/**
 * Central validation utility.
 * All methods throw ValidationException on failure.
 * No regex libraries — manual character-level checks.
 */
public class Validator {

    // ── String / Null ─────────────────────────────────────────────────────────

    public static void notNull(Object value, String field) throws ValidationException {
        if (value == null)
            throw new ValidationException(field, "must not be null");
    }

    public static void notEmpty(String value, String field) throws ValidationException {
        if (value == null || value.trim().isEmpty())
            throw new ValidationException(field, "must not be empty");
    }

    public static void minLength(String value, int min, String field) throws ValidationException {
        notEmpty(value, field);
        if (value.trim().length() < min)
            throw new ValidationException(field, "must be at least " + min + " characters");
    }

    public static void maxLength(String value, int max, String field) throws ValidationException {
        if (value != null && value.length() > max)
            throw new ValidationException(field, "must not exceed " + max + " characters");
    }

    // ── Numeric ───────────────────────────────────────────────────────────────

    public static void positiveInt(int value, String field) throws ValidationException {
        if (value <= 0)
            throw new ValidationException(field, "must be a positive integer, got: " + value);
    }

    public static void positiveDouble(double value, String field) throws ValidationException {
        if (value <= 0.0)
            throw new ValidationException(field, "must be positive, got: " + value);
    }

    public static void rangeInt(int value, int min, int max, String field) throws ValidationException {
        if (value < min || value > max)
            throw new ValidationException(field, "must be between " + min + " and " + max + ", got: " + value);
    }

    // ── Age ───────────────────────────────────────────────────────────────────

    public static void validAge(int age) throws ValidationException {
        rangeInt(age, 1, 120, "age");
    }

    // ── Phone ─────────────────────────────────────────────────────────────────

    public static void validPhone(String phone) throws ValidationException {
        notEmpty(phone, "phone");
        String p = phone.trim();
        if (p.length() != 10)
            throw new ValidationException("phone", "must be 10 digits, got: " + p.length());
        for (int i = 0; i < p.length(); i++) {
            char c = p.charAt(i);
            if (c < '0' || c > '9')
                throw new ValidationException("phone", "must contain digits only");
        }
    }

    // ── Email ─────────────────────────────────────────────────────────────────

    public static void validEmail(String email) throws ValidationException {
        notEmpty(email, "email");
        String e = email.trim();
        int atIdx = -1, dotAfterAt = -1;
        for (int i = 0; i < e.length(); i++) {
            if (e.charAt(i) == '@') {
                if (atIdx != -1)
                    throw new ValidationException("email", "must contain exactly one '@'");
                atIdx = i;
            }
            if (atIdx != -1 && e.charAt(i) == '.') dotAfterAt = i;
        }
        if (atIdx <= 0 || dotAfterAt <= atIdx + 1 || dotAfterAt >= e.length() - 1)
            throw new ValidationException("email", "invalid format: " + e);
    }

    // ── Password ──────────────────────────────────────────────────────────────

    public static void validPassword(String password) throws ValidationException {
        notEmpty(password, "password");
        if (password.length() < 6)
            throw new ValidationException("password", "must be at least 6 characters");
        boolean hasUpper = false, hasDigit = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (c >= 'A' && c <= 'Z') hasUpper = true;
            if (c >= '0' && c <= '9') hasDigit = true;
        }
        if (!hasUpper)
            throw new ValidationException("password", "must contain at least one uppercase letter");
        if (!hasDigit)
            throw new ValidationException("password", "must contain at least one digit");
    }

    // ── Date (format: YYYY-MM-DD) ─────────────────────────────────────────────

    public static void validDateFormat(String date) throws ValidationException {
        notEmpty(date, "date");
        String d = date.trim();
        if (d.length() != 10 || d.charAt(4) != '-' || d.charAt(7) != '-')
            throw new ValidationException("date", "must be YYYY-MM-DD format, got: " + d);
        int year  = parseIntSafe(d.substring(0, 4), "date");
        int month = parseIntSafe(d.substring(5, 7), "date");
        int day   = parseIntSafe(d.substring(8, 10), "date");
        if (year < 2000 || year > 2100)
            throw new ValidationException("date", "year out of range: " + year);
        if (month < 1 || month > 12)
            throw new ValidationException("date", "month out of range: " + month);
        if (day < 1 || day > daysInMonth(year, month))
            throw new ValidationException("date", "day out of range: " + day);
    }

    public static void futureDate(String date) throws ValidationException {
        validDateFormat(date);
        // simple string comparison works for YYYY-MM-DD
        String today = java.time.LocalDate.now().toString();
        if (date.compareTo(today) < 0)
            throw new ValidationException("date", "must be a future date, got: " + date);
    }

    // ── PNR ───────────────────────────────────────────────────────────────────

    public static void validPNR(String pnr) throws ValidationException {
        notEmpty(pnr, "PNR");
        if (pnr.trim().length() < 6 || pnr.trim().length() > 20)
            throw new ValidationException("PNR", "must be 6–20 characters, got: " + pnr.length());
    }

    // ── Gender ────────────────────────────────────────────────────────────────

    public static void validGender(String gender) throws ValidationException {
        notEmpty(gender, "gender");
        String g = gender.trim().toUpperCase();
        if (!g.equals("M") && !g.equals("F") && !g.equals("O"))
            throw new ValidationException("gender", "must be M, F, or O");
    }

    // ── Seat number (alphanumeric) ────────────────────────────────────────────

    public static void validSeatNumber(String seat) throws ValidationException {
        notEmpty(seat, "seat_number");
        for (int i = 0; i < seat.length(); i++) {
            char c = seat.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                      || (c >= '0' && c <= '9');
            if (!ok)
                throw new ValidationException("seat_number", "must be alphanumeric, got: " + seat);
        }
    }

    // ── Username ──────────────────────────────────────────────────────────────

    public static void validUsername(String username) throws ValidationException {
        notEmpty(username, "username");
        String u = username.trim();
        if (u.length() < 4 || u.length() > 20)
            throw new ValidationException("username", "must be 4–20 characters");
        for (int i = 0; i < u.length(); i++) {
            char c = u.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                      || (c >= '0' && c <= '9') || c == '_';
            if (!ok)
                throw new ValidationException("username", "only letters, digits, and '_' allowed");
        }
    }

    // ── Name ──────────────────────────────────────────────────────────────────

    public static void validName(String name, String field) throws ValidationException {
        notEmpty(name, field);
        String n = name.trim();
        if (n.length() < 2)
            throw new ValidationException(field, "must be at least 2 characters");
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == ' ' || c == '.';
            if (!ok)
                throw new ValidationException(field, "must contain only letters and spaces");
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private static int parseIntSafe(String s, String field) throws ValidationException {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new ValidationException(field, "contains non-numeric characters: " + s);
        }
    }

    private static int daysInMonth(int year, int month) {
        int[] days = {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (month == 2 && isLeapYear(year)) return 29;
        return days[month];
    }

    private static boolean isLeapYear(int y) {
        return (y % 4 == 0 && y % 100 != 0) || (y % 400 == 0);
    }
}