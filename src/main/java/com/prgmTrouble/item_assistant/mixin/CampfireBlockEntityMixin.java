package com.prgmTrouble.item_assistant.mixin;

import com.prgmTrouble.item_assistant.util.InventoryInspector;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Clearable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CampfireBlockEntity.class)
public abstract class CampfireBlockEntityMixin extends BlockEntity implements Clearable
{
    public CampfireBlockEntityMixin(final BlockEntityType<?> type,final BlockPos pos,final BlockState state)
    {
        super(type,pos,state);
    }
    
    @Inject
    (
        method = "litServerTick",
        locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At
        (
            value = "INVOKE",
            target = "Lnet/minecraft/util/collection/DefaultedList;set(ILjava/lang/Object;)Ljava/lang/Object;"
        )
    )
    private static void litServerTick(final World world,final BlockPos pos,final BlockState state,
                                      final CampfireBlockEntity campfire,final CallbackInfo ci,
                                      final boolean bl,final int i)
    {
        InventoryInspector.modify(pos,InventoryInspector.set((byte)i,ItemStack.EMPTY));
    }
    
    private static BlockPos p = null;
    @Inject(method = "addItem",at = @At("HEAD"))
    public void addItem(final ItemStack item,final int integer,final CallbackInfoReturnable<Boolean> cir)
    {
        p = getPos();
    }
    @Redirect
    (
        method = "addItem",
        at = @At
        (
            value = "INVOKE",
            target = "Lnet/minecraft/util/collection/DefaultedList;set(ILjava/lang/Object;)Ljava/lang/Object;"
        )
    )
    public <E> E set(final DefaultedList<E> defaultedList,final int index,final E element)
    {
        InventoryInspector.modify(p,InventoryInspector.set((byte)index,(ItemStack)element));
        return defaultedList.set(index,element);
    }
    @Inject(method = "addItem",at = @At("RETURN"))
    public void addItemEnd(final ItemStack item,final int integer,final CallbackInfoReturnable<Boolean> cir)
    {
        p = null;
    }
}