package kassuk.addon.blackout.utils;

import kassuk.addon.blackout.modules.combat.*;
import kassuk.addon.blackout.modules.misc.*;
import kassuk.addon.blackout.modules.Player.ScaffoldPlus;

/**
 * @author OLEPOSSU
 */

public class PriorityUtils {
    // Tell me a better way to do this pls
    public static int get(Object module) {
        if (module instanceof AnchorAura) return 9;
        if (module instanceof AntiAim) return 12;
        if (module instanceof AutoCraftingTable) return 4;
        if (module instanceof AutoCrystal) return 10;
        if (module instanceof AutoMend) return 4;
        if (module instanceof PistonCrystal) return 10;
        if (module instanceof AutoMine) return 9;
        if (module instanceof AutoPearl) return 6;
        if (module instanceof AutoTrap) return 5;
        if (module instanceof BedAura) return 8;
        if (module instanceof HoleFillPlus) return 7;
        if (module instanceof HoleFill) return 7;
        if (module instanceof killAura) return 11;
        if (module instanceof ScaffoldPlus) return 2;
        if (module instanceof SelfTrapPlus) return 1;
        if (module instanceof SurroundPlus) return 0;

        return 100;
    }
}
