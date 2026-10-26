package dev.areamaterials;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;

public final class AreaMenu extends GuiBase {
    private final SchematicPlacement placement;

    public AreaMenu(SchematicPlacement placement) {
        this.placement = placement;
        this.title = StringUtils.translate("area_materials.menu.title");
    }

    @Override public void initGui() {
        super.initGui();
        int x = 12, y = 30;
        int buttonWidth = Math.min(270, this.width - 114);
        var selection = AreaMaterialsClient.selection(placement);
        this.addLabel(x, y, -1, 12, 0xFFFFFFFF, placement.getName());
        this.addLabel(x, y + 18, -1, 12, 0xFF58E2D3, StringUtils.translate("area_materials.point", 1, format(selection.first)));
        this.addLabel(x, y + 32, -1, 12, 0xFFFFBB55, StringUtils.translate("area_materials.point", 2, format(selection.second)));
        this.addLabel(x, y + 52, -1, 12, 0xFFBBBBBB, StringUtils.translate("area_materials.mode_hint"));
        this.addLabel(x, y + 64, -1, 12, 0xFFBBBBBB, StringUtils.translate("area_materials.cycle_hint"));
        ButtonGeneric list = button(x, y + 76, buttonWidth, "area_materials.show", () -> AreaMaterialsClient.openList(placement, this, false));
        list.setEnabled(selection.bounds() != null && placement.isEnabled());
        ButtonGeneric visible = button(x, y + 100, buttonWidth, "area_materials.visible.show", () -> AreaMaterialsClient.openList(placement, this, true));
        visible.setEnabled(selection.bounds() != null && placement.isEnabled());
        visible.setHoverStrings(StringUtils.translate("area_materials.visible.hover").split("\n"));
        button(x, y + 124, buttonWidth, "area_materials.clear", () -> { AreaMaterialsClient.clear(placement); initGui(); });
        button(x, y + 148, buttonWidth, "area_materials.back", () -> GuiBase.openGui(this.getParent()));
        // Useful for selecting an internal corner in empty space.
        button(x + buttonWidth + 8, y + 18, 82, "area_materials.here1", () -> {
            if (this.mc.player != null) AreaMaterialsClient.setCorner(placement, this.mc.player.getBlockPos(), true);
            initGui();
        });
        button(x + buttonWidth + 8, y + 42, 82, "area_materials.here2", () -> {
            if (this.mc.player != null) AreaMaterialsClient.setCorner(placement, this.mc.player.getBlockPos(), false);
            initGui();
        });
        this.addLabel(x, y + 178, -1, 12, 0xFFBBBBBB, StringUtils.translate("area_materials.help1"));
        this.addLabel(x, y + 192, -1, 12, 0xFFBBBBBB, StringUtils.translate("area_materials.help2"));
    }

    private ButtonGeneric button(int x, int y, int width, String key, Runnable action) {
        return this.addButton(new ButtonGeneric(x, y, width, 20, StringUtils.translate(key)), (b, mouseButton) -> action.run());
    }

    public static String format(net.minecraft.util.math.BlockPos pos) {
        return pos == null ? "—" : pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
}
