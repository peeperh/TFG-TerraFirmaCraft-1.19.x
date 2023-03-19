/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.fluids;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.ForgeFlowingFluid;

import net.dries007.tfc.mixin.accessor.FlowingFluidAccessor;

public abstract class MixingFluid extends ForgeFlowingFluid
{
    /**
     * @see net.minecraft.world.level.material.FlowingFluid#getCacheKey(BlockPos, BlockPos)
     */
    public static short getCacheKey(BlockPos from, BlockPos to)
    {
        int deltaX = to.getX() - from.getX();
        int deltaZ = to.getZ() - from.getZ();
        return (short) ((deltaX + 128 & 255) << 8 | deltaZ + 128 & 255);
    }

    protected MixingFluid(Properties properties)
    {
        super(properties);
    }

    /**
     * @param worldIn The world
     * @param pos     A position
     * @return The number of adjacent source blocks of this fluid
     * @see net.minecraft.world.level.material.FlowingFluid#sourceNeighborCount(LevelReader, BlockPos)
     */
    public int sourceNeighborCount(LevelReader worldIn, BlockPos pos)
    {
        int adjacentSources = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            BlockPos adjacentPos = pos.relative(direction);
            FluidState adjacentFluid = worldIn.getFluidState(adjacentPos);
            if (isSourceBlockOfThisType(adjacentFluid))
            {
                adjacentSources++;
            }
        }
        return adjacentSources;
    }

    /**
     * This is used for flow checks
     *
     * @param stateIn A fluid state
     * @return If the fluid state is a source block of this fluid
     * @see net.minecraft.world.level.material.FlowingFluid#isSourceBlockOfThisType(FluidState)
     */
    public boolean isSourceBlockOfThisType(FluidState stateIn)
    {
        return stateIn.getType().isSame(this) && stateIn.isSource();
    }

    /**
     * @see net.minecraft.world.level.material.FlowingFluid#spreadToSides(LevelAccessor, BlockPos, FluidState, BlockState)
     */
    public void spreadToSides(LevelAccessor world, BlockPos pos, FluidState fluidState, BlockState blockState)
    {
        int adjacentAmount = fluidState.getAmount() - getDropOff(world);
        if (fluidState.getValue(FALLING))
        {
            // Falling indicates this fluid is being fed from above - this is then going to spread like a source block (8 - drop off)
            adjacentAmount = 7;
        }
        if (adjacentAmount > 0)
        {
            // Calculate where the fluid should spread based on each direction
            Map<Direction, FluidState> map = getSpread(world, pos, blockState);
            for (Map.Entry<Direction, FluidState> entry : map.entrySet())
            {
                Direction direction = entry.getKey();
                FluidState fluidstate = entry.getValue();
                BlockPos blockpos = pos.relative(direction);
                BlockState blockstate = world.getBlockState(blockpos);
                if (canSpreadTo(world, pos, blockState, direction, blockpos, blockstate, world.getFluidState(blockpos), fluidstate.getType()))
                {
                    spreadTo(world, blockpos, blockstate, direction, fluidstate);
                }
            }
        }
    }

    @Override
    protected void spread(LevelAccessor worldIn, BlockPos pos, FluidState stateIn)
    {
        // Only spread if the current state has actual fluid
        if (!stateIn.isEmpty())
        {
            BlockState blockStateAt = worldIn.getBlockState(pos);
            BlockPos posBelow = pos.below();
            BlockState blockStateBelow = worldIn.getBlockState(posBelow);

            // First, try and flow downwards. Calculate a fluid state directly below this one
            FluidState fluidstate = getNewLiquid(worldIn, posBelow, blockStateBelow);

            // This checks if the block border is passable, and that the below fluid state returns true to being replaced with this fluid state
            if (canSpreadTo(worldIn, pos, blockStateAt, Direction.DOWN, posBelow, blockStateBelow, worldIn.getFluidState(posBelow), fluidstate.getType()))
            {
                // Try and spread directly below
                spreadTo(worldIn, posBelow, blockStateBelow, Direction.DOWN, fluidstate);

                // Count the number of adjacent blocks horizontally
                // A fluid (regardless of source vs. flowing) will always spread to the sides when there are three or more neighboring source blocks
                // This, notably, happens regardless of if the source blocks are passable
                if (this.sourceNeighborCount(worldIn, pos) >= 3)
                {
                    spreadToSides(worldIn, pos, stateIn, blockStateAt);
                }
            }
            else if (stateIn.isSource() || !((FlowingFluidAccessor) this).invoke$isWaterHole(worldIn, fluidstate.getType(), pos, blockStateAt, posBelow, blockStateBelow))
            {
                // Source blocks, if they can't spread downwards, will always spread sideways (this happens one tick after they spread downwards)
                // Flowing blocks will only spread sideways if they can't fall downwards, either to a water hole, or directly falling down (the above if chain)
                spreadToSides(worldIn, pos, stateIn, blockStateAt);
            }
        }
    }

    /**
     * Modified to use mixing mechanics, in such a way that can be exposed to other subclasses
     *
     * @see FluidHelpers#getNewFluidWithMixing(FlowingFluid, LevelReader, BlockPos, BlockState, boolean, int)
     */
    @Override
    protected FluidState getNewLiquid(LevelReader worldIn, BlockPos pos, BlockState blockStateIn)
    {
        return FluidHelpers.getNewFluidWithMixing(this, worldIn, pos, blockStateIn, canConvertToSource(), getDropOff(worldIn));
    }

    @Override
    public void tick(Level level, BlockPos pos, FluidState state)
    {
        if (!state.isSource())
        {
            // Flowing fluid ticks
            // Inside this statement, we know we're in a non-waterlogged block, as flowing fluids cannot be waterlogged.
            FluidState fluidAt = getNewLiquid(level, pos, level.getBlockState(pos));
            int spreadDelay = getSpreadDelay(level, pos, state, fluidAt);
            if (fluidAt.isEmpty())
            {
                // The current state should have no fluid in it - set the block to empty, and then call spread with an empty fluid (mojang why?)
                state = fluidAt;
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            else if (!fluidAt.equals(state)) // equals() is not overridden, so this is just a state object equality check
            {
                // The new fluid state is not the same as the existing one. In vanilla, this could happen if the state height was changed, or if it was converted into a source block.
                // In tfc, this may also happen when a fluid with a larger amount replaces this one.
                state = fluidAt;
                BlockState blockstate = fluidAt.createLegacyBlock();
                level.setBlock(pos, blockstate, 2);
                level.scheduleTick(pos, fluidAt.getType(), spreadDelay);
                level.updateNeighborsAt(pos, blockstate.getBlock());
            }
        }
        spread(level, pos, state);
    }

    public static class Flowing extends MixingFluid
    {
        public Flowing(Properties properties)
        {
            super(properties);
        }

        public boolean isSource(FluidState state)
        {
            return false;
        }

        public int getAmount(FluidState state)
        {
            return state.getValue(LEVEL);
        }

        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder)
        {
            super.createFluidStateDefinition(builder.add(LEVEL));
        }
    }

    public static class Source extends MixingFluid
    {
        public Source(Properties properties)
        {
            super(properties);
        }

        public boolean isSource(FluidState state)
        {
            return true;
        }

        public int getAmount(FluidState state)
        {
            return 8;
        }
    }
}
