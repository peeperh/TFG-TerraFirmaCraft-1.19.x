/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blocks;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.HitResult;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.util.EnvironmentHelpers;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.OverworldClimateModel;

public class IcePileBlock extends IceBlock implements IForgeBlockExtension, EntityBlockExtension
{
    public static void placeIcePileOrIce(LevelAccessor level, BlockPos groundPos, BlockState groundState, boolean skipEdgeCheck)
    {
        // Don't just check for water, check for water with plants that can freeze as well
        final FluidState fluid = groundState.getFluidState();
        final boolean icePileAtGround = Helpers.isBlock(groundState.getBlock(), TFCTags.Blocks.CAN_BE_ICE_PILED);
        if (fluid.getType() == Fluids.WATER && (icePileAtGround || groundState.getBlock() == Blocks.WATER) && (skipEdgeCheck || EnvironmentHelpers.isAdjacentToNotWater(level, groundPos)))
        {
            // Freeze block, handling possible plants *in* the block, and *above* the block
            final BlockPos surfacePos = groundPos.above();
            final BlockState surfaceState = level.getBlockState(surfacePos);
            final boolean icePileAtSurface = Helpers.isBlock(surfaceState.getBlock(), TFCTags.Blocks.CAN_BE_ICE_PILED);
            if (icePileAtGround || icePileAtSurface)
            {
                // Requires an ice pile
                final BlockState savedGroundState = icePileAtGround ? groundState : Blocks.WATER.defaultBlockState();
                final BlockState savedSurfaceState = icePileAtSurface ? surfaceState : null;

                level.setBlock(groundPos, TFCBlocks.ICE_PILE.get().defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                level.getBlockEntity(groundPos, TFCBlockEntities.PILE.get()).ifPresent(pile -> pile.setHiddenStates(savedGroundState, savedSurfaceState, false));

                // Remove the block above, if it's been absorbed into the ice pile
                if (icePileAtSurface)
                {
                    Helpers.removeBlock(level, surfacePos, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }

                // Block updates after we have removed both blocks
                level.blockUpdated(groundPos, TFCBlocks.ICE_PILE.get());
                if (icePileAtSurface)
                {
                    level.blockUpdated(surfacePos, Blocks.AIR);
                }
            }
            else
            {
                // No ice pile required
                level.setBlock(groundPos, Blocks.ICE.defaultBlockState(), 3);
            }
        }
    }

    public static void removeIcePileOrIce(LevelAccessor level, BlockPos pos, BlockState state)
    {
        final boolean ultrawarm = level.dimensionType().ultraWarm();
        final Material material = level.getBlockState(pos.below()).getMaterial();
        if (!ultrawarm && (material.blocksMotion() || material.isLiquid()))
        {
            if (state.getBlock() == Blocks.ICE)
            {
                // Just place water
                level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
            }
            else
            {
                level.getBlockEntity(pos, TFCBlockEntities.PILE.get()).ifPresent(pile -> {
                    if (!level.isClientSide())
                    {
                        level.setBlock(pos, pile.getInternalState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        if (pile.getAboveState() != null)
                        {
                            level.setBlock(pos.above(), pile.getAboveState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        }

                        // Block ticks after both blocks are placed
                        level.blockUpdated(pos, pile.getInternalState().getBlock());
                        level.scheduleTick(pos, Fluids.WATER, 1);
                        if (pile.getAboveState() != null)
                        {
                            level.blockUpdated(pos.above(), pile.getAboveState().getBlock());
                        }
                    }
                });
            }
        }
        else if (state.getBlock() == Blocks.ICE || state.getBlock() == TFCBlocks.ICE_PILE.get())
        {
            level.removeBlock(pos, false);
        }
    }

    private final ExtendedProperties properties;

    public IcePileBlock(ExtendedProperties properties)
    {
        super(properties.properties());
        this.properties = properties;
    }

    @Override
    public ExtendedProperties getExtendedProperties()
    {
        return properties;
    }

    /**
     * When destroyed, replace with the hidden blocks, and schedule ticks for them
     */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid)
    {
        playerWillDestroy(level, pos, state, player);
        removeIcePileOrIce(level, pos, state);
        return false; // Don't remove the block
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, Random random)
    {
        // Heavily reduced chance, as most snow melting happens through EnvironmentHelpers, this is only really to account for overhangs and hidden snow
        if (level.getRandom().nextInt(EnvironmentHelpers.ICE_MELT_RANDOM_TICK_CHANCE) == 0 && Climate.getTemperature(level, pos) > OverworldClimateModel.SNOW_MELT_TEMPERATURE)
        {
            removeIcePileOrIce(level, pos, state);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter world, BlockPos pos, Player player)
    {
        return new ItemStack(Blocks.ICE);
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState otherState, Direction facing)
    {
        return Helpers.isBlock(otherState, Blocks.ICE) || super.skipRendering(state, otherState, facing);
    }

    @Override
    public boolean hidesNeighborFace(BlockGetter level, BlockPos pos, BlockState state, BlockState neighborState, Direction dir)
    {
        return Helpers.isBlock(neighborState, Blocks.ICE);
    }
}
