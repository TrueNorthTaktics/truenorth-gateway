package ca.truenorthtak.gateway.codec;

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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Structured XML exchange format following NIEM-style conventions (qualified
 * elements, representation-term names such as ...Code, ...Text, ...Value). The
 * schema for every message type is generated from its definition by
 * {@link XsdGenerator}, so output can be validated with standard XML tooling.
 *
 * <p>This is a representative exchange model, not an official NIEM, MTF-XML or
 * NATO schema. Mapping to an authoritative model is planned work.</p>
 */
public final class ExchangeXmlCodec implements Codec {
    public static final String NS = "urn:truenorthtak:gateway:exchange:1";
    public static final String PREFIX = "tng";

    private final DefinitionRegistry defs;
    private final boolean indent;

    public ExchangeXmlCodec(DefinitionRegistry defs) {
        this(defs, false);
    }

    public ExchangeXmlCodec(DefinitionRegistry defs, boolean indent) {
        this.defs = defs;
        this.indent = indent;
    }

    @Override public String id() { return "xml"; }
    @Override public String name() { return "Structured XML exchange (NIEM-style, schema-validated)"; }
    @Override public String mediaType() { return "application/xml"; }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] encode(Message m) {
        m.validate();
        MessageDef d = m.def;
        W w = new W(indent);
        w.raw("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        w.open(d.xmlName, " xmlns:" + PREFIX + "=\"" + NS + "\"");
        w.open("MessageHeader", "");
        w.leaf("MessageDefinitionIdentifier", d.id);
        w.leaf("MessageDefinitionVersionNumber", String.valueOf(d.version));
        w.leaf("OriginatorCallSignText", m.originator());
        w.leaf("MessageSerialNumber", String.valueOf(m.serial()));
        w.leaf("MessageDateTime", Dtg.iso(m.time()));
        w.leaf("MessagePrecedenceCode", m.precedence().name());
        w.close("MessageHeader");
        for (Map.Entry<String, Object> e : m.values().entrySet()) {
            FieldDef f = d.field(e.getKey());
            Object v = e.getValue();
            switch (f.type) {
                case POSITION: {
                    Position p = (Position) v;
                    w.open(f.xml, "");
                    w.leaf("LatitudeDegreeValue", String.format(Locale.ROOT, "%.5f", p.lat()));
                    w.leaf("LongitudeDegreeValue", String.format(Locale.ROOT, "%.5f", p.lon()));
                    w.close(f.xml);
                    break;
                }
                case ENUM_SET:
                    for (String c : (List<String>) v) w.leaf(f.xml, c);
                    break;
                case COUNTS:
                    for (Map.Entry<String, Integer> c : ((Map<String, Integer>) v).entrySet()) {
                        w.open(f.xml, "");
                        w.leaf("CategoryCode", c.getKey());
                        w.leaf("Quantity", String.valueOf(c.getValue()));
                        w.close(f.xml);
                    }
                    break;
                case DECIMAL: w.leaf(f.xml, ((BigDecimal) v).toPlainString()); break;
                case DTG: w.leaf(f.xml, Dtg.iso((Long) v)); break;
                default: w.leaf(f.xml, v.toString());
            }
        }
        w.close(d.xmlName);
        return w.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Message decode(byte[] data) {
        Document doc = Xml.parse(data);
        Element root = doc.getDocumentElement();
        if (!NS.equals(root.getNamespaceURI())) throw new ValidationException("wrong namespace " + root.getNamespaceURI());
        MessageDef d = defs.byXmlName(root.getLocalName());
        Element h = Xml.child(root, "MessageHeader");
        if (h == null) throw new ValidationException("missing MessageHeader");
        if (!d.id.equals(Xml.text(Xml.child(h, "MessageDefinitionIdentifier")))) {
            throw new ValidationException("header definition does not match root element");
        }
        if (Integer.parseInt(req(h, "MessageDefinitionVersionNumber")) != d.version) {
            throw new ValidationException(d.id + ": unsupported version");
        }
        Message m = new Message(d, req(h, "OriginatorCallSignText"),
                Integer.parseInt(req(h, "MessageSerialNumber")),
                Dtg.parseIso(req(h, "MessageDateTime")));
        m.setPrecedence(Precedence.valueOf(req(h, "MessagePrecedenceCode")));

        Map<String, FieldDef> byXml = new LinkedHashMap<>();
        for (FieldDef f : d.fields) byXml.put(f.xml, f);
        Map<String, List<String>> sets = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> counts = new LinkedHashMap<>();
        for (Element e : Xml.children(root)) {
            if (e == h) continue;
            if (!NS.equals(e.getNamespaceURI())) throw new ValidationException("unexpected element " + e.getNodeName());
            FieldDef f = byXml.get(e.getLocalName());
            if (f == null) throw new ValidationException(d.id + ": unexpected element " + e.getLocalName());
            switch (f.type) {
                case POSITION:
                    m.set(f.name, new Position(ValueText.parse5(req(e, "LatitudeDegreeValue")),
                            ValueText.parse5(req(e, "LongitudeDegreeValue"))));
                    break;
                case ENUM_SET:
                    sets.computeIfAbsent(f.name, k -> new ArrayList<>()).add(Xml.text(e));
                    break;
                case COUNTS:
                    counts.computeIfAbsent(f.name, k -> new LinkedHashMap<>())
                            .put(req(e, "CategoryCode"), Integer.parseInt(req(e, "Quantity")));
                    break;
                default:
                    m.set(f.name, ValueText.fromText(f, Xml.text(e)));
            }
        }
        for (Map.Entry<String, List<String>> e : sets.entrySet()) m.set(e.getKey(), e.getValue());
        for (Map.Entry<String, Map<String, Integer>> e : counts.entrySet()) m.set(e.getKey(), e.getValue());
        return m.validate();
    }

    private static String req(Element parent, String name) {
        String t = Xml.text(Xml.child(parent, name));
        if (t == null) throw new ValidationException("missing <" + name + ">");
        return t;
    }

    /** Tiny XML writer producing prefixed, optionally indented output. */
    private static final class W {
        private final StringBuilder b = new StringBuilder(1024);
        private final boolean indent;
        private int depth;

        W(boolean indent) { this.indent = indent; }

        void raw(String s) { b.append(s); }

        void open(String name, String attrs) {
            nl();
            b.append('<').append(PREFIX).append(':').append(name).append(attrs).append('>');
            depth++;
        }

        void close(String name) {
            depth--;
            nl();
            b.append("</").append(PREFIX).append(':').append(name).append('>');
        }

        void leaf(String name, String text) {
            nl();
            b.append('<').append(PREFIX).append(':').append(name).append('>')
             .append(Xml.esc(text))
             .append("</").append(PREFIX).append(':').append(name).append('>');
        }

        private void nl() {
            if (!indent) return;
            b.append('\n');
            for (int k = 0; k < depth; k++) b.append("  ");
        }

        @Override public String toString() { return b.toString() + (indent ? "\n" : ""); }
    }
}
