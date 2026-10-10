package ca.truenorthtak.gateway.codec;

import ca.truenorthtak.gateway.model.ValidationException;

/** Reads values written by {@link BitWriter}. */
final class BitReader {
    private final byte[] data;
    private int pos; // bit position

    BitReader(byte[] data) {
        this.data = data;
    }

    long read(int bits) {
        if (pos + bits > data.length * 8) throw new ValidationException("binary message truncated");
        long v = 0;
        for (int k = 0; k < bits; k++) {
            int b = (data[pos >>> 3] >>> (7 - (pos & 7))) & 1;
            v = (v << 1) | b;
            pos++;
        }
        return v;
    }

    boolean bit() {
        return read(1) == 1;
    }

    /** True if only zero padding remains. */
    boolean atEnd() {
        return data.length * 8 - pos < 8;
    }
}
