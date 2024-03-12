package kassuk.addon.blackout.modules.chat;

import kassuk.addon.blackout.BlackOut;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ChatConfig extends Module {
    public final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>().name("prefix").description("The way to render League prefix.").defaultValue(Mode.League).build());
    public final Setting<String> text = sgGeneral.add(new StringSetting.Builder().name("text").description("Text of the prefix").defaultValue("League").build());
    public final Setting<Boolean> chatFormatting = sgGeneral.add(new BoolSetting.Builder().name("chat-formatting").description("Changes style of messages.").defaultValue(false).build());
    private final Setting<ChatFormatting> formattingMode = sgGeneral.add(new EnumSetting.Builder<ChatFormatting>().name("mode").description("The style of messages.").defaultValue(ChatFormatting.Bold).visible(chatFormatting::get).build());

    public ChatConfig() {
        super(BlackOut.CHATPLUS, "chat-config", "The way to render chat messages.");
    }

    @Override
    public void onActivate() {
        if (mode.get() == Mode.League) ChatUtils.registerCustomPrefix("kassuk.addon.blackout", this::getPrefix);
    }



    public Text getPrefix() {
        MutableText logo = Text.literal(text.get());
        MutableText prefix = Text.literal("");
        logo.setStyle(logo.getStyle().withFormatting(Formatting.DARK_PURPLE));
        prefix.setStyle(prefix.getStyle().withFormatting(Formatting.DARK_PURPLE));
        prefix.append("[");
        prefix.append(logo);
        prefix.append("] ");
        return prefix;
    }

    private Formatting getFormatting(ChatFormatting chatFormatting) {
        return switch (chatFormatting) {
            case Obfuscated -> Formatting.OBFUSCATED;
            case Bold -> Formatting.BOLD;
            case Strikethrough -> Formatting.STRIKETHROUGH;
            case Underline -> Formatting.UNDERLINE;
            case Italic -> Formatting.ITALIC;
        };
    }

    public enum Mode {
        Always, League, Clear
    }

    public enum ChatFormatting {
        Obfuscated, Bold, Strikethrough, Underline, Italic
    }
}
