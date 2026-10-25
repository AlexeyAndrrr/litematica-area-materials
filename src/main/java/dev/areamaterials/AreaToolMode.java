package dev.areamaterials;

import fi.dy.masa.litematica.tool.ToolMode;

/** The extra enum value is installed while Litematica initializes its tool modes. */
public final class AreaToolMode {
    public static final String NAME = "AREA_MATERIALS";

    private AreaToolMode() {}

    public static ToolMode get() {
        return ToolMode.valueOf(NAME);
    }
}
