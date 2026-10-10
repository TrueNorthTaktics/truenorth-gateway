package ca.truenorthtak.gateway.fec;

/**
 * Systematic Reed-Solomon erasure code over GF(2^8). {@code k} data shards are
 * extended with {@code m} parity shards; the original data can be rebuilt from
 * <em>any</em> {@code k} of the {@code k+m} shards. No retransmission or
 * acknowledgement is needed, which suits one-way and receive-only links.
 *
 * <p>Construction: a (k+m) x k Vandermonde matrix is multiplied by the inverse of
 * its top k x k block, giving an identity top (data passes through unchanged)
 * while every k x k sub-matrix stays invertible.</p>
 */
public final class ReedSolomon {
    public static final int MAX_SHARDS = 255;

    public final int dataShards;
    public final int parityShards;
    private final int[][] matrix; // (k+m) x k

    public ReedSolomon(int dataShards, int parityShards) {
        if (dataShards < 1 || parityShards < 0 || dataShards + parityShards > MAX_SHARDS) {
            throw new IllegalArgumentException("need 1 <= k and k + m <= 255");
        }
        this.dataShards = dataShards;
        this.parityShards = parityShards;
        int n = dataShards + parityShards;
        int[][] vand = new int[n][dataShards];
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < dataShards; c++) vand[r][c] = GF256.pow(r, c);
        }
        int[][] top = new int[dataShards][];
        for (int r = 0; r < dataShards; r++) top[r] = vand[r].clone();
        this.matrix = GF256.multiply(vand, GF256.invert(top));
    }

    public int totalShards() {
        return dataShards + parityShards;
    }

    /** Computes parity shards. {@code shards} holds k data shards followed by m buffers to fill. */
    public void encode(byte[][] shards) {
        check(shards);
        int len = shards[0].length;
        for (int p = 0; p < parityShards; p++) {
            int[] row = matrix[dataShards + p];
            byte[] out = shards[dataShards + p];
            java.util.Arrays.fill(out, (byte) 0);
            for (int c = 0; c < dataShards; c++) {
                int coef = row[c];
                if (coef == 0) continue;
                byte[] in = shards[c];
                for (int i = 0; i < len; i++) out[i] ^= (byte) GF256.mul(coef, in[i] & 0xFF);
            }
        }
    }

    /**
     * Rebuilds missing data shards in place. {@code present[i]} says whether
     * shard i arrived. Returns false if fewer than k shards are present.
     */
    public boolean reconstructData(byte[][] shards, boolean[] present) {
        check(shards);
        int have = 0;
        for (boolean b : present) if (b) have++;
        if (have < dataShards) return false;
        boolean dataComplete = true;
        for (int i = 0; i < dataShards; i++) dataComplete &= present[i];
        if (dataComplete) return true;

        int len = -1;
        for (int i = 0; i < totalShards(); i++) if (present[i]) { len = shards[i].length; break; }
        int[][] sub = new int[dataShards][];
        byte[][] subShards = new byte[dataShards][];
        for (int i = 0, r = 0; i < totalShards() && r < dataShards; i++) {
            if (!present[i]) continue;
            sub[r] = matrix[i].clone();
            subShards[r] = shards[i];
            r++;
        }
        int[][] dec = GF256.invert(sub);
        for (int d = 0; d < dataShards; d++) {
            if (present[d]) continue;
            byte[] out = new byte[len];
            for (int c = 0; c < dataShards; c++) {
                int coef = dec[d][c];
                if (coef == 0) continue;
                byte[] in = subShards[c];
                for (int i = 0; i < len; i++) out[i] ^= (byte) GF256.mul(coef, in[i] & 0xFF);
            }
            shards[d] = out;
            present[d] = true;
        }
        return true;
    }

    private void check(byte[][] shards) {
        if (shards.length != totalShards()) throw new IllegalArgumentException("expected " + totalShards() + " shards");
    }
}
