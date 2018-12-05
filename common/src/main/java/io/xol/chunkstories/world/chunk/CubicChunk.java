//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.chunk;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import io.xol.chunkstories.world.chunk.deriveddata.AutoRebuildingProperty;
import io.xol.chunkstories.world.chunk.deriveddata.ChunkOcclusionProperty;
import io.xol.chunkstories.world.storage.ChunkHolderImplementation;
import org.joml.Vector3dc;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.events.voxel.WorldModificationCause;
import io.xol.chunkstories.api.exceptions.world.WorldException;
import io.xol.chunkstories.api.net.packets.PacketVoxelUpdate;
import io.xol.chunkstories.api.server.RemotePlayer;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.util.IterableIteratorWrapper;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSide;
import io.xol.chunkstories.api.voxel.components.VoxelComponent;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.WorldUser;
import io.xol.chunkstories.api.world.cell.Cell;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.cell.FutureCell;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.ChunkLightUpdater;
import io.xol.chunkstories.api.world.heightmap.Heightmap;
import io.xol.chunkstories.api.world.region.Region;
import io.xol.chunkstories.entity.EntitySerializer;
import io.xol.chunkstories.util.concurrency.SimpleLock;
import io.xol.chunkstories.voxel.components.CellComponentsHolder;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.WorldTool;
import io.xol.chunkstories.world.storage.RegionImplementation;

import javax.annotation.Nonnull;

/**
 * Essential class that holds actual chunk voxel data, entities and voxel
 * component !
 */
public class CubicChunk implements Chunk {
	final protected WorldImplementation world;
	final protected RegionImplementation holdingRegion;
	final protected ChunkHolderImplementation chunkHolder;

	final protected int chunkX;
	final protected int chunkY;
	final protected int chunkZ;
	final protected int uuid;

	// Actual data holding here
	public int[] voxelDataArray = null;

	// Count unsaved edits atomically, fancy :]
	public final AtomicInteger compressionUncommitedModifications = new AtomicInteger();

	public final ChunkOcclusionManager occlusion;
	public final ChunkLightBaker lightingManager;

	// Set to true after destroy()
	public boolean isDestroyed = false;
	public final Semaphore chunkDestructionSemaphore = new Semaphore(1);

	@Nonnull
	public ChunkMesh meshData;

	public final Map<Integer, CellComponentsHolder> allCellComponents = new HashMap<Integer, CellComponentsHolder>();
	public final Set<Entity> localEntities = ConcurrentHashMap.newKeySet();

	//TODO use semaphores/RW locks
	public final SimpleLock componentsLock = new SimpleLock();
	public final SimpleLock entitiesLock = new SimpleLock();

	private Semaphore chunkDataArrayCreation = new Semaphore(1);

	public static final AtomicInteger chunksCounter = new AtomicInteger(0);

