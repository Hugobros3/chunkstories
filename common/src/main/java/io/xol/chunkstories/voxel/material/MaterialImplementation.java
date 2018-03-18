//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.voxel.material;

import java.io.BufferedReader;
import java.io.IOException;

import io.xol.chunkstories.api.voxel.materials.Material;
import io.xol.chunkstories.content.GenericNamedConfigurable;

public class MaterialImplementation extends GenericNamedConfigurable implements Material
{	
	public MaterialImplementation(String name, BufferedReader reader) throws IOException
	{
		super(name, reader);
	}
}
