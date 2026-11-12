package com.blockscanner.client.mixin;

import com.blockscanner.client.nameprotect.NameProtectManager;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextRenderer.class)
public abstract class TextRendererMixin {
	@ModifyVariable(
			method = "drawInternal(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;IIZ)I",
			at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private String blockscanner$protectPlainText(String text) {
		return NameProtectManager.replace(text);
	}

	@ModifyVariable(
			method = "drawInternal(Lnet/minecraft/text/OrderedText;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;IIZ)I",
			at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private OrderedText blockscanner$protectFormattedText(OrderedText text) {
		return NameProtectManager.replace(text);
	}
}