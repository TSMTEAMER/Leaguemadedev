// Decompiled with: CFR 0.152
// Class Version: 17
package kassuk.addon.blackout.modules.combat;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.events.SoundInstanceEvent;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.GameMode;
import kassuk.addon.blackout.ModuleHelper;
import kassuk.addon.blackout.utils.vh.Origin;
import kassuk.addon.blackout.utils.vh.ColorUtils;
import kassuk.addon.blackout.utils.vh.RandUtils;
import kassuk.addon.blackout.utils.vh.BlockUtils2;
public class PacketMine
extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> strict = sgGeneral.add(new BoolSetting.Builder()
        .name("strict")
        .description("For test 2b. might be buggy.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("How many blocks away you can mine.")
        .defaultValue(5).sliderRange(1, 10)
        .build()
    );

    private final Setting<Origin> rangemode = sgGeneral.add(new EnumSetting.Builder<Origin>()
        .name("range-mode")
        .description("How to calculate the range.")
        .defaultValue(Origin.NCP)
        .build()
    );
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Whether to rotate when mining.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> automineoption = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-remine")
        .description("Continues mining the block when it gets replaced.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> eatPause = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-while-eating")
        .description("Won't swap slots while you are eating, to not interupt it.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> maxInstaMineAttempts = sgGeneral.add(new IntSetting.Builder()
        .name("insta-mine-attempts")
        .description("How many times you want to attempt to insta mine in a row without having to remine.")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );
    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
        .name("swing")
        .description("Makes your hand swing client side when mining.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> render = sgGeneral.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders the block you are mining.")
        .defaultValue(true)
        .build()
    );
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("Shape Mode")
        .description("Which parts of render should be rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );
    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Line Color")
        .description("Line color of rendered boxes")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );
    public final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Side Color")
        .description("Side color of rendered boxes")
        .defaultValue(new SettingColor(255, 0, 0, 50))
        .build()
    );

    private final Setting<Boolean> percColors = sgGeneral.add(new BoolSetting.Builder()
        .name("progress-colors")
        .description("Will render red when starting and green when finished with gradient.")
        .defaultValue(true)
        .build()
    );
    private float progress;
    private volatile boolean hasSwitched;
    private long breakTimeMs;
    private int bestSlot;
    private int amountOfInstaBreaks;
    private BlockState lastState;
    private BlockPos pos;
    private Direction direction;

    public PacketMine() {
        super(BlackOut.COMBATPLUS, "packet-mine-vh", "Actual good packet mine. Can be used as auto mine.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (this.progress == -1.0f || this.pos == null || this.lastState == null) {
            return;
        }
        if (BlockUtils2.outOfMiningRange(this.pos, this.rangemode.get(), this.range.get())) {
        this.pos = null;
        this.lastState = null;
        return;
    }
        this.lastState = this.mc.world.getBlockState(this.pos);
        this.findBestSlot();
        if (!this.lastState.isAir()) {
            this.progress = (float)((double)this.progress + Math.max(0.0, this.getBreakDelta(this.bestSlot != -1 ? this.bestSlot : this.mc.player.getInventory().selectedSlot, this.lastState)) * 20.0 / (double)TickRate.INSTANCE.getTickRate());
        }
        if (this.progress >= 1.0f) {
            if (this.lastState.isAir()) {
                this.hasSwitched = false;
                return;
            }
            if (!this.hasSwitched && this.amountOfInstaBreaks <= this.maxInstaMineAttempts.get()) {
                if (this.canSwitch()) {
                    this.doBreakAndSwitch(this.pos, this.rotate.get());
                }
            } else {
                if ((double)TickRate.INSTANCE.getTimeSinceLastTick() > 1.5) {
                    return;
                }
                PlayerListEntry playerlistEntry = this.mc.player.networkHandler.getPlayerListEntry(this.mc.getSession().getProfile().getId());
                if (playerlistEntry != null && (float)(System.currentTimeMillis() - this.breakTimeMs) > (float)MathHelper.clamp(playerlistEntry.getLatency(), 50, 300) * 20.0f / TickRate.INSTANCE.getTickRate()) {
                    if (automineoption.get().booleanValue()) {
                        if (this.amountOfInstaBreaks < this.maxInstaMineAttempts.get() && this.canSwitch()) {
                            this.doBreakAndSwitch(this.pos, this.rotate.get());
                        }
                        this.startMining(this.pos, this.rotate.get());
                    }  {
                        this.pos = null;
                        this.lastState = null;
                    }
                }
            }
        }
    }
    private void doBreakAndSwitch(BlockPos pos, boolean rotate) {
        if (rotate) {
            Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 50, () -> this.doBreakAndSwitch(pos, false));
            return;
        }
        this.findBestSlot();
        if (this.bestSlot != -1) {
            this.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.bestSlot));
        }
        this.mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, this.direction));
        RandUtils.swing(this.swing.get());
        this.breakTimeMs = System.currentTimeMillis();
        this.hasSwitched = true;
        ++this.amountOfInstaBreaks;
        if (this.bestSlot != -1) {
            this.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.mc.player.getInventory().selectedSlot));
        }
    }

    @EventHandler
    private void onStartMining(StartBreakingBlockEvent event) {
        GameMode gamemode;
        PlayerListEntry playerlistEntry = this.mc.player.networkHandler.getPlayerListEntry(this.mc.getSession().getProfile().getId());
        if (playerlistEntry != null && ((gamemode = playerlistEntry.getGameMode()) == GameMode.SPECTATOR || gamemode == GameMode.ADVENTURE)) {
            return;
        }
        BlockState blockState = this.mc.world.getBlockState(event.blockPos);
        if (blockState.getHardness(null, null) <= 0.0f) {
            this.pos = null;
            this.lastState = null;
            return;
        }
        this.direction = event.direction;
        if (event.blockPos.equals(this.pos)) {
            return;
        }
        this.pos = event.blockPos;
        this.lastState = blockState;
        this.startMining(this.pos, this.rotate.get());
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (this.pos == null) {
            return;
        }
        Packet packet = event.packet;
        if (packet instanceof PlayerActionC2SPacket) {
            PlayerActionC2SPacket packet2 = (PlayerActionC2SPacket)packet;
            if (packet2.getPos().equals(this.pos) || this.mc.world.getBlockState(this.pos).isAir()) {
                return;
            }
            event.cancel();
        } else {
            packet = event.packet;
            if (packet instanceof UpdateSelectedSlotC2SPacket) {
                UpdateSelectedSlotC2SPacket packet3 = (UpdateSelectedSlotC2SPacket)packet;
                if (this.strict.get().booleanValue() && this.bestSlot != -1 && packet3.getSelectedSlot() != this.bestSlot && !this.hasSwitched && this.lastState != null && !this.lastState.isAir()) {
                     {
                        this.pos = null;
                    }
                }
            }
        }
    }

    private void startMining(BlockPos pos, boolean rotate) {
        if (pos == null) {
            return;
        }
        if (rotate) {
            Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 50, () -> this.startMining(pos, false));
            return;
        }
        if (this.strict.get().booleanValue()) {
            this.findBestSlot();
            if (this.bestSlot != -1) {
                this.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.bestSlot));
            }
        }
        this.hasSwitched = false;
        this.amountOfInstaBreaks = 0;
        this.progress = 0.0f;
        this.mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, this.direction));
        this.mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos, this.direction));
        RandUtils.swing(this.swing.get());
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (this.pos == null || this.lastState == null || this.progress == -1.0f || !this.render.get().booleanValue()) {
            return;
        }
        float prog = 1.0f - MathHelper.clamp((this.progress > 0.5f ? this.progress - 0.5f : 0.5f - this.progress) * 2.0f, 0.0f, 1.0f);
        VoxelShape shape = this.lastState.getOutlineShape(this.mc.world, this.pos);
        if (shape.isEmpty()) {
            return;
        }
        Box original = shape.getBoundingBox();
        Box box = original.shrink(original.getXLength() * (double)prog, original.getYLength() * (double)prog, original.getZLength() * (double)prog);
        double xShrink = original.getXLength() * (double)prog * 0.5;
        double yShrink = original.getYLength() * (double)prog * 0.5;
        double zShrink = original.getZLength() * (double)prog * 0.5;
        Color sideProgressColor = ColorUtils.getColorFromPercent(this.progress).a(this.sideColor.get().a);
        Color lineProgressColor = ColorUtils.getColorFromPercent(this.progress).a(this.lineColor.get().a);
        event.renderer.box((double)this.pos.getX() + box.minX + xShrink, (double)this.pos.getY() + box.minY + yShrink, (double)this.pos.getZ() + box.minZ + zShrink, (double)this.pos.getX() + box.maxX + xShrink, (double)this.pos.getY() + box.maxY + yShrink, (double)this.pos.getZ() + box.maxZ + zShrink, this.percColors.get() != false ? sideProgressColor : this.sideColor.get(), this.percColors.get() != false ? lineProgressColor : this.lineColor.get(), this.shapeMode.get(), 0);
    }


    private boolean canSwitch() {
        if (!this.eatPause.get().booleanValue() || this.bestSlot == -1) {
            return true;
        }
        return !this.mc.player.isUsingItem() || this.mc.player.getActiveHand() == Hand.OFF_HAND;
    }

    public BlockState getState(BlockPos pos) {
        if (this.isActive() && this.hasSwitched && this.progress >= 1.0f && pos.equals(this.pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return this.mc.world.getBlockState(pos);
    }

    public boolean isMineTarget(BlockPos pos) {
        return this.isActive() && pos.equals(this.pos) && this.progress > 0.0f;
    }

    private void findBestSlot() {
        if (this.lastState == null) {
            return;
        }
        this.bestSlot = -1;
        double bestScore = -1.0;
        for (int i = 0; i < 9; ++i) {
            ItemStack itemStack = this.mc.player.getInventory().getStack(i);
            float score = itemStack.getMiningSpeedMultiplier(this.lastState);
            if (score == 1.0f || !((double)score > bestScore)) continue;
            bestScore = score;
            this.bestSlot = i;
        }
    }

    private double getBreakDelta(int slot, BlockState state) {
        ItemStack tool;
        int efficiency;
        PlayerListEntry playerlistEntry = this.mc.player.networkHandler.getPlayerListEntry(this.mc.getSession().getProfile().getId());
        if (playerlistEntry != null && playerlistEntry.getGameMode() == GameMode.CREATIVE) {
            return 1.0;
        }
        float hardness = state.getHardness(null, null);
        if (hardness == -1.0f) {
            return 0.0;
        }
        float speed = ((ItemStack)this.mc.player.getInventory().main.get(slot)).getMiningSpeedMultiplier(state);
        if (speed > 1.0f && (efficiency = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, tool = this.mc.player.getInventory().getStack(slot))) > 0 && !tool.isEmpty()) {
            speed += (float)(efficiency * efficiency + 1);
        }
        if (StatusEffectUtil.hasHaste(this.mc.player)) {
            speed *= 1.0f + (float)(StatusEffectUtil.getHasteAmplifier(this.mc.player) + 1) * 0.2f;
        }
        if (this.mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            speed *= (float)(this.mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier() + 1) * 0.3f;
        }
        if (this.mc.player.isSubmergedIn(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(this.mc.player)) {
            speed /= 5.0f;
        }
        if (!this.mc.player.isOnGround()) {
            speed /= 5.0f;
        }
        return speed / hardness / (float)(!state.isToolRequired() || ((ItemStack)this.mc.player.getInventory().main.get(slot)).isSuitableFor(state) ? 30 : 100);
    }
    public enum Origin {
        VANILLA("Vanilla"),
        NCP("No Cheat Plus");

        private final String title;

        private Origin(String title) {
            this.title = title;
        }

        public String toString() {
            return this.title;
        }

        public Vec3d get(Vec3d pos) {
            if (this == VANILLA) {
                return pos;
            }
            return pos.add(0.0, MeteorClient.mc.player.getEyeHeight(MeteorClient.mc.player.getPose()), 0.0);
        }
    }
    public void onDeactivate() {
        this.pos = null;
        this.lastState = null;
        this.progress = -1.0f;
        this.bestSlot = -1;
        if (this.mc.player == null) {
            return;
        }
        this.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.mc.player.getInventory().selectedSlot));


    }
}
