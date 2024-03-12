package kassuk.addon.blackout.modules.combat;

import kassuk.addon.blackout.BlackOut;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.AbstractBlockAccessor;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
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

import static kassuk.addon.blackout.utils.bed.advance.BedUtils.getTargetSphere;
import static kassuk.addon.blackout.utils.bed.basic.BlockInfo.*;
import static kassuk.addon.blackout.utils.bed.basic.EntityInfo.*;

public class Holefillrewrite extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder().name("blocks-per-tick").defaultValue(2).min(0).sliderRange(0, 6).build());
    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder().name("place-range").defaultValue(4.2).min(1).sliderMin(1).sliderMax(7).build());
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range").defaultValue(7).min(3).sliderMin(3).sliderMax(9).build());
    private final Setting<Double> selfIgnore = sgGeneral.add(new DoubleSetting.Builder().name("self-ignore").defaultValue(1.7).min(0).sliderMin(0).sliderMax(3).build());
    private final Setting<Integer> horizontalRadius = sgGeneral.add(new IntSetting.Builder().name("horizontal-radius").defaultValue(3).min(0).sliderMax(6).build());
    private final Setting<Boolean> doubles = sgGeneral.add(new BoolSetting.Builder().name("doubles").defaultValue(false).build());
    private final Setting<Boolean> self = sgGeneral.add(new BoolSetting.Builder().name("self").defaultValue(false).build());
    private final Setting<Boolean> ignoreSurrounded = sgGeneral.add(new BoolSetting.Builder().name("ignore-surrounded").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(true).build());
    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder().name("block").description("Which blocks used for surround.").defaultValue(Collections.singletonList(Blocks.OBSIDIAN)).filter(this::blockFilter).build());

    // Render
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").defaultValue(true).build());
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders an overlay where blocks will be placed.").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color of the target block rendering.").defaultValue(new SettingColor(200, 200, 150, 10)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color of the target block rendering.").defaultValue(new SettingColor(200, 200, 190)).build());

    public final List<PlayerEntity> targets = new ArrayList<>();
    private final Pool<Hole> holePool = new Pool<>(Hole::new);
    private final List<Hole> holes = new ArrayList<>();
    private final byte NULL = 0;
    private int bpt;

    public Holefillrewrite() {
        super(BlackOut.COMBATPLUS, "Hole Fill Rewrite", "Automatically fills holes around target.");
    }

    @Override
    public void onActivate() {
        targets.clear();
        bpt = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        findTargets();
        for (Hole hole : holes) holePool.free(hole);
        holes.clear();

        FindItemResult block = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem && blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        if (!block.found()) return;

        for (PlayerEntity target : targets) {
            ArrayList<BlockPos> sphere = new ArrayList<>(getTargetSphere(target, horizontalRadius.get(), 6));

            for (BlockPos blockPos : sphere) {
                if (target.getBlockPos().getY() < blockPos.getY()) continue;

                if (!validHole(blockPos)) continue;

                int blocks = 0;
                Direction air = null;

                for (Direction direction : Direction.values()) {
                    if (direction == Direction.UP) continue;

                    if (isBlastResist(blockPos.offset(direction))) blocks++;

                    else if (direction == Direction.DOWN) continue;
                    else if (validHole(blockPos.offset(direction)) && air == null) {
                        for (Direction dir : Direction.values()) {
                            if (dir == direction.getOpposite() || dir == Direction.UP) continue;
                            if (isBlastResist(blockPos.offset(direction).offset(dir))) blocks++;
                            else continue;
                        }

                        air = direction;
                    }
                }

                if (mc.player.getBlockPos().equals(blockPos) && self.get()) blockPos = blockPos.up(2);

                if (blocks == 5 && air == null) holes.add(holePool.get().set(blockPos, NULL));
                else if (blocks == 8 && doubles.get() && air != null) {
                    holes.add(holePool.get().set(blockPos, Dir.get(air)));
                }
            }
        }
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        if (!holes.isEmpty()) {
            for (Hole b : holes) {
                if (bpt > blocksPerTick.get()) continue;

                FindItemResult block = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem && blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));

                    packetPlace(b.blockPos);

                    BlockUtils.place(b.blockPos, block, rotate.get(), 10, true);

                bpt++;
            }
        }
        bpt = 0;
    }

    private boolean blockFilter(Block block) {
        return isCombatBlock(block);
    }

    private boolean validHole(BlockPos pos) {
        if (self.get() && mc.player.getBlockPos().equals(pos))
            pos = pos.up(2);

        if (mc.player.getBlockPos().equals(pos)) return false;
        if (!mc.player.getBlockPos().isWithinDistance(pos, placeRange.get())) return false;
        if (mc.player.getPos().distanceTo(new Box(pos.getX(), pos.getY(),pos.getZ(),pos.getX()+1,pos.getY()+1,pos.getZ()+1).getCenter()) < selfIgnore.get()) return false;
        if (!mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), pos, ShapeContext.absent())) return false;
        if (((AbstractBlockAccessor) mc.world.getBlockState(pos).getBlock()).isCollidable()) return false;
        return !((AbstractBlockAccessor) mc.world.getBlockState(pos.up()).getBlock()).isCollidable();
    }

    private void packetPlace(BlockPos pos) {
        if (pos != null) {
            if (rotate.get()) Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos));
            FindItemResult iresult = InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
            BlockHitResult result = new BlockHitResult(Utils.vec3d(pos), Direction.DOWN, pos, false);
            int prevSlot = mc.player.getInventory().selectedSlot;
            InvUtils.swap(iresult.slot(), false);
            mc.player.getInventory().selectedSlot = prevSlot;
            if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
    }

    private void findTargets() {
        targets.clear();

        // Players
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (isCreative(player) || player == mc.player || isSurrounded(player) && ignoreSurrounded.get()) continue;

            if (!isDead(player) && isAlive(player) && Friends.get().shouldAttack(player) && player.distanceTo(mc.player) <= targetRange.get()) {
                targets.add(player);
            }
        }

        // Fake players
        for (PlayerEntity player : FakePlayerManager.getFakePlayers()) {
            if (isSurrounded(player) && ignoreSurrounded.get()) continue;
            if (!isDead(player) && isAlive(player) && Friends.get().shouldAttack(player) && player.distanceTo(mc.player) <= targetRange.get()) {
                targets.add(player);
            }
        }
    }

    @EventHandler()
    private void onRender(Render3DEvent event) {
        if (!render.get()) return;

        for (Hole hole : holes) {

            Color side = sideColor.get();
            Color line = lineColor.get();

            event.renderer.box(hole.blockPos, side, line, shapeMode.get(), hole.exclude);
        }
    }

    private static class Hole {
        public BlockPos.Mutable blockPos = new BlockPos.Mutable();
        public byte exclude;

        public Hole set(BlockPos blockPos, byte exclude) {
            this.blockPos.set(blockPos);
            this.exclude = exclude;

            return this;
        }
    }
}
