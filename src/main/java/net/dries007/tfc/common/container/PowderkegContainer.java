/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.container;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.dries007.tfc.common.blockentities.PowderkegBlockEntity;
import net.dries007.tfc.common.blocks.devices.PowderkegBlock;
import net.dries007.tfc.common.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

public class PowderkegContainer extends BlockEntityContainer<PowderkegBlockEntity> implements ButtonHandlerContainer
{
    public static PowderkegContainer create(PowderkegBlockEntity powderkeg, Inventory playerInv, int windowId)
    {
        return new PowderkegContainer(windowId, powderkeg).init(playerInv, 0);
    }

    private PowderkegContainer(int windowId, PowderkegBlockEntity powderkeg)
    {
        super(TFCContainerTypes.POWDERKEG.get(), windowId, powderkeg);
    }

    @Override
    public void onButtonPress(int buttonID, @Nullable CompoundTag extraNBT)
    {
        Level level = blockEntity.getLevel();
        if (level != null)
        {
            PowderkegBlock.toggleSeal(level, blockEntity.getBlockPos(), blockEntity.getBlockState());
        }
    }

    @Override
    protected boolean moveStack(ItemStack stack, int slotIndex)
    {
        if (blockEntity.getBlockState().getValue(PowderkegBlock.SEALED)) return true;
        return switch (typeOf(slotIndex))
            {
                case MAIN_INVENTORY, HOTBAR -> !moveItemStackTo(stack, 0, PowderkegBlockEntity.SLOTS, false);
                case CONTAINER -> !moveItemStackTo(stack, containerSlots, slots.size(), false);
            };
    }

    @Override
    protected void addContainerSlots()
    {
        blockEntity.getCapability(Capabilities.ITEM).ifPresent(inventory -> {
            for (int y = 0; y < 3; y++)
            {
                for (int x = 0; x < 4; x++)
                {
                    addSlot(new CallbackSlot(blockEntity, inventory, x * 3 + y, 25 + x * 18, 19 + y * 18));
                }
            }
        });
    }
}
