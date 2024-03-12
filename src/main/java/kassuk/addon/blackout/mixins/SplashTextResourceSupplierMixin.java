package kassuk.addon.blackout.mixins;

import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.resource.SplashTextResourceSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SplashTextResourceSupplier.class)
public class SplashTextResourceSupplierMixin {
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void walper(CallbackInfoReturnable<SplashTextRenderer> cir) {
        cir.setReturnValue(new SplashTextRenderer("Trayvon.us best client aswell as ricin.cc"));
    }
}
