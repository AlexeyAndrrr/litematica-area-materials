package dev.areamaterials.mixin;

import fi.dy.masa.litematica.tool.ToolMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(value = ToolMode.class, remap = false)
public abstract class ToolModeMixin {
    @Shadow @Final @Mutable private static ToolMode[] $VALUES;

    @Invoker("<init>")
    private static ToolMode areaMaterials$create(String name, int ordinal, String configName, String translationKey,
                                                boolean creativeOnly, boolean usesSchematic) {
        throw new AssertionError("Mixin constructor invoker");
    }

    // Register before upstream builds its serialization codec and immutable mode list.
    @Inject(method = "<clinit>", at = @At(value = "FIELD",
            target = "Lfi/dy/masa/litematica/tool/ToolMode;$VALUES:[Lfi/dy/masa/litematica/tool/ToolMode;",
            opcode = Opcodes.PUTSTATIC, shift = At.Shift.AFTER))
    private static void areaMaterials$appendTenthMode(CallbackInfo ci) {
        int ordinal = $VALUES.length;
        ToolMode mode = areaMaterials$create("AREA_MATERIALS", ordinal, "area_materials", "area_materials.mode", false, true);
        ToolMode[] modes = Arrays.copyOf($VALUES, ordinal + 1);
        modes[ordinal] = mode;
        $VALUES = modes;
    }
}
