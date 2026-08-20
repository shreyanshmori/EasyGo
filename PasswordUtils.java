package common.utilities;

/**
 * Password hashing and verification using a custom SHA-256 implementation.
 * No external libraries — pure Java logic.
 */
public class PasswordUtils {

    // ── SHA-256 constants (first 32 bits of fractional parts of cube roots) ──
    private static final int[] K = {
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
        0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
        0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
        0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
        0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
        0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
        0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
        0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
        0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
        0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
        0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };

    // ── Hash a plain-text password ────────────────────────────────────────────
    public static String hash(String password) {
        byte[] input   = password.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] hashBytes = sha256(input);
        return bytesToHex(hashBytes);
    }

    // ── Verify plain password against stored hash ─────────────────────────────
    public static boolean verify(String plainPassword, String storedHash) {
        return hash(plainPassword).equals(storedHash);
    }

    // ── SHA-256 core ──────────────────────────────────────────────────────────
    private static byte[] sha256(byte[] input) {
        // initial hash values (first 32 bits of square roots of first 8 primes)
        int h0 = 0x6a09e667, h1 = 0xbb67ae85, h2 = 0x3c6ef372, h3 = 0xa54ff53a;
        int h4 = 0x510e527f, h5 = 0x9b05688c, h6 = 0x1f83d9ab, h7 = 0x5be0cd19;

        // pre-processing: padding
        int origLen  = input.length;
        int bitLen   = origLen * 8;
        int newLen   = origLen + 1;
        while ((newLen % 64) != 56) newLen++;
        newLen += 8;

        byte[] msg = new byte[newLen];
        for (int i = 0; i < origLen; i++) msg[i] = input[i];
        msg[origLen] = (byte) 0x80;
        // append bit length as 64-bit big-endian
        for (int i = 0; i < 8; i++)
            msg[newLen - 1 - i] = (byte) ((bitLen >>> (8 * i)) & 0xFF);

        // process each 512-bit chunk
        for (int chunk = 0; chunk < newLen / 64; chunk++) {
            int[] w = new int[64];
            for (int i = 0; i < 16; i++) {
                int base = chunk * 64 + i * 4;
                w[i] = ((msg[base]   & 0xFF) << 24) | ((msg[base+1] & 0xFF) << 16)
                      |((msg[base+2] & 0xFF) <<  8) |  (msg[base+3] & 0xFF);
            }
            for (int i = 16; i < 64; i++) {
                int s0 = rotr(w[i-15], 7)  ^ rotr(w[i-15], 18) ^ (w[i-15] >>> 3);
                int s1 = rotr(w[i-2],  17) ^ rotr(w[i-2],  19) ^ (w[i-2]  >>> 10);
                w[i] = w[i-16] + s0 + w[i-7] + s1;
            }
            int a=h0, b=h1, c=h2, d=h3, e=h4, f=h5, g=h6, h=h7;
            for (int i = 0; i < 64; i++) {
                int S1  = rotr(e,6) ^ rotr(e,11) ^ rotr(e,25);
                int ch  = (e & f) ^ (~e & g);
                int tmp1 = h + S1 + ch + K[i] + w[i];
                int S0  = rotr(a,2) ^ rotr(a,13) ^ rotr(a,22);
                int maj = (a & b) ^ (a & c) ^ (b & c);
                int tmp2 = S0 + maj;
                h=g; g=f; f=e; e=d+tmp1;
                d=c; c=b; b=a; a=tmp1+tmp2;
            }
            h0+=a; h1+=b; h2+=c; h3+=d; h4+=e; h5+=f; h6+=g; h7+=h;
        }
        // produce digest
        byte[] digest = new byte[32];
        intToBytes(h0, digest, 0);  intToBytes(h1, digest, 4);
        intToBytes(h2, digest, 8);  intToBytes(h3, digest, 12);
        intToBytes(h4, digest, 16); intToBytes(h5, digest, 20);
        intToBytes(h6, digest, 24); intToBytes(h7, digest, 28);
        return digest;
    }

    private static int rotr(int x, int n) { return (x >>> n) | (x << (32 - n)); }

    private static void intToBytes(int val, byte[] arr, int offset) {
        arr[offset]   = (byte) ((val >> 24) & 0xFF);
        arr[offset+1] = (byte) ((val >> 16) & 0xFF);
        arr[offset+2] = (byte) ((val >>  8) & 0xFF);
        arr[offset+3] = (byte)  (val        & 0xFF);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b & 0xFF));
        return sb.toString();
    }
}

