/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.capabilities;

import java.util.function.Predicate;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

/**
 * A fluid handler implementation for an {@link ItemStack} which has one internal tank, capable of holding either 0, or a specific amount of a fluid.
 *
 * @see ItemStackFluidHandler for a continous valued variation.
 */
public class DiscreteItemStackFluidHandler extends ItemStackFluidHandler
{
    public DiscreteItemStackFluidHandler(ItemStack stack, TagKey<Fluid> allowedFluids, int capacity)
    {
        super(stack, allowedFluids, capacity);
    }

    public DiscreteItemStackFluidHandler(ItemStack stack, Predicate<Fluid> allowedFluids, int capacity)
    {
        super(stack, allowedFluids, capacity);
    }

    @Override
    public int fill(FluidStack fill, FluidAction action)
    {
        if (fluid.isEmpty() && fill.getAmount() >= capacity && isFluidValid(0, fill))
        {
            if (action.execute())
            {
                fluid = fill.copy();
                fluid.setAmount(capacity);
                save();
            }
            return capacity;
        }
        return 0;
    }

    @NotNull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action)
    {
        // Like buckets, we avoid draining unless we're asked for at least 1000, to avoid either returning > maxDrain, or losing fluid.
        // The handlers, i.e. in FluidHelpers, are designed to be able to work with this type of restriction and implement the lossy transfers there.
        if (!fluid.isEmpty() && maxDrain >= capacity)
        {
            final FluidStack result = fluid.copy();
            if (action.execute())
            {
                fluid = FluidStack.EMPTY;
                save();
            }
            return result;
        }
        return FluidStack.EMPTY;
    }
}
