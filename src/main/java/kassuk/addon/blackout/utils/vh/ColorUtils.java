// Decompiled with: CFR 0.152
// Class Version: 17
package kassuk.addon.blackout.utils.vh;

import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public class ColorUtils
extends Utils {
    public static SettingColor getColorFromPercent(double percent) {
        int g;
        int r;
        SettingColor distanceColor = new SettingColor(0, 0, 0);
        if (percent < 0.0 || percent > 1.0) {
            distanceColor.set(0, 255, 0, 255);
            return distanceColor;
        }
        if (percent < 0.5) {
            r = 255;
            g = (int)(255.0 * percent / 0.5);
        } else {
            g = 255;
            r = 255 - (int)(255.0 * (percent - 0.5) / 0.5);
        }
        distanceColor.set(r, g, 0, 255);
        return distanceColor;
    }

    public static MutableText coloredText(String text, Color color) {
        return Text.literal(text).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color.getPacked())));
    }
}
