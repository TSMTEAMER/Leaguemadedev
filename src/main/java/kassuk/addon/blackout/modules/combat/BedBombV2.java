package kassuk.addon.blackout.modules.combat;

import com.google.common.util.concurrent.AtomicDouble;
import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.utils.OLEPOSSUtils;
import kassuk.addon.blackout.utils.bed.port.advance.BedUtils;
import kassuk.addon.blackout.utils.bed.port.advance.Task;
import kassuk.addon.blackout.utils.bed.port.basic.BlockInfo;
import kassuk.addon.blackout.utils.bed.port.basic.Vec3dInfo;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import static kassuk.addon.blackout.utils.bed.port.advance.BedUtils.*;
import static kassuk.addon.blackout.utils.bed.port.basic.BlockInfo.*;
import static kassuk.addon.blackout.utils.bed.port.basic.EntityInfo.*;
import static kassuk.addon.blackout.utils.bed.port.basic.Vec3dInfo.closestVec3d;

public class BedBombV2 extends Module {
    public BedBombV2() {
        super(BlackOut.COMBATPLUS, "Bed Bomb V2", "Automatically places and explodes beds in the Nether and End :massivetroll:.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPredict = settings.createGroup("Predict");
    private final SettingGroup sgPvP = settings.createGroup("PvP");
    private final SettingGroup sgTrapBreaker = settings.createGroup("Trap Breaker");
    private final SettingGroup sgBurrowBreaker = settings.createGroup("Burrow Breaker");
    private final SettingGroup sgStringBreaker = settings.createGroup("String Breaker");
    private final SettingGroup sgOther = settings.createGroup("Other");
    private final SettingGroup sgBedRefill = settings.createGroup("Bed Re-fill");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgNone = settings.createGroup("");

    // General
    public final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder().name("place-delay").description("The delay between placing beds in ticks.").defaultValue(10).sliderRange(0, 20).build());
    public final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range").description("The range at which players can be targeted.").defaultValue(15).sliderRange(1, 25).build());
    public final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder().name("place-range").description("The range at which beds can be placed.").defaultValue(4.5).sliderRange(1, 7).build());
    public final Setting<Double> minTargetDamage = sgGeneral.add(new DoubleSetting.Builder().name("min-target-damage").description("The minimum damage to inflict on your target.").defaultValue(7).range(0, 36).sliderMax(36).build());
    public final Setting<Double> maxSelfDamage = sgGeneral.add(new DoubleSetting.Builder().name("max-self-damage").description("The maximum damage to inflict on yourself.").defaultValue(4).range(0, 36).sliderMax(36).build());
    public final Setting<Boolean> antiFriendPop = sgGeneral.add(new BoolSetting.Builder().name("anti-friend-pop").description("Prevents from popping friends.").defaultValue(false).build());
    public final Setting<Double> maxFriendDamage = sgGeneral.add(new DoubleSetting.Builder().name("max-damage").description("Maximum damage that beds can deal to your friends.").defaultValue(6).range(0, 36).sliderMax(36).visible(antiFriendPop::get).build());
    public final Setting<Boolean> ignoreTerrain = sgGeneral.add(new BoolSetting.Builder().name("ignore-terrain").description("Completely ignores terrain if it can be blown up by beds.").defaultValue(true).build());
    private final Setting<Boolean> strictDirection = sgGeneral.add(new BoolSetting.Builder().name("strict-direction").description("Only places beds in the direction you are facing.").defaultValue(false).build());
    public final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder().name("debug").description("Sends info in chat about calculation.").defaultValue(false).build());

    // Predict
    public final Setting<Boolean> predict = sgPredict.add(new BoolSetting.Builder().name("predict-position").description("Predicts target position.").defaultValue(true).build());
    public final Setting<Integer> predictIncrease = sgPredict.add(new IntSetting.Builder().name("predict-increase").description("Increasing range from predicted position to target.").defaultValue(2).sliderRange(1, 4).min(1).max(4).visible(predict::get).build());
    public final Setting<Boolean> predictCollision = sgPredict.add(new BoolSetting.Builder().name("predict-collision").description("Whether to consider collision when predicting.").defaultValue(true).visible(predict::get).build());

    // PvP
    private final Setting<Boolean> lay = sgPvP.add(new BoolSetting.Builder().name("auto-lay").description("Esu enemy.").defaultValue(true).build());
    private final Setting<Keybind> forceLay = sgPvP.add(new KeybindSetting.Builder().name("force-lay").description("AutoLay starts work if the keybind is pressed. Useful agains player with bed instamine.").defaultValue(Keybind.none()).build());
    public final Setting<Integer> allowedFails = sgPvP.add(new IntSetting.Builder().name("fail-times").description("How much AutoLay fails can be dealed.").defaultValue(2).sliderRange(0, 10).min(0).max(10).visible(lay::get).build());
    private final Setting<Boolean> zeroTick = sgPvP.add(new BoolSetting.Builder().name("zero-tick").description("Tries to zero tick your target faster.").defaultValue(true).build());

    // Trap Breaker
    public final Setting<Boolean> tBreakerMain = sgTrapBreaker.add(new BoolSetting.Builder().name("trap-breaker").description("Breaks targets self trap and prevent re-trapping.").defaultValue(false).build());
    private final Setting<MineMode> tBreakerMode = sgTrapBreaker.add(new EnumSetting.Builder<MineMode>().name("mine-method").defaultValue(MineMode.Client).visible(tBreakerMain::get).build());
    private final Setting<Boolean> tBreakerSwap = sgTrapBreaker.add(new BoolSetting.Builder().name("auto-swap").description("Automatically switches to pickaxe slot.").defaultValue(true).visible(tBreakerMain::get).build());
    private final Setting<Boolean> tBreakerOnlySur = sgTrapBreaker.add(new BoolSetting.Builder().name("surround-only").description("Works only while player is surrounded.").defaultValue(true).visible(tBreakerMain::get).build());
    private final Setting<Boolean> tBreakerGround = sgTrapBreaker.add(new BoolSetting.Builder().name("only-on-ground").description("Works only while player is standing on ground.").defaultValue(true).visible(tBreakerMain::get).build());

    // Burrow Breaker
    public final Setting<Boolean> bBreakerMain = sgBurrowBreaker.add(new BoolSetting.Builder().name("burrow-breaker").description("Breaks targets burrow and prevent re-burrowing.").defaultValue(false).build());
    private final Setting<MineMode> bBreakerMode = sgBurrowBreaker.add(new EnumSetting.Builder<MineMode>().name("mine-method").defaultValue(MineMode.Client).visible(bBreakerMain::get).build());
    private final Setting<Boolean> bBreakerSwap = sgBurrowBreaker.add(new BoolSetting.Builder().name("auto-swap").description("Automatically switches to pickaxe slot.").defaultValue(true).visible(bBreakerMain::get).build());
    private final Setting<Boolean> bBreakerOnlySur = sgBurrowBreaker.add(new BoolSetting.Builder().name("surround-only").description("Works only while player is surrounded.").defaultValue(true).visible(bBreakerMain::get).build());
    private final Setting<Boolean> bBreakerGround = sgBurrowBreaker.add(new BoolSetting.Builder().name("only-on-ground").description("Works only while player is standing on ground.").defaultValue(true).visible(bBreakerMain::get).build());

    // String Breaker
    public final Setting<Boolean> sBreakerMain = sgStringBreaker.add(new BoolSetting.Builder().name("string-breaker").description("Breaks strings around target.").defaultValue(false).build());
    private final Setting<MineMode> sBreakerMode = sgStringBreaker.add(new EnumSetting.Builder<MineMode>().name("mine-method").defaultValue(MineMode.Packet).visible(sBreakerMain::get).build());
    private final Setting<Boolean> sBreakerOnlySur = sgStringBreaker.add(new BoolSetting.Builder().name("surround-only").description("Works only while player is surrounded.").defaultValue(true).visible(sBreakerMain::get).build());
    private final Setting<Boolean> sBreakerGround = sgStringBreaker.add(new BoolSetting.Builder().name("only-on-ground").description("Works only while player is standing on ground.").defaultValue(true).visible(sBreakerMain::get).build());

    // Other
    private final Setting<Boolean> pauseOnUse = sgOther.add(new BoolSetting.Builder().name("pause-on-use").description("Pauses while using items.").defaultValue(true).build());
    private final Setting<Boolean> pauseOnCA = sgOther.add(new BoolSetting.Builder().name("pause-on-CA").description("Pauses while Crystal Aura is activated.").defaultValue(false).build());
    private final Setting<Boolean> hurtTime = sgOther.add(new BoolSetting.Builder().name("hurt-time").description("Place only while target can recieve damage. Not recommended to use this.").defaultValue(false).build());

    // Bed Re-fill
    private final Setting<Boolean> bedRefill = sgBedRefill.add(new BoolSetting.Builder().name("bed-refill").description("Moves beds into a selected hotbar slot.").defaultValue(true).build());
    private final Setting<Integer> bedSlot = sgBedRefill.add(new IntSetting.Builder().name("bed-slot").description("The slot auto move moves beds to.").defaultValue(7).min(1).max(9).sliderMin(1).sliderMax(9).visible(bedRefill::get).build());

    // Render
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders the block where it is placing a bed.").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color for positions to be placed.").defaultValue(new SettingColor(255, 0, 170, 90)).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color for positions to be placed.").defaultValue(new SettingColor(255, 0, 170, 10)).build());
    private final Setting<SettingColor> textColor = sgRender.add(new ColorSetting.Builder().name("Text Color").description("The text color for positions to be mined.").defaultValue(new SettingColor(255, 0, 170, 10)).build());


    public static ExecutorService cached = Executors.newCachedThreadPool();
    AtomicDouble bestDamage = new AtomicDouble(0);
    private BlockPos finalPos = null;
    int placeTicks, countTicks, failTimes;
    public static PlayerEntity target;
    private final ArrayList<PlayerEntity> targets = new ArrayList<>();
    private CardinalDirection placeDirection;
    double offsetTargetDamage;
    boolean smartLay;
    BlockPos prevBreakPos;
    Boolean Boolean;

    private final Task breakTask = new Task();
    private final Task infoTask = new Task();
    private final Task stageTask = new Task();
    private final Task secondStageTask = new Task();

    private List<BlockPos> strings = new ArrayList<>();
    private final Pool<RenderText> renderTextPool = new Pool<>(RenderText::new);
    private final List<RenderText> renderTexts = new ArrayList<>();

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    private final Pool<RenderBreak> renderBreakPool = new Pool<>(RenderBreak::new);
    private final List<RenderBreak> renderBreaks = new ArrayList<>();

    public enum MineMode {
        Packet,
        Client
    }

    public void onActivate() {
        infoTask.reset();
        breakTask.reset();
        stageTask.reset();
        secondStageTask.reset();
        failTimes = -1;
        finalPos = null;
        placeDirection = null;
        smartLay = true;
        countTicks = placeDelay.get();
        placeTicks = 0;
        bestDamage.set(0);

        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
        for (RenderBreak renderBreak : renderBreaks) renderBreakPool.free(renderBreak);
        renderBreaks.clear();
        for (RenderText renderText : renderTexts) renderTextPool.free(renderText);
        renderTexts.clear();
    }


    public void onDeactivate() {
        bestDamage.set(0);

        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
        for (RenderBreak renderBreak : renderBreaks) renderBreakPool.free(renderBreak);
        renderBreaks.clear();
        for (RenderText renderText : renderTexts) renderTextPool.free(renderText);
        renderTexts.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST + 1000)
    private void onTick(TickEvent.Pre event) {
        if (mc.world.getDimension().bedWorks()) {
            warning("You can't blow up beds in this dimension.");
            return;
        }

        boolean sHurt;
        countTicks = placeDelay.get();
        placeTicks--;

        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);

        renderBreaks.forEach(RenderBreak::tick);
        renderBreaks.removeIf(renderBreak -> renderBreak.ticks <= 0);

        renderTexts.forEach(RenderText::tick);
        renderTexts.removeIf(renderText -> renderText.ticks <= 0);

        if (pauseOnCA.get() && (Modules.get().get(CrystalAura.class).isActive() || Modules.get().get(AutoCrystal.class).isActive() || Modules.get().get(AutoCrystalPlus.class).isActive()))
            return;

        target = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestDistance);
        if (TargetUtils.isBadTarget(target, targetRange.get()) || pauseOnUse.get() && mc.player.isUsingItem()) return;

        if ((lay.get() || forceLay.get().isPressed()) && isSurrounded(target) && !isFaceTrapped(target)) {
            calculateHolePos();

            if (placeDirection != null && finalPos != null) {

                int i = placeDelay.get() <= 9 ? 0 : placeDelay.get() / 2;
                if (failTimes >= allowedFails.get() || smartLay == true) i = 0;
                countTicks = placeDelay.get() - i;
                if (placeTicks <= 0) {
                    bedRefill();
                    doHolePlace();
                    placeTicks = countTicks;
                }
            }
            return;
        } else if (!isFaceTrapped(target)){
            smartLay = true;
            failTimes = -1;
        }

        if (target.hurtTime != 0 && hurtTime.get()) sHurt = false;
        else sHurt = true;

        if (EntityUtils.getTotalHealth(target) <= 11 && zeroTick.get() && !isSurrounded(target)) {
            int i = placeDelay.get() <= 9 ? 0 : placeDelay.get() / 2;
            countTicks = placeDelay.get() - i;
            sHurt = true;
        }
        cached.execute(this::calculatePos);
        if (placeTicks <= 0 && sHurt) {
            if (finalPos == null || placeDirection == null) return;
            bedRefill();
            doPlace();
            placeTicks = countTicks;
        }
    }

    @EventHandler
    private void onPreSlowTick(TickEvent.Pre event) {
        if (TargetUtils.isBadTarget(target, targetRange.get()) || pauseOnUse.get() && mc.player.isUsingItem()) return;

        if (bBreakerMain.get()
                && (!bBreakerGround.get() || mc.player.isOnGround())
                && (!bBreakerOnlySur.get() || isSurrounded(mc.player))
                && BedUtils.shouldBurrowBreak()) {

            BlockPos burrowBp = target.getBlockPos();
            infoTask.run(() -> {
                renderBreaks.add(renderBreakPool.get().set(burrowBp));
                warning("Burrow Breaker triggered!");

            });

            switch (bBreakerMode.get()) {
                case Packet -> packetMine(burrowBp, bBreakerSwap.get(), breakTask);
                case Client -> normalMine(burrowBp, bBreakerSwap.get());
            }
            return;
        } else {
            stageTask.run(() -> {
                infoTask.reset();
                breakTask.reset();
            });

            if (tBreakerMain.get()
                    && (!tBreakerGround.get() || mc.player.isOnGround())
                    && (!tBreakerOnlySur.get() || isSurrounded(mc.player))
                    && BedUtils.shouldTrapBreak()) {

                BlockPos trapBp = BedUtils.getTrapBlock(target, 4.5);

                infoTask.run(() -> {
                    renderBreaks.add(renderBreakPool.get().set(trapBp));
                        warning("Trap Breaker triggered!");
                });

                switch (tBreakerMode.get()) {
                    case Packet -> packetMine(trapBp, tBreakerSwap.get(), breakTask);
                    case Client -> normalMine(trapBp, tBreakerSwap.get());
                }
                return;
            } else {
                secondStageTask.run(() -> {
                    infoTask.reset();
                    breakTask.reset();
                });
                if (sBreakerMain.get()
                        && (!sBreakerGround.get() || mc.player.isOnGround())
                        && (!sBreakerOnlySur.get() || isSurrounded(mc.player))
                        && BedUtils.shouldStringBreak()) {

                    strings.clear();
                    for (CardinalDirection dir : CardinalDirection.values()) {
                        if (strictDirection.get()
                            && dir.toDirection() != mc.player.getHorizontalFacing()
                            && dir.toDirection().getOpposite() != mc.player.getHorizontalFacing()) continue;
                        BlockPos cPos = target.getBlockPos().up();

                        if (mc.world.getBlockState(cPos).getBlock().asItem().equals(Items.STRING) && mc.player.getPos().distanceTo(getCenterVec3d(cPos)) < 4.5)
                            strings.add(cPos);

                        if (mc.world.getBlockState(cPos.offset(dir.toDirection())).getBlock().asItem().equals(Items.STRING) && mc.player.getPos().distanceTo(getCenterVec3d(cPos.offset(dir.toDirection()))) < 4.5)
                            strings.add(cPos.offset(dir.toDirection()));
                    }
                    if (!strings.isEmpty()) {
                        infoTask.run(() -> {
                              info("String Breaker triggered!");
                        });
                        for (BlockPos p : strings) {
                            renderTexts.add(renderTextPool.get().set(p, "String"));
                            switch (sBreakerMode.get()) {
                                case Packet -> packetMine(p, false, breakTask);
                                default -> normalMine(p, false);
                            }
                        }
                    }
                }
            }
        }

        secondStageTask.reset();
        stageTask.reset();
        infoTask.reset();
        breakTask.reset();
    }

    @EventHandler
    public void onBreakPacket(PacketEvent.Receive event) {
        if (!lay.get() || mc.world == null || mc.player == null || target == null || finalPos == null || placeDirection == null)
            return;
        if (event.packet instanceof BlockBreakingProgressS2CPacket) {
            BlockBreakingProgressS2CPacket packet = (BlockBreakingProgressS2CPacket) event.packet;
            BlockPos packetBp = packet.getPos();

            if (packetBp.equals(prevBreakPos) && packet.getProgress() > 0) return;

            Boolean = getBlock(packetBp) instanceof BedBlock;

            if (Boolean && packetBp.equals(finalPos))
                smartLay = false;

            else if (Boolean && packetBp.equals(finalPos.offset(placeDirection.toDirection())))
                smartLay = false;

            prevBreakPos = packetBp;
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!render.get()) return;
        renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
        renderBlocks.forEach(renderBlock -> renderBlock.render(event, sideColor.get(), lineColor.get(), shapeMode.get()));

        renderBreaks.sort(Comparator.comparingInt(o -> -o.ticks));
        renderBreaks.forEach(renderBreak -> renderBreak.render(event, sideColor.get(), lineColor.get(), shapeMode.get()));
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!render.get()) return;
        renderTexts.sort(Comparator.comparingInt(o -> -o.ticks));
        renderTexts.forEach(renderText -> renderText.render(event, textColor.get()));
    }

    private void bedRefill() {
        if (bedRefill.get()) {
            FindItemResult bedItem = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
            if (bedItem.found() && bedItem.slot() != bedSlot.get() - 1) {
                InvUtils.move().from(bedItem.slot()).toHotbar(bedSlot.get() - 1);
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(bedSlot.get() - 1));
            }
        }
    }

    private void doPlace() {
        FindItemResult bedItem = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BedItem);
        assert bedItem.isHotbar();

        BlockHitResult placeResult = new BlockHitResult(closestVec3d(finalPos), Direction.UP, finalPos, false);
        BlockHitResult breakResult = new BlockHitResult(closestVec3d(finalPos), Direction.UP, finalPos, false);

        double y = switch (placeDirection) {
            case North -> 180;
            case East -> -90;
            case West -> 90;
            case South -> 0;
        };
        double p = Rotations.getPitch(closestVec3d(finalPos));

        Rotations.rotate(y, p, 1000000, () -> {
            int prevSlot = mc.player.getInventory().selectedSlot;
            InvUtils.swap(bedItem.slot(), false);
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, placeResult, 0));
            mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, breakResult);
            mc.player.getInventory().selectedSlot = prevSlot;
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.OFF_HAND));
            renderBlocks.add(renderBlockPool.get().set(finalPos, placeDirection));
            bestDamage.set(0);
            finalPos = null;
            placeDirection = null;
        });
    }

    private void doHolePlace() {
        FindItemResult bedItem = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BedItem);
        if (finalPos == null || !bedItem.isHotbar()) return;

        if (!(mc.world.getBlockState(finalPos).getBlock() instanceof BedBlock)) failTimes++;
        BlockHitResult placeResult = new BlockHitResult(closestVec3d(finalPos), Direction.UP, finalPos, false);
        BlockHitResult breakResult = new BlockHitResult(closestVec3d(finalPos), Direction.UP, finalPos, false);

        double y = switch (placeDirection) {
            case North -> 180;
            case East -> -90;
            case West -> 90;
            case South -> 0;
        };
        double p = Rotations.getPitch(closestVec3d(finalPos));
        Rotations.rotate(y, p, () -> {
            int prevSlot = mc.player.getInventory().selectedSlot;
            InvUtils.swap(bedItem.slot(), false);
            if (failTimes >= allowedFails.get()) {
                mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, placeResult, 0));
                mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, breakResult);
            } else {
                mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, breakResult);
                mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, placeResult, 0));
            }
            mc.player.getInventory().selectedSlot = prevSlot;
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.OFF_HAND));
            renderBlocks.add(renderBlockPool.get().set(finalPos, placeDirection));
            bestDamage.set(0);
            finalPos = null;
            placeDirection = null;
        });
    }

    private void calculateHolePos() {
        long startTime = System.currentTimeMillis();
        BlockPos p = getBlockPos(target).up();

        double selfDMG = BedUtils.getDamage(mc.player, getCenterVec3d(p), false, false, 0, true);
        double targetDMG = BedUtils.getDamage(target, getCenterVec3d(p), false, false, 0, true);

        if (canBed(p.north(), p) && isWithinRange(p.north(), placeRange.get()) && selfDMG < maxSelfDamage.get() && targetDMG > minTargetDamage.get()) {
            finalPos = p.north();
            placeDirection = CardinalDirection.South;
        } else if (canBed(p.south(), p) && isWithinRange(p.south(), placeRange.get()) && selfDMG < maxSelfDamage.get() && targetDMG > minTargetDamage.get()) {
            finalPos = p.south();
            placeDirection = CardinalDirection.North;
        } else if (canBed(p.east(), p) && isWithinRange(p.east(), placeRange.get()) && selfDMG < maxSelfDamage.get() && targetDMG > minTargetDamage.get()) {
            finalPos = p.east();
            placeDirection = CardinalDirection.West;
        } else if (canBed(p.west(), p) && isWithinRange(p.west(), placeRange.get()) && selfDMG < maxSelfDamage.get() && targetDMG > minTargetDamage.get()) {
            finalPos = p.west();
            placeDirection = CardinalDirection.East;
        }
    }



    private void calculatePos() {
        long startTime = System.currentTimeMillis();
        int radius = (int) mc.player.distanceTo(target);
        radius -= 2;
        if (radius < 2) radius = 2;
        if (radius > 6) radius = 6;
        ArrayList<BlockPos> sphere = new ArrayList<>(getTargetSphere(target, radius, 3));
        CardinalDirection localDirection = null;
        BlockPos localPos = null;

        try {
            for (BlockPos p : sphere) {
                offsetTargetDamage = 0;
                //removing bad blocks, better for optimization
                if (intersectsWithEntities(p)) continue;
                if (!Vec3dInfo.isWithinRange(BlockInfo.closestVec3d(p), placeRange.get()) || !OLEPOSSUtils.replaceable(p)) continue;

                //4 times loop for every direction
                for (CardinalDirection d : CardinalDirection.values()) {
                    double targetDMG = BedUtils.getDamage(target, getCenterVec3d(p.offset(d.toDirection())), predict.get(), predictCollision.get(), predictIncrease.get(), ignoreTerrain.get());
                    double selfDMG = BedUtils.getDamage(mc.player, getCenterVec3d(p.offset(d.toDirection())), predict.get(), predictCollision.get(), predictIncrease.get(), ignoreTerrain.get());
                    double friendDMG = 0;

                    if (antiFriendPop.get()) {
                        for (Entity entity : mc.world.getEntities()) {
                            if (entity instanceof PlayerEntity friend && Friends.get().isFriend(friend)) {
                                friendDMG = BedUtils.getDamage(friend, getCenterVec3d(p.offset(d.toDirection())), false, false, 1, false);
                            }
                        }
                    }

                    if (!canBed(p, p.offset(d.toDirection()))
                            || selfDMG > maxSelfDamage.get()
                            || targetDMG < minTargetDamage.get()
                            || (friendDMG != 0 && friendDMG > maxFriendDamage.get())) continue;

                    offsetTargetDamage = targetDMG;
                    if (offsetTargetDamage > bestDamage.get()) {
                        bestDamage.set(offsetTargetDamage);
                        localDirection = d;
                        localPos = p.toImmutable();
                    }
                }
            }
        } catch (Exception e) {
            e.fillInStackTrace();
        }

        if (localPos == null || localDirection == null) return;
        finalPos = localPos;
        placeDirection = localDirection;
    }

    private boolean intersectsWithEntities(BlockPos blockPos) {
        Box box = new Box(
                blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockPos.getX() + 1, blockPos.getY() + 0.6, blockPos.getZ() + 1
        );

        return EntityUtils.intersectsWithEntity(box, entity ->
                entity instanceof PlayerEntity || entity instanceof EndCrystalEntity || entity instanceof TntEntity
        );
    }


    public String getInfoString() {
        return target != null ? target.getGameProfile().getName() : null;
    }
}
