package kassuk.addon.blackout.utils.bed.port.advance;

import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PacketUtils {

    //Обычный пакет майн
    public static void start(BlockPos pos) {mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));}
    public static void stop(BlockPos pos) {mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));}
    public static void abort(BlockPos pos) {mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos, Direction.UP));}

    public static void startPacketMine(BlockPos blockpos, boolean clientSwing) {
        start(blockpos);
        if (clientSwing) mc.player.swingHand(Hand.MAIN_HAND);
                else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        stop(blockpos);
    }

    public static void abortPacketMine(BlockPos blockpos) {
        abort(blockpos);
    }

    public static void packetPlace(BlockPos pos, FindItemResult slot, boolean rotate, boolean clientSwing) {
        if (pos != null) {
            if (rotate) Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos));
            BlockHitResult result = new BlockHitResult(Utils.vec3d(pos), Direction.DOWN, pos, true);
            int prevSlot = mc.player.getInventory().selectedSlot;
            InvUtils.swap(slot.slot(), false);
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, 0));
            mc.player.getInventory().selectedSlot = prevSlot;
            if (clientSwing) mc.player.swingHand(Hand.MAIN_HAND);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
    }


    //Пакет Майн с таймером
    private double progress = 0;
    private BlockPos blockPos;
    public PacketUtils(BlockPos pos) {
        blockPos = pos;
        progress = 0;
    }
    public PacketUtils() {
        progress = 0;
    }
    public void reset() {
        progress = 0;
    }
    public double getProgress() {
        return progress;
    }
    public BlockPos getBlockPos() {
        return blockPos;
    }
    public boolean isReady() {
        return progress >= 1;
    }
    public boolean isReadyOn(double var) {
        return progress >= var;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public void setBlockPos(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public void mine(BlockPos blockPos, Task task) {
        task.run(() -> {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
        });

        BlockState blockState = mc.world.getBlockState(blockPos);
        double bestScore = -1;
        int bestSlot = -1;

        for (int i = 0; i < 9; i++) {
            double score = mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(blockState);

            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        progress += getBreakDelta(bestSlot != -1 ? bestSlot : mc.player.getInventory().selectedSlot, blockState);
    }

    public void abortMining(BlockPos blockPos) {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockPos, Direction.UP));
    }

    private double getBreakDelta(int slot, BlockState state) {
        float hardness = state.getHardness(null, null);
        if (hardness == -1) return 0;
        else {
            return getBlockBreakingSpeed(slot, state) / hardness / (!state.isToolRequired() || mc.player.getInventory().main.get(slot).isSuitableFor(state) ? 30 : 100);
        }
    }

    private double getBlockBreakingSpeed(int slot, BlockState block) {
        double speed = mc.player.getInventory().main.get(slot).getMiningSpeedMultiplier(block);

        if (speed > 1) {
            ItemStack tool = mc.player.getInventory().getStack(slot);

            int efficiency = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, tool);

            if (efficiency > 0 && !tool.isEmpty()) speed += efficiency * efficiency + 1;
        }

        if (StatusEffectUtil.hasHaste(mc.player)) {
            speed *= 1 + (StatusEffectUtil.getHasteAmplifier(mc.player) + 1) * 0.2F;
        }

        if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float k = switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };

            speed *= k;
        }

        if (mc.player.isSubmergedIn(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(mc.player)) {
            speed /= 5.0F;
        }

        if (!mc.player.isOnGround()) {
            speed /= 5.0F;
        }

        return speed;
    }
}
