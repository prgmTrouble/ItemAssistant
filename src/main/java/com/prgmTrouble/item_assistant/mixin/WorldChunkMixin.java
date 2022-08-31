package com.prgmTrouble.item_assistant.mixin;

import com.prgmTrouble.item_assistant.util.InventoryInspector;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin implements Chunk
{
    @Shadow @Nullable public abstract BlockEntity getBlockEntity(final BlockPos pos);
    
    @Inject
    (
        method = "setBlockEntity",
        at = @At
        (
            value = "INVOKE",
            target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
        )
    )
    public void setBlockEntity(final BlockEntity blockEntity,final CallbackInfo ci)
    {
        if(blockEntity instanceof final Inventory i)
            InventoryInspector.placeInventory(blockEntity.getPos(),i);
    }
    @Inject
    (
        method = "removeBlockEntity(Lnet/minecraft/util/math/BlockPos;)V",
        at = @At
        (
            value = "INVOKE",
            target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;"
        )
    )
    public void removeBlockEntity(final BlockPos pos,final CallbackInfo ci)
    {
        final BlockEntity be = getBlockEntity(pos);
        if(be instanceof Inventory)
            InventoryInspector.removeInventory(pos);
    }
    @Inject(method = "removeBlockEntity(Lnet/minecraft/block/entity/BlockEntity;)V",at = @At("HEAD"))
    public void removeBlockEntity(final BlockEntity blockEntity,final CallbackInfo ci)
    {
        if(blockEntity instanceof Inventory)
            InventoryInspector.removeInventory(blockEntity.getPos());
    }
}