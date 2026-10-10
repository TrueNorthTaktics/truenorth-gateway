package ca.truenorthtak.gateway.codec;

import ca.truenorthtak.gateway.model.DefinitionRegistry;
import ca.truenorthtak.gateway.model.Dtg;
import ca.truenorthtak.gateway.model.FieldDef;
import ca.truenorthtak.gateway.model.Message;
import ca.truenorthtak.gateway.model.MessageDef;
import ca.truenorthtak.gateway.model.Position;
import ca.truenorthtak.gateway.model.Precedence;
import ca.truenorthtak.gateway.model.ValidationException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Schema-driven bit-packed encoding for very low bandwidth links. Every field
 * uses only the bits its definition allows (an ENUM of 4 codes takes 2 bits, a
 * position 51 bits at about 1 m resolution). Layout is documented in
 * docs/INTERFACES.md.
 */
public final class CompactBinaryCodec implements Codec {
    static final int LAT_BITS = 25; // 0..18,000,000
    static final int LON_BITS = 26; // 0..36,000,000
    static final int TIME_BITS = 24; // minutes since 2020-01-01
    static final int CHAR_BITS = 6; // DEC SIXBIT

    private final DefinitionRegistry defs;

    public CompactBinaryCodec(DefinitionRegistry defs) {
        this.defs = defs;
    }

    @Override public String id() { return "bin"; }
    @Override public String name() { return "Compact binary (schema-driven bit packing)"; }
    @Override public String mediaType() { return "application/octet-stream"; }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] encode(Message m) {
        m.validate();
        MessageDef d = m.def;
        BitWriter w = new BitWriter();
        w.write(d.code, 8);
        w.write(d.version, 4);
        w.write(m.precedence().ordinal(), 2);
        text(w, m.originator(), Message.MAX_ORIGINATOR);
        w.write(m.serial(), 16);
        w.write(minutes(m.time()), TIME_BITS);
        for (FieldDef f : d.fields) {
            if (!f.required) w.bit(m.get(f.name) != null);
        }
        for (FieldDef f : d.fields) {
            Object v = m.get(f.name);
            if (v == null) continue;
            switch (f.type) {
                case POSITION: {
                    Position p = (Position) v;
                    w.write(p.lat5 + 9000000L, LAT_BITS);
                    w.write(p.lon5 + 18000000L, LON_BITS);
                    break;
                }
                case TEXT: text(w, (String) v, (int) f.max); break;
                case INT: w.write(((Integer) v) - f.min, BitWriter.bitsFor(f.max - f.min)); break;
                case DECIMAL: {
                    long units = ((BigDecimal) v).movePointRight(f.scale).longValueExact();
                    long lo = BigDecimal.valueOf(f.min).movePointRight(f.scale).longValueExact();
                    long hi = BigDecimal.valueOf(f.max).movePointRight(f.scale).longValueExact();
                    w.write(units - lo, BitWriter.bitsFor(hi - lo));
                    break;
                }
                case ENUM: w.write(f.codeIndex((String) v), BitWriter.bitsFor(f.codes.size() - 1)); break;
                case ENUM_SET: {
                    List<String> set = (List<String>) v;
                    for (int k = 0; k < f.codes.size(); k++) w.bit(set.contains(f.codes.get(k).code));
                    break;
                }
                case COUNTS: {
                    Map<String, Integer> c = (Map<String, Integer>) v;
                    int bits = BitWriter.bitsFor(f.max);
                    for (int k = 0; k < f.codes.size(); k++) {
                        Integer n = c.get(f.codes.get(k).code);
                        w.write(n == null ? 0 : n, bits);
                    }
                    break;
                }
                case DTG: w.write(minutes((Long) v), TIME_BITS); break;
                default: throw new IllegalStateException("unhandled type " + f.type);
            }
        }
        return w.toBytes();
    }

    @Override
    public Message decode(byte[] data) {
        BitReader r = new BitReader(data);
        MessageDef d = defs.byCode((int) r.read(8));
        int version = (int) r.read(4);
        if (version != d.version) throw new ValidationException(d.id + ": unsupported version " + version);
        Precedence prec = Precedence.values()[(int) r.read(2)];
        String orig = text(r, Message.MAX_ORIGINATOR);
        int serial = (int) r.read(16);
        long time = fromMinutes(r.read(TIME_BITS));
        Message m = new Message(d, orig, serial, time).setPrecedence(prec);
        Map<String, Boolean> present = new LinkedHashMap<>();
        for (FieldDef f : d.fields) present.put(f.name, f.required || r.bit());
        for (FieldDef f : d.fields) {
            if (!present.get(f.name)) continue;
            switch (f.type) {
                case POSITION:
                    m.set(f.name, new Position((int) (r.read(LAT_BITS) - 9000000L), (int) (r.read(LON_BITS) - 18000000L)));
                    break;
                case TEXT: m.set(f.name, text(r, (int) f.max)); break;
                case INT: m.set(f.name, r.read(BitWriter.bitsFor(f.max - f.min)) + f.min); break;
                case DECIMAL: {
                    long lo = BigDecimal.valueOf(f.min).movePointRight(f.scale).longValueExact();
                    long hi = BigDecimal.valueOf(f.max).movePointRight(f.scale).longValueExact();
                    long units = r.read(BitWriter.bitsFor(hi - lo)) + lo;
                    m.set(f.name, BigDecimal.valueOf(units, f.scale));
                    break;
                }
                case ENUM: {
                    int idx = (int) r.read(BitWriter.bitsFor(f.codes.size() - 1));
                    if (idx >= f.codes.size()) throw new ValidationException(f.name + ": bad code index");
                    m.set(f.name, f.codes.get(idx).code);
                    break;
                }
                case ENUM_SET: {
                    List<String> set = new ArrayList<>();
                    for (int k = 0; k < f.codes.size(); k++) if (r.bit()) set.add(f.codes.get(k).code);
                    m.set(f.name, set);
                    break;
                }
                case COUNTS: {
                    Map<String, Integer> c = new LinkedHashMap<>();
                    int bits = BitWriter.bitsFor(f.max);
                    for (int k = 0; k < f.codes.size(); k++) c.put(f.codes.get(k).code, (int) r.read(bits));
                    m.set(f.name, c);
                    break;
                }
                case DTG: m.set(f.name, fromMinutes(r.read(TIME_BITS))); break;
                default: throw new IllegalStateException("unhandled type " + f.type);
            }
        }
        if (!r.atEnd()) throw new ValidationException("unexpected trailing data");
        return m.validate();
    }

    private static void text(BitWriter w, String s, int max) {
        w.write(s.length(), BitWriter.bitsFor(max));
        for (int k = 0; k < s.length(); k++) w.write(s.charAt(k) - 32, CHAR_BITS);
    }

    private static String text(BitReader r, int max) {
        int len = (int) r.read(BitWriter.bitsFor(max));
        if (len > max) throw new ValidationException("text length " + len + " exceeds " + max);
        StringBuilder b = new StringBuilder(len);
        for (int k = 0; k < len; k++) b.append((char) (r.read(CHAR_BITS) + 32));
        return b.toString();
    }

    private static long minutes(long epochSeconds) {
        return (epochSeconds - Dtg.EPOCH_2020) / 60L;
    }

    private static long fromMinutes(long minutes) {
        return Dtg.EPOCH_2020 + minutes * 60L;
    }
}
