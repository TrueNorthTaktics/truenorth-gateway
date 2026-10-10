package ca.truenorthtak.gateway.codec;

import ca.truenorthtak.gateway.model.DefinitionRegistry;
import ca.truenorthtak.gateway.model.ValidationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** The format adapters available to a gateway. */
public final class CodecRegistry {
    private final Map<String, Codec> codecs = new LinkedHashMap<>();

    /** CoT, XML exchange, character-oriented text and compact binary. */
    public static CodecRegistry standard(DefinitionRegistry defs) {
        return new CodecRegistry()
                .register(new CotCodec(defs))
                .register(new ExchangeXmlCodec(defs))
                .register(new SlashTextCodec(defs))
                .register(new CompactBinaryCodec(defs));
    }

    public CodecRegistry register(Codec c) {
        codecs.put(c.id(), c);
        return this;
    }

    public Codec get(String id) {
        Codec c = codecs.get(id);
        if (c == null) throw new ValidationException("unknown format '" + id + "'");
        return c;
    }

    public Collection<Codec> all() {
        return Collections.unmodifiableList(new ArrayList<>(codecs.values()));
    }
}
