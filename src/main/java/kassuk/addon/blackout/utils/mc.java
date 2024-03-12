package kassuk.addon.blackout.utils;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import kassuk.addon.blackout.mixins.MixinClientPlayerEntity;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.misc.input.KeyBinds;

public class mc {
    public static MinecraftClient mc;


    public static MixinClientPlayerEntity player;
}
