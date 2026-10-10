package ca.truenorthtak.gateway.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** A message instance: a header plus field values, always held in canonical form. */
public final class Message {
    public static final int MAX_ORIGINATOR = 12;

    public final MessageDef def;
    private String originator;
    private int serial;
    private long time;
    private Precedence precedence;
    private final Map<String, Object> values = new LinkedHashMap<>();

    public Message(MessageDef def, String originator, int serial, long time) {
        this.def = def;
        setOriginator(originator);
        setSerial(serial);
        setTime(time);
        this.precedence = def.defaultPrecedence;
    }

    public Message set(String field, Object value) {
        FieldDef f = def.field(field);
        Object v = f.normalize(value);
        if (v == null) values.remove(field); else values.put(field, v);
        return this;
    }

    public Object get(String field) {
        return values.get(field);
    }

    /** Field values in definition order. */
    public Map<String, Object> values() {
        Map<String, Object> ordered = new LinkedHashMap<>();
        for (FieldDef f : def.fields) {
            if (values.containsKey(f.name)) ordered.put(f.name, values.get(f.name));
        }
        return Collections.unmodifiableMap(ordered);
    }

    public Message setOriginator(String o) {
        String n = TextRules.normalize(o);
        if (n == null || n.isEmpty()) throw new ValidationException("originator required");
        TextRules.check("originator", n, MAX_ORIGINATOR);
        this.originator = n;
        return this;
    }

    public Message setSerial(int s) {
        if (s < 0 || s > 65535) throw new ValidationException("serial must be 0..65535");
        this.serial = s;
        return this;
    }

    public Message setTime(long epochSeconds) {
        this.time = Dtg.normalize(epochSeconds);
        return this;
    }

    public Message setPrecedence(Precedence p) {
        this.precedence = Objects.requireNonNull(p);
        return this;
    }

    public String originator() { return originator; }
    public int serial() { return serial; }
    public long time() { return time; }
    public Precedence precedence() { return precedence; }

    /** Stable identifier used as the CoT uid. */
    public String uid() {
        return "TNG." + def.id + "." + originator.replace(' ', '_') + "." + serial;
    }

    /** Throws if a required field is missing. */
    public Message validate() {
        for (FieldDef f : def.fields) {
            if (f.required && !values.containsKey(f.name)) {
                throw new ValidationException(def.id + ": required field '" + f.name + "' missing");
            }
        }
        return this;
    }

    @Override public boolean equals(Object o) {
        if (!(o instanceof Message)) return false;
        Message m = (Message) o;
        return m.def.id.equals(def.id) && m.originator.equals(originator) && m.serial == serial
                && m.time == time && m.precedence == precedence && m.values().equals(values());
    }

    @Override public int hashCode() {
        return Objects.hash(def.id, originator, serial, time, precedence, values());
    }

    @Override public String toString() {
        return def.id + "[" + originator + "#" + serial + " " + Dtg.iso(time) + " " + precedence + "] " + values();
    }
}
