/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blockentities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

import net.dries007.tfc.util.Helpers;

import static net.dries007.tfc.TerraFirmaCraft.MOD_ID;

public class ThatchBedBlockEntity extends InventoryBlockEntity<ItemStackHandler>
{
    private static final Component NAME = Helpers.translatable(MOD_ID + ".tile_entity.thatch_bed");

    private BlockState headState;
    private BlockState footState;

    public ThatchBedBlockEntity(BlockPos pos, BlockState state)
    {
        super(TFCBlockEntities.THATCH_BED.get(), pos, state, defaultInventory(1), NAME);
        headState = footState = Blocks.AIR.defaultBlockState();
    }

    public void setBed(BlockState head, BlockState foot, ItemStack top)
    {
        assert level != null;
        headState = head;
        footState = foot;
        inventory.setStackInSlot(0, top);
    }

    public void destroyBed()
    {
        ejectInventory();
        invalidateCapabilities();
        if (level instanceof ServerLevel serverLevel)
        {
            Helpers.dropWithContext(serverLevel, headState, worldPosition, ctx -> {}, true);
            Helpers.dropWithContext(serverLevel, footState, worldPosition, ctx -> {}, true);
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag)
    {
        tag.put("HeadBlockState", NbtUtils.writeBlockState(headState));
        tag.put("FootBlockState", NbtUtils.writeBlockState(footState));
        super.saveAdditional(tag);
    }

    @Override
    public void loadAdditional(CompoundTag tag)
    {
        headState = NbtUtils.readBlockState(tag.getCompound("HeadBlockState"));
        footState = NbtUtils.readBlockState(tag.getCompound("FootBlockState"));
        super.loadAdditional(tag);
    }
}
