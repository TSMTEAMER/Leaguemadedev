package kassuk.addon.blackout.utils.bed.advance;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.mixininterface.IExplosion;
import meteordevelopment.meteorclient.mixininterface.IRaycastContext;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.explosion.Explosion;

import java.util.ArrayList;
import java.util.List;


import static kassuk.addon.blackout.utils.bed.advance.PacketUtils.startPacketMine;
import static kassuk.addon.blackout.utils.bed.basic.EntityInfo.*;
import static kassuk.addon.blackout.utils.bed.basic.BlockInfo.*;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BedUtils {
    private static Explosion explosion;
    private static final Vec3d vec3d = new Vec3d(0, 0, 0);
    private static RaycastContext raycastContext;

    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(BedUtils.class);
        MeteorClient.EVENT_BUS.subscribe(CrystalUtils.class);
    }

    @EventHandler
    private static void onGameJoined(GameJoinedEvent event) {
        explosion = new Explosion(mc.world, null, 0, 0, 0, 6, false, Explosion.DestructionType.DESTROY);
        raycastContext = new RaycastContext(null, null, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, mc.player);
    }


    public static void packetMine(BlockPos blockpos, boolean autoSwap, Task task) {
        task.run(() -> {
            FindItemResult best = InvUtils.findFastestTool(getState(blockpos));
            if (!best.found()) return;
            if (autoSwap) InvUtils.swap(best.slot(), false);
            startPacketMine(blockpos,true);
        });
    }

    public static void normalMine(BlockPos blockpos, boolean autoSwap) {
        FindItemResult best = InvUtils.findFastestTool(getState(blockpos));
        if (!best.found()) return;
        if (autoSwap) InvUtils.swap(best.slot(), false);
        BlockUtils.breakBlock(blockpos, false);
    }

    public static ArrayList<BlockPos> getTargetSphere(PlayerEntity target, int xRadius, int yRadius) {
        ArrayList<BlockPos> al = new ArrayList<>();
        BlockPos tPos = getBlockPos(target);
        BlockPos.Mutable p = new BlockPos.Mutable();

        for (int x = -xRadius; x <= xRadius; x++) {
            for (int y = -yRadius; y <= yRadius; y++) {
                for (int z = -xRadius; z <= xRadius; z++) {
                    p.set(tPos).move(x, y, z);
                    if (MathHelper.sqrt((float) ((tPos.getX() - p.toImmutable().getX()) * (tPos.getX() - p.toImmutable().getX()) + (tPos.getZ() - p.toImmutable().getZ()) * (tPos.getZ() - p.toImmutable().getZ()))) <= xRadius && MathHelper.sqrt((float) ((tPos.getY() - p.toImmutable().getY()) * (tPos.getY() - p.toImmutable().getY()))) <= yRadius) {
                        if (!al.contains(p.toImmutable())) al.add(p.toImmutable());
                    }
                }
            }
        }
        return al;
            }
        }
