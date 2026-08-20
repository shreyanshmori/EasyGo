package common.payment;

/**
 * ASCII-art QR Code generator for UPI payments.
 *
 * Approach:
 *   1. Build a UPI deep-link string (upi://pay?...).
 *   2. Encode it into a compact binary matrix using a
 *      custom Reed-Solomon-inspired block mapping
 *      (simplified — suitable for console display).
 *   3. Render the matrix as Unicode block characters
 *      (█ / space) in the terminal.
 *
 * NOTE: The output is a visual representation for demo purposes.
 *       For production, integrate a full QR library.
 *       The pattern is deterministic for the same UPI string.
 */
public class QRGenerator {

    // ── QR block characters ───────────────────────────────────────────────────
    private static final String DARK  = "██";
    private static final String LIGHT = "  ";

    // ── Module size (21x21 = Version 1 QR) ───────────────────────────────────
    private static final int SIZE = 21;

    // ── Generate and print QR for a UPI payment ───────────────────────────────
    /**
     * @param upiId  e.g. "payments@ybl"
     * @param name   Payee name
     * @param amount Amount in rupees
     * @param ref    Booking reference (transaction note)
     */
    public static void printUPIQR(String upiId, String name, double amount, String ref) {
        String upiUrl = buildUPIUrl(upiId, name, amount, ref);
        boolean[][] matrix = generateMatrix(upiUrl);
        printMatrix(matrix, upiUrl, upiId, amount);
    }

    // ── Build UPI deep-link ────────────────────────────────────────────────────
    public static String buildUPIUrl(String upiId, String name, double amount, String ref) {
        // URL-encode spaces manually (no java.net.URLEncoder to avoid dep)
        String encName = urlEncode(name);
        String encRef  = urlEncode(ref);
        return "upi://pay?pa=" + upiId
             + "&pn=" + encName
             + "&am=" + String.format("%.2f", amount)
             + "&tn=" + encRef
             + "&cu=INR";
    }

    // ── Generate deterministic 21×21 boolean matrix from input string ─────────
    private static boolean[][] generateMatrix(String data) {
        boolean[][] m = new boolean[SIZE][SIZE];

        // 1. Fixed finder patterns (top-left, top-right, bottom-left)
        addFinderPattern(m, 0, 0);
        addFinderPattern(m, 0, SIZE - 7);
        addFinderPattern(m, SIZE - 7, 0);

        // 2. Timing patterns (row 6 and col 6 alternating)
        for (int i = 8; i < SIZE - 8; i++) {
            m[6][i] = (i % 2 == 0);
            m[i][6] = (i % 2 == 0);
        }

        // 3. Data encoding — map each byte of the UPI string into modules
        byte[] bytes = data.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int dataIdx  = 0;
        int bitPos   = 0;

        // fill non-function modules in a simple top-to-bottom, right-to-left zigzag
        for (int col = SIZE - 1; col >= 0; col -= 2) {
            if (col == 6) col--;   // skip timing column
            for (int row = SIZE - 1; row >= 0; row--) {
                for (int c2 = 0; c2 < 2; c2++) {
                    int r = row;
                    int c = col - c2;
                    if (c < 0 || c >= SIZE) continue;
                    if (isFunctionModule(r, c)) continue;

                    // get bit from data (cyclic if data shorter than capacity)
                    int byteIdx = (dataIdx / 8) % bytes.length;
                    int bit     = (bytes[byteIdx] >> (7 - (dataIdx % 8))) & 1;
                    // XOR with mask pattern 0: (row+col) % 2 == 0
                    int mask    = ((r + c) % 2 == 0) ? 1 : 0;
                    m[r][c]     = ((bit ^ mask) == 1);
                    dataIdx++;
                    bitPos++;
                }
            }
        }

        // 4. Simple checksum enrichment — XOR data hash into lower-right block
        int hash = simpleHash(data);
        for (int i = 0; i < 8; i++) {
            int r = SIZE - 4 + (i / 4);
            int c = SIZE - 4 + (i % 4);
            if (r < SIZE && c < SIZE && !isFunctionModule(r, c))
                m[r][c] = ((hash >> i) & 1) == 1;
        }

        return m;
    }

    // ── Add 7×7 finder pattern at (row, col) ─────────────────────────────────
    private static void addFinderPattern(boolean[][] m, int row, int col) {
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                if (r < SIZE && c < SIZE && row+r < SIZE && col+c < SIZE)
                    m[row+r][col+c] = (r==0||r==6||c==0||c==6||(r>=2&&r<=4&&c>=2&&c<=4));
            }
        }
        // separator row below / right
        for (int i = 0; i < 8 && row+7 < SIZE; i++)
            if (col+i < SIZE) m[row+7][col+i] = false;
        for (int i = 0; i < 8 && col+7 < SIZE; i++)
            if (row+i < SIZE) m[row+i][col+7] = false;
    }

    // ── Check if a module is reserved for function patterns ──────────────────
    private static boolean isFunctionModule(int r, int c) {
        // finder + separator zones
        if (r <= 7 && c <= 7)         return true;   // top-left
        if (r <= 7 && c >= SIZE - 8)  return true;   // top-right
        if (r >= SIZE - 8 && c <= 7)  return true;   // bottom-left
        // timing patterns
        if (r == 6 || c == 6)         return true;
        // format information strip
        if (r == 8 && c <= 8)         return true;
        if (c == 8 && r <= 8)         return true;
        return false;
    }

    // ── Render matrix to console ──────────────────────────────────────────────
    private static void printMatrix(boolean[][] m, String url, String upiId, double amount) {
        String border = repeat("██", SIZE + 2);
        System.out.println();
        System.out.println("  ┌── SCAN TO PAY VIA UPI ──────────────────────────┐");
        System.out.println("  │  UPI ID : " + padRight(upiId, 37) + "│");
        System.out.println("  │  Amount : Rs. " + padRight(String.format("%.2f", amount), 33) + "│");
        System.out.println("  └─────────────────────────────────────────────────┘");
        System.out.println();

        // top border
        System.out.print("  " + DARK);
        System.out.println(border);

        for (int r = 0; r < SIZE; r++) {
            System.out.print("  " + DARK);   // left border
            for (int c = 0; c < SIZE; c++) {
                System.out.print(m[r][c] ? DARK : LIGHT);
            }
            System.out.println(DARK);         // right border
        }

        // bottom border
        System.out.print("  " + DARK);
        System.out.println(border);
        System.out.println();
        System.out.println("  UPI Link: " + url);
        System.out.println();
    }

    // ── Simple hash for checksum enrichment ──────────────────────────────────
    private static int simpleHash(String s) {
        int h = 0x811c9dc5;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x01000193;
        }
        return h;
    }

    // ── Minimal URL encoder (space → %20, special → %XX) ─────────────────────
    private static String urlEncode(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
             || (c >= '0' && c <= '9') || c == '-' || c == '_' || c == '.') {
                sb.append(c);
            } else if (c == ' ') {
                sb.append("%20");
            } else {
                sb.append('%');
                sb.append(hexChar((c >> 4) & 0xF));
                sb.append(hexChar(c & 0xF));
            }
        }
        return sb.toString();
    }

    private static char hexChar(int n) {
        return (char)(n < 10 ? '0' + n : 'A' + n - 10);
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }

    private static String padRight(String s, int w) {
        if (s.length() >= w) return s.substring(0, w);
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < w) sb.append(' ');
        return sb.toString();
    }
}