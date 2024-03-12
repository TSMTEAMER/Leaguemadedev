package kassuk.addon.blackout.modules.render;

import kassuk.addon.blackout.BlackOut;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.BlockPos;

public class TntRange extends Module {

    public TntRange() {
        super(BlackOut.RENDERPLUS, "tnt-range", "Renders the damage range of ignited tnt");
    }
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<ShapeMode> renderType = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("mode")
        .defaultValue(ShapeMode.Lines)
        .build()
    );
    private final Setting<SettingColor> renderColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Color")
        .defaultValue(new SettingColor(255, 170, 0, 255))
        .build()
    );

    @EventHandler
    public void onRender3dEvent(Render3DEvent event) {
        for (Entity tnt : mc.world.getEntities()) {
            if (tnt instanceof TntEntity tntEntity) {
                BlockPos tntPosition = tntEntity.getBlockPos();
                event.renderer.box(tntPosition.getX() - 5.2, tntPosition.getY() - 5.2, tntPosition.getZ() - 5.2, tntPosition.getX() + 5.2, tntPosition.getY() + 5.2, tntPosition.getZ() + 5.2, new Color(renderColor.get().r, renderColor.get().g, renderColor.get().b, renderColor.get().a), new Color(renderColor.get().r, renderColor.get().g, renderColor.get().b, renderColor.get().a), renderType.get(), 0);
            }
        }
    }
}
