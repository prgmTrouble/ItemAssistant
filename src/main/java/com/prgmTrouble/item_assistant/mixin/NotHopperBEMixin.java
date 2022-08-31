package com.prgmTrouble.item_assistant.mixin;

import com.prgmTrouble.item_assistant.util.InventoryInspector;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractFurnaceBlockEntity.class,BrewingStandBlockEntity.class,LootableContainerBlockEntity.class})
public abstract class NotHopperBEMixin extends LockableContainerBlockEntity
{
    public NotHopperBEMixin(final BlockEntityType<?> type,final BlockPos pos,final BlockState state)
    {
        super(type,pos,state);
    }
    
    @Inject(method = "removeStack(I)Lnet/minecraft/item/ItemStack;",at = @At("RETURN"))
    public void removeStack(final int slot,final CallbackInfoReturnable<ItemStack> cir)
    {
        InventoryInspector.modify(getPos(),InventoryInspector.remove((byte)slot,(byte)getStack(slot).getCount()));
    }
}