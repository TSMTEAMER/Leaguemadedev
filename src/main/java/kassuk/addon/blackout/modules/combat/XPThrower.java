package kassuk.addon.blackout.modules.combat;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.utils.bana.BEntityUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.EnchantedGoldenAppleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import static kassuk.addon.blackout.utils.bana.TimerUtils.getTPSMatch;

public class XPThrower extends Module {
    public enum SwitchMode {
        Normal,
        Silent,
    }


    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlayer = settings.createGroup("Player");
    private final SettingGroup sgPause = settings.createGroup("Pause");


    // General
    public final Setting<Keybind> throwBind = sgGeneral.add(new KeybindSetting.Builder()
            .name("keybind")
            .description("The keybind to throw XP.")
            .defaultValue(Keybind.none())
            .build()
    );

    public final Setting<Boolean> justThrow = sgGeneral.add(new BoolSetting.Builder()
            .name("force-throw")
            .description("Throw XP even if your items are fully repaired.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Boolean> replenish = sgGeneral.add(new BoolSetting.Builder()
            .name("replenish")
            .description("Automatically move XP into your hotbar.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> hotbarSlot = sgGeneral.add(new IntSetting.Builder()
            .name("hotbar-slot")
            .description("Which hotbar slot to move the XP to.")
            .defaultValue(5)
            .range(1,9)
            .sliderRange(1,9)
            .visible(replenish::get)
            .build()
    );

    private final Setting<Integer> maxThreshold = sgGeneral.add(new IntSetting.Builder()
            .name("max-durability")
            .description("The maximum durability to repair items to.")
            .defaultValue(80)
            .range(1,100)
            .sliderRange(1,100)
            .visible(()-> !justThrow.get())
            .build()
    );

    public final Setting<Boolean> repairHeld = sgGeneral.add(new BoolSetting.Builder()
            .name("repair-held")
            .description("Repairs the item you are holding in your main hand.")
            .defaultValue(false)
            .build()
    );


    // Player
    private final Setting<SwitchMode> autoSwitch = sgPlayer.add(new EnumSetting.Builder<SwitchMode> ()
            .name("auto-switch")
            .description("How to switch to XP.")
            .defaultValue(SwitchMode.Silent)
            .build()
    );

    private final Setting<Boolean> noGapSwitch = sgPlayer.add(new BoolSetting.Builder()
            .name("no-gap-switch")
            .description("Whether to switch to XP if you're holding a gap.")
            .defaultValue(true)
            .visible(() -> autoSwitch.get() == SwitchMode.Normal)
            .build()
    );

    private final Setting<Integer> throwDelay = sgPlayer.add(new IntSetting.Builder()
            .name("throw-delay")
            .description("How fast to throw XP.")
            .defaultValue(1)
            .range(0,20)
            .sliderRange(0,20)
            .build()
    );

    private final Setting<Boolean> tpsSync = sgPlayer.add(new BoolSetting.Builder()
            .name("TPS-sync")
            .description("Syncs the throw delay with the server's TPS.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> lookDown = sgPlayer.add(new BoolSetting.Builder()
            .name("look-down")
            .description("Forces you to rotate downwards when throwing XP.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> onlyOnGround = sgPlayer.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Only activate when you are on the ground.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> onlyInHole = sgPlayer.add(new BoolSetting.Builder()
            .name("only-in-hole")
            .description("Only activate when you are in a hole.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> allowDoubles = sgPlayer.add(new BoolSetting.Builder()
            .name("allow-doubles")
            .description("Allows double holes to count as holes.")
            .defaultValue(false)
            .visible(onlyInHole::get)
            .build()
    );


    //Pause
    private final Setting<Boolean> eatPause = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-eat")
            .description("Whether to pause while eating.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> drinkPause = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-drink")
            .description("Whether to pause while eating.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> minePause = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-mine")
            .description("Whether to pause while eating.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> minHealth = sgPause.add(new DoubleSetting.Builder()
            .name("min-health")
            .description("How much health you must have to throw XP.")
            .defaultValue(10)
            .range(1,36)
            .sliderRange(1,36)
            .build()
    );


    public XPThrower() {
        super(BlackOut.COMBATPLUS, "XP-thrower", "Throw XP bottles to repair your armor and tools.");
    }


    private int delay = 0;
    public boolean isRepairing;

    @Override
    public void onActivate() {
        delay = 0;
        isRepairing = false;
    }

    @Override
    public void onDeactivate() {
        isRepairing = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Decrement throw delay
        if (delay > 0) delay--;

        // If the player is not in a suitable place to repair, return
        if(shouldWait()) return;

        // Activate if the bind is pressed or if a piece of armor is below the threshold
        if (throwBind.get().isPressed() && mc.currentScreen == null && (!isRepaired() || justThrow.get())) {
            isRepairing = true;

            FindItemResult XP = InvUtils.find(Items.EXPERIENCE_BOTTLE);
            FindItemResult hotbarXP = InvUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);

            // Make sure that the player has XP, otherwise return / toggle off
            if (XP.found()) {

                if (!hotbarXP.found()) {
                    if (!replenish.get()) return;
                    InvUtils.move().from(XP.slot()).toHotbar(hotbarSlot.get() - 1);
                }

                // <= Because the way TPS sync does math sometimes causes the delay to go below zero
                if (delay <= 0) {
                    if (lookDown.get()) Rotations.rotate(mc.player.getYaw(), 90, () -> throwXP(hotbarXP));
                    else throwXP(hotbarXP);

                    // Reset throw delay
                    delay = (int) (throwDelay.get() / getTPSMatch(tpsSync.get()));
                }
            }
        } else isRepairing = false;
    }

    public boolean isRepaired() {
        ItemStack helmet = mc.player.getInventory().getArmorStack(3);
        ItemStack chestplate = mc.player.getInventory().getArmorStack(2);
        ItemStack leggings = mc.player.getInventory().getArmorStack(1);
        ItemStack boots = mc.player.getInventory().getArmorStack(0);
        ItemStack tool = mc.player.getMainHandStack();

        boolean helmetRepaired;
        boolean chestplateRepaired;
        boolean leggingsRepaired;
        boolean bootsRepaired;
        boolean toolsRepaired;

        if (EnchantmentHelper.getLevel(Enchantments.MENDING, helmet) > 0) {
            helmetRepaired = (float) (helmet.getMaxDamage() - helmet.getDamage()) / helmet.getMaxDamage() * 100 >= maxThreshold.get();
        } else helmetRepaired = true;

        if (EnchantmentHelper.getLevel(Enchantments.MENDING, chestplate) > 0) {
            chestplateRepaired = (float) (chestplate.getMaxDamage() - chestplate.getDamage()) / chestplate.getMaxDamage() * 100 >= maxThreshold.get();
        } else chestplateRepaired = true;

        if (EnchantmentHelper.getLevel(Enchantments.MENDING, leggings) > 0) {
            leggingsRepaired = (float) (leggings.getMaxDamage() - leggings.getDamage()) / leggings.getMaxDamage() * 100 >= maxThreshold.get();
        } else leggingsRepaired = true;

        if (EnchantmentHelper.getLevel(Enchantments.MENDING, boots) > 0) {
            bootsRepaired = (float) (boots.getMaxDamage() - boots.getDamage()) / boots.getMaxDamage() * 100 >= maxThreshold.get();
        } else bootsRepaired = true;

        if (repairHeld.get()) {
            if (EnchantmentHelper.getLevel(Enchantments.MENDING, tool) > 0) {
                toolsRepaired = (float) (tool.getMaxDamage() - tool.getDamage()) / tool.getMaxDamage() * 100 >= maxThreshold.get();
            } else toolsRepaired = true;
        } else toolsRepaired = true;

        return helmetRepaired && chestplateRepaired && leggingsRepaired && bootsRepaired && toolsRepaired;
    }

    private boolean shouldWait() {
        if (PlayerUtils.shouldPause(minePause.get(), drinkPause.get(), eatPause.get())) return true;

        if (onlyOnGround.get() && !mc.player.isOnGround()) return true;
        if (allowDoubles.get()  && !BEntityUtils.isInHole(mc.player, allowDoubles.get(), BEntityUtils.BlastResistantType.Any)) return true;
        return (PlayerUtils.getTotalHealth() <= minHealth.get());
    }

    private void throwXP(FindItemResult hotbarExp) {
        int prevSlot = mc.player.getInventory().selectedSlot;

        // If it's in the offhand just use that and don't switch
        if (hotbarExp.isOffhand()) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
        } else {
            // Switching on the mainhand
            if (autoSwitch.get() == SwitchMode.Silent || !(noGapSwitch.get() && mc.player.getMainHandStack().getItem() instanceof EnchantedGoldenAppleItem)) {
                InvUtils.swap(hotbarExp.slot(), false);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                if (autoSwitch.get() == SwitchMode.Silent) InvUtils.swap(prevSlot, false);
            }
        }
    }
}
