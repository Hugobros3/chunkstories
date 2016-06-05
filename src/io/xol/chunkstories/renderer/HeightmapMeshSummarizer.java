package io.xol.chunkstories.renderer;

public class HeightmapMeshSummarizer
{
	public HeightmapMeshSummarizer(int[] heightmap, int offset, int dimension, int x0, int y0, int heightmapSize)
	{
		this.heightmap = heightmap;
		this.offset = offset;
		this.dimension = dimension;
		
		this.x0 = x0;
		this.y0 = y0;
		this.heightmapSize = heightmapSize;
		mask = new boolean[dimension * dimension];
	}
	
	boolean[] mask;
	int[] heightmap;
	int offset, dimension;
	
	int n = 0;
	int x, y, w, h;
	int x0, y0, heightmapSize;
	
	private int accessHeightmap(int x, int y)
	{
		return heightmap[offset + (x0 + x) * heightmapSize + (y0 + y)];
	}
	
	public Surface nextSurface()
	{
		//boolean[] mask = new boolean[dimension * dimension];
		while(n < dimension * dimension)
		{
			//Only creates faces once !
			if(!mask[n])
			{
				x = n / dimension;
				y = n % dimension;
				int level = accessHeightmap(x, y);
				if(level >= 0)
				{
					mask[n] = true;
					
					w = 1;
					h = 1;
					
					int i;
					//Expand X as long as we can
					for(int j = x+1; j < dimension; j++)
					{
						i = j * dimension + y;
						if(accessHeightmap(j, y) == level && !mask[i])
						{
							//We are clear to expand the rectangle
							w++;
							mask[i] = true;
						}
						else
						{
							break;
						}
					}
					//Expand Y as long as we can
					for(int j = y+1; j < dimension; j++)
					{
						//Check the entire next line
						boolean wholeLineIsOk = true;
						for(int k = x; k < x+w; k++)
						{
							i = k * dimension + j;
							if(accessHeightmap(k, j) == level && !mask[i])
							{
								
							}
							else
							{
								wholeLineIsOk = false;
								break;
							}
						}
						if(wholeLineIsOk)
						{
							//We are clear to expand the rectangle
							for(int k = x; k < x+w; k++)
							{
								i = k * dimension + j;
								mask[i] = true;
							}
							h++;
						}
						else
						{
							break;
						}
					}
					n++;
					return new Surface(x, y, w, h, level);
					//System.out.println("Contains rectangle of "+level+" start: "+x+":"+y+" size: "+w+"x"+h);
				}
			}	
			n++;
		}
		return null;
	}
	
	class Surface {
		//Describes a uniform surface
		
		int x, y, w, h, level;

		public int getX()
		{
			return x;
		}

		public int getY()
		{
			return y;
		}

		public int getW()
		{
			return w;
		}

		public int getH()
		{
			return h;
		}
		
		public int getLevel()
		{
			return level;
		}

		public Surface(int x, int y, int w, int h, int level)
		{
			super();
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.level = level;
		}
	}
}
