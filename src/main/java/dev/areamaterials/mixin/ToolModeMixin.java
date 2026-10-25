package dev.areamaterials.mixin;

import fi.dy.masa.litematica.tool.ToolMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(value = ToolMode.class, remap = false)
public abstract class ToolModeMixin {
    @Shadow @Final @Mutable private static ToolMode[] $VALUES;

    @Invoker("<init>")
    private static ToolMode areaMaterials$create(String name, int ordinal, String translationKey,
                                                boolean creativeOnly, boolean usesSchematic) {
        throw new AssertionError("Mixin constructor invoker");
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void areaMaterials$appendTenthMode(CallbackInfo ci) {
        int ordinal = $VALUES.length;
        ToolMode mode = areaMaterials$create("AREA_MATERIALS", ordinal, "area_materials.mode", false, true);
        ToolMode[] modes = Arrays.copyOf($VALUES, ordinal + 1);
        modes[ordinal] = mode;
        $VALUES = modes;
    }
}
