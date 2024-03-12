package kassuk.addon.blackout.modules.combat;

import com.google.common.util.concurrent.AtomicDouble;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.utils.bed.port.basic.RenderInfo;
import kassuk.addon.blackout.utils.bed.port.advance.CrystalUtils;
import kassuk.addon.blackout.utils.bed.port.advance.Interaction;
import kassuk.addon.blackout.utils.bed.port.advance.RenderUtils;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.EntityVelocityUpdateS2CPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IBox;
import meteordevelopment.meteorclient.mixininterface.IRaycastContext;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static kassuk.addon.blackout.utils.bed.port.basic.EntityInfo.*;
import static kassuk.addon.blackout.utils.bed.port.basic.BlockInfo.*;
import static kassuk.addon.blackout.utils.bed.port.basic.BlockInfo.Z;
import static kassuk.addon.blackout.utils.bed.port.basic.BlockInfo.Y;
import static kassuk.addon.blackout.utils.bed.port.basic.BlockInfo.X;
import static kassuk.addon.blackout.utils.bed.port.advance.RenderUtils.*;
import static kassuk.addon.blackout.utils.bed.port.advance.PredictionUtils.returnPredictBox;
import static meteordevelopment.meteorclient.renderer.Mesh.Attrib.Vec3;

public class AutoCrystalPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgDamage = settings.createGroup("Damage");
    private final SettingGroup sgPredict = settings.createGroup("Predict");
    private final SettingGroup sgFacePlace = settings.createGroup("Face Place");
    private final SettingGroup sgMisc = settings.createGroup("Misc");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range").description("Range in which to target players.").defaultValue(10).min(0).sliderMax(16).build());
    private final Setting<Boolean> predictCrystal = sgGeneral.add(new BoolSetting.Builder().name("predict-crystal").description("Predicts crystal position.").defaultValue(false).build());
    public final Setting<Boolean> ignoreTerrain = sgGeneral.add(new BoolSetting.Builder().name("ignore-terrain").description("Completely ignores terrain if it can be blown up by crystals.").defaultValue(true).build());
    private final Setting<AutoSwitchMode> autoSwitch = sgGeneral.add(new EnumSetting.Builder<AutoSwitchMode>().name("auto-switch").description("Switches to crystals in your hotbar once a target is found.").defaultValue(AutoSwitchMode.Normal).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Rotates server-side towards the crystals being hit/placed.").defaultValue(false).build());
    private final Setting<YawStepMode> yawStepMode = sgGeneral.add(new EnumSetting.Builder<YawStepMode>().name("yaw-steps-mode").description("When to run the yaw steps check.").defaultValue(YawStepMode.Break).visible(rotate::get).build());
    private final Setting<Double> yawSteps = sgGeneral.add(new DoubleSetting.Builder().name("yaw-steps").description("Maximum number of degrees its allowed to rotate in one tick.").defaultValue(180).range(1, 180).visible(rotate::get).build());

    // Place
    private final Setting<Boolean> doPlace = sgPlace.add(new BoolSetting.Builder().name("place").description("If the CA should place crystals.").defaultValue(true).build());
    private final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder().name("place-delay").description("The delay in ticks to wait to place a crystal after it's exploded.").defaultValue(0).min(0).sliderMax(20).build());
    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder().name("place-range").description("Range in which to place crystals.").defaultValue(4.5).min(0).sliderMax(6).build());
    private final Setting<Double> placeWallsRange = sgPlace.add(new DoubleSetting.Builder().name("place-walls-range").description("Range in which to place crystals when behind blocks.").defaultValue(4.5).min(0).sliderMax(6).build());
    private final Setting<Boolean> placement112 = sgPlace.add(new BoolSetting.Builder().name("1.12-placement").description("Uses 1.12 crystal placement.").defaultValue(false).build());
    private final Setting<SupportMode> support = sgPlace.add(new EnumSetting.Builder<SupportMode>().name("support").description("Places a support block in air if no other position have been found.").defaultValue(SupportMode.Disabled).build());
    private final Setting<Integer> supportDelay = sgPlace.add(new IntSetting.Builder().name("support-delay").description("Delay in ticks after placing support block.").defaultValue(1).min(0).visible(() -> support.get() != SupportMode.Disabled).build());

    // Break
    private final Setting<Boolean> doBreak = sgBreak.add(new BoolSetting.Builder().name("break").description("If the CA should break crystals.").defaultValue(true).build());
    public final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder().name("break-delay").description("The delay in ticks to wait to break a crystal after it's placed.").defaultValue(0).min(0).sliderMax(20).build());
    private final Setting<Integer> switchDelay = sgBreak.add(new IntSetting.Builder().name("switch-delay").description("The delay in ticks to wait to break a crystal after switching hotbar slot.").defaultValue(0).min(0).build());
    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder().name("break-range").description("Range in which to break crystals.").defaultValue(4.5).min(0).sliderMax(6).build());
    private final Setting<Double> breakWallsRange = sgBreak.add(new DoubleSetting.Builder().name("break-walls-range").description("Range in which to break crystals when behind blocks.").defaultValue(4.5).min(0).sliderMax(6).build());
    private final Setting<Boolean> onlyBreakOwn = sgBreak.add(new BoolSetting.Builder().name("only-own").description("Only breaks own crystals.").defaultValue(false).build());
    private final Setting<Integer> breakAttempts = sgBreak.add(new IntSetting.Builder().name("break-attempts").description("How many times to hit a crystal before stopping to target it.").defaultValue(2).sliderMin(1).sliderMax(5).build());
    private final Setting<CancelCrystal> cancelCrystal = sgBreak.add(new EnumSetting.Builder<CancelCrystal>().name("cancel-crystal").description("The way for removing crystals.").defaultValue(CancelCrystal.NoDesync).build());
    private final Setting<Integer> crystalAge = sgBreak.add(new IntSetting.Builder().name("crystal-age").description("Amount of ticks a crystal needs to have lived for it to be attacked by AutoCrystal.").defaultValue(0).min(0).build());
    private final Setting<Integer> attackFrequency = sgBreak.add(new IntSetting.Builder().name("attack-frequency").description("Maximum hits to do per second.").defaultValue(25).min(1).sliderRange(1, 30).build());
    private final Setting<Boolean> fastBreak = sgBreak.add(new BoolSetting.Builder().name("fast-break").description("Ignores break delay and tries to break the crystal as soon as it's spawned in the world.").defaultValue(true).build());
    private final Setting<Boolean> antiWeakness = sgBreak.add(new BoolSetting.Builder().name("anti-weakness").description("Switches to tools with high enough damage to explode the crystal with weakness effect.").defaultValue(true).build());

    // Damage
    public final Setting<Boolean> ignoreBreakDmg = sgDamage.add(new BoolSetting.Builder().name("ignore-break-dmg").description("Ignores break damage, useful if crystals didn't breaks on move.").defaultValue(false).build());
    public final Setting<Double> minDmg = sgDamage.add(new DoubleSetting.Builder().name("min-dmg").description("Minimum damage the crystal needs to deal to your target.").defaultValue(8.5).min(0).build());
    public final Setting<Double> maxDmg = sgDamage.add(new DoubleSetting.Builder().name("max-dmg").description("Maximum damage crystals can deal to yourself.").defaultValue(6).range(0, 36).sliderMax(36).build());
    public final Setting<Boolean> antiFriendPop = sgDamage.add(new BoolSetting.Builder().name("anti-friend-pop").description("Prevents from popping friends.").defaultValue(false).build());
    public final Setting<Double> maxFriendDmg = sgDamage.add(new DoubleSetting.Builder().name("max-dmg").description("Maximum damage crystals can deal to your friends.").defaultValue(6).range(0, 36).sliderMax(36).visible(antiFriendPop::get).build());
    public final Setting<DamageToPlayer> dmgToPlayer = sgBreak.add(new EnumSetting.Builder<DamageToPlayer>().name("dmg-to-player").description("Will not place and break crystals if they will kill you.").defaultValue(DamageToPlayer.AntiSuicide).build());

    //Predict
    public final Setting<Boolean> predict = sgPredict.add(new BoolSetting.Builder().name("predict-position").description("Predicts target position.").defaultValue(true).build());
    public final Setting<Integer> predictIncrease = sgPredict.add(new IntSetting.Builder().name("predict-increase").description("Increasing range from predicted position to target.").defaultValue(2).visible(predict::get).build());
    public final Setting<Boolean> predictCollision = sgPredict.add(new BoolSetting.Builder().name("predict-collision").description("Whether to consider collision when predicting.").defaultValue(true).visible(predict::get).build());

    // Face place
    public final Setting<Boolean> facePlace = sgFacePlace.add(new BoolSetting.Builder().name("face-place").description("Will face-place when target is below a certain health or armor durability threshold.").defaultValue(true).build());
    public final Setting<Double> facePlaceHealth = sgFacePlace.add(new DoubleSetting.Builder().name("face-place-health").description("The health the target has to be at to start face placing.").defaultValue(8).min(1).sliderMin(1).sliderMax(36).visible(facePlace::get).build());
    public final Setting<Double> armorDurability = sgFacePlace.add(new DoubleSetting.Builder().name("armor-durability").description("The durability threshold percentage to be able to face-place.").defaultValue(2).min(1).sliderMin(1).sliderMax(100).visible(facePlace::get).build());
    public final Setting<Boolean> facePlaceHurt = sgFacePlace.add(new BoolSetting.Builder().name("delayed-break").description("Breaks crystals only while target can receive damage.").defaultValue(true).visible(facePlace::get).build());
    public final Setting<Boolean> facePlaceArmor = sgFacePlace.add(new BoolSetting.Builder().name("face-place-missing-armor").description("Automatically starts face placing when a target misses a piece of armor.").defaultValue(false).visible(facePlace::get).build());
    public final Setting<Keybind> forceFacePlace = sgFacePlace.add(new KeybindSetting.Builder().name("force-face-place").description("Starts face place when this button is pressed.").defaultValue(Keybind.none()).build());

    // Misc
    public final Setting<Boolean> surroundBreak = sgMisc.add(new BoolSetting.Builder().name("surround-break").description("Placing crystal next to surrounded player to prevent re-surround").defaultValue(true).build());
    public final Setting<Boolean> antiSelf = sgMisc.add(new BoolSetting.Builder().name("anti-self").description("Prevent placing crystals if they will block your own surround").defaultValue(true).visible(surroundBreak::get).build());
    public final Setting<Boolean> surroundHold = sgMisc.add(new BoolSetting.Builder().name("surround-hold").description("Attacks crystals slowly if crystal position equals target surround.").defaultValue(true).build());
    public final Setting<Boolean> extraPlaces = sgMisc.add(new BoolSetting.Builder().name("extra-places").description("Using more position for surround holding.").defaultValue(false).visible(surroundHold::get).build());

    // Pause
    private final Setting<Boolean> eatPause = sgPause.add(new BoolSetting.Builder().name("pause-on-eat").description("Pauses Crystal Aura when eating.").defaultValue(true).build());
    private final Setting<Boolean> drinkPause = sgPause.add(new BoolSetting.Builder().name("pause-on-drink").description("Pauses Crystal Aura when drinking.").defaultValue(true).build());
    private final Setting<Boolean> minePause = sgPause.add(new BoolSetting.Builder().name("pause-on-mine").description("Pauses Crystal Aura when mining.").defaultValue(false).build());

    // Render
    public final Setting<Interaction.SwingHand> swing = sgRender.add(new EnumSetting.Builder<Interaction.SwingHand>().name("swing").description("How the renred are rendered.").defaultValue(Interaction.SwingHand.Auto).build());
    public final Setting<Boolean> packetSwing = sgRender.add(new BoolSetting.Builder().name("packet-swing").description("Renders hand swinging client side.").defaultValue(true).build());
    private final Setting<RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderMode>().name("render-mode").description("How the render are rendered.").defaultValue(RenderMode.UpperSide).build());
    public final Setting<Double> height = sgRender.add(new DoubleSetting.Builder().name("height").description("Maximum damage anchors can deal to yourself.").defaultValue(0.99).sliderRange(0, 1).visible(() -> visibleHeight(renderMode.get())).build());
    private final Setting<Boolean> fade = sgRender.add(new BoolSetting.Builder().name("fade").description("Fade mode.").defaultValue(false).build());
    public final Setting<Integer> fadeTime = sgRender.add(new IntSetting.Builder().name("fade-time").description("Duration for render place pos.").defaultValue(10).range(0, 50).visible(fade::get).build());
    public final Setting<Integer> fadeAmount = sgRender.add(new IntSetting.Builder().name("fade-amount").description("Amount of smooth.").defaultValue(10).range(0, 50).visible(fade::get).build());
    private final Setting<Boolean> renderBreak = sgRender.add(new BoolSetting.Builder().name("break").description("Renders a block overlay over the block the crystals are broken on.").defaultValue(false).build());
    private final Setting<Integer> renderBreakTime = sgRender.add(new IntSetting.Builder().name("break-time").description("How long to render breaking for.").defaultValue(13).min(0).sliderMax(20).visible(renderBreak::get).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color of the block overlay.").defaultValue(new SettingColor(255, 255, 255, 45)).visible(() -> visibleSide(shapeMode.get())).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color of the block overlay.").defaultValue(new SettingColor(255, 255, 255)).visible(() -> visibleLine(shapeMode.get())).build());
    private final Setting<Integer> renderTime = sgRender.add(new IntSetting.Builder().name("render-time").description("How long to render for.").defaultValue(10).min(0).sliderMax(20).build());
    private final Setting<Boolean> damageText = sgRender.add(new BoolSetting.Builder().name("damage").description("Renders crystal damage text in the block overlay.").defaultValue(true).build());
    private final Setting<Double> damageTextScale = sgRender.add(new DoubleSetting.Builder().name("damage-scale").description("How big the damage text should be.").defaultValue(1.25).min(1).sliderMax(4).visible(damageText::get).build());

    // Fields
    private int breakTimer, placeTimer, switchTimer, ticksPassed;
    public final List<PlayerEntity> targets = new ArrayList<>();

    private final Vec3d vec3d = new Vec3d(0, 0, 0);
    private final Vec3d playerEyePos = new Vec3d(0, 0, 0);
    private final Vector3d vec3 = new Vector3d();
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private final Box box = new Box(0, 0, 0, 0, 0, 0);

    private final Vec3d vec3dRayTraceEnd = new Vec3d(0, 0, 0);
    private RaycastContext raycastContext;

    private final IntSet placedCrystals = new IntOpenHashSet();
    private boolean placing;
    private int placingTimer;
    private final BlockPos.Mutable placingCrystalBlockPos = new BlockPos.Mutable();

    private final IntSet removed = new IntOpenHashSet();
    private final Int2IntMap attemptedBreaks = new Int2IntOpenHashMap();
    private final Int2IntMap waitingToExplode = new Int2IntOpenHashMap();
    private int attacks;

    private double serverYaw;
    public static float ticksBehind;

    public PlayerEntity bestTarget;
    private double bestTargetDamage;
    private int bestTargetTimer;

    public EndCrystalEntity sbCrystal;

    private boolean didRotateThisTick;
    private boolean isLastRotationPos;
    private final Vec3d lastRotationPos = new Vec3d(0, 0, 0);
    private double lastYaw, lastPitch;
    private int lastRotationTimer;
    private int lastEntityId, last;

    private int renderTimer, breakRenderTimer;
    private final BlockPos.Mutable renderPos = new BlockPos.Mutable();
    private final BlockPos.Mutable breakRenderPos = new BlockPos.Mutable();
    private double renderDamage;

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();
    private final Pool<RenderBlock> renderBreakBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBreakBlocks = new ArrayList<>();

    // crystal per second
    private final int[] second = new int[20];
    private static int cps;
    private int tick, i, lastSpawned = 20;

    public AutoCrystalPlus() {
        super(BlackOut.COMBATPLUS, "auto-crystal-plus", "Automatically places and attacks crystals.");
    }

    @Override
    public void onActivate() {
        breakTimer = 0;
        placeTimer = 0;
        ticksPassed = 0;

        raycastContext = new RaycastContext(new Vec3d(0, 0, 0), new Vec3d(0, 0, 0), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);

        placing = false;
        placingTimer = 0;

        attacks = 0;

        serverYaw = mc.player.getYaw();

        bestTargetDamage = 0;
        bestTargetTimer = 0;

        sbCrystal = null;

        lastRotationTimer = getLastRotationStopDelay();

        renderTimer = 0;
        breakRenderTimer = 0;

        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
        for (RenderBlock renderBlock : renderBreakBlocks) renderBreakBlockPool.free(renderBlock);
        renderBlocks.clear();

        tick = 0;
        Arrays.fill(second, 0);
        i = 0;
    }

    @Override
    public void onDeactivate() {
        targets.clear();

        placedCrystals.clear();

        attemptedBreaks.clear();
        waitingToExplode.clear();

        removed.clear();

        bestTarget = null;

        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
        for (RenderBlock renderBlock : renderBreakBlocks) renderBreakBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    private int getLastRotationStopDelay() {
        return Math.max(10, placeDelay.get() / 2 + breakDelay.get() / 2 + 10);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onPreTick(TickEvent.Pre event) {

        // Calculating crystal per second
        calcCPS();

        // Update last rotation
        didRotateThisTick = false;
        lastRotationTimer++;

        // Decrement placing timer
        if (placing) {
            if (placingTimer > 0) placingTimer--;
            else placing = false;
        }

        if (ticksPassed < 20) ticksPassed++;
        else {
            ticksPassed = 0;
            attacks = 0;
        }

        // Decrement best target timer
        if (bestTargetTimer > 0) bestTargetTimer--;
        bestTargetDamage = 0;

        // Decrement break, place and switch timers
        if (breakTimer > 0) breakTimer--;
        if (placeTimer > 0) placeTimer--;
        if (switchTimer > 0) switchTimer--;

        if (!shouldSurroundHold() && breakTimer > breakDelay.get()) breakTimer = 0;

        // Decrement render timers
        if (renderTimer > 0) renderTimer--;
        if (breakRenderTimer > 0) breakRenderTimer--;

        if (mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null) {
            ticksBehind = (float) mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency() / (50.0f * (20.0f / TickRate.INSTANCE.getTickRate()));
        }

        // Update waiting to explode crystals and mark them as existing if reached threshold
        for (int it : waitingToExplode.keySet()) {
            int ticks = waitingToExplode.get(it);

            if (ticks > 3) {
                waitingToExplode.remove(it);
                removed.remove(it);
            } else {
                waitingToExplode.put(it, ticks + 1);
            }
        }

        // Check pause settings
        if (PlayerUtils.shouldPause(minePause.get(), eatPause.get(), drinkPause.get())) return;

        // Set player eye pos
        ((IVec3d) playerEyePos).set(mc.player.getPos().x, mc.player.getPos().y + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getPos().z);

        // Find targets, break and place
        findTargets();

        if (targets.size() > 0) {
            if (!didRotateThisTick) doBreak();
            if (!didRotateThisTick) doPlace();
        }

        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf((renderBlock) -> renderBlock.ticks <= 0);
        renderBreakBlocks.forEach(RenderBlock::tick);
        renderBreakBlocks.removeIf((renderBlock) -> renderBlock.ticks <= 0);
    }

    @EventHandler(priority = EventPriority.LOWEST - 666)
    private void onPreTickLast(TickEvent.Pre event) {
        // Rotate to last rotation
        if (rotate.get() && lastRotationTimer < getLastRotationStopDelay() && !didRotateThisTick) {
            Rotations.rotate(isLastRotationPos ? Rotations.getYaw(lastRotationPos) : lastYaw, isLastRotationPos ? Rotations.getPitch(lastRotationPos) : lastPitch, -100, null);
        }
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (!(event.entity instanceof EndCrystalEntity)) return;

        last = event.entity.getId() - lastEntityId;
        lastEntityId = event.entity.getId();

        if (placing && event.entity.getBlockPos().equals(placingCrystalBlockPos)) {
            placing = false;
            placingTimer = 0;
            placedCrystals.add(event.entity.getId());
        }

        if (fastBreak.get() && !didRotateThisTick && attacks < attackFrequency.get()) {
            double damage = getBreakDamage(event.entity, true);
            if (damage > minDmg.get()) doBreak(event.entity);
        }
    }

    @EventHandler
    private void onEntityRemoved(EntityRemovedEvent event) {
        if (event.entity instanceof EndCrystalEntity) {
            // Adds count to crystal per second counter
            if (placedCrystals.contains(event.entity.getId())) {
                lastSpawned = 20;
                tick++;
            }

            placedCrystals.remove(event.entity.getId());
            removed.remove(event.entity.getId());
            waitingToExplode.remove(event.entity.getId());
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof EntityVelocityUpdateS2CPacket packet
                && bestTarget != null
                && ((EntityVelocityUpdateS2CPacket) event.packet).getId() == bestTarget.getId()
                && bestTarget.hurtTime >= 10) {
            double velX = (packet.getVelocityX() / 8000d - bestTarget.getVelocity().x) * 0;
            double velY = (packet.getVelocityY() / 8000d - bestTarget.getVelocity().y) * 0;
            double velZ = (packet.getVelocityZ() / 8000d - bestTarget.getVelocity().z) * 0;
            ((EntityVelocityUpdateS2CPacketAccessor) packet).setX((int) (velX * 8000 + bestTarget.getVelocity().x * 8000));
            ((EntityVelocityUpdateS2CPacketAccessor) packet).setY((int) (velY * 8000 + bestTarget.getVelocity().y * 8000));
            ((EntityVelocityUpdateS2CPacketAccessor) packet).setZ((int) (velZ * 8000 + bestTarget.getVelocity().z * 8000));
        }
    }

    private void setRotation(boolean isPos, Vec3d pos, double yaw, double pitch) {
        didRotateThisTick = true;
        isLastRotationPos = isPos;

        if (isPos) ((IVec3d) lastRotationPos).set(pos.x, pos.y, pos.z);
        else {
            lastYaw = yaw;
            lastPitch = pitch;
        }

        lastRotationTimer = 0;
    }

    // Break

    private void doBreak() {
        if (!doBreak.get() || breakTimer > 0 || switchTimer > 0 || attacks >= attackFrequency.get()) return;

        double bestDamage = 0;
        Entity crystal = null;

        // Find best crystal to break
        for (Entity entity : mc.world.getEntities()) {
            double damage = getBreakDamage(entity, true);

            if (damage > bestDamage) {
                bestDamage = damage;
                crystal = entity;
            }
        }

        // Break the crystal
        if (crystal != null) {
            doBreak(crystal);
        }
    }

    private double getBreakDamage(Entity entity, boolean checkCrystalAge) {
        if (!(entity instanceof EndCrystalEntity)) return 0;

        // Check only break own
        if (onlyBreakOwn.get() && !placedCrystals.contains(entity.getId())) return 0;

        // Check if it should already be removed
        if (removed.contains(entity.getId())) return 0;

        // Check attempted breaks
        if (attemptedBreaks.get(entity.getId()) > breakAttempts.get()) return 0;

        // Check crystal age
        if (checkCrystalAge && entity.age < crystalAge.get()) return 0;

        // Slow faceplace
        //if (shouldFacePlace() && mc.player.hurtTime != 0) return 0;

        // Check range
        if (isOutOfRange(entity.getPos(), entity.getBlockPos(), false)) return 0;

        // Check damage to self and anti suicide
        blockPos.set(entity.getBlockPos()).move(0, -1, 0);
        double selfDamage = CrystalUtils.crystalDamage(mc.player, entity.getPos());

        switch (dmgToPlayer.get()) {
            case AntiSuicide:
                if (!ignoreBreakDmg.get() && (selfDamage > maxDmg.get() || selfDamage >= EntityUtils.getTotalHealth(mc.player)))
                    return 0;
            case Invincibility:
                if (!ignoreBreakDmg.get() && (selfDamage > maxDmg.get() && mc.player.hurtTime > 0)) return 0;
        }

        // Check damage to targets and face place
        double damage = getDamageToTargets(entity.getPos(), true, false);

        if (!shouldFacePlace() && damage < minDmg.get()) return 0;

        return damage;
    }

    private void doBreak(Entity crystal) {
        // Anti weakness
        if (antiWeakness.get()) {
            StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
            StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);

            // Check for strength
            if (weakness != null && (strength == null || strength.getAmplifier() <= weakness.getAmplifier())) {
                // Check if the item in your hand is already valid
                if (!isValidWeaknessItem(mc.player.getMainHandStack())) {
                    // Find valid item to break with
                    if (!InvUtils.swap(InvUtils.findInHotbar(this::isValidWeaknessItem).slot(), false)) return;

                    switchTimer = 1;
                    return;
                }
            }
        }

        if (rotate.get()) {
            double yaw = Rotations.getYaw(crystal);
            double pitch = Rotations.getPitch(crystal, Target.Feet);

            if (doYawSteps(yaw, pitch)) {
                setRotation(true, crystal.getPos(), 0, 0);
                Rotations.rotate(yaw, pitch, 50, () -> attackCrystal(crystal));

                breakTimer = getDelay();
            }
        } else {
            attackCrystal(crystal);
            breakTimer = getDelay();
        }

        if (cancelCrystal.get() == CancelCrystal.OnHit) {
            placedCrystals.remove(crystal.getId());
            placedCrystals.remove(crystal.getId());
            crystal.kill();
        }


        removed.add(crystal.getId());
        attemptedBreaks.put(crystal.getId(), attemptedBreaks.get(crystal.getId()) + 1);
        waitingToExplode.put(crystal.getId(), 0);

        // Break render
        renderBreakBlocks.add((renderBreakBlockPool.get()).set(crystal.getBlockPos().down()));
        breakRenderPos.set(crystal.getBlockPos().down());
        breakRenderTimer = renderBreakTime.get();
    }

    private boolean isValidWeaknessItem(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof ToolItem) || itemStack.getItem() instanceof HoeItem) return false;

        ToolMaterial material = ((ToolItem) itemStack.getItem()).getMaterial();
        return material == ToolMaterials.DIAMOND || material == ToolMaterials.NETHERITE;
    }

    private void attackCrystal(Entity entity) {
        // Attack
        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));

        Hand hand = InvUtils.findInHotbar(Items.END_CRYSTAL).getHand();
        if (hand == null) hand = Hand.MAIN_HAND;

        // Swing
        Interaction.doSwing(swing.get(), packetSwing.get(), hand);
        attacks++;
    }

    private void calcCPS() {
        i++;
        if (i >= second.length) i = 0;

        second[i] = tick;
        tick = 0;

        cps = 0;
        for (int i : second) cps += i;

        lastSpawned--;
        if (lastSpawned >= 0 && cps > 0) cps--;
    }

    public static int getCPS() {
        return cps;
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
            switchTimer = switchDelay.get();
        }
    }

    // Place

    private void doPlace() {
        if (!doPlace.get() || placeTimer > 0) return;

        // Return if there are no crystals in hotbar or offhand
        if (!InvUtils.findInHotbar(Items.END_CRYSTAL).found()) return;

        // Return if there are no crystals in either hand and auto switch mode is none
        if (autoSwitch.get() == AutoSwitchMode.None && mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL && mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL)
            return;

        for (Entity entity : mc.world.getEntities()) {
            if (getBreakDamage(entity, false) > 0) return;
        }

        // Setup variables
        AtomicDouble bestDamage = new AtomicDouble(0);
        AtomicReference<BlockPos.Mutable> bestBlockPos = new AtomicReference<>(new BlockPos.Mutable());
        AtomicBoolean isSupport = new AtomicBoolean(support.get() != SupportMode.Disabled);
        AtomicInteger i = new AtomicInteger();
        // Find best position to place the crystal on
        BlockIterator.register((int) Math.ceil(placeRange.get()), (int) Math.ceil(placeRange.get()), (bp, blockState) -> {
            // Check if its bedrock or obsidian and return if isSupport is false
            boolean hasBlock = blockState.isOf(Blocks.BEDROCK) || blockState.isOf(Blocks.OBSIDIAN);
            if (!hasBlock && (!isSupport.get() || !blockState.isReplaceable())) return;


            // Check if there is air on top
            blockPos.set(bp.getX(), bp.getY() + 1, bp.getZ());
            if (!mc.world.getBlockState(blockPos).isAir()) return;

            if (placement112.get()) {
                blockPos.move(0, 1, 0);
                if (!mc.world.getBlockState(blockPos).isAir()) return;
            }

            // Check range
            ((IVec3d) vec3d).set(bp.getX() + 0.5, bp.getY() + 1, bp.getZ() + 0.5);
            blockPos.set(bp).move(0, 1, 0);
            if (isOutOfRange(vec3d, blockPos, true)) return;

            // Check damage to self, anti suicide and anti friend pop
            double selfDamage = CrystalUtils.crystalDamage(mc.player, vec3d);

            switch (dmgToPlayer.get()) {
                case AntiSuicide:
                    if (selfDamage > maxDmg.get() || selfDamage >= EntityUtils.getTotalHealth(mc.player)) return;
                case Invincibility:
                    if (selfDamage > maxDmg.get() && mc.player.hurtTime <= 0) return;
            }

            for (Entity entity : mc.world.getEntities()) {
                if (antiFriendPop.get() && entity instanceof PlayerEntity friend && Friends.get().isFriend(friend)) {
                    double friendDamage = CrystalUtils.crystalDamage(friend, vec3d);

                    if (friendDamage > maxFriendDmg.get()) return;
                }
            }

            // Check damage to targets and face place
            double damage = getDamageToTargets(vec3d, false, !hasBlock && support.get() == SupportMode.Fast);

            boolean surroundBreaking = (shouldSurroundBreak(blockPos) && !shouldFacePlace() && i.get() == 0);
            if (surroundBreaking) i.getAndIncrement();
            if (!shouldFacePlace() && !surroundBreaking && damage < minDmg.get()) return;

            // Check if it can be placed
            double x = X(bp);
            double y = Y(bp) + 1;
            double z = Z(bp);
            ((IBox) box).set(x, y, z, x + 1, y + (placement112.get() ? 1 : 2), z + 1);

            if (intersectsWithEntities(box)) return;

            if (predict.get()) {
                Box bx = returnPredictBox(bestTarget, predictCollision.get(), predictIncrease.get());
                if (box.intersects(bx)) return;
            }

            // Compare damage
            if (damage > bestDamage.get() || (isSupport.get() && hasBlock)) {
                bestDamage.set(damage);
                bestBlockPos.get().set(bp);
            }

            if (hasBlock) isSupport.set(false);
        });

        // Place the crystal
        BlockIterator.after(() -> {
            if (bestDamage.get() == 0) return;

            BlockHitResult result = getPlaceInfo(bestBlockPos.get());
            Vec3d pos = Utils.vec3d(bestBlockPos.get());

            ((IVec3d) vec3d).set(
                    result.getBlockPos().getX() + 0.5 + result.getSide().getVector().getX() * 1.0 / 2.0,
                    result.getBlockPos().getY() + 0.5 + result.getSide().getVector().getY() * 1.0 / 2.0,
                    result.getBlockPos().getZ() + 0.5 + result.getSide().getVector().getZ() * 1.0 / 2.0
            );

            if (rotate.get()) {
                double yaw = Rotations.getYaw(vec3d);
                double pitch = Rotations.getPitch(vec3d);

                if (yawStepMode.get() == YawStepMode.Break || doYawSteps(yaw, pitch)) {
                    setRotation(true, vec3d, 0, 0);
                    Rotations.rotate(yaw, pitch, 50, () -> placeCrystal(result, bestDamage.get(), isSupport.get() ? bestBlockPos.get() : null));

                    placeTimer += placeDelay.get();
                }
            } else {
                placeCrystal(result, bestDamage.get(), isSupport.get() ? bestBlockPos.get() : null);
                placeTimer += placeDelay.get();
            }

            if (predictCrystal.get()) {
                EndCrystalEntity crystal = new EndCrystalEntity(mc.world, pos.x + 0.5, pos.y + 1.0, pos.z + 0.5);
                crystal.setId(lastEntityId + last);
                doBreak(crystal);
            }
        });
    }

    private BlockHitResult getPlaceInfo(BlockPos blockPos) {
        ((IVec3d) vec3d).set(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());

        for (Direction side : Direction.values()) {
            ((IVec3d) vec3dRayTraceEnd).set(
                    blockPos.getX() + 0.5 + side.getVector().getX() * 0.5,
                    blockPos.getY() + 0.5 + side.getVector().getY() * 0.5,
                    blockPos.getZ() + 0.5 + side.getVector().getZ() * 0.5
            );

            ((IRaycastContext) raycastContext).set(vec3d, vec3dRayTraceEnd, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult result = mc.world.raycast(raycastContext);

            if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(blockPos)) {
                return result;
            }
        }

        Direction side = blockPos.getY() > vec3d.y ? Direction.DOWN : Direction.UP;
        return new BlockHitResult(vec3d, side, blockPos, false);
    }

    private void placeCrystal(BlockHitResult result, double damage, BlockPos supportBlock) {
        // Switch
        Item targetItem = supportBlock == null ? Items.END_CRYSTAL : Items.OBSIDIAN;

        FindItemResult item = InvUtils.findInHotbar(targetItem);
        if (!item.found()) return;

        int prevSlot = mc.player.getInventory().selectedSlot;

        if (autoSwitch.get() != AutoSwitchMode.None && !item.isOffhand()) Interaction.updateSlot(item, true);

        Hand hand = item.getHand();
        if (hand == null) return;

        // Place
        if (supportBlock == null) {
            // Place crystal
            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, result, 0));

            // Swing
            Interaction.doSwing(swing.get(), packetSwing.get(), hand);

            placing = true;
            placingTimer = 4;
            placingCrystalBlockPos.set(result.getBlockPos()).move(0, 1, 0);

            renderBlocks.add((renderBlockPool.get()).set(result.getBlockPos()));
            renderTimer = renderTime.get();
            renderPos.set(result.getBlockPos());
            renderDamage = damage;
        } else {
            // Place support block
            BlockUtils.place(supportBlock, item, false, 0, true, true, false);
            placeTimer += supportDelay.get();

            if (supportDelay.get() == 0) placeCrystal(result, damage, null);
        }

        // Switch back
        if (autoSwitch.get() == AutoSwitchMode.Silent) Interaction.swapBack(prevSlot);
    }

    // Yaw steps

    @EventHandler
    private void onPacketSent(PacketEvent.Sent event) {
        if (event.packet instanceof PlayerMoveC2SPacket) {
            serverYaw = ((PlayerMoveC2SPacket) event.packet).getYaw((float) serverYaw);
        }
    }

    public boolean doYawSteps(double targetYaw, double targetPitch) {
        targetYaw = MathHelper.wrapDegrees(targetYaw) + 180;
        double serverYaw = MathHelper.wrapDegrees(this.serverYaw) + 180;

        if (distanceBetweenAngles(serverYaw, targetYaw) <= yawSteps.get()) return true;

        double delta = Math.abs(targetYaw - serverYaw);
        double yaw = this.serverYaw;

        if (serverYaw < targetYaw) {
            if (delta < 180) yaw += yawSteps.get();
            else yaw -= yawSteps.get();
        } else {
            if (delta < 180) yaw -= yawSteps.get();
            else yaw += yawSteps.get();
        }

        setRotation(false, null, yaw, targetPitch);
        Rotations.rotate(yaw, targetPitch, -100, null); // Priority -100 so it sends the packet as the last one, im pretty sure it doesn't matte but idc
        return false;
    }

    private static double distanceBetweenAngles(double alpha, double beta) {
        double phi = Math.abs(beta - alpha) % 360;
        return phi > 180 ? 360 - phi : phi;
    }

    // Others

    private boolean isOutOfRange(Vec3d vec3d, BlockPos blockPos, boolean place) {
        ((IRaycastContext) raycastContext).set(playerEyePos, vec3d, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);

        BlockHitResult result = mc.world.raycast(raycastContext);
        boolean behindWall = result == null || !result.getBlockPos().equals(blockPos);
        double distance = mc.player.getPos().distanceTo(vec3d);

        return distance > (behindWall ? (place ? placeWallsRange : breakWallsRange).get() : (place ? placeRange : breakRange).get());
    }

    private PlayerEntity getNearestTarget() {
        PlayerEntity nearestTarget = null;
        double nearestDistance = Double.MAX_VALUE;

        for (PlayerEntity target : targets) {
            double distance = target.squaredDistanceTo(mc.player);

            if (distance < nearestDistance) {
                nearestTarget = target;
                nearestDistance = distance;
            }
        }

        return nearestTarget;
    }

    private double getDamageToTargets(Vec3d vec3d, boolean breaking, boolean fast) {
        double damage = 0;

        if (fast) {
            PlayerEntity target = getNearestTarget();
            if (!(breaking && target.hurtTime > 0))
                damage = CrystalUtils.crystalDamage(target, vec3d, predict.get(), predictCollision.get(), predictIncrease.get(), null, ignoreTerrain.get());
        } else {
            for (PlayerEntity target : targets) {
                double dmg = CrystalUtils.crystalDamage(target, vec3d, predict.get(), predictCollision.get(), predictIncrease.get(), null, ignoreTerrain.get());

                // Update best target
                if (dmg > bestTargetDamage) {
                    bestTarget = target;
                    bestTargetDamage = dmg;
                    bestTargetTimer = 10;
                }

                damage += dmg;
            }
        }

        return damage;
    }

    @Override
    public String getInfoString() {
        return bestTarget != null && bestTargetTimer > 0 ? bestTarget.getGameProfile().getName() : null;
    }

    private void findTargets() {
        targets.clear();

        // Players
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (isCreative(player) || player == mc.player) continue;

            if (!isDead(player) && isAlive(player) && Friends.get().shouldAttack(player) && player.distanceTo(mc.player) <= targetRange.get()) {
                targets.add(player);
            }
        }

        // Fake players
        for (PlayerEntity player : FakePlayerManager.getFakePlayers()) {
            if (!isDead(player) && isAlive(player) && Friends.get().shouldAttack(player) && player.distanceTo(mc.player) <= targetRange.get()) {
                targets.add(player);
            }
        }
    }

    private boolean intersectsWithEntities(Box box) {
        return EntityUtils.intersectsWithEntity(box, entity -> !entity.isSpectator() && !removed.contains(entity.getId()));
    }

    // Render
    @EventHandler
    private void onRender3d(Render3DEvent event) {
        RenderInfo ri = new RenderInfo(event, renderMode.get(), shapeMode.get());

        if (renderTimer > 0 && renderMode.get() != RenderMode.None) {
            if (fade.get()) {
                renderBlocks.sort(Comparator.comparingInt((o) -> -o.ticks));
                renderBlocks.forEach((renderBlock) -> renderBlock.render(ri, sideColor.get(), lineColor.get()));
                return;
            }

            render(ri, renderPos, sideColor.get(), lineColor.get(), height.get());
        }


        if (breakRenderTimer > 0 && renderBreak.get() && !mc.world.getBlockState(breakRenderPos).isAir()) {
            int preSideA = sideColor.get().a;
            sideColor.get().a -= 20;
            sideColor.get().validate();

            int preLineA = lineColor.get().a;
            lineColor.get().a -= 20;
            lineColor.get().validate();

            if (fade.get()) {
                renderBlocks.sort(Comparator.comparingInt((o) -> -o.ticks));
                renderBlocks.forEach((renderBlock) -> renderBlock.render(ri, sideColor.get(), lineColor.get()));
            } else render(ri, breakRenderPos, sideColor.get(), lineColor.get(), height.get());

            sideColor.get().a = preSideA;
            lineColor.get().a = preLineA;
        }
    }

    public boolean shouldFacePlace() {
        if (!facePlace.get()) return false;

        for (PlayerEntity target : targets) {
            if (!isSurrounded(target) || isTrapped(target)) return false;
            if (facePlaceHurt.get() && target.hurtTime != 0) return false;

            if (forceFacePlace.get().isPressed()) return true;
            if (EntityUtils.getTotalHealth(target) <= facePlaceHealth.get()) return true;

            for (ItemStack itemStack : target.getArmorItems()) {
                if (facePlaceArmor.get()) {
                    if (itemStack == null || itemStack.isEmpty()) return true;
                    else {
                        boolean shouldBreakArmor = (double) (itemStack.getMaxDamage() - itemStack.getDamage()) / itemStack.getMaxDamage() * 100 <= armorDurability.get();
                        if (shouldBreakArmor) return true;
                    }
                }
            }
        }

        return false;
    }

    public int getDelay() {
        if (shouldSurroundHold()) return 10;

        return breakDelay.get();
    }

    public boolean shouldSurroundBreak(BlockPos crystal) {
        if (!surroundBreak.get()) return false;

        for (PlayerEntity target : targets) {
            if (target != bestTarget) continue;
            if (!trueSurround(bestTarget)) {
                if (sbCrystal != null) attackCrystal(sbCrystal);
                return false;
            }

            BlockPos targetPos = getBlockPos(target);
            Vec3d crystalVec = Vec3d.ofCenter(crystal);
            if (antiSelf.get() && crystal.getY() <= mc.player.getY() && mc.player.getPos().distanceTo(crystalVec) <= 2.7)
                return false;

            for (Direction direction : Direction.values()) {
                if (direction.equals(Direction.UP) || direction.equals(Direction.DOWN)) continue;

                BlockPos offsetPos = targetPos.offset(direction);

                for (Direction direction2 : Direction.values()) {
                    if (direction2.equals(Direction.UP) || direction2.equals(Direction.DOWN)) continue;

                    if (isCombatBlock(offsetPos) && crystal.equals(offsetPos.offset(direction2))) {
                        sbCrystal = getSbCrystal(offsetPos.offset(direction2));
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public EndCrystalEntity getSbCrystal(BlockPos blockPos) {
        for (Entity entity : getEntities()) {
            if (entity instanceof EndCrystalEntity crystal && getBlockPos(crystal).equals(blockPos)) return crystal;
        }

        return null;
    }

    public boolean shouldSurroundHold() {
        if (!surroundHold.get()) return false;

        for (PlayerEntity target : targets) {
            BlockPos targetPos = getBlockPos(target);

            for (Direction direction : Direction.values()) {
                if (direction.equals(Direction.UP)) continue;

                BlockPos offsetPos = targetPos.offset(direction);

                for (Direction direction2 : Direction.values()) {
                    if (direction2.equals(Direction.UP)) continue;

                    for (Entity entity : mc.world.getEntities()) {
                        if (entity instanceof EndCrystalEntity crystal) {
                            BlockPos crystalPos = getBlockPos(crystal);


                            if (crystalPos.equals(offsetPos)) return true;
                            if (extraPlaces.get() && crystalPos.equals(offsetPos.offset(direction2))) return true;
                        }
                    }
                }
            }
        }

        return false;
    }


    public boolean trueSurround(LivingEntity entity) {
        BlockPos entityBlockPos = getBlockPos(entity);

        return isCombatBlock(entity.getBlockPos().add(1, 0, 0))
                || isCombatBlock(entityBlockPos.add(-1, 0, 0))
                || isCombatBlock(entityBlockPos.add(0, 0, 1))
                || isCombatBlock(entityBlockPos.add(0, 0, -1));
    }

    public boolean isTrapped(LivingEntity t) {
        BlockPos p = t.getBlockPos().up();
        return isBlastResist(p.south()) && isBlastResist(p.north()) && isBlastResist(p.west()) && isBlastResist(p.east());
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (renderMode.get() == RenderMode.None || renderTimer <= 0 || !damageText.get()) return;

        if (renderMode.get() != RenderMode.UpperSide || renderMode.get() != RenderMode.LowerSide) {
            vec3.set(renderPos.getX() + 0.5, renderPos.getY() + 0.5, renderPos.getZ() + 0.5);
        } else vec3.set(renderPos.getX() + 0.5, renderPos.getY() + 1.1, renderPos.getZ() + 0.5);

        if (NametagUtils.to2D(vec3, damageTextScale.get())) {
            NametagUtils.begin(vec3);
            TextRenderer.get().begin(1, false, true);

            String text = String.format("%.1f", renderDamage);
            double w = TextRenderer.get().getWidth(text) / 2;
            TextRenderer.get().render(text, -w, 0, lineColor.get(), true);

            TextRenderer.get().end();
            NametagUtils.end();
        }
    }

    public class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public int ticks;

        public void tick() {
            --this.ticks;
        }

        public void render(RenderInfo ri, Color sides, Color lines) {
            int preSideA = sides.a;
            int preLineA = lines.a;
            sides.a = (int) ((double) sides.a * ((double) this.ticks / (double) fadeAmount.get()));
            lines.a = (int) ((double) lines.a * ((double) this.ticks / (double) fadeAmount.get()));
            RenderUtils.render(ri, this.pos, sides, lines, height.get());
            sides.a = preSideA;
            lines.a = preLineA;
        }

        public RenderBlock set(BlockPos blockPos) {
            this.pos.set(blockPos);
            this.ticks = fadeTime.get();
            return this;
        }
    }

    public enum YawStepMode {
        Break, All
    }

    public enum AutoSwitchMode {
        Normal, Silent, None
    }

    public enum SupportMode {
        Disabled, Accurate, Fast
    }

    public enum CancelCrystal {
        NoDesync, OnHit
    }

    public enum DamageToPlayer {
        Suicide, AntiSuicide, Invincibility
    }
}
