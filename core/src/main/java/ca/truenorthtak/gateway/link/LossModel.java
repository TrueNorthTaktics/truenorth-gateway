package ca.truenorthtak.gateway.link;

import java.util.Random;

/** Decides, fragment by fragment, whether a transmission is lost. */
public interface LossModel {
    boolean lost(Random rng);

    /** Resets any internal state (start of a new message). */
    default void reset(Random rng) {}

    String describe();

    /** Independent random loss with probability p. */
    static LossModel random(double p) {
        return new LossModel() {
            @Override public boolean lost(Random rng) { return rng.nextDouble() < p; }
            @Override public String describe() { return String.format(java.util.Locale.ROOT, "random %.0f%%", p * 100); }
        };
    }

    /**
     * Gilbert-Elliott burst loss: a two-state channel (good/bad) that models fades
     * and jamming bursts. {@code pGoodToBad} and {@code pBadToGood} are per-fragment
     * transition probabilities; {@code lossInBad} is the loss rate while bad.
     */
    static LossModel burst(double pGoodToBad, double pBadToGood, double lossInBad) {
        return new LossModel() {
            private boolean bad;

            @Override public void reset(Random rng) {
                double steadyBad = pGoodToBad / (pGoodToBad + pBadToGood);
                bad = rng.nextDouble() < steadyBad;
            }

            @Override public boolean lost(Random rng) {
                boolean l = bad && rng.nextDouble() < lossInBad;
                bad = bad ? rng.nextDouble() >= pBadToGood : rng.nextDouble() < pGoodToBad;
                return l;
            }

            @Override public String describe() {
                double avg = pGoodToBad / (pGoodToBad + pBadToGood) * lossInBad;
                return String.format(java.util.Locale.ROOT, "burst (avg %.0f%%)", avg * 100);
            }
        };
    }
}
