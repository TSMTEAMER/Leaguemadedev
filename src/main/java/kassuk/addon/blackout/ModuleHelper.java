// Decompiled with: CFR 0.152
// Class Version: 17
package kassuk.addon.blackout;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BlockPosSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnchantmentListSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.PotionSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.SoundEventListSetting;
import meteordevelopment.meteorclient.settings.StatusEffectAmplifierMapSetting;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.MyPotion;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import kassuk.addon.blackout.mixins.ISpoofName;

public class ModuleHelper
extends Module {
    protected final SettingGroup sgGeneral;

    public String title() {
        String spoofName = ((ISpoofName)((Object)this)).getSpoofName();
        return spoofName.isEmpty() ? this.title : spoofName;
    }

    public Setting<Boolean> setting(String name, String description, boolean defaultValue, String group) {
        return this.groupFromString(group).add(((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)new BoolSetting.Builder().name(name)).description(description)).defaultValue(defaultValue)).build());
    }

    public <T> Setting<T> setting(String name, String description, T defaultValue) {
        return this.setting(name, description, defaultValue, this.sgGeneral);
    }

    public <T> Setting<T> setting(String name, String description, T defaultValue, IVisible visible) {
        return this.setting(name, description, defaultValue, this.sgGeneral, visible, null, null);
    }

    public <T> Setting<T> setting(String name, String description, T defaultValue, Consumer<T> onChanged) {
        return this.setting(name, description, defaultValue, this.sgGeneral, null, onChanged);
    }

    public <T> Setting<T> setting(String name, String description, T defaultValue, IVisible visible, Consumer<T> onChanged) {
        return this.setting(name, description, defaultValue, this.sgGeneral, visible, onChanged, null);
    }

    public <T> Setting<T> setting(String name, String description, T defaultValue, IVisible visible, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated) {
        return this.setting(name, description, defaultValue, this.sgGeneral, visible, onChanged, onModuleActivated);
    }

    public <T> Setting<T> setting(String name, String description, T defaultValue, SettingGroup group) {
        return this.setting(name, description, defaultValue, group, null, null);
    }

    public <T> Setting<T> setting(String name, String description, T defaultValue, SettingGroup group, IVisible visible) {
        return this.setting(name, description, defaultValue, group, visible, null);
    }

    public <T> Setting<T> setting(String name, String description, T defaultValue, SettingGroup group, Consumer<T> onChanged) {
        return this.setting(name, description, defaultValue, group, null, onChanged);
    }

    public <T> Setting<T> setting(String name, String description, T defaultValue, SettingGroup group, IVisible visible, Consumer<T> onChanged) {
        return this.setting(name, description, defaultValue, group, visible, onChanged, null);
    }

    public <T> Setting<T> setting(String name, String description, T defaultValue, SettingGroup group, IVisible visible, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated) {
        if (defaultValue instanceof Enum) {
            Enum e8um = (Enum)defaultValue;
            if (e8um instanceof MyPotion) {
            }
            return group.add(new EnumSetting(name, description, (Enum)defaultValue, onChanged, onModuleActivated, visible));
        }
        if (defaultValue instanceof Boolean) {
        }
        if (defaultValue instanceof SettingColor) {
        }
        if (defaultValue instanceof Keybind) {
        }
        if (defaultValue instanceof String) {
        }
        if (defaultValue instanceof BlockPos) {
        }
        if (defaultValue instanceof Object2IntMap)
        if (defaultValue instanceof Integer) {
        }
        if (defaultValue instanceof Double) {
            return (Setting<T>) group.add(((DoubleSetting.Builder)((DoubleSetting.Builder)((DoubleSetting.Builder)new DoubleSetting.Builder().name(name)).description(description)).defaultValue((Double)defaultValue)).build());
        }
        return null;
    }

    @SafeVarargs
    public final <T> Setting<List<T>> setting(String name, String description, SettingGroup group, IVisible visible, T ... defaultValue) {
        return this.setting(name, description, group, visible, null, defaultValue);
    }

    @SafeVarargs
    public final <T> Setting<List<T>> setting(String name, String description, SettingGroup group, IVisible visible, Consumer<List<T>> onChanged, T ... defaultValue) {
        return this.setting(name, description, group, visible, onChanged, null, defaultValue);
    }

    public final <T> Setting<List<T>> setting(String name, String description, SettingGroup group, IVisible visible, Consumer<List<T>> onChanged, Consumer<Setting<List<T>>> onModuleActivated, T ... defaultValue) {
        if (defaultValue[0] instanceof String) {
        }
        if (defaultValue[0] instanceof Enchantment) {
        }
        if (defaultValue[0] instanceof Module) {
        }
        if (defaultValue[0] instanceof Block) {
        }
        if (defaultValue[0] instanceof SoundEvent) {
        }
        return null;
    }

    public Setting<Integer> setting(String name, String description, int defaultValue, SettingGroup group, int sliderMax) {
        return group.add(((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name(name)).description(description)).defaultValue(defaultValue)).sliderMax(sliderMax).build());
    }

    public <T> Setting<T> setting(String name, String description, T defaultValue, SettingGroup group, double sliderMin, double sliderMax, int min, int max) {
        return this.setting(name, description, defaultValue, group, null, null, null, sliderMin, sliderMax, min, max, 3);
    }

    public <T> Setting<T> setting(String name, String description, T defaultValue, SettingGroup group, IVisible visible, double sliderMax) {
        return this.setting(name, description, defaultValue, group, visible, null, null, 0.0, sliderMax, Integer.MIN_VALUE, Integer.MAX_VALUE, 3);
    }

    public <T> Setting<T> setting(String name, String description, T defaultValue, SettingGroup group, Consumer<T> onChanged, double sliderMax) {
        return this.setting(name, description, defaultValue, group, null, onChanged, null, 0.0, sliderMax, Integer.MIN_VALUE, Integer.MAX_VALUE, 3);
    }

    public <T> Setting<T> setting(String name, String description, T defaultValue, double sliderMin, double sliderMax) {
        return this.setting(name, description, defaultValue, this.sgGeneral, null, null, null, sliderMin, sliderMax, Integer.MIN_VALUE, Integer.MAX_VALUE, 3);
    }

    public <T> Setting<T> setting(String name, String description, T defaultValue, SettingGroup group, IVisible visible, double sliderMin, double sliderMax, int min, int max) {
        return this.setting(name, description, defaultValue, group, visible, null, null, sliderMin, sliderMax, min, max, 3);
    }

    public Setting<Double> setting(String name, String description, double defaultValue, SettingGroup group, double sliderMin, double sliderMax, int min, int max, int decimalPlaces) {
        return group.add(((DoubleSetting.Builder)((DoubleSetting.Builder)new DoubleSetting.Builder().name(name)).description(description)).defaultValue(defaultValue).sliderRange(sliderMin, sliderMax).range(min, max).decimalPlaces(decimalPlaces).build());
    }

    public <T> Setting<T> setting(String name, String description, T defaultValue, SettingGroup group, double sliderMin, double sliderMax) {
        return this.setting(name, description, defaultValue, group, null, null, null, sliderMin, sliderMax, Integer.MIN_VALUE, Integer.MAX_VALUE, 3);
    }

    public <T> Setting<T> setting(String name, String description, T defaultValue, SettingGroup group, IVisible visible, double sliderMin, double sliderMax) {
        return this.setting(name, description, defaultValue, group, visible, null, null, sliderMin, sliderMax, Integer.MIN_VALUE, Integer.MAX_VALUE, 3);
    }

    public Setting<Double> setting(String name, String description, double defaultValue, SettingGroup group, IVisible visible, double sliderMin, double sliderMax, int decimalPlaces) {
        return group.add(((DoubleSetting.Builder)((DoubleSetting.Builder)((DoubleSetting.Builder)new DoubleSetting.Builder().name(name)).description(description)).defaultValue(defaultValue).visible(visible)).sliderRange(sliderMin, sliderMax).decimalPlaces(decimalPlaces).build());
    }

    public Setting<Double> setting(String name, String description, double defaultValue, SettingGroup group, double sliderMin, double sliderMax, int decimalPlaces) {
        return group.add(((DoubleSetting.Builder)((DoubleSetting.Builder)new DoubleSetting.Builder().name(name)).description(description)).defaultValue(defaultValue).sliderRange(sliderMin, sliderMax).decimalPlaces(decimalPlaces).build());
    }

    public Setting<Double> setting(String name, String description, double defaultValue, SettingGroup group, int decimalPlaces) {
        return group.add(((DoubleSetting.Builder)((DoubleSetting.Builder)new DoubleSetting.Builder().name(name)).description(description)).defaultValue(defaultValue).decimalPlaces(decimalPlaces).build());
    }

    public <T> Setting<T> setting(String name, String description, T defaultValue, SettingGroup group, IVisible visible, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated, double sliderMin, double sliderMax, int min, int max, int decimalPlaces) {
        if (defaultValue instanceof Integer) {
        }
        return null;
    }

    public Setting<SettingColor> setting(String name, String description, int red, int green, int blue, int alpha) {
        return this.setting(name, description, red, green, blue, alpha, false, this.sgGeneral, null);
    }

    public Setting<SettingColor> setting(String name, String description, int red, int green, int blue, SettingGroup group) {
        return this.setting(name, description, red, green, blue, 255, false, group, null);
    }

    public Setting<SettingColor> setting(String name, String description, int red, int green, int blue, int alpha, SettingGroup group) {
        return this.setting(name, description, red, green, blue, alpha, false, group, null);
    }

    public Setting<SettingColor> setting(String name, String description, int red, int green, int blue, SettingGroup group, IVisible visible) {
        return this.setting(name, description, red, green, blue, 255, false, group, visible, null, null);
    }

    public Setting<SettingColor> setting(String name, String description, int red, int green, int blue, int alpha, SettingGroup group, IVisible visible) {
        return this.setting(name, description, red, green, blue, alpha, false, group, visible, null, null);
    }

    public Setting<SettingColor> setting(String name, String description, int red, int green, int blue, int alpha, boolean rainbow, SettingGroup group, IVisible visible) {
        return this.setting(name, description, red, green, blue, alpha, rainbow, group, visible, null, null);
    }

    public Setting<SettingColor> setting(String name, String description, int red, int green, int blue, SettingGroup group, IVisible visible, Consumer<SettingColor> onChanged) {
        return this.setting(name, description, red, green, blue, 255, false, group, visible, onChanged, null);
    }

    public Setting<SettingColor> setting(String name, String description, int red, int green, int blue, int alpha, SettingGroup group, IVisible visible, Consumer<SettingColor> onChanged) {
        return this.setting(name, description, red, green, blue, alpha, false, group, visible, onChanged, null);
    }

    public Setting<SettingColor> setting(String name, String description, int red, int green, int blue, int alpha, boolean rainbow, SettingGroup group, IVisible visible, Consumer<SettingColor> onChanged, Consumer<Setting<SettingColor>> onModuleActivated) {
        return group.add(new ColorSetting(name, description, new SettingColor(red, green, blue, alpha, rainbow), onChanged, onModuleActivated, visible));
    }



    public Setting<List<Item>> setting(String name, String description, SettingGroup group, boolean bypassFilterWhenSavingAndLoading, Predicate<Item> filter, IVisible visible, Consumer<List<Item>> onChanged, Consumer<Setting<List<Item>>> onModuleActivated, Item ... defaultValue) {
        return group.add(new ItemListSetting(name, description, Arrays.asList(defaultValue), onChanged, onModuleActivated, visible, filter, bypassFilterWhenSavingAndLoading));
    }

    public SettingGroup groupFromString(String group) {
        SettingGroup settingGroup = this.settings.getGroup(group);
        if (settingGroup == null) {
            return this.settings.createGroup(group);
        }
        return settingGroup;
    }

    public SettingGroup group(String name) {
        return this.settings.createGroup(name);
    }

    public void toggleWithInfo(String message, Object ... args) {
        this.info(message, args);
        this.toggle();
    }

    public void toggleWithInfo(int id, String message, Object ... args) {
        this.info(id, message, args);
        this.toggle();
    }

    public void toggleWithInfo(int id, Text message) {
        this.info(id, message);
        this.toggle();
    }

    public void toggleWithwarning(String message, Object ... args) {
        this.warning(message, args);
        this.toggle();
    }

    public void toggleWithwarning(int id, String message, Object ... args) {
        this.warning(id, message, args);
        this.toggle();
    }

    public void toggleWithError(Text message) {
        ChatUtils.sendMsg(0, this.title(), Formatting.RED, message);
        this.toggle();
    }

    public void toggleWithError(int id, String message, Object ... args) {
        this.error(id, message, args);
        this.toggle();
    }

    public void toggleWithError(int id, MutableText message) {
        this.error(id, message);
        this.toggle();
    }

    public void info(int id, String message, Object ... args) {
        ChatUtils.forceNextPrefixClass(this.getClass());
        ChatUtils.sendMsg(id, this.title(), Formatting.LIGHT_PURPLE, Formatting.GRAY, message, args);
    }

    public void info(int id, Text message) {
        ChatUtils.forceNextPrefixClass(this.getClass());
        ChatUtils.sendMsg(id, this.title(), Formatting.LIGHT_PURPLE, message);
    }

    public void warning(MutableText message) {
        ChatUtils.forceNextPrefixClass(this.getClass());
        ChatUtils.sendMsg(0, this.title(), Formatting.LIGHT_PURPLE, message.formatted(Formatting.YELLOW));
    }

    public void warning(int id, String message, Object ... args) {
        ChatUtils.forceNextPrefixClass(this.getClass());
        ChatUtils.sendMsg(id, this.title(), Formatting.LIGHT_PURPLE, Formatting.YELLOW, message, args);
    }

    public void error(int id, String message, Object ... args) {
        ChatUtils.forceNextPrefixClass(this.getClass());
        ChatUtils.sendMsg(id, this.title(), Formatting.LIGHT_PURPLE, Formatting.RED, message, args);
    }

    public void error(MutableText message) {
        ChatUtils.forceNextPrefixClass(this.getClass());
        ChatUtils.sendMsg(0, this.title(), Formatting.LIGHT_PURPLE, message.formatted(Formatting.RED));
    }

    public void error(int id, MutableText message) {
        ChatUtils.forceNextPrefixClass(this.getClass());
        ChatUtils.sendMsg(id, this.title(), Formatting.LIGHT_PURPLE, message.formatted(Formatting.RED));
    }

    public void info(MutableText message) {
        ChatUtils.forceNextPrefixClass(this.getClass());
        ChatUtils.sendMsg(this.title(), message.formatted(Formatting.GRAY));
    }

    public void info(String message, Object ... args) {
        ChatUtils.forceNextPrefixClass(this.getClass());
        ChatUtils.info(this.title(), message, args);
    }

    public void warning(String message, Object ... args) {
        ChatUtils.forceNextPrefixClass(this.getClass());
        ChatUtils.warning(this.title(), message, args);
    }

    public void error(String message, Object ... args) {
        ChatUtils.forceNextPrefixClass(this.getClass());
        ChatUtils.error(this.title(), message, args);
    }

    public ModuleHelper(Category category, String name, String description) {
        super(category, name, description);
        this.sgGeneral = this.settings.getDefaultGroup();
    }


}
