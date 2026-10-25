package dev.areamaterials.mixin;

import dev.areamaterials.AreaMaterialsClient;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "fi.dy.masa.litematica.event.KeyCallbacks$KeyCallbackHotkeys", remap = false)
public abstract class ToolCallbackMixin {
    @Inject(method = "onKeyAction", at = @At("HEAD"), cancellable = true)
    private void areaMaterials$selectCorner(KeyAction action, IKeybind key, CallbackInfoReturnable<Boolean> cir) {
        if (AreaMaterialsClient.handleToolKey(key)) cir.setReturnValue(true);
    }
}
