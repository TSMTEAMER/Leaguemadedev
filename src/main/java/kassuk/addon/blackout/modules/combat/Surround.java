package kassuk.addon.blackout.modules.combat;

import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.utils.bed.advance.RenderUtils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.WorldRendererAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import static kassuk.addon.blackout.utils.bed.basic.BlockInfo.*;
import static kassuk.addon.blackout.utils.bed.basic.EntityInfo.getBlockPos;

public class Surround extends Module {
    private final SettingGroup sgGeneral = settings.createGroup("General", true);
    private final SettingGroup sgForce = settings.createGroup("Force", true);
    private final SettingGroup sgSafe = settings.createGroup("Safe", true);
    private final SettingGroup sgCenter = settings.createGroup("Center", true);
    private final SettingGroup sgMisc = settings.createGroup("Misc", true);
    private final SettingGroup sgRender = settings.createGroup("Render", true);

    //general
    private final Setting<Integer> bpt = sgGeneral.add(new IntSetting.Builder().name("blocks-per-tick").description("Number of blocks that can be placed per tick").defaultValue(3).min(1).sliderMax(10).build());
    private final Setting<Boolean> doubleH = sgGeneral.add(new BoolSetting.Builder().name("double").description("Places obsidian in face place positions").defaultValue(false).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Automatically faces towards the obsidian being placed.").defaultValue(false).build());
    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder().name("block").description("Which blocks used for surround.").defaultValue(Collections.singletonList(Blocks.OBSIDIAN)).filter(this::blockFilter).build());

    //force
    private final Setting<Boolean> forceSurround = sgForce.add(new BoolSetting.Builder().name("force-surround").description("Force places surround blocks(cool for ping players or bad servers)").defaultValue(false).build());
    private final Setting<Keybind> forceDouble = sgForce.add(new KeybindSetting.Builder().name("force-doube").description("Force double height surround").defaultValue(Keybind.fromKey(-1)).build());
    private final Setting<Keybind> forceTrap = sgForce.add(new KeybindSetting.Builder().name("force-trap").description("Force self trap").defaultValue(Keybind.fromKey(-1)).build());
    private final Setting<Keybind> forceAntiCity = sgForce.add(new KeybindSetting.Builder().name("force-anti-city").description("Force anti city blocks").defaultValue(Keybind.fromKey(-1)).build());

    //Safe
    private final Setting<Boolean> fagMode = sgSafe.add(new BoolSetting.Builder().name("break-safety").description("Force some blocks while surround is attacked.").defaultValue(false).build());
    private final Setting<Boolean> AGB = sgSafe.add(new BoolSetting.Builder().name("anti-ghost-block").description("Removing client-side surround blocks.").defaultValue(true).build());
    private final Setting<Boolean> antiCrystal = sgSafe.add(new BoolSetting.Builder().name("anti-crystal").description("Destroys all nearby crystals which can block surround blocks.").defaultValue(false).build());
    private final Setting<ACMode> antiCrystalMode = sgSafe.add(new EnumSetting.Builder<ACMode>().name("mode").description("Events to trigger crystal breaker.").defaultValue(ACMode.Break).visible(antiCrystal::get).build());
    private final Setting<Integer> antiCrystalDelay = sgSafe.add(new IntSetting.Builder().name("break-delay").description("Delay for breaking crystal.").defaultValue(3).min(0).sliderMax(20).visible(antiCrystal::get).build());
    private final Setting<Integer> antiCrystalSwapDelay = sgSafe.add(new IntSetting.Builder().name("swap-delay").description("Delay before swapping(bypasses some anticheats).").defaultValue(2).min(0).sliderMax(10).visible(antiCrystal::get).build());
    private final Setting<Boolean> antiCrystalBlock = sgSafe.add(new BoolSetting.Builder().name("block-on-crystal").description("Placing block on crystal.").defaultValue(false).visible(antiCrystal::get).build());
    private final Setting<Boolean> antiCrystalBlockSwap = sgSafe.add(new BoolSetting.Builder().name("block-swap").description("Swapping to obsidian block while breaking crystal(for servers with break delay after switch)").defaultValue(true).visible(antiCrystal::get).build());

