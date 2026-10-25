package dev.areamaterials;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.util.StringUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public final class AreaMaterialList extends MaterialListBase {
    private final SchematicPlacement placement;
    private final IntBox bounds;
    private CountJob job;

    public AreaMaterialList(SchematicPlacement placement, IntBox bounds) {
        this.placement = placement;
        this.bounds = bounds;
    }

    @Override public String getName() { return placement.getName() + " [" + bounds.minX() + ", " + bounds.minY() + ", " + bounds.minZ() + "]"; }
    @Override public String getTitle() { return StringUtils.translate("area_materials.list.title", placement.getName()); }

    @Override public void reCreateMaterialList() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        job = new CountJob(PlacementSnapshot.capture(placement), mc.world);
        AreaMaterialsClient.queue(this);
    }

    public boolean tick(MinecraftClient mc) {
        if (job == null) return true;
        if (mc.world != job.world || mc.player == null || !placement.isEnabled()
                || !DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().contains(placement)) {
            job = null;
            return true;
        }
        if (!job.snapshot.layout().equals(PlacementSnapshot.capture(placement).layout())) {
            AreaMaterialsClient.message("area_materials.changed");
            job = null;
            return true;
        }
        if (!job.step()) return false;
        setMaterialListEntries(MaterialListUtils.getMaterialList(job.total, job.missing, job.mismatch, mc.player));
        job = null;
        AreaMaterialsClient.message("area_materials.complete");
        return true;
    }

    public String progress() {
        if (job == null) return "";
        return job.waiting ? StringUtils.translate("area_materials.waiting", job.x >> 4, job.z >> 4)
                : StringUtils.translate("area_materials.progress", job.size == 0 ? 100 : job.done * 100 / job.size);
    }

    private final class CountJob {
        final PlacementSnapshot snapshot;
        final ClientWorld world;
        final List<IntBox> boxes;
        final Object2IntOpenHashMap<BlockState> total = new Object2IntOpenHashMap<>();
        final Object2IntOpenHashMap<BlockState> missing = new Object2IntOpenHashMap<>();
        final Object2IntOpenHashMap<BlockState> mismatch = new Object2IntOpenHashMap<>();
        final boolean ignoreState = Configs.Generic.MATERIAL_LIST_IGNORE_STATE.getBooleanValue();
        final long size;
        int boxIndex, x, y, z;
        long done;
        boolean waiting;
        final BlockPos.Mutable pos = new BlockPos.Mutable();

        CountJob(PlacementSnapshot snapshot, ClientWorld world) {
            this.snapshot = snapshot;
            this.world = world;
            boxes = snapshot.clippedBoxes(bounds, world.getBottomY(), world.getBottomY() + world.getHeight());
            size = boxes.stream().mapToLong(IntBox::volume).sum();
            resetCursor();
        }

        void resetCursor() {
            if (boxIndex < boxes.size()) {
                IntBox box = boxes.get(boxIndex);
                x = box.minX(); y = box.minY(); z = box.minZ();
            }
        }

        boolean step() {
            waiting = false;
            // Both a block budget and a time budget keep large selections responsive.
            long deadline = System.nanoTime() + 3_000_000L;
            int budget = 8192;
            while (boxIndex < boxes.size() && budget-- > 0 && System.nanoTime() < deadline) {
                if (!world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) { waiting = true; return false; }
                pos.set(x, y, z);
                BlockState expected = snapshot.stateAt(pos);
                if (!expected.isAir()) {
                    total.addTo(expected, 1);
                    BlockState actual = world.getBlockState(pos);
                    if (actual.isAir()) missing.addTo(expected, 1);
                    else if (actual != expected && (!ignoreState || actual.getBlock() != expected.getBlock())) {
                        missing.addTo(expected, 1);
                        mismatch.addTo(expected, 1);
                    }
                }
                done++;
                IntBox box = boxes.get(boxIndex);
                if (++x > box.maxX()) {
                    x = box.minX();
                    if (++z > box.maxZ()) {
                        z = box.minZ();
                        if (++y > box.maxY()) { boxIndex++; resetCursor(); }
                    }
                }
            }
            return boxIndex >= boxes.size();
        }
    }
}
