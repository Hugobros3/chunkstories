//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer;

/**
 * Inspired by "greedy meshing" code
 * It's simpler, provides an "ok" mesh
 */
public class FloodMesh2D
{
	public static void main(String[] a)
	{
		int[] cuck = {
				32, 
			//YX0123
				1, 1, 0, 0,
				0, 1, 2, 2,
				0, 1, 2, 2,
				1, 0, 0, 0,
		};
		floodMesh(cuck, 1, 4);
	}
	
	public static void floodMesh(int[] heightmap, int offset, int dimension)
	{
		boolean[] mask = new boolean[dimension * dimension];
		int n = 0;
		int x, y, w, h;
		
		while(n < dimension * dimension)
		{
			//Only creates faces once !
			if(!mask[n])
			{
				int level = heightmap[offset + n];
				if(level > 0)
				{
					x = n / dimension;
					y = n % dimension;
					
					w = 1;
					h = 1;
					
					int i;
					//Expand X as long as we can
					for(int j = x+1; j < dimension; j++)
					{
						i = j * dimension + y;
						if(heightmap[offset + i] == level && !mask[i])
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
							if(heightmap[offset + i] == level && !mask[i])
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
					System.out.println("Contains rectangle of "+level+" start: "+x+":"+y+" size: "+w+"x"+h);
				}
			}	
			n++;
		}
	}
}
