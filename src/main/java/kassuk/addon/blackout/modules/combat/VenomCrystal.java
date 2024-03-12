package kassuk.addon.blackout.modules.combat;

import com.google.common.util.concurrent.AtomicDouble;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.utils.meteorplus.CaUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class VenomCrystal extends Module {
    public enum Mode {Safe, Suicide}
    public enum RotationMode {None, Place, Break, Both}
    public enum SwitchMode {None, Auto}
    public enum Logic {PlaceBreak, BreakPlace}
    public enum Canceller {NoDesync, HitCanceller}
    public enum Type {None, Place, Break, Both}
    public enum BreakHand {Mainhand, Offhand, Auto}

    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgMisc = settings.createGroup("Misc");
    private final SettingGroup sgTarget = settings.createGroup("Target");
    private final SettingGroup sgFacePlace = settings.createGroup("FacePlace");
    private final SettingGroup sgSupport = settings.createGroup("Support");
    private final SettingGroup sgSurround = settings.createGroup("Surround");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgSwitch = settings.createGroup("Switch");
    private final SettingGroup sgRotations = settings.createGroup("Rotations");
    private final SettingGroup sgExperimental = settings.createGroup("Experimental");
    private final SettingGroup sgRender = settings.createGroup("Render");
    // Misc
    private final Setting<Type> antiFriendPop = sgMisc.add(new EnumSetting.Builder<Type>().name("anti-friend-pop").description("Avoids popping your friends.").defaultValue(Type.Both).build());
    private final Setting<Boolean> crystalSave = sgMisc.add(new BoolSetting.Builder().name("crystal-saver").description("Only targets players that can get hurt.").defaultValue(false).build());
    private final Setting<Logic> orderLogic = sgMisc.add(new EnumSetting.Builder<Logic>().name("Logic").description("What to do first.").defaultValue(Logic.BreakPlace).build());
    private final Setting<Boolean> antiWeakness = sgMisc.add(new BoolSetting.Builder().name("anti-weakness").description("Switches to tools to break crystals instead of your fist.").defaultValue(true).build());
    public final Setting<Boolean> oldMode = sgMisc.add(new BoolSetting.Builder().name("1.12-mode").description("Won't place in 1 high holes and enables walls options.").defaultValue(false).build());
    private final Setting<Type> rayTrace = sgMisc.add(new EnumSetting.Builder<Type>().name("ray-trace").description("Wont place / break through walls when on.").visible(oldMode::get).defaultValue(Type.None).build());
    // Place
    private final Setting<Mode> placeMode = sgPlace.add(new EnumSetting.Builder<Mode>().name("place-mode").description("The placement mode for crystals.").defaultValue(Mode.Safe).build());
    private final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder().name("place-delay").description("The amount of delay in ticks before placing.").defaultValue(1).min(0).sliderMax(10).build());
    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder().name("place-range").description("The radius in which crystals can be placed in.").defaultValue(5).min(0).sliderMax(6).build());
    private final Setting<Double> placeWallsRange = sgPlace.add(new DoubleSetting.Builder().name("walls-range").description("The radius in which crystals can be placed through walls.").visible(() -> rayTrace.get() != Type.Place && rayTrace.get() != Type.Both && oldMode.get()).defaultValue(5).min(0).sliderMax(6).build());
    private final Setting<Double> verticalRange = sgPlace.add(new DoubleSetting.Builder().name("vertical-range").description("Vertical place range.").defaultValue(5).min(0).sliderMax(6).build());
    private final Setting<Double> minPlaceDamage = sgPlace.add(new DoubleSetting.Builder().name("min-damage").description("The minimum damage the crystal will place.").defaultValue(6).build());
    private final Setting<Double> maxPlaceDamage = sgPlace.add(new DoubleSetting.Builder().name("max-damage").description("The maximum self damage the crystal will place.").visible(() -> placeMode.get() == Mode.Safe).defaultValue(2).build());
    private final Setting<Double> torque = sgPlace.add(new DoubleSetting.Builder().name("torque").description("Defines how lethal the placements are; With 0 being ultra careful and 1 completely ignoring self damage.").visible(() -> placeMode.get() == Mode.Suicide).defaultValue(1).min(0).sliderMax(1).max(1).build());
    private final Setting<Boolean> inBreakRange = sgPlace.add(new BoolSetting.Builder().name("within-break-range").description("Will only place when the spawned crystal is within break range.").defaultValue(false).build());
    // Break
    private final Setting<Mode> breakMode = sgBreak.add(new EnumSetting.Builder<Mode>().name("break-mode").description("The type of break mode for crystals.").defaultValue(Mode.Safe).build());
    private final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder().name("break-delay").description("The amount of delay in ticks before breaking.").defaultValue(1).min(0).sliderMax(10).build());
    private final Setting<Double> minBreakDamage = sgBreak.add(new DoubleSetting.Builder().name("min-damage").description("The minimum damage for a crystal to get broken.").defaultValue(4.5).min(0).sliderMax(36).build());
    private final Setting<Double> maxBreakDamage = sgBreak.add(new DoubleSetting.Builder().name("max-self-damage").description("The maximum self-damage allowed.").visible(() -> breakMode.get() == Mode.Safe).defaultValue(3).sliderMax(36).build());
    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder().name("break-range").description("The maximum range that crystals can be to be broken.").defaultValue(5).min(0).sliderMax(6).build());
    private final Setting<Double> breakWallsRange = sgBreak.add(new DoubleSetting.Builder().name("walls-range").description("The maximum range that crystals can be to be broken through walls.").visible(() -> rayTrace.get() != Type.Break && rayTrace.get() != Type.Both && oldMode.get()).defaultValue(5).min(0).sliderMax(6).build());
    private final Setting<BreakHand> breakHand = sgBreak.add(new EnumSetting.Builder<BreakHand>().name("hand").description("Which hand to swing for breaking.").defaultValue(BreakHand.Auto).build());
    private final Setting<Integer> breakAttempts = sgBreak.add(new IntSetting.Builder().name("break-attempts").description("How many times to hit a crystal before stopping to target it.").defaultValue(2).sliderMin(1).sliderMax(10).build());
    private final Setting<Canceller> removeCrystals = sgBreak.add(new EnumSetting.Builder<Canceller>().name("canceller").description("Hitcanceller is the fastest but might cause desync on strict anticheats.").defaultValue(Canceller.NoDesync).build());
    private final Setting<Integer> minAge = sgBreak.add(new IntSetting.Builder().name("minimum-crystal-age").description("How ticks a crystal has to exist in order to consider it.").defaultValue(0).sliderMax(4).build());
    // Target
    public final Setting<Double> targetRange = sgTarget.add(new DoubleSetting.Builder().name("target-range").description("The maximum range the entity can be to be targeted.").defaultValue(10).min(0).sliderMax(15).build());
    public final Setting<Boolean> predict = sgTarget.add(new BoolSetting.Builder().name("predict").description("Predicts target movement.").defaultValue(false).build());
    public final Setting<Boolean> ignoreTerrain = sgTarget.add(new BoolSetting.Builder().name("ignore-terrain").description("Ignores non blast resistant blocks in damage calcs (useful during terrain pvp).").defaultValue(true).build());
    private final Setting<Integer> numberOfDamages = sgTarget.add(new IntSetting.Builder().name("number-of-targets").description("Maximum number of targets to perform calculations on. Might lag your game when set too high.").defaultValue(3).sliderMin(1).sliderMax(5).build());
    //Faceplace
    private final Setting<Boolean> facePlace = sgFacePlace.add(new BoolSetting.Builder().name("face-place").description("Will face-place when target is below a certain health or armor durability threshold.").defaultValue(true).build());
    private final Setting<Double> facePlaceHealth = sgFacePlace.add(new DoubleSetting.Builder().name("health").description("The health the target has to be at to start faceplacing.").visible(facePlace::get).defaultValue(12).min(1).sliderMin(1).sliderMax(36).build());
    private final Setting<Double> facePlaceDurability = sgFacePlace.add(new DoubleSetting.Builder().name("durability").description("The durability threshold to be able to face-place.").visible(facePlace::get).defaultValue(10).min(1).sliderMin(1).sliderMax(100).max(100).build());
    private final Setting<Boolean> facePlaceSelf = sgFacePlace.add(new BoolSetting.Builder().name("face-place-self").description("Whether to faceplace when you are in the same hole as your target.").visible(facePlace::get).defaultValue(true).build());
    private final Setting<Boolean> facePlaceHole = sgFacePlace.add(new BoolSetting.Builder().name("hole-fags").description("Automatically starts faceplacing surrounded or burrowed targets.").visible(facePlace::get).defaultValue(false).build());
    private final Setting<Boolean> facePlaceArmor = sgFacePlace.add(new BoolSetting.Builder().name("missing-armor").description("Automatically starts faceplacing when a target misses a piece of armor.").visible(facePlace::get).defaultValue(true).build());
    private final Setting<Keybind> forceFacePlace = sgFacePlace.add(new KeybindSetting.Builder().name("force-face-place").description("Starts faceplacing when this button is pressed").visible(facePlace::get).defaultValue(Keybind.fromKey(-1)).build());
    private final Setting<Boolean> pauseSword = sgFacePlace.add(new BoolSetting.Builder().name("pause-when-swording").description("Doesnt faceplace when you are holding a sword.").visible(facePlace::get).defaultValue(true).build());
    //Support
    private final Setting<Boolean> support = sgSupport.add(new BoolSetting.Builder().name("support").description("Places a block in the air and crystals on it. Helps with killing players that are flying.").defaultValue(false).build());
    private final Setting<Integer> supportDelay = sgSupport.add(new IntSetting.Builder().name("support-delay").description("The delay between support blocks being placed.").visible(support::get).defaultValue(5).min(0).sliderMax(10).build());
    private final Setting<Boolean> supportBackup = sgSupport.add(new BoolSetting.Builder().name("support-backup").description("Makes it so support only works if there are no other options.").visible(support::get).defaultValue(true).build());
    private final Setting<Boolean> supportAirPlace = sgSupport.add(new BoolSetting.Builder().name("airplace").description("Whether to airplace the support block or not.").defaultValue(true).visible(support::get).build());
    //Surround
    private final Setting<Boolean> surroundHold = sgSurround.add(new BoolSetting.Builder().name("surround-hold").description("Places a crystal next to a player so they cannot use Surround.").defaultValue(true).build());
    private final Setting<Boolean> surroundBreak = sgSurround.add(new BoolSetting.Builder().name("surround-break").description("Places a crystal next to a surrounded player and keeps it there so they cannot use Surround again.").defaultValue(false).build());
    private final Setting<Boolean> surroundPickaxe = sgSurround.add(new BoolSetting.Builder().name("only-pickaxe").description("Will only attempt to surround break while you hold a pickaxe").visible(surroundBreak::get).defaultValue(false).build());
    private final Setting<Boolean> antiSurroundBreak = sgSurround.add(new BoolSetting.Builder().name("anti-surround-break").description("Breaks crystals that could surround break you.").defaultValue(false).build());
    // Pause
    private final Setting<Type> pauseMode = sgPause.add(new EnumSetting.Builder<Type>().name("pause-mode").description("What to pause.").defaultValue(Type.None).build());
    private final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder().name("pause-on-eat").description("Pauses Crystal Aura while eating.").visible(() -> pauseMode.get() != Type.None).defaultValue(false).build());
    private final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder().name("pause-on-drink").description("Pauses Crystal Aura while drinking a potion.").visible(() -> pauseMode.get() != Type.None).defaultValue(false).build());
    private final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder().name("pause-on-mine").description("Pauses Crystal Aura while mining blocks.").visible(() -> pauseMode.get() != Type.None).defaultValue(false).build());
    private final Setting<Boolean> facePlacePause = sgPause.add(new BoolSetting.Builder().name("pause-face-placing").description("When to interrupt face-placing.").visible(() -> pauseMode.get() != Type.None).defaultValue(false).build());
    private final Setting<Boolean> facePlacePauseEat = sgPause.add(new BoolSetting.Builder().name("fp-pause-on-eat").description("Pauses face placing while eating.").visible(facePlacePause::get).defaultValue(false).build());
    private final Setting<Boolean> facePlacePauseDrink = sgPause.add(new BoolSetting.Builder().name("fp-pause-on-drink").description("Pauses face placing while drinking.").visible(facePlacePause::get).defaultValue(false).build());
    private final Setting<Boolean> facePlacePauseMine = sgPause.add(new BoolSetting.Builder().name("fp-pause-on-mine").description("Pauses face placing while mining.").visible(facePlacePause::get).defaultValue(false).build());
    //Switch
    private final Setting<SwitchMode> switchMode = sgSwitch.add(new EnumSetting.Builder<SwitchMode>().name("switch-mode").description("How to switch items.").defaultValue(SwitchMode.Auto).build());
    private final Setting<Boolean> switchBack = sgSwitch.add(new BoolSetting.Builder().name("switch-back").description("Switches back to your previous slot when disabling Crystal Aura.").defaultValue(false).build());
    private final Setting<Integer> switchDelay = sgSwitch.add(new IntSetting.Builder().name("switch-delay").description("The amount of delay in ticks before switching slots again.").defaultValue(1).min(0).sliderMax(5).build());
    private final Setting<Boolean> noFoodSwitch = sgSwitch.add(new BoolSetting.Builder().name("no-food-switch").description("Won't switch when eating food. Useful when using with anti-weakness or mainhanding.").defaultValue(false).build());
    private final Setting<Integer> switchHealth = sgSwitch.add(new IntSetting.Builder().name("switch-health").description("The health to stop switching to crystals.").min(0).sliderMax(20).defaultValue(0).build());
    // Rotations
    private final Setting<RotationMode> rotationMode = sgRotations.add(new EnumSetting.Builder<RotationMode>().name("rotation-mode").description("The method of rotating when using Crystal Aura.").defaultValue(RotationMode.None).build());
    private final Setting<Boolean> strictLook = sgRotations.add(new BoolSetting.Builder().name("strict-look").description("Looks at exactly where you're placing.").visible(() -> rotationMode.get() != RotationMode.None).defaultValue(false).build());
    //Experimental
    private final Setting<Boolean> debugTest = sgExperimental.add(new BoolSetting.Builder().name("debug-text").description("debug").defaultValue(false).build());
    // Render
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").description("Renders your swing client-side.").defaultValue(true).build());
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders the block under where it is placing a crystal.").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").visible(render::get).defaultValue(ShapeMode.Sides).build());
    private final Setting<Integer> renderTime = sgRender.add(new IntSetting.Builder().name("render-time").description("The amount of time between changing the block render.").visible(render::get).defaultValue(1).min(0).sliderMax(5).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color.").visible(render::get).defaultValue(new SettingColor(255, 0, 0, 75, true)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color.").visible(render::get).defaultValue(new SettingColor(255, 0, 0, 200)).build());
    private final Setting<Boolean> renderDamage = sgRender.add(new BoolSetting.Builder().name("render-damage").description("Renders the damage of the crystal where it is placing.").defaultValue(true).build());
    private final Setting<Integer> roundDamage = sgRender.add(new IntSetting.Builder().name("round-damage").description("Round damage to x decimal places.").visible(renderDamage::get).defaultValue(2).min(0).max(3).sliderMax(3).build());
    private final Setting<Double> damageScale = sgRender.add(new DoubleSetting.Builder().name("damage-scale").description("The scale of the damage text.").visible(renderDamage::get).defaultValue(1.4).min(0).sliderMax(5).build());
    private final Setting<SettingColor> damageColor = sgRender.add(new ColorSetting.Builder().name("damage-color").description("The color of the damage text.").visible(renderDamage::get).defaultValue(new SettingColor(0, 0, 0, 255)).build());

    public VenomCrystal() {
        super(BlackOut.COMBATPLUS, "venom-crystal", "Auto crystal made by tyrannus00. (still in the middle of porting will crash)");
    }

    private byte fails;
    private Vec3d playerPos;
    public static Vec3d eyeHeight;
    private BlockPos lastBlock;
    private Item mItem, oItem;
    public static float ticksBehind;
    private float lastDamage;
    private boolean switched, isUsing, weak;
    private int supportSlot, preSlot, placeDelayLeft, breakDelayLeft, switchDelayLeft, supportDelayLeft, lastEntityId, dif;
    private FindItemResult supportSlotResult;
    private final Modules modules = Modules.get();
    private LivingEntity target, closestTarget;
    private List<LivingEntity> targets = new ArrayList<>();
    private List<PlayerEntity> friends = new ArrayList<>();
    private List<EndCrystalEntity> crystals = new ArrayList<>(), attemptedCrystals = new ArrayList<>();
    private final IntSet entitiesToRemove = new IntOpenHashSet();
    private final Int2IntMap attemptedBreaks = new Int2IntOpenHashMap();

    @Override
    public void onActivate() {
        placeDelayLeft = 0; breakDelayLeft = 0; supportDelayLeft = 0; switchDelayLeft = 0;
        supportSlot = -1; preSlot = -1;
        supportSlotResult = null;
        weak = false;
        fails = 0;
        lastDamage = 0;
        lastBlock = null;
        target = null;
        friends.clear();
        crystals.clear();
        targets.clear();
        attemptedBreaks.clear();
    }

    @Override
    public void onDeactivate() {
        if(switchBack.get() && preSlot != -1 && switchDelayLeft <= 0) mc.player.getInventory().selectedSlot = preSlot;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(SendMovementPacketsEvent.Pre event) {
        placeDelayLeft--;
        breakDelayLeft--;
        supportDelayLeft--;
        switchDelayLeft--;
        if(mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null) ticksBehind = (float) mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency() / (50 * (20 / TickRate.INSTANCE.getTickRate()));         //50ms is the time it takes for 1 tick to pass under 20tps
        if(TickRate.INSTANCE.getTimeSinceLastTick() >= 1f) attemptedBreaks.clear();     //This is to prevent lag spikes from fucking your hit attempts

        getEntities();      //Gets targets, friends and crystals
        if(targets.isEmpty()) return;

        playerPos = mc.player.getPos();
        eyeHeight = new Vec3d(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        mItem = mc.player.getMainHandStack().getItem();
        oItem = mc.player.getOffHandStack().getItem();
        isUsing = mc.player.isUsingItem();

        Map<StatusEffect, StatusEffectInstance> effects = mc.player.getActiveStatusEffects();   //Anti weakness stuff
        weak = false;
        if(!effects.isEmpty()) {
            boolean strong = false;
            if(effects.containsKey(StatusEffects.STRENGTH) && effects.get(StatusEffects.STRENGTH).getAmplifier() == 1) strong = true;   //You can destroy crystals with your bare hands if you have strength 2 with weakness 1
            if(effects.containsKey(StatusEffects.WEAKNESS)) weak = effects.get(StatusEffects.WEAKNESS).getAmplifier() == 1 || !strong;
        }
        if(fails > renderTime.get()) {
            lastBlock = null;
        }

        switch (orderLogic.get()) {
            case BreakPlace: {
                doBreak();
                doPlace();
                break;
            }
            case PlaceBreak: {
                doPlace();
                doBreak();
                break;
            }
        }
    }

    private void getEntities() {
        targets.clear();
        friends.clear();
        crystals.clear();

        // Players
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.getAbilities().creativeMode || player == mc.player) continue;

            if (!player.isDead() && player.isAlive() && Friends.get().shouldAttack(player) && player.distanceTo(mc.player) <= targetRange.get()) {
                if (targets.size() < numberOfDamages.get()) {
                    targets.add(player);
                    if (closestTarget == null || mc.player.distanceTo(player) < mc.player.distanceTo(closestTarget))
                        closestTarget = player;
                }
            }
        }

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity && mc.player.distanceTo(entity) <= breakRange.get()) {
                crystals.add((EndCrystalEntity) entity);
            }
        }

        // Fake players
        for (PlayerEntity player : FakePlayerManager.getFakePlayers()) {
            if (!player.isDead() && player.isAlive() && Friends.get().shouldAttack(player) && player.distanceTo(mc.player) <= targetRange.get()) {
                targets.add(player);
            }
        }
    }
    private void doPlace() {
        if (this.placeDelayLeft > 0) {
            return;
        }
        if (this.oItem != Items.END_CRYSTAL && this.mItem != Items.END_CRYSTAL) {
            if (this.switchMode.get() == SwitchMode.None) {
                this.fails = (byte)(this.fails + 1);
                return;
            }
            if (this.noFoodSwitch.get().booleanValue() && this.isUsing && (this.mItem.isFood() || this.oItem.isFood())) {
                this.fails = (byte)(this.fails + 1);
                return;
            }
            int slot = InvUtils.findInHotbar(Items.END_CRYSTAL).slot();
            if (slot < 0 || slot > 8) {
                this.fails = (byte)(this.fails + 1);
                return;
            }
        }
        if (this.pauseMode.get() == Type.Place || this.pauseMode.get() == Type.Both) {
            if (this.isUsing && (((Boolean)this.pauseOnDrink.get()).booleanValue() && (this.mItem instanceof PotionItem || this.oItem instanceof PotionItem) || this.pauseOnEat.get().booleanValue() && (this.mItem.isFood() || this.oItem.isFood()))) {
                this.fails = (byte)(this.fails + 1);
                return;
            }
            if (this.pauseOnMine.get().booleanValue() && this.mc.interactionManager.isBreakingBlock()) {
                this.fails = (byte)(this.fails + 1);
                return;
            }
        }
        boolean canSpprt = false;
        if (this.support.get().booleanValue()) {
            for (int i = 0; i < 9; ++i) {
                if (this.mc.player.getInventory().getStack(i).getItem() != Items.OBSIDIAN) continue;
                canSpprt = true;
                this.supportSlot = i;
                break;
            }
            this.supportSlotResult = InvUtils.findInHotbar(Items.OBSIDIAN);
        }
        boolean canSupport = canSpprt;
        AtomicReference bestSupportPos = new AtomicReference();
        AtomicReference bestPos = new AtomicReference();
        AtomicReference surroundTarget = new AtomicReference();
        AtomicDouble bestValue = new AtomicDouble();
        AtomicDouble displayDamage = new AtomicDouble();
        AtomicDouble bestSupportValue = new AtomicDouble();
        AtomicDouble displayBackupDamage = new AtomicDouble();
        AtomicInteger surroundBroken = new AtomicInteger();
        BlockIterator.register((int)Math.ceil(this.placeRange.get()), (int)Math.ceil(this.verticalRange.get()), (blockPos, blockState) -> {
            Vec3d posNew = new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            Vec3d checkVector = new Vec3d(posNew.x + ((double)blockPos.getX() < this.playerPos.x ? Math.min(1.0, this.playerPos.x - (double)blockPos.getX()) : 0.0), posNew.y + ((double)blockPos.getY() < this.playerPos.y ? Math.min(1.0, this.playerPos.y - (double)blockPos.getY()) : 0.0), posNew.z + ((double)blockPos.getZ() < this.playerPos.z ? Math.min(1.0, this.playerPos.z - (double)blockPos.getZ()) : 0.0));
            if (!checkVector.isInRange(this.playerPos.add(eyeHeight), this.placeRange.get())) {
                return;
            }
            if (((Boolean)this.inBreakRange.get()).booleanValue() && this.inBreakRange(new EndCrystalEntity(this.mc.world, posNew.x + 0.5, posNew.y + 1.0, posNew.z + 0.5))) {
                return;
            }
            if (((Boolean)this.antiSurroundBreak.get()).booleanValue() && CaUtils.getSurroundBreak(this.mc.player, blockPos) > 0 && CaUtils.isSurrounded(this.mc.player)) {
                return;
            }
            float friendDamage = 0.0f;
            boolean facePlaceLimit = false;
            for (LivingEntity target : this.targets) {
                int breakValue;
                boolean facePlace;
                if (target.isDead()) continue;
                Vec3d v = (Boolean)this.predict.get() != false ? target.getVelocity() : new Vec3d(0.0, 0.0, 0.0);
                Vec3d targetPos = target.getPos().add(v.x * (double)ticksBehind, v.y * (double)ticksBehind, v.z * (double)ticksBehind);
                boolean bl = facePlace = this.shouldFacePlace(target) && !facePlaceLimit;
                if (facePlace) {
                    facePlaceLimit = true;
                }
                if (((Boolean)this.crystalSave.get()).booleanValue() && target.hurtTime - Math.max(this.breakDelayLeft, 0) > 0 && !CaUtils.isFucked(target) || blockPos.getY() > target.getBlockPos().getY() || checkVector.distanceTo(targetPos) > 9.0) continue;
                if (!((blockState.getBlock() == Blocks.BEDROCK || blockState.getBlock() == Blocks.OBSIDIAN || canSupport && blockState.isReplaceable() && this.supportDelayLeft <= 0) && this.isEmpty(blockPos.add(0, 1, 0)))) {
                    return;
                }
                if (blockState.isAir()) {
                    if (((Boolean)this.supportBackup.get()).booleanValue() && bestPos.get() != null) {
                        return;
                    }
                    if (this.mc.player.getInventory().selectedSlot != this.supportSlot && this.switchDelayLeft > 0) {
                        return;
                    }
                    if (!((Boolean)this.supportAirPlace.get()).booleanValue()) {
                        boolean neighbourFound = false;
                        for (Direction side : Direction.values()) {
                            BlockPos neighbor = blockPos.offset(side);
                            if (this.mc.world.getBlockState(neighbor).isAir() || BlockUtils.isClickable(this.mc.world.getBlockState(neighbor).getBlock())) continue;
                            neighbourFound = true;
                            break;
                        }
                        if (!neighbourFound) {
                            return;
                        }
                    }
                }
                if (CaUtils.rayTraceCheck(blockPos, false) == null && this.oldMode.get()) {
                    if (this.rayTrace.get() == Type.Place || this.rayTrace.get() == Type.Both) {
                        return;
                    }
                    if (!checkVector.isInRange(this.playerPos.add(eyeHeight), this.placeWallsRange.get())) {
                        return;
                    }
                }
                if (!this.isSafePlace(this.mc.player, posNew.add(0.5, 1.0, 0.5))) {
                    return;
                }
                if (this.antiFriendPop.get() == Type.Place || this.antiFriendPop.get() == Type.Both) {
                    for (PlayerEntity friend : this.friends) {
                        if (!checkVector.isInRange(friend.getPos(), 9.0)) continue;
                        if (!this.isSafePlace(friend, posNew.add(0.5, 1.0, 0.5))) {
                            return;
                        }
                        friendDamage = CaUtils.crystalDamage(friend, posNew.add(0.5, 1.0, 0.5), (Boolean)this.predict.get(), (Boolean)this.ignoreTerrain.get());
                    }
                }
                if (!(!((Boolean)this.surroundBreak.get()).booleanValue() || bestPos.get() != null && surroundBroken.get() <= 0 || target != TargetUtils.getPlayerTarget((Double)this.placeRange.get() + 1.0, SortPriority.LowestHealth) || CaUtils.isSurroundBroken(target) || this.fails <= 0 || !(this.mItem instanceof PickaxeItem) && ((Boolean)this.surroundPickaxe.get()).booleanValue() || (breakValue = CaUtils.getSurroundBreak(target, blockPos)) <= surroundBroken.get())) {
                    if (((Boolean)this.debugTest.get()).booleanValue()) {
                        ChatUtils.info("surround breaking...", new Object[0]);
                    }
                    surroundBroken.set(breakValue);
                    bestPos.set(posNew);
                    surroundTarget.set(target);
                }
                float minDmg = facePlace ? 2.5f : (float)((Double)this.minPlaceDamage.get()).doubleValue();
                float damage = CaUtils.crystalDamage(target, posNew.add(0.5, 1.0, 0.5), (Boolean)this.predict.get(), (Boolean)this.ignoreTerrain.get());
                if (damage < minDmg) continue;
                boolean stop = false;
                for (Entity entity : this.mc.world.getEntities()) {
                    float multiDamage;
                    EndCrystalEntity crystal;
                    if (!(entity instanceof EndCrystalEntity) || (crystal = (EndCrystalEntity)entity).distanceTo(target) > 9.0f || !this.shouldBreak(crystal) || this.inBreakRange(crystal) || (multiDamage = CaUtils.crystalDamage(target, crystal.getPos(), (Boolean)this.predict.get(), (Boolean)this.ignoreTerrain.get())) < minDmg || this.isSafeBreak(this.mc.player, crystal.getPos())) continue;
                    stop = true;
                    break;
                }
                if (stop) continue;
                float selfDamage = CaUtils.crystalDamage(this.mc.player, posNew.add(0.5, 1.0, 0.5), false, (Boolean)this.ignoreTerrain.get()) + friendDamage;
                float newValue = damage / (float)Math.pow(selfDamage, 1.0 - (Double)this.torque.get());
                if (!this.mc.world.isAir((BlockPos)blockPos) || !((Boolean)this.supportBackup.get()).booleanValue()) {
                    if (!((double)newValue > bestValue.get())) continue;
                    bestPos.set(posNew);
                    bestValue.set(newValue);
                    displayDamage.set(damage);
                    surroundBroken.set(0);
                    continue;
                }
                if (bestPos.get() != null || !((double)newValue > bestSupportValue.get())) continue;
                bestSupportPos.set(posNew);
                bestSupportValue.set(newValue);
                displayBackupDamage.set(damage);
                surroundBroken.set(0);
            }
        });
        BlockIterator.after(() -> {
            if (bestPos.get() != null || bestSupportPos.get() != null) {
                Vec3d pos = bestPos.get() != null ? (Vec3d)bestPos.get() : (Vec3d)bestSupportPos.get();
                BlockPos blockPos = new BlockPos((int)pos.x, (int)pos.y, (int)pos.z);
                this.fails = 0;
                this.lastBlock = blockPos;
                if (this.mc.world.isAir(blockPos)) {
                    if (this.mc.player.getInventory().selectedSlot != this.supportSlot && this.switchDelayLeft > 0) {
                        return;
                    }
                    BlockUtils.place(new BlockPos(blockPos), this.supportSlotResult, false, 0, (Boolean)this.swing.get(), true, false);
                    this.supportDelayLeft = (Integer)this.supportDelay.get();
                }
                this.lastDamage = (float)displayDamage.get();
                this.placeDelayLeft = (Integer)this.placeDelay.get();
                EndCrystalEntity newCrystal = new EndCrystalEntity(this.mc.world, pos.x + 0.5, pos.y + 1.0, pos.z + 0.5);
                if (this.switchMode.get() == SwitchMode.Auto) {
                    this.doSwitch();
                }
                this.attemptedCrystals.add(newCrystal);
                BlockHitResult result = CaUtils.getPlaceResult(blockPos);
                if (this.rotationMode.get() == RotationMode.Place || this.rotationMode.get() == RotationMode.Both) {
                    float[] fArray = PlayerUtils.calculateAngle((Boolean)this.strictLook.get() != false ? new Vec3d((double)result.getBlockPos().getX() + 0.5 + (double)result.getSide().getVector().getX() * 0.5, (double)result.getBlockPos().getY() + 0.5 + (double)result.getSide().getVector().getY() * 0.5, (double)result.getBlockPos().getZ() + 0.5 + (double)result.getSide().getVector().getZ() * 0.5) : pos.add(0.5, 1.0, 0.5));
                }
            }
        });
    }

    private void doBreak() {
        if(breakDelayLeft > 0) return;
        if(pauseBreak()) return;    //Pauses

        float bestDamage = 0;
        EndCrystalEntity bestCrystal = null;
        for(EndCrystalEntity crystal : crystals) {

            if(!shouldBreak(crystal)) continue;
            if(crystal.age < minAge.get()) continue;

            if(inBreakRange(crystal)) continue;

            Vec3d crystalPos = crystal.getPos();
            for(LivingEntity target : targets) {

                if(target.isDead()) continue;

                if(target instanceof PlayerEntity && ((PlayerEntity) target).getAbilities().invulnerable) continue;

                if(crystal.distanceTo(target) > 9) continue;

                if(crystalSave.get() && target.hurtTime > 0) continue; //removing ticksbehind for now since it can be buggy

                float minDmg = shouldFacePlace(target) ? 2.5F : (float) minBreakDamage.get().doubleValue();
                float damage = CaUtils.crystalDamage(target, crystalPos, predict.get(), ignoreTerrain.get());

                if(surroundHold.get() && target.hurtTime> 0 && CaUtils.isFucked(target) && damage >= 10) continue;//removing ticksbehind for now since it can be buggy

                if(isSafeBreak(mc.player, crystalPos)) break;//removing ticksbehind for now since it can be buggy

                if(antiFriendPop.get() == Type.Both || antiFriendPop.get() == Type.Break) {
                    boolean skip = false;
                    for(PlayerEntity friend : friends) {
                        if(!CaUtils.crystalEdgePos(crystal).isInRange(friend.getPos(), 9)) continue;

                        if(isSafeBreak(friend, crystalPos)) {
                            skip = true;
                            break;
                        }
                    }
                    if(skip) break;
                }
                if(antiSurroundBreak.get() && bestCrystal == null) {
                    BlockPos playerBlockPos = null;
                    boolean stop = false;
                    for(Vec3i block : CaUtils.city) {
                        double x = playerBlockPos.add(block).getX();
                        double y = playerBlockPos.add(block).getY();
                        double z = playerBlockPos.add(block).getZ();
                        if(mc.world.getBlockState(playerBlockPos.add(block)).isOf(Blocks.BEDROCK)) continue;
                        for(Entity entity : mc.world.getOtherEntities(null, new Box(x, y, z, x + 1, y + 1, z + 1))) {
                            if(entity.equals(crystal)) {
                                bestCrystal = crystal;
                                stop = true;
                                break;
                            }
                        }
                        if(stop) break;
                    }
                    if(stop) continue;
                }

                if(damage < minDmg) continue;
                if(damage > bestDamage) {
                    bestDamage = damage;
                    bestCrystal = crystal;
                    this.target = target;
                }
            }
        }
        if(bestCrystal == null) return;

        final EndCrystalEntity crystal = bestCrystal;
        preSlot = mc.player.getInventory().selectedSlot;
        if(weak && antiWeakness.get() && switchDelayLeft <= 0) {
            for(int i = 0; i < 9; i++) {
                if(mc.player.getInventory().getStack(i).getItem() instanceof ToolItem) {
                    mc.player.getInventory().selectedSlot = i;
                    switched = true;
                    break;
                }
            }
        }
        doAttack(crystal);
    }

    private void doAttack(EndCrystalEntity crystal) {
        entitiesToRemove.add(crystal.getId());
        if(rotationMode.get() == RotationMode.Break || rotationMode.get() == RotationMode.Both) {
            float[] rotation = PlayerUtils.calculateAngle(CaUtils.crystalEdgePos(crystal));
            Rotations.rotate(rotation[0], rotation[1], 50, () -> attackCrystal(crystal));
        } else attackCrystal(crystal);
    }

    private void attackCrystal(EndCrystalEntity crystal) {
        if(antiWeakness.get() && weak) mc.interactionManager.attackEntity(mc.player, crystal);
        else mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, false));
        Hand breakerHand = breakHand.get() == BreakHand.Auto ? getHand() : breakHand.get() == BreakHand.Mainhand ? Hand.MAIN_HAND : Hand.OFF_HAND;
        if(swing.get()) mc.player.swingHand(breakerHand);
        else mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(breakerHand));

        if(removeCrystals.get() == Canceller.HitCanceller) {
            crystals.remove(crystal);
            crystal.kill();
        }
        attemptedBreaks.put(crystal.getId(), attemptedBreaks.get(crystal.getId()) + 1);

        if(switchDelayLeft <= 0 && switched && switchBack.get()) {
            mc.player.getInventory().selectedSlot = preSlot;
            switched = false;
        }
        breakDelayLeft = breakDelay.get();
        if(debugTest.get()) ChatUtils.info( "Distance from eye pos to crystal: " + playerPos.add(eyeHeight).distanceTo(new Vec3d(crystal.getX(), crystal.getY(), crystal.getZ())));
    }


    private boolean shouldBreak(EndCrystalEntity crystal) {
        if(!crystal.isAlive()) return false;            //Self explanatory

        for(int id : entitiesToRemove)  if(crystal == mc.world.getEntityById(id)) return false;            //Continue if crystal is already about to get removed

        return attemptedBreaks.get(crystal.getId()) < breakAttempts.get();            //Check break attempts
    }

    private boolean inBreakRange(EndCrystalEntity crystal) {
        Vec3d crystalPos = CaUtils.crystalEdgePos(crystal);
        if(!crystalPos.isInRange(playerPos.add(eyeHeight), breakRange.get())) return true;    //Making sure crystal is in range and its a sphere not a cube
        if(!mc.player.canSee(crystal) && oldMode.get()) {   //Raytrace & walls range check
            if(rayTrace.get() == Type.Break || rayTrace.get() == Type.Both) return true;
            if(!crystalPos.isInRange(playerPos.add(eyeHeight), breakWallsRange.get())) return true;
        }
        return false;
    }

    private boolean isSafeBreak(LivingEntity entity, Vec3d crystalPos) {
        if(breakMode.get() == Mode.Suicide) return false;
        float damage = CaUtils.crystalDamage(entity, crystalPos, !entity.equals(mc.player) && predict.get(), ignoreTerrain.get());
        return !((EntityUtils.getTotalHealth((PlayerEntity) entity)) > damage) || !(damage <= maxBreakDamage.get());
    }

    private boolean isSafePlace(LivingEntity entity, Vec3d crystalPos) {
        if(placeMode.get() == Mode.Suicide) return true;
        float damage = CaUtils.crystalDamage(entity, crystalPos, !entity.equals(mc.player) && predict.get(), ignoreTerrain.get());
        return (EntityUtils.getTotalHealth((PlayerEntity) entity)) > damage && damage <= maxPlaceDamage.get();
    }

    private boolean pauseBreak() {
        if(pauseMode.get() == Type.Break || pauseMode.get() == Type.Both) {
            if(isUsing) {
                if(pauseOnEat.get() && (mItem.isFood() || oItem.isFood())) return true;
                if(pauseOnDrink.get() && (mItem instanceof PotionItem || oItem instanceof PotionItem)) return true;
            }
            if(pauseOnMine.get() && mc.interactionManager.isBreakingBlock()) return true;
        }
        if(weak) {
            if(noFoodSwitch.get() && isUsing && (mItem.isFood() || (oItem.isFood() && !(mItem instanceof ToolItem)))) return true;
            boolean strong = false;
            for(int i = 0; i < 9; i++) {
                Item item = mc.player.getInventory().getStack(i).getItem();
                if(item instanceof ToolItem) {
                    strong = true;
                    break;
                }
            }
            if(!strong) return true;
        }
        return false;
    }

    private void doSwitch(){
        if(switchDelayLeft > 0) return;
        if(mItem == Items.END_CRYSTAL || oItem == Items.END_CRYSTAL) return;
        if (mc.player.getHealth() <= switchHealth.get()) return;
        int slot = InvUtils.find(Items.END_CRYSTAL).slot();
        if(slot != -1 && slot < 9) {
            preSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = slot;
            switched = true;
        }
    }

    private boolean isEmpty(BlockPos pos) {
        double x = pos.up().getX();
        double y = pos.up().getY();
        double z = pos.up().getZ();
        List<Entity> entities = mc.world.getOtherEntities(null, new Box(x, y - 1, z, x + 1.0D, y + 1.0D, z + 1.0D));
        for(int id : entitiesToRemove) {
            entities.remove(mc.world.getEntityById(id));
        }
        return (mc.world.getBlockState(pos).isAir() && entities.isEmpty() && (!oldMode.get() || mc.world.getBlockState(pos.add(0, 1, 0)).isAir()));
    }

    private boolean shouldFacePlace(LivingEntity target) {
        if(!facePlace.get()) return false;
        if(forceFacePlace.get().isPressed() && target.equals(closestTarget)) return true;
        if(!(target instanceof PlayerEntity)) return false;
        if(!facePlaceSelf.get() && mc.player.distanceTo(target) < 1) return false;
        if(pauseSword.get() && (mItem instanceof ToolItem)) return false;
        if(facePlacePause.get()) {  //faceplace pause;
            if(isUsing) {
                if(facePlacePauseEat.get() && (mItem.isFood() || oItem.isFood())) return false;
                if(facePlacePauseDrink.get() && (mItem instanceof PotionItem || oItem instanceof PotionItem)) return false;
            }
            if(facePlacePauseMine.get() && mc.interactionManager.isBreakingBlock()) return false;
        }
        if(facePlaceHole.get() && (CaUtils.isSurrounded(target) || CaUtils.isBurrowed(target))) return true;
        if(EntityUtils.getTotalHealth((PlayerEntity) target) <= facePlaceHealth.get()) return true;

        Iterable<ItemStack> armourItems = target.getArmorItems();
        for(ItemStack itemStack : armourItems){
            if(itemStack == null || itemStack.isEmpty()) {
                if(facePlaceArmor.get()) return true;
            } else
            if((((double) (itemStack.getMaxDamage() - itemStack.getDamage()) / itemStack.getMaxDamage()) * 100) <= facePlaceDurability.get()) return true;
        }
        return false;
    }


    private Hand getHand() {
        Hand hand = Hand.MAIN_HAND;
        if(mItem != Items.END_CRYSTAL && oItem == Items.END_CRYSTAL) {
            hand = Hand.OFF_HAND;
        }
        return hand;
    }

    @EventHandler
    private void onEntity(EntityAddedEvent event) {
        dif = event.entity.getId() - lastEntityId;
        lastEntityId = event.entity.getId();
        if(!(event.entity instanceof EndCrystalEntity)) return;
        EndCrystalEntity crystal = (EndCrystalEntity) event.entity;
        if(pauseBreak() || breakDelayLeft > 0) return;
        if(!shouldBreak(crystal)) return;
        if(crystal.age < minAge.get()) return;

        for(LivingEntity target : targets) {
            if(target.isDead()) continue;
            if(target.hurtTime > 0 && crystalSave.get()) continue; //removing ticksbehind for now since it can be buggy
            if(crystal.distanceTo(target) > 9) continue;
            float damage = CaUtils.crystalDamage(target, crystal.getPos(), predict.get(), ignoreTerrain.get());
            if(target.hurtTime > 0 && CaUtils.isFucked(target) && surroundHold.get() && damage >= 10) continue;
            if(damage < (shouldFacePlace(target) ? 2.5 : minBreakDamage.get())) continue;

            if(isSafeBreak(mc.player, crystal.getPos())) return; //removing ticksbehind for now since it can be buggy

            if(antiFriendPop.get() == Type.Both || antiFriendPop.get() == Type.Break) {
                for(PlayerEntity friend : friends) {
                    if(!CaUtils.crystalEdgePos(crystal).isInRange(friend.getPos(), 9)) continue;
                    if(isSafeBreak(friend, crystal.getPos())) return;
                }
            }
            doAttack(crystal);
            break;
        }
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
            switchDelayLeft = switchDelay.get();
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if(lastBlock == null || !render.get() || targets.isEmpty()) return;
        event.renderer.box(lastBlock, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if(lastBlock == null || !renderDamage.get() || targets.isEmpty() || lastDamage == 0) return;
            TextRenderer.get().begin(1, false, true);
            String damageText = String.valueOf(Math.round(lastDamage));
            switch (roundDamage.get()) {
                case 0:
                    damageText = String.valueOf(Math.round(lastDamage));
                    break;
                case 1:
                    damageText = String.valueOf(Math.round(lastDamage * 10.0) / 10.0);
                    break;
                case 2:
                    damageText = String.valueOf(Math.round(lastDamage * 100.0) / 100.0);
                    break;
                case 3:
                    damageText = String.valueOf(Math.round(lastDamage * 1000.0) / 1000.0);
                    break;
            }
            double w = TextRenderer.get().getWidth(damageText) / 2;
            TextRenderer.get().render(damageText, -w, 0, damageColor.get());
            TextRenderer.get().end();
            NametagUtils.end();
    }

}
