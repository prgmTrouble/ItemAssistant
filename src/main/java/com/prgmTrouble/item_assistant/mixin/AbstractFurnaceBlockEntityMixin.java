package com.prgmTrouble.item_assistant.mixin;

import com.prgmTrouble.item_assistant.util.InventoryInspector;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeInputProvider;
import net.minecraft.recipe.RecipeUnlocker;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin extends LockableContainerBlockEntity implements SidedInventory,RecipeUnlocker,RecipeInputProvider
{
    @Shadow protected DefaultedList<ItemStack> inventory;
    
    protected AbstractFurnaceBlockEntityMixin(final BlockEntityType<?> blockEntityType,final BlockPos blockPos,final BlockState blockState)
    {
        super(blockEntityType,blockPos,blockState);
    }
    
    @Inject
    (
        method = "readNbt",
        at = @At
        (
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/inventory/Inventories;readNbt(Lnet/minecraft/nbt/NbtCompound;Lnet/minecraft/util/collection/DefaultedList;)V"
        )
    )
    public void readNbt(final NbtCompound nbt,final CallbackInfo ci)
    {
        InventoryInspector.modify(getPos(),InventoryInspector.fill(inventory));
    }
    
    @Inject
    (
        method = "tick",
        at = @At
        (
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/item/ItemStack;decrement(I)V"
        )
    )
    private static void tick(final World world,final BlockPos pos,final BlockState state,
                             final AbstractFurnaceBlockEntity blockEntity,final CallbackInfo ci)
    {
        InventoryInspector.modify(pos,InventoryInspector.remove((byte)1,(byte)1));
    }
    private static BlockPos p = null;
    @Inject
    (
        method = "tick",
        at = @At
        (
            value = "INVOKE",
            target = "Lnet/minecraft/util/collection/DefaultedList;set(ILjava/lang/Object;)Ljava/lang/Object;"
        )
    )
    private static void tickStart(final World world,final BlockPos pos,final BlockState state,
                                  final AbstractFurnaceBlockEntity blockEntity,final CallbackInfo ci)
    {
        p = pos;
    }
    @Inject
    (
        method = "tick",
        at = @At
        (
            value = "INVOKE",
            target = "Lnet/minecraft/block/entity/AbstractFurnaceBlockEntity;craftRecipe(Lnet/minecraft/recipe/Recipe;Lnet/minecraft/util/collection/DefaultedList;I)Z"
        )
    )
    private static void set(final World world,final BlockPos pos,final BlockState state,
                            final AbstractFurnaceBlockEntity blockEntity,final CallbackInfo ci)
    {
        p = pos;
    }
    @Redirect
    (
        method = {"tick","craftRecipe"},
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
        method = "craftRecipe",
        locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At
        (
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/item/ItemStack;increment(I)V"
        )
    )
    private static void increment(final Recipe<?> recipe,final DefaultedList<ItemStack> slots,
                                  final int count,final CallbackInfoReturnable<Boolean> cir,
                                  final ItemStack itemStack,final ItemStack itemStack2,
                                  final ItemStack itemStack3)
    {
        InventoryInspector.modify(p,InventoryInspector.set((byte)2,itemStack3));
    }
    @Inject
    (
        method = "craftRecipe",
        locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At
        (
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/item/ItemStack;decrement(I)V"
        )
    )
    private static void decrement(final Recipe<?> recipe,final DefaultedList<ItemStack> slots,
                                  final int count,final CallbackInfoReturnable<Boolean> cir,
                                  final ItemStack itemStack)
    {
        InventoryInspector.modify(p,InventoryInspector.set((byte)0,itemStack));
    }
    
    @Inject
    (
        method = "tick",
        at = @At
        (
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/util/collection/DefaultedList;set(ILjava/lang/Object;)Ljava/lang/Object;"
        )
    )
    private static void tickEnd(final World world,final BlockPos pos,final BlockState state,
                                final AbstractFurnaceBlockEntity blockEntity,final CallbackInfo ci)
    {
        p = null;
    }
    @Inject(method = "craftRecipe",at = @At("RETURN"))
    private static void craftRecipe(final Recipe<?> recipe,final DefaultedList<ItemStack> slots,
                                    final int count,final CallbackInfoReturnable<Boolean> cir)
    {
        p = null;
    }
}