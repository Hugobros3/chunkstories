package io.xol.chunkstories.world;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class WorldInfoFile extends WorldInfoImplementation {

	private File file;
	
	public WorldInfoFile(File file) throws IOException {
		this(new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")));
		
		this.file = file;
	}

	private WorldInfoFile(BufferedReader bufferedReader) throws IOException {
		super(bufferedReader);
		
		bufferedReader.close();
	}

	public File getFile() {
		return file;
	}
	
	/** Utility to create new worlds */
	public static WorldInfoFile createNewWorld(File folderIn, WorldInfoImplementation basedOn) throws IOException {
		File file = new File(folderIn.getPath() + "/info.txt");
		
		basedOn.save(file);
		
		return new WorldInfoFile(file);
	}
}
