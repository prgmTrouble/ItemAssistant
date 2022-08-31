package com.prgmTrouble.item_assistant.mixin;

import com.prgmTrouble.item_assistant.util.InventoryInspector;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.*;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AbstractFurnaceBlockEntity.class,BrewingStandBlockEntity.class,
        LootableContainerBlockEntity.class,CampfireBlockEntity.class})
public abstract class ClearableMixin extends BlockEntity
{
    protected ClearableMixin(final BlockEntityType<?> blockEntityType,final BlockPos blockPos,final BlockState blockState)
    {
        super(blockEntityType,blockPos,blockState);
    }
    
    @Inject(method = "clear",at = @At("HEAD"))
    public void clear(final CallbackInfo ci) {InventoryInspector.modify(getPos(),InventoryInspector.clear());}
}