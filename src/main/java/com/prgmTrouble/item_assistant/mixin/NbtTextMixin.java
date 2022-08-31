package com.prgmTrouble.item_assistant.mixin;

import net.minecraft.text.NbtText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(NbtText.class) public interface NbtTextMixin {@Accessor Optional<Text> getSeparator();}