package ca.truenorthtak.gateway.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** The set of message definitions known to a gateway instance. */
public final class DefinitionRegistry {
    /** Definitions shipped with the library. */
    public static final String[] BUILT_IN = {"medevac9.json", "spotrep.json"};

    private final Map<String, MessageDef> byId = new LinkedHashMap<>();
    private final Map<Integer, MessageDef> byCode = new LinkedHashMap<>();

    public static DefinitionRegistry builtIn() {
        DefinitionRegistry r = new DefinitionRegistry();
        for (String name : BUILT_IN) {
            try (InputStream in = DefinitionRegistry.class.getResourceAsStream("/ca/truenorthtak/gateway/defs/" + name)) {
                if (in == null) throw new IllegalStateException("missing built-in definition " + name);
                r.register(DefinitionLoader.fromStream(in));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return r;
    }

    public DefinitionRegistry register(MessageDef def) {
        if (byId.containsKey(def.id)) throw new ValidationException("duplicate definition id " + def.id);
        if (byCode.containsKey(def.code)) throw new ValidationException("duplicate definition code " + def.code);
        byId.put(def.id, def);
        byCode.put(def.code, def);
        return this;
    }

    public MessageDef byId(String id) {
        MessageDef d = byId.get(id);
        if (d == null) throw new ValidationException("unknown message definition '" + id + "'");
        return d;
    }

    public MessageDef byCode(int code) {
        MessageDef d = byCode.get(code);
        if (d == null) throw new ValidationException("unknown message code " + code);
        return d;
    }

    public MessageDef byXmlName(String name) {
        for (MessageDef d : byId.values()) if (d.xmlName.equals(name)) return d;
        throw new ValidationException("unknown XML message '" + name + "'");
    }

    public Collection<MessageDef> all() {
        return Collections.unmodifiableList(new ArrayList<>(byId.values()));
    }
}
