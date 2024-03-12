package kassuk.addon.blackout.modules.misc;

import kassuk.addon.blackout.BlackOut;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.Script;
import meteordevelopment.starscript.compiler.Compiler;
import meteordevelopment.starscript.compiler.Parser;
import meteordevelopment.starscript.utils.StarscriptError;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.*;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.EditGameRulesScreen;
import net.minecraft.client.gui.screen.world.EditWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsScreen;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

public class DiscordRPC extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgLine1 = settings.createGroup("Line 1");
    private final SettingGroup sgLine2 = settings.createGroup("Line 2");

    // Line 1
    private final Setting<Mode> type = sgGeneral.add(new EnumSetting.Builder<Mode>().name("type").defaultValue(Mode.League).build());
    private final Setting<String> version = sgGeneral.add(new StringSetting.Builder().name("version").defaultValue("League.win").build());

    private final Setting<List<String>> line1Strings = sgLine1.add(new StringListSetting.Builder().name("line-1-messages").description("Messages used for the first line.").defaultValue("{player}").onChanged(strings -> recompileLine1()).build());
    private final Setting<Integer> line1UpdateDelay = sgLine1.add(new IntSetting.Builder().name("update-delay").description("How fast to update the first line in ticks.").defaultValue(200).min(10).sliderRange(10, 200).build());
    private final Setting<SelectMode> line1SelectMode = sgLine1.add(new EnumSetting.Builder<SelectMode>().name("line-1-select-mode").description("How to select messages for the first line.").defaultValue(SelectMode.Sequential).build());

    // Line 2
    private final Setting<List<String>> line2Strings = sgLine2.add(new StringListSetting.Builder().name("line-2-messages").description("Messages used for the second line.").defaultValue("League.win >","Owning {server}.", "spawn was cleared!", "GG predicted by League.win").onChanged(strings -> recompileLine2()).build());
    private final Setting<Integer> line2UpdateDelay = sgLine2.add(new IntSetting.Builder().name("update-delay").description("How fast to update the second line in ticks.").defaultValue(60).min(10).sliderRange(10, 200).build());
    private final Setting<SelectMode> line2SelectMode = sgLine2.add(new EnumSetting.Builder<SelectMode>().name("line-2-select-mode").description("How to select messages for the second line.").defaultValue(SelectMode.Sequential).build());

    private static final RichPresence rpc = new RichPresence();
    private boolean forceUpdate, lastWasInMainMenu;

    private final List<Script> line1Scripts = new ArrayList<>();
    private int line1Ticks, line1I;

    private final List<Script> line2Scripts = new ArrayList<>();
    private int line2Ticks, line2I;

    public DiscordRPC() {
        super(BlackOut.MISCPLUS, "DiscordRPC", "Displays league as your presence on Discord.");

        runInMainMenu = true;
    }

    @Override
    public void onActivate() {
        String id = "League", image = "League", name = "League";
        switch (type.get()) {
            case League -> {
                id = "1205886386104963143";
                image = "1";
                name = " ";
            }
        }
        switch (type.get()) {
            case Trayvon -> {
                id = "1176956044988141599";
                image = "trayvon";
                name = " ";
            }
            case Ricin -> {
                id = "1144767822942175313";
                image = "logo1";
                name = " ";
            }

            case BedTrap -> {
                id = "853721983241551873";
                image = "btpng";
                name = " ";
            }
            case Banana -> {
                id = "870386147069661274";
                image = "banana_plus";
                name = "";
            }
            case Ion -> {
                id = "915376082507685998";
                image = "ion";
                name = " ";
            }
        }

        DiscordIPC.start(Long.parseLong(id), null);

        rpc.setStart(System.currentTimeMillis() / 1000L);
        rpc.setLargeImage(image, name + version.get());

        recompileLine1();
        recompileLine2();

        line1Ticks = 0;
        line2Ticks = 0;
        lastWasInMainMenu = false;

        line1I = 0;
        line2I = 0;
    }

    @Override
    public void onDeactivate() {
        DiscordIPC.stop();
    }

    private void recompile(List<String> messages, List<Script> scripts) {
        scripts.clear();

        for (int i = 0; i < messages.size(); i++) {
            Parser.Result result = Parser.parse(messages.get(i));

            if (result.hasErrors()) {
                if (Utils.canUpdate()) {
                    MeteorStarscript.printChatError(i, result.errors.get(0));
                }

                continue;
            }

            scripts.add(Compiler.compile(result));
        }

        forceUpdate = true;
    }

    private void recompileLine1() {
        recompile(line1Strings.get(), line1Scripts);
    }

    private void recompileLine2() {
        recompile(line2Strings.get(), line2Scripts);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        boolean update = false;

        if (Utils.canUpdate()) {
            // Line 1
            if (line1Ticks >= line1UpdateDelay.get() || forceUpdate) {
                if (line1Scripts.size() > 0) {
                    int i = Utils.random(0, line1Scripts.size());
                    if (line1SelectMode.get() == SelectMode.Sequential) {
                        if (line1I >= line1Scripts.size()) line1I = 0;
                        i = line1I++;
                    }

                    try {
                        rpc.setDetails(String.valueOf(MeteorStarscript.ss.run(line1Scripts.get(i))));
                    } catch (StarscriptError e) {
                        ChatUtils.error("Starscript", e.getMessage());
                    }
                }
                update = true;

                line1Ticks = 0;
            } else line1Ticks++;

            // Line 2
            if (line2Ticks >= line2UpdateDelay.get() || forceUpdate) {
                if (line2Scripts.size() > 0) {
                    int i = Utils.random(0, line2Scripts.size());
                    if (line2SelectMode.get() == SelectMode.Sequential) {
                        if (line2I >= line2Scripts.size()) line2I = 0;
                        i = line2I++;
                    }

                    try {
                        rpc.setState(String.valueOf(MeteorStarscript.ss.run(line2Scripts.get(i))));
                    } catch (StarscriptError e) {
                        ChatUtils.error("Starscript", e.getMessage());
                    }
                }
                update = true;

                line2Ticks = 0;
            } else line2Ticks++;
        } else {
            if (!lastWasInMainMenu) {
                rpc.setDetails("League.win");

                if (mc.currentScreen instanceof TitleScreen) rpc.setState("Looking at title screen");
                else if (mc.currentScreen instanceof SelectWorldScreen) rpc.setState("Selecting world");
                else if (mc.currentScreen instanceof CreateWorldScreen || mc.currentScreen instanceof EditGameRulesScreen) rpc.setState("Creating world");
                else if (mc.currentScreen instanceof EditWorldScreen) rpc.setState("Editing world");
                else if (mc.currentScreen instanceof LevelLoadingScreen) rpc.setState("Loading world");
                else if (mc.currentScreen instanceof MultiplayerScreen) rpc.setState("Selecting server");
                else if (mc.currentScreen instanceof AddServerScreen) rpc.setState("Adding server");
                else if (mc.currentScreen instanceof ConnectScreen || mc.currentScreen instanceof DirectConnectScreen) rpc.setState("Connecting to server");
                else if (mc.currentScreen instanceof WidgetScreen) rpc.setState("Browsing Meteor's GUI");
                else if (mc.currentScreen instanceof OptionsScreen || mc.currentScreen instanceof SkinOptionsScreen || mc.currentScreen instanceof SoundOptionsScreen || mc.currentScreen instanceof VideoOptionsScreen || mc.currentScreen instanceof ControlsOptionsScreen || mc.currentScreen instanceof LanguageOptionsScreen || mc.currentScreen instanceof ChatOptionsScreen || mc.currentScreen instanceof PackScreen || mc.currentScreen instanceof AccessibilityOptionsScreen) rpc.setState("Changing options");
                else if (mc.currentScreen instanceof CreditsScreen) rpc.setState("Reading credits");
                else if (mc.currentScreen instanceof RealmsScreen) rpc.setState("Browsing Realms");
                else {
                    String className = mc.currentScreen.getClass().getName();

                    if (className.startsWith("com.terraformersmc.modmenu.gui")) rpc.setState("Browsing mods");
                    else if (className.startsWith("me.jellysquid.mods.sodium.client")) rpc.setState("Changing options");
                    else rpc.setState("In main menu");
                }

                update = true;
            }
        }

        // Update
        if (update) DiscordIPC.setActivity(rpc);
        forceUpdate = false;
        lastWasInMainMenu = !Utils.canUpdate();
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (!Utils.canUpdate()) lastWasInMainMenu = false;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WButton help = theme.button("Open documentation.");
        help.action = () -> Util.getOperatingSystem().open("https://github.com/MeteorDevelopment/meteor-client/wiki/Starscript");

        return help;
    }

    public enum SelectMode {
        Sequential, Random
    }

    public enum Mode {
        League,
        Trayvon,
        Ricin,
        BedTrap,
        Banana,
        Ion,
        Venomhack,
        MeteorPlus,
        SnusClient
    }
}
