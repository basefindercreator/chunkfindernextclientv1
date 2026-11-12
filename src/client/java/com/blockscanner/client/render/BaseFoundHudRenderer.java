package com.blockscanner.client.render;

import com.blockscanner.client.BlockScannerClient;
import com.blockscanner.client.ScannerConfig;
import com.blockscanner.client.scanner.ChunkPatternAnalyzer;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;

public final class BaseFoundHudRenderer {
	private static final int LIGHT_BLUE = 0xFF38BDF8;

	private BaseFoundHudRenderer() {
	}

	public static void render(GuiGraphicsExtractor context, ChunkPatternAnalyzer analyzer) {
		Minecraft client = Minecraft.getInstance();
		if (!BlockScannerClient.isLocalWorld(client) || !ScannerConfig.get().enabled || client.player == null) {
			return;
		}

		Set<Long> chunks = analyzer.baseFlaggedChunks();
		if (chunks.isEmpty()) {
			return;
		}

		long nearest = chunks.stream()
				.min((first, second) -> Double.compare(distanceSquared(first, client),
						distanceSquared(second, client)))
				.orElseThrow();
		int chunkX = ChunkPos.getX(nearest);
		int chunkZ = ChunkPos.getZ(nearest);
		double targetX = chunkX * 16.0 + 8.0;
		double targetZ = chunkZ * 16.0 + 8.0;
		double dx = targetX - client.player.getX();
		double dz = targetZ - client.player.getZ();
		int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
		String direction = direction(dx, dz);
		String guidance = "Chunk " + chunkX + ", " + chunkZ + "  |  " + distance + " blocks " + direction;
		int panelWidth = Math.max(client.font.width("BASE FOUND"),
				client.font.width(guidance)) + 28;
		int left = (context.guiWidth() - panelWidth) / 2;
		int top = 32;
		context.fill(left, top, left + panelWidth, top + 39, 0xE510202B);
		context.fill(left, top, left + 3, top + 39, LIGHT_BLUE);
		context.centeredText(client.font, Component.literal("BASE FOUND"),
				context.guiWidth() / 2, top + 5, LIGHT_BLUE);
		context.centeredText(client.font, Component.literal(guidance),
				context.guiWidth() / 2, top + 21, 0xFFE2F6FF);
	}

	private static double distanceSquared(long packedChunk, Minecraft client) {
		double x = ChunkPos.getX(packedChunk) * 16.0 + 8.0 - client.player.getX();
		double z = ChunkPos.getZ(packedChunk) * 16.0 + 8.0 - client.player.getZ();
		return x * x + z * z;
	}

	private static String direction(double dx, double dz) {
		String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
		double degrees = Math.toDegrees(Math.atan2(dx, -dz));
		int index = Math.floorMod((int) Math.round(degrees / 45.0), directions.length);
		return directions[index];
	}
}
