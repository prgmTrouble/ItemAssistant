package com.prgmTrouble.item_assistant.mixin;

import com.prgmTrouble.item_assistant.util.InventoryInspector;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.*;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin extends LootableContainerBlockEntity implements Hopper
{
    protected HopperBlockEntityMixin(final BlockEntityType<?> blockEntityType,
                                     final BlockPos blockPos,final BlockState blockState)
    {
        super(blockEntityType,blockPos,blockState);
    }
    
    @Inject
    (
        method = "transfer(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/inventory/Inventory;Lnet/minecraft/item/ItemStack;ILnet/minecraft/util/math/Direction;)Lnet/minecraft/item/ItemStack;",
        locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At
        (
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;increment(I)V",
            shift = At.Shift.AFTER
        )
    )
    private static void transfer(final Inventory from,final Inventory to,final ItemStack stack,final int slot,
                                 final Direction direction,final CallbackInfoReturnable<ItemStack> cir,
                                 final ItemStack itemStack)
    {
        if(to instanceof final BlockEntity b) InventoryInspector.modify(b.getPos(),InventoryInspector.set((byte)slot,itemStack));
        else if(to instanceof final Entity e) InventoryInspector.modify(e         ,InventoryInspector.set((byte)slot,itemStack));
    }
}