package dev.areamaterials.mixin;

import dev.areamaterials.BlockVariantFamilies;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;
import fi.dy.masa.litematica.util.OverlayType;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChunkRendererSchematicVbo.class, remap = false)
public abstract class SchematicOverlayMixin {
    @Inject(method = "getOverlayType", at = @At("HEAD"), cancellable = true)
    private void areaMaterials$highlightRelatedVariants(BlockState expected, BlockState actual,
                                                        CallbackInfoReturnable<OverlayType> cir) {
        if (BlockVariantFamilies.isRelatedVariant(expected, actual)) {
            // Reuse both the user's wrong-state color and its visibility toggle, including edge rendering.
            cir.setReturnValue(OverlayType.WRONG_STATE);
        }
    }
}
