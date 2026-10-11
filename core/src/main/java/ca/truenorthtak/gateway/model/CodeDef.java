package ca.truenorthtak.gateway.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** One allowed code of an ENUM, ENUM_SET or COUNTS field, with a label per language. */
public final class CodeDef {
    public final String code;
    /** English label (the default). */
    public final String label;
    /** Labels by language tag, for example "en" and "fr". */
    public final Map<String, String> labels;

    public CodeDef(String code, String label) {
        this(code, label == null ? null : Collections.singletonMap("en", label));
    }

    public CodeDef(String code, Map<String, String> labels) {
        this.code = code;
        Map<String, String> m = new LinkedHashMap<>();
        if (labels != null) m.putAll(labels);
        if (!m.containsKey("en")) m.put("en", code);
        this.labels = Collections.unmodifiableMap(m);
        this.label = this.labels.get("en");
    }

    /** Label in the requested language, falling back to English. */
    public String label(String lang) {
        String l = labels.get(lang);
        return l == null ? label : l;
    }
}
