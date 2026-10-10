package ca.truenorthtak.gateway.codec;

import ca.truenorthtak.gateway.model.Dtg;
import ca.truenorthtak.gateway.model.FieldDef;
import ca.truenorthtak.gateway.model.Position;
import ca.truenorthtak.gateway.model.ValidationException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Single-string representation of field values, used by the CoT codec. */
final class ValueText {
    private ValueText() {}

    @SuppressWarnings("unchecked")
    static String toText(FieldDef f, Object v) {
        switch (f.type) {
            case POSITION: {
                Position p = (Position) v;
                return String.format(Locale.ROOT, "%.5f,%.5f", p.lat(), p.lon());
            }
            case DECIMAL: return ((BigDecimal) v).toPlainString();
            case ENUM_SET: return String.join(" ", (List<String>) v);
            case COUNTS: {
                StringBuilder b = new StringBuilder();
                for (Map.Entry<String, Integer> e : ((Map<String, Integer>) v).entrySet()) {
                    if (b.length() > 0) b.append(' ');
                    b.append(e.getKey()).append(e.getValue());
                }
                return b.toString();
            }
            case DTG: return Dtg.iso((Long) v);
            default: return v.toString();
        }
    }

    static Object fromText(FieldDef f, String s) {
        try {
            switch (f.type) {
                case POSITION: {
                    String[] ll = s.split(",");
                    return new Position(parse5(ll[0]), parse5(ll[1]));
                }
                case TEXT: return s;
                case INT: return Long.parseLong(s);
                case DECIMAL: return new BigDecimal(s);
                case ENUM: return s;
                case ENUM_SET: {
                    List<String> out = new ArrayList<>();
                    for (String c : s.trim().split("\\s+")) if (!c.isEmpty()) out.add(c);
                    return out;
                }
                case COUNTS: return parseCounts(f, s.trim().split("\\s+"));
                case DTG: return Dtg.parseIso(s);
                default: throw new ValidationException("unsupported type " + f.type);
            }
        } catch (RuntimeException e) {
            if (e instanceof ValidationException) throw e;
            throw new ValidationException(f.name + ": cannot parse '" + s + "'", e);
        }
    }

    /** Parses tokens such as "A2" (code followed by count). */
    static Map<String, Integer> parseCounts(FieldDef f, String[] tokens) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (String t : tokens) {
            if (t.isEmpty()) continue;
            String code = null;
            for (int k = 0; k < f.codes.size(); k++) {
                String c = f.codes.get(k).code;
                if (t.startsWith(c) && t.length() > c.length() && Character.isDigit(t.charAt(c.length()))
                        && (code == null || c.length() > code.length())) {
                    code = c;
                }
            }
            if (code == null) throw new ValidationException(f.name + ": bad count token '" + t + "'");
            m.put(code, Integer.parseInt(t.substring(code.length())));
        }
        return m;
    }

    /** Parses a decimal degree string to 1e-5 degree units without floating point error. */
    static int parse5(String s) {
        return new BigDecimal(s.trim()).movePointRight(5).intValueExact();
    }
}
