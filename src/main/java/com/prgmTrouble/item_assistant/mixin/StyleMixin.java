package com.prgmTrouble.item_assistant.mixin;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Style.class)
public interface StyleMixin
{
    @Invoker("<init>")
    static Style constructor(@SuppressWarnings("unused") final TextColor color,
                             @SuppressWarnings("unused") final Boolean bold,
                             @SuppressWarnings("unused") final Boolean italic,
                             @SuppressWarnings("unused") final Boolean underlined,
                             @SuppressWarnings("unused") final Boolean strikethrough,
                             @SuppressWarnings("unused") final Boolean obfuscated,
                             @SuppressWarnings("unused") final ClickEvent clickEvent,
                             @SuppressWarnings("unused") final HoverEvent hoverEvent,
                             @SuppressWarnings("unused") final String insertion,
                             @SuppressWarnings("unused") final Identifier font)
    {
        // Don't return null so that the IDE doesn't complain about nulls
        //noinspection ConstantConditions
        return (Style)new Object();
    }
}