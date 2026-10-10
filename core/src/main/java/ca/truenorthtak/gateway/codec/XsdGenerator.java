package ca.truenorthtak.gateway.codec;

import ca.truenorthtak.gateway.model.CodeDef;
import ca.truenorthtak.gateway.model.FieldDef;
import ca.truenorthtak.gateway.model.MessageDef;
import ca.truenorthtak.gateway.model.Precedence;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Generates the W3C XML Schema for the {@link ExchangeXmlCodec} format directly
 * from message definitions. Adding a definition automatically extends the schema.
 */
public final class XsdGenerator {
    private XsdGenerator() {}

    /** Characters allowed in TEXT fields: ASCII 0x20-0x5F except '/' (0x2F). */
    static final String TEXT_PATTERN = "[&#x20;-&#x2E;&#x30;-&#x5F;]*";

    public static String generate(Collection<MessageDef> defs) {
        StringBuilder b = new StringBuilder(8192);
        b.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        b.append("<!-- Generated from message definitions. Representative exchange model; not an official NIEM/MTF schema. -->\n");
        b.append("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n");
        b.append("           xmlns:tng=\"").append(ExchangeXmlCodec.NS).append("\"\n");
        b.append("           targetNamespace=\"").append(ExchangeXmlCodec.NS).append("\"\n");
        b.append("           elementFormDefault=\"qualified\">\n\n");

        b.append("  <xs:complexType name=\"MessageHeaderType\">\n    <xs:sequence>\n");
        b.append("      <xs:element name=\"MessageDefinitionIdentifier\" type=\"xs:token\"/>\n");
        b.append("      <xs:element name=\"MessageDefinitionVersionNumber\" type=\"xs:unsignedByte\"/>\n");
        b.append("      <xs:element name=\"OriginatorCallSignText\" type=\"tng:CallSignTextType\"/>\n");
        b.append("      <xs:element name=\"MessageSerialNumber\" type=\"xs:unsignedShort\"/>\n");
        b.append("      <xs:element name=\"MessageDateTime\" type=\"xs:dateTime\"/>\n");
        b.append("      <xs:element name=\"MessagePrecedenceCode\" type=\"tng:PrecedenceCodeType\"/>\n");
        b.append("    </xs:sequence>\n  </xs:complexType>\n\n");

        b.append("  <xs:simpleType name=\"CallSignTextType\">\n    <xs:restriction base=\"xs:string\">\n");
        b.append("      <xs:minLength value=\"1\"/>\n      <xs:maxLength value=\"12\"/>\n");
        b.append("      <xs:pattern value=\"").append(TEXT_PATTERN).append("\"/>\n");
        b.append("    </xs:restriction>\n  </xs:simpleType>\n\n");

        b.append("  <xs:simpleType name=\"PrecedenceCodeType\">\n    <xs:restriction base=\"xs:token\">\n");
        for (Precedence p : Precedence.values()) {
            b.append("      <xs:enumeration value=\"").append(p.name()).append("\"/>\n");
        }
        b.append("    </xs:restriction>\n  </xs:simpleType>\n\n");

        b.append("  <xs:complexType name=\"GeoPointType\">\n    <xs:sequence>\n");
        b.append("      <xs:element name=\"LatitudeDegreeValue\">\n        <xs:simpleType><xs:restriction base=\"xs:decimal\">")
         .append("<xs:fractionDigits value=\"5\"/><xs:minInclusive value=\"-90\"/><xs:maxInclusive value=\"90\"/>")
         .append("</xs:restriction></xs:simpleType>\n      </xs:element>\n");
        b.append("      <xs:element name=\"LongitudeDegreeValue\">\n        <xs:simpleType><xs:restriction base=\"xs:decimal\">")
         .append("<xs:fractionDigits value=\"5\"/><xs:minInclusive value=\"-180\"/><xs:maxInclusive value=\"180\"/>")
         .append("</xs:restriction></xs:simpleType>\n      </xs:element>\n");
        b.append("    </xs:sequence>\n  </xs:complexType>\n");

        for (MessageDef d : defs) {
            b.append("\n  <!-- ").append(d.id).append(" v").append(d.version).append(": ").append(d.title).append(" -->\n");
            b.append("  <xs:element name=\"").append(d.xmlName).append("\">\n    <xs:complexType>\n      <xs:sequence>\n");
            b.append("        <xs:element name=\"MessageHeader\" type=\"tng:MessageHeaderType\"/>\n");
            for (FieldDef f : d.fields) field(b, f);
            b.append("      </xs:sequence>\n    </xs:complexType>\n  </xs:element>\n");
        }
        b.append("</xs:schema>\n");
        return b.toString();
    }

