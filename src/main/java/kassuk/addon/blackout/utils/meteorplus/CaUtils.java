package kassuk.addon.blackout.utils.meteorplus;

import meteordevelopment.meteorclient.mixininterface.IExplosion;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.explosion.Explosion;

public class CaUtils {
    private static final Explosion explosion = new Explosion(null, null, 0, 0, 0, 6, false, Explosion.DestructionType.DESTROY);
    private static final Vec3d hitPos = new Vec3d(0, 0, 0);
    public static MinecraftClient mc = MinecraftClient.getInstance();
    public static Vec3i[] city = {new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(0, 0, -1)};

    //Always Calculate damage, then armour, then enchantments, then potion effect
    public static float crystalDamage(LivingEntity player, Vec3d crystal, boolean predict, boolean ignoreTerrain) {
        if (player instanceof PlayerEntity && ((PlayerEntity) player).getAbilities().creativeMode) return 0;
        Vec3d v = predict ? player.getVelocity() : new Vec3d(0, 0, 0);
        Vec3d playerPos = player.getPos().add(v);

        // Calculate crystal damage
        float modDistance = (float) Math.sqrt(playerPos.squaredDistanceTo(crystal));
        if (modDistance > 12) return 0;

        float exposure = getExposure(crystal, player, predict, ignoreTerrain);
        float impact = (float) (1.0 - (modDistance / 12.0)) * exposure;
        float damage = (impact * impact + impact) / 2 * 7 * (6 * 2) + 1;

        // Multiply damage by difficulty
        damage = (float) getDamageForDifficulty(damage);

        // Reduce by armour
        damage = DamageUtil.getDamageLeft(damage, (float) player.getArmor(), (float) player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue());

        // Reduce by resistance
        damage = (float) resistanceReduction(player, damage);

        // Reduce by enchants
        ((IExplosion) explosion).set(crystal, 6, false);

        return Math.max(damage, 0F);
    }

    private static double getDamageForDifficulty(double damage) {
        return switch (mc.world.getDifficulty()) {
            case PEACEFUL -> 0;
            case EASY -> Math.min(damage / 2.0F + 1.0F, damage);
            case HARD -> damage * 3.0F / 2.0F;
            default -> damage;
        };
    }



    private static double resistanceReduction(LivingEntity player, double damage) {
        if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
            int lvl = (player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1);
            damage *= 1 - (lvl * 0.2);
        }

