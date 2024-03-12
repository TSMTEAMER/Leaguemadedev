package kassuk.addon.blackout.utils;

import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.starscript.Script;
import meteordevelopment.starscript.compiler.Compiler;
import meteordevelopment.starscript.compiler.Parser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class StarscriptUtils {
    public static Script compile(String message) {
        Parser.Result result = Parser.parse(message);

        if (result.hasErrors() && Utils.canUpdate()) {
            MeteorStarscript.printChatError(result.errors.get(0));
            return null;
        }

        return Compiler.compile(result);
    }
}