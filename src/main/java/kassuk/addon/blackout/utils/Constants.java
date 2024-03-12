package kassuk.addon.blackout.utils;

import it.unimi.dsi.fastutil.chars.Char2CharMap;
import it.unimi.dsi.fastutil.chars.Char2CharOpenHashMap;
import meteordevelopment.meteorclient.utils.PreInit;

public final class Constants {
    public static final float TICK = 1.0f / 20.0f;
    public static final Char2CharMap FANCY = new Char2CharOpenHashMap();

    @PreInit
    public static void init() {
        String[] a = "abcdefghijklmnoprstuvwz".split("");
        String[] b = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴩʀꜱᴛᴜᴠᴡᴢ".split("");
        for (int i = 0; i < a.length; i++) FANCY.put(a[i].charAt(0), b[i].charAt(0));
    }
}