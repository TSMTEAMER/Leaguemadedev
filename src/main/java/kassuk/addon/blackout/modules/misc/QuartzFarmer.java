package kassuk.addon.blackout.modules.misc;

import kassuk.addon.blackout.BlackOut;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public class QuartzFarmer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> toggleModules = sgGeneral.add(new BoolSetting.Builder()
            .name("toggle-modules")
            .description("Turn off specific modules when QuartzFarmer is activated (Select ur AutoTotem for offhand repairing).")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> toggleBack = sgGeneral.add(new BoolSetting.Builder()
            .name("toggle-back-on")
            .description("Turn the specific modules back on when QuartzFarmer is deactivated (Select ur AutoTotem for offhand repairing).")
            .defaultValue(false)
            .visible(toggleModules::get)
            .build()
    );

    private final Setting<List<Module>> modules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("modules")
            .description("Which modules to disable on activation.")
            .defaultValue(new ArrayList<>() {{
            }})
            .visible(toggleModules::get)
            .build()
    );

    private final Setting<Boolean> selfToggle = sgGeneral.add(new BoolSetting.Builder()
        .name("self-toggle")
        .description("Disables when you reach the desired amount of Quartz.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreExisting = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-existing")
        .description("Ignores existing Quartz in your inventory and mines the total target amount.")
        .defaultValue(true)
        .visible(selfToggle::get)
        .build()
    );

    private final Setting<Integer> amount = sgGeneral.add(new IntSetting.Builder()
            .name("amount")
            .description("The amount of Quartz to farm.")
            .defaultValue(256)
            .sliderMax(9999)
            .range(8, 9999)
            .sliderRange(1, 999)
            .visible(selfToggle::get)
            .build()
    );

    // Render

    private final Setting<Boolean> swingHand = sgRender.add(new BoolSetting.Builder()
        .name("swing-hand")
        .description("Swing hand client-side.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders a block overlay where the Quartz will be placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 50))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 255))
        .build()
    );

    private final VoxelShape SHAPE = Block.createCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);

    private BlockPos target;
    private int startCount;

    public QuartzFarmer() {
        super(BlackOut.MISCPLUS, "QuartzFarmer", "Places and breaks EChests to farm Quartz.");
    }

    public ArrayList<Module> toActivate;

    @Override
    public void onActivate() {
        toActivate = new ArrayList<>();
        target = null;
        startCount = InvUtils.find(Items.QUARTZ).count();
        if (toggleModules.get() && !modules.get().isEmpty() && mc.world != null && mc.player != null) {
            for (Module module : modules.get()) {
                if (module.isActive()) {
                    module.toggle();
                    toActivate.add(module);
                }
            }
        }
    }

    @Override
    public void onDeactivate() {
        if (toggleBack.get() && !toActivate.isEmpty() && mc.world != null && mc.player != null) {
            for (Module module : toActivate) {
                if (!module.isActive()) {
                    module.toggle();
                    InvUtils.swapBack();
                }
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Finding target pos
        if (target == null) {
            if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;

            BlockPos pos = ((BlockHitResult) mc.crosshairTarget).getBlockPos().up();
            BlockState state = mc.world.getBlockState(pos);

            if (state.isReplaceable() || state.getBlock() == Blocks.NETHER_QUARTZ_ORE) {
                target = ((BlockHitResult) mc.crosshairTarget).getBlockPos().up();
            } else return;
        }

        // Disable if the block is too far away
        if (!PlayerUtils.isWithinReach(target)) {
            error("Target block pos out of reach.");
            target = null;
            return;
        }

        // Toggle if Quartz amount reached
        if (selfToggle.get() && InvUtils.find(Items.QUARTZ).count() - (ignoreExisting.get() ? startCount : 0) >= amount.get()) {
            InvUtils.swapBack();
            toggle();
            return;
        }

        // Break existing Quartz Ore at target pos
        if (mc.world.getBlockState(target).getBlock() == Blocks.NETHER_QUARTZ_ORE) {
            double bestScore = -1;
            int bestSlot = -1;

            for (int i = 0; i < 9; i++) {
                ItemStack itemStack = mc.player.getInventory().getStack(i);
                if (EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, itemStack) > 0) continue;

                double score = itemStack.getMiningSpeedMultiplier(Blocks.NETHER_QUARTZ_ORE.getDefaultState());

                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = i;
                }
            }

            if (bestSlot == -1) return;

            InvUtils.swap(bestSlot, true);
            BlockUtils.breakBlock(target, swingHand.get());
        }

        // Place Quartz Ore if the target pos is empty
        if (mc.world.getBlockState(target).isReplaceable()) {
            FindItemResult echest = InvUtils.findInHotbar(Items.NETHER_QUARTZ_ORE);

            if (!echest.found()) {
                error("No Ore in hotbar, disabling");
                toggle();
                return;
            }

            BlockUtils.place(target, echest, true, 0, true);
        }
    }}
