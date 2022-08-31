package com.prgmTrouble.item_assistant.mixin;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(NbtList.class)
public interface NbtListMixin
{
    @Accessor List<NbtElement> getValue();
    
    @Invoker("<init>")
    static NbtList constructor(@SuppressWarnings("unused")final List<NbtElement> list,
                               @SuppressWarnings("unused")final byte type)
    {
        // Don't return null so that the IDE doesn't complain about nulls
        //noinspection ConstantConditions
        return (NbtList)new Object();
    }
}