// Decompiled with: CFR 0.152
// Class Version: 17
package kassuk.addon.blackout.utils.vh;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;

public interface IBlockItem {
    public boolean canBePlaced(ItemPlacementContext var1, BlockState var2);

    public BlockState getThePlacementState(ItemPlacementContext var1);
}
