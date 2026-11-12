package com.blockscanner.client.scanner;

import com.blockscanner.client.BlockScannerClient;
import com.blockscanner.client.ScannerConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.registry.Registries;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

public final class ChunkPatternAnalyzer {
	private static final int CHUNKS_PER_TICK = 2;
	private final Map<Long, List<ScanTarget>> results = new HashMap<>();
	private final Set<Long> flaggedChunks = new HashSet<>();
	private int cursorX;
	private int cursorZ;
	private int currentRadius;
	private int currentCenterX = Integer.MIN_VALUE;
	private int currentCenterZ = Integer.MIN_VALUE;

	public void tick(MinecraftClient client) {
		if (!BlockScannerClient.isLocalWorld(client) || !ScannerConfig.get().enabled || client.player == null) {
			clear();
			return;
		}
		int radius = Math.min(ScannerConfig.get().radarScopeChunks, client.options.getViewDistance().getValue());
		int centerX = client.player.getChunkPos().x;
		int centerZ = client.player.getChunkPos().z;
		if (radius != currentRadius || centerX != currentCenterX || centerZ != currentCenterZ) {
			currentRadius = radius;
			currentCenterX = centerX;
			currentCenterZ = centerZ;
			cursorX = centerX - radius;
			cursorZ = centerZ - radius;
		}
		int scanned = 0;
		int attempts = 0;
		while (scanned < CHUNKS_PER_TICK && attempts++ < 128) {
			int x = cursorX;
			int z = cursorZ;
			cursorX++;
			if (cursorX > centerX + radius) {
				cursorX = centerX - radius;
				cursorZ++;
				if (cursorZ > centerZ + radius) {
					cursorZ = centerZ - radius;
				}
			}
			WorldChunk chunk = client.world.getChunkManager().getChunk(x, z, ChunkStatus.FULL, false);
			if (chunk != null) {
				analyzeChunk(chunk);
				scanned++;
			}
		}
		long maxDistance = (long) (radius + 1) * (radius + 1);
		results.keySet().removeIf(key -> distanceSquared(key, centerX, centerZ) > maxDistance);
		flaggedChunks.removeIf(key -> distanceSquared(key, centerX, centerZ) > maxDistance);
	}

	private void analyzeChunk(WorldChunk chunk) {
		ChunkPos chunkPos = chunk.getPos();
		List<BlockPos> chests = new ArrayList<>();
		List<BlockPos> hoppers = new ArrayList<>();
		Map<BlockPos, String> blockIds = new HashMap<>();
		for (BlockPos pos : chunk.getBlockEntities().keySet()) {
			var block = chunk.getBlockState(pos).getBlock();
			if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) {
				chests.add(pos.toImmutable());
				blockIds.put(pos.toImmutable(), Registries.BLOCK.getId(block).toString());
			} else if (block == Blocks.HOPPER) {
				hoppers.add(pos.toImmutable());
				blockIds.put(pos.toImmutable(), Registries.BLOCK.getId(block).toString());
			}
		}
		List<ScanTarget> chunkResults = new ArrayList<>();
		Set<BlockPos> paired = new HashSet<>();
		for (BlockPos chest : chests) {
			boolean hasNeighbor = false;
			for (Direction direction : Direction.Type.HORIZONTAL) {
				BlockPos neighbor = chest.offset(direction);
				if (chunk.getBlockState(neighbor).isOf(Blocks.CHEST)
						|| chunk.getBlockState(neighbor).isOf(Blocks.TRAPPED_CHEST)) {
					hasNeighbor = true;
					paired.add(neighbor.toImmutable());
				}
			}
			chunkResults.add(new ScanTarget(chest,
					hasNeighbor ? ScanTarget.Kind.CONTAINER_CLUSTER : ScanTarget.Kind.CHEST, blockIds.get(chest)));
		}
		for (BlockPos hopper : hoppers) {
			chunkResults.add(new ScanTarget(hopper, ScanTarget.Kind.HOPPER, blockIds.get(hopper)));
		}
		long key = ChunkPos.toLong(chunkPos.x, chunkPos.z);
		if (chunkResults.isEmpty()) {
			results.remove(key);
			flaggedChunks.remove(key);
		} else {
			results.put(key, List.copyOf(chunkResults));
			if (!paired.isEmpty() || hoppers.size() >= 3) {
				flaggedChunks.add(key);
			} else {
				flaggedChunks.remove(key);
			}
		}
	}

	public Map<Long, List<ScanTarget>> results() {
		return Map.copyOf(results);
	}

	public Set<Long> flaggedChunks() {
		return Set.copyOf(flaggedChunks);
	}

	public void clear() {
		results.clear();
		flaggedChunks.clear();
		cursorX = 0;
		cursorZ = 0;
	}

	private static long distanceSquared(long packed, int x, int z) {
		int chunkX = ChunkPos.getPackedX(packed);
		int chunkZ = ChunkPos.getPackedZ(packed);
		long dx = (long) chunkX - x;
		long dz = (long) chunkZ - z;
		return dx * dx + dz * dz;
	}
}