package ca.truenorthtak.gateway.model;

import ca.truenorthtak.gateway.json.Json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Builds {@link MessageDef}s from JSON definition files. */
public final class DefinitionLoader {
    private DefinitionLoader() {}

    public static MessageDef fromJson(String json) {
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) Json.parse(json);
        List<FieldDef> fields = new ArrayList<>();
        for (Object o : list(m, "fields")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> f = (Map<String, Object>) o;
            FieldType type = FieldType.valueOf(str(f, "type", null));
            List<CodeDef> codes = new ArrayList<>();
            if (f.containsKey("codes")) {
                for (Object c : list(f, "codes")) {
                    if (c instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> cm = (Map<String, Object>) c;
                        codes.add(new CodeDef(str(cm, "code", null), str(cm, "label", null)));
                    } else {
                        codes.add(new CodeDef(c.toString(), null));
                    }
                }
            }
            String name = str(f, "name", null);
            fields.add(new FieldDef(
                    name,
                    str(f, "set", name.toUpperCase()),
                    str(f, "xml", Character.toUpperCase(name.charAt(0)) + name.substring(1)),
                    type,
                    bool(f, "required", true),
                    str(f, "label", name),
                    num(f, "min", 0),
                    num(f, "max", defaultMax(type)),
                    (int) num(f, "scale", 0),
                    codes));
        }
        return new MessageDef(
                str(m, "id", null),
                (int) num(m, "version", 1),
                (int) num(m, "code", -1),
                str(m, "title", null),
                str(m, "xmlName", null),
                str(m, "cotType", "a-u-G"),
                str(m, "pointField", null),
                Precedence.valueOf(str(m, "defaultPrecedence", "ROUTINE")),
                fields);
    }

    public static MessageDef fromStream(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        return fromJson(new String(out.toByteArray(), StandardCharsets.UTF_8));
    }

    private static long defaultMax(FieldType t) {
        switch (t) {
            case TEXT: return 64;
            case COUNTS: return 15;
            default: return Integer.MAX_VALUE;
        }
    }

    private static String str(Map<String, Object> m, String k, String dflt) {
        Object v = m.get(k);
        if (v == null) {
            if (dflt == null) throw new ValidationException("definition missing '" + k + "'");
            return dflt;
        }
        return v.toString();
    }

    private static long num(Map<String, Object> m, String k, long dflt) {
        Object v = m.get(k);
        return v == null ? dflt : ((Number) v).longValue();
    }

    private static boolean bool(Map<String, Object> m, String k, boolean dflt) {
        Object v = m.get(k);
        return v == null ? dflt : (Boolean) v;
    }

    private static List<?> list(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (!(v instanceof List)) throw new ValidationException("definition missing list '" + k + "'");
        return (List<?>) v;
    }
}
