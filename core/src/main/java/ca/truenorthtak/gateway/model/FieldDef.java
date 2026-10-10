package ca.truenorthtak.gateway.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Definition of one field in a message: its type, limits and how each format names it. */
public final class FieldDef {
    public final String name;
    /** Character-oriented set identifier (fields sharing a set are written in one set). */
    public final String set;
    /** XML element name, following NIEM-style representation-term naming. */
    public final String xml;
    public final FieldType type;
    public final boolean required;
    public final String label;
    /** TEXT: maximum length. INT/DECIMAL: maximum value. COUNTS: maximum count per code. */
    public final long max;
    /** INT/DECIMAL: minimum value. */
    public final long min;
    /** DECIMAL: fraction digits. */
    public final int scale;
    public final List<CodeDef> codes;

    public FieldDef(String name, String set, String xml, FieldType type, boolean required, String label,
                    long min, long max, int scale, List<CodeDef> codes) {
        this.name = name;
        this.set = set;
        this.xml = xml;
        this.type = type;
        this.required = required;
        this.label = label == null ? name : label;
        this.min = min;
        this.max = max;
        this.scale = scale;
        this.codes = codes == null ? Collections.<CodeDef>emptyList() : Collections.unmodifiableList(new ArrayList<>(codes));
    }

    public int codeIndex(String code) {
        for (int k = 0; k < codes.size(); k++) {
            if (codes.get(k).code.equals(code)) return k;
        }
        return -1;
    }

    /** Converts a caller-supplied value to the canonical form for this field, validating it. */
    @SuppressWarnings("unchecked")
    public Object normalize(Object v) {
        if (v == null) return null;
        try {
            switch (type) {
                case POSITION:
                    if (!(v instanceof Position)) throw bad("expected Position");
                    return v;
                case TEXT: {
                    String t = TextRules.normalize(v.toString());
                    TextRules.check(name, t, (int) max);
                    // "-" is the "no value" marker in character-oriented sets.
                    return t.isEmpty() || t.equals("-") ? null : t;
                }
                case INT: {
                    long n = ((Number) v).longValue();
                    if (n < min || n > max) throw bad("value " + n + " outside " + min + ".." + max);
                    return (int) n;
                }
                case DECIMAL: {
                    BigDecimal d = (v instanceof BigDecimal) ? (BigDecimal) v : new BigDecimal(v.toString());
                    BigDecimal scaled;
                    try {
                        scaled = d.setScale(scale, RoundingMode.UNNECESSARY);
                    } catch (ArithmeticException e) {
                        throw bad("more than " + scale + " decimal places");
                    }
                    if (scaled.compareTo(BigDecimal.valueOf(min)) < 0 || scaled.compareTo(BigDecimal.valueOf(max)) > 0) {
                        throw bad("value outside " + min + ".." + max);
                    }
                    return scaled;
                }
                case ENUM: {
                    String c = v.toString();
                    if (codeIndex(c) < 0) throw bad("unknown code '" + c + "'");
                    return c;
                }
                case ENUM_SET: {
                    Collection<?> in = (Collection<?>) v;
                    List<String> out = new ArrayList<>();
                    for (CodeDef cd : codes) {
                        if (in.contains(cd.code)) out.add(cd.code);
                    }
                    for (Object o : in) {
                        if (codeIndex(o.toString()) < 0) throw bad("unknown code '" + o + "'");
                    }
                    return out.isEmpty() ? null : Collections.unmodifiableList(out);
                }
                case COUNTS: {
                    Map<String, ?> in = (Map<String, ?>) v;
                    for (String k : in.keySet()) {
                        if (codeIndex(k) < 0) throw bad("unknown code '" + k + "'");
                    }
                    Map<String, Integer> out = new LinkedHashMap<>();
                    for (CodeDef cd : codes) {
                        Object n = in.get(cd.code);
                        if (n == null) continue;
                        int c = ((Number) n).intValue();
                        if (c < 0 || c > max) throw bad("count for " + cd.code + " outside 0.." + max);
                        if (c > 0) out.put(cd.code, c);
                    }
                    return out.isEmpty() ? null : Collections.unmodifiableMap(out);
                }
                case DTG: {
                    long t = ((Number) v).longValue();
                    return Dtg.normalize(t);
                }
                default:
                    throw bad("unsupported type");
            }
        } catch (ClassCastException e) {
            throw bad("wrong value type " + v.getClass().getSimpleName());
        }
    }

    private ValidationException bad(String why) {
        return new ValidationException(name + ": " + why);
    }
}
