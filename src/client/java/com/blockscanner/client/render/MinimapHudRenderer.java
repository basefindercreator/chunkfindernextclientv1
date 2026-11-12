package com.blockscanner.client.render;

import com.blockscanner.client.BlockScannerClient;
import com.blockscanner.client.ScannerConfig;
import com.blockscanner.client.scanner.ChunkPatternAnalyzer;
import com.blockscanner.client.scanner.ScanTarget;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;

public final class MinimapHudRenderer {
	private static final int SIZE = 142;

	private MinimapHudRenderer() {
	}

	public static void render(DrawContext context, ChunkPatternAnalyzer analyzer) {
		MinecraftClient client = MinecraftClient.getInstance();
		ScannerConfig config = ScannerConfig.get();
		if (!BlockScannerClient.isLocalWorld(client) || !config.enabled || !config.minimapEnabled) {
			return;
		}

		int left = context.getScaledWindowWidth() - SIZE - 12;
		int top = context.getScaledWindowHeight() - SIZE - 30;
		int centerX = left + SIZE / 2;
		int centerY = top + SIZE / 2 + 9;
		int radius = 54;
		int rangeBlocks = Math.max(16, config.radarScopeChunks * 16);
		double scale = radius / (double) rangeBlocks;

		context.fill(left, top, left + SIZE, top + SIZE + 18, 0xD90B1419);
		context.fill(left, top, left + SIZE, top + 2, 0xFF42F56C);
		context.drawTextWithShadow(client.textRenderer, Text.literal("LOCAL RADAR"), left + 9, top + 7, 0xFFEAF7EF);
		context.drawTextWithShadow(client.textRenderer, Text.literal(config.radarScopeChunks + " CHUNKS"),
				left + SIZE - 67, top + 7, 0xFF8DA49A);
		context.fill(centerX - radius, centerY - radius, centerX + radius, centerY + radius, 0xA50C171B);
		drawRing(context, centerX, centerY, radius, 0xFF32443D);
		drawRing(context, centerX, centerY, radius * 2 / 3, 0xFF263831);
		drawRing(context, centerX, centerY, radius / 3, 0xFF263831);
		context.fill(centerX - radius, centerY, centerX + radius + 1, centerY + 1, 0x804B6658);
		context.fill(centerX, centerY - radius, centerX + 1, centerY + radius + 1, 0x804B6658);

		Vec3d playerPos = client.player.getPos();
		for (long packed : analyzer.flaggedChunks()) {
			int chunkX = ChunkPos.getPackedX(packed);
			int chunkZ = ChunkPos.getPackedZ(packed);
			drawPoint(context, centerX, centerY,
					centerX + ((chunkX << 4) + 8 - playerPos.x) * scale,
					centerY + ((chunkZ << 4) + 8 - playerPos.z) * scale,
					radius, config.flaggedChunkColor, 5);
		}
		for (List<ScanTarget> targets : analyzer.results().values()) {
			for (ScanTarget target : targets) {
				BlockPos pos = target.pos();
				int rgb = config.blockColors.getOrDefault(target.blockId(), config.chunkColor);
				drawPoint(context, centerX, centerY,
						centerX + (pos.getX() + 0.5 - playerPos.x) * scale,
						centerY + (pos.getZ() + 0.5 - playerPos.z) * scale, radius, rgb, 3);
			}
		}

		drawPlayer(context, client, centerX, centerY);
		context.drawTextWithShadow(client.textRenderer, Text.literal("N"), centerX - 3, centerY - radius - 9, 0xFFB8C6CC);
	}

	private static void drawRing(DrawContext context, int centerX, int centerY, int radius, int color) {
		for (int x = -radius; x <= radius; x++) {
			int y = (int) Math.round(Math.sqrt(radius * radius - x * x));
			context.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
			context.fill(centerX + x, centerY - y, centerX + x + 1, centerY - y + 1, color);
		}
	}

	private static void drawPoint(DrawContext context, int centerX, int centerY,
			double x, double y, int radius, int rgb, int size) {
		int px = (int) Math.round(x);
		int py = (int) Math.round(y);
		int dx = px - centerX;
		int dy = py - centerY;
		if (dx * dx + dy * dy > radius * radius) {
			return;
		}
		int color = (0xD2 << 24) | (rgb & 0xFFFFFF);
		context.fill(px - size / 2, py - size / 2, px + (size + 1) / 2, py + (size + 1) / 2, color);
	}

	private static void drawPlayer(DrawContext context, MinecraftClient client, int x, int y) {
		float yaw = client.player.getYaw();
		double radians = Math.toRadians(yaw);
		int tipX = x + (int) Math.round(Math.sin(radians) * 9);
		int tipY = y - (int) Math.round(Math.cos(radians) * 9);
		int steps = 9;
		for (int step = 0; step <= steps; step++) {
			int px = x + (tipX - x) * step / steps;
			int py = y + (tipY - y) * step / steps;
			context.fill(px, py, px + 2, py + 2, 0xFFFFFFFF);
		}
		context.fill(x - 3, y - 3, x + 4, y + 4, 0xFFFFFFFF);
		context.fill(x - 1, y - 1, x + 2, y + 2, 0xFF0B1419);
	}
}