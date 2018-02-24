//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import io.xol.chunkstories.content.GameDirectory;

public class WorldInfoMaster extends WorldInfoImplementation {

	private File worldInfoFile;
	
	public WorldInfoMaster(File file) throws IOException {
		this(new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")));
		this.worldInfoFile = file;
	}

	// Private constructor so I can close the reader :(
	private WorldInfoMaster(BufferedReader bufferedReader) throws IOException {
		super(bufferedReader);
		bufferedReader.close();
	}

	public WorldInfoMaster(WorldInfoImplementation worldInfo) {
		super(worldInfo.getInternalName(), worldInfo.getName(), worldInfo.getSeed(), worldInfo.getDescription(), worldInfo.getSize(), worldInfo.getGeneratorName());
		worldInfoFile = new File(GameDirectory.getGameFolderPath() + "/worlds/" + worldInfo.getInternalName() + "/info.world");
		worldInfo.getProperties().forEach((key, value) -> this.setProperty(key, value));
	}

	public File getFile() {
		return worldInfoFile;
	}
	
	public void save()
	{
		try
		{
			if(!worldInfoFile.getParentFile().exists())
				worldInfoFile.getParentFile().mkdirs();
			
			FileOutputStream output = new FileOutputStream(worldInfoFile);
			saveInStream(output);
			output.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
