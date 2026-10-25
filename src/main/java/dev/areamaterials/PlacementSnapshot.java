package dev.areamaterials;

import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.PositionUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Reads only the chosen schematic, even if another placement overlaps it. */
public record PlacementSnapshot(List<Region> regions, String layout) {
    public static PlacementSnapshot capture(SchematicPlacement placement) {
        var regions = new ArrayList<Region>();
        StringBuilder layout = new StringBuilder().append(placement.getOrigin()).append(placement.getMirror())
                .append(placement.getRotation()).append(placement.isEnabled());
        var boxes = placement.getSubRegionBoxes(SubRegionPlacement.RequiredEnabled.PLACEMENT_ENABLED);
        var schematic = placement.getSchematic();
        for (String name : boxes.keySet().stream().sorted().toList()) {
            var sub = placement.getRelativeSubRegionPlacement(name);
            var size = schematic.getAreaSize(name);
            var container = schematic.getSubRegionContainer(name);
            if (sub == null || size == null || container == null) continue;
            BlockPos offset = PositionUtils.getTransformedBlockPos(sub.getPos(), placement.getMirror(), placement.getRotation())
                    .add(placement.getOrigin());
            BlockPos minOffset = new BlockPos(Math.min(0, size.getX() + 1), Math.min(0, size.getY() + 1), Math.min(0, size.getZ() + 1));
            var box = boxes.get(name);
            BlockMirror subStateMirror = sub.getMirror();
            if (subStateMirror != BlockMirror.NONE && (placement.getRotation() == BlockRotation.CLOCKWISE_90
                    || placement.getRotation() == BlockRotation.COUNTERCLOCKWISE_90)) {
                subStateMirror = subStateMirror == BlockMirror.FRONT_BACK ? BlockMirror.LEFT_RIGHT : BlockMirror.FRONT_BACK;
            }
            regions.add(new Region(bounds(box.getPos1(), box.getPos2()), offset, minOffset, container,
                    placement.getMirror(), placement.getRotation(), sub.getMirror(), sub.getRotation(),
                    subStateMirror, placement.getRotation().rotate(sub.getRotation())));
            layout.append(name).append(sub.getPos()).append(sub.getMirror()).append(sub.getRotation()).append(size);
        }
        return new PlacementSnapshot(List.copyOf(regions), layout.toString());
    }

    public static IntBox bounds(BlockPos a, BlockPos b) {
        return IntBox.between(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
    }

    public BlockState stateAt(BlockPos worldPos) {
        // A stable order for overlapping subregions. Air and structure void never hide solid blocks.
        for (int i = regions.size() - 1; i >= 0; i--) {
            Region region = regions.get(i);
            if (!region.bounds.contains(worldPos.getX(), worldPos.getY(), worldPos.getZ())) continue;
            BlockState state = region.stateAt(worldPos);
            if (!state.isAir() && !state.isOf(Blocks.STRUCTURE_VOID)) return state;
        }
        return Blocks.AIR.getDefaultState();
    }

    public List<IntBox> clippedBoxes(IntBox selection, int bottomY, int topY) {
        IntBox worldHeight = new IntBox(selection.minX(), bottomY, selection.minZ(), selection.maxX(), topY - 1, selection.maxZ());
        IntBox withinWorld = selection.intersection(worldHeight);
        if (withinWorld == null) return List.of();
        List<IntBox> boxes = new ArrayList<>();
        for (Region region : regions) {
            IntBox clipped = region.bounds.intersection(withinWorld);
            if (clipped != null) boxes.add(clipped);
        }
        return IntBox.union(boxes);
    }

    public record Region(IntBox bounds, BlockPos worldOrigin, BlockPos minOffset,
                         LitematicaBlockStateContainer container, BlockMirror mainMirror, BlockRotation mainRotation,
                         BlockMirror subMirror, BlockRotation subRotation, BlockMirror subStateMirror,
                         BlockRotation combinedRotation) {
        public BlockState stateAt(BlockPos worldPos) {
            BlockPos local = PositionUtils.getReverseTransformedBlockPos(worldPos.subtract(worldOrigin), subMirror, subRotation);
            local = PositionUtils.getReverseTransformedBlockPos(local, mainMirror, mainRotation).subtract(minOffset);
            var size = container.getSize();
            if (local.getX() < 0 || local.getY() < 0 || local.getZ() < 0 || local.getX() >= size.getX()
                    || local.getY() >= size.getY() || local.getZ() >= size.getZ()) return Blocks.AIR.getDefaultState();
            BlockState state = container.get(local.getX(), local.getY(), local.getZ());
            return state.mirror(mainMirror).mirror(subStateMirror).rotate(combinedRotation);
        }
    }
}
