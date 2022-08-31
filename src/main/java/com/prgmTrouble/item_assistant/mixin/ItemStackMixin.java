package com.prgmTrouble.item_assistant.mixin;

import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;

@Mixin(ItemStack.class)
public interface ItemStackMixin
{
    @Invoker("<init>")
    static ItemStack constructor(@SuppressWarnings("unused") final ItemConvertible item,
                                 @SuppressWarnings("unused") final int count,
                                 @SuppressWarnings({"unused","OptionalUsedAsFieldOrParameterType"})
                                 final Optional<NbtCompound> nbt)
    {
        // Don't return null so that the IDE doesn't complain about nulls
        //noinspection ConstantConditions
        return (ItemStack)new Object();
    }
}