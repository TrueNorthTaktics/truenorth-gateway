package ca.truenorthtak.gateway.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A message type, loaded from a definition file. Every codec is driven by this
 * definition, so adding a new report type is a data change, not a code change.
 */
public final class MessageDef {
    public final String id;
    public final int version;
    /** One-byte identifier used by the compact binary codec. */
    public final int code;
    public final String title;
    /** Root element name in the XML exchange format. */
    public final String xmlName;
    /** Cursor-on-Target event type. */
    public final String cotType;
    /** Name of the POSITION field used as the CoT event point. */
    public final String pointField;
    public final Precedence defaultPrecedence;
    public final List<FieldDef> fields;
    private final Map<String, FieldDef> byName = new LinkedHashMap<>();
    private final Map<String, List<FieldDef>> bySet = new LinkedHashMap<>();

    public MessageDef(String id, int version, int code, String title, String xmlName, String cotType,
                      String pointField, Precedence defaultPrecedence, List<FieldDef> fields) {
        if (code < 0 || code > 255) throw new ValidationException(id + ": code must be 0..255");
        if (version < 0 || version > 15) throw new ValidationException(id + ": version must be 0..15");
        this.id = id;
        this.version = version;
        this.code = code;
        this.title = title;
        this.xmlName = xmlName;
        this.cotType = cotType;
        this.pointField = pointField;
        this.defaultPrecedence = defaultPrecedence;
        this.fields = Collections.unmodifiableList(new ArrayList<>(fields));
        for (FieldDef f : fields) {
            if (byName.put(f.name, f) != null) throw new ValidationException(id + ": duplicate field " + f.name);
            bySet.computeIfAbsent(f.set, k -> new ArrayList<>()).add(f);
        }
        for (Map.Entry<String, List<FieldDef>> e : bySet.entrySet()) {
            for (FieldDef f : e.getValue()) {
                boolean multi = f.type == FieldType.COUNTS || f.type == FieldType.ENUM_SET;
                if (multi && e.getValue().size() > 1) {
                    throw new ValidationException(id + ": " + f.type + " field " + f.name + " must be alone in set " + e.getKey());
                }
            }
        }
        if (pointField != null) {
            FieldDef p = byName.get(pointField);
            if (p == null || p.type != FieldType.POSITION || !p.required) {
                throw new ValidationException(id + ": pointField must name a required POSITION field");
            }
        }
    }

    public FieldDef field(String name) {
        FieldDef f = byName.get(name);
        if (f == null) throw new ValidationException(id + ": no field '" + name + "'");
        return f;
    }

    public boolean hasField(String name) {
        return byName.containsKey(name);
    }

    /** Fields grouped by character-oriented set, in definition order. */
    public Map<String, List<FieldDef>> sets() {
        return Collections.unmodifiableMap(bySet);
    }
}
