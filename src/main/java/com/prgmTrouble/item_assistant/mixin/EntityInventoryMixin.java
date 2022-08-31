package com.prgmTrouble.item_assistant.mixin;

import com.prgmTrouble.item_assistant.util.InventoryInspector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(StorageMinecartEntity.class)
public abstract class EntityInventoryMixin extends AbstractMinecartEntity implements Inventory,NamedScreenHandlerFactory
{
    public EntityInventoryMixin(final EntityType<?> entityType,final World world) {super(entityType,world);}
    
    private static Entity e = null;
    @Inject
    (
        method = "readCustomDataFromNbt",
        at = @At
        (
            value = "INVOKE",
            target = "Lnet/minecraft/inventory/Inventories;readNbt(Lnet/minecraft/nbt/NbtCompound;Lnet/minecraft/util/collection/DefaultedList;)V"
        )
    )
    public void readCustomDataFromNbt(final NbtCompound nbt,final CallbackInfo ci) {e = this;}
    @Redirect
    (
        method = "readCustomDataFromNbt",
        at = @At
        (
            value = "INVOKE",
            target = "Lnet/minecraft/inventory/Inventories;readNbt(Lnet/minecraft/nbt/NbtCompound;Lnet/minecraft/util/collection/DefaultedList;)V"
        )
    )
    public void readNbt(final NbtCompound nbt,final DefaultedList<ItemStack> stacks)
    {
        Inventories.readNbt(nbt,stacks);
        InventoryInspector.modify(e,InventoryInspector.fill(stacks));
    }
    @Inject
    (
        method = "readCustomDataFromNbt",
        at = @At
        (
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/inventory/Inventories;readNbt(Lnet/minecraft/nbt/NbtCompound;Lnet/minecraft/util/collection/DefaultedList;)V"
        )
    )
    public void readCustomDataFromNbtEnd(final NbtCompound nbt,final CallbackInfo ci) {e = null;}
    
    @Inject(method = "setStack",at = @At("HEAD"))
    public void setStack(final int slot,final ItemStack stack,final CallbackInfo ci)
    {
        InventoryInspector.modify(this,InventoryInspector.set((byte)slot,stack));
    }
    @Inject(method = "removeStack(II)Lnet/minecraft/item/ItemStack;",at = @At("HEAD"))
    public void removeStack(final int slot,final int amount,final CallbackInfoReturnable<ItemStack> cir)
    {
        InventoryInspector.modify(this,InventoryInspector.remove((byte)slot,(byte)amount));
    }
    @Inject
    (
        method = "removeStack(I)Lnet/minecraft/item/ItemStack;",
        locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At("RETURN")
    )
    public void removeStack(final int slot,final CallbackInfoReturnable<ItemStack> cir,final ItemStack itemStack)
    {
        InventoryInspector.modify(this,InventoryInspector.remove((byte)slot,(byte)itemStack.getCount()));
    }
    @Inject(method = "clear",at = @At("HEAD"))
    public void clear(final CallbackInfo ci) {InventoryInspector.modify(this,InventoryInspector.clear());}
    @Inject(method = "remove",at = @At("HEAD"))
    public void remove(final RemovalReason reason,final CallbackInfo ci) {InventoryInspector.removeInventory(this);}
}