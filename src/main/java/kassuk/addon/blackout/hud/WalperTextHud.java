package kassuk.addon.blackout.hud;

import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;

public class WalperTextHud {
    public static final HudElementInfo<TextHud> INFO = new HudElementInfo<>(Hud.GROUP, "league-spotify", "", WalperTextHud::create);

    public static TextHud create() {
        return new TextHud(INFO);
    }

    public static HudElementInfo<TextHud>.Preset create(String name, String presettext) {
        return INFO.addPreset(name, textHud -> {
            textHud.text.set(presettext);
            textHud.updateDelay.set(1);
        });
    }
}
