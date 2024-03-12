package kassuk.addon.blackout.hud;

import kassuk.addon.blackout.BlackOut;
import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class Grass extends HudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Double> GrassScale = sgGeneral.add(new DoubleSetting.Builder()
        .name("Grass Scale")
        .description("Modify the size of the Grass.")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 10)
        .build()
    );
    private final Setting<SideMode> side = sgGeneral.add(new EnumSetting.Builder<SideMode>()
        .name("Kill Message Mode")
        .description("What kind of messages to send.")
        .defaultValue(SideMode.Right)
        .build()
    );
        private final Identifier Grass = new Identifier("blackout", "grass.png");

    public static final HudElementInfo<Grass> INFO = new HudElementInfo<>(BlackOut.HUD_BLACKOUT, "grass", "It's a BIGRAT what do you want", Grass::new);

    public Grass() {super(INFO);}
    @Override
    public void render(HudRenderer renderer) {
        setSize(450 * GrassScale.get(),755  * GrassScale.get());
        MatrixStack matrixStack = new MatrixStack();

        GL.bindTexture(Grass);
        Renderer2D.TEXTURE.begin();
        Renderer2D.TEXTURE.texQuad(x + (side.get() == SideMode.Left ? GrassScale.get() * 450 : 0),y, GrassScale.get() * (side.get() == SideMode.Left ? GrassScale.get() * -450 : 450), GrassScale.get() * 755, new Color(255, 255, 255, 255));
        Renderer2D.TEXTURE.render(matrixStack);
    }
    public enum SideMode {
        Right,
        Left
    }
}
