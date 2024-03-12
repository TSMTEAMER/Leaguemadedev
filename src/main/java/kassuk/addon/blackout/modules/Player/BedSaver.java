package kassuk.addon.blackout.modules.Player;

import kassuk.addon.blackout.BlackOut;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BedBlock;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.math.BlockPos;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BedSaver extends Module {
    public BedSaver() {
        super(BlackOut.PLAYERPLUS, "bed-saver", "Saves bed locations to a file (\"beds.txt\") everytime you right click one.");
    }

    private static void writeToFile(String info) {
        try (FileWriter fw = new FileWriter("beds.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            out.println(info + " on " + dtf.format(now) + " on server: " + Utils.getWorldName());
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof PlayerInteractBlockC2SPacket && PlayerUtils.getDimension() == Dimension.Overworld) {
            BlockPos blockPos = ((PlayerInteractBlockC2SPacket) event.packet).getBlockHitResult().getBlockPos();
            if (mc.world.getBlockState(blockPos).getBlock() instanceof BedBlock) {
                writeToFile(blockPos.getX() + " " + blockPos.getY() + " " + blockPos.getZ());
            }
        }
    }
}
