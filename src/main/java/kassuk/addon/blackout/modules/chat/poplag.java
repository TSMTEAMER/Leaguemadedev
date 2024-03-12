package kassuk.addon.blackout.modules.chat;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.BlackOutModule;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
public class poplag extends BlackOutModule {
    public poplag() {
        super(BlackOut.CHATPLUS, "Poplag", "straight outta earth");
    }

    private final SettingGroup sgPop = settings.createGroup("Pop");


    private final Setting<Boolean> pop = sgPop.add(new BoolSetting.Builder()
        .name("Pop")
        .description("Should we send a message when enemy pops a totem")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> tickDelay = sgPop.add(new IntSetting.Builder()
        .name("Delay")
        .description("How many ticks to wait between sending messages.")
        .defaultValue(50)
        .min(0)
        .sliderRange(0, 100)
        .build()
    );

    private final Setting<List<String>> popMessages = sgPop.add(new StringListSetting.Builder()
        .name("Pop Messages")
        .description("Messages to send when popping an enemy")
        .defaultValue(List.of("/msg <NAME> āȁ́ Ё܁ࠁँਁଁก༁ခᄁሁጁᐁᔁᘁᜁ᠁ᤁᨁᬁᰁ܁⼁、㈁㌁㐁㔁㘁㜁㠁㤁㨁㬁㰁㴁㸁㼁䀁䄁䈁䌁䐁䔁䘁䜁䠁䤁䨁䬁䰁䴁丁企倁儁刁匁吁唁嘁圁堁夁威嬁封崁币弁态愁戁持搁攁昁朁栁椁樁欁氁洁渁漁瀁焁爁猁琁甁瘁省码礁稁笁簁紁縁缁老脁舁茁萁蔁蘁蜁蠁褁訁謁谁贁踁輁送鄁鈁錁鐁锁阁霁頁餁騁鬁鰁鴁鸁鼁ꀁꄁꈁꌁꐁꔁꘁ꜁ꠁ꤁ꨁꬁ각괁긁꼁뀁넁눁댁됁딁똁뜁렁뤁먁묁밁봁"))
        .build()
    );
    private final Setting<MessageMode> fixpop = sgPop.add(new EnumSetting.Builder<MessageMode>()
        .name("lag")
        .description("test to fix the pop not popping niggas")
        .defaultValue(MessageMode.Lagger)
        .build()
    );
    private final Setting<Double> range = sgPop.add(new DoubleSetting.Builder()
        .name("Enemy Range")
        .description("Only send message if enemy died inside this range.")
        .defaultValue(25)
        .min(0)
        .sliderRange(0, 50)
        .build()
    );

    private final Random r = new Random();
    private int lastNum;
    private int lastPop;
    private boolean lastState;
    private String name = null;
    private final List<Message> messageQueue = new LinkedList<>();
    private int timer = 0;

    private final String[] Lagger = new String[]{
        "āȁ́ Ё܁ࠁँਁଁก༁ခᄁሁጁᐁᔁᘁᜁ᠁ᤁᨁᬁᰁ܁⼁、㈁㌁㐁㔁㘁㜁㠁㤁㨁㬁㰁㴁㸁㼁䀁䄁䈁䌁䐁䔁䘁䜁䠁䤁䨁䬁䰁䴁丁企倁儁刁匁吁唁嘁圁堁夁威嬁封崁币",
        "弁态愁戁持搁攁昁朁栁椁樁欁氁洁渁漁瀁焁爁猁琁甁瘁省码礁稁笁簁紁縁缁老脁舁茁萁蔁蘁蜁蠁褁訁",
        "謁谁贁踁輁送鄁鈁錁鐁锁阁霁頁餁騁鬁鰁鴁鸁鼁ꀁꄁꈁꌁꐁꔁꘁ꜁ꠁ꤁ꨁꬁ각괁긁꼁뀁넁눁댁됁딁똁뜁렁뤁먁묁밁봁"

    };

    @Override
    public void onActivate() {
        super.onActivate();
        lastState = false;
        lastNum = -1;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Pre event) {
        timer++;
        if (mc.player != null && mc.world != null) {
            if (anyDead(range.get()) && pop.get()) {
                if (!lastState) {
                    lastState = true;
                    sendPopMessage();
                }
            } else lastState = false;

            if (timer >= tickDelay.get() && !messageQueue.isEmpty()) {
                poplag.Message msg = messageQueue.get(0);
                ChatUtils.sendPlayerMsg(msg.message);
                timer = 0;

                if (msg.kill) messageQueue.clear();
                else messageQueue.remove(0);
            }
        }
    }



    @SuppressWarnings("DataFlowIssue")
    private boolean anyDead(double range) {
        for (PlayerEntity pl : mc.world.getPlayers()) {
            if (pl != mc.player && !Friends.get().isFriend(pl) && pl.getPos().distanceTo(mc.player.getPos()) <= range
                && pl.getHealth() <= 0) {
                name = pl.getName().getString();
                return true;
            }
        }
        return false;
    }
    @EventHandler
    private void onReceive(PacketEvent.Receive event) {
        if (event.packet instanceof EntityStatusS2CPacket packet) {
            // Pop
            if (packet.getStatus() == 35) {
                Entity entity = packet.getEntity(mc.world);
                if (pop.get() && mc.player != null && mc.world != null && entity instanceof PlayerEntity) {
                    if (entity != mc.player && !Friends.get().isFriend((PlayerEntity) entity) &&
                        mc.player.getPos().distanceTo(entity.getPos()) <= range.get()) {
                        sendPopMessage(entity.getName().getString());
                    }
                }
            }
        }
    }
    private void sendPopMessage() {
        if (popMessages.get().equals(Lagger)) {
            int num = r.nextInt(0, Lagger.length);
            if (num == lastNum) {
                num = num < Lagger.length - 1 ? num + 1 : 0;
            }
            lastNum = num;
            messageQueue.add(0, new Message(Lagger[num].replace("%s", name == null ? "You" : name), true));
        }
    }

    private void sendPopMessage(String name) {
        if (!popMessages.get().isEmpty()) {
            int num = r.nextInt(0, popMessages.get().size() - 1);
            if (num == lastPop) {
                num = num < popMessages.get().size() - 1 ? num + 1 : 0;
            }
            lastPop = num;
            messageQueue.add(new Message(popMessages.get().get(num).replace("<NAME>", name), false));
        }
    }
        private record Message(String message, boolean kill) {
        }
    public enum MessageMode {

        Lagger
    }
    }

