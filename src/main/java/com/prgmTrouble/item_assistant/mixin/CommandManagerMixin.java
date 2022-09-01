package com.prgmTrouble.item_assistant.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.prgmTrouble.item_assistant.command.InventoryInspectorCommand;
import com.prgmTrouble.item_assistant.command.ItemEntityCheckpointCommand;
import com.prgmTrouble.item_assistant.command.NameCleanseCommand;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandManager.class)
public abstract class CommandManagerMixin
{
    @Shadow @Final private CommandDispatcher<ServerCommandSource> dispatcher;
    
    @Inject(method = "<init>",at = @At("RETURN"))
    private void onRegister(final CommandManager.RegistrationEnvironment arg,final CallbackInfo ci)
    {
        ItemEntityCheckpointCommand.register(dispatcher);
        InventoryInspectorCommand.register(dispatcher);
        NameCleanseCommand.register(dispatcher);
    }
}