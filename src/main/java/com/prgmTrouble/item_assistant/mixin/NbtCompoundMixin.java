package com.prgmTrouble.item_assistant.mixin;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(NbtCompound.class)
public interface NbtCompoundMixin
{
    @Accessor Map<String,NbtElement> getEntries();
    
    @Invoker("<init>")
    static NbtCompound constructor(@SuppressWarnings("unused")final Map<String,NbtElement> entries)
    {
        // Don't return null so that the IDE doesn't complain about nulls
        //noinspection ConstantConditions
        return (NbtCompound)new Object();
    }
}