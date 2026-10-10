package ca.truenorthtak.gateway.samples;

import ca.truenorthtak.gateway.model.DefinitionRegistry;
import ca.truenorthtak.gateway.model.Dtg;
import ca.truenorthtak.gateway.model.Message;
import ca.truenorthtak.gateway.model.Position;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Synthetic, unclassified example messages used by tests, the benchmark and demos. */
public final class Samples {
    private Samples() {}

    /** 2026-10-10T18:45:00Z */
    public static final long TIME = Dtg.parseIso("2026-10-10T18:45:00Z");

    public static Message medevac(DefinitionRegistry defs) {
        Map<String, Integer> prec = new LinkedHashMap<>();
        prec.put("A", 2);
        prec.put("C", 1);
        Map<String, Integer> type = new LinkedHashMap<>();
        type.put("L", 2);
        type.put("A", 1);
        Map<String, Integer> status = new LinkedHashMap<>();
        status.put("A", 3);
        return new Message(defs.byId("MEDEVAC9"), "BRAVO22", 42, TIME)
                .set("pickupLocation", Position.of(45.90123, -77.31234))
                .set("radioFrequency", new BigDecimal("30.125"))
                .set("callsign", "MEDIC21")
                .set("patientsByPrecedence", prec)
                .set("specialEquipment", Arrays.asList("B", "D"))
                .set("patientsByType", type)
                .set("pickupSecurity", "P")
                .set("markingMethod", "C")
                .set("patientsByStatus", status)
                .set("remarks", "LZ GREEN SMOKE. 1X CASUALTY REQUIRES VENTILATOR.")
                .validate();
    }

    public static Message spotrep(DefinitionRegistry defs) {
        return new Message(defs.byId("SPOTREP"), "RECCE31", 7, TIME)
                .set("size", 4)
                .set("activity", "MOVING NORTH ON MSR TORONTO AT 30 KMH")
                .set("location", Position.of(45.88761, -77.29010))
                .set("affiliation", "H")
                .set("unit", "UNKNOWN MECHANIZED ELEMENT")
                .set("observedTime", Dtg.parseIso("2026-10-10T18:31:00Z"))
                .set("equipment", "2X WHEELED APC, 2X LIGHT TRUCK")
                .validate();
    }
}
