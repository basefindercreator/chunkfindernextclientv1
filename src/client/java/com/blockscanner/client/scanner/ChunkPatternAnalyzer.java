package com.blockscanner.client.scanner;

import com.blockscanner.client.BlockScannerClient;
import com.blockscanner.client.ScannerConfig;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class ChunkPatternAnalyzer {
	private static final int CHUNKS_PER_TICK = 2;
	private static final Set<Block> BASE_BLOCKS = createBaseBlocks();
	private final Map<Long, List<ScanTarget>> results = new HashMap<>();
	private final Set<Long> flaggedChunks = new HashSet<>();
	private final Set<Long> baseFlaggedChunks = new HashSet<>();
	private int cursorX;
	private int cursorZ;
	private int currentRadius;
	private int currentCenterX = Integer.MIN_VALUE;
	private int currentCenterZ = Integer.MIN_VALUE;

	public void tick(Minecraft client) {
		if (!BlockScannerClient.isLocalWorld(client) || !ScannerConfig.get().enabled || client.player == null) {
			clear();
			return;
		}
		int radius = Math.max(1, Math.min(100, ScannerConfig.get().radarScopeChunks));
		int centerX = client.player.chunkPosition().x();
		int centerZ = client.player.chunkPosition().z();
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
			LevelChunk chunk = client.level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
			if (chunk != null) {
				analyzeChunk(chunk, client.level.getMinY(), client.level.getHeight());
				scanned++;
			}
		}
		long maxDistance = (long) (radius + 1) * (radius + 1);
		results.keySet().removeIf(key -> distanceSquared(key, centerX, centerZ) > maxDistance);
		flaggedChunks.removeIf(key -> distanceSquared(key, centerX, centerZ) > maxDistance);
		baseFlaggedChunks.removeIf(key -> distanceSquared(key, centerX, centerZ) > maxDistance);
	}

	private void analyzeChunk(LevelChunk chunk, int bottomY, int worldHeight) {
		ChunkPos chunkPos = chunk.getPos();
		List<BlockPos> chests = new ArrayList<>();
		List<BlockPos> hoppers = new ArrayList<>();
		Map<Block, String> watchedBlocks = watchedBlocks();
		Map<BlockPos, String> blockIds = new HashMap<>();
		Map<BlockPos, Block> foundBlocks = new HashMap<>();
		Set<BlockPos> basePositions = new HashSet<>();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
			for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
				for (int y = bottomY; y < bottomY + worldHeight; y++) {
					pos.set(x, y, z);
					Block block = chunk.getBlockState(pos).getBlock();
					String blockId = watchedBlocks.get(block);
					if (blockId == null) {
						continue;
					}
					BlockPos immutablePos = pos.immutable();
					blockIds.put(immutablePos, blockId);
					foundBlocks.put(immutablePos, block);
					if (BASE_BLOCKS.contains(block)) {
						basePositions.add(immutablePos);
					}
					if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) {
						chests.add(immutablePos);
					} else if (block == Blocks.HOPPER) {
						hoppers.add(immutablePos);
					}
				}
			}
		}
		List<ScanTarget> chunkResults = new ArrayList<>();
		Set<BlockPos> paired = new HashSet<>();
		Set<BlockPos> chestPositions = Set.copyOf(chests);
		for (BlockPos chest : chests) {
			boolean hasNeighbor = false;
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				BlockPos neighbor = chest.relative(direction);
				if (chestPositions.contains(neighbor)) {
					hasNeighbor = true;
					paired.add(neighbor.immutable());
				}
			}
			chunkResults.add(new ScanTarget(chest,
					hasNeighbor ? ScanTarget.Kind.CONTAINER_CLUSTER : ScanTarget.Kind.CHEST, blockIds.get(chest)));
		}
		for (BlockPos hopper : hoppers) {
			chunkResults.add(new ScanTarget(hopper, ScanTarget.Kind.HOPPER, blockIds.get(hopper)));
		}
		for (Map.Entry<BlockPos, String> entry : blockIds.entrySet()) {
			Block block = foundBlocks.get(entry.getKey());
			if (block != Blocks.CHEST && block != Blocks.TRAPPED_CHEST && block != Blocks.HOPPER) {
				chunkResults.add(new ScanTarget(entry.getKey(), ScanTarget.Kind.BLOCK, entry.getValue()));
			}
		}
		long key = chunkPos.pack();
		int doubleChestPairs = paired.size() / 2;
		boolean baseLike = isBaseLike(basePositions, doubleChestPairs);
		if (baseLike) {
			baseFlaggedChunks.add(key);
		} else {
			baseFlaggedChunks.remove(key);
		}
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

	private static Map<Block, String> watchedBlocks() {
		Map<Block, String> watched = new HashMap<>();
		// Keep the built-in chest and hopper checks available even if their color entries are removed.
		watched.put(Blocks.CHEST, "minecraft:chest");
		watched.put(Blocks.TRAPPED_CHEST, "minecraft:trapped_chest");
		watched.put(Blocks.HOPPER, "minecraft:hopper");
		for (Block block : BASE_BLOCKS) {
			watched.put(block, BuiltInRegistries.BLOCK.getKey(block).toString());
		}
		for (String id : ScannerConfig.get().blockColors.keySet()) {
			Identifier identifier = Identifier.tryParse(id);
			if (identifier != null && BuiltInRegistries.BLOCK.containsKey(identifier)) {
				watched.put(BuiltInRegistries.BLOCK.getValue(identifier), id);
			}
		}
		return watched;
	}

	private static Set<Block> createBaseBlocks() {
		Set<Block> blocks = new HashSet<>(List.of(
				Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.BARREL, Blocks.HOPPER,
				Blocks.DISPENSER, Blocks.DROPPER, Blocks.OBSERVER, Blocks.PISTON, Blocks.STICKY_PISTON,
				Blocks.PISTON_HEAD, Blocks.SHULKER_BOX, Blocks.GLASS, Blocks.TNT, Blocks.REDSTONE_BLOCK));
		for (Identifier id : BuiltInRegistries.BLOCK.keySet()) {
			String path = id.getPath();
			if (path.endsWith("_shulker_box") || path.endsWith("_glass")
					|| path.equals("glass_pane") || path.endsWith("_glass_pane")) {
				blocks.add(BuiltInRegistries.BLOCK.getValue(id));
			}
		}
		return Set.copyOf(blocks);
	}

	private static boolean isBaseLike(Set<BlockPos> positions, int doubleChestPairs) {
		return doubleChestPairs >= 2 || largestNearbyCluster(positions) >= 4;
	}

	private static int largestNearbyCluster(Set<BlockPos> positions) {
		Set<BlockPos> remaining = new HashSet<>(positions);
		int largest = 0;
		while (!remaining.isEmpty()) {
			BlockPos start = remaining.iterator().next();
			remaining.remove(start);
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			queue.add(start);
			int size = 0;
			while (!queue.isEmpty()) {
				BlockPos current = queue.removeFirst();
				size++;
				for (int dx = -1; dx <= 1; dx++) {
					for (int dy = -1; dy <= 1; dy++) {
						for (int dz = -1; dz <= 1; dz++) {
							if (dx == 0 && dy == 0 && dz == 0) {
								continue;
							}
							BlockPos neighbor = current.offset(dx, dy, dz);
							if (remaining.remove(neighbor)) {
								queue.addLast(neighbor);
							}
						}
					}
				}
			}
			largest = Math.max(largest, size);
		}
		return largest;
	}

	public Map<Long, List<ScanTarget>> results() {
		return Map.copyOf(results);
	}

	public Set<Long> flaggedChunks() {
		return Set.copyOf(flaggedChunks);
	}

	public Set<Long> baseFlaggedChunks() {
		return Set.copyOf(baseFlaggedChunks);
	}

	public void clear() {
		results.clear();
		flaggedChunks.clear();
		baseFlaggedChunks.clear();
		cursorX = 0;
		cursorZ = 0;
	}

	private static long distanceSquared(long packed, int x, int z) {
		int chunkX = ChunkPos.getX(packed);
		int chunkZ = ChunkPos.getZ(packed);
		long dx = (long) chunkX - x;
		long dz = (long) chunkZ - z;
		return dx * dx + dz * dz;
	}
}