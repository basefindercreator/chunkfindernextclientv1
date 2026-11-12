package com.blockscanner.client.mixin;

import com.blockscanner.client.nameprotect.NameProtectManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiGraphicsExtractor.class)
public abstract class TextRendererMixin {
	@ModifyVariable(
			method = "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V",
			at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private String blockscanner$protectPlainText(String text) {
		return NameProtectManager.replace(text);
	}

	@ModifyVariable(
			method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V",
			at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private FormattedCharSequence blockscanner$protectFormattedText(FormattedCharSequence text) {
		return NameProtectManager.replace(text);
	}

	@ModifyVariable(
			method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V",
			at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private Component blockscanner$protectComponent(Component text) {
		return Component.literal(NameProtectManager.replace(text.getString()));
	}
}