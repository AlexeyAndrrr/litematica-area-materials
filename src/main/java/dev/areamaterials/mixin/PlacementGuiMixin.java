package dev.areamaterials.mixin;

import dev.areamaterials.AreaMaterialsClient;
import fi.dy.masa.litematica.gui.GuiPlacementConfiguration;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GuiPlacementConfiguration.class, remap = false)
public abstract class PlacementGuiMixin extends GuiBase {
    @Shadow @Final public SchematicPlacement placement;
    @Unique private ButtonBase areaMaterials$materials;
    @Unique private ButtonBase areaMaterials$verifier;

    @Inject(method = "createButton", at = @At("RETURN"))
    private void areaMaterials$rememberButtons(int x, int y, int width, GuiPlacementConfiguration.ButtonListener.Type type,
                                              CallbackInfoReturnable<Integer> cir) {
        var buttons = ((GuiBaseAccessor) this).areaMaterials$getButtons();
        if (type == GuiPlacementConfiguration.ButtonListener.Type.OPEN_MATERIAL_LIST_GUI) areaMaterials$materials = buttons.getLast();
        if (type == GuiPlacementConfiguration.ButtonListener.Type.OPEN_VERIFIER_GUI) areaMaterials$verifier = buttons.getLast();
    }

    @Inject(method = "getBrowserHeight", at = @At("RETURN"), cancellable = true)
    private void areaMaterials$reserveBottomRow(CallbackInfoReturnable<Integer> cir) {
        if (this.height < 350) cir.setReturnValue(cir.getReturnValue() - 24);
    }

    @Inject(method = "initGui", at = @At("TAIL"))
    private void areaMaterials$addAreaButton(CallbackInfo ci) {
        ButtonBase verifier = areaMaterials$verifier;
        ButtonBase materials = areaMaterials$materials;
        if (verifier == null || materials == null) return;
        var buttons = ((GuiBaseAccessor) this).areaMaterials$getButtons();
        // Upstream adds the return-to-placements button last.
        ButtonBase back = buttons.getLast();
        String label = StringUtils.translate("area_materials.button");
        ButtonGeneric area;
        if (this.height < 350) {
            materials.setX(10); materials.setY(this.height - 46);
            verifier.setX(11 + materials.getWidth()); verifier.setY(this.height - 46);
            area = new ButtonGeneric(10, this.height - 22, -1, 20, label);
            back.setX(11 + area.getWidth()); back.setY(this.height - 22);
        } else {
            area = new ButtonGeneric(verifier.getX(), verifier.getY() + 21, verifier.getWidth(), 20, label);
            back.setY(Math.max(back.getY(), area.getY() + 32));
        }
        area.setHoverStrings("area_materials.button.hover");
        this.addButton(area, (button, mouseButton) -> AreaMaterialsClient.openMenu(placement, this));
    }
}
