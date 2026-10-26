package dev.areamaterials;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** Potentially visible surfaces of the finished placement, independent of the player's camera. */
public final class VisibleMaterialFilter {
    private static final Direction[] SIDES = Direction.values();
    private final PlacementSnapshot snapshot;
    private final BlockPos.Mutable neighborPos = new BlockPos.Mutable();

    public VisibleMaterialFilter(PlacementSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public boolean isVisible(BlockState state, BlockPos pos) {
        for (Direction side : SIDES) {
            neighborPos.set(pos, side);
            // Read the whole chosen placement: the selection boundary is not a cut in the building.
            BlockState neighbor = snapshot.stateAt(neighborPos);
            // Leaves can have holes in their texture even when graphics settings cull some faces.
            if (neighbor.getBlock() instanceof LeavesBlock || Block.shouldDrawSide(state, neighbor, side)) {
                return true;
            }
        }
        return false;
    }
}
