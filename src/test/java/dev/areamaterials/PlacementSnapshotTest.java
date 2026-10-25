package dev.areamaterials;

import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.util.PositionUtils;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PlacementSnapshotTest {
    @BeforeAll static void bootstrap() { SharedConstants.createGameVersion(); Bootstrap.initialize(); }

    @Test void appliesMixinsToAllLitematicaIntegrationPoints() throws ClassNotFoundException {
        Class.forName("fi.dy.masa.litematica.gui.GuiPlacementConfiguration");
        Class.forName("fi.dy.masa.litematica.event.KeyCallbacks$KeyCallbackHotkeys");
        Class.forName("fi.dy.masa.litematica.event.InputHandler");
        assertTrue(dev.areamaterials.mixin.GuiBaseAccessor.class.isAssignableFrom(fi.dy.masa.malilib.gui.GuiBase.class));
    }

    @Test void samplesSignedRegionsWithEveryPlacementAndSubregionTransform() {
        var container = new LitematicaBlockStateContainer(3, 2, 4);
        var state = Blocks.FURNACE.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH);
        container.set(2, 1, 3, state);
        for (int signX : new int[]{-1, 1}) for (int signY : new int[]{-1, 1}) for (int signZ : new int[]{-1, 1})
            for (BlockMirror mainMirror : BlockMirror.values()) for (BlockRotation mainRotation : BlockRotation.values())
                for (BlockMirror subMirror : BlockMirror.values()) for (BlockRotation subRotation : BlockRotation.values()) {
                    BlockPos min = new BlockPos(signX < 0 ? -2 : 0, signY < 0 ? -1 : 0, signZ < 0 ? -3 : 0);
                    BlockPos origin = new BlockPos(-31, 80, 17);
                    BlockPos offset = new BlockPos(2, 1, 3).add(min);
                    BlockPos world = PositionUtils.getTransformedBlockPos(offset, mainMirror, mainRotation);
                    world = PositionUtils.getTransformedBlockPos(world, subMirror, subRotation).add(origin);
                    BlockMirror stateMirror = subMirror;
                    if (subMirror != BlockMirror.NONE && (mainRotation == BlockRotation.CLOCKWISE_90 || mainRotation == BlockRotation.COUNTERCLOCKWISE_90))
                        stateMirror = subMirror == BlockMirror.FRONT_BACK ? BlockMirror.LEFT_RIGHT : BlockMirror.FRONT_BACK;
                    BlockRotation combined = mainRotation.rotate(subRotation);
                    var region = new PlacementSnapshot.Region(new IntBox(-100, -100, -100, 100, 200, 100), origin, min,
                            container, mainMirror, mainRotation, subMirror, subRotation, stateMirror, combined);
                    assertSame(state.mirror(mainMirror).mirror(stateMirror).rotate(combined), region.stateAt(world));
                }
    }

    @Test void clipsSelectionToRegionAndWorldHeight() {
        var container = new LitematicaBlockStateContainer(3, 2, 4);
        var region = new PlacementSnapshot.Region(new IntBox(-2, -70, 4, 2, -60, 8), BlockPos.ORIGIN, BlockPos.ORIGIN,
                container, BlockMirror.NONE, BlockRotation.NONE, BlockMirror.NONE, BlockRotation.NONE, BlockMirror.NONE, BlockRotation.NONE);
        var snapshot = new PlacementSnapshot(List.of(region), "");
        var clipped = snapshot.clippedBoxes(new IntBox(-1, -80, 3, 1, -62, 6), -64, 320);
        assertEquals(List.of(new IntBox(-1, -64, 4, 1, -62, 6)), clipped);
        assertTrue(snapshot.clippedBoxes(new IntBox(100, 0, 100, 101, 1, 101), -64, 320).isEmpty());
    }

    @Test void overlappingRegionsCountOnePositionAndIgnoreAirAndStructureVoid() {
        var a = new LitematicaBlockStateContainer(1, 1, 1);
        var b = new LitematicaBlockStateContainer(1, 1, 1);
        a.set(0,0,0,Blocks.STONE.getDefaultState());
        var box = new IntBox(0,0,0,0,0,0);
        var ra = new PlacementSnapshot.Region(box,BlockPos.ORIGIN,BlockPos.ORIGIN,a,BlockMirror.NONE,BlockRotation.NONE,BlockMirror.NONE,BlockRotation.NONE,BlockMirror.NONE,BlockRotation.NONE);
        var rb = new PlacementSnapshot.Region(box,BlockPos.ORIGIN,BlockPos.ORIGIN,b,BlockMirror.NONE,BlockRotation.NONE,BlockMirror.NONE,BlockRotation.NONE,BlockMirror.NONE,BlockRotation.NONE);
        var snapshot = new PlacementSnapshot(List.of(ra,rb), "");
        assertSame(Blocks.STONE.getDefaultState(), snapshot.stateAt(BlockPos.ORIGIN));
        b.set(0,0,0,Blocks.STRUCTURE_VOID.getDefaultState());
        assertSame(Blocks.STONE.getDefaultState(), snapshot.stateAt(BlockPos.ORIGIN));
        b.set(0,0,0,Blocks.GOLD_BLOCK.getDefaultState());
        assertSame(Blocks.GOLD_BLOCK.getDefaultState(), snapshot.stateAt(BlockPos.ORIGIN));
        assertEquals(1, snapshot.clippedBoxes(box,-64,320).stream().mapToLong(IntBox::volume).sum());
    }
}
