package kassuk.addon.blackout.modules.combat;

import kassuk.addon.blackout.BlackOut;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import kassuk.addon.blackout.utils.*;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.compiler.Expr;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static kassuk.addon.blackout.utils.bed.basic.EntityInfo.isInHole;
import static kassuk.addon.blackout.utils.bed.basic.EntityInfo.getBlockPos;
import static kassuk.addon.blackout.utils.bed.basic.BlockInfo.isCombatBlock;
import static kassuk.addon.blackout.utils.bed.basic.BlockInfo.getSphere;
import static kassuk.addon.blackout.utils.bed.advance.PacketUtils.packetPlace;

public class AutoTraprewrite extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgNone = settings.createGroup("");

    // General
    private final Setting<Integer> bpt = sgGeneral.add(new IntSetting.Builder().name("blocks-per-tick").description("How many blocks can be placed per one tick.").defaultValue(2).sliderMin(1).sliderMax(5).build());
    public final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range").description("The range at which players can be targeted.").defaultValue(5).sliderRange(1, 7).build());
    public final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder().name("place-range").description("The range at which blocks can be placed.").defaultValue(4.5).sliderRange(1, 6).build());
    private final Setting<TopMode> modes = sgGeneral.add(new EnumSetting.Builder<TopMode>().name("mode").description("Which positions to place.").defaultValue(TopMode.Full).build());
    private final Setting<Boolean> betweenBlocks = sgGeneral.add(new BoolSetting.Builder().name("between-blocks").description("Places blocks around of target if he stand between blocks.").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Sends rotation packets to the server when placing.").defaultValue(false).build());
    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder().name("block").description("Which blocks used for placing.").defaultValue(Collections.singletonList(Blocks.OBSIDIAN)).filter(this::blockFilter).build());
    private final Setting<Boolean> onlyHole = sgGeneral.add(new BoolSetting.Builder().name("only-hole").description("Turning off auto trap if target isnt in hole.").defaultValue(true).build());

    // Render
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").description("Client side hand-swing").defaultValue(true).build());
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders a block overlay where the obsidian will be placed.").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The color of the sides of the blocks being rendered.").defaultValue(new SettingColor(204, 0, 0, 10)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The color of the lines of the blocks being rendered.").defaultValue(new SettingColor(204, 0, 0, 255)).build());

    private static List<BlockPos> placePositions = new ArrayList<>();
    public static PlayerEntity target;
    private int places;

    public AutoTraprewrite() {
        super(BlackOut.COMBATPLUS, "auto-trap-plus", "Places obsidian above target head.");
    }

    @Override
    public void onActivate() {
        if (!placePositions.isEmpty()) placePositions.clear();

    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        target = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestDistance);
        if (TargetUtils.isBadTarget(target, targetRange.get())) return;

        if (onlyHole.get() && !isInHole(target)) {
            toggle();
            return;
        }
        FindItemResult block = InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));

        if (!block.found()) {
            placePositions.clear();
            return;
        }

        if (!placePositions.isEmpty()) placePositions.clear();

        findPlacePos();
        places = 0;

        if (placePositions.isEmpty()) return;

        for (BlockPos b : placePositions) {
            if (places <= bpt.get() && b != null && mc.player.getPos().distanceTo(Utils.vec3d(b)) <= placeRange.get()) {
                    packetPlace(b, InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem()))), rotate.get(), swing.get());
                } else {
                    BlockUtils.place(b, block, rotate.get(), 50);
                }
                places++;
            }
        }


    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || placePositions.isEmpty()) return;
        for (BlockPos pos : placePositions) event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    private void findPlacePos() {
        placePositions.clear();
        BlockPos pos = getBlockPos(target);
        if (betweenBlocks.get() && getSurroundBlocks(target).isEmpty()) return;

        switch (modes.get()) {
            case Full -> {
                if (betweenBlocks.get()) {
                    getSurroundBlocks(target).forEach(this::add);
                } else {
                    add(pos.add(0, 2, 0));
                    add(pos.add(1, 1, 0));
                    add(pos.add(-1, 1, 0));
                    add(pos.add(0, 1, 1));
                    add(pos.add(0, 1, -1));
                }
            }
            case Head -> {
                if (betweenBlocks.get()) {
                    getSurroundBlocks(target).forEach(this::add);
                } else {
                    add(pos.add(0, 2, 0));
                }
            }
            case antiFP -> {
                if (betweenBlocks.get()) {
                    getSurroundBlocks(target).forEach(this::add);
                } else {
                    add(pos.add(1, 1, 0));
                    add(pos.add(-1, 1, 0));
                    add(pos.add(0, 1, 1));
                    add(pos.add(0, 1, -1));
                }
            }
        }
    }

    private boolean blockFilter(Block block) {
        return isCombatBlock(block);
    }

    private void add(BlockPos blockPos) {
            placePositions.add(blockPos);
    }

    private ArrayList<BlockPos> getSurroundBlocks(PlayerEntity player) {
        ArrayList<BlockPos> positions = new ArrayList<>();
        List<Entity> getEntityBoxes;

        if (byMode() == null) return positions;
        for (BlockPos blockPos : getSphere(player.getBlockPos().up((Integer) byMode()[0]), 3, (Integer) byMode()[1])) {
            getEntityBoxes = mc.world.getOtherEntities(null, new Box(blockPos), entity -> entity == player);
            if (!getEntityBoxes.isEmpty()) continue;

            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP || ((Boolean) byMode()[2]) && direction == Direction.DOWN) continue;

                getEntityBoxes = mc.world.getOtherEntities(null, new Box(blockPos.offset(direction)), entity -> entity == player);
                if (!getEntityBoxes.isEmpty()) positions.add(blockPos);
            }
        }

        return positions;
    }

    private Object[] byMode() {
        return switch (modes.get()) {
            case Full -> new Object[]{2,1, false};
            case Head -> new Object[]{3,1, false};
            case antiFP -> new Object[]{2,1, true};
        };
    }

    public enum TopMode {
        Full, Head, antiFP
    }
}
