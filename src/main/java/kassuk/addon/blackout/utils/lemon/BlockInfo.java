package kassuk.addon.blackout.utils.lemon;


import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BlockInfo {
    public static double progress = 0;

    private static final ArrayList<BlockPos> blocks = new ArrayList<>();

    public static VoxelShape getShape(BlockPos block) {return mc.world.getBlockState(block).getOutlineShape(mc.world, block);}
    public static Box getBox(BlockPos block) {return getShape(block).getBoundingBox();}
    public static Block getBlock(BlockPos block) {
        return mc.world.getBlockState(block).getBlock();
    }
    public static boolean isAir(BlockPos block) {
        return mc.world.getBlockState(block).isAir();
    }
    public static float getBlastResistance(BlockPos block) {
        return mc.world.getBlockState(block).getBlock().getBlastResistance();
    }
    public static float getBlastResistance(Block block) {
        return block.getBlastResistance();
    }
    public static boolean canBreak(int slot, BlockPos blockPos) {
        if (progress >= 1) return true;
        BlockState blockState = mc.world.getBlockState(blockPos);

        if (progress < 1) progress += getBreakDelta(slot != 420 ? slot : mc.player.getInventory().selectedSlot, blockState);
        return false;
    }
    public static boolean isReplaceable(BlockPos block) {
        return mc.world.getBlockState(block).isReplaceable();
    }
    public static boolean isSolid(BlockPos block) {return mc.world.getBlockState(block).isSolid();}
    public static boolean isBurnable(BlockPos block) {return mc.world.getBlockState(block).isBurnable();}
    public static boolean isLiquid(BlockPos block) {return mc.world.getBlockState(block).isLiquid();}
    public static float getHardness(BlockPos block) {
        return mc.world.getBlockState(block).getHardness(mc.world, block);
    }
    public static float getHardness(Block block) {return block.getHardness();}
    public static boolean isBlastResist(BlockPos block) {return getBlastResistance(block) >= 600;}
    public static boolean isBlastResist(Block block) {return getBlastResistance(block) >= 600;}
    public static boolean isBreakable(BlockPos pos) {
        return getHardness(pos) > 0;
    }
    public static boolean isBreakable(Block block) {return getHardness(block) > 0;}
    public static boolean isCombatBlock(BlockPos block) {return isBlastResist(block) && isBreakable(block);}
    public static boolean isCombatBlock(Block block) {return isBlastResist(block) && isBreakable(block);}
    public static Vec3d getCenterVec3d(BlockPos block) {return new Vec3d(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5);}
    public static boolean notNull(BlockPos block) {return block != null;}
    public static boolean isNull(BlockPos block) {
        return block == null;
    }
    public static boolean isWithinRange(BlockPos block, double range) {
        return mc.player.getBlockPos().isWithinDistance(block, range);
    }
    public static boolean isFullCube(BlockPos block) {
        return mc.world.getBlockState(block).isFullCube(mc.world, block);
    }

    public static double getBreakDelta(int slot, BlockState state) {
        float hardness = state.getHardness(null, null);

        if (hardness == -1) return 0;
        else {
            return getBlockBreakingSpeed(slot, state) / hardness / (!state.isToolRequired() || mc.player.getInventory().main.get(slot).isSuitableFor(state) ? 30 : 100);
        }
    }

    public static boolean canPlace(BlockPos pos, boolean breakCrystal, double safeHealth) {
        if (pos == null) return false;

        if (!World.isValid(pos)) return false;

        if (!mc.world.getBlockState(pos).isReplaceable()) return false;

        return !EntityInfo.checkEntity(pos, breakCrystal, safeHealth);
    }

    private static double getBlockBreakingSpeed(int slot, BlockState block) {
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

    public static int getBlockBreakingSpeed(BlockState block, BlockPos pos, int slot) {
        PlayerEntity player = mc.player;

        float f = (player.getInventory().getStack(slot)).getMiningSpeedMultiplier(block);
        if (f > 1.0F) {
            int i = EnchantmentHelper.get(player.getInventory().getStack(slot)).getOrDefault(Enchantments.EFFICIENCY, 0);
            if (i > 0) {
                f += (float)(i * i + 1);
            }
        }

        if (StatusEffectUtil.hasHaste(player)) {
            f *= 1.0F + (float)(StatusEffectUtil.getHasteAmplifier(player) + 1) * 0.2F;
        }

        if (player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float k = switch (player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };

            f *= k;
        }

        if (player.isSubmergedIn(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(player)) {
            f /= 5.0F;
        }

        if (!player.isOnGround()) {
            f /= 5.0F;
        }

        float t = block.getHardness(mc.world, pos);
        if (t == -1.0F) {
            return 0;
        } else {
            return (int) Math.ceil(1 / (f / t / 30));
        }
    }

    public static boolean doesBoxTouchBlock(Box box, Block block) {
        for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                    if (mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() == block) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static Vec3d closestVec3d(BlockPos blockPos) {
        if (blockPos == null) return new Vec3d(0.0, 0.0, 0.0);
        double x = MathHelper.clamp((mc.player.getX() - blockPos.getX()), 0.0, 1.0);
        double y = MathHelper.clamp((mc.player.getY() - blockPos.getY()), 0.0, 0.6);
        double z = MathHelper.clamp((mc.player.getZ() - blockPos.getZ()), 0.0, 1.0);
        return new Vec3d(blockPos.getX() + x, blockPos.getY() + y, blockPos.getZ() + z);
    }

    private static Vec3d closestVec3d(Box box) {
        if (box == null) return new Vec3d(0.0, 0.0, 0.0);
        Vec3d eyePos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());

        double x = MathHelper.clamp(eyePos.getX(), box.minX, box.maxX);
        double y = MathHelper.clamp(eyePos.getY(), box.minY, box.maxY);
        double z = MathHelper.clamp(eyePos.getZ(), box.minZ, box.maxZ);

        return new Vec3d(x, y, z);
    }

    public static Vec3d closestVec3d2(BlockPos pos) {
        return closestVec3d(box(pos));
    }

    public static List<BlockPos> getSphere(BlockPos centerPos, int radius, int height) {
        blocks.clear();

        for (int i = centerPos.getX() - radius; i < centerPos.getX() + radius; i++) {
            for (int j = centerPos.getY() - height; j < centerPos.getY() + height; j++) {
                for (int k = centerPos.getZ() - radius; k < centerPos.getZ() + radius; k++) {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (distanceBetween(centerPos, pos) <= radius && !blocks.contains(pos)) blocks.add(pos);
                }
            }
        }

        return blocks;
    }

    public static List<BlockPos> getSphere(BlockPos centerPos, double radius, double height) {
        ArrayList<BlockPos> blocks = new ArrayList<>();

        for (int i = centerPos.getX() - (int) radius; i < centerPos.getX() + radius; i++) {
            for (int j = centerPos.getY() - (int) height; j < centerPos.getY() + height; j++) {
                for (int k = centerPos.getZ() - (int) radius; k < centerPos.getZ() + radius; k++) {
                    BlockPos pos = new BlockPos(i, j, k);

                    if (distanceTo(centerPos, pos) <= radius && !blocks.contains(pos)) blocks.add(pos);
                }
            }
        }

        return blocks;
    }

    public static double distanceBetween(BlockPos blockPos1, BlockPos blockPos2) {
        double d = blockPos1.getX() - blockPos2.getX();
        double e = blockPos1.getY() - blockPos2.getY();
        double f = blockPos1.getZ() - blockPos2.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }

    public static Box box(BlockPos blockPos) {
        return new Box(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockPos.getX() + 1, blockPos.getY() + 1, blockPos.getZ() + 1);
    }

    public static double distanceTo(BlockPos pos) {
        Vec3d eyePos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());

        float f = (float) (eyePos.getX() - closestVec3d2(pos).x);
        float g = (float) (eyePos.getY() - closestVec3d2(pos).y);
        float h = (float) (eyePos.getZ() - closestVec3d2(pos).z);
        return MathHelper.sqrt(f * f + g * g + h * h);
    }

    public static double distanceTo(BlockPos blockPos1, BlockPos blockPos2) {
        double d = blockPos1.getX() - blockPos2.getX();
        double e = blockPos1.getY() - blockPos2.getY();
        double f = blockPos1.getZ() - blockPos2.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }

    public static double distanceTo(double x, double y, double z) {
        Vec3d eyePos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        Vec3d vec3d = closestVec3d(Box.from(new Vec3d(x, y, z)));

        float f = (float) (eyePos.getX() - vec3d.x);
        float g = (float) (eyePos.getY() - vec3d.y);
        float h = (float) (eyePos.getZ() - vec3d.z);
        return MathHelper.sqrt(f * f + g * g + h * h);
    }

    public static BlockPos roundBlockPos(Vec3d vec) {
        return new BlockPos((int) vec.x, (int) Math.round(vec.y), (int) vec.z);
    }

    public static void state(Block block, BlockPos pos) {
        mc.world.setBlockState(pos, block.getDefaultState());
    }

    public static boolean of(Block block, BlockPos pos) {
        return mc.world.getBlockState(pos).isOf(block);
    }

    public boolean of(Class<?> klass, BlockPos pos) {
        return klass.isInstance(mc.world.getBlockState(pos).getBlock());
    }

    public boolean of(Fluid fluid, BlockPos pos) {
        return mc.world.getBlockState(pos).getFluidState().isOf(fluid);
    }

    public static BlockPos getBlockPos(BlockPos blockPos) {
        return new BlockPos(blockPos);
    }
}
