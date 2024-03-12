// Decompiled with: CFR 0.152
// Class Version: 17
package kassuk.addon.blackout.utils.vh;

import java.util.HashSet;
import java.util.Set;

import kassuk.addon.blackout.modules.combat.PacketMine;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.Nullable;


public class BlockUtils2
extends Utils {
    public static boolean outOfMiningRange(BlockPos pos, PacketMine.Origin origin, Integer range) {
        double dz;
        double dy;
        if (origin == PacketMine.Origin.VANILLA) {
            double deltaZ;
            double deltaY;
            double deltaX = MeteorClient.mc.player.getX() - ((double)pos.getX() + 0.5);
            return deltaX * deltaX + (deltaY = MeteorClient.mc.player.getY() - ((double)pos.getY() + 0.5) + 1.5) * deltaY + (deltaZ = MeteorClient.mc.player.getZ() - ((double)pos.getZ() + 0.5)) * deltaZ > range * range;
        }
        Vec3d eyesPos = PlayerUtils2.eyePos(MeteorClient.mc.player);
        double dx = eyesPos.x - (double)pos.getX() - 0.5;
        return dx * dx + (dy = eyesPos.y - (double)pos.getY() - 0.5) * dy + (dz = eyesPos.z - (double)pos.getZ() - 0.5) * dz > range * range;
    }



}
