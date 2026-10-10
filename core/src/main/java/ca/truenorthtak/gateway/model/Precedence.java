package ca.truenorthtak.gateway.model;

/** Message precedence, lowest to highest. Drives forward-error-correction redundancy. */
public enum Precedence {
    ROUTINE, PRIORITY, IMMEDIATE, FLASH
}
