package kassuk.addon.blackout.modules.combat;

import kassuk.addon.blackout.BlackOut;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.Burrow;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.screen.slot.SlotActionType;

public class OffHando extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder().name("health").description("Swaps item in offhand if your health lower than specified.").defaultValue(12).sliderRange(1,36).build());
    private final Setting<ItemList> items = sgGeneral.add(new EnumSetting.Builder<ItemList>().name("items").description("The item which you will put in offhand slot.").defaultValue(ItemList.Crystal).build());
    private final Setting<Boolean> damageCheck = sgGeneral.add(new BoolSetting.Builder().name("damage-check").description("Takes totem in your offhand if crystal is spawned nearly of you.").defaultValue(true).build());
    private final Setting<Boolean> crystalOnCA = sgGeneral.add(new BoolSetting.Builder().name("crystal-on-CA").description("Takes crystal in your offhand if Crystal Aura is turned on.").defaultValue(true).build());
    private final Setting<Boolean> egapOnSword = sgGeneral.add(new BoolSetting.Builder().name("egap-on-sword").description("Takes gap in your offhand if you holding swordwwwwwwwwww.").defaultValue(false).build());
    public final Setting<Boolean> anvilOnBurrow = sgGeneral.add(new BoolSetting.Builder().name("anvil-on-burrow").description("Takes anvil in your offhand if Burrow is turned on. Works only with Burrow Plus").defaultValue(true).build());

    public OffHando() {
        super(BlackOut.COMBATPLUS, "off-hando", "Automatically puts items in your offhand.");
    }

    private Item itemTarget = null;
    private boolean hasTotem = false;
    private boolean rightClick = false;
    private int swapBackSlot = -1;

    @Override
    public void onActivate() {
        itemTarget = null;
        hasTotem = false;
        rightClick = false;
        swapBackSlot = -1;

    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (!mc.isOnThread()) return;

        if (mc.player.playerScreenHandler != mc.player.currentScreenHandler || mc.currentScreen instanceof AbstractInventoryScreen || mc.player.isCreative())
            return;

        if (!hasTotem) {
            itemTarget = getItemTarget();
            if (itemTarget == mc.player.getOffHandStack().getItem()) {
                itemTarget = null;
            }
        }

        if (itemTarget == null) {
            if (swapBackSlot != -1 && mc.player.getOffHandStack().getItem() == Items.ENCHANTED_GOLDEN_APPLE && !mc.options.useKey.isPressed()) {
                mc.interactionManager.clickSlot(0, 45, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(0, swapBackSlot, 0, SlotActionType.PICKUP, mc.player);
                if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                    mc.interactionManager.clickSlot(0, 45, 0, SlotActionType.PICKUP, mc.player);
                }
                swapBackSlot = -1;
            }
            return;
        }

        if (mc.player.currentScreenHandler.getCursorStack().getItem() != itemTarget) {
            int index = 44;
            while (index >= 9) {
                if (mc.player.getInventory().getStack(index >= 36 ? index - 36 : index).getItem() == itemTarget) {
                    hasTotem = true;
                    mc.interactionManager.clickSlot(0, index, 0, SlotActionType.PICKUP, mc.player);
                    if (rightClick) {
                        rightClick = false;
                        swapBackSlot = index;
                    } else {
                        swapBackSlot = -1;
                    }
                }
                index--;
            }
        }

        if (mc.player.currentScreenHandler.getCursorStack().getItem() == itemTarget) {
            mc.interactionManager.clickSlot(0, 45, 0, SlotActionType.PICKUP, mc.player);
            if (mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                hasTotem = false;
                return;
            }
        }

        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty() && mc.player.getOffHandStack().getItem() == itemTarget) {
            int index = SlotUtils.OFFHAND - 1;
            while (index >= 9) {
                if (mc.player.getInventory().getStack(index >= 36 ? index - 36 : index).isEmpty()) {
                    if (mc.player.currentScreenHandler.getCursorStack().getItem() != itemTarget) {
                        mc.interactionManager.clickSlot(0, index, 0, SlotActionType.PICKUP, mc.player);
                        hasTotem = false;
                        if (rightClick) {
                            rightClick = false;
                            swapBackSlot = index;
                        } else {
                            swapBackSlot = -1;
                        }
                    }
                }
                index--;
            }
        }
    }

    private net.minecraft.item.Item getItemTarget() {
        if (EntityUtils.getTotalHealth(mc.player) <= health.get()) return getItem(ItemList.Totem);

        if (damageCheck.get()) {
            if (mc.player.fallDistance > 8F) return getItem(ItemList.Totem);

            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EndCrystalEntity && entity.distanceTo(mc.player) < 6) {
                    if (EntityUtils.getTotalHealth(mc.player) - DamageUtils.crystalDamage(mc.player, Utils.vec3d(entity.getBlockPos())) <= health.get()) {
                        return getItem(ItemList.Totem);
                    }
                }
            }

            for (BlockEntity blockEntity : Utils.blockEntities()) {
                if (blockEntity instanceof BedBlockEntity && PlayerUtils.distanceTo(blockEntity.getPos()) < 6) {
                    if (EntityUtils.getTotalHealth(mc.player) - DamageUtils.bedDamage(mc.player, Utils.vec3d(blockEntity.getPos())) <= health.get()) {
                        return getItem(ItemList.Totem);
                    }
                }
            }
        }

        if (anvilOnBurrow.get() && Modules.get().get(Burrow.class).isActive()) return getItem(ItemList.Anvil);
        if (egapOnSword.get() && mc.player.getMainHandStack().getItem() instanceof SwordItem) return getItem(ItemList.EGap);
        if (crystalOnCA.get() && Modules.get().get(AutoCrystal.class).isActive()) return getItem(ItemList.Crystal);

        return switch (items.get()) {
            case Totem -> getItem(ItemList.Totem);
            case EGap -> getItem(ItemList.EGap);
            case Crystal -> getItem(ItemList.Crystal);
            case Gap -> getItem(ItemList.Gap);
            case Exp -> getItem(ItemList.Exp);
            default -> throw new IllegalStateException("Unexpected value: " + items.get());
        };
    }

    private net.minecraft.item.Item getItem(ItemList item) {
        switch (item) {
            case Totem: return Items.TOTEM_OF_UNDYING;
            case EGap: return  Items.ENCHANTED_GOLDEN_APPLE;
            case Gap: return Items.GOLDEN_APPLE;
            case Crystal: return Items.END_CRYSTAL;
            case Exp: return Items.EXPERIENCE_BOTTLE;
            case Anvil: {
                if (InvUtils.find(Items.ANVIL).found()) return Items.ANVIL;
                if (InvUtils.find(Items.DAMAGED_ANVIL).found()) return Items.DAMAGED_ANVIL;
                if (InvUtils.find(Items.CHIPPED_ANVIL).found()) return Items.CHIPPED_ANVIL;
            }
        }

        return null;
    }

    public enum ItemList {
        Totem,
        EGap,
        Gap,
        Crystal,
        Exp,
        Anvil
    }
}
