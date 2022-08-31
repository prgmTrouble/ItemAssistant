package com.prgmTrouble.item_assistant.mixin;

import com.prgmTrouble.item_assistant.util.InventoryInspector;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BarrelBlockEntity.class,ChestBlockEntity.class,DispenserBlockEntity.class,
        HopperBlockEntity.class,ShulkerBoxBlockEntity.class})
public abstract class InvStackListMixin extends LootableContainerBlockEntity
{
    protected InvStackListMixin(final BlockEntityType<?> blockEntityType,final BlockPos blockPos,final BlockState blockState)
    {
        super(blockEntityType,blockPos,blockState);
    }
    
    @Inject(method = "setInvStackList",at = @At("HEAD"))
    public void setInvStackList(final DefaultedList<ItemStack> list,final CallbackInfo ci)
    {
        InventoryInspector.modify(getPos(),InventoryInspector.fill(list));
    }
}