    //center
    private final Setting<TpMode> centerMode = sgCenter.add(new EnumSetting.Builder<TpMode>().name("center-mode").description("Teleports you to the center of the surround.").defaultValue(TpMode.Default).build());
    private final Setting<Integer> centerDelay = sgCenter.add(new IntSetting.Builder().name("delay").description("Delay for teleporting to the center.").defaultValue(5).min(1).sliderMax(20).visible(() -> centerMode.get() == TpMode.Smooth).build());
    private final Setting<Boolean> stop = sgCenter.add(new BoolSetting.Builder().name("stop-moving").description("Stop all movements").defaultValue(false).build());
    private final Setting<Boolean> anchor = sgCenter.add(new BoolSetting.Builder().name("anchor").description("Slows you to prevent massive cope").defaultValue(true).build());

    //misc
    private final Setting<Boolean> pauseOnUse = sgMisc.add(new BoolSetting.Builder().name("pause-on-use").description("Pauses surround if players is using item(eating etc).").defaultValue(false).build());
    private final Setting<Boolean> onlyOnGround = sgMisc.add(new BoolSetting.Builder().name("only-on-ground").description("Works only when you standing on blocks.").defaultValue(true).build());
    private final Setting<Boolean> disableOnJump = sgMisc.add(new BoolSetting.Builder().name("disable-on-jump").description("Automatically disables when you jump.").defaultValue(true).build());
    private final Setting<Boolean> disableOnTp = sgMisc.add(new BoolSetting.Builder().name("disable-on-tp").description("Automatically disables when you teleport (chorus or pearl).").defaultValue(true).build());
    private final Setting<Boolean> disableOnYChange = sgMisc.add(new BoolSetting.Builder().name("disable-on-y-change").description("Automatically disables when your y level changes.").defaultValue(true).build());

    // Render
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").description("Client side hand-swing").defaultValue(true).build());
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders an overlay where blocks will be placed.").defaultValue(true).build());
    private final Setting<Boolean> newRender = sgRender.add(new BoolSetting.Builder().name("new-render").description("New type of block render").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("Side color.").defaultValue(new SettingColor(255, 0, 170, 35)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("Line color.").defaultValue(new SettingColor(255, 0, 170)).build());
    private final Setting<Double> lineSize = sgRender.add(new DoubleSetting.Builder().name("line-size").defaultValue(0.02).sliderRange(0.01, 1).visible(newRender::get).build());
    private final Setting<SettingColor> sideColor2 = sgRender.add(new ColorSetting.Builder().name("second-side-color").description("Side color.").defaultValue(new SettingColor(255, 255, 255, 35)).visible(newRender::get).build());
    private final Setting<SettingColor> lineColor2 = sgRender.add(new ColorSetting.Builder().name("second-line-color").description("Line color.").defaultValue(new SettingColor(255, 0, 170)).visible(newRender::get).build());

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();
    private static List<BlockPos> posPlaceBlocks = new ArrayList<>();

    Boolean obb;
    private BlockPos prevBreakPos, antiCrystalPos;
    private boolean fagNorth, fagEast, fagSouth, fagWest;
    private int ticks, centerDelayLeft, crystalDelay, swapDelay;

    public Surround() {
        super(BlackOut.COMBATPLUS, "surround-plus", "Surrounds you in blocks to prevent crystal damage");
    }

    @Override
    public void onActivate() {
        swapDelay = 0;
        fagNorth = false;
        fagEast = false;
        fagSouth = false;
        fagWest = false;
        centerDelayLeft = 0;
        crystalDelay = antiCrystalDelay.get();
        switch (centerMode.get()) {
            case Default -> {
                PlayerUtils.centerPlayer();
                pause();
            }
            case Smooth -> {
                centerDelayLeft = centerDelay.get();
                if (inCenter()) {
                    centerDelayLeft = 0;
                }
                if (anchor.get()) ((IVec3d) mc.player.getVelocity()).set(0, mc.player.getVelocity().y, 0);
            }
        }
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @Override
    public void onDeactivate() {
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @EventHandler
    public void onBreakPacket(PacketEvent.Receive event) {
        if (fagMode.get()) {
            assert mc.world != null;
            assert mc.player != null;
            if (event.packet instanceof BlockBreakingProgressS2CPacket) {
                BlockBreakingProgressS2CPacket bbpp = (BlockBreakingProgressS2CPacket) event.packet;
                BlockPos bbp = bbpp.getPos();

                if (bbp.equals(prevBreakPos) && bbpp.getProgress() > 0) return;

                BlockPos playerBlockPos = mc.player.getBlockPos();
                obb = mc.world.getBlockState(bbp).getBlock().getBlastResistance() >= 600 && getHardness(bbp) > 0;


                if (obb && bbp.equals(playerBlockPos.north())) {
                    fagNorth = true;
                } else if (obb && bbp.equals(playerBlockPos.east())) {
                    fagEast = true;
                } else if (obb && bbp.equals(playerBlockPos.south())) {
                    fagSouth = true;
                } else if (obb && bbp.equals(playerBlockPos.west())) {
                    fagWest = true;
                }

                prevBreakPos = bbp;
            }
        }
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        swapDelay--;
        crystalDelay--;
        antiCrystalPos = null;

        FindItemResult block =  InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        if (AGB.get()) {
            for (BlockPos p : posPlaceBlocks) {
                for (Entity e : mc.world.getEntities()) {
                    if (e instanceof EndCrystalEntity && e.getBlockPos().equals(p))
                        mc.world.setBlockState(p, Blocks.AIR.getDefaultState());
                }
            }
        }
        if (crystalDelay <= 0) {
            crystalDelay = antiCrystalDelay.get();
            for (BlockPos p : posPlaceBlocks) {
                if (antiCrystal.get()) {
                    boolean mine = isBreakable(p) || antiCrystalMode.get() != ACMode.Break;
                    for (BlockBreakingInfo value : ((WorldRendererAccessor) mc.worldRenderer).getBlockBreakingInfos().values()) {
                        if (value.getPos().equals(p)) {
                            mine = true;
                            break;
                        }
                    }
                    Box pBox = new Box(
                        p.getX() - 1, p.getY() - 1, p.getZ() - 1,
                        p.getX() + 1, p.getY() + 1, p.getZ() + 1
                    );

                    boolean finalMine = mine;
                    Predicate<Entity> ePr = entity -> entity instanceof EndCrystalEntity && finalMine;

                    for (Entity crstal : mc.world.getOtherEntities(null, pBox, ePr)) {
                        if (mc.player.distanceTo(crstal) <= 2.6) {
                            if (antiCrystal.get() && antiCrystalBlockSwap.get()) InvUtils.swap(block.slot(), false);
                            if (swapDelay>0) return;
                            if (rotate.get()) {
                                Rotations.rotate(Rotations.getPitch(crstal), Rotations.getYaw(crstal), () -> {
                                    mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crstal, mc.player.isSneaking()));
                                });
                            } else {
                                mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crstal, mc.player.isSneaking()));
                            }
                            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                            if (antiCrystalBlock.get()) antiCrystalPos = crstal.getBlockPos();
                        }
                    }
                }
            }
            posPlaceBlocks.clear();
        }
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (onlyOnGround.get() && !mc.player.isOnGround()) return;
        if ((disableOnJump.get() && (mc.options.jumpKey.isPressed() || mc.player.input.jumping)) || (disableOnYChange.get() && mc.player.prevY < mc.player.getY())) {
            toggle();
            return;
        }

