//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.serialization.OfflineSerializedData;

public class SerializedEntityFile implements OfflineSerializedData {
	private final File file;

	public SerializedEntityFile(String string) {
		file = new File(string);
	}

	public boolean exists() {
		return file.exists();
	}

	public String toString() {
		return "[CSF File: " + file.toString() + "]";
	}

	public Entity read(World world) {
		try {
			DataInputStream in = new DataInputStream(new FileInputStream(file));

			Entity entity = EntitySerializer.readEntityFromStream(in, this, world);

			in.close();

			return entity;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void write(Entity entity) {
		try {
			file.getParentFile().mkdirs();

			DataOutputStream out = new DataOutputStream(new FileOutputStream(file));

			EntitySerializer.writeEntityToStream(out, this, entity);

			out.flush();
			out.close();

			System.out.println("Wrote serialized entity to : " + file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
