/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import net.dries007.tfc.common.fluids.BucketPickupExtension;
import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.common.fluids.TFCFluids;
import net.dries007.tfc.world.river.Flow;

public class RiverWaterBlock extends LiquidBlock implements BucketPickupExtension
{
    public static final EnumProperty<Flow> FLOW = TFCBlockStateProperties.FLOW;

    public RiverWaterBlock(Properties properties)
    {
        super(TFCFluids.RIVER_WATER, properties);

        registerDefaultState(getStateDefinition().any().setValue(LEVEL, 0).setValue(FLOW, Flow.NONE));
    }

    @Override
    public FluidStack pickupBlock(LevelAccessor level, BlockPos pos, BlockState state, IFluidHandler.FluidAction action)
    {
        if (state.getValue(LEVEL) == 0)
        {
            if (action.execute())
            {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
            }
            // Always pickup into water
            return new FluidStack(Fluids.WATER, FluidHelpers.BUCKET_VOLUME);
        }
        else
        {
            return FluidStack.EMPTY;
        }
    }

    @Override
    public ItemStack pickupBlock(LevelAccessor level, BlockPos pos, BlockState state)
    {
        return new ItemStack(pickupBlock(level, pos, state, IFluidHandler.FluidAction.EXECUTE).getFluid().getBucket());
    }

    @Override
    public FluidState getFluidState(BlockState state)
    {
        return getFluid().defaultFluidState().setValue(FLOW, state.getValue(FLOW));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder.add(FLOW));
    }

    @Override
    protected synchronized void initFluidStateCache()
    {
        // No-op
    }
}