    private static void field(StringBuilder b, FieldDef f) {
        String ind = "        ";
        String min = f.required ? "1" : "0";
        switch (f.type) {
            case POSITION:
                b.append(ind).append("<xs:element name=\"").append(f.xml).append("\" type=\"tng:GeoPointType\" minOccurs=\"")
                 .append(min).append("\"/>\n");
                return;
            case ENUM_SET:
                b.append(ind).append("<xs:element name=\"").append(f.xml).append("\" minOccurs=\"").append(min)
                 .append("\" maxOccurs=\"").append(f.codes.size()).append("\">\n");
                b.append(ind).append("  <xs:simpleType>").append(enumeration(f)).append("</xs:simpleType>\n");
                b.append(ind).append("</xs:element>\n");
                return;
            case COUNTS:
                b.append(ind).append("<xs:element name=\"").append(f.xml).append("\" minOccurs=\"").append(min)
                 .append("\" maxOccurs=\"").append(f.codes.size()).append("\">\n");
                b.append(ind).append("  <xs:complexType><xs:sequence>\n");
                b.append(ind).append("    <xs:element name=\"CategoryCode\"><xs:simpleType>").append(enumeration(f))
                 .append("</xs:simpleType></xs:element>\n");
                b.append(ind).append("    <xs:element name=\"Quantity\"><xs:simpleType><xs:restriction base=\"xs:int\">")
                 .append("<xs:minInclusive value=\"1\"/><xs:maxInclusive value=\"").append(f.max)
                 .append("\"/></xs:restriction></xs:simpleType></xs:element>\n");
                b.append(ind).append("  </xs:sequence></xs:complexType>\n");
                b.append(ind).append("</xs:element>\n");
                return;
            default:
                b.append(ind).append("<xs:element name=\"").append(f.xml).append("\" minOccurs=\"").append(min).append("\">\n");
                b.append(ind).append("  <xs:simpleType>").append(simple(f)).append("</xs:simpleType>\n");
                b.append(ind).append("</xs:element>\n");
        }
    }

    private static String simple(FieldDef f) {
        switch (f.type) {
            case TEXT:
                return "<xs:restriction base=\"xs:string\"><xs:minLength value=\"1\"/><xs:maxLength value=\"" + f.max
                        + "\"/><xs:pattern value=\"" + TEXT_PATTERN + "\"/></xs:restriction>";
            case INT:
                return "<xs:restriction base=\"xs:long\"><xs:minInclusive value=\"" + f.min
                        + "\"/><xs:maxInclusive value=\"" + f.max + "\"/></xs:restriction>";
            case DECIMAL:
                return "<xs:restriction base=\"xs:decimal\"><xs:fractionDigits value=\"" + f.scale
                        + "\"/><xs:minInclusive value=\"" + BigDecimal.valueOf(f.min).toPlainString()
                        + "\"/><xs:maxInclusive value=\"" + BigDecimal.valueOf(f.max).toPlainString()
                        + "\"/></xs:restriction>";
            case ENUM:
                return enumeration(f);
            case DTG:
                return "<xs:restriction base=\"xs:dateTime\"/>";
            default:
                throw new IllegalStateException("unhandled type " + f.type);
        }
    }

    private static String enumeration(FieldDef f) {
        StringBuilder b = new StringBuilder("<xs:restriction base=\"xs:token\">");
        for (CodeDef c : f.codes) b.append("<xs:enumeration value=\"").append(Xml.esc(c.code)).append("\"/>");
        return b.append("</xs:restriction>").toString();
    }
}
