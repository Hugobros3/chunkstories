package io.xol.chunkstories.world;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class WorldAccessor
{

	// This takes care of loading/saving the world : either via flat files,
	// either in multiplayer
	protected World world;

	public void initialize(World w)
	{
		// System.out.println("World init to :"+w);
		// System.out.println("World not init to :"+null);
		world = w;
	}

	public abstract CubicChunk loadChunk(int cx, int cy, int cz);

	public abstract boolean saveChunk(int cx, int cy, int cz, CubicChunk c);

	public int getDataAt(int x, int y, int z, int h)
	{
		return 0;
	}

	public int getHeightAt(int x, int z)
	{
		// TODO Auto-generated method stub
		return 0;
	}
}
