package kassuk.addon.blackout.utils.spotify;

import com.sun.jna.Platform;
import kassuk.addon.blackout.BlackOut;
import org.apache.commons.codec.digest.DigestUtils;

import javax.swing.*;

public class OSUtil {

    public static boolean isWindows = false;

    public static void init() {
        if (getOS().equals(OSType.Windows)) isWindows = true;
    }


    public static OSType getOS() {
        if (Platform.isWindows()) return OSType.Windows;
        if (Platform.isLinux()) return OSType.Linux;
        if (Platform.isMac()) return OSType.Mac;
        return OSType.Unsupported;

    }

    public enum OSType {
        Windows,
        Linux,
        Mac,
        Unsupported
    }

    public static void messageBox(String title, String msg, int type) {
        try { JOptionPane.showMessageDialog(null, msg, title, type); } catch (Exception ignored) {}
    }

    public static void invalidError() {
        String h = "error";
        try {
            h = DigestUtils.sha256Hex(System.getProperty("user.name") + java.net.InetAddress.getLocalHost().getHostName() + "cope_harder");
        } catch (Exception ignored) {}

    }

}
