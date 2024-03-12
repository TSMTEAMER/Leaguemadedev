package kassuk.addon.blackout.modules.combat;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.utils.OLEPOSSUtils;
import kassuk.addon.blackout.utils.bed.basic.RenderInfo;
import kassuk.addon.blackout.utils.bed.port.advance.Interaction;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static kassuk.addon.blackout.utils.bed.basic.BlockInfo.*;
import static kassuk.addon.blackout.utils.bed.advance.RenderUtils.*;
import static kassuk.addon.blackout.utils.bed.basic.EntityInfo.*;


public class AnchorBomb extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> targetRange = sgGeneral.add(new IntSetting.Builder().name("target-range").defaultValue(15).min(0).sliderMax(20).build());
    private final Setting<Boolean> doPlace = sgGeneral.add(new BoolSetting.Builder().name("place").defaultValue(true).build());
    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder().name("place-delay").description("The delay in ticks to wait to place a anchor after it's exploded.").defaultValue(3).min(0).sliderMax(20).build());
    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder().name("place-range").defaultValue(5.5).sliderRange(0, 8).build());
    private final Setting<Double> minDamage = sgGeneral.add(new DoubleSetting.Builder().name("min-damage").description("Minimum damage the anchor needs to deal to your target.").defaultValue(6).min(0).build());
    private final Setting<Double> maxDamage = sgGeneral.add(new DoubleSetting.Builder().name("max-damage").description("Maximum damage anchors can deal to yourself.").defaultValue(6).range(0, 36).sliderMax(36).build());
    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder().name("radius").defaultValue(5).min(1).sliderMax(15).build());

    private final Setting<RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderMode>().name("render").description("How the render will work.").defaultValue(RenderMode.Box).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color for positions to be placed.").defaultValue(new SettingColor(255, 0, 170, 10)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color for positions to be placed.").defaultValue(new SettingColor(255, 0, 170, 90)).build());
    private final Setting<Double> height = sgRender.add(new DoubleSetting.Builder().name("height").description("Maximum damage anchors can deal to yourself.").defaultValue(0.99).sliderRange(0,1).visible(() -> renderMode.get() == RenderMode.UpperSide || renderMode.get() == RenderMode.LowerSide).build());

    public AnchorBomb() {
        super(BlackOut.COMBATPLUS, "anchor-bomb", "Automatically blows up target with anchors.");
    }

    private FindItemResult glowstone, anchor;
    private PlayerEntity target;
    private BlockPos targetPos;

    private int placeTimer;

    private BlockPos bestPos;
    private double bestDamage, tempDamage;
    private ArrayList<BlockPos> positions = new ArrayList<>();

    private final ExecutorService cached = Executors.newCachedThreadPool();

    @Override
    public void onActivate() {
        placeTimer = 0;

        bestPos = null;
        bestDamage = 0;
        tempDamage = 0;
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (placeTimer > 0) placeTimer--;

        glowstone = InvUtils.findInHotbar(Items.GLOWSTONE);
        anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
        if (!glowstone.found() || !anchor.found()) {
            info("Glowstone or Anchor not found, disabling...");
            toggle();
            return;
        }

        target = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestDistance);
        if (TargetUtils.isBadTarget(target, targetRange.get())) return;

        targetPos = getBlockPos(target);

        positions = new ArrayList<>(getSphere(targetPos, radius.get()));
        if (bestPos == null) cached.execute(this::calculatePos);

        doPlace();
    }

    private void doPlace() {
        if (placeTimer > 2) return;

        doPlace(bestPos, doPlace.get());
    }

    private void doPlace(BlockPos blockPos, boolean place) {
        if (blockPos == null || !place) return;
        BlockHitResult result = new BlockHitResult(Utils.vec3d(blockPos), Direction.UP, blockPos, false);

        if (placeTimer == 2) {
            info("placing");
            Interaction.updateSlot(anchor, true);
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(anchor.getHand(), result, 0));
            Interaction.doSwing(anchor.getHand(), false);
        }
        if (placeTimer == 1) {
            Interaction.updateSlot(glowstone, true);
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(glowstone.getHand(), result, 0));
            Interaction.doSwing(glowstone.getHand(), false);
        }
        if (placeTimer <= 0) {
            if (!mc.world.getBlockState(blockPos).isAir()) {
                Interaction.updateSlot(anchor, true);
                mc.interactionManager.interactBlock(mc.player, anchor.getHand(), result);
                Interaction.doSwing(anchor.getHand(), false);
            }

            bestPos = null;
            bestDamage = 0;
            placeTimer = placeDelay.get();
        }
    }

    private void calculatePos() {
        BlockPos finalPos = null;
        long startTime = System.currentTimeMillis();

        try {
            for (BlockPos blockPos : positions) {
                double selfDamage = DamageUtils.bedDamage(mc.player, getCenterVec3d(blockPos));
                double targetDamage = DamageUtils.bedDamage(target, getCenterVec3d(blockPos));

                if (blockPos.equals(targetPos) || blockPos.equals(targetPos.up())) continue;
                if (selfDamage > maxDamage.get() || targetDamage < minDamage.get()) continue;

                tempDamage = targetDamage;
                if (tempDamage > bestDamage) {
                    bestDamage = tempDamage;
                    finalPos = blockPos;
                }
            }
        } catch (Exception e) {
            e.fillInStackTrace();
        }

        if (finalPos == null) return;
        info("pos calculated in " + (System.currentTimeMillis() - startTime) + "ms");
        bestPos = finalPos;
    }

    private List<BlockPos> getSphere(BlockPos centerPos, int radius) {
        ArrayList<BlockPos> blocks = new ArrayList<>();

        for (int i = centerPos.getX() - radius; i < centerPos.getX() + radius; i++) {
            for (int j = centerPos.getY() - radius; j < centerPos.getY() + radius; j++) {
                for (int k = centerPos.getZ() - radius; k < centerPos.getZ() + radius; k++) {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (distanceBetween(centerPos, pos) <= radius &&
                            mc.player.getBlockPos().isWithinDistance(pos, placeRange.get()) &&
                            !blocks.contains(pos) &&
                            (isAir(pos) || getBlock(pos) == Blocks.RESPAWN_ANCHOR || OLEPOSSUtils.replaceable(pos)))
                        blocks.add(pos);
                }
            }
        }

        return blocks;
    }

    private double distanceBetween(BlockPos pos1, BlockPos pos2) {
        double d = pos1.getX() - pos2.getX();
        double e = pos1.getY() - pos2.getY();
        double f = pos1.getZ() - pos2.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        RenderInfo ri = new RenderInfo(event, renderMode.get(), shapeMode.get());
        if (bestPos == null) return;

        render(ri, bestPos, sideColor.get(), lineColor.get(), height.get());
    }

    @Override
    public String getInfoString() {
        return target != null ? getName(target) : null; // adds target name to the module array list
    }
}
