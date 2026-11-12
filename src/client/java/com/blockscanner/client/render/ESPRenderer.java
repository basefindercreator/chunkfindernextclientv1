package com.blockscanner.client.render;

import com.blockscanner.client.BlockScannerClient;
import com.blockscanner.client.ScannerConfig;
import com.blockscanner.client.scanner.ChunkPatternAnalyzer;
import com.blockscanner.client.scanner.ScanTarget;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.client.util.math.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

public final class ESPRenderer {
	private static final float ALPHA = 0.88F;

	private ESPRenderer() {
	}

	public static void render(WorldRenderContext context, ChunkPatternAnalyzer analyzer) {
		MinecraftClient client = MinecraftClient.getInstance();
		ScannerConfig config = ScannerConfig.get();
		World world = context.world();
		if (!BlockScannerClient.isLocalWorld(client) || !config.enabled || !config.espEnabled
				|| world == null || context.matrixStack() == null || context.consumers() == null) {
			return;
		}

		MatrixStack matrices = context.matrixStack();
		VertexConsumerProvider consumers = context.consumers();
		VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
		Vec3d camera = context.camera().getPos();
		int minY = world.getBottomY();
		int maxY = world.getBottomY() + world.getHeight();

		matrices.push();
		matrices.translate(-camera.x, -camera.y, -camera.z);
		RenderSystem.disableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.lineWidth(2.0F);
		try {
			float[] flagColor = color(config.flaggedChunkColor);
			Set<Long> flags = analyzer.flaggedChunks();
			for (long packedChunk : flags) {
				int chunkX = ChunkPos.getPackedX(packedChunk);
				int chunkZ = ChunkPos.getPackedZ(packedChunk);
				Box chunkBox = new Box(chunkX * 16.0, minY, chunkZ * 16.0,
						chunkX * 16.0 + 16.0, maxY, chunkZ * 16.0 + 16.0);
				VertexRendering.drawBox(matrices, lines, chunkBox, flagColor[0], flagColor[1], flagColor[2], ALPHA);
			}

			for (List<ScanTarget> targets : analyzer.results().values()) {
				for (ScanTarget target : targets) {
					BlockPos pos = target.pos();
					Box targetBox = new Box(pos).expand(0.03);
					int rgb = config.blockColors.getOrDefault(target.blockId(), config.chunkColor);
					float[] targetColor = color(rgb);
					VertexRendering.drawBox(matrices, lines, targetBox, targetColor[0], targetColor[1], targetColor[2], ALPHA);
				}
			}
		} finally {
			RenderSystem.lineWidth(1.0F);
			RenderSystem.disableBlend();
			RenderSystem.enableDepthTest();
			matrices.pop();
		}
	}

	private static float[] color(int rgb) {
		return new float[] {((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F,
				(rgb & 0xFF) / 255.0F};
	}
}