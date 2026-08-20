package common.utilities;

public class DateUtils {

    /**
     * Returns today's date as YYYY-MM-DD string.
     * Uses java.time minimally (just for current date).
     */
    public static String today() {
        return java.time.LocalDate.now().toString();
    }

    /** Add n days to a YYYY-MM-DD date string. Returns new date string. */
    public static String addDays(String date, int days) {
        int[] parts  = splitDate(date);
        int year = parts[0], month = parts[1], day = parts[2];
        day += days;
        while (day > daysInMonth(year, month)) {
            day -= daysInMonth(year, month);
            month++;
            if (month > 12) { month = 1; year++; }
        }
        while (day < 1) {
            month--;
            if (month < 1) { month = 12; year--; }
            day += daysInMonth(year, month);
        }
        return format(year, month, day);
    }

    /** Subtract n days from a date string. */
    public static String subtractDays(String date, int days) {
        return addDays(date, -days);
    }

    /** Compare two YYYY-MM-DD strings. Returns -1, 0, or 1. */
    public static int compare(String d1, String d2) {
        return d1.compareTo(d2);
    }

    /** Returns true if d1 is before d2. */
    public static boolean isBefore(String d1, String d2) {
        return compare(d1, d2) < 0;
    }

    /** Returns true if d1 is after d2. */
    public static boolean isAfter(String d1, String d2) {
        return compare(d1, d2) > 0;
    }

    /** Returns day-of-week name for a YYYY-MM-DD date (Tomohiko Sakamoto algorithm). */
    public static String dayOfWeek(String date) {
        int[] parts = splitDate(date);
        int y = parts[0], m = parts[1], d = parts[2];
        int[] t = {0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4};
        if (m < 3) y--;
        int day = (y + y/4 - y/100 + y/400 + t[m-1] + d) % 7;
        String[] names = {"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};
        return names[day];
    }

    /** Format date as "DD MMM YYYY" (e.g. "05 Jun 2025"). */
    public static String friendly(String date) {
        int[] parts = splitDate(date);
        String[] months = {"Jan","Feb","Mar","Apr","May","Jun",
                           "Jul","Aug","Sep","Oct","Nov","Dec"};
        return pad2(parts[2]) + " " + months[parts[1]-1] + " " + parts[0];
    }

    /** Number of days between two dates (d2 - d1). */
    public static int daysBetween(String d1, String d2) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(
            java.time.LocalDate.parse(d1),
            java.time.LocalDate.parse(d2)
        );
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static int[] splitDate(String date) {
        // YYYY-MM-DD
        int year  = Integer.parseInt(date.substring(0, 4));
        int month = Integer.parseInt(date.substring(5, 7));
        int day   = Integer.parseInt(date.substring(8, 10));
        return new int[]{year, month, day};
    }

    private static String format(int y, int m, int d) {
        return y + "-" + pad2(m) + "-" + pad2(d);
    }

    private static String pad2(int n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }

    private static int daysInMonth(int y, int m) {
        int[] days = {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (m == 2 && ((y%4==0 && y%100!=0) || y%400==0)) return 29;
        return days[m];
    }
}