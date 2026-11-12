package com.blockscanner.client.scanner;

import net.minecraft.util.math.BlockPos;

public record ScanTarget(BlockPos pos, Kind kind, String blockId) {
	public enum Kind {
		CHEST,
		HOPPER,
		CONTAINER_CLUSTER
	}
}