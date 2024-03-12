package kassuk.addon.blackout.modules.combat;

import kassuk.addon.blackout.BlackOut;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;

public class BowBomb extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> bows = sgGeneral.add(new BoolSetting.Builder().name("bows").description("Allows to use exploit with bows.").defaultValue(true).build());
    private final Setting<Boolean> pearls = sgGeneral.add(new BoolSetting.Builder().name("pearls").description("Allows to use exploit with pearls.").defaultValue(true).build());
    private final Setting<Integer> timeout = sgGeneral.add(new IntSetting.Builder().name("timeout").min(0).max(20000).sliderMin(100).sliderMax(20000).defaultValue(5000).build());
    private final Setting<Integer> spoofs = sgGeneral.add(new IntSetting.Builder().name("spoofs").description("How many packets will sended while you shooting.").min(0).max(20000).sliderMin(100).sliderMax(20000).defaultValue(5000).build());
    private final Setting<Boolean> bypass = sgGeneral.add(new BoolSetting.Builder().name("bypass").description("Uses reverse exploit logic.").defaultValue(false).build());

    public BowBomb() {
        super(BlackOut.COMBATPLUS, "bow-bomb", "One tapping by using bow exploit.");
    }

    private boolean shooting;
    private long lastShootTime;

    @Override
    public void onActivate() {
        shooting = false;
        lastShootTime = System.currentTimeMillis();
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerActionC2SPacket packet) {
            if (packet.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
                ItemStack handStack = mc.player.getStackInHand(Hand.MAIN_HAND);

                if (!handStack.isEmpty() && handStack.getItem() != null && handStack.getItem() instanceof BowItem && bows.get()) {
                    doSpoofs();
                }
            }

        } else if (event.packet instanceof PlayerInteractItemC2SPacket packet2) {
            if (packet2.getHand() == Hand.MAIN_HAND) {
                ItemStack handStack = mc.player.getStackInHand(Hand.MAIN_HAND);

                if (!handStack.isEmpty() && handStack.getItem() != null) {
                    if (handStack.getItem() instanceof EnderPearlItem && pearls.get()) {
                        doSpoofs();
                    }
                }
            }
        }
    }

    private void doSpoofs() {
        if (System.currentTimeMillis() - lastShootTime >= timeout.get()) {
            shooting = true;
            lastShootTime = System.currentTimeMillis();

            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));

            for (int i = 0; i < spoofs.get(); i++) {
                if (bypass.get()) {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1e-10, mc.player.getZ(), false));
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 1e-10, mc.player.getZ(), true));
                } else {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 1e-10, mc.player.getZ(), true));
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1e-10, mc.player.getZ(), false));
                }
            }

            shooting = false;
        }
    }
}
