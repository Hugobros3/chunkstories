//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.iterators;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.chunkstories.api.Location;
import xyz.chunkstories.api.physics.Box;
import xyz.chunkstories.api.util.IterableIterator;
import xyz.chunkstories.api.voxel.Voxel;
import xyz.chunkstories.api.voxel.VoxelFormat;
import xyz.chunkstories.api.voxel.VoxelSide;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.api.world.cell.CellData;

public class AABBVoxelIterator implements IterableIterator<CellData>, CellData {
	private final World world;
	private final Box Box;

	// private final Voxels voxels;

	private int i, j, k;
	private int i2, j2, k2;

	private int minx, miny, minz;
	private int maxx, maxy, maxz;

	Voxel voxel;
	int sunlight, blocklight, metadata;

	public AABBVoxelIterator(World world, Box Box) {
		this.world = world;
		this.Box = Box;

		// this.voxels = world.getGameContext().getContent().voxels();

		this.minx = (int) Math.floor(Box.xPosition);
		this.miny = (int) Math.floor(Box.yPosition);
		this.minz = (int) Math.floor(Box.zPosition);

		this.maxx = (int) Math.ceil(Box.xPosition + Box.xWidth);
		this.maxy = (int) Math.ceil(Box.yPosition + Box.yHeight);
		this.maxz = (int) Math.ceil(Box.zPosition + Box.zWidth);

		this.i = minx;
		this.j = miny;
		this.k = minz;
	}

	public Box getBox() {
		return Box;
	}

	@Override
	public boolean hasNext() {
		return k <= maxz;
		/*
		 * if(i == maxx && j == maxy && k == maxz) return false; return true;
		 */
		// return k <= (int)Math.ceil(Box.zpos + Box.zw);
	}

	@Override
	public CellData next() {

		i2 = i;
		j2 = j;
		k2 = k;

		i++;
		if (i > maxx) {
			j++;
			i = minx;
		}
		if (j > maxy) {
			k++;
			j = miny;
		}
		if (k > maxz) {

		} // throw new UnsupportedOperationException("Out of bounds iterator. Called when
			// hasNext() returned false.");

		// Optimisation here:
		// Instead of making a new CellData object for each iteration we just change
		// this one by pulling the properties
		int raw_data = world.peekRaw(i2, j2, k2);
		voxel = world.getContentTranslator().getVoxelForId(VoxelFormat.id(raw_data));
		sunlight = VoxelFormat.sunlight(raw_data);
		blocklight = VoxelFormat.blocklight(raw_data);
		metadata = VoxelFormat.meta(raw_data);

		return this;
	}

	@Override
	public int getX() {
		return i2;
	}

	@Override
	public int getY() {
		return j2;
	}

	@Override
	public int getZ() {
		return k2;
	}

	@Override
	public World getWorld() {
		return world;
	}

	@Override
	public Voxel getVoxel() {
		return voxel;
	}

	@Override
	public int getMetaData() {
		return metadata;
	}

	@Override
	public int getSunlight() {
		return sunlight;
	}

	@Override
	public int getBlocklight() {
		return blocklight;
	}

	@Override
	public CellData getNeightbor(int side_int) {
		VoxelSide side = VoxelSide.values()[side_int];
		return world.peekSafely(getX() + side.getDx(), getY() + side.getDy(), getZ() + side.getDz());
	}

	@NotNull
	@Override
	public Location getLocation() {
		return new Location(world, getX(), getY(), getZ());
	}

	@Nullable
	@Override
	public xyz.chunkstories.api.physics.Box[] getTranslatedCollisionBoxes() {
		return new Box[0];
	}

	@Override
	public int getNeightborMetadata(int i) {
		return 0;
	}

	@Nullable
	@Override
	public Voxel getNeightborVoxel(int i) {
		return null;
	}
}