	public CubicChunk(ChunkHolderImplementation holder, int chunkX, int chunkY, int chunkZ, CompressedData data) {
		chunksCounter.incrementAndGet();

		this.chunkHolder = holder;

		this.holdingRegion = holder.getRegion();
		this.world = holdingRegion.getWorld();

		this.chunkX = chunkX;
		this.chunkY = chunkY;
		this.chunkZ = chunkZ;

		this.uuid = ((chunkX << world.getWorldInfo().getSize().bitlengthOfVerticalChunksCoordinates) | chunkY) << world
				.getWorldInfo().getSize().bitlengthOfHorizontalChunksCoordinates | chunkZ;

		occlusion = new ChunkOcclusionProperty(this);
		lightingManager = new ChunkLightBaker(this);

		if (data != null) {
			try {
				this.voxelDataArray = data.getVoxelData();

				if (data.voxelComponentsCompressedData != null) {
					ByteArrayInputStream bais = new ByteArrayInputStream(data.voxelComponentsCompressedData);
					DataInputStream dis = new DataInputStream(bais);

					byte[] smallArray = new byte[4096];
					ByteArrayInputStream bias = new ByteArrayInputStream(smallArray);
					DataInputStream dias = new DataInputStream(bias);

					byte keepGoing = dis.readByte();
					while (keepGoing != 0x00) {
						int index = dis.readInt();
						CellComponentsHolder components = new CellComponentsHolder(this, index);
						allCellComponents.put(index, components);

						// Call the block's onPlace method as to make it spawn the necessary components
						FreshChunkCell peek = peek(components.getX(), components.getY(), components.getZ());
						// System.out.println("peek"+peek);
						FreshFutureCell future = new FreshFutureCell(peek);
						// System.out.println("future"+future);

						peek.getVoxel().whenPlaced(future);
						// System.out.println("future comps"+future.components().getX() + ":" +
						// future.components().getY() + ": " + future.components().getZ());

						String componentName = dis.readUTF();
						while (!componentName.equals("\n")) {
							// System.out.println("componentName: "+componentName);

							// Read however many bytes this component wrote
							int bytes = dis.readShort();
							dis.readFully(smallArray, 0, bytes);

							VoxelComponent component = components.getVoxelComponent(componentName);
							if (component == null) {
								System.out.println("Error, a component named " + componentName
										+ " was saved, but it was not recreated by the voxel whenPlaced() method.");
							} else {
								// Hope for the best
								// System.out.println("called pull on "+component.getClass());
								component.pull(holder.getRegion().getHandler(), dias);
							}

							dias.reset();
							componentName = dis.readUTF();
						}
						keepGoing = dis.readByte();
					}
				}

				if (data.entitiesCompressedData != null) {
					ByteArrayInputStream bais = new ByteArrayInputStream(data.entitiesCompressedData);
					DataInputStream dis = new DataInputStream(bais);

					// Read entities until we hit -1
					Entity entity = null;
					do {
						entity = EntitySerializer.readEntityFromStream(dis, holder.getRegion().getHandler(), world);
						if (entity != null) {
							this.addEntity(entity);
							world.addEntity(entity);
						}
					} while (entity != null);
				}
			} catch (UnloadableChunkDataException | IOException e) {

				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}

		meshData = DummyChunkRenderingData.INSTANCE;

		// Send chunk to whoever already subscribed
		if (data == null)
			data = new CompressedData(null, null, null);
	}

	public int getChunkX() {
		return chunkX;
	}

	public int getChunkY() {
		return chunkY;
	}

	public int getChunkZ() {
		return chunkZ;
	}

	private int sanitizeCoordinate(int a) {
		return a & 0x1F;
	}

	@Override
	public ActualChunkVoxelContext peek(int x, int y, int z) {
		return new ActualChunkVoxelContext(x, y, z, peekRaw(x, y, z));
	}

	@Override
	public ActualChunkVoxelContext peek(Vector3dc location) {
		return peek((int) (double) location.x(), (int) (double) location.y(), (int) (double) location.z());
	}

	@Override
	public Voxel peekSimple(int x, int y, int z) {
		return world.getContentTranslator().getVoxelForId(VoxelFormat.id(peekRaw(x, y, z)));
	}

	@Override
	public int peekRaw(int x, int y, int z) {
		x = sanitizeCoordinate(x);
		y = sanitizeCoordinate(y);
		z = sanitizeCoordinate(z);

		if (voxelDataArray == null) {
			// Empty chunk ?
			// Use the heightmap to figure out wether or not that getCell should be skylit.
			int sunlight = 0;
			int groundHeight = world.getRegionsSummariesHolder().getHeightAtWorldCoordinates(chunkX * 32 + x,
					chunkZ * 32 + z);
			if (groundHeight < y + chunkY * 32 && groundHeight != Heightmap.Companion.getNO_DATA())
				sunlight = 15;

			return VoxelFormat.format(0, 0, sunlight, 0);
		} else {
			return voxelDataArray[x * 32 * 32 + y * 32 + z];
		}
	}

	public ChunkCell poke(int x, int y, int z, Voxel voxel, int sunlight, int blocklight, int metadata,
			WorldModificationCause cause) {
		return pokeInternal(x, y, z, voxel, sunlight, blocklight, metadata, 0x00, false, true, true, cause);
	}

	public void pokeSimple(int x, int y, int z, Voxel voxel, int sunlight, int blocklight, int metadata) {
		pokeInternal(x, y, z, voxel, sunlight, blocklight, metadata, 0x00, false, true, false, null);
	}

	public void pokeSimpleSilently(int x, int y, int z, Voxel voxel, int sunlight, int blocklight, int metadata) {
		pokeInternal(x, y, z, voxel, sunlight, blocklight, metadata, 0x00, false, false, false, null);
	}

	public void pokeRaw(int x, int y, int z, int raw_data_bits) {
		pokeInternal(x, y, z, null, 0, 0, 0, raw_data_bits, true, true, false, null);
	}

	public void pokeRawSilently(int x, int y, int z, int raw_data_bits) {
		pokeInternal(x, y, z, null, 0, 0, 0, raw_data_bits, true, false, false, null);
	}

	class FreshFutureCell extends FutureCell implements FreshChunkCell {

		public FreshFutureCell(CellData ogContext) {
			super(ogContext);
		}

		@Override
		public Chunk getChunk() {
			return CubicChunk.this;
		}

		@Override
		public CellComponentsHolder components() {
			return CubicChunk.this.components(x, y, z);
		}

		@Override public void refreshRepresentation() {
			//nope
		}

		@Override
		public void registerComponent(String name, VoxelComponent component) {
			components().put(name, component);
		}

	}

	/**
	 * The 'core' of the core, this private function is responsible for placing and
	 * keeping everyone up to snuff on block modifications. It all comes back to
	 * this really.
	 */
	private ActualChunkVoxelContext pokeInternal(final int worldX, final int worldY, final int worldZ, Voxel newVoxel,
			final int sunlight, final int blocklight, final int metadata, int raw_data, final boolean use_raw_data,
			final boolean update, final boolean return_context, final WorldModificationCause cause) {
		int x = sanitizeCoordinate(worldX);
		int y = sanitizeCoordinate(worldY);
		int z = sanitizeCoordinate(worldZ);

		ActualChunkVoxelContext cell_pre = peek(x, y, z);
		Voxel formerVoxel = cell_pre.getVoxel();
		assert formerVoxel != null;

		FreshFutureCell future = new FreshFutureCell(cell_pre);

		if (use_raw_data) {
			// We need this for voxel placement logic
			newVoxel = world.getContentTranslator().getVoxelForId(VoxelFormat.id(raw_data));
			// Build the future from parsing the raw data
			future.setVoxel(newVoxel);
			future.setSunlight(VoxelFormat.sunlight(raw_data));
			future.setBlocklight(VoxelFormat.blocklight(raw_data));
			future.setMetaData(VoxelFormat.meta(raw_data));
		} else {
			// Build the raw data from the set parameters by editing the in-place data
			// (because we allow only editing some aspects of the getCell data)
			raw_data = cell_pre.getData();
			if (newVoxel != null) {
				raw_data = VoxelFormat.changeId(raw_data, world.getContentTranslator().getIdForVoxel(newVoxel));
				future.setVoxel(newVoxel);
			}
			if (sunlight >= 0) {
				raw_data = VoxelFormat.changeSunlight(raw_data, sunlight);
				future.setSunlight(sunlight);
			}
			if (blocklight >= 0) {
				raw_data = VoxelFormat.changeBlocklight(raw_data, blocklight);
				future.setBlocklight(blocklight);
			}
			if (metadata >= 0) {
				raw_data = VoxelFormat.changeMeta(raw_data, metadata);
				future.setMetaData(metadata);
			}
		}

		try {
			if (newVoxel == null || formerVoxel.equals(newVoxel)) {
				formerVoxel.onModification(cell_pre, future, cause);
			} else {
				formerVoxel.onRemove(cell_pre, cause);
				newVoxel.onPlace(future, cause);
			}
		} catch (WorldException e) {
			// Abort !
			if (return_context)
				return cell_pre;
			// throw e;
			else
				return null;
		}

		// Allocate if it makes sense
		if (voxelDataArray == null)
			voxelDataArray = atomicalyCreateInternalData();

		voxelDataArray[x * 32 * 32 + y * 32 + z] = raw_data;

		if (newVoxel != null && !formerVoxel.equals(newVoxel))
			newVoxel.whenPlaced(future);

		// Update lightning
		if (update)
			lightingManager.computeLightSpread(x, y, z, cell_pre.raw_data, raw_data);

		// Increment the modifications counter
		compressionUncommitedModifications.incrementAndGet();

		// Don't spam the thread creation spawn
		occlusion.requestUpdate();

		// Update related summary
		if (update)
			world.getRegionsSummariesHolder().updateOnBlockPlaced(x, y, z, future);

		// Mark the nearby chunks to be re-rendered
		if (update) {
			int sx = chunkX;
			int ex = sx;
			int sy = chunkY;
			int ey = sy;
			int sz = chunkZ;
			int ez = sz;

			if (x == 0)
				sx--;
			else if (x == 31)
				ex++;

			if (y == 0)
				sy--;
			else if (y == 31)
				ey++;

			if (z == 0)
				sz--;
			else if (z == 31)
				ez++;

			for (int ix = sx; ix <= ex; ix++)
				for (int iy = sy; iy <= ey; iy++)
					for (int iz = sz; iz <= ez; iz++) {
						Chunk chunk = world.getChunk(ix, iy, iz);
						if (chunk != null)
							chunk.mesh().requestUpdate();
					}
		}

		// If this is a 'master' world, notify remote users of the change !
		if (update && world instanceof WorldMaster && !(world instanceof WorldTool)) {
			PacketVoxelUpdate packet = new PacketVoxelUpdate(
					new ActualChunkVoxelContext(chunkX * 32 + x, chunkY * 32 + y, chunkZ * 32 + z, raw_data));

			for (WorldUser user : this.chunkHolder.getUsers()) {
				if (!(user instanceof RemotePlayer))
					continue;

				RemotePlayer player = (RemotePlayer) user;

				Entity clientEntity = player.getControlledEntity();
				if (clientEntity == null)
					continue; // Ignore clients that aren't playing

				player.pushPacket(packet);
			}
		}

		if (return_context)
			return new ActualChunkVoxelContext(chunkX * 32 + x, chunkY * 32 + y, chunkZ * 32 + z, raw_data);
		else
			return null;
	}

	@Override
	public CellComponentsHolder components(int x, int y, int z) {
		x &= 0x1f;
		y &= 0x1f;
		z &= 0x1f;

		int index = x * 1024 + y * 32 + z;
		// System.out.println(index);

		CellComponentsHolder components = allCellComponents.get(index);
		if (components == null) {
			components = new CellComponentsHolder(this, index);
			allCellComponents.put(index, components);
		}
		return components;
	}

	public void removeComponents(int index) {
		allCellComponents.remove(index);
	}

	private int[] atomicalyCreateInternalData() {
		chunkDataArrayCreation.acquireUninterruptibly();

		// If it's STILL null
		if (voxelDataArray == null)
			voxelDataArray = new int[32 * 32 * 32];

		chunkDataArrayCreation.release();

		return voxelDataArray;
	}

	@Override
	public String toString() {
		return "[CubicChunk x:" + this.chunkX + " y:" + this.chunkY + " z:" + this.chunkZ + " air:" + isAirChunk() + " light:" + this.lightingManager + "]";
	}

	public boolean isAirChunk() {
		return voxelDataArray == null;
	}

	@Override
	public World getWorld() {
		return world;
	}

	@Override
	public Region getRegion() {
		return holdingRegion;
	}

	public ChunkHolderImplementation holder() {
		return chunkHolder;
	}

	@Override
	public int hashCode() {
		return uuid;
	}

	class ActualChunkVoxelContext extends Cell implements ChunkCell, FreshChunkCell {

		int raw_data;

		public ActualChunkVoxelContext(int x, int y, int z, int data) {
			super((x & 0x1F), (y & 0x1F), (z & 0x1F), world.getContentTranslator().getVoxelForId(VoxelFormat.id(data)),
					VoxelFormat.meta(data), VoxelFormat.blocklight(data), VoxelFormat.sunlight(data));

			this.raw_data = data;
		}

		@Override
		public World getWorld() {
			return world;
		}

		@Override
		public int getX() {
			return x + (chunkX << 5);
		}

		@Override
		public int getY() {
			return y + (chunkY << 5);
		}

		@Override
		public int getZ() {
			return z + (chunkZ << 5);
		}

		@Override
		public Location getLocation() {
			return new Location(world, getX(), getY(), getZ());
		}

		@Override
		public int getData() {
			return raw_data;
		}

		@Override public void refreshRepresentation() {
			//TODO
		}

		@Deprecated
		public int getNeightborData(int side) {
			switch (side) {
			case (0):
				return world.peekRaw(getX() - 1, getY(), getZ());
			case (1):
				return world.peekRaw(getX(), getY(), getZ() + 1);
			case (2):
				return world.peekRaw(getX() + 1, getY(), getZ());
			case (3):
				return world.peekRaw(getX(), getY(), getZ() - 1);
			case (4):
				return world.peekRaw(getX(), getY() + 1, getZ());
			case (5):
				return world.peekRaw(getX(), getY() - 1, getZ());
			}
			throw new RuntimeException("Fuck off");
		}

		@Override
		public Chunk getChunk() {
			return CubicChunk.this;
		}

		@Override
		public CellComponentsHolder components() {
			return CubicChunk.this.components(x, y, z);
		}

		@Override
		public CellData getNeightbor(int side_int) {
			VoxelSide side = VoxelSide.values()[side_int];

			// Fast path for in-chunk neigtbor
			if ((side == VoxelSide.LEFT && x > 0) || (side == VoxelSide.RIGHT && x < 31)
					|| (side == VoxelSide.BOTTOM && y > 0) || (side == VoxelSide.TOP && y < 31)
					|| (side == VoxelSide.BACK && z > 0) || (side == VoxelSide.FRONT && z < 31)) {
				return CubicChunk.this.peek(x + side.dx, y + side.dy, z + side.dz);
			}

			return world.peekSafely(getX() + side.dx, getY() + side.dy, getZ() + side.dz);
		}

		@Override
		public void setVoxel(Voxel voxel) {
			super.setVoxel(voxel);
			poke();
			peek();
		}

		@Override
		public void setMetaData(int metadata) {
			super.setMetaData(metadata);
			poke();
			peek();
		}

		@Override
		public void setSunlight(int sunlight) {
			super.setSunlight(sunlight);
			poke();
			peek();
		}

		@Override
		public void setBlocklight(int blocklight) {
			super.setBlocklight(blocklight);
			poke();
			peek();
		}

		private void peek() {
			raw_data = CubicChunk.this.peekRaw(x, y, z);
		}

		private void poke() {
			CubicChunk.this.pokeSimple(x, y, z, voxel, sunlight, blocklight, metadata);
		}

		@Override
		public void registerComponent(String name, VoxelComponent component) {
			components().put(name, component);
		}
	}

	@Override
	public void destroy() {
		chunkDestructionSemaphore.acquireUninterruptibly();
		this.lightingManager.destroy();
		if(meshData instanceof AutoRebuildingProperty)
			((AutoRebuildingProperty)this.meshData).destroy();
		this.isDestroyed = true;
		//chunksCounter.decrementAndGet();
		chunkDestructionSemaphore.release();
	}

	@Override
	public void addEntity(Entity entity) {
		entitiesLock.lock();
		localEntities.add(entity);
		entitiesLock.unlock();
	}

	@Override
	public void removeEntity(Entity entity) {
		entitiesLock.lock();
		localEntities.remove(entity);
		entitiesLock.unlock();
	}

	@Override
	public IterableIterator<Entity> getEntitiesWithinChunk() {
		return new IterableIteratorWrapper<Entity>(localEntities.iterator()) {

			@Override
			public void remove() {

				entitiesLock.lock();
				super.remove();
				entitiesLock.unlock();
			}

		};
	}

	@Override
	public ChunkLightUpdater lightBaker() {
		return lightingManager;
	}

	@Override
	public ChunkMesh mesh() {
		return meshData;
	}

	@Override
	public ChunkOcclusionManager occlusion() {
		return occlusion;
	}
}
