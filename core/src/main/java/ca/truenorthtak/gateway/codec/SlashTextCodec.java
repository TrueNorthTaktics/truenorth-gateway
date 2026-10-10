package ca.truenorthtak.gateway.codec;

import ca.truenorthtak.gateway.model.DefinitionRegistry;
import ca.truenorthtak.gateway.model.Dtg;
import ca.truenorthtak.gateway.model.FieldDef;
import ca.truenorthtak.gateway.model.FieldType;
import ca.truenorthtak.gateway.model.Message;
import ca.truenorthtak.gateway.model.MessageDef;
import ca.truenorthtak.gateway.model.Position;
import ca.truenorthtak.gateway.model.Precedence;
import ca.truenorthtak.gateway.model.ValidationException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Character-oriented message text in the slash-delimited style of ADatP-3
 * (sets of fields separated by '/', each set terminated by '//'). Output is
 * plain 7-bit ASCII suitable for HF, LF/VLF broadcast or teletype channels.
 *
 * <pre>
 * MSGID/MEDEVAC9/1/BRAVO22/42/IMMEDIATE//
 * DTG/101845ZOCT26//
 * L1/45.90123N/077.31234W//
 * L2/30.125/MEDIC21//
 * L3/A2/C1//
 * </pre>
 *
 * The grammar is a representative character-oriented grammar declared in
 * docs/INTERFACES.md; it is not an official ADatP-3 / APP-11 message.
 */
public final class SlashTextCodec implements Codec {
    private static final String NONE = "-";
    private final DefinitionRegistry defs;

    public SlashTextCodec(DefinitionRegistry defs) {
        this.defs = defs;
    }

    @Override public String id() { return "slash"; }
    @Override public String name() { return "Character-oriented slash text (ADatP-3 style)"; }
    @Override public String mediaType() { return "text/plain; charset=us-ascii"; }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] encode(Message m) {
        m.validate();
        MessageDef d = m.def;
        StringBuilder b = new StringBuilder(256);
        b.append("MSGID/").append(d.id).append('/').append(d.version).append('/').append(m.originator())
         .append('/').append(m.serial()).append('/').append(m.precedence()).append("//\n");
        b.append("DTG/").append(Dtg.dtg(m.time())).append("//\n");
        for (Map.Entry<String, List<FieldDef>> set : d.sets().entrySet()) {
            List<String> parts = new ArrayList<>();
            boolean any = false;
            for (FieldDef f : set.getValue()) {
                Object v = m.get(f.name);
                if (v == null) {
                    parts.add(NONE);
                    if (f.type == FieldType.POSITION) parts.add(NONE); // positions span two fields
                    continue;
                }
                any = true;
                switch (f.type) {
                    case ENUM_SET: parts.addAll((List<String>) v); break;
                    case COUNTS:
                        for (Map.Entry<String, Integer> c : ((Map<String, Integer>) v).entrySet()) {
                            parts.add(c.getKey() + c.getValue());
                        }
                        break;
                    default: parts.add(field(f, v));
                }
            }
            if (!any) continue;
            b.append(set.getKey());
            for (String p : parts) b.append('/').append(p);
            b.append("//\n");
        }
        return b.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private static String field(FieldDef f, Object v) {
        switch (f.type) {
            case POSITION: {
                Position p = (Position) v;
                return String.format(Locale.ROOT, "%08.5f%s/%09.5f%s",
                        Math.abs(p.lat()), p.lat5 < 0 ? "S" : "N", Math.abs(p.lon()), p.lon5 < 0 ? "W" : "E");
            }
            case DECIMAL: return ((BigDecimal) v).toPlainString();
            case DTG: return Dtg.dtg((Long) v);
            default: return v.toString();
        }
    }

    @Override
    public Message decode(byte[] data) {
        String text = new String(data, StandardCharsets.US_ASCII).replace("\r", "").replace("\n", "");
        List<String[]> sets = new ArrayList<>();
        for (String s : text.split("//")) {
            if (!s.trim().isEmpty()) sets.add(s.split("/", -1));
        }
        if (sets.size() < 2 || !"MSGID".equals(sets.get(0)[0]) || !"DTG".equals(sets.get(1)[0])) {
            throw new ValidationException("expected MSGID and DTG sets first");
        }
        String[] id = sets.get(0);
        if (id.length != 6) throw new ValidationException("MSGID set must have 5 fields");
        MessageDef d = defs.byId(id[1]);
        if (Integer.parseInt(id[2]) != d.version) throw new ValidationException(d.id + ": unsupported version " + id[2]);
        Message m = new Message(d, id[3], Integer.parseInt(id[4]), Dtg.parseDtg(sets.get(1)[1]));
        m.setPrecedence(Precedence.valueOf(id[5]));

        for (String[] s : sets.subList(2, sets.size())) {
            List<FieldDef> fs = d.sets().get(s[0]);
            if (fs == null) throw new ValidationException(d.id + ": unknown set '" + s[0] + "'");
            List<String> vals = new ArrayList<>(Arrays.asList(s).subList(1, s.length));
            FieldDef first = fs.get(0);
            if (first.type == FieldType.ENUM_SET) {
                m.set(first.name, vals);
                continue;
            }
            if (first.type == FieldType.COUNTS) {
                m.set(first.name, ValueText.parseCounts(first, vals.toArray(new String[0])));
                continue;
            }
            int k = 0;
            for (FieldDef f : fs) {
                int width = f.type == FieldType.POSITION ? 2 : 1;
                if (k + width > vals.size()) throw new ValidationException(s[0] + ": too few fields");
                String raw = vals.get(k);
                if (!NONE.equals(raw)) {
                    m.set(f.name, parse(f, f.type == FieldType.POSITION ? raw + "/" + vals.get(k + 1) : raw));
                }
                k += width;
            }
            if (k != vals.size()) throw new ValidationException(s[0] + ": too many fields");
        }
        return m.validate();
    }

    private static Object parse(FieldDef f, String s) {
        try {
            switch (f.type) {
                case POSITION: {
                    String[] ll = s.split("/");
                    return new Position(signed(ll[0], 'N', 'S'), signed(ll[1], 'E', 'W'));
                }
                case TEXT: return s;
                case INT: return Long.parseLong(s);
                case DECIMAL: return new BigDecimal(s);
                case ENUM: return s;
                case DTG: return Dtg.parseDtg(s);
                default: throw new ValidationException("unsupported type " + f.type);
            }
        } catch (ValidationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ValidationException(f.name + ": cannot parse '" + s + "'", e);
        }
    }

    private static int signed(String s, char pos, char neg) {
        char h = s.charAt(s.length() - 1);
        int v = ValueText.parse5(s.substring(0, s.length() - 1));
        if (h == pos) return v;
        if (h == neg) return -v;
        throw new ValidationException("bad hemisphere in '" + s + "'");
    }
}
