package com.prgmTrouble.item_assistant.mixin;

import com.prgmTrouble.item_assistant.util.InventoryInspector;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.server.command.ItemCommand;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.*;

@Mixin(ItemCommand.class)
public abstract class ItemCommandMixin
{
    private static Entity e = null;
    private static byte s = 0;
    @Inject
    (
        method = "executeEntityModify",
        locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At
        (
            value = "INVOKE",
            target = "Lnet/minecraft/inventory/StackReference;set(Lnet/minecraft/item/ItemStack;)Z"
        )
    )
    private static void executeEntityModify(final ServerCommandSource source,final Collection<? extends Entity> targets,
                                            final int slot,final LootFunction modifier,final CallbackInfoReturnable<Integer> cir,
                                            @SuppressWarnings("rawtypes") final Map map,@SuppressWarnings("rawtypes") final Iterator var5,
                                            final Entity entity)
    {
        s = (byte)slot;
        e = entity;
    }
    @Inject
    (
        method = "executeEntityReplace",
        locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At
        (
            value = "INVOKE",
            target = "Lnet/minecraft/inventory/StackReference;set(Lnet/minecraft/item/ItemStack;)Z"
        )
    )
    private static void executeEntityReplace(final ServerCommandSource source,final Collection<? extends Entity> targets,
                                             final int slot,final ItemStack stack,final CallbackInfoReturnable<Integer> cir,
                                             @SuppressWarnings("rawtypes") final List list,@SuppressWarnings("rawtypes") final Iterator var5,
                                             final Entity entity)
    {
        s = (byte)slot;
        e = entity;
    }
    @Redirect
    (
        method = {"executeEntityModify","executeEntityReplace"},
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
    @Inject
    (
        method = "executeEntityModify",
        at = @At
        (
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/inventory/StackReference;set(Lnet/minecraft/item/ItemStack;)Z"
        )
    )
    private static void executeEntityModify(final ServerCommandSource source,final Collection<? extends Entity> targets,
                                            final int slot,final LootFunction modifier,final CallbackInfoReturnable<Integer> cir)
    {
        s = 0;
        e = null;
    }
    @Inject
    (
        method = "executeEntityReplace",
        at = @At
        (
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/inventory/StackReference;set(Lnet/minecraft/item/ItemStack;)Z"
        )
    )
    private static void executeEntityReplace(final ServerCommandSource source,final Collection<? extends Entity> targets,
                                             final int slot,final ItemStack stack,final CallbackInfoReturnable<Integer> cir)
    {
        s = 0;
        e = null;
    }
}