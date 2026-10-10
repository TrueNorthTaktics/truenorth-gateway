package ca.truenorthtak.gateway;

import ca.truenorthtak.gateway.model.Message;
import ca.truenorthtak.gateway.model.ValidationException;
import ca.truenorthtak.gateway.samples.Samples;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ValidationTest {
    private final Gateway gw = Gateway.withDefaults();

    private void expectInvalid(Runnable r) {
        try {
            r.run();
            fail("expected ValidationException");
        } catch (ValidationException expected) {
            // ok
        }
    }

    @Test
    public void rejectsBadValues() {
        Message m = Samples.medevac(gw.definitions);
        expectInvalid(() -> m.set("pickupSecurity", "Z"));
        expectInvalid(() -> m.set("remarks", "CONTAINS / SLASH"));
        expectInvalid(() -> m.set("radioFrequency", new java.math.BigDecimal("30.1234")));
        expectInvalid(() -> m.set("patientsByPrecedence", Collections.singletonMap("A", 99)));
        expectInvalid(() -> m.set("noSuchField", "X"));
        expectInvalid(() -> new Message(gw.definitions.byId("MEDEVAC9"), "BRAVO22", 1, Samples.TIME).validate());
    }

    @Test
    public void textIsNormalisedToUpperCase() {
        Message m = Samples.medevac(gw.definitions).set("callsign", "medic21");
        assertEquals("MEDIC21", m.get("callsign"));
    }

    @Test
    public void rejectsMalformedInput() {
        expectInvalid(() -> gw.decode("MSGID/NOPE/1/X/1/ROUTINE//DTG/101845ZOCT26//".getBytes(StandardCharsets.US_ASCII), "slash"));
        expectInvalid(() -> gw.decode("<event/>".getBytes(StandardCharsets.UTF_8), "cot"));
        expectInvalid(() -> gw.decode(new byte[] {1, 2}, "bin"));
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><event>&e;</event>";
        expectInvalid(() -> gw.decode(xxe.getBytes(StandardCharsets.UTF_8), "cot"));
    }
}
