package dev.areamaterials.mixin;

import dev.areamaterials.AreaMaterialsClient;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.event.InputHandler;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = InputHandler.class, remap = false)
public abstract class InputHandlerMixin {
    @Inject(method = {"handleAttackKey", "handleUseKey"}, at = @At("HEAD"), cancellable = true)
    private void areaMaterials$consumeToolAction(MinecraftClient mc, CallbackInfoReturnable<Boolean> cir) {
        if (AreaMaterialsClient.toolActive()) cir.setReturnValue(true);
    }

    @Inject(method = "handleMouseScroll", at = @At("HEAD"), cancellable = true)
    private void areaMaterials$protectPlacement(double amount, MinecraftClient mc, CallbackInfoReturnable<Boolean> cir) {
        if (!AreaMaterialsClient.toolActive()) return;
        if (Hotkeys.OPERATION_MODE_CHANGE_MODIFIER.getKeybind().isKeybindHeld()) {
            DataManager.setToolMode(DataManager.getToolMode().cycle(mc.player, amount < 0));
            cir.setReturnValue(true);
        }
        else cir.setReturnValue(false);
    }
}
