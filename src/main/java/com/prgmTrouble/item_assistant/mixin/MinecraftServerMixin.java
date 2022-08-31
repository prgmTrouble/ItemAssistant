package com.prgmTrouble.item_assistant.mixin;

import com.prgmTrouble.item_assistant.util.InventoryInspector;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin
{
    @Inject(method = "startServer",at = @At("RETURN"))
    private static <S extends MinecraftServer> void startServer(final Function<Thread,S> serverFactory,
                                                                final CallbackInfoReturnable<S> cir)
    {
        InventoryInspector.SERVER = cir.getReturnValue();
    }
}