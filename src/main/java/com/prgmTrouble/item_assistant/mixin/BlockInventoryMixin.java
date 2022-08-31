package com.prgmTrouble.item_assistant.mixin;

import com.prgmTrouble.item_assistant.util.InventoryInspector;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractFurnaceBlockEntity.class,BrewingStandBlockEntity.class,HopperBlockEntity.class,LootableContainerBlockEntity.class})
public abstract class BlockInventoryMixin extends BlockEntity
{
    protected BlockInventoryMixin(final BlockEntityType<?> blockEntityType,final BlockPos blockPos,final BlockState blockState)
    {
        super(blockEntityType,blockPos,blockState);
    }
    
    @Inject(method = "setStack",at = @At("RETURN"))
    public void setStack(final int slot,final ItemStack stack,final CallbackInfo ci)
    {
        InventoryInspector.modify(getPos(),InventoryInspector.set((byte)slot,stack));
    }
    @Inject(method = "removeStack(II)Lnet/minecraft/item/ItemStack;",at = @At("HEAD"))
    public void removeStack(final int slot,final int amount,final CallbackInfoReturnable<ItemStack> cir)
    {
        InventoryInspector.modify(getPos(),InventoryInspector.remove((byte)slot,(byte)amount));
    }
}