// Decompiled with: CFR 0.152
// Class Version: 17
package kassuk.addon.blackout.utils.vh;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.util.math.Vec3d;

public enum Origin {
    VANILLA("Vanilla"),
    NCP("No Cheat Plus");

    private final String title;

    private Origin(String title) {
        this.title = title;
    }

    public String toString() {
        return this.title;
    }

    public Vec3d getOrigin(Vec3d pos) {
        if (this == VANILLA) {
            return pos;
        }
        return pos.add(0.0, MeteorClient.mc.player.getEyeHeight(MeteorClient.mc.player.getPose()), 0.0);
    }


    }
