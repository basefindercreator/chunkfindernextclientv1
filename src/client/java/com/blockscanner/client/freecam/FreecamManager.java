package com.blockscanner.client.freecam;

import com.blockscanner.client.BlockScannerClient;
import com.blockscanner.client.ScannerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class FreecamManager {
	private Entity camera;
	private Vec3 bodyPosition;
	private Vec3 bodyVelocity;
	private boolean previousNoGravity;
	private boolean active;
	private boolean[] heldMovement = new boolean[7];

	public boolean isActive() {
		return active;
	}

	public void toggle(Minecraft client) {
		if (active) {
			deactivate(client);
		} else {
			activate(client);
		}
	}

	public boolean activate(Minecraft client) {
		if (!BlockScannerClient.isLocalWorld(client) || active) {
			return false;
		}
		LocalPlayer player = client.player;
		Entity spawnedCamera = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("armor_stand"))
				.create(client.level, EntitySpawnReason.COMMAND);
		if (!(spawnedCamera instanceof ArmorStand cameraEntity)) {
			return false;
		}
		bodyPosition = player.position();
		bodyVelocity = player.getDeltaMovement();
		previousNoGravity = player.isNoGravity();
		player.setDeltaMovement(Vec3.ZERO);
		player.setNoGravity(true);
		cameraEntity.setPos(player.getX(), player.getEyeY(), player.getZ());
		cameraEntity.setYRot(player.getYRot());
		cameraEntity.setXRot(player.getXRot());
		cameraEntity.setNoGravity(true);
		cameraEntity.noPhysics = true;
		camera = cameraEntity;
		client.setCameraEntity(cameraEntity);
		active = true;
		return true;
	}

	public void onStartTick(Minecraft client) {
		if (!active) {
			return;
		}
		if (!BlockScannerClient.isLocalWorld(client)) {
			deactivate(client);
			return;
		}
		var options = client.options;
		heldMovement = new boolean[] {
				options.keyUp.isDown(), options.keyDown.isDown(), options.keyLeft.isDown(),
				options.keyRight.isDown(), options.keyJump.isDown(), options.keyShift.isDown(),
				options.keySprint.isDown()
		};
		setMovementKeys(options, false);
		pinBody(client.player);
	}

	public void onEndTick(Minecraft client) {
		if (!active || !(camera instanceof ArmorStand cameraEntity) || client.player == null) {
			return;
		}
		LocalPlayer player = client.player;
		pinBody(player);

		double forward = (heldMovement[0] ? 1.0 : 0.0) - (heldMovement[1] ? 1.0 : 0.0);
		double sideways = (heldMovement[3] ? 1.0 : 0.0) - (heldMovement[2] ? 1.0 : 0.0);
		double vertical = (heldMovement[4] ? 1.0 : 0.0) - (heldMovement[5] ? 1.0 : 0.0);
		double length = Math.sqrt(forward * forward + sideways * sideways + vertical * vertical);
		if (length > 0.0) {
			forward /= length;
			sideways /= length;
			vertical /= length;
			double yaw = Math.toRadians(cameraEntity.getYRot());
			double speed = ScannerConfig.get().freecamSpeed * (heldMovement[6] ? 2.0 : 1.0);
			double dx = (-Math.sin(yaw) * forward + Math.cos(yaw) * sideways) * speed;
			double dz = (Math.cos(yaw) * forward + Math.sin(yaw) * sideways) * speed;
			cameraEntity.setPos(cameraEntity.getX() + dx,
					cameraEntity.getY() + vertical * speed, cameraEntity.getZ() + dz);
		}
		setMovementKeys(client.options, true);
	}

	public void deactivate(Minecraft client) {
		if (!active) {
			return;
		}
		if (client.player != null) {
			client.player.setPos(bodyPosition);
			client.player.setDeltaMovement(bodyVelocity == null ? Vec3.ZERO : bodyVelocity);
			client.player.setNoGravity(previousNoGravity);
			client.setCameraEntity(client.player);
		}
		camera = null;
		bodyPosition = null;
		bodyVelocity = null;
		active = false;
		if (client.options != null) {
			setMovementKeys(client.options, true);
		}
	}

	private void pinBody(LocalPlayer player) {
		if (bodyPosition != null) {
			player.setPos(bodyPosition);
			player.setDeltaMovement(Vec3.ZERO);
			player.setNoGravity(true);
		}
	}

	private void setMovementKeys(Options options, boolean restore) {
		boolean[] values = restore ? heldMovement : new boolean[7];
		options.keyUp.setDown(values[0]);
		options.keyDown.setDown(values[1]);
		options.keyLeft.setDown(values[2]);
		options.keyRight.setDown(values[3]);
		options.keyJump.setDown(values[4]);
		options.keyShift.setDown(values[5]);
		options.keySprint.setDown(values[6]);
	}
}