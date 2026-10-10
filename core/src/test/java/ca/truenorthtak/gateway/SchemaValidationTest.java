package ca.truenorthtak.gateway;

import ca.truenorthtak.gateway.codec.XsdGenerator;
import ca.truenorthtak.gateway.samples.Samples;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.fail;

public class SchemaValidationTest {
    private final Gateway gw = Gateway.withDefaults();

    private Schema schema() throws SAXException {
        String xsd = XsdGenerator.generate(gw.definitions.all());
        return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new StreamSource(new StringReader(xsd)));
    }

    @Test
    public void generatedXmlValidatesAgainstGeneratedSchema() throws Exception {
        Schema s = schema();
        for (byte[] xml : new byte[][] {
                gw.encode(Samples.medevac(gw.definitions), "xml"),
                gw.encode(Samples.spotrep(gw.definitions), "xml")}) {
            s.newValidator().validate(new StreamSource(new ByteArrayInputStream(xml)));
        }
    }

    @Test
    public void schemaRejectsInvalidDocument() throws Exception {
        String bad = new String(gw.encode(Samples.medevac(gw.definitions), "xml"), StandardCharsets.UTF_8)
                .replace("<tng:PickupSiteSecurityCode>P<", "<tng:PickupSiteSecurityCode>Q<");
        try {
            schema().newValidator().validate(new StreamSource(new ByteArrayInputStream(bad.getBytes(StandardCharsets.UTF_8))));
            fail("schema accepted an invalid code");
        } catch (SAXException expected) {
            // ok
        }
    }
}