        return damage < 0 ? 0 : damage;
    }

    private static float getExposure(Vec3d source, Entity entity, boolean predict, boolean ignoreTerrain) {
        Vec3d v = predict ? entity.getVelocity() : new Vec3d(0, 0, 0);
        Box box = entity.getBoundingBox().offset(v);
        double d = 1.0D / ((box.maxX - box.minX) * 2.0D + 1.0D);
        double e = 1.0D / ((box.maxY - box.minY) * 2.0D + 1.0D);
        double f = 1.0D / ((box.maxZ - box.minZ) * 2.0D + 1.0D);
        double g = (1.0D - Math.floor(1.0D / d) * d) / 2.0D;
        double h = (1.0D - Math.floor(1.0D / f) * f) / 2.0D;
        if (!(d < 0.0D) && !(e < 0.0D) && !(f < 0.0D)) {
            int i = 0;//nonsolid
            int j = 0;//total

            for (float k = 0.0F; k <= 1.0F; k = (float) ((double) k + d)) {
                for (float l = 0.0F; l <= 1.0F; l = (float) ((double) l + e)) {
                    for (float m = 0.0F; m <= 1.0F; m = (float) ((double) m + f)) {
                        double n = MathHelper.lerp(k, box.minX, box.maxX);
                        double o = MathHelper.lerp(l, box.minY, box.maxY);
                        double p = MathHelper.lerp(m, box.minZ, box.maxZ);
                        Vec3d vec3d = new Vec3d(n + g, o, p + h);
                        ++j;
                    }
                }
            }

            return (float) i / (float) j;
        } else {
            return 0.0F;
        }
    }


    public static Direction rayTraceCheck(BlockPos pos, boolean forceReturn) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        for (Direction direction : Direction.values()) {
            RaycastContext raycastContext = new RaycastContext(eyesPos, new Vec3d(pos.getX() + 0.5 + direction.getVector().getX() * 0.5,
                pos.getY() + 0.5 + direction.getVector().getY() * 0.5,
                pos.getZ() + 0.5 + direction.getVector().getZ() * 0.5), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult result = mc.world.raycast(raycastContext);
            if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(pos)) {
                return direction;
            }
        }
        if (forceReturn) { // When we're placing, we have to return a direction so we have a side to place against
            if ((double) pos.getY() > eyesPos.y) {
                return Direction.DOWN; // The player can never see the top of a block if they are under it
            }
            return Direction.UP;
        }
        return null;
    }

    public static boolean isSurrounded(LivingEntity target) {
        assert mc.world != null;
        return !mc.world.getBlockState(target.getBlockPos().add(1, 0, 0)).isAir()
            && !mc.world.getBlockState(target.getBlockPos().add(-1, 0, 0)).isAir()
            && !mc.world.getBlockState(target.getBlockPos().add(0, 0, 1)).isAir()
            && !mc.world.getBlockState(target.getBlockPos().add(0, 0, -1)).isAir();
    }

    public static boolean isFucked(LivingEntity target) {
        assert mc.world != null;
        assert mc.player != null;
        int count = 0;
        int count2 = 0;
        if (isBurrowed(target)) return false;

        if(isBurrowed(mc.player) && target.getBlockPos().getX() == mc.player.getBlockPos().getX() && target.getBlockPos().getZ() == mc.player.getBlockPos().getZ() && target.getBlockPos().getY() - mc.player.getBlockPos().getY() <= 2) return true;

        if (!mc.world.getBlockState(target.getBlockPos().add(0, 2, 0)).isAir()) return true;

        if (!mc.world.getBlockState(target.getBlockPos().add(1, 0, 0)).isAir()) count++;
        if (!mc.world.getBlockState(target.getBlockPos().add(-1, 0, 0)).isAir()) count++;
        if (!mc.world.getBlockState(target.getBlockPos().add(0, 0, 1)).isAir()) count++;
        if (!mc.world.getBlockState(target.getBlockPos().add(0, 0, -1)).isAir()) count++;

        if (count == 3) return true;

        if (!mc.world.getBlockState(target.getBlockPos().add(1, 1, 0)).isAir()) count2++;
        if (!mc.world.getBlockState(target.getBlockPos().add(-1, 1, 0)).isAir()) count2++;
        if (!mc.world.getBlockState(target.getBlockPos().add(0, 1, 1)).isAir()) count2++;
        if (!mc.world.getBlockState(target.getBlockPos().add(0, 1, -1)).isAir()) count2++;

        return count < 4 && count2 == 4;
    }

    public static boolean isBurrowed(LivingEntity target) {
        return !mc.world.getBlockState(target.getBlockPos()).isAir();
    }

    public static boolean obbySurrounded(LivingEntity entity) {
        BlockPos entityBlockPos = entity.getBlockPos();
        return isBlastRes(mc.world.getBlockState(entity.getBlockPos().add(1, 0, 0)).getBlock())
            && isBlastRes(mc.world.getBlockState(entityBlockPos.add(-1, 0, 0)).getBlock())
            && isBlastRes(mc.world.getBlockState(entityBlockPos.add(0, 0, 1)).getBlock())
            && isBlastRes(mc.world.getBlockState(entityBlockPos.add(0, 0, -1)).getBlock());
    }

    public static boolean isBlastRes(Block block) {
        if (block.getBlastResistance() < 600) return false;
        if (block == Blocks.RESPAWN_ANCHOR) {
            return getDimension() == Dimension.Nether;
        }
        return true;
    }

    public static Dimension getDimension() {
        return switch (MinecraftClient.getInstance().world.getRegistryKey().getValue().getPath()) {
            case "the_nether" -> Dimension.Nether;
            case "the_end" -> Dimension.End;
            default -> Dimension.Overworld;
        };
    }

    public static boolean placeBlock(BlockPos blockPos, int slot, Hand hand, boolean airPlace) {
        if (slot == -1) return false;

        int preSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;

        boolean a = placeBlock(blockPos, hand, true, airPlace);

        mc.player.getInventory().selectedSlot = preSlot;
        return a;
    }

    public static boolean placeBlock(BlockPos blockPos, Hand hand, boolean swing, boolean airPlace) {
        if (!BlockUtils.canPlace(blockPos)) return false;

        // Try to find a neighbour to click on to avoid air place
        for (Direction side : Direction.values()) {

            BlockPos neighbor = blockPos.offset(side);
            Direction side2 = side.getOpposite();

            // Check if neighbour isn't empty
            if (mc.world.getBlockState(neighbor).isAir() || BlockUtils.isClickable(mc.world.getBlockState(neighbor).getBlock())) continue;

            // Calculate hit pos
            ((IVec3d) hitPos).set(neighbor.getX() + 0.5 + side2.getVector().getX() * 0.5, neighbor.getY() + 0.5 + side2.getVector().getY() * 0.5, neighbor.getZ() + 0.5 + side2.getVector().getZ() * 0.5);

            // Place block
            boolean wasSneaking = mc.player.input.sneaking;
            mc.player.input.sneaking = false;

            if (swing) mc.player.swingHand(hand);

            mc.player.input.sneaking = wasSneaking;
            return true;
        }

        if (!airPlace) return false;
        // Air place if no neighbour was found
        ((IVec3d) hitPos).set(blockPos);

        if (swing) mc.player.swingHand(hand);

        return true;
    }

    public static int getSurroundBreak(LivingEntity target, BlockPos pos) {
        BlockPos targetBlockPos = target.getBlockPos();
        if (!mc.world.getBlockState(targetBlockPos.add(1, 0, 0)).isOf(Blocks.BEDROCK) && mc.player.getPos().distanceTo(Vec3d.ofCenter(targetBlockPos.add(1, 0, 0))) <= 6) {
            if (targetBlockPos.add(2, -1, 0).equals(pos)) return 5;
            if (targetBlockPos.add(2, -1, 1).equals(pos)) return 4;
            if (targetBlockPos.add(2, -1, -1).equals(pos)) return 4;
            if (targetBlockPos.add(2, -2, 0).equals(pos)) return 3;
            if (targetBlockPos.add(2, -2, 1).equals(pos)) return 2;
            if (targetBlockPos.add(2, -2, -1).equals(pos)) return 2;
            if (targetBlockPos.add(1, -2, 0).equals(pos)) return 1;
            if (targetBlockPos.add(1, -2, 1).equals(pos)) return 1;
            if (targetBlockPos.add(1, -2, -1).equals(pos)) return 1;
            if (targetBlockPos.add(1, -1, 1).equals(pos)) return 1;
            if (targetBlockPos.add(1, -1, -1).equals(pos)) return 1;
        }
        if (!mc.world.getBlockState(targetBlockPos.add(-1, 0, 0)).isOf(Blocks.BEDROCK) && mc.player.getPos().distanceTo(Vec3d.ofCenter(targetBlockPos.add(-1, 0, 0))) <= 6) {
            if (targetBlockPos.add(-2, -1, 0).equals(pos)) return 5;
            if (targetBlockPos.add(-2, -1, 1).equals(pos)) return 4;
            if (targetBlockPos.add(-2, -1, -1).equals(pos)) return 4;
            if (targetBlockPos.add(-2, -2, 0).equals(pos)) return 3;
            if (targetBlockPos.add(-2, -2, 1).equals(pos)) return 2;
            if (targetBlockPos.add(-2, -2, -1).equals(pos)) return 2;
            if (targetBlockPos.add(-1, -2, 0).equals(pos)) return 1;
            if (targetBlockPos.add(-1, -2, 1).equals(pos)) return 1;
            if (targetBlockPos.add(-1, -2, -1).equals(pos)) return 1;
            if (targetBlockPos.add(-1, -1, 1).equals(pos)) return 1;
            if (targetBlockPos.add(-1, -1, -1).equals(pos)) return 1;
        }
        if (!mc.world.getBlockState(targetBlockPos.add(0, 0, 1)).isOf(Blocks.BEDROCK) && mc.player.getPos().distanceTo(Vec3d.ofCenter(targetBlockPos.add(0, 0, 1))) <= 6) {
            if (targetBlockPos.add(0, -1, 2).equals(pos)) return 5;
            if (targetBlockPos.add(1, -1, 2).equals(pos)) return 4;
            if (targetBlockPos.add(-1, -1, 2).equals(pos)) return 4;
            if (targetBlockPos.add(0, -2, 2).equals(pos)) return 3;
            if (targetBlockPos.add(1, -2, 2).equals(pos)) return 2;
            if (targetBlockPos.add(-1, -2, 2).equals(pos)) return 2;
            if (targetBlockPos.add(0, -2, 1).equals(pos)) return 1;
            if (targetBlockPos.add(1, -2, 1).equals(pos)) return 1;
            if (targetBlockPos.add(-1, -2, 1).equals(pos)) return 1;
            if (targetBlockPos.add(1, -1, 1).equals(pos)) return 1;
            if (targetBlockPos.add(-1, -1, 1).equals(pos)) return 1;
        }
        if (!mc.world.getBlockState(targetBlockPos.add(0, 0, -1)).isOf(Blocks.BEDROCK) && mc.player.getPos().distanceTo(Vec3d.ofCenter(targetBlockPos.add(0, 0, -1))) <= 6) {
            if (targetBlockPos.add(0, -1, -2).equals(pos)) return 5;
            if (targetBlockPos.add(1, -1, -2).equals(pos)) return 4;
            if (targetBlockPos.add(-1, -1, -2).equals(pos)) return 4;
            if (targetBlockPos.add(0, -2, -2).equals(pos)) return 3;
            if (targetBlockPos.add(1, -2, -2).equals(pos)) return 2;
            if (targetBlockPos.add(-1, -2, -2).equals(pos)) return 2;
            if (targetBlockPos.add(0, -2, -1).equals(pos)) return 1;
            if (targetBlockPos.add(1, -2, -1).equals(pos)) return 1;
            if (targetBlockPos.add(-1, -2, -1).equals(pos)) return 1;
            if (targetBlockPos.add(1, -1, -1).equals(pos)) return 1;
            if (targetBlockPos.add(-1, -1, -1).equals(pos)) return 1;
        }
        return 0;
    }

    public static boolean isSurroundBroken(LivingEntity target) {
        BlockPos targetBlockPos = target.getBlockPos();
        for (Vec3i block : city) {
            double x = targetBlockPos.add(block).getX();
            double y = targetBlockPos.add(block).getY();
            double z = targetBlockPos.add(block).getZ();
            if (mc.world.getBlockState(targetBlockPos.add(block)).isOf(Blocks.BEDROCK)) continue;
            if (mc.player.getPos().distanceTo(new Vec3d(x, y, z)) > 6) continue;
            if (!mc.world.getOtherEntities(null, new Box(x, y, z, x + 1, y + 1, z + 1)).isEmpty()) return true;
        }
        return false;
    }

    public static Vec3d crystalEdgePos(EndCrystalEntity crystal) {
        Vec3d crystalPos = crystal.getPos();
        //X
        if (crystalPos.x < mc.player.getX()) crystalPos.add(Math.min(1, mc.player.getX() - crystalPos.x), 0, 0);
        else if (crystalPos.x > mc.player.getX()) crystalPos.add(Math.max(-1, mc.player.getX() - crystalPos.x), 0, 0);
        //Y
        if (crystalPos.y < mc.player.getY()) crystalPos.add(0, Math.min(2, mc.player.getY() - crystalPos.y), 0);
        //Z
        if (crystalPos.z < mc.player.getZ()) crystalPos.add(0, 0, Math.min(1, mc.player.getZ() - crystalPos.z));
        else if (crystalPos.z > mc.player.getZ()) crystalPos.add(0, 0, Math.max(-1, mc.player.getZ() - crystalPos.z));

        return crystalPos;
    }

    public static BlockHitResult getPlaceResult(BlockPos pos) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        for (Direction direction : Direction.values()) {
            RaycastContext raycastContext = new RaycastContext(eyesPos, new Vec3d(
                pos.getX() + 0.5 + direction.getVector().getX() * 0.5,
                pos.getY() + 0.5 + direction.getVector().getY() * 0.5,
                pos.getZ() + 0.5 + direction.getVector().getZ() * 0.5),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult result = mc.world.raycast(raycastContext);
            if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(pos)) {
                return result;
            }
        }
        return new BlockHitResult(eyesPos, pos.getY() < mc.player.getY() ? Direction.UP : Direction.DOWN, new BlockPos(pos), false);
    }

}
