package ca.truenorthtak.gateway.fec;

/** Arithmetic in GF(2^8) with the primitive polynomial x^8+x^4+x^3+x^2+1 (0x11D). */
final class GF256 {
    private GF256() {}

    static final int[] EXP = new int[512];
    static final int[] LOG = new int[256];

    static {
        int x = 1;
        for (int i = 0; i < 255; i++) {
            EXP[i] = x;
            LOG[x] = i;
            x <<= 1;
            if ((x & 0x100) != 0) x ^= 0x11D;
        }
        for (int i = 255; i < 512; i++) EXP[i] = EXP[i - 255];
    }

    static int mul(int a, int b) {
        if (a == 0 || b == 0) return 0;
        return EXP[LOG[a] + LOG[b]];
    }

    static int div(int a, int b) {
        if (b == 0) throw new ArithmeticException("divide by zero in GF(256)");
        if (a == 0) return 0;
        return EXP[LOG[a] + 255 - LOG[b]];
    }

    static int pow(int a, int n) {
        if (n == 0) return 1;
        if (a == 0) return 0;
        return EXP[(LOG[a] * n) % 255];
    }

    /** Inverts a square matrix in place using Gauss-Jordan elimination. */
    static int[][] invert(int[][] m) {
        int n = m.length;
        int[][] a = new int[n][2 * n];
        for (int r = 0; r < n; r++) {
            System.arraycopy(m[r], 0, a[r], 0, n);
            a[r][n + r] = 1;
        }
        for (int c = 0; c < n; c++) {
            int p = c;
            while (p < n && a[p][c] == 0) p++;
            if (p == n) throw new ArithmeticException("singular matrix");
            int[] t = a[p]; a[p] = a[c]; a[c] = t;
            int inv = div(1, a[c][c]);
            for (int k = 0; k < 2 * n; k++) a[c][k] = mul(a[c][k], inv);
            for (int r = 0; r < n; r++) {
                if (r == c || a[r][c] == 0) continue;
                int f = a[r][c];
                for (int k = 0; k < 2 * n; k++) a[r][k] ^= mul(f, a[c][k]);
            }
        }
        int[][] out = new int[n][n];
        for (int r = 0; r < n; r++) System.arraycopy(a[r], n, out[r], 0, n);
        return out;
    }

    static int[][] multiply(int[][] x, int[][] y) {
        int rows = x.length, inner = y.length, cols = y[0].length;
        int[][] out = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int s = 0;
                for (int k = 0; k < inner; k++) s ^= mul(x[r][k], y[k][c]);
                out[r][c] = s;
            }
        }
        return out;
    }
}
