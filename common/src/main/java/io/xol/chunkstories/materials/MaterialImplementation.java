//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.materials;

import java.io.BufferedReader;
import java.io.IOException;

import io.xol.chunkstories.api.voxel.materials.Material;

public class MaterialImplementation extends GenericNamedConfigurable implements Material
{	
	public MaterialImplementation(String name, BufferedReader reader) throws IOException
	{
		super(name, reader);
		
		//this.setProperty("sounds", "sounds/materials/<name>/");
		//this.setProperty("walkingSounds", "sounds/footsteps/generic[1-3].ogg");
		//this.setProperty("runningSounds", "<walkingSounds>");
		//this.setProperty("jumpingSounds", "sounds/footsteps/jump.ogg");
		//this.setProperty("landingSounds", "sounds/footsteps/land.ogg");
	}
}
