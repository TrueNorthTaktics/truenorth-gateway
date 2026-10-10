package ca.truenorthtak.gateway;

import ca.truenorthtak.gateway.samples.Samples;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class SlashTextTest {
    private final Gateway gw = Gateway.withDefaults();

    @Test
    public void medevacMatchesDocumentedGrammar() {
        String expected =
                "MSGID/MEDEVAC9/1/BRAVO22/42/IMMEDIATE//\n" +
                "DTG/101845ZOCT26//\n" +
                "L1/45.90123N/077.31234W//\n" +
                "L2/30.125/MEDIC21//\n" +
                "L3/A2/C1//\n" +
                "L4/B/D//\n" +
                "L5/L2/A1//\n" +
                "L6/P//\n" +
                "L7/C//\n" +
                "L8/A3//\n" +
                "RMKS/LZ GREEN SMOKE. 1X CASUALTY REQUIRES VENTILATOR.//\n";
        assertEquals(expected, new String(gw.encode(Samples.medevac(gw.definitions), "slash"), StandardCharsets.US_ASCII));
    }

    @Test
    public void toleratesMissingLineBreaks() {
        byte[] oneLine = new String(gw.encode(Samples.medevac(gw.definitions), "slash"), StandardCharsets.US_ASCII)
                .replace("\n", "").getBytes(StandardCharsets.US_ASCII);
        assertEquals(Samples.medevac(gw.definitions), gw.decode(oneLine, "slash"));
    }
}
