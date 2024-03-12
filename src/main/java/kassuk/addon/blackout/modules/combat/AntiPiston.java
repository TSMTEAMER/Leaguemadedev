package kassuk.addon.blackout.modules.combat;

import com.google.common.eventbus.Subscribe;
import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.utils.bed.port.client.InvUtils;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class AntiPiston extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> push = sgGeneral.add(new BoolSetting.Builder()
        .name("Push")
        .defaultValue(false)
        .build()
    );

    private BlockPos blockPos;

    public AntiPiston() {
        super(BlackOut.COMBATPLUS, "Anti Piston", "Prevents pistons from pushing blocks.");
    }

    @Override
    public void onActivate() {
        blockPos = null;
    }

    @Subscribe
    public void onBlock(BlockUpdateEvent event) {
        if (!(event.newState.getBlock() instanceof PistonBlock)) {
            return;
        }

        if (PistonAura.get.shouldReturn(event.pos)) {
            return;
        }

        Box pistonBox = new Box(event.pos);

        for (Box box : getNearbyBoxes()) {
            if (pistonBox.intersects(box)) {
                blockPos = event.pos;
                break;
            }
        }

        if (blockPos != null) {
            doBlockBreak();
        }
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (blockPos != null && mc.world.isAir(blockPos)) {
            InvUtils.syncSlots();
            blockPos = null;
        }
    }

    private void doBlockBreak() {
        if (blockPos == null) return;

        BlockState blockState = mc.world.getBlockState(blockPos);

        // Break the block
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.DOWN));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.DOWN));

        // Replace the broken block with obsidian
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.DOWN));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.DOWN));

        // Swap to obsidian in hotbar
        InvUtils.swap(InvUtils.findInHotbar(Items.OBSIDIAN));

        // Swing the hand
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        // Swap back to previous tool/item
        InvUtils.swapBack();
    }

    private List<Box> getNearbyBoxes() {
        List<Box> boxes = new ArrayList<>();

        for (int height = 1; height <= 2; height++) {
            BlockPos blockPos = mc.player.getBlockPos().up(height);

            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP || direction == Direction.DOWN) {
                    continue;
                }

                BlockPos crystalPos = blockPos.offset(direction);
                boolean canPlace = mc.world.isAir(crystalPos) &&
                    (mc.world.getBlockState(crystalPos.down()).isOf(Blocks.OBSIDIAN) || mc.world.getBlockState(crystalPos.down()).isOf(Blocks.BEDROCK));

                if (canPlace) {
                    Vec3d vec3d = new Vec3d(crystalPos.getX(), crystalPos.getY(), crystalPos.getZ());
                    boxes.add(new Box(vec3d.x - 0.5, vec3d.y, vec3d.z - 0.5, vec3d.x + 1.5, vec3d.y + 2, vec3d.z + 1.5));
                }
            }
        }

        return boxes;
    }
}
