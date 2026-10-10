package ca.truenorthtak.gateway.fec;

import ca.truenorthtak.gateway.model.Precedence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Splits an encoded message into link-sized fragments and adds Reed-Solomon
 * parity fragments. Each fragment carries a 7-byte header:
 *
 * <pre>
 * tag (u16) | k data shards (u8) | m parity shards (u8) | index (u8) | payload length (u16)
 * </pre>
 */
public final class Fragmenter {
    public static final int HEADER = 7;

    private Fragmenter() {}

    public static List<byte[]> split(byte[] payload, int tag, int maxFragmentBytes, Precedence precedence,
                                     RedundancyPolicy policy) {
        int room = maxFragmentBytes - HEADER;
        if (room < 1) throw new IllegalArgumentException("fragment size too small");
        if (payload.length > 0xFFFF) throw new IllegalArgumentException("payload too large");
        int k = Math.max(1, (payload.length + room - 1) / room);
        int shard = Math.max(1, (payload.length + k - 1) / k);
        int m = policy.parityFor(precedence, k);
        if (k + m > ReedSolomon.MAX_SHARDS) throw new IllegalArgumentException("too many shards; raise fragment size");
        byte[][] shards = new byte[k + m][shard];
        for (int i = 0; i < k; i++) {
            int from = i * shard;
            int to = Math.min(payload.length, from + shard);
            if (from < to) System.arraycopy(payload, from, shards[i], 0, to - from);
        }
        if (m > 0) new ReedSolomon(k, m).encode(shards);
        List<byte[]> out = new ArrayList<>(k + m);
        for (int i = 0; i < k + m; i++) {
            byte[] f = new byte[HEADER + shard];
            f[0] = (byte) (tag >>> 8);
            f[1] = (byte) tag;
            f[2] = (byte) k;
            f[3] = (byte) m;
            f[4] = (byte) i;
            f[5] = (byte) (payload.length >>> 8);
            f[6] = (byte) payload.length;
            System.arraycopy(shards[i], 0, f, HEADER, shard);
            out.add(f);
        }
        return out;
    }

    /** Collects fragments for one message and rebuilds it once enough have arrived. */
    public static final class Reassembler {
        private int tag = -1, k, m, length, shard;
        private byte[][] shards;
        private boolean[] present;
        private int received;
        private byte[] done;

        /** Adds a fragment; returns the payload when it can first be rebuilt, else null. */
        public byte[] accept(byte[] f) {
            if (done != null || f.length < HEADER + 1) return null;
            int t = ((f[0] & 0xFF) << 8) | (f[1] & 0xFF);
            int fk = f[2] & 0xFF, fm = f[3] & 0xFF, idx = f[4] & 0xFF;
            int len = ((f[5] & 0xFF) << 8) | (f[6] & 0xFF);
            if (tag < 0) {
                tag = t; k = fk; m = fm; length = len; shard = f.length - HEADER;
                shards = new byte[k + m][];
                present = new boolean[k + m];
            }
            if (t != tag || fk != k || fm != m || len != length || f.length - HEADER != shard || idx >= k + m) return null;
            if (present[idx]) return null;
            shards[idx] = Arrays.copyOfRange(f, HEADER, f.length);
            present[idx] = true;
            received++;
            if (received < k) return null;
            if (m > 0) {
                for (int i = 0; i < k + m; i++) if (shards[i] == null) shards[i] = new byte[shard];
                if (!new ReedSolomon(k, m).reconstructData(shards, present.clone())) return null;
                // reconstructData fills missing data shards in place
            }
            byte[] out = new byte[length];
            for (int i = 0; i < k; i++) {
                int from = i * shard;
                int n = Math.min(shard, length - from);
                if (n > 0) System.arraycopy(shards[i], 0, out, from, n);
            }
            done = out;
            return out;
        }

        public boolean isComplete() {
            return done != null;
        }
    }
}
