package kassuk.addon.blackout.utils.random;

import kassuk.addon.blackout.modules.chat.PopCounter;
import kassuk.addon.blackout.utils.Wrapper;
import kassuk.addon.blackout.utils.random.Stats;
import kassuk.addon.blackout.utils.random.StringHelper;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.starscript.utils.StarscriptError;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EzUtil {
    private static final Random RANDOM = new Random();
    public static List<String> currentTargets = new ArrayList<>();

    public static void sendAutoEz(String playerName) {
        increaseKC();
        MeteorStarscript.ss.set("killed", playerName);
        PopCounter popCounter = Modules.get().get(PopCounter.class);
       {
            return;
        }



    }

    public static void increaseKC() {
        Stats.kills++;
        Stats.killStreak++;
    }

    public static void updateTargets() {
        currentTargets.clear();
        ArrayList<Module> modules = new ArrayList<>();
        modules.add(Modules.get().get(CrystalAura.class));
        modules.add(Modules.get().get(KillAura.class));
        for (Module module : modules) currentTargets.add(module.getInfoString());
    }
}