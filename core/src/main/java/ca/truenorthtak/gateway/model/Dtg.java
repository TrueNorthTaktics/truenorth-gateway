package ca.truenorthtak.gateway.model;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Date-time helpers. All times are UTC epoch seconds at one-minute resolution. */
public final class Dtg {
    private Dtg() {}

    /** 2020-01-01T00:00:00Z, the epoch used by the compact binary codec. */
    public static final long EPOCH_2020 = 1577836800L;
    /** Latest representable time in the 24-bit minute counter (about 31 years). */
    public static final long MAX = EPOCH_2020 + ((1L << 24) - 1) * 60L;

    private static final DateTimeFormatter ADATP =
            DateTimeFormatter.ofPattern("ddHHmm'Z'MMMyy", Locale.ROOT).withZone(ZoneOffset.UTC);

    public static long normalize(long epochSeconds) {
        if (epochSeconds < EPOCH_2020 || epochSeconds > MAX) {
            throw new ValidationException("time outside supported range (2020-2051)");
        }
        return epochSeconds - Math.floorMod(epochSeconds, 60L);
    }

    public static String iso(long epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds).toString();
    }

    public static long parseIso(String s) {
        return Instant.parse(s).getEpochSecond();
    }

    /** Military date-time group, for example 101845ZOCT26. */
    public static String dtg(long epochSeconds) {
        return ADATP.format(Instant.ofEpochSecond(epochSeconds)).toUpperCase(Locale.ROOT);
    }

    public static long parseDtg(String s) {
        if (s.length() != 12) throw new ValidationException("bad DTG '" + s + "'");
        // ddHHmm Z MMM yy : month letters are at 7..9; formatter expects "Oct" style.
        String norm = s.substring(0, 8) + s.substring(8, 10).toLowerCase(Locale.ROOT) + s.substring(10);
        try {
            return Instant.from(ADATP.parse(norm)).getEpochSecond();
        } catch (RuntimeException e) {
            throw new ValidationException("bad DTG '" + s + "'", e);
        }
    }
}
