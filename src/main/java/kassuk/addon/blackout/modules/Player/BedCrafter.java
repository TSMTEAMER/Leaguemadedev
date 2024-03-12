/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package kassuk.addon.blackout.modules.Player;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.utils.bed.port.advance.CraftUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.recipebook.RecipeBookGroup;
import net.minecraft.item.BedItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;


import static kassuk.addon.blackout.utils.bed.port.basic.Vec3dInfo.closestVec3d;
import static kassuk.addon.blackout.utils.bed.port.basic.EntityInfo.isMoving;
import static kassuk.addon.blackout.utils.bed.port.basic.EntityInfo.isInHole;
import static kassuk.addon.blackout.utils.bed.port.basic.BlockInfo.getSphere;
import static kassuk.addon.blackout.utils.bed.port.basic.BlockInfo.getBlock;
import static kassuk.addon.blackout.utils.bed.port.advance.CraftUtils.*;
import static kassuk.addon.blackout.utils.bed.port.advance.CraftUtils.getEmptySlots;

public class BedCrafter extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAuto = settings.createGroup("Auto (Buggy)");

    private final Setting<Boolean> disableAfter = sgGeneral.add(new BoolSetting.Builder().name("disable-after").description("Toggle off after filling your inv with beds.").defaultValue(false).build());
    private final Setting<Boolean> disableNoMats = sgGeneral.add(new BoolSetting.Builder().name("disable-on-no-mats").description("Toggle off if you run out of material.").defaultValue(false).build());
    private final Setting<Boolean> closeAfter = sgGeneral.add(new BoolSetting.Builder().name("close-after").description("Close the crafting GUI after filling.").defaultValue(true).build());
    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder().name("table-place-delay").description("Delay between placing crafting tables.").defaultValue(3).min(1).sliderMax(10).build());
    private final Setting<Integer> openDelay = sgGeneral.add(new IntSetting.Builder().name("table-open-delay").description("Delay between opening crafting tables.").defaultValue(3).min(1).sliderMax(10).build());

    private final Setting<Boolean> automatic = sgAuto.add(new BoolSetting.Builder().name("automatic").description("Automatically place/search for and open crafting tables when you're out of beds.").defaultValue(false).build());
    private final Setting<Boolean> antiTotemFail = sgAuto.add(new BoolSetting.Builder().name("anti-totem-fail").description("Will not open / close current crafting table if you don't have a totem.").defaultValue(false).build());
    private final Setting<Boolean> antiDesync = sgAuto.add(new BoolSetting.Builder().name("anti-desync").description("Try to prevent inventory desync.").defaultValue(false).build());
    private final Setting<Boolean> autoOnlyHole = sgAuto.add(new BoolSetting.Builder().name("in-hole-only").description("Only auto refill while in a hole.").defaultValue(false).build());
    private final Setting<Boolean> autoOnlyGround = sgAuto.add(new BoolSetting.Builder().name("on-ground-only").description("Only auto refill while on the ground.").defaultValue(false).build());
    private final Setting<Boolean> autoWhileMoving = sgAuto.add(new BoolSetting.Builder().name("while-moving").description("Allow auto refill while in motion").defaultValue(false).build());
    private final Setting<Integer> refillAt = sgAuto.add(new IntSetting.Builder().name("refill-at").description("How many beds are left in your inventory to start filling.").defaultValue(3).min(1).build());
    private final Setting<Integer> emptySlotsNeeded = sgAuto.add(new IntSetting.Builder().name("required-empty-slots").description("How many empty slots are required for activation.").defaultValue(5).min(1).build());
    private final Setting<Integer> radius = sgAuto.add(new IntSetting.Builder().name("radius").description("How far to search for crafting tables near you.").defaultValue(3).min(1).build());
    private final Setting<Double> minHealth = sgAuto.add(new DoubleSetting.Builder().name("min-health").description("Min health require to activate.").defaultValue(10).min(1).max(36).sliderMax(36).build());

    public BedCrafter() {
        super(BlackOut.PLAYERPLUS, "bed-crafter", "Automatically craft beds.");
    }

    private boolean didRefill = false;
    private boolean startedRefill = false;
    private boolean alertedNoMats = false;

    private int placeTimer, openTimer;

    @Override
    public void onActivate() {
        placeTimer = placeDelay.get();
        openTimer = openDelay.get();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (PlayerUtils.getTotalHealth() <= minHealth.get()) {
            closeCraftingTable();
            return;
        }

        if (willTotemFail()) {
            closeCraftingTable();
            return;
        }

        if (automatic.get() && isOutOfMaterial() && !alertedNoMats) {
            alertedNoMats = true;
        }

        if (automatic.get() && needsRefill() && canRefill(true) && !isOutOfMaterial() && !(mc.player.currentScreenHandler instanceof CraftingScreenHandler)) {
            FindItemResult craftTable = CraftUtils.findCraftTable();
            if (!craftTable.found()) {
                toggle();
                return;
            }
            BlockPos tablePos;
            tablePos = findCraftingTable();
            if (tablePos == null) {
                if (placeTimer <= 0) {
                    placeTimer = placeDelay.get();
                } else {
                    placeTimer--;
                    return;
                }
                placeCraftingTable(craftTable);
                return;
            }
            if (openTimer <= 0) {
                openCraftingTable(tablePos);
                openTimer = openDelay.get();
            } else {
                openTimer--;
                return;
            }
            didRefill = true;
            return;
        }

        if (didRefill && !needsRefill()) {
            didRefill = false;
            startedRefill = false;
        }

        if (mc.player.currentScreenHandler instanceof CraftingScreenHandler currentScreenHandler) {
            if (mc.player.getRecipeBook() != null && !mc.player.getRecipeBook().isGuiOpen(RecipeBookCategory.CRAFTING)) {
                mc.player.getRecipeBook().setGuiOpen(RecipeBookCategory.CRAFTING, true);
            }

            if (PlayerUtils.getTotalHealth() <= minHealth.get() || willTotemFail()) {
                closeCraftingTable();
                return;
            }
            if (!canRefill(false)) {
                closeCraftingTable();
                if (antiDesync.get()) mc.player.getInventory().updateItems();
                return;
            }
            if (isOutOfMaterial()) {
                if (disableNoMats.get()) toggle();
                closeCraftingTable();
                if (antiDesync.get()) mc.player.getInventory().updateItems();
                return;
            }
            if (isInventoryFull()) {
                if (disableAfter.get()) toggle();
                if (closeAfter.get()) {
                    closeCraftingTable();
                    if (antiDesync.get()) mc.player.getInventory().updateItems();
                }
                return;
            }
            List<RecipeResultCollection> recipeResultCollectionList = mc.player.getRecipeBook().getResultsForGroup(RecipeBookGroup.CRAFTING_MISC);

            for (RecipeResultCollection recipeResultCollection : recipeResultCollectionList) {
                for (Recipe<?> recipe : recipeResultCollection.getRecipes(true)) {
                    if (recipe.getOutput(mc.world.getRegistryManager()).getItem() instanceof BedItem) {
                        for (int i = 0; i < getEmptySlots(); i++) {
                            if (recipe.isEmpty())  break;
                            assert mc.interactionManager != null;
                            mc.interactionManager.clickRecipe(currentScreenHandler.syncId, recipe, false);
                            windowClick(currentScreenHandler, 0, SlotActionType.QUICK_MOVE, 1);
                        }
                    }
                }
            }
            if (isInventoryFull()) closeCraftingTable();
        }
    }

    private void placeCraftingTable(FindItemResult craftTable) {
        List<BlockPos> nearbyBlocks = getSphere(mc.player.getBlockPos(), radius.get(), radius.get());
        for (BlockPos block : nearbyBlocks) {
            if (getBlock(block) == Blocks.AIR && BlockUtils.canPlace(block, true)) {
                BlockUtils.place(block, craftTable, 0, true);
                break;
            }
        }
    }

    private BlockPos findCraftingTable() {
        List<BlockPos> nearbyBlocks = getSphere(mc.player.getBlockPos(), radius.get(), radius.get());
        for (BlockPos block : nearbyBlocks) if (getBlock(block) == Blocks.CRAFTING_TABLE) return block;
        return null;
    }

    private void openCraftingTable(BlockPos tablePos) {
        Vec3d tableVec = closestVec3d(tablePos);
        BlockHitResult table = new BlockHitResult(tableVec, Direction.UP, tablePos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, table);
    }

    private void closeCraftingTable() {
        if (mc.player.currentScreenHandler instanceof CraftingScreenHandler) mc.player.closeHandledScreen();
    }

    private boolean needsRefill() {
        FindItemResult bed = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
        if (!bed.found()) return true;
        if (bed.count() <= refillAt.get()) return true;
        return !isInventoryFull();
    }

    private boolean canRefill(boolean checkSlots) {
        if (!autoWhileMoving.get() && isMoving(mc.player)) return false;
        if (autoOnlyHole.get() && !isInHole(mc.player)) return false;
        if (autoOnlyGround.get() && !mc.player.isOnGround()) return false;
        if (isInventoryFull()) return false;
        if (checkSlots) if (getEmptySlots() < emptySlotsNeeded.get()) return false;
        return !(PlayerUtils.getTotalHealth() <= minHealth.get());
    }

    private boolean isOutOfMaterial() {
        FindItemResult wool = InvUtils.find(itemStack -> CraftUtils.wools.contains(itemStack.getItem()));
        FindItemResult plank = InvUtils.find(itemStack -> CraftUtils.planks.contains(itemStack.getItem()));
        FindItemResult craftTable = CraftUtils.findCraftTable();
        if (!craftTable.found()) return true;
        if (!wool.found() || !plank.found()) return true;
        return wool.count() < 3 || plank.count() < 3;
    }

    private boolean willTotemFail() {
        if (!antiTotemFail.get()) return false;
        Item offhand = mc.player.getOffHandStack().getItem();
        if (offhand == null) return true;
        return offhand != Items.TOTEM_OF_UNDYING;
    }

    private void windowClick(ScreenHandler container, int slot, SlotActionType action, int clickData) {
        assert mc.interactionManager != null;
        mc.interactionManager.clickSlot(container.syncId, slot, clickData, action, mc.player);
    }
}
