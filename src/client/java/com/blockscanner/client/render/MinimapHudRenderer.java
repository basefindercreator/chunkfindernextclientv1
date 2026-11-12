package com.blockscanner.client.render;

import com.blockscanner.client.BlockScannerClient;
import com.blockscanner.client.ScannerConfig;
import com.blockscanner.client.scanner.ChunkPatternAnalyzer;
import com.blockscanner.client.scanner.ScanTarget;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public final class MinimapHudRenderer {
	private static final int SIZE = 142;
	private static final int MAX_TRACERS = 128;

	private MinimapHudRenderer() {
	}

	public static void render(GuiGraphicsExtractor context, ChunkPatternAnalyzer analyzer) {
		Minecraft client = Minecraft.getInstance();
		ScannerConfig config = ScannerConfig.get();
		if (!BlockScannerClient.isLocalWorld(client) || !config.enabled || !config.minimapEnabled) {
			return;
		}

		int left = context.guiWidth() - SIZE - 12;
		int top = context.guiHeight() - SIZE - 30;
		int centerX = left + SIZE / 2;
		int centerY = top + SIZE / 2 + 9;
		int radius = 54;
		int rangeBlocks = Math.max(16, config.radarScopeChunks * 16);
		double scale = radius / (double) rangeBlocks;

		context.fill(left, top, left + SIZE, top + SIZE + 18, 0xD90B1419);
		context.fill(left, top, left + SIZE, top + 2, 0xFF42F56C);
		context.text(client.font, Component.literal("LOCAL RADAR"), left + 9, top + 7, 0xFFEAF7EF);
		context.text(client.font, Component.literal(config.radarScopeChunks + " CHUNKS"),
				left + SIZE - 67, top + 7, 0xFF8DA49A);
		context.fill(centerX - radius, centerY - radius, centerX + radius, centerY + radius, 0xA50C171B);
		drawRing(context, centerX, centerY, radius, 0xFF32443D);
		drawRing(context, centerX, centerY, radius * 2 / 3, 0xFF263831);
		drawRing(context, centerX, centerY, radius / 3, 0xFF263831);
		context.fill(centerX - radius, centerY, centerX + radius + 1, centerY + 1, 0x804B6658);
		context.fill(centerX, centerY - radius, centerX + 1, centerY + radius + 1, 0x804B6658);

		Vec3 playerPos = client.player.position();
		for (long packed : analyzer.flaggedChunks()) {
			int chunkX = ChunkPos.getX(packed);
			int chunkZ = ChunkPos.getZ(packed);
			drawChunkOutline(context, centerX, centerY, radius, scale, playerPos, chunkX, chunkZ,
					config.flaggedChunkColor);
			drawPoint(context, centerX, centerY,
					centerX + ((chunkX << 4) + 8 - playerPos.x) * scale,
					centerY + ((chunkZ << 4) + 8 - playerPos.z) * scale,
					radius, config.flaggedChunkColor, 5);
		}
		for (long packed : analyzer.baseFlaggedChunks()) {
			int chunkX = ChunkPos.getX(packed);
			int chunkZ = ChunkPos.getZ(packed);
			drawChunkOutline(context, centerX, centerY, radius, scale, playerPos, chunkX, chunkZ, 0x38BDF8);
			drawPoint(context, centerX, centerY,
					centerX + ((chunkX << 4) + 8 - playerPos.x) * scale,
					centerY + ((chunkZ << 4) + 8 - playerPos.z) * scale,
					radius, 0x38BDF8, 6);
		}
		int tracerCount = 0;
		for (List<ScanTarget> targets : analyzer.results().values()) {
			for (ScanTarget target : targets) {
				if (!config.blockColors.containsKey(target.blockId())) {
					continue;
				}
				BlockPos pos = target.pos();
				int rgb = target.kind() == ScanTarget.Kind.CONTAINER_CLUSTER
						? config.doubleChestColor
						: config.blockColors.getOrDefault(target.blockId(), config.chunkColor);
				double targetX = centerX + (pos.getX() + 0.5 - playerPos.x) * scale;
				double targetY = centerY + (pos.getZ() + 0.5 - playerPos.z) * scale;
				if (isInsideCircle(centerX, centerY, radius, targetX, targetY)) {
					if (tracerCount++ < MAX_TRACERS) {
						drawMapLine(context, centerX, centerY, (int) Math.round(targetX),
								(int) Math.round(targetY), centerX, centerY, radius, 0xB0000000 | (rgb & 0xFFFFFF));
					}
					drawPoint(context, centerX, centerY, targetX, targetY, radius, rgb, 3);
				}
			}
		}

		drawPlayer(context, client, centerX, centerY);
		context.text(client.font, Component.literal("N"), centerX - 3, centerY - radius - 9, 0xFFB8C6CC);
	}

	private static void drawChunkOutline(GuiGraphicsExtractor context, int centerX, int centerY, int radius,
			double scale, Vec3 playerPos, int chunkX, int chunkZ, int color) {
		double left = centerX + (chunkX * 16.0 - playerPos.x) * scale;
		double right = centerX + ((chunkX + 1) * 16.0 - playerPos.x) * scale;
		double top = centerY + (chunkZ * 16.0 - playerPos.z) * scale;
		double bottom = centerY + ((chunkZ + 1) * 16.0 - playerPos.z) * scale;
		int x1 = (int) Math.round(left);
		int x2 = (int) Math.round(right);
		int y1 = (int) Math.round(top);
		int y2 = (int) Math.round(bottom);
		drawMapLine(context, x1, y1, x2, y1, centerX, centerY, radius, color);
		drawMapLine(context, x2, y1, x2, y2, centerX, centerY, radius, color);
		drawMapLine(context, x2, y2, x1, y2, centerX, centerY, radius, color);
		drawMapLine(context, x1, y2, x1, y1, centerX, centerY, radius, color);
	}

	private static void drawMapLine(GuiGraphicsExtractor context, int x1, int y1, int x2, int y2,
			int centerX, int centerY, int radius, int color) {
		int dx = Math.abs(x2 - x1);
		int dy = Math.abs(y2 - y1);
		int steps = Math.max(dx, dy);
		if (steps == 0) {
			if (isInsideCircle(centerX, centerY, radius, x1, y1)) {
				context.fill(x1, y1, x1 + 1, y1 + 1, color);
			}
			return;
		}
		for (int step = 0; step <= steps; step++) {
			int x = x1 + (x2 - x1) * step / steps;
			int y = y1 + (y2 - y1) * step / steps;
			if (isInsideCircle(centerX, centerY, radius, x, y)) {
				context.fill(x, y, x + 1, y + 1, color);
			}
		}
	}

	private static boolean isInsideCircle(int centerX, int centerY, int radius, double x, double y) {
		double dx = x - centerX;
		double dy = y - centerY;
		return dx * dx + dy * dy <= radius * radius;
	}

	private static void drawRing(GuiGraphicsExtractor context, int centerX, int centerY, int radius, int color) {
		for (int x = -radius; x <= radius; x++) {
			int y = (int) Math.round(Math.sqrt(radius * radius - x * x));
			context.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
			context.fill(centerX + x, centerY - y, centerX + x + 1, centerY - y + 1, color);
		}
	}

	private static void drawPoint(GuiGraphicsExtractor context, int centerX, int centerY,
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

	private static void drawPlayer(GuiGraphicsExtractor context, Minecraft client, int x, int y) {
		float yaw = client.player.getYRot();
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