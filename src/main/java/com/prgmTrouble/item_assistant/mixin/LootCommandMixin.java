package com.prgmTrouble.item_assistant.mixin;

import com.prgmTrouble.item_assistant.util.InventoryInspector;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.LootCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(LootCommand.class)
public abstract class LootCommandMixin
{
    @Inject
    (
        method = "insert",
        locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At
        (
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/item/ItemStack;increment(I)V"
        )
    )
    private static void insert(final Inventory inventory,final ItemStack stack,final CallbackInfoReturnable<Boolean> cir,
                               final boolean bl,final int i,final ItemStack itemStack)
    {
        InventoryInspector.modify(((BlockEntity)inventory).getPos(),InventoryInspector.set((byte)i,itemStack));
    }
    private static Entity e = null;
    @Inject(method = "replace",at = @At("HEAD"))
    private static void replace(final Entity entity,final List<ItemStack> stacks,final int slot,final int stackCount,
                                final List<ItemStack> addedStacks,final CallbackInfo ci)
    {
        e = entity;
    }
    private static byte s = 0;
    @Inject
    (
        method = "replace",
        locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At
        (
            value = "INVOKE",
            target = "Lnet/minecraft/inventory/StackReference;set(Lnet/minecraft/item/ItemStack;)Z"
        )
    )
    private static void replace(final Entity entity,final List<ItemStack> stacks,final int slot,final int stackCount,
                                final List<ItemStack> addedStacks,final CallbackInfo ci,final int i)
    {
        s = (byte)(slot + i);
    }
    @Redirect
    (
        method = "replace",
        at = @At
        (
            value = "INVOKE",
            target = "Lnet/minecraft/inventory/StackReference;set(Lnet/minecraft/item/ItemStack;)Z"
        )
    )
    private static boolean set(final StackReference stackReference,final ItemStack stack)
    {
        if(stackReference.set(stack))
        {
            InventoryInspector.modify(e,InventoryInspector.set(s,stack));
            return true;
        }
        return false;
    }
    @Inject(method = "replace",at = @At("RETURN"))
    private static void replaceEnd(final Entity entity,final List<ItemStack> stacks,final int slot,
                                   final int stackCount,final List<ItemStack> addedStacks,final CallbackInfo ci)
    {
        e = null;
    }
}