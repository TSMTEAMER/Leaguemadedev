package kassuk.addon.blackout.mixins;

import kassuk.addon.blackout.modules.render.SwingModifier;
import kassuk.addon.blackout.modules.movement.TickShift;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value={ClientPlayerEntity.class}, priority=1001)
public abstract class MixinClientPlayerEntity {
    @Shadow
    @Final
    public ClientPlayNetworkHandler networkHandler;
    private static boolean sent = false;

    @Inject(method={"swingHand(Lnet/minecraft/util/Hand;)V"}, at={@At(value="HEAD")})
    private void swingHand(Hand hand, CallbackInfo ci) {
        ((SwingModifier)Modules.get().get(SwingModifier.class)).startSwing(hand);
    }

    @Inject(method={"sendMovementPackets"}, at={@At(value="HEAD")})
    private void sendPacketsHead(CallbackInfo ci) {
        sent = false;
    }

    @Inject(method={"sendMovementPackets"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V")})
    private void onSendPacket(CallbackInfo ci) {
        sent = true;
    }

    @Inject(method={"sendMovementPackets"}, at={@At(value="TAIL")})
    private void sendPacketsTail(CallbackInfo ci) {
        TickShift tickShift;
        if (!sent && (tickShift = (TickShift)Modules.get().get(TickShift.class)).isActive()) {
            tickShift.unSent = Math.min((Integer)tickShift.packets.get(), tickShift.unSent + 1);
        }
    }
}
