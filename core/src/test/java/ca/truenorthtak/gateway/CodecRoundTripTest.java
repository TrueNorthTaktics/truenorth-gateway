package ca.truenorthtak.gateway;

import ca.truenorthtak.gateway.codec.Codec;
import ca.truenorthtak.gateway.model.Message;
import ca.truenorthtak.gateway.samples.Samples;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CodecRoundTripTest {
    private final Gateway gw = Gateway.withDefaults();

    private List<Message> samples() {
        return Arrays.asList(Samples.medevac(gw.definitions), Samples.spotrep(gw.definitions));
    }

    @Test
    public void everyFormatRoundTripsLosslessly() {
        for (Message m : samples()) {
            for (Codec c : gw.codecs.all()) {
                Message back = c.decode(c.encode(m));
                assertEquals(c.id() + " " + m.def.id, m, back);
            }
        }
    }

    @Test
    public void crossFormatChainPreservesMeaning() {
        for (Message m : samples()) {
            byte[] cot = gw.encode(m, "cot");
            byte[] slash = gw.transcode(cot, "cot", "slash");
            byte[] bin = gw.transcode(slash, "slash", "bin");
            byte[] xml = gw.transcode(bin, "bin", "xml");
            byte[] cot2 = gw.transcode(xml, "xml", "cot");
            assertEquals(m, gw.decode(cot2, "cot"));
        }
    }

    @Test
    public void optionalFieldsMayBeAbsent() {
        Message m = Samples.medevac(gw.definitions).set("remarks", null);
        for (Codec c : gw.codecs.all()) assertEquals(c.id(), m, c.decode(c.encode(m)));
        Message s = Samples.spotrep(gw.definitions).set("unit", null).set("equipment", null);
        for (Codec c : gw.codecs.all()) assertEquals(c.id(), s, c.decode(c.encode(s)));
    }

    @Test
    public void southernAndEasternHemispheres() {
        Message m = Samples.medevac(gw.definitions)
                .set("pickupLocation", ca.truenorthtak.gateway.model.Position.of(-33.86882, 151.20929));
        for (Codec c : gw.codecs.all()) assertEquals(c.id(), m, c.decode(c.encode(m)));
    }
}
