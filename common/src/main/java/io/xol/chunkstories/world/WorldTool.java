//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world;

import java.util.Iterator;
import java.util.Set;

import io.xol.chunkstories.api.graphics.systems.dispatching.DecalsManager;
import io.xol.chunkstories.api.world.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3dc;
import org.joml.Vector3fc;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.sound.SoundSource;
import io.xol.chunkstories.api.sound.SoundSource.Mode;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.sound.source.DummySoundSource;
import io.xol.chunkstories.world.io.IOTasks;
import io.xol.chunkstories.world.io.IOTasksImmediate;

public class WorldTool extends WorldImplementation implements WorldMaster {
	private final GameContext toolContext;

	final IOTasks ioHandler;
	private boolean isLightningEnabled = false;

	public WorldTool(GameContext toolContext, WorldInfo info) throws WorldLoadingException {
		this(toolContext, info, true);
	}

	public WorldTool(GameContext toolContext, WorldInfo info, boolean immediateIO)
			throws WorldLoadingException {
		super(toolContext, info, null, null);

		this.toolContext = toolContext;

		if (immediateIO)
			ioHandler = (new IOTasksImmediate(this));
		else {
			// Normal IO.
			ioHandler = (new IOTasks(this));
			getIoHandler().start();
		}
		// ioHandler.start();
	}

	/*
	 * public WorldTool(GameContext toolContext, File csWorldDir) {
	 * this(toolContext, csWorldDir.getAbsolutePath()); }
	 */

	public GameContext getGameContext() {
		return toolContext;
	}

	@Override
	public SoundManager getSoundManager() {
		return nullSoundManager;
	}

	NullSoundManager nullSoundManager = new NullSoundManager();

	@NotNull
	@Override
	public IOTasks getIoHandler() {
		return ioHandler;
	}

	class NullSoundManager implements SoundManager {

		@Override
		public SoundSource playSoundEffect(String soundEffect) {

			return null;
		}

		@Override
		public void stopAnySound(String soundEffect) {

		}

		@Override
		public void stopAnySound() {
		}

		@Override
		public Iterator<SoundSource> getAllPlayingSounds() {
			return null;
		}

		@Override
		public void setListenerPosition(float x, float y, float z, Vector3fc lookAt, Vector3fc up) {
		}

		@Override
		public SoundSource playSoundEffect(String soundEffect, Mode mode, Vector3dc position, float pitch, float gain,
				float attStart, float attEnd) {
			// TODO Auto-generated method stub
			return new DummySoundSource();
		}

	}

	@Override
	public ParticlesManager getParticlesManager() {
		return nullParticlesManager;
	}

	NullParticlesManager nullParticlesManager = new NullParticlesManager();

	static class NullParticlesManager implements ParticlesManager {

		@Override
		public void spawnParticleAtPosition(String particleTypeName, Vector3dc location) {
			// TODO Auto-generated method stub

		}

		@Override
		public void spawnParticleAtPositionWithVelocity(String particleTypeName, Vector3dc location,
				Vector3dc velocity) {
			// TODO Auto-generated method stub

		}
	}

	@Override
	public DecalsManager getDecalsManager() {
		return nullDecalsManager;
	}

	NullDecalsManager nullDecalsManager = new NullDecalsManager();

	static class NullDecalsManager implements DecalsManager {

		@Override
		public void add(@NotNull Vector3dc vector3dc, @NotNull Vector3dc vector3dc1, @NotNull Vector3dc vector3dc2, @NotNull String s) {

		}
	}

	@Override
	public void spawnPlayer(Player player) {
		throw new UnsupportedOperationException("spawnPlayer");
	}

	@Override
	public Set<Player> getPlayers() {
		throw new UnsupportedOperationException("getPlayers");
	}

	@Override
	public Player getPlayerByName(String playerName) {
		throw new UnsupportedOperationException("getPlayers");
	}

	public boolean isLightningEnabled() {
		return isLightningEnabled;
	}

	public void setLightning(boolean e) {
		isLightningEnabled = e;
	}
}