        BlockPos blockPos = (notSafeBlock(getBlock(getBlockPos(mc.player))) ? getBlockPos(mc.player).up() : getBlockPos(mc.player));

        if (mc.player.isUsingItem() && pauseOnUse.get()) return;
        //center
        if (centerMode.get() == TpMode.Smooth) {
            if (centerDelayLeft > 0) {
                pause();
                assert mc.player != null;
                double decrX = MathHelper.floor(mc.player.getX()) + 0.5 - mc.player.getX();
                double decrZ = MathHelper.floor(mc.player.getZ()) + 0.5 - mc.player.getZ();
                double sqrtPos = Math.sqrt(Math.pow(decrX, 2.0) + Math.pow(decrZ, 2.0));
                double div = Math.sqrt(0.5) / centerDelay.get();
                if (sqrtPos <= div) {
                    centerDelayLeft = 0;
                    double x = MathHelper.floor(mc.player.getX()) + 0.5;
                    double z = MathHelper.floor(mc.player.getZ()) + 0.5;
                    mc.player.setPosition(x, mc.player.getY(), z);
                    return;
                }
                double x = mc.player.getX();
                double z = mc.player.getZ();
                double incX = MathHelper.floor(mc.player.getX()) + 0.5;
                double incZ = MathHelper.floor(mc.player.getZ()) + 0.5;
                double incResult = 0.0;
                double decrResult = 0.0;
                double x_ = mc.player.getX();
                double z_ = mc.player.getZ();
                if (Math.sqrt(Math.pow(decrX, 2.0)) > Math.sqrt(Math.pow(decrZ, 2.0))) {
                    if (decrX > 0.0) {
                        incResult = 0.5 / centerDelay.get();
                    } else if (decrX < 0.0) {
                        incResult = -0.5 / centerDelay.get();
                    }
                    x_ = mc.player.getX() + incResult;
                    z_ = z(x, z, incX, incZ, x_);
                } else if (Math.sqrt(Math.pow(decrX, 2.0)) < Math.sqrt(Math.pow(decrZ, 2.0))) {
                    if (decrZ > 0.0) {
                        decrResult = 0.5 / centerDelay.get();
                    } else if (decrZ < 0.0) {
                        decrResult = -0.5 / centerDelay.get();
                    }
                    z_ = mc.player.getZ() + decrResult;
                    x_ = x(x, z, incX, incZ, z_);
                } else if (Math.sqrt(Math.pow(decrX, 2.0)) == Math.sqrt(Math.pow(decrZ, 2.0))) {
                    if (decrX > 0.0) {
                        incResult = 0.5 / (double) centerDelay.get();
                    } else if (decrX < 0.0) {
                        incResult = -0.5 / (double) centerDelay.get();
                    }
                    x_ = mc.player.getX() + incResult;
                    if (decrZ > 0.0) {
                        decrResult = 0.5 / (double) centerDelay.get();
                    } else if (decrZ < 0.0) {
                        decrResult = -0.5 / (double) centerDelay.get();
                    }
                    z_ = mc.player.getZ() + decrResult;
                }
                pause();
                mc.player.setPosition(x_, mc.player.getY(), z_);
            }
        }

