package ca.truenorthtak.gateway.link;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Representative, unclassified characteristics of a radio bearer. Values are
 * planning assumptions for emulation (see docs/LINK_PROFILES.md), not measured
 * performance of any specific equipment.
 */
public final class LinkProfile {
    public final String id;
    public final String name;
    /** Usable application data rate in bits per second. */
    public final int bitsPerSecond;
    /** Largest fragment the link carries in one transmission unit, in bytes. */
    public final int maxFragmentBytes;
    /** True if receivers cannot transmit (broadcast or emission control). */
    public final boolean oneWay;
    /** Preferred wire format for this link. */
    public final String preferredFormat;

    public LinkProfile(String id, String name, int bitsPerSecond, int maxFragmentBytes, boolean oneWay,
                       String preferredFormat) {
        this.id = id;
        this.name = name;
        this.bitsPerSecond = bitsPerSecond;
        this.maxFragmentBytes = maxFragmentBytes;
        this.oneWay = oneWay;
        this.preferredFormat = preferredFormat;
    }

    public static final LinkProfile VLF_BROADCAST =
            new LinkProfile("vlf", "VLF/LF broadcast (one-way)", 50, 16, true, "bin");
    public static final LinkProfile HF_DATA =
            new LinkProfile("hf", "HF data link", 2400, 64, false, "bin");
    public static final LinkProfile SATCOM_NB =
            new LinkProfile("satcom", "Narrowband SATCOM", 9600, 256, false, "slash");
    public static final LinkProfile UHF_MANET =
            new LinkProfile("manet", "UHF tactical MANET", 1_000_000, 1024, false, "cot");

    public static List<LinkProfile> all() {
        return Collections.unmodifiableList(Arrays.asList(VLF_BROADCAST, HF_DATA, SATCOM_NB, UHF_MANET));
    }

    /** Seconds needed to send the given number of bytes, ignoring modem framing. */
    public double airtimeSeconds(int bytes) {
        return bytes * 8.0 / bitsPerSecond;
    }
}
