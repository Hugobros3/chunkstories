package io.xol.chunkstories.api.world;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface WorldInfo
{
	public String getInternalName();

	public String getName();

	public String getSeed();

	public String getDescription();

	public WorldSize getSize();

	public String getGeneratorName();

	public enum WorldSize
	{
		TINY(32, "1x1km"), SMALL(64, "2x2km"), MEDIUM(128, "4x4km"), LARGE(512, "16x16km"), HUGE(2048, "64x64km");
	
		// Warning : this can be VERY ressource intensive as it will make a
		// 4294km2 map,
		// leading to enormous map sizes ( in the order of 10Gbs to 100Gbs )
		// when fully explored.
	
		WorldSize(int sizeInChunks, String n)
		{
			this.sizeInChunks = sizeInChunks;
			this.maskForChunksCoordinates = sizeInChunks - 1;
			this.bitlengthOfHorizontalChunksCoordinates = (int) (Math.log(sizeInChunks) / Math.log(2));
			name = n;
		}
	
		public final int sizeInChunks;
		public final int maskForChunksCoordinates;
		public final int bitlengthOfHorizontalChunksCoordinates;
		public final int heightInChunks = 32;
		public final int bitlengthOfVerticalChunksCoordinates = 5;
		public final String name;
	
		public static String getAllSizes()
		{
			String sizes = "";
			for (WorldSize s : WorldSize.values())
			{
				sizes = sizes + s.name() + ", " + s.name + " ";
			}
			return sizes;
		}
	
		public static WorldSize getWorldSize(String name)
		{
			name.toUpperCase();
			for (WorldSize s : WorldSize.values())
			{
				if (s.name().equals(name))
					return s;
			}
			return null;
		}
	}
}