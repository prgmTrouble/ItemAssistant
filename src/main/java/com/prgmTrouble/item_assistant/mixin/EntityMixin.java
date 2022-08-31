package com.prgmTrouble.item_assistant.mixin;


import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.Box;
import net.minecraft.world.entity.EntityLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class EntityMixin implements Nameable,EntityLike,CommandOutput
{
    @Shadow public abstract void setPos(final double x,final double y,final double z);
    @Shadow public abstract void setBoundingBox(final Box boundingBox);
    @Shadow protected abstract Box calculateBoundingBox();
    
    /**
     * Avoids calling the overridden 'setPosition' method on load which would cause a null deref
     * in ItemEntityMixin.
     */
    @Redirect
    (
        method = "<init>",
        at = @At
        (
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;setPosition(DDD)V"
        )
    )
    private void onLoad(final Entity entity,final double x,final double y,final double z)
    {
        setPos(x,y,z);
        setBoundingBox(calculateBoundingBox());
    }
}