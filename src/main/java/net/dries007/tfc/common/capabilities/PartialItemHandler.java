/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.capabilities;

import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * A partially exposed item handler, implementing the actual handler part of {@link SidedHandler} for {@link net.minecraftforge.items.IItemHandler}s
 * This allows selective slots to be set as valid to insert or extract. Any other operations are denied.
 */
public class PartialItemHandler implements DelegateItemHandler
{
    private final IItemHandlerModifiable internal;
    private final boolean[] insertSlots;
    private final boolean[] extractSlots;

    public PartialItemHandler(IItemHandlerModifiable internal)
    {
        this.internal = internal;
        this.insertSlots = new boolean[internal.getSlots()];
        this.extractSlots = new boolean[internal.getSlots()];
    }

    public PartialItemHandler insertAll()
    {
        Arrays.fill(insertSlots, true);
        return this;
    }

    public PartialItemHandler insert(int... slots)
    {
        for (int slot : slots)
        {
            insertSlots[slot] = true;
        }
        return this;
    }

    public PartialItemHandler extractAll()
    {
        Arrays.fill(extractSlots, true);
        return this;
    }

    public PartialItemHandler extract(int... slots)
    {
        for (int slot : slots)
        {
            extractSlots[slot] = true;
        }
        return this;
    }

    @Override
    public IItemHandlerModifiable getItemHandler()
    {
        return internal;
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        return insertSlots[slot] ? internal.insertItem(slot, stack, simulate) : stack;
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        return extractSlots[slot] ? internal.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
    }
}
