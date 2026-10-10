package ca.truenorthtak.gateway.codec;

import ca.truenorthtak.gateway.model.CodeDef;
import ca.truenorthtak.gateway.model.DefinitionRegistry;
import ca.truenorthtak.gateway.model.Dtg;
import ca.truenorthtak.gateway.model.FieldDef;
import ca.truenorthtak.gateway.model.Message;
import ca.truenorthtak.gateway.model.MessageDef;
import ca.truenorthtak.gateway.model.Position;
import ca.truenorthtak.gateway.model.Precedence;
import ca.truenorthtak.gateway.model.ValidationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Cursor-on-Target (CoT) event codec. The structured report travels in a
 * {@code <tng_msg>} detail element; a human-readable summary goes in
 * {@code <remarks>} so stock TAK clients still show something useful.
 */
public final class CotCodec implements Codec {
    public static final String DETAIL = "tng_msg";
    /** How long a report stays current on the map. */
    public static final long STALE_SECONDS = 3600;

    private final DefinitionRegistry defs;

    public CotCodec(DefinitionRegistry defs) {
        this.defs = defs;
    }

    @Override public String id() { return "cot"; }
    @Override public String name() { return "Cursor-on-Target XML"; }
    @Override public String mediaType() { return "application/cot+xml"; }

    @Override
    public byte[] encode(Message m) {
        m.validate();
        MessageDef d = m.def;
        Position p = (Position) m.get(d.pointField);
        String t = Dtg.iso(m.time());
        StringBuilder b = new StringBuilder(512);
        b.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        b.append("<event version=\"2.0\" uid=\"").append(Xml.esc(m.uid()))
         .append("\" type=\"").append(Xml.esc(d.cotType))
         .append("\" how=\"h-g-i-g-o\" time=\"").append(t)
         .append("\" start=\"").append(t)
         .append("\" stale=\"").append(Dtg.iso(m.time() + STALE_SECONDS)).append("\">");
        b.append("<point lat=\"").append(String.format(Locale.ROOT, "%.5f", p.lat()))
         .append("\" lon=\"").append(String.format(Locale.ROOT, "%.5f", p.lon()))
         .append("\" hae=\"9999999.0\" ce=\"9999999.0\" le=\"9999999.0\"/>");
        b.append("<detail><contact callsign=\"").append(Xml.esc(m.originator())).append("\"/>");
        b.append('<').append(DETAIL).append(" def=\"").append(d.id)
         .append("\" ver=\"").append(d.version)
         .append("\" orig=\"").append(Xml.esc(m.originator()))
         .append("\" serial=\"").append(m.serial())
         .append("\" prec=\"").append(m.precedence()).append("\">");
        for (Map.Entry<String, Object> e : m.values().entrySet()) {
            if (e.getKey().equals(d.pointField)) continue;
            FieldDef f = d.field(e.getKey());
            b.append("<f n=\"").append(f.name).append("\" v=\"").append(Xml.esc(ValueText.toText(f, e.getValue()))).append("\"/>");
        }
        b.append("</").append(DETAIL).append('>');
        b.append("<remarks>").append(Xml.esc(summary(m))).append("</remarks>");
        b.append("</detail></event>");
        return b.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Message decode(byte[] data) {
        Document doc = Xml.parse(data);
        Element ev = doc.getDocumentElement();
        if (!"event".equals(ev.getNodeName())) throw new ValidationException("not a CoT event");
        Element detail = Xml.child(ev, "detail");
        Element tm = detail == null ? null : Xml.child(detail, DETAIL);
        if (tm == null) throw new ValidationException("CoT event carries no <" + DETAIL + "> detail");
        MessageDef d = defs.byId(Xml.requireAttr(tm, "def"));
        if (Integer.parseInt(Xml.requireAttr(tm, "ver")) != d.version) {
            throw new ValidationException(d.id + ": unsupported version " + tm.getAttribute("ver"));
        }
        Message m = new Message(d, Xml.requireAttr(tm, "orig"),
                Integer.parseInt(Xml.requireAttr(tm, "serial")),
                Dtg.parseIso(Xml.requireAttr(ev, "time")));
        m.setPrecedence(Precedence.valueOf(Xml.requireAttr(tm, "prec")));
        Element point = Xml.child(ev, "point");
        if (point == null) throw new ValidationException("CoT event has no <point>");
        m.set(d.pointField, new Position(ValueText.parse5(Xml.requireAttr(point, "lat")),
                ValueText.parse5(Xml.requireAttr(point, "lon"))));
        for (Element f : Xml.children(tm)) {
            if (!"f".equals(f.getNodeName())) continue;
            FieldDef fd = d.field(Xml.requireAttr(f, "n"));
            m.set(fd.name, ValueText.fromText(fd, Xml.requireAttr(f, "v")));
        }
        return m.validate();
    }

    /** Plain-language summary built from the definition's labels. */
    @SuppressWarnings("unchecked")
    static String summary(Message m) {
        StringBuilder b = new StringBuilder(m.def.title).append(". ");
        for (Map.Entry<String, Object> e : m.values().entrySet()) {
            FieldDef f = m.def.field(e.getKey());
            b.append(f.label).append(": ");
            Object v = e.getValue();
            switch (f.type) {
                case ENUM: b.append(label(f, (String) v)); break;
                case ENUM_SET: {
                    List<String> l = (List<String>) v;
                    for (int k = 0; k < l.size(); k++) b.append(k > 0 ? ", " : "").append(label(f, l.get(k)));
                    break;
                }
                case COUNTS: {
                    int k = 0;
                    for (Map.Entry<String, Integer> c : ((Map<String, Integer>) v).entrySet()) {
                        b.append(k++ > 0 ? ", " : "").append(label(f, c.getKey())).append(' ').append(c.getValue());
                    }
                    break;
                }
                default: b.append(ValueText.toText(f, v));
            }
            if (b.charAt(b.length() - 1) != '.') b.append('.');
            b.append(' ');
        }
        return b.toString().trim();
    }

    private static String label(FieldDef f, String code) {
        for (CodeDef c : f.codes) if (c.code.equals(code)) return c.label;
        return code;
    }
}
