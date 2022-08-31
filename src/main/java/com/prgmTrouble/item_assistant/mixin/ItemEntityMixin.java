package com.prgmTrouble.item_assistant.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.prgmTrouble.item_assistant.util.ItemEntityDetector.testCheckpoint;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity
{
    public ItemEntityMixin(final EntityType<?> type,final World world) {super(type,world);}
    
    private BlockPos cachedBlockPos;
    private Text cachedName;
    
    @Inject
    (
        method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;)V",
        at = @At
        (
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;)V",
            shift = At.Shift.AFTER
        )
    )
    private void ctor(final EntityType<? extends ItemEntity> entityType,final World world,final CallbackInfo ci)
    {
        cachedBlockPos = getBlockPos();
    }
    @Inject
    (
        method = "<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;DDD)V",
        at = @At
        (
            value = "INVOKE",
            target = "Lnet/minecraft/entity/ItemEntity;setPosition(DDD)V",
            shift = At.Shift.BEFORE
        )
    )
    private void ctor(final World world,final double x,final double y,final double z,final ItemStack stack,
                      final double velocityX,final double velocityY,final double velocityZ,final CallbackInfo ci)
    {
        cachedName = stack.getName();
    }
    @Inject
    (
        method = "<init>(Lnet/minecraft/entity/ItemEntity;)V",
        at = @At
        (
            value = "INVOKE",
            target = "Lnet/minecraft/entity/ItemEntity;setStack(Lnet/minecraft/item/ItemStack;)V",
            shift = At.Shift.AFTER
        )
    )
    private void ctor(final ItemEntity entity,final CallbackInfo ci)
    {
        cachedBlockPos = entity.getBlockPos();
        cachedName = entity.getStack().getName();
    }
    
    @Override
    public void setPosition(final double x,final double y,final double z)
    {
        super.setPosition(x,y,z);
        if(!cachedBlockPos.equals(getBlockPos()))
            testCheckpoint((ServerWorld)world,cachedBlockPos = getBlockPos(),cachedName);
    }
}