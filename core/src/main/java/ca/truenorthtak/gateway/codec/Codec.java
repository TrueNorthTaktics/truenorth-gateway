package ca.truenorthtak.gateway.codec;

import ca.truenorthtak.gateway.model.Message;

/**
 * Converts messages to and from one wire format. Codecs are pluggable: a new
 * standard is supported by registering another implementation.
 */
public interface Codec {
    /** Short identifier, for example "cot" or "slash". */
    String id();

    /** Human-readable name of the format. */
    String name();

    /** MIME-style media type of the encoded form. */
    String mediaType();

    byte[] encode(Message message);

    Message decode(byte[] data);
}
