package kassuk.addon.blackout.modules.misc;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;

import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.util.math.BlockPos;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.utils.ikea.ChunkUtils;
import kassuk.addon.blackout.utils.ikea.DiscordWebhook;
import kassuk.addon.blackout.utils.ikea.LogUtils;

import java.awt.*;
import java.io.IOException;

public class DiscordNotifier extends Module {

    private final String name = "League bot";
    private final String avatar = "";

    private DiscordWebhook hook;

    public SettingGroup sgGeneral = settings.getDefaultGroup();
    public SettingGroup sgNotifs = settings.createGroup("Notifications");

    public DiscordNotifier() {
        super(BlackOut.MISCPLUS,"discord-notifier", "Sends notifications to a Discord webhook on certain events.");
    }

    private final Setting<String> link = sgGeneral.add(new StringSetting.Builder()
        .name("webhook-URL")
        .description("Discord Webhook URL to send messages to")
        .defaultValue("https://discord.com/api/webhooks/1169659987593789572/Ksuw1_lgZpbetxtMfnxwxNqga-OTHA2fOy7VhIRZc9d4ZDMrsQXyXj2wkdwEIHgvwSTj")
        .build());

    private final Setting<DiscordNotifier.PingModes> pingMode = sgGeneral.add(new EnumSetting.Builder<DiscordNotifier.PingModes>()
        .name("ping-mode")
        .description("How the notifier should ping")
        .defaultValue(PingModes.NoPing)
        .build()
    );

    private final Setting<String> userId = sgGeneral.add(new StringSetting.Builder()
        .name("discord-ID")
        .description("ID of the user to ping")
        .defaultValue("")
        .visible(() -> pingMode.get() == PingModes.User)
        .build());

    private final Setting<Boolean> stashNotifier = sgNotifs.add(new BoolSetting.Builder()
        .name("stash-notifier")
        .defaultValue(true)
        .build());

    private final Setting<Integer> chestLimit = sgNotifs.add(new IntSetting.Builder()
        .name("chest-limit")
        .description("How many chests until you get notified")
        .sliderRange(0,100)
        .defaultValue(50)
        .visible(stashNotifier::get)
        .build());

    private final Setting<Integer> shulkerLimit = sgNotifs.add(new IntSetting.Builder()
        .name("shulker-limit")
        .description("How many shulkers until you get notified")
        .sliderRange(0,100)
        .defaultValue(50)
        .visible(stashNotifier::get)
        .build());

    private final Setting<Boolean> deathNotifier = sgNotifs.add(new BoolSetting.Builder()
        .name("death-notifier")
        .description("Get notified on death")
        .defaultValue(true)
        .build());





    @Override
    public void onActivate() {
        String url = link.get();
        if (!url.isEmpty()) {
            hook = new DiscordWebhook(url);
        } else {
            LogUtils.info("Invalid webhook URL in DiscordNotifier");
            this.toggle();
        }

    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) throws IOException {
        assert mc.player != null;
        if (hook == null) return;

        if (stashNotifier.get()) {
            BlockPos pos = event.chunk().getPos().getStartPos();
            String posStr = "X: " + pos.getX() + " Z: " + pos.getZ();
            int chestCount = ChunkUtils.getChestCount(event.chunk());
            int shulkerCount = ChunkUtils.getShulkerCount(event.chunk());
            if (chestCount > chestLimit.get() || shulkerCount > shulkerLimit.get()) {
                readyHook(hook);
                hook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle("Unusual chest or shulker amount!" + " (" + mc.player.getName() + ")")
                    .setColor(Color.YELLOW)
                    .addField("Coordinates:", posStr, false)
                    .addField("Chest Amount:", String.valueOf(chestCount), false)
                    .addField("Shulker Amount:", String.valueOf(shulkerCount), false)
                    .setThumbnail(avatar));

                hook.execute();
                hook.clearEmbeds();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onOpenScreen(OpenScreenEvent event) throws IOException {
        assert mc.player != null;
        if (hook == null) return;

        if (event.screen instanceof DeathScreen && deathNotifier.get()) {
            BlockPos pos = mc.player.getBlockPos();
            String posStr = "X: " + pos.getX() + " Y: " + pos.getY() + " Z: " + pos.getZ();
            readyHook(hook);
            hook.addEmbed(new DiscordWebhook.EmbedObject()
                .setTitle("You have died!" + " (" + mc.player.getName() + ")")
                .setColor(Color.YELLOW)
                .addField("Coordinates:", posStr,false)
                .addField("Dimension:", PlayerUtils.getDimension().toString(), false)
                .setThumbnail(avatar));

            hook.execute();
            hook.clearEmbeds();
        }
    }

    private void readyHook(DiscordWebhook hook) {
        assert mc.player != null;
        if (hook == null) return;

        String mention = "";
        switch (pingMode.get()) {
            case Everyone -> mention = "@everyone";
            case User -> mention = "<@" + userId.get() + ">";
            case NoPing -> mention = "";
        }

        hook.setContent(mention);
        hook.setAvatarUrl(avatar);
        hook.setUsername(name);
    }

    public enum PingModes {
        User,
        Everyone,
        NoPing
    }
}
