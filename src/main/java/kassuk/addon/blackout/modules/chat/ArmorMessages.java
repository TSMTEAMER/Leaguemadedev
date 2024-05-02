package kassuk.addon.blackout.modules.chat;

import kassuk.addon.blackout.BlackOut;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ArmorMessages extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> durThreshold = sgGeneral.add(new IntSetting.Builder()
            .name("durability-threshold")
            .description("The minimum durability to alert.")
            .defaultValue(20)
            .range(1,99)
            .sliderRange(1,99)
            .build()
    );

    private final Setting<Boolean> alertSelf = sgGeneral.add(new BoolSetting.Builder()
            .name("alert-self")
            .description("Whether to alert yourself.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> alertFriend = sgGeneral.add(new BoolSetting.Builder()
            .name("alert-friend")
            .description("Whether to alert friend.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("The delay between messages in ticks.")
            .defaultValue(20)
            .min(0)
            .sliderMax(200)
            .visible(alertFriend::get)
            .build()
    );

    public ArmorMessages() {
        super(BlackOut.CHATPLUS, "armor-alert", "Send alerts to people with low armor.");
    }


    private final List<String> messages = new ArrayList<>();
    private final List<PlayerEntity> helmet = new ArrayList<>();
    private final List<PlayerEntity> chestplate = new ArrayList<>();
    private final List<PlayerEntity> pants = new ArrayList<>();
    private final List<PlayerEntity> boots = new ArrayList<>();

    private int messageI, timer;


    @Override
    public void onActivate() {
        timer = 0;
        helmet.clear();
        chestplate.clear();
        pants.clear();
        boots.clear();

    }

    @Override
    public void onDeactivate() {
        helmet.clear();
        chestplate.clear();
        pants.clear();
        boots.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {

        for (PlayerEntity player : mc.world.getPlayers()) {

            if (player == mc.player && alertSelf.get()){
                check(player);
            }
            else if (Friends.get().isFriend(player) && alertFriend.get()){
                check(player);
            }
        }

        if (!messages.isEmpty()){
            if (timer <= 0) {
                int i;

                if (messageI >= messages.size()) messageI = 0;
                i = messageI++;

                ChatUtils.sendPlayerMsg(messages.get(i));
                messages.remove(i);

                timer = delay.get();
            } else {
                timer--;
            }
        }
    }

    public void check(PlayerEntity player) {
        Iterable<ItemStack> armorPieces = player.getArmorItems();

        for (ItemStack armorPiece : armorPieces) {

            if (checkDur(armorPiece)) {
                if (armorPiece == player.getInventory().getArmorStack(3) && player.getInventory().getArmorStack(3) != null && !helmet.contains(player)) {
                    sendMsg("helmet", player);
                    helmet.add(player);
                }
                if (armorPiece == player.getInventory().getArmorStack(2) && player.getInventory().getArmorStack(2) != null && !chestplate.contains(player)) {
                    if (mc.player.getInventory().getArmorStack(2).getItem() == Items.ELYTRA){
                        sendMsg("elytra", player);
                    }
                    else {
                        sendMsg("chestplate", player);
                    }
                    chestplate.add(player);
                }
                if (armorPiece == player.getInventory().getArmorStack(1) && player.getInventory().getArmorStack(1) != null && !pants.contains(player)) {
                    sendMsg("leggings", player);
                    pants.add(player);
                }
                if (armorPiece == player.getInventory().getArmorStack(0) && player.getInventory().getArmorStack(0) != null && !boots.contains(player)) {
                    sendMsg("boots", player);
                    boots.add(player);
                }
            }
            if (!checkDur(armorPiece)) {
                if (armorPiece == player.getInventory().getArmorStack(3)) helmet.remove(player);
                if (armorPiece == player.getInventory().getArmorStack(2)) chestplate.remove(player);
                if (armorPiece == player.getInventory().getArmorStack(1)) pants.remove(player);
                if (armorPiece == player.getInventory().getArmorStack(0)) boots.remove(player);
            }
        }
    }

    private boolean checkDur(ItemStack i){
        return (float) (i.getMaxDamage() - i.getDamage()) / i.getMaxDamage() * 100 <= durThreshold.get();
    }

    private void sendMsg(String armor, PlayerEntity player) {

        String grammar;
        if (armor.endsWith("s")) grammar = " are low!"; else grammar = " is low!";

        if (player == mc.player){
            info(" Your " + armor + grammar);
        }
        else {
            messages.add("/msg " + player.getName() + " Your " + armor + grammar);
        }
    }
}
