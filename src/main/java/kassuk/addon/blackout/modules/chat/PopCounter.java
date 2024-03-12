package kassuk.addon.blackout.modules.chat;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.utils.Wrapper;
import kassuk.addon.blackout.utils.random.EzUtil;
import kassuk.addon.blackout.utils.random.Stats;
import kassuk.addon.blackout.utils.random.StringHelper;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.Script;
import meteordevelopment.starscript.compiler.Parser;
import meteordevelopment.starscript.compiler.Compiler;

import meteordevelopment.starscript.utils.StarscriptError;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Formatting;

import java.util.*;

public class PopCounter extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> own = sgGeneral.add(new BoolSetting.Builder().name("own").description("Notifies you of your own totem pops.").defaultValue(false).build());
    private final Setting<Boolean> friends = sgGeneral.add(new BoolSetting.Builder().name("friends").description("Notifies you of your friends totem pops.").defaultValue(true).build());
    private final Setting<Boolean> others = sgGeneral.add(new BoolSetting.Builder().name("others").description("Notifies you of other players totem pops.").defaultValue(true).build());

    private final Setting<Boolean> dontAnnounceFriends = sgGeneral.add(new BoolSetting.Builder().name("dont-announce-friends").description("Don't annnounce when your friends pop.").defaultValue(true).build());




    public final Object2IntMap<UUID> totemPops = new Object2IntOpenHashMap<>();
    private final Object2IntMap<UUID> chatIds = new Object2IntOpenHashMap<>();

    private static final Random RANDOM = new Random();
    private int updateWait = 45;
    public Script suffixScript;


    public PopCounter() {
        super(BlackOut.CHATPLUS, "pop-counter", "Count player's totem pops.");

    }
    private int announceWait;

    @Override
    public void onActivate() {
        EzUtil.updateTargets();
        totemPops.clear();
        chatIds.clear();
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        Stats.reset();
        totemPops.clear();
        chatIds.clear();
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket p)) return;

        if (p.getStatus() != 35) return;

        Entity entity = p.getEntity(mc.world);
        if (entity != null && ! (entity instanceof PlayerEntity)) return;
        if (entity == null
                || (entity.equals(mc.player) && !own.get())
                || (Friends.get().isFriend(((PlayerEntity) entity)) && !others.get())
                || (!Friends.get().isFriend(((PlayerEntity) entity)) && !friends.get())
        ) return;

        synchronized (totemPops) {
            int pops = totemPops.getOrDefault(entity.getUuid(), 0);
            totemPops.put(entity.getUuid(), ++pops);

            ChatUtils.sendMsg(getChatId(entity), Formatting.DARK_PURPLE, "(highlight)%s (default)popped (highlight)%d (default)%s.", entity.getEntityName(), pops, pops == 1 ? "totem" : "totems");
        }
            if (dontAnnounceFriends.get() && Friends.get().isFriend((PlayerEntity) entity)) return;
            try {



            } catch (StarscriptError error) {
                MeteorStarscript.printChatError(error);
            }
        }



    @EventHandler
    private void onTick(TickEvent.Post event) {
        updateWait--;
        if (updateWait <= 0) {
            EzUtil.updateTargets();
            updateWait = 45;
        }
        announceWait--;
        synchronized (totemPops) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (!totemPops.containsKey(player.getUuid())) continue;

                if (player.deathTime > 0 || player.getHealth() <= 0) {
                    int pops = totemPops.removeInt(player.getUuid());

                    ChatUtils.sendMsg(getChatId(player), Formatting.DARK_PURPLE, "(highlight)%s (default)popped (highlight)%d (default)%s.", player.getEntityName(), pops, pops == 1 ? "totem" : "totems");
                    chatIds.removeInt(player.getUuid());
                    if (EzUtil.currentTargets.contains(player.getEntityName())) EzUtil.sendAutoEz(player.getEntityName());
                }
            }
        }
    }

    private int getChatId(Entity entity) throws StarscriptError {
        return chatIds.computeIfAbsent(entity.getUuid(), value -> RANDOM.nextInt());

    }

    private static Script compile(String script) {
        if (script == null) return null;
        Parser.Result result = Parser.parse(script);
        if (result.hasErrors()) {
            MeteorStarscript.printChatError(result.errors.get(0));
            return null;
        }
        return Compiler.compile(result);
    }
}
