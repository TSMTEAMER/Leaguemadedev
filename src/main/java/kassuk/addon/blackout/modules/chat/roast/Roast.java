package kassuk.addon.blackout.modules.chat.roast;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.events.EventHandler;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import kassuk.addon.blackout.modules.chat.roast.Utilsroast;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.item.Items;
import org.apache.commons.lang3.RandomStringUtils;

public class Roast extends Module {
    private int messageI, timer;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAntiSpamBypass = settings.createGroup("Anti Spam Bypass");

    // General

    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-self")
        .description("Skips messages when you're in %player%.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Skips messages when the %player% is a friend.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The delay between specified messages in ticks.")
        .defaultValue(250)
        .min(0)
        .sliderRange(0, 1000)
        .build()
    );

    private final Setting<Boolean> disableOnLeave = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-leave")
        .description("Disables spam when you leave a server.")
        .defaultValue(true)
        .build()
    );


    private final Setting<Boolean> disableOnDisconnect = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-disconnect")
        .description("Disables spam when you are disconnected from a server.")
        .defaultValue(true)
        .build()
    );

    // Anti Spam Bypass

    private final Setting<Boolean> randomText = sgAntiSpamBypass.add(new BoolSetting.Builder()
        .name("random-text")
        .description("Adds random text at the bottom of the text.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> randomTextLength = sgAntiSpamBypass.add(new IntSetting.Builder()
        .name("length")
        .description("Text length of anti spam bypass.")
        .defaultValue(16)
        .sliderRange(1, 256)
        .visible(randomText::get)
        .build()
    );

    public Roast() {
        super(BlackOut.CHATPLUS, "roast", "Insults a random player");
    }

    @Override
    public void onActivate() {
        timer = delay.get();
        messageI = 0;
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (disableOnDisconnect.get() && event.screen instanceof DisconnectedScreen) toggle();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disableOnLeave.get()) toggle();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (timer <= 0) {
            String text = ((RoastMessage)HTTP.get("https://evilinsult.com/generate_insult.php?lang=en&type=json").sendJson(RoastMessage.class)).insult;
            if (randomText.get()) text += " " + RandomStringUtils.randomAlphabetic(randomTextLength.get()).toLowerCase();

            String player;
            do {
                player = Utilsroast.getRandomPlayer();
            } while (ignoreSelf.get() && player.equals(mc.getSession().getUsername()) || ignoreFriends.get() && Friends.get().get(player) != null);

            mc.player.networkHandler.sendChatMessage(player + ", " + text);

            timer = delay.get();
        } else timer--;
    }

    private static class RoastMessage {
        public String number;
        public String language;
        public String insult;
        public String created;
        public String shown;
        public String createdby;
        public String active;
        public String comment;
    }
}
