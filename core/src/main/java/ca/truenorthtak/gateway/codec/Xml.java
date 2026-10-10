package ca.truenorthtak.gateway.codec;

import ca.truenorthtak.gateway.model.ValidationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/** Small XML helpers. Parsing is hardened against external entities (XXE). */
final class Xml {
    private Xml() {}

    static String esc(String s) {
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            switch (c) {
                case '&': b.append("&amp;"); break;
                case '<': b.append("&lt;"); break;
                case '>': b.append("&gt;"); break;
                case '"': b.append("&quot;"); break;
                case '\'': b.append("&apos;"); break;
                default: b.append(c);
            }
        }
        return b.toString();
    }

    static Document parse(byte[] data) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setExpandEntityReferences(false);
            trySet(f, "http://apache.org/xml/features/disallow-doctype-decl", true);
            trySet(f, "http://xml.org/sax/features/external-general-entities", false);
            trySet(f, "http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder b = f.newDocumentBuilder();
            b.setErrorHandler(null);
            return b.parse(new ByteArrayInputStream(data));
        } catch (Exception e) {
            throw new ValidationException("malformed XML: " + e.getMessage(), e);
        }
    }

    private static void trySet(DocumentBuilderFactory f, String feature, boolean v) {
        try {
            f.setFeature(feature, v);
        } catch (Exception ignored) {
            // Not every platform parser (for example Android's) supports every feature.
        }
    }

    static List<Element> children(Element e) {
        List<Element> out = new ArrayList<>();
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE) out.add((Element) n);
        }
        return out;
    }

    static Element child(Element e, String localName) {
        for (Element c : children(e)) {
            if (localName.equals(c.getLocalName()) || localName.equals(c.getNodeName())) return c;
        }
        return null;
    }

    static String text(Element e) {
        return e == null ? null : e.getTextContent().trim();
    }

    static String requireAttr(Element e, String name) {
        if (!e.hasAttribute(name)) throw new ValidationException("missing attribute '" + name + "' on <" + e.getNodeName() + ">");
        return e.getAttribute(name);
    }
}
