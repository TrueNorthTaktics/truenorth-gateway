package ca.truenorthtak.gateway.model;

/** Value types a message field can hold. Each codec knows how to represent every type. */
public enum FieldType {
    /** Geographic point, stored at 1e-5 degree resolution (about 1.1 m). */
    POSITION,
    /** Upper-case free text restricted to the character-oriented (DEC SIXBIT) alphabet, no '/'. */
    TEXT,
    /** Bounded integer. */
    INT,
    /** Bounded decimal with a fixed number of fraction digits. */
    DECIMAL,
    /** One code from a fixed list. */
    ENUM,
    /** Zero or more codes from a fixed list. */
    ENUM_SET,
    /** A count per code (for example patients per precedence category). */
    COUNTS,
    /** Date-time group at one-minute resolution (UTC). */
    DTG
}
