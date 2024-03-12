package kassuk.addon.blackout.mixins;

import kassuk.addon.blackout.modules.chat.ChatConfig;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatUtils.class)
public class ChatUtilsMixin {
    @Inject(method = "getPrefix", at = @At("HEAD"), cancellable = true, remap = false)
    private static void getPrefix(CallbackInfoReturnable<Text> cir) {
        if (!Modules.get().get(ChatConfig.class).isActive()) return;
        if (Modules.get().get(ChatConfig.class).mode.get() != ChatConfig.Mode.League) {
            MutableText PREFIX = Text.literal(Modules.get().get(ChatConfig.class).mode.get() == ChatConfig.Mode.Clear ? "" : Modules.get().get(ChatConfig.class).text.get());
            MutableText prefix = Text.literal("");
            PREFIX.setStyle(PREFIX.getStyle().withFormatting(Formatting.DARK_PURPLE));
            prefix.setStyle(prefix.getStyle().withFormatting(Formatting.DARK_PURPLE));
            prefix.append(Modules.get().get(ChatConfig.class).mode.get() == ChatConfig.Mode.Clear ? "" : "[");
            prefix.append(PREFIX);
            prefix.append(Modules.get().get(ChatConfig.class).mode.get() == ChatConfig.Mode.Clear ? "" : "] ");
            cir.setReturnValue(prefix);
        }
    }
}
