package kassuk.addon.blackout.utils.lemon;

import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.block.AirBlock;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

import static kassuk.addon.blackout.utils.bed.port.basic.BlockInfo.isBlastResist;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EntityInfo {
    public static boolean checkEntity(BlockPos pos, boolean breakCrystal, double safeHealth) {
        for (Entity entity : mc.world.getOtherEntities(null, new Box(pos), entity -> entity == mc.player)) {
            if (EntityInfo.isDead(mc.player) || entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity || entity instanceof ExperienceBottleEntity || entity instanceof ArrowEntity || !(entity instanceof EndCrystalEntity ? !breakCrystal || mc.player.getHealth() < safeHealth : entity != mc.player)) continue;
            return true;
        }
        return false;
    }

    public static boolean checkEntity(BlockPos pos) {
        for (Entity entity : mc.world.getOtherEntities(null, new Box(pos), entity -> entity == mc.player)) {
            if (isDead(mc.player) || entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity || entity instanceof ExperienceBottleEntity || entity instanceof ArrowEntity) continue;
            return true;
        }
        return false;
    }

    public static double distanceTo(BlockPos blockPos1, PlayerEntity player) {
        if (blockPos1 == null || player == null) return 99;

        double d = blockPos1.getX() - player.getX();
        double e = blockPos1.getY() - player.getY();
        double f = blockPos1.getZ() - player.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }

    public static boolean isPlayerNear(BlockPos blockPos) {
        if (blockPos == null) return false;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null) continue;
            if (distanceTo(blockPos, player) > 5) continue;

            if (player.isDead()) continue;
            if (player == mc.player) continue;
            if (Friends.get().isFriend(player)) continue;

            if (getBlocksAround(player).contains(blockPos)) return true;
        }

        return false;
    }
    // Самые полезные переменные для Ентити
    public static boolean isAlive(PlayerEntity entity) {
        return entity.isAlive();
    }
    public static boolean isDead(PlayerEntity entity) {
        return entity.isDead();
    }
    public static boolean isWebbed(PlayerEntity entity) {
        return BlockInfo.doesBoxTouchBlock(entity.getBoundingBox(), Blocks.COBWEB);
    }
    public static boolean isOnGround(PlayerEntity entity) {
        return entity.isOnGround();
    }
    public static float getMovementSpeed(PlayerEntity entity) {
        return entity.getMovementSpeed();
    }
    public static boolean isMoving(PlayerEntity entity) {return entity.forwardSpeed != 0 || entity.sidewaysSpeed != 0;}
    public static boolean canRecieveDmg(PlayerEntity entity) {
        return entity.hurtTime == 0;
    }
    public static BlockPos getBlockPos(PlayerEntity entity) {
        return entity.getBlockPos();
    }
    public static BlockPos getBlockPos(Entity entity) {
        return entity.getBlockPos();
    }
    public static Position getPos(PlayerEntity entity) {
        return entity.getPos();
    }
    public static Vec3d getVelocity(PlayerEntity entity) {
        return entity.getVelocity();
    }
    public static Box getBoundingBox(PlayerEntity entity) {
        return entity.getBoundingBox();
    }
    public static Box getBoundingBox(Entity entity) {
        return entity.getBoundingBox();
    }
    public static PlayerAbilities getAbilities(PlayerEntity entity) {
        return entity.getAbilities();
    }
    public static boolean isCreative(PlayerEntity entity) {
        return entity.getAbilities().creativeMode;
    }
    public static float getFlySpeed(PlayerEntity entity) {
        return entity.getAbilities().getFlySpeed();
    }
    public static float getWalkSpeed(PlayerEntity entity) {
        return entity.getAbilities().getWalkSpeed();
    }
    public static void setWalkSpeed(PlayerEntity entity, float speed) {
        entity.getAbilities().setWalkSpeed(speed);
    }
    public static void setFlySpeed(PlayerEntity entity, float speed) {
        entity.getAbilities().setFlySpeed(speed);
    }
    public static World getWorld(PlayerEntity entity) {
        return entity.getWorld();
    }
    public static int deathTime(PlayerEntity entity) {
        return entity.deathTime;
    }
    public static int getFoodLevel(PlayerEntity entity) {
        return entity.getHungerManager().getFoodLevel();
    }
    public static float getSaturationLevel(PlayerEntity entity) {return entity.getHungerManager().getSaturationLevel();}
    public static float getExhaustionLevel(PlayerEntity entity) {
        return entity.getHungerManager().getExhaustion();
    }
    public static Inventory getInventory(PlayerEntity entity) {
        return entity.getInventory();
    }
    public static ItemStack getStack(PlayerEntity entity, int slot) {
        return entity.getInventory().getStack(slot);
    }
    public static int getMainSlot(PlayerEntity entity) {
        return entity.getInventory().selectedSlot;
    }
    public static int getOffhandSlot(PlayerEntity entity) {
        return 45;
    }
    public static int getEmptySlot(PlayerEntity entity) {
        return entity.getInventory().getEmptySlot();
    }
    public static boolean isEmptyInventory(PlayerEntity entity) {
        return entity.getInventory().isEmpty();
    }
    public static boolean isBlastResistant(BlockPos pos, BlastResistantType type) {
        Block block = mc.world.getBlockState(pos).getBlock();
        switch (type) {
            case Any, Mineable -> {
                return block == Blocks.OBSIDIAN
                    || block == Blocks.CRYING_OBSIDIAN
                    || block instanceof AnvilBlock
                    || block == Blocks.NETHERITE_BLOCK
                    || block == Blocks.ENDER_CHEST
                    || block == Blocks.RESPAWN_ANCHOR
                    || block == Blocks.ANCIENT_DEBRIS
                    || block == Blocks.ENCHANTING_TABLE
                    || (block == Blocks.BEDROCK && type == BlastResistantType.Any)
                    || (block == Blocks.END_PORTAL_FRAME && type == BlastResistantType.Any);
            }
            case Unbreakable -> {
                return block == Blocks.BEDROCK
                    || block == Blocks.END_PORTAL_FRAME;
            }
            case NotAir -> {
                return block != Blocks.AIR;
            }
        }
        return false;
    }
    public static boolean notNull(PlayerEntity entity) {
        return entity != null;
    }
    public static String getName(PlayerEntity entity) {
        return entity.getGameProfile().getName();
    }
    public static double X(PlayerEntity entity) {
        return entity.getX();
    }
    public static double Y(PlayerEntity entity) {
        return entity.getY();
    }
    public static double Z(PlayerEntity entity) {
        return entity.getZ();
    }
    public static Iterable<Entity> getEntities() {
        return mc.world.getEntities();
    }
    public static List<AbstractClientPlayerEntity> getPlayers() {
        return mc.world.getPlayers();
    }

    public static boolean isSurrounded(LivingEntity entity) {
        return isBlastResist(entity.getBlockPos().south())
            && isBlastResist(entity.getBlockPos().west())
            && isBlastResist(entity.getBlockPos().east())
            && isBlastResist(entity.getBlockPos().north())
            && isBlastResist(entity.getBlockPos().down());
    }

    public static boolean isTrapped(LivingEntity entity) {
        return isBlastResist(entity.getBlockPos().south().up())
            && isBlastResist(entity.getBlockPos().west().up())
            && isBlastResist(entity.getBlockPos().east().up())
            && isBlastResist(entity.getBlockPos().north().up())
            && isBlastResist(entity.getBlockPos().up(2));
    }

    public static boolean isFaceTrapped(LivingEntity entity) {
        return isBlastResist(entity.getBlockPos().south().up())
            && isBlastResist(entity.getBlockPos().west().up())
            && isBlastResist(entity.getBlockPos().east().up())
            && isBlastResist(entity.getBlockPos().north().up());
    }

    public static boolean isInHole(PlayerEntity p) {
        BlockPos pos = p.getBlockPos();
        return !mc.world.getBlockState(pos.add(1, 0, 0)).isAir()
            && !mc.world.getBlockState(pos.add(-1, 0, 0)).isAir()
            && !mc.world.getBlockState(pos.add(0, 0, 1)).isAir()
            && !mc.world.getBlockState(pos.add(0, 0, -1)).isAir()
            && !mc.world.getBlockState(pos.add(0, -1, 0)).isAir();
    }

    public static boolean isInLiquid() {
        if (mc.player.fallDistance >= 3.0f) {
            return false;
        }
        boolean inLiquid = false;
        Box bb = /*mc.player.getRidingEntity() != null ? mc.player.getRidingEntity().getEntityBoundingBox() : */mc.player.getBoundingBox();
        int y = (int) bb.minY;
        for (int x = MathHelper.floor(bb.minX); x < MathHelper.floor(bb.maxX) + 1; ++x) {
            for (int z = MathHelper.floor(bb.minZ); z < MathHelper.floor(bb.maxZ) + 1; ++z) {
                Block block = mc.world.getBlockState(new BlockPos(x, y, z)).getBlock();
                if (block instanceof AirBlock) continue;
                if (!(block == Blocks.WATER)) {
                    return false;
                }
                inLiquid = true;
            }
        }
        return inLiquid;
    }

    public static BlockPos playerPos(PlayerEntity targetEntity) {
        return BlockInfo.roundBlockPos(targetEntity.getPos());
    }

    public static boolean isInHole(PlayerEntity targetEntity, boolean doubles, BlastResistantType type) {
        if (!Utils.canUpdate()) return false;

        BlockPos blockPos = playerPos(targetEntity);
        int air = 0;

        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) continue;

            if (!isBlastResistant(blockPos.offset(direction), type)) {
                if (!doubles || direction == Direction.DOWN) return false;

                air++;

                for (Direction dir : Direction.values()) {
                    if (dir == direction.getOpposite() || dir == Direction.UP) continue;

                    if (!isBlastResistant(blockPos.offset(direction).offset(dir), type)) {
                        return false;
                    }
                }
            }
        }

        return air < 2;
    }

    public static List<BlockPos> getBlocksAround(PlayerEntity player) {
        List<BlockPos> positions = new ArrayList<>();
        List<Entity> getEntityBoxes;

        for (BlockPos blockPos : BlockInfo.getSphere(player.getBlockPos(), 3, 1)) {
            getEntityBoxes = mc.world.getOtherEntities(null, new Box(blockPos), entity -> entity == player);
            if (!getEntityBoxes.isEmpty()) continue;

            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP || direction == Direction.DOWN) continue;

                getEntityBoxes = mc.world.getOtherEntities(null, new Box(blockPos.offset(direction)), entity -> entity == player);
                if (!getEntityBoxes.isEmpty()) positions.add(new BlockPos(blockPos));
            }
        }

        return positions;
    }

    public static Direction rayTraceCheck(BlockPos pos, boolean forceReturn) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + (double)mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        Direction[] var3 = Direction.values();

        for (Direction direction : var3) {
            RaycastContext raycastContext = new RaycastContext(eyesPos, new Vec3d((double) pos.getX() + 0.5D + (double) direction.getVector().getX() * 0.5D, (double) pos.getY() + 0.5D + (double) direction.getVector().getY() * 0.5D, (double) pos.getZ() + 0.5D + (double) direction.getVector().getZ() * 0.5D), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult result = mc.world.raycast(raycastContext);
            if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(pos)) {
                return direction;
            }
        }

        if (forceReturn) {
            if ((double)pos.getY() > eyesPos.y) {
                return Direction.DOWN;
            } else {
                return Direction.UP;
            }
        } else {
            return null;
        }
    }

    public static boolean isDoubleSurrounded(LivingEntity entity) {
        BlockPos blockPos = entity.getBlockPos();
        return isBlastResist(blockPos.add(1, 0, 0)) &&
            isBlastResist(blockPos.add(-1, 0, 0)) &&
            isBlastResist(blockPos.add(0, 0, 1)) &&
            isBlastResist(blockPos.add(0, 0, -1)) &&
            isBlastResist(blockPos.add(1, 1, 0)) &&
            isBlastResist(blockPos.add(-1, 1, 0)) &&
            isBlastResist(blockPos.add(0, 1, 1)) &&
            isBlastResist(blockPos.add(0, 1, -1));
    }

    public enum BlastResistantType {
        Any, // Any blast resistant block
        Unbreakable, // Can't be mined
        Mineable, // You can mine the block
        NotAir // Doesn't matter as long it's not air
    }
}
