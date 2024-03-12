package kassuk.addon.blackout.mixins;

import kassuk.addon.blackout.themes.MercuryGuiTheme;
import meteordevelopment.meteorclient.gui.GuiTheme;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiTheme.class)
public class GuiThemeMixin {
    @Shadow
    @Final
    @Mutable
    public String name;

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lmeteordevelopment/meteorclient/gui/GuiTheme;name:Ljava/lang/String;"))
    private void rename(GuiTheme guiTheme, String value) {
        name = value;
        if (guiTheme instanceof MercuryGuiTheme) name = "Mercury";
    }

}
