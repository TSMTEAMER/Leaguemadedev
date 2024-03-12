package kassuk.addon.blackout.utils.bed.client;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class APUtils {
    public static List<BlockPos> getSphere(PlayerEntity player, double radius) {
        Vec3d eyePos = new Vec3d(player.getX(), player.getY() + player.getEyeHeight(player.getPose()), player.getZ());
        ArrayList<BlockPos> blocks = new ArrayList<>();

        for (double i = eyePos.getX() - radius; i < eyePos.getX() + radius; i++) {
            for (double j = eyePos.getY() - radius; j < eyePos.getY() + radius; j++) {
                for (double k = eyePos.getZ() - radius; k < eyePos.getZ() + radius; k++) {
                    Vec3d vecPos = new Vec3d(i, j, k);

                    // Closest Vec3d
                    double x = MathHelper.clamp(eyePos.getX() - vecPos.getX(), 0.0, 1.0);
                    double y = MathHelper.clamp(eyePos.getY() - vecPos.getY(), 0.0, 1.0);
                    double z = MathHelper.clamp(eyePos.getZ() - vecPos.getZ(), 0.0, 1.0);
                    Vec3d vec3d = new Vec3d(eyePos.getX() + x, eyePos.getY() + y, eyePos.getZ() + z);

                    // Distance to Vec3d
                    float f = (float) (eyePos.getX() - vec3d.x);
                    float g = (float) (eyePos.getY() - vec3d.y);
                    float h = (float) (eyePos.getZ() - vec3d.z);

                    double distance = MathHelper.sqrt(f * f + g * g + h * h);
                }
            }
        }

        return blocks;
    }
}
