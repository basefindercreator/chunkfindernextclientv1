package com.blockscanner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class PlayerActivityTracker {
	private static final long WINDOW_MILLIS = 5 * 60 * 1000L;
	private static final int REPLACEMENT_THRESHOLD = 8;
	private static final Map<ServerLevel, Map<Long, ChunkActivity>> ACTIVITY = new HashMap<>();
	private static final Map<ServerLevel, List<PendingPlacement>> PENDING_PLACEMENTS = new HashMap<>();

	private PlayerActivityTracker() {
	}

	public static void initialize() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
				recordBreak(serverLevel, serverPlayer, pos, state);
			}
		});

		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!level.isClientSide() && level instanceof ServerLevel serverLevel
					&& player instanceof ServerPlayer serverPlayer
					&& serverPlayer.getItemInHand(hand).getItem() instanceof BlockItem) {
				BlockPos clicked = hit.getBlockPos().immutable();
				BlockPos adjacent = clicked.relative(hit.getDirection()).immutable();
				PENDING_PLACEMENTS.computeIfAbsent(serverLevel, ignored -> new ArrayList<>()).add(
						new PendingPlacement(serverPlayer, clicked, level.getBlockState(clicked),
								adjacent, level.getBlockState(adjacent)));
			}
			return InteractionResult.PASS;
		});

		ServerTickEvents.END_LEVEL_TICK.register(PlayerActivityTracker::finishTick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			ACTIVITY.clear();
			PENDING_PLACEMENTS.clear();
		});
	}

	private static void recordBreak(ServerLevel world, ServerPlayer player, BlockPos pos, BlockState state) {
		long key = ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
		ChunkActivity activity = ACTIVITY.computeIfAbsent(world, ignored -> new HashMap<>())
				.computeIfAbsent(key, ignored -> new ChunkActivity());
		long time = System.currentTimeMillis();
		activity.prune(time);
		activity.recentBreaks.put(pos.immutable(),
				new BreakRecord(time, player.getName().getString(), blockId(state)));
		activity.actions.addLast(new Action(time, false));
	}

	private static void finishTick(ServerLevel world) {
		List<PendingPlacement> pending = PENDING_PLACEMENTS.remove(world);
		if (pending != null) {
			for (PendingPlacement placement : pending) {
				BlockState clickedAfter = world.getBlockState(placement.clicked());
				if (!clickedAfter.equals(placement.clickedBefore()) && !clickedAfter.isAir()) {
					recordPlace(world, placement.player(), placement.clicked(), clickedAfter);
					continue;
				}
				BlockState adjacentAfter = world.getBlockState(placement.adjacent());
				if (!adjacentAfter.equals(placement.adjacentBefore()) && !adjacentAfter.isAir()) {
					recordPlace(world, placement.player(), placement.adjacent(), adjacentAfter);
				}
			}
		}

		Map<Long, ChunkActivity> chunks = ACTIVITY.get(world);
		if (chunks != null) {
			long time = System.currentTimeMillis();
			chunks.values().forEach(activity -> activity.prune(time));
			chunks.values().removeIf(ChunkActivity::isEmpty);
			if (chunks.isEmpty()) {
				ACTIVITY.remove(world);
			}
		}
	}

	private static void recordPlace(ServerLevel world, ServerPlayer player, BlockPos pos, BlockState state) {
		long key = ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
		ChunkActivity activity = ACTIVITY.computeIfAbsent(world, ignored -> new HashMap<>())
				.computeIfAbsent(key, ignored -> new ChunkActivity());
		long time = System.currentTimeMillis();
		activity.prune(time);
		activity.actions.addLast(new Action(time, true));

		BreakRecord broken = activity.recentBreaks.remove(pos);
		if (broken == null || time - broken.time() > WINDOW_MILLIS) {
			return;
		}

		String placedBlock = blockId(state);
		Replacement replacement = new Replacement(time, broken.blockId(), placedBlock,
				broken.player(), player.getName().getString());
		activity.replacements.addLast(replacement);
		long matchingReplacements = activity.replacements.stream()
				.filter(event -> event.brokenBlockId().equals(broken.blockId())
						&& event.placedBlockId().equals(placedBlock)).count();
		String pattern = patternId(broken.blockId(), placedBlock);
		if (matchingReplacements >= REPLACEMENT_THRESHOLD && activity.alertedPatterns.add(pattern)) {
			alertOperators(world, key, activity, broken.blockId(), placedBlock, matchingReplacements, replacement);
		}
	}

	private static void alertOperators(ServerLevel world, long packedChunk, ChunkActivity activity,
			String brokenBlockId, String placedBlockId, long matchingReplacements, Replacement latest) {
		int x = ChunkPos.getX(packedChunk);
		int z = ChunkPos.getZ(packedChunk);
		long breaks = activity.actions.stream().filter(action -> !action.placed()).count();
		long places = activity.actions.stream().filter(Action::placed).count();
		String message = "[BlockScanner] Activity flagged in chunk [" + x + ", " + z + "]: "
				+ matchingReplacements + " replacements of " + brokenBlockId + " with " + placedBlockId
				+ " in 5 minutes; " + breaks + " blocks broken and " + places + " placed. Latest edit: "
				+ latest.breaker() + " broke, " + latest.placer() + " placed.";
		ModScannerMod.LOGGER.warn(message);
		for (ServerPlayer operator : world.players()) {
			if (operator.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)) {
				operator.sendSystemMessage(Component.literal(message), false);
			}
		}
	}

	private static String blockId(BlockState state) {
		return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
	}

	private static String patternId(String brokenBlockId, String placedBlockId) {
		return brokenBlockId + "->" + placedBlockId;
	}

	private record PendingPlacement(ServerPlayer player, BlockPos clicked, BlockState clickedBefore,
			BlockPos adjacent, BlockState adjacentBefore) {
	}

	private record BreakRecord(long time, String player, String blockId) {
	}

	private record Replacement(long time, String brokenBlockId, String placedBlockId,
			String breaker, String placer) {
	}

	private record Action(long time, boolean placed) {
	}

	private static final class ChunkActivity {
		private final ArrayDeque<Action> actions = new ArrayDeque<>();
		private final ArrayDeque<Replacement> replacements = new ArrayDeque<>();
		private final Map<BlockPos, BreakRecord> recentBreaks = new HashMap<>();
		private final Set<String> alertedPatterns = new HashSet<>();

		private void prune(long currentTime) {
			while (!actions.isEmpty() && currentTime - actions.peekFirst().time() > WINDOW_MILLIS) {
				actions.removeFirst();
			}
			while (!replacements.isEmpty()
					&& currentTime - replacements.peekFirst().time() > WINDOW_MILLIS) {
				replacements.removeFirst();
			}
			recentBreaks.values().removeIf(record -> currentTime - record.time() > WINDOW_MILLIS);
			alertedPatterns.removeIf(pattern ->
					replacements.stream().noneMatch(event -> patternId(event.brokenBlockId(),
							event.placedBlockId()).equals(pattern)));
		}

		private boolean isEmpty() {
			return actions.isEmpty() && replacements.isEmpty() && recentBreaks.isEmpty();
		}
	}
}
