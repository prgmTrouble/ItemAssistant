package com.prgmTrouble.item_assistant.mixin;

import com.prgmTrouble.item_assistant.util.InventoryInspector;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin extends LockableContainerBlockEntity implements SidedInventory
{
    protected BrewingStandBlockEntityMixin(final BlockEntityType<?> blockEntityType,final BlockPos blockPos,final BlockState blockState)
    {
        super(blockEntityType,blockPos,blockState);
    }
    
    @Inject
    (
        method = "tick",
        locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At
        (
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/item/ItemStack;decrement(I)V"
        )
    )
    private static void decrement(final World world,final BlockPos pos,final BlockState state,
                                  final BrewingStandBlockEntity blockEntity,final CallbackInfo ci,
                                  final ItemStack itemStack)
    {
        InventoryInspector.modify(pos,InventoryInspector.set((byte)4,itemStack));
    }
    
    private static BlockPos p = null;
    @Inject(method = "craft",at = @At("HEAD"))
    private static void craft(final World world,final BlockPos pos,final DefaultedList<ItemStack> slots,
                              final CallbackInfo ci)
    {
        p = pos;
    }
    @Redirect
    (
        method = "craft",
        at = @At
        (
            value = "INVOKE",
            target = "Lnet/minecraft/util/collection/DefaultedList;set(ILjava/lang/Object;)Ljava/lang/Object;"
        )
    )
    private static <E> E set(final DefaultedList<E> defaultedList,final int index,final E element)
    {
        InventoryInspector.modify(p,InventoryInspector.set((byte)index,(ItemStack)element));
        return defaultedList.set(index,element);
    }
    @Inject
    (
        method = "craft",
        locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At
        (
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/item/ItemStack;decrement(I)V"
        )
    )
    private static void craft(final World world,final BlockPos pos,final DefaultedList<ItemStack> slots,
                              final CallbackInfo ci,final ItemStack itemStack)
    {
        InventoryInspector.modify(pos,InventoryInspector.set((byte)3,itemStack));
    }
    @Inject(method = "craft",at = @At("RETURN"))
    private static void craftEnd(final World world,final BlockPos pos,final DefaultedList<ItemStack> slots,
                                 final CallbackInfo ci)
    {
        p = null;
    }
}