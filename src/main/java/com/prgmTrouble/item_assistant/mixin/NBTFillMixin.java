package com.prgmTrouble.item_assistant.mixin;

import com.prgmTrouble.item_assistant.util.InventoryInspector;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.*;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AbstractFurnaceBlockEntity.class,BarrelBlockEntity.class,BrewingStandBlockEntity.class,
        CampfireBlockEntity.class,ChestBlockEntity.class,DispenserBlockEntity.class,
        HopperBlockEntity.class})
public abstract class NBTFillMixin extends BlockEntity
{
    public NBTFillMixin(final BlockEntityType<?> type,final BlockPos pos,final BlockState state)
    {
        super(type,pos,state);
    }
    
    private static BlockPos p = null;
    @Inject(method = "readNbt",at = @At("HEAD")) public void readNbt(final NbtCompound nbt,final CallbackInfo ci) {p = getPos();}
    @Redirect
    (
        method = "readNbt",
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
    @Inject(method = "readNbt",at = @At("RETURN")) public void readNbtEnd(final NbtCompound nbt,final CallbackInfo ci) {p = null;}
}