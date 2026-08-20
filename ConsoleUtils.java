package common.utilities;

import java.util.Scanner;

/**
 * Console I/O utilities.
 * Provides: input reading with retry, menus, banners, tables, masking.
 */
public class ConsoleUtils {

    private static final Scanner scanner = new Scanner(System.in);
    private static final int     WIDTH   = 60;

    // ── Banners ───────────────────────────────────────────────────────────────

    public static void printHeader(String title) {
        printLine('=');
        int pad = (WIDTH - title.length() - 2) / 2;
        String left  = repeat(" ", pad);
        String right = repeat(" ", WIDTH - pad - title.length() - 2);
        System.out.println("|" + left + title + right + "|");
        printLine('=');
    }

    public static void printSubHeader(String title) {
        printLine('-');
        System.out.println("  " + title);
        printLine('-');
    }

    public static void printLine(char c) {
        System.out.println(repeat(String.valueOf(c), WIDTH));
    }

    public static void printSuccess(String msg) {
        System.out.println("\n  [SUCCESS] " + msg + "\n");
    }

    public static void printError(String msg) {
        System.out.println("\n  [ERROR] " + msg + "\n");
    }

    public static void printWarning(String msg) {
        System.out.println("\n  [WARNING] " + msg + "\n");
    }

    public static void printInfo(String msg) {
        System.out.println("  >> " + msg);
    }

    // ── Input helpers ─────────────────────────────────────────────────────────

    public static String readString(String prompt) {
        System.out.print("  " + prompt + ": ");
        try {
            return scanner.nextLine().trim();
        } catch (java.util.NoSuchElementException e) {
            System.out.println();
            return "";
        }
    }

    public static String readStringNonEmpty(String prompt) {
        while (true) {
            String val = readString(prompt);
            if (!val.isEmpty()) return val;
            printError("Input cannot be empty. Please try again.");
        }
    }

    public static int readInt(String prompt) {
        while (true) {
            try {
                System.out.print("  " + prompt + ": ");
                String line = scanner.nextLine().trim();
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                printError("Please enter a valid integer.");
            } catch (java.util.NoSuchElementException e) {
                // stdin closed (non-interactive run) — treat as exit/back
                System.out.println();
                return 0;
            }
        }
    }

    public static int readIntInRange(String prompt, int min, int max) {
        while (true) {
            int val = readInt(prompt + " (" + min + "-" + max + ")");
            if (val >= min && val <= max) return val;
            printError("Please enter a value between " + min + " and " + max + ".");
        }
    }

    public static double readDouble(String prompt) {
        while (true) {
            try {
                System.out.print("  " + prompt + ": ");
                String line = scanner.nextLine().trim();
                return Double.parseDouble(line);
            } catch (NumberFormatException e) {
                printError("Please enter a valid number.");
            }
        }
    }

    public static boolean readYesNo(String prompt) {
        while (true) {
            String val = readString(prompt + " (Y/N)").toUpperCase();
            if (val.equals("Y")) return true;
            if (val.equals("N")) return false;
            printError("Please enter Y or N.");
        }
    }

    /**
     * Read password. Uses the same Scanner as other fields so input works in
     * VS Code / Cursor integrated terminals (System.console().readPassword()
     * conflicts with Scanner on System.in and often reads empty).
     */
    public static String readPassword(String prompt) {
        return readStringNonEmpty(prompt);
    }

    // ── Menu ──────────────────────────────────────────────────────────────────

    public static int showMenu(String title, String[] options) {
        printHeader(title);
        for (int i = 0; i < options.length; i++) {
            System.out.println("  [" + (i + 1) + "] " + options[i]);
        }
        System.out.println("  [0] Back / Exit");
        printLine('-');
        return readIntInRange("Choose", 0, options.length);
    }

    // ── Table ─────────────────────────────────────────────────────────────────

    /** Print a simple fixed-width table row. */
    public static void printTableRow(String[] cols, int[] widths) {
        StringBuilder sb = new StringBuilder("  |");
        for (int i = 0; i < cols.length; i++) {
            String cell = (cols[i] == null ? "" : cols[i]);
            sb.append(padRight(cell, widths[i])).append("|");
        }
        System.out.println(sb.toString());
    }

    public static void printTableHeader(String[] headers, int[] widths) {
        printTableSeparator(widths);
        printTableRow(headers, widths);
        printTableSeparator(widths);
    }

    public static void printTableSeparator(int[] widths) {
        StringBuilder sb = new StringBuilder("  +");
        for (int w : widths) sb.append(repeat("-", w)).append("+");
        System.out.println(sb.toString());
    }

    // ── Pause ─────────────────────────────────────────────────────────────────

    public static void pause() {
        System.out.print("\n  Press ENTER to continue...");
        scanner.nextLine();
    }

    public static void clearScreen() {
        // Works in real terminal; ignored in IDEs
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private static String repeat(String s, int n) {
        if (n <= 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }

    private static String padRight(String s, int width) {
        if (s.length() >= width) return s.substring(0, width);
        return s + repeat(" ", width - s.length());
    }

    public static Scanner getScanner() { return scanner; }
}

