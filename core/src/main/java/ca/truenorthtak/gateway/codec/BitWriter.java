package ca.truenorthtak.gateway.codec;

import java.io.ByteArrayOutputStream;

/** Writes unsigned values MSB-first into a byte array. */
final class BitWriter {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private int cur;
    private int used;

    void write(long value, int bits) {
        if (bits < 0 || bits > 63) throw new IllegalArgumentException("bits");
        if (bits < 63 && (value < 0 || value >= (1L << bits))) {
            throw new IllegalArgumentException("value " + value + " does not fit in " + bits + " bits");
        }
        for (int k = bits - 1; k >= 0; k--) {
            cur = (cur << 1) | (int) ((value >>> k) & 1);
            if (++used == 8) {
                out.write(cur);
                cur = 0;
                used = 0;
            }
        }
    }

    void bit(boolean b) {
        write(b ? 1 : 0, 1);
    }

    byte[] toBytes() {
        if (used > 0) {
            out.write(cur << (8 - used));
            cur = 0;
            used = 0;
        }
        return out.toByteArray();
    }

    /** Number of bits needed to hold values 0..range. */
    static int bitsFor(long range) {
        return range <= 0 ? 0 : 64 - Long.numberOfLeadingZeros(range);
    }
}
