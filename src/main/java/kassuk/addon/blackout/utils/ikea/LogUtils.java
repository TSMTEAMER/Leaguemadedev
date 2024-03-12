package kassuk.addon.blackout.utils.ikea;

import meteordevelopment.meteorclient.mixininterface.IChatHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class LogUtils {
    protected static MinecraftClient mc = MinecraftClient.getInstance();

    public static void info(String txt) {
        assert mc.world != null;

        MutableText message = Text.literal("");
        message.append(Formatting.GRAY + "[" + Formatting.DARK_RED + "Ricin" + Formatting.GREEN + ".cc" + Formatting.GRAY + "] " + Formatting.GRAY);
        message.append(txt);

        IChatHud chatHud = (IChatHud) mc.inGameHud.getChatHud();
        chatHud.meteor$add(message,0);
    }
}
