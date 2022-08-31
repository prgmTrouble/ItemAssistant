package com.prgmTrouble.item_assistant.mixin;

import com.prgmTrouble.item_assistant.util.InventoryInspector;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin
{
    private static Slot s = null;
    @Inject
    (
        method = "insertItem",
        locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At
        (
            value = "INVOKE",
            ordinal = 1,
            target = "Lnet/minecraft/item/ItemStack;setCount(I)V"
        )
    )
    public void insertItem1(final ItemStack stack,final int startIndex,final int endIndex,
                            final boolean fromLast,final CallbackInfoReturnable<Boolean> cir,
                            final boolean bl,final int i,final Slot slot)
    {
        s = slot;
    }
    @Inject
    (
        method = "insertItem",
        locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At
        (
            value = "INVOKE",
            ordinal = 2,
            target = "Lnet/minecraft/item/ItemStack;setCount(I)V"
        )
    )
    public void insertItem2(final ItemStack stack,final int startIndex,final int endIndex,
                            final boolean fromLast,final CallbackInfoReturnable<Boolean> cir,
                            final boolean bl,final int i,final Slot slot)
    {
        s = slot;
    }
    @Redirect
    (
        method = "insertItem",
        at = @At
        (
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;setCount(I)V"
        )
    )
    public void setCount(final ItemStack itemStack,final int count)
    {
        itemStack.setCount(count);
        if(s != null)
        {
            final byte idx = (byte)s.getIndex();
            if(s.inventory instanceof final BlockEntity be)
                InventoryInspector.modify(be.getPos(),InventoryInspector.set(idx,itemStack));
            else if(s.inventory instanceof final Entity e)
                InventoryInspector.modify(e,InventoryInspector.set(idx,itemStack));
        }
    }
    @Inject
    (
        method = "insertItem",
        at = @At
        (
            value = "INVOKE",
            shift = At.Shift.AFTER,
            ordinal = 1,
            target = "Lnet/minecraft/item/ItemStack;setCount(I)V"
        )
    )
    public void insertItem1(final ItemStack stack,final int startIndex,final int endIndex,
                            final boolean fromLast,final CallbackInfoReturnable<Boolean> cir)
    {
        s = null;
    }
    @Inject
    (
        method = "insertItem",
        at = @At
        (
            value = "INVOKE",
            shift = At.Shift.AFTER,
            ordinal = 2,
            target = "Lnet/minecraft/item/ItemStack;setCount(I)V"
        )
    )
    public void insertItem2(final ItemStack stack,final int startIndex,final int endIndex,
                            final boolean fromLast,final CallbackInfoReturnable<Boolean> cir)
    {
        s = null;
    }
}