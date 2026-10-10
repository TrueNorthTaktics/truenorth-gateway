package ca.truenorthtak.gateway.model;

/** One allowed code of an ENUM, ENUM_SET or COUNTS field. */
public final class CodeDef {
    public final String code;
    public final String label;

    public CodeDef(String code, String label) {
        this.code = code;
        this.label = label == null ? code : label;
    }
}
