package kassuk.addon.blackout.utils.bed.advance;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

import static kassuk.addon.blackout.utils.bed.basic.EntityInfo.*;
import static kassuk.addon.blackout.utils.bed.basic.BlockInfo.isBlastResist;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PredictionUtils {

    public static ArrayList<BlockPos> getPredictCollisionBlocks(BlockPos blockpos) {
        ArrayList<BlockPos> array = new ArrayList<>();

        if (isBlastResist(blockpos)) array.add(blockpos);
        for (Direction d : Direction.values()) {
            if (d == Direction.UP || d == Direction.DOWN) continue;
            BlockPos pos = blockpos.offset(d);
            if (isBlastResist(pos)) array.add(pos);
        }

        return array;
    }


    public static Box returnPredictBox(PlayerEntity entity, boolean collision, int i) {
        Box eBox = getBoundingBox(entity);
        Vec3d eVec = getVelocity(entity);
        Box pBox = eBox.offset(eVec.getX() * i, 0, eVec.getZ() * i);
        if (getEntitySpeed(entity) < 4){
            return eBox;
        }

        return pBox;
    }
    public static Vec3d returnPredictVec(PlayerEntity entity, boolean collision, int i) {
        Box eBox = getBoundingBox(entity);
        Vec3d eVec = getVelocity(entity);
        Vec3d pVec = new Vec3d(entity.getPos().x, entity.getPos().y, entity.getPos().z);
        Box pBox = eBox.offset(eVec.getX() * i, 0, eVec.getZ() * i);
        if (getEntitySpeed(entity) < 4){
            return pVec;
        }

        Vec3d spVec = new Vec3d(pVec.x + eVec.x * i, pVec.y, pVec.z + eVec.z * i);
        if (mc.player.getPos().distanceTo(spVec) > 7) return pVec;
        return spVec;
    }


    public static double getEntitySpeed(PlayerEntity entity) {
        if (entity == null) return 0;

        double tX = Math.abs(entity.getX() - entity.prevX);
        double tZ = Math.abs(entity.getZ() - entity.prevZ);
        double length = Math.sqrt(tX * tX + tZ * tZ);

        if (entity == mc.player) {
            Timer timer = Modules.get().get(Timer.class);
            if (timer.isActive()) length *= Modules.get().get(Timer.class).getMultiplier();
        }

        return length * 20;
    }
}