        ticks = 0;
        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);

        for (Direction side : Direction.values()) {
            if (side == Direction.UP || side == Direction.DOWN) continue;
            if (forceSurround.get()) {
              BlockUtils.place(blockPos.offset(side), InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem()))), rotate.get(), 20, swing.get(), false);
                renderBlocks.add(renderBlockPool.get().set(blockPos.offset(side)));
            }
        }

        posPlaceBlocks.clear();

        if (!onlyOnGround.get()) posPlaceBlocks.add(blockPos.down());
        for (CardinalDirection direction : CardinalDirection.values()) {
            posPlaceBlocks.add(blockPos.offset(direction.toDirection()));
            if (doubleH.get() || forceDouble.get().isPressed() || forceTrap.get().isPressed()) {
                posPlaceBlocks.add(blockPos.offset(direction.toDirection()).up());
                if (forceTrap.get().isPressed()) posPlaceBlocks.add(blockPos.up(2));
            }

            if (forceAntiCity.get().isPressed()) {
                posPlaceBlocks.add(blockPos.offset(direction.toDirection(), 2));
            }
        }

        if (forceAntiCity.get().isPressed()) {
            posPlaceBlocks.add(blockPos.add(1, 0, 1));
            posPlaceBlocks.add(blockPos.add(1, 0, -1));
            posPlaceBlocks.add(blockPos.add(-1, 0, 1));
            posPlaceBlocks.add(blockPos.add(-1, 0, -1));
        }

        if (fagMode.get()) {
            if (fagNorth) {
                posPlaceBlocks.add(blockPos.add(0, 0, -2));
                posPlaceBlocks.add(blockPos.add(0, 1, -1));
                posPlaceBlocks.add(blockPos.add(1, 0, -1));
                posPlaceBlocks.add(blockPos.add(-1, 0, -1));
                posPlaceBlocks.add(blockPos.add(0, -1, -1));
            }

            if (fagSouth) {
                posPlaceBlocks.add(blockPos.add(0, 0, 2));
                posPlaceBlocks.add(blockPos.add(0, 1, 1));
                posPlaceBlocks.add(blockPos.add(1, 0, 1));
                posPlaceBlocks.add(blockPos.add(-1, 0, 1));
                posPlaceBlocks.add(blockPos.add(0, -1, 1));
            }

            if (fagWest) {
                posPlaceBlocks.add(blockPos.add(-2, 0, 0));
                posPlaceBlocks.add(blockPos.add(-1, 1, 0));
                posPlaceBlocks.add(blockPos.add(-1, 0, 1));
                posPlaceBlocks.add(blockPos.add(-1, 0, -1));
                posPlaceBlocks.add(blockPos.add(-1, -1, 0));
            }

            if (fagEast) {
                posPlaceBlocks.add(blockPos.add(2, 0, 0));
                posPlaceBlocks.add(blockPos.add(1, 1, 0));
                posPlaceBlocks.add(blockPos.add(1, 0, 1));
                posPlaceBlocks.add(blockPos.add(1, 0, -1));
                posPlaceBlocks.add(blockPos.add(1, -1, 0));
            }
        }

        if (antiCrystalBlock.get() && antiCrystalPos != null) posPlaceBlocks.add(antiCrystalPos);
        for (BlockPos p : posPlaceBlocks) {
            if (BlockUtils.canPlace(p, false) && ticks <= bpt.get()) {
                if (anchor.get()) ((IVec3d) mc.player.getVelocity()).set(0, mc.player.getVelocity().y, 0);
                else BlockUtils.place(p, InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem()))), rotate.get(), 20, swing.get(), false);
                if (AGB.get()) mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, p, Direction.UP));
                renderBlocks.add(renderBlockPool.get().set(p));
                ticks++;
            }
        }
    }

    private boolean notSafeBlock(Block block) {
        if (block instanceof ChestBlock) return true;
        if (block instanceof EnderChestBlock) return true;

        return block instanceof SlabBlock;
    }

    private boolean blockFilter(Block block) {
        return isCombatBlock(block);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get()) {
            renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
            renderBlocks.forEach(renderBlock -> renderBlock.render(event, sideColor.get(), lineColor.get(),sideColor2.get(),lineColor2.get(), shapeMode.get(), lineSize.get()));
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof TeleportConfirmC2SPacket && disableOnTp.get()) toggle();
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) swapDelay = antiCrystalSwapDelay.get();
    }

    private class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public int ticks;

        public RenderBlock set(BlockPos blockPos) {
            pos.set(blockPos);
            ticks = 8;
            return this;
        }

        public void tick() {
            ticks--;
        }

        public void render(Render3DEvent event, Color sides, Color lines,Color sides2,Color lines2, ShapeMode shapeMode, double lineSize) {
            int preSideA = sides.a;
            int preLineA = lines.a;
            int preSideA2 = sides2.a;
            int preLineA2 = lines2.a;
            sides.a *= (double) ticks / 8;
            lines.a *= (double) ticks / 8;
            sides2.a *= (double) ticks / 8;
            lines2.a *= (double) ticks / 8;
            if (!newRender.get()) event.renderer.box(pos, sides, lines, shapeMode, 0);
            else RenderUtils.thickRender(event,pos,shapeMode,lines,lines2,sides,sides2, lineSize);
            sides.a = preSideA;
            lines.a = preLineA;
            sides2.a = preSideA2;
            lines2.a = preLineA2;
        }
    }


    private boolean inCenter() {
        if (mc.player == null) {
            return false;
        }
        if (mc.world == null) {
            return false;
        }
        if (mc.interactionManager == null) {
            return false;
        }
        int count = 0;
        if (mc.player.getBlockPos().equals(new BlockPos((int) (mc.player.getX() - (mc.player.getWidth() + 0.1) / 2.0), (int) mc.player.getY(), (int) (mc.player.getZ() - (mc.player.getWidth() + 0.1) / 2.0)))) {
            count++;
        }
        if (mc.player.getBlockPos().equals(new BlockPos((int) (mc.player.getX() + (mc.player.getWidth() + 0.1) / 2.0), (int) mc.player.getY(), (int) (mc.player.getZ() + (mc.player.getWidth() + 0.1) / 2.0)))) {
            count++;
        }
        if (mc.player.getBlockPos().equals(new BlockPos((int) (mc.player.getX() - (mc.player.getWidth() + 0.1) / 2.0), (int) mc.player.getY(), (int) (mc.player.getZ() + (mc.player.getWidth() + 0.1) / 2.0)))) {
            count++;
        }
        if (mc.player.getBlockPos().equals(new BlockPos((int) (mc.player.getX() + (mc.player.getWidth() + 0.1) / 2.0), (int) mc.player.getY(), (int) (mc.player.getZ() - (mc.player.getWidth() + 0.1) / 2.0)))) {
            count++;
        }
        return count == 4;
    }

    private double z(double a, double b, double c, double d, double e) {
        return (e - a) * (d - b) / (c - a) + b;
    }

    private double x(double a, double b, double c, double d, double e) {
        return (e - b) * (c - a) / (d - b) + a;
    }

    private void pause() {
        if (stop.get()) {
            mc.options.jumpKey.setPressed(false);
            mc.options.sprintKey.setPressed(false);
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
        }
    }

    public enum TpMode {
        Default, Smooth, None
    }

    public enum ACMode {
        Always, Break
    }
}
