package dev.areamaterials;

import dev.areamaterials.mixin.AxeItemAccessor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Oxidizable;
import net.minecraft.item.HoneycombItem;

import java.util.IdentityHashMap;
import java.util.Map;

/** Explicit vanilla treatment families; unrelated materials and different shapes stay distinct. */
public final class BlockVariantFamilies {
    private BlockVariantFamilies() {}

    public static boolean isRelatedVariant(BlockState expected, BlockState actual) {
        Block wanted = expected.getBlock();
        Block placed = actual.getBlock();
        if (wanted == placed) return false;
        return Families.BASE_BLOCKS.getOrDefault(wanted, wanted) == Families.BASE_BLOCKS.getOrDefault(placed, placed);
    }

    private static final class Families {
        // Immutable after construction: Litematica also classifies overlays on its chunk worker threads.
        static final Map<Block, Block> BASE_BLOCKS = create();

        private static Map<Block, Block> create() {
            Map<Block, Block> result = new IdentityHashMap<>();
            // The game's oxidation chains preserve block/slab/stair/etc. form automatically.
            Oxidizable.OXIDATION_LEVEL_INCREASES.get().forEach((before, after) -> {
                result.put(before, Oxidizable.getUnaffectedOxidationBlock(before));
                result.put(after, Oxidizable.getUnaffectedOxidationBlock(after));
            });
            HoneycombItem.WAXED_TO_UNWAXED_BLOCKS.get().forEach((waxed, unwaxed) ->
                    result.put(waxed, Oxidizable.getUnaffectedOxidationBlock(unwaxed)));
            // Includes logs, wood, stems, hyphae and bamboo, without mixing species or forms.
            AxeItemAccessor.areaMaterials$getStrippedBlocks().forEach((original, stripped) ->
                    result.put(stripped, original));
            return Map.copyOf(result);
        }
    }
}
