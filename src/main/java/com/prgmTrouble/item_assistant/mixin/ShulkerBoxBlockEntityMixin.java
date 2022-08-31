package com.prgmTrouble.item_assistant.mixin;

import com.prgmTrouble.item_assistant.util.InventoryInspector;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShulkerBoxBlockEntity.class)
public abstract class ShulkerBoxBlockEntityMixin extends LootableContainerBlockEntity implements SidedInventory
{
    protected ShulkerBoxBlockEntityMixin(final BlockEntityType<?> blockEntityType,final BlockPos blockPos,final BlockState blockState)
    {
        super(blockEntityType,blockPos,blockState);
    }
    
    private static BlockPos p = null;
    @Inject(method = "readInventoryNbt",at = @At("HEAD")) public void readNbt(final NbtCompound nbt,final CallbackInfo ci) {p = getPos();}
    @Redirect
    (
        method = "readInventoryNbt",
        at = @At
        (
            value = "INVOKE",
            target = "Lnet/minecraft/inventory/Inventories;readNbt(Lnet/minecraft/nbt/NbtCompound;Lnet/minecraft/util/collection/DefaultedList;)V"
        )
    )
    public void readNbt(final NbtCompound nbt,final DefaultedList<ItemStack> stacks)
    {
        Inventories.readNbt(nbt,stacks);
        InventoryInspector.modify(p,InventoryInspector.fill(stacks));
    }
    @Inject(method = "readInventoryNbt",at = @At("RETURN")) public void readNbtEnd(final NbtCompound nbt,final CallbackInfo ci) {p = null;}
}