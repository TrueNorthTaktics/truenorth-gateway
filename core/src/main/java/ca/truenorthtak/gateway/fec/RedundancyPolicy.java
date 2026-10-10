package ca.truenorthtak.gateway.fec;

import ca.truenorthtak.gateway.model.Precedence;

import java.util.EnumMap;
import java.util.Map;

/**
 * Decides how many parity shards to add. Higher-precedence traffic gets more
 * protection, so an urgent MEDEVAC request survives worse links than routine
 * traffic. This is the hook for future AI/ML prioritisation: a model can adjust
 * precedence or ratios from link telemetry.
 */
public final class RedundancyPolicy {
    private final Map<Precedence, Double> ratio = new EnumMap<>(Precedence.class);
    private final int minParity;

    public RedundancyPolicy(double routine, double priority, double immediate, double flash, int minParity) {
        ratio.put(Precedence.ROUTINE, routine);
        ratio.put(Precedence.PRIORITY, priority);
        ratio.put(Precedence.IMMEDIATE, immediate);
        ratio.put(Precedence.FLASH, flash);
        this.minParity = minParity;
    }

    /** Default: 25% / 50% / 100% / 150% parity, at least one parity shard. */
    public static RedundancyPolicy standard() {
        return new RedundancyPolicy(0.25, 0.5, 1.0, 1.5, 1);
    }

    /** No forward error correction (baseline for comparison). */
    public static RedundancyPolicy none() {
        return new RedundancyPolicy(0, 0, 0, 0, 0);
    }

    public int parityFor(Precedence p, int dataShards) {
        int m = (int) Math.ceil(dataShards * ratio.get(p));
        m = Math.max(m, minParity);
        return Math.min(m, ReedSolomon.MAX_SHARDS - dataShards);
    }
}
