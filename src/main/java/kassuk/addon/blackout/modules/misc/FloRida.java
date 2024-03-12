package kassuk.addon.blackout.modules.misc;

import kassuk.addon.blackout.BlackOut;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.Quiver;
import meteordevelopment.meteorclient.systems.modules.player.EXPThrower;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;

//From venomhack

public class FloRida extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("rotation-speed")
            .description("The speed at which you rotate.")
            .defaultValue(20)
            .sliderMin(0.0)
            .sliderMax(50.0)
            .build()
    );
    private final Setting<Boolean> bowMode = sgGeneral.add(new BoolSetting.Builder()
        .name("Look Up With Bow")
        .description("Looks up while holding a bow.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> encMode = sgGeneral.add(new BoolSetting.Builder()
        .name("Look Down With Exp")
        .description("Looks down while holding experience bottles.")
        .defaultValue(true)
        .build()
    );

    private int count = 0;
    public FloRida() {super(BlackOut.MISCPLUS, "flo-rida", "Makes you spin right round.");}

    @EventHandler
    public void onTick(TickEvent.Post event) {
        Modules modules = Modules.get();
        if (!modules.isActive(EXPThrower.class) && !modules.isActive(Quiver.class) && !modules.isActive(EXPThrower.class)) {
            count += speed.get();
            if (count > 180) {
                count -= 360;
            }

            Rotations.rotate(count, 0.0);
        }
    }
}
