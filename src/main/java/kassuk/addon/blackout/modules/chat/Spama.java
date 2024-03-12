package kassuk.addon.blackout.modules.chat;

import com.google.common.collect.Lists;
import kassuk.addon.blackout.BlackOut;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.List;

@Environment(EnvType.CLIENT)
public class Spama extends Module {
    public enum Text {
        Ad
    }

    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Enum<Text>> text = sgGeneral.add(new EnumSetting.Builder<Enum<Text>>()
        .name("text")
        .defaultValue(Text.Ad)
        .onChanged(e -> messageI = 0)
        .build()
    );



    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The delay between specified messages in ticks.")
        .defaultValue(20)
        .min(0)
        .sliderMax(200)
        .build()
    );

    private int messageI, timer;

    public Spama() {
        super(BlackOut.CHATPLUS, "Spammer+", "Better than spam.");
    }

    @Override
    public void onActivate() {
        timer = delay.get();
        messageI = 0;



    }
    private static final List<String> AD = Lists.newArrayList(
              "Join Ricin.cc gang https://discord.gg/FDNay74pee"

    );
}
