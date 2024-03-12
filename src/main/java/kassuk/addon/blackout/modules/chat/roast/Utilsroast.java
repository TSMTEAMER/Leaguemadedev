package kassuk.addon.blackout.modules.chat.roast;

import net.minecraft.client.network.PlayerListEntry;

import java.util.*;


import kassuk.addon.blackout.utils.mc;
import static org.lwjgl.glfw.GLFW.*;

public class Utilsroast {

    public static double frameTime;


    /**
     * @return
     */
    public static String getRandomPlayer() {
        Random random = new Random();
        int size = mc.player.networkHandler.getPlayerList().size();
        int r = random.nextInt(size);
        int c = 0;

        for (PlayerListEntry playerListEntry : mc.player.networkHandler.getPlayerList()) {
            if (c == r) return playerListEntry.getProfile().getName();
            c++;
        }

        return "";

    }

}
