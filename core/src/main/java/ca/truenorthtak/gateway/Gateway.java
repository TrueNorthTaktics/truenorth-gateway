package ca.truenorthtak.gateway;

import ca.truenorthtak.gateway.codec.Codec;
import ca.truenorthtak.gateway.codec.CodecRegistry;
import ca.truenorthtak.gateway.fec.Fragmenter;
import ca.truenorthtak.gateway.fec.RedundancyPolicy;
import ca.truenorthtak.gateway.link.LinkProfile;
import ca.truenorthtak.gateway.model.DefinitionRegistry;
import ca.truenorthtak.gateway.model.Message;

import java.util.List;

/**
 * Entry point used by the ATAK plugin (and any other host): translate between
 * formats and prepare messages for a given radio link.
 *
 * <pre>
 * Gateway gw = Gateway.withDefaults();
 * byte[] slash = gw.transcode(cotBytes, "cot", "slash");
 * List&lt;byte[]&gt; fragments = gw.prepare(message, LinkProfile.HF_DATA);
 * </pre>
 */
public final class Gateway {
    public final DefinitionRegistry definitions;
    public final CodecRegistry codecs;
    public final RedundancyPolicy redundancy;

    public Gateway(DefinitionRegistry definitions, CodecRegistry codecs, RedundancyPolicy redundancy) {
        this.definitions = definitions;
        this.codecs = codecs;
        this.redundancy = redundancy;
    }

    public static Gateway withDefaults() {
        DefinitionRegistry defs = DefinitionRegistry.builtIn();
        return new Gateway(defs, CodecRegistry.standard(defs), RedundancyPolicy.standard());
    }

    public byte[] encode(Message m, String format) {
        return codecs.get(format).encode(m);
    }

    public Message decode(byte[] data, String format) {
        return codecs.get(format).decode(data);
    }

    /** Converts an encoded message from one format to another. */
    public byte[] transcode(byte[] data, String from, String to) {
        Codec in = codecs.get(from);
        Codec out = codecs.get(to);
        return out.encode(in.decode(data));
    }

    /** Encodes in the link's preferred format, fragments, and adds precedence-based FEC. */
    public List<byte[]> prepare(Message m, LinkProfile link) {
        byte[] payload = encode(m, link.preferredFormat);
        return Fragmenter.split(payload, m.serial(), link.maxFragmentBytes, m.precedence(), redundancy);
    }
}
