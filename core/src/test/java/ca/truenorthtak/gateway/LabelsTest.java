package ca.truenorthtak.gateway;

import ca.truenorthtak.gateway.model.FieldDef;
import ca.truenorthtak.gateway.model.MessageDef;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LabelsTest {
    private final Gateway gw = Gateway.withDefaults();

    @Test
    public void definitionsCarryEnglishAndFrenchLabels() {
        MessageDef d = gw.definitions.byId("MEDEVAC9");
        assertEquals("Demande d'évacuation sanitaire en 9 lignes (représentative)", d.title("fr"));
        FieldDef sec = d.field("pickupSecurity");
        assertEquals("Line 6 - Security at pickup site", sec.label("en"));
        assertEquals("Ligne 6 - Sécurité au site de ramassage", sec.label("fr"));
        assertEquals("Troupes ennemies possibles", sec.codes.get(sec.codeIndex("P")).label("fr"));
    }

    @Test
    public void everyLabelHasAFrenchVersion() {
        for (MessageDef d : gw.definitions.all()) {
            assertNotNull(d.id, d.titles.get("fr"));
            for (FieldDef f : d.fields) {
                assertNotNull(f.name, f.labels.get("fr"));
                for (int k = 0; k < f.codes.size(); k++) assertNotNull(f.name, f.codes.get(k).labels.get("fr"));
            }
        }
    }

    @Test
    public void missingLanguageFallsBackToEnglish() {
        assertEquals("Line 6 - Security at pickup site", gw.definitions.byId("MEDEVAC9").field("pickupSecurity").label("de"));
    }
}
