package dev.areamaterials;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.event.TickHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class AreaMaterialsClient implements ClientModInitializer {
    private static final Map<SchematicPlacement, Selection> SELECTIONS = new WeakHashMap<>();
    private static final List<AreaMaterialList> JOBS = new ArrayList<>();
    private static ClientWorld currentWorld;

    @Override public void onInitializeClient() {
        InitializationHandler.getInstance().registerInitializationHandler(() -> {
            AreaRenderer renderer = new AreaRenderer();
            RenderEventHandler.getInstance().registerWorldLastRenderer(renderer);
            RenderEventHandler.getInstance().registerGameOverlayRenderer(renderer);
            TickHandler.getInstance().registerClientTickHandler(AreaMaterialsClient::tick);
        });
    }

    public static Selection selection(SchematicPlacement placement) {
        Selection selection = SELECTIONS.computeIfAbsent(placement, p -> new Selection());
        String layout = PlacementSnapshot.capture(placement).layout();
        if (!layout.equals(selection.layout)) {
            selection.first = null;
            selection.second = null;
            selection.layout = layout;
        }
        return selection;
    }

    public static boolean selectionModeActive() {
        return DataManager.getToolMode() == AreaToolMode.get();
    }

    public static SchematicPlacement activePlacement() {
        if (!selectionModeActive()) return null;
        SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
        return placement != null && placement.isEnabled() ? placement : null;
    }

    public static boolean toolActive() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return selectionModeActive() && mc.world != null && mc.player != null && mc.currentScreen == null
                && Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue() && Configs.Visuals.ENABLE_RENDERING.getBooleanValue()
                && EntityUtils.hasToolItem(mc.player);
    }

    public static boolean handleToolKey(IKeybind key) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (selectionModeActive() && mc.player != null && EntityUtils.hasToolItem(mc.player)
                && (key == Hotkeys.PICK_BLOCK_FIRST.getKeybind() || key == Hotkeys.PICK_BLOCK_LAST.getKeybind())) return true;
        if (!toolActive()) return false;
        boolean first = key == Hotkeys.TOOL_PLACE_CORNER_1.getKeybind();
        boolean second = key == Hotkeys.TOOL_PLACE_CORNER_2.getKeybind();
        // Consume Litematica's element-selection action without opening a screen or moving the placement.
        if (key == Hotkeys.TOOL_SELECT_ELEMENTS.getKeybind()) {
            return true;
        }
        if (!first && !second) return false;
        SchematicPlacement placement = activePlacement();
        if (placement == null) {
            message("area_materials.no_placement");
            return true;
        }
        // Hit the schematic itself, including ghost blocks with no real block underneath.
        var trace = RayTraceUtils.traceToSchematicWorld(mc.player, 200, true, false);
        if (trace == null || trace.getType() != HitResult.Type.BLOCK) {
            message("area_materials.no_target");
            return true;
        }
        setCorner(placement, trace.getBlockPos(), first);
        return true;
    }

    public static void setCorner(SchematicPlacement placement, BlockPos pos, boolean first) {
        var snapshot = PlacementSnapshot.capture(placement);
        if (snapshot.regions().stream().noneMatch(r -> r.bounds().contains(pos.getX(), pos.getY(), pos.getZ()))) {
            message("area_materials.outside");
            return;
        }
        Selection selection = selection(placement);
        if (first) selection.first = pos.toImmutable(); else selection.second = pos.toImmutable();
        message("area_materials.corner", first ? 1 : 2, pos.getX(), pos.getY(), pos.getZ());
    }

    public static void clear(SchematicPlacement placement) {
        SELECTIONS.remove(placement);
        message("area_materials.cleared");
    }

    public static void openMenu(SchematicPlacement placement, Screen parent) {
        AreaMenu menu = new AreaMenu(placement);
        menu.setParent(parent);
        GuiBase.openGui(menu);
    }

    public static void openList(SchematicPlacement placement, Screen parent, boolean visibleOnly) {
        Selection selection = selection(placement);
        if (!placement.isEnabled()) { message("area_materials.placement_disabled"); return; }
        if (selection.bounds() == null) { message("area_materials.incomplete"); return; }
        AreaMaterialList materials = new AreaMaterialList(placement, selection.bounds(), visibleOnly);
        GuiMaterialList gui = new GuiMaterialList(materials);
        gui.setParent(parent);
        GuiBase.openGui(gui);
        materials.reCreateMaterialList();
    }

    public static void queue(AreaMaterialList list) {
        JOBS.clear();
        JOBS.add(list);
    }

    private static void tick(MinecraftClient mc) {
        if (currentWorld != mc.world) {
            currentWorld = mc.world;
            SELECTIONS.clear();
            JOBS.clear();
        }
        // Only one job runs per tick; repeated refresh replaces its own pending calculation.
        if (!JOBS.isEmpty() && JOBS.getFirst().tick(mc)) JOBS.removeFirst();
    }

    public static String progress() { return JOBS.isEmpty() ? "" : JOBS.getFirst().progress(); }

    public static void message(String key, Object... args) {
        var player = MinecraftClient.getInstance().player;
        if (player != null) player.sendMessage(Text.translatable(key, args), true);
    }

    public static final class Selection {
        public BlockPos first;
        public BlockPos second;
        String layout = "";
        public IntBox bounds() { return first == null || second == null ? null : PlacementSnapshot.bounds(first, second); }
    }
}
