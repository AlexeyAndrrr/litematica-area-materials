package dev.areamaterials;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;

public final class AreaRenderer implements IRenderer {
    private static final Color4f FIRST = new Color4f(0.2f, 0.95f, 0.85f, 1f);
    private static final Color4f SECOND = new Color4f(1f, 0.65f, 0.15f, 1f);

    @Override public void onRenderWorldLast(Matrix4f position, Matrix4f projection) {
        var mc = MinecraftClient.getInstance();
        var placement = AreaMaterialsClient.activePlacement();
        if (placement == null || mc.world == null || mc.player == null || !Configs.Visuals.ENABLE_RENDERING.getBooleanValue()) return;
        var selection = AreaMaterialsClient.selection(placement);
        if (selection.first != null) RenderUtils.renderBlockOutline(selection.first, 0.003f, 2f, FIRST);
        if (selection.second != null) RenderUtils.renderBlockOutline(selection.second, 0.003f, 2f, SECOND);
        if (selection.bounds() != null) RenderUtils.renderAreaOutline(selection.first, selection.second, 2f, FIRST, FIRST, FIRST);
    }

    @Override public void onRenderGameOverlayPost(GuiContext context) {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        var placement = AreaMaterialsClient.activePlacement();
        if (placement != null && mc.currentScreen == null) {
            var selection = AreaMaterialsClient.selection(placement);
            draw(context, StringUtils.translate("area_materials.hud"), 8, 0xFF58E2D3);
            draw(context, "1: " + AreaMenu.format(selection.first) + "   2: " + AreaMenu.format(selection.second), 21, 0xFFFFFFFF);
        }
        String progress = AreaMaterialsClient.progress();
        if (!progress.isEmpty()) draw(context, progress, mc.getWindow().getScaledHeight() - 52, 0xFFFFCC66);
    }

    private void draw(GuiContext context, String text, int y, int color) {
        var font = MinecraftClient.getInstance().textRenderer;
        context.fill(6, y - 2, 12 + font.getWidth(text), y + 11, 0x99000000);
        context.drawTextWithShadow(font, text, 9, y, color);
    }
}
