package kassuk.addon.blackout.modules.combat;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.utils.bed.port.basic.RenderInfo;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;

import static kassuk.addon.blackout.utils.bed.port.basic.EntityInfo.getBlockPos;
import static kassuk.addon.blackout.utils.bed.port.basic.BlockInfo.*;
import static kassuk.addon.blackout.utils.bed.port.advance.RenderUtils.*;
import static kassuk.addon.blackout.utils.bed.port.advance.Interaction.*;
import static meteordevelopment.meteorclient.utils.entity.TargetUtils.getPlayerTarget;
import static meteordevelopment.meteorclient.utils.entity.TargetUtils.isBadTarget;
import static meteordevelopment.meteorclient.utils.player.InvUtils.findInHotbar;
import static meteordevelopment.meteorclient.utils.player.PlayerUtils.distanceTo;
import static meteordevelopment.meteorclient.utils.world.BlockUtils.canPlace;

public class BTPistonPush extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range").description("Range in which to target players.").defaultValue(6).sliderRange(0, 10).build());
    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder().name("place-range").description("Range in which to place blocks.").defaultValue(4.6).sliderRange(0, 7).build());
    private final Setting<Boolean> packetPlace = sgGeneral.add(new BoolSetting.Builder().name("packet").description("Using packet interaction instead of client.").defaultValue(false).build());
    private final Setting<Boolean> antiSelf = sgGeneral.add(new BoolSetting.Builder().name("anti-self").description("Doesn't place if you can push yourself.").defaultValue(false).build());
    private final Setting<Boolean> reverse = sgGeneral.add(new BoolSetting.Builder().name("reverse").description("Placing redstone block and then placing piston.").defaultValue(false).build());
    private final Setting<Boolean> holeFill = sgGeneral.add(new BoolSetting.Builder().name("hole-fill").description("Places obsidian inside of the target block.").defaultValue(true).build());
    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder().name("swap-back").description("Automatically swaps to previous slot.").defaultValue(true).build());
    private final Setting<Boolean> zeroTick = sgGeneral.add(new BoolSetting.Builder().name("zero-tick").description("Places all blocks in one tick.").defaultValue(true).build());
    private final Setting<Boolean> eatPause = sgGeneral.add(new BoolSetting.Builder().name("pause-on-eat").description("Pauses if player is eating.").defaultValue(true).build());

    private final Setting<SwingHand> swing = sgRender.add(new EnumSetting.Builder<SwingHand>().name("swing").description("The way to render swing.").defaultValue(SwingHand.Auto).build());
    private final Setting<Boolean> packetSwing = sgRender.add(new BoolSetting.Builder().name("packet-swing").description("Swings with packets.").defaultValue(false).build());
    private final Setting<RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderMode>().name("render").description("How the render will work.").defaultValue(RenderMode.Box).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color for positions to be placed.").defaultValue(new SettingColor(255, 0, 170, 10)).visible(() -> visibleSide(shapeMode.get())).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color for positions to be placed.").defaultValue(new SettingColor(255, 0, 170, 90)).visible(() -> visibleLine(shapeMode.get())).build());
    private final Setting<Double> height = sgRender.add(new DoubleSetting.Builder().name("height").description("Maximum damage anchors can deal to yourself.").defaultValue(0.99).sliderRange(0, 1).visible(() -> visibleHeight(renderMode.get())).build());

    public BTPistonPush() {
        super(BlackOut.COMBATPLUS, "BT-piston-push", "Pushing P.");
    }

    private BlockPos pistonPos, activatorPos, obsidianPos, targetPos;
    private Direction direction;

    private PlayerEntity target;
    private Stage stage;

    private int prevSlot;

    @Override
    public void onActivate() {
        pistonPos = null;
        activatorPos = null;
        obsidianPos = null;

        direction = null;

        stage = Stage.Preparing;
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        target = getPlayerTarget(targetRange.get(), SortPriority.LowestDistance);
        if (isBadTarget(target, targetRange.get())) {
            info("Target is null.");
            toggle();
            return;
        }

        if (!findInHotbar(Items.PISTON, Items.STICKY_PISTON).found() || !findInHotbar(Items.REDSTONE_BLOCK).found()) {
            info("Required items not found.");
            toggle();
            return;
        }

        if (eatPause.get() && PlayerUtils.shouldPause(false, true, true)) return;

        switch (stage) {
            case Preparing -> {
                prevSlot = mc.player.getInventory().selectedSlot;

                targetPos = getBlockPos(target).up();
                pistonPos = getPistonPos(targetPos);
                activatorPos = getRedstonePos(pistonPos);
                obsidianPos = targetPos.down();

                if (hasNull(targetPos, pistonPos, activatorPos, obsidianPos)) stage = Stage.Toggle;
                if (hasFar(targetPos, pistonPos, activatorPos, obsidianPos)) stage = Stage.Toggle;
                if (antiSelf.get() && hasEntity(new Box(targetPos), entity -> entity == mc.player)) stage = Stage.Toggle;

                stage = zeroTick.get() ? Stage.ZeroTick : (reverse.get() ? Stage.Redstone : Stage.Piston);
            }
            case Piston -> {
                doPlace(InvUtils.findInHotbar(Items.PISTON, Items.STICKY_PISTON), pistonPos);
                stage = reverse.get() ? Stage.Obsidian : Stage.Redstone;
            }
            case Redstone -> {
                doPlace(findInHotbar(Items.REDSTONE_BLOCK), activatorPos);
                stage = reverse.get() ? Stage.Piston : Stage.Obsidian;
            }
            case Obsidian -> {
                if (holeFill.get() && InvUtils.findInHotbar(Items.OBSIDIAN).found()) {
                    doPlace(InvUtils.findInHotbar(Items.OBSIDIAN), obsidianPos);
                }

                stage = Stage.Toggle;
            }
            case ZeroTick -> {
                doPlace(InvUtils.findInHotbar(Items.PISTON, Items.STICKY_PISTON), pistonPos);
                doPlace(findInHotbar(Items.REDSTONE_BLOCK), activatorPos);
                stage = Stage.Toggle;
            }
            case Toggle -> {
                if (swapBack.get()) mc.player.getInventory().selectedSlot = prevSlot;
                toggle();
            }
        }
    }

    private void doPlace(FindItemResult result, BlockPos blockPos) {
        if (isNull(blockPos)) return;
        Hand hand = result.isOffhand() ? Hand.OFF_HAND : Hand.MAIN_HAND;

        Rotations.rotate(getYaw(direction), 0, () -> {
            updateSlot(result, true);
            placeBlock(hand, new BlockHitResult(closestVec3d(blockPos), Direction.DOWN, blockPos, true), packetPlace.get());
            doSwing(swing.get(), packetSwing.get(), result.getHand());
        });
    }

    private int getYaw(Direction direction) {
        if (direction == null) return (int) mc.player.getYaw();
        return switch (direction) {
            case NORTH -> 180;
            case SOUTH -> 0;
            case WEST -> 90;
            case EAST -> -90;
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        };
    }

    private Direction revert(Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.SOUTH;
            case SOUTH -> Direction.NORTH;
            case WEST -> Direction.EAST;
            case EAST -> Direction.WEST;
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        };
    }

    private BlockPos getRedstonePos(BlockPos blockPos) {
        ArrayList<BlockPos> pos = new ArrayList<>();
        if (isNull(blockPos)) return null;

        for (Direction dir : Direction.values()) {
            if (hasEntity(new Box(blockPos.offset(dir)))) continue;

            if (canPlace(blockPos.offset(dir))) pos.add(blockPos.offset(dir));
        }

        if (pos.isEmpty()) return null;
        pos.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));

        return pos.get(0);
    }

    private BlockPos getPistonPos(BlockPos blockPos) {
        ArrayList<BlockPos> pos = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN || dir == Direction.UP) continue;
            if (hasEntity(new Box(blockPos.offset(dir)))) continue;

            boolean canPush = isAir(blockPos.up()) && isAir(blockPos.offset(revert(dir))) && isAir(blockPos.offset(revert(dir)).up());

            if (canPlace(blockPos.offset(dir)) && canPush) {
                pos.add(blockPos.offset(dir));
            }
        }

        if (pos.isEmpty()) return null;

        pos.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));
        direction = getDirection(blockPos, pos.get(0));

        return pos.get(0);
    }

    private Direction getDirection(BlockPos from, BlockPos to) {
        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN || dir == Direction.UP) continue;

            if (from.offset(dir).equals(to)) return dir;
        }

        return null;
    }

    private boolean hasNull(BlockPos... blockPoses) {
        for (BlockPos blockPos : blockPoses) {
            if (isNull(blockPos)) return true;
        }

        return false;
    }

    private boolean hasFar(BlockPos... blockPoses) {
        for (BlockPos blockPos : blockPoses) {
            if (distanceTo(closestVec3d(blockPos)) > placeRange.get()) return true;
        }

        return false;
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        RenderInfo ri = new RenderInfo(event, renderMode.get(), shapeMode.get());

        render(ri, pistonPos, sideColor.get(), lineColor.get(), height.get());
        render(ri, activatorPos, sideColor.get(), lineColor.get(), height.get());

        if (!holeFill.get()) return;
        render(ri, obsidianPos, sideColor.get(), lineColor.get(), height.get());
    }

    @Override
    public String getInfoString() {
        return target != null ? target.getGameProfile().getName() : null;
    }

    public enum Stage {
        Preparing, Piston, Redstone, Obsidian, ZeroTick, Toggle
    }
}
