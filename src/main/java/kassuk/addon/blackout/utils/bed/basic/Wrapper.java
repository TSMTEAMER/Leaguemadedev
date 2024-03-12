package kassuk.addon.blackout.utils.bed.basic;

import kassuk.addon.blackout.BlackOut;
import meteordevelopment.meteorclient.systems.config.Config;
import net.minecraft.client.tutorial.TutorialStep;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.util.Objects;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Wrapper {
    public static int isLinux = 92; // 92 = false // 225 = true

    public static int randomNum(int min, int max) {
        return min + (int) (Math.random() * ((max - min) + 1));

    }
}
