package kassuk.addon.blackout.modules.combat;

import kassuk.addon.blackout.BlackOut;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.potion.PotionUtil;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class QQuiver extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<StatusEffect>> effects = sgGeneral.add(new StatusEffectListSetting.Builder().name("effects").description("Which effects to shoot you with.").defaultValue(StatusEffects.STRENGTH).build());
    private final Setting<Boolean> lowMode = sgGeneral.add(new BoolSetting.Builder().name("low-mode").description("Works only w/ 1-2 types of arrows.").defaultValue(true).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("release-interval").defaultValue(4).min(0).max(7).sliderRange(0, 7).build());
    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder().name("only-on-ground").defaultValue(true).build());
    private final Setting<Boolean> checkEffects = sgGeneral.add(new BoolSetting.Builder().name("check-existing-effects").description("Won't shoot you with effects you already have.").defaultValue(true).build());
    private final Setting<Boolean> silentBow = sgGeneral.add(new BoolSetting.Builder().name("silent-bow").defaultValue(true).build());

    private final List<Integer> arrowSlots = new ArrayList<>();
    Timer afterTimer = new Timer();
    int interval;
    int prevBowSlot;

    public QQuiver() {
        super(BlackOut.COMBATPLUS, "QQuiver", "");
    }

    @Override
    public void onActivate() {
        interval = 0;
        prevBowSlot = -1;

        FindItemResult bow = InvUtils.find(Items.BOW);

        if (!bow.found()) {
            toggle();
        }

        if (silentBow.get() && !bow.isHotbar()) {
            prevBowSlot = bow.slot();
            InvUtils.move().from(bow.slot()).to(mc.player.getInventory().selectedSlot);
        } else if (!bow.isHotbar()) {
            ChatUtils.error("No bow in inventory found.");
            toggle();
        }

        mc.options.useKey.setPressed(false);
        mc.interactionManager.stopUsingItem(mc.player);

        if (!silentBow.get()) InvUtils.swap(bow.slot(), true);

        arrowSlots.clear();

        List<StatusEffect> usedEffects = new ArrayList<>();

        for (int i = mc.player.getInventory().size(); i > 0; i--) {
            if (i == mc.player.getInventory().selectedSlot) continue;

            ItemStack item = mc.player.getInventory().getStack(i);

            if (item.getItem() != Items.TIPPED_ARROW) continue;

            List<StatusEffectInstance> effects = PotionUtil.getPotionEffects(item);

            if (effects.isEmpty()) continue;

            StatusEffect effect = effects.get(0).getEffectType();

            if (this.effects.get().contains(effect)
                    && !usedEffects.contains(effect)
                    && (!hasEffect(effect) || !checkEffects.get())) {
                usedEffects.add(effect);
                arrowSlots.add(i);
            }
        }
    }

    private boolean hasEffect(StatusEffect effect) {
        for (StatusEffectInstance statusEffect : mc.player.getStatusEffects()) {
            if (statusEffect.getEffectType() == effect) return true;
        }

        return false;
    }

    @Override
    public void onDeactivate() {
        if (lowMode.get()) mc.options.sneakKey.setPressed(false);
        if (silentBow.get() && prevBowSlot != -1)
            InvUtils.move().from(mc.player.getInventory().selectedSlot).to(prevBowSlot);
        else
            InvUtils.swapBack();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (onlyOnGround.get() && !mc.player.isOnGround()) return;
        if (arrowSlots.isEmpty() || !InvUtils.findInHotbar(Items.BOW).isMainHand()) {
            return;
        }

        interval--;
        if (interval > 0) return;

        boolean charging = mc.options.useKey.isPressed();
        double charge = lowMode.get() ? 0.1 : 0.12;

        if (!charging) {
            InvUtils.move().from(arrowSlots.get(0)).to(9);
            mc.options.useKey.setPressed(true);
        } else {
            if (BowItem.getPullProgress(mc.player.getItemUseTime()) >= charge) {
                int targetSlot = arrowSlots.get(0);
                arrowSlots.remove(0);

                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(mc.player.getYaw(), -90, mc.player.isOnGround()));
                mc.options.useKey.setPressed(false);
                mc.interactionManager.stopUsingItem(mc.player);
                if (targetSlot != 9) InvUtils.move().from(9).to(targetSlot);
                if (arrowSlots.isEmpty() && lowMode.get()) mc.options.sneakKey.setPressed(true);
                interval = delay.get();
            }
        }
    }
}
