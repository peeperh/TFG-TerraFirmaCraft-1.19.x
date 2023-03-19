/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.container;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class CallbackSlot extends SlotItemHandler
{
    private final ISlotCallback callback;

    public CallbackSlot(ISlotCallback callback, IItemHandler inventory, int index, int x, int y)
    {
        super(inventory, index, x, y);

        this.callback = callback;
    }

    @Override
    public void onTake(Player player, ItemStack stack)
    {
        callback.onSlotTake(player, getSlotIndex(), stack);
        super.onTake(player, stack);
    }

    @Override
    public void setChanged()
    {
        callback.setAndUpdateSlots(getSlotIndex());
        super.setChanged();
    }

    @Override
    public boolean mayPlace(ItemStack stack)
    {
        return callback.isItemValid(getSlotIndex(), stack) && super.mayPlace(stack);
    }

    @Override
    public int getMaxStackSize()
    {
        return Math.min(callback.getSlotStackLimit(getSlotIndex()), super.getMaxStackSize());
    }
}